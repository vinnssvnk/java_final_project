package com.ivanna.evcharging.infrastructure.io;

import com.ivanna.evcharging.domain.model.charger.AcCharger;
import com.ivanna.evcharging.domain.model.charger.Charger;
import com.ivanna.evcharging.domain.model.charger.DcFastCharger;
import com.ivanna.evcharging.domain.model.reservation.Reservation;
import com.ivanna.evcharging.domain.model.station.Station;
import com.ivanna.evcharging.domain.model.vehicle.Vehicle;
import com.ivanna.evcharging.domain.pricing.FlatPricing;
import com.ivanna.evcharging.domain.pricing.TimeOfUsePricing;
import com.ivanna.evcharging.domain.repository.Repository;
import com.ivanna.evcharging.exception.DataLoadException;
import com.ivanna.evcharging.infrastructure.persistence.InMemoryRepository;
import com.ivanna.evcharging.infrastructure.persistence.InMemoryReservationRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataInitializerTest {

    private final Repository<Station, String> stations = new InMemoryRepository<>(Station::id);
    private final Repository<Charger, String> chargers = new InMemoryRepository<>(Charger::getId);
    private final Repository<Vehicle, String> vehicles = new InMemoryRepository<>(Vehicle::id);
    private final InMemoryReservationRepository reservations = new InMemoryReservationRepository();

    private DataInitializer initializer(String directory) {
        return new DataInitializer(directory, stations, chargers, vehicles, reservations);
    }

    @Test
    void shouldLoadBundledSampleDataWithoutErrors() throws DataLoadException {
        DataInitializer.Summary summary = initializer("/data").initialize();

        assertTrue(summary.errors().isEmpty(), () -> "unexpected errors: " + summary.errors());
        assertEquals(3, summary.stations());
        assertEquals(8, summary.chargers());
        assertEquals(8, summary.vehicles());
        assertEquals(7, summary.reservations());
        assertInstanceOf(FlatPricing.class, stations.findById("S1").orElseThrow().pricing());
        assertInstanceOf(TimeOfUsePricing.class, stations.findById("S2").orElseThrow().pricing());
        assertInstanceOf(AcCharger.class, chargers.findById("C1").orElseThrow());
        assertInstanceOf(DcFastCharger.class, chargers.findById("C5").orElseThrow());
    }

    @Test
    void shouldRegisterActiveReservationsInTheirChargerSchedule() throws DataLoadException {
        initializer("/data").initialize();

        List<String> scheduled = chargers.findById("C5").orElseThrow().getSchedule().reservations().stream()
                .map(Reservation::getId)
                .toList();
        assertEquals(List.of("R001", "R002"), scheduled);
    }

    @Test
    void shouldSkipInvalidRowsAndUnknownReferences() throws DataLoadException {
        DataInitializer.Summary summary = initializer("/data-with-errors").initialize();

        assertEquals(1, summary.stations());
        assertEquals(1, summary.chargers());
        assertEquals(1, summary.vehicles());
        assertEquals(List.of("R1", "R4"), reservations.findAll().stream().map(Reservation::getId).toList());
        assertEquals(List.of(
                "stations.csv line 3: invalid tariff 'NIGHT_RATE'",
                "chargers.csv line 3: unknown station 'S9'",
                "chargers.csv line 4: invalid maxPowerKw 'abc'",
                "vehicles.csv line 3: invalid connector 'SCHUKO'",
                "reservations.csv line 4: unknown vehicle 'V9'"), summary.errors().subList(0, 5));
        assertTrue(summary.errors().get(5).startsWith("reservations.csv R2: Charger C1 is already booked by R1"));
    }

    @Test
    void shouldFailWhenDataFileIsMissing() {
        assertThrows(DataLoadException.class, () -> initializer("/no-such-directory").initialize());
    }
}
