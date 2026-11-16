package com.ivanna.evcharging.domain.model.charger;

import com.ivanna.evcharging.domain.model.vehicle.Vehicle;
import com.ivanna.evcharging.exception.IncompatibleConnectorException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DcFastChargerTest {

    private final Vehicle vehicle = new Vehicle("V1", "Test", 77, 11, 150, Set.of(ConnectorType.TYPE2, ConnectorType.CCS2));

    @Test
    void shouldChargeAtFullPowerBelowEightyPercent() {
        Charger charger = new DcFastCharger("C1", "S1", ConnectorType.CCS2, 150);

        // 0.77 kWh per percent: 46.2 kWh at 150 kW = 18.48 min
        assertEquals(Duration.ofMinutes(19), charger.estimateDuration(vehicle, 10, 70));
    }

    @Test
    void shouldChargeSlowerAboveEightyPercent() {
        Charger charger = new DcFastCharger("C1", "S1", ConnectorType.CCS2, 150);

        Duration belowTaper = charger.estimateDuration(vehicle, 70, 80);
        Duration aboveTaper = charger.estimateDuration(vehicle, 80, 90);

        assertEquals(Duration.ofMinutes(4), belowTaper);   // 7.7 kWh at 150 kW = 3.08 min
        assertEquals(Duration.ofMinutes(7), aboveTaper);   // 7.7 kWh at 75 kW = 6.16 min
        // 3.08 + 6.16 = 9.24 min, rounded once at the end
        assertEquals(Duration.ofMinutes(10), charger.estimateDuration(vehicle, 70, 90));
        assertTrue(aboveTaper.compareTo(belowTaper) > 0);
    }

    @Test
    void shouldUseVehicleDcLimitWhenLowerThanCharger() {
        Charger charger = new DcFastCharger("C1", "S1", ConnectorType.CCS2, 350);
        Vehicle slowDc = new Vehicle("V2", "Test", 50, 11, 50, Set.of(ConnectorType.CCS2));

        // 25 kWh at 50 kW = 30 min
        assertEquals(Duration.ofMinutes(30), charger.estimateDuration(slowDc, 20, 70));
    }

    @Test
    void shouldThrowWhenVehicleLacksConnector() {
        Charger charger = new DcFastCharger("C1", "S1", ConnectorType.CHADEMO, 50);

        assertThrows(IncompatibleConnectorException.class, () -> charger.estimateDuration(vehicle, 20, 80));
    }

    @Test
    void shouldThrowWhenVehicleWithoutDcUsesDcCharger() {
        Charger charger = new DcFastCharger("C1", "S1", ConnectorType.CCS2, 150);
        Vehicle acOnly = new Vehicle("V3", "City car", 32, 7.4, 0, Set.of(ConnectorType.TYPE2, ConnectorType.CCS2));

        assertThrows(IncompatibleConnectorException.class, () -> charger.estimateDuration(acOnly, 20, 80));
    }
}
