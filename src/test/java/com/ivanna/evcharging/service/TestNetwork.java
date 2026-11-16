package com.ivanna.evcharging.service;

import com.ivanna.evcharging.domain.model.charger.AcCharger;
import com.ivanna.evcharging.domain.model.charger.Charger;
import com.ivanna.evcharging.domain.model.charger.ConnectorType;
import com.ivanna.evcharging.domain.model.charger.DcFastCharger;
import com.ivanna.evcharging.domain.model.station.Station;
import com.ivanna.evcharging.domain.model.vehicle.Vehicle;
import com.ivanna.evcharging.domain.pricing.CancellationPolicy;
import com.ivanna.evcharging.domain.pricing.FlatPricing;
import com.ivanna.evcharging.domain.repository.Repository;
import com.ivanna.evcharging.infrastructure.persistence.InMemoryRepository;
import com.ivanna.evcharging.infrastructure.persistence.InMemoryReservationRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

/**
 * A small, fully known charging network for service tests.
 *   - S1 "Cheap" flat 0.40 EUR/kWh with AC1 and AC2 (AC, TYPE2, 11 kW)
 *   - S2 "Fast" flat 0.60 EUR/kWh with DC1 (DC, CCS2, 150 kW)
 *   - V1: 50 kWh, AC 11 kW, DC 100 kW, TYPE2 + CCS2
 *   - V2: 40 kWh, AC 11 kW only, TYPE2
 */
final class TestNetwork {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 14, 8, 0);

    final Repository<Station, String> stations = new InMemoryRepository<>(Station::id);
    final Repository<Charger, String> chargers = new InMemoryRepository<>(Charger::getId);
    final Repository<Vehicle, String> vehicles = new InMemoryRepository<>(Vehicle::id);
    final InMemoryReservationRepository reservations = new InMemoryReservationRepository();

    TestNetwork() {
        stations.save(new Station("S1", "Cheap", "Vienna", new FlatPricing(new BigDecimal("0.40"))));
        stations.save(new Station("S2", "Fast", "Vienna", new FlatPricing(new BigDecimal("0.60"))));
        chargers.save(new AcCharger("AC1", "S1", ConnectorType.TYPE2, 11));
        chargers.save(new AcCharger("AC2", "S1", ConnectorType.TYPE2, 11));
        chargers.save(new DcFastCharger("DC1", "S2", ConnectorType.CCS2, 150));
        vehicles.save(new Vehicle("V1", "Both", 50, 11, 100, Set.of(ConnectorType.TYPE2, ConnectorType.CCS2)));
        vehicles.save(new Vehicle("V2", "AC only", 40, 11, 0, Set.of(ConnectorType.TYPE2)));
    }

    static Clock clockAt(LocalDateTime time) {
        return Clock.fixed(time.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    ReservationService reservationService() {
        return new ReservationService(stations, chargers, vehicles, reservations, clockAt(NOW));
    }

    CancellationService cancellationServiceAt(LocalDateTime now) {
        return new CancellationService(reservations, chargers, new CancellationPolicy(), clockAt(now));
    }

    ReportService reportService() {
        return new ReportService(stations, chargers, vehicles, reservations);
    }

    Charger charger(String id) {
        return chargers.findById(id).orElseThrow();
    }
}
