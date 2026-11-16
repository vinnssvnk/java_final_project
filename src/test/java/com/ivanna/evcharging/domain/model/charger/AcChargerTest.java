package com.ivanna.evcharging.domain.model.charger;

import com.ivanna.evcharging.domain.model.vehicle.Vehicle;
import com.ivanna.evcharging.exception.IncompatibleConnectorException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AcChargerTest {

    @Test
    void shouldUseChargerPowerWhenItIsTheLimit() {
        Charger charger = new AcCharger("C1", "S1", ConnectorType.TYPE2, 7.4);
        Vehicle vehicle = new Vehicle("V1", "Test", 37, 11, 50, Set.of(ConnectorType.TYPE2));

        // 18.5 kWh at 7.4 kW = 2.5 h
        assertEquals(Duration.ofMinutes(150), charger.estimateDuration(vehicle, 10, 60));
    }

    @Test
    void shouldUseVehicleLimitWhenLowerThanCharger() {
        Charger charger = new AcCharger("C1", "S1", ConnectorType.TYPE2, 22);
        Vehicle vehicle = new Vehicle("V1", "Test", 44, 11, 50, Set.of(ConnectorType.TYPE2));

        // 22 kWh at 11 kW (vehicle on-board charger) = 2 h
        assertEquals(Duration.ofMinutes(120), charger.estimateDuration(vehicle, 0, 50));
    }

    @Test
    void shouldRoundDurationUpToWholeMinute() {
        Charger charger = new AcCharger("C1", "S1", ConnectorType.TYPE2, 11);
        Vehicle vehicle = new Vehicle("V1", "Test", 50, 11, 50, Set.of(ConnectorType.TYPE2));

        // 30 kWh at 11 kW = 163.6 min
        assertEquals(Duration.ofMinutes(164), charger.estimateDuration(vehicle, 20, 80));
    }

    @Test
    void shouldThrowWhenVehicleLacksConnector() {
        Charger charger = new AcCharger("C1", "S1", ConnectorType.TYPE2, 22);
        Vehicle chademoOnly = new Vehicle("V1", "Old Leaf", 24, 3.6, 50, Set.of(ConnectorType.CHADEMO));

        assertThrows(IncompatibleConnectorException.class, () -> charger.estimateDuration(chademoOnly, 20, 80));
    }

    @Test
    void shouldThrowWhenVehicleDoesNotSupportAc() {
        Charger charger = new AcCharger("C1", "S1", ConnectorType.TYPE2, 22);
        Vehicle dcOnly = new Vehicle("V1", "Test", 50, 0, 50, Set.of(ConnectorType.TYPE2));

        assertThrows(IncompatibleConnectorException.class, () -> charger.estimateDuration(dcOnly, 20, 80));
    }

    @Test
    void shouldRejectInvalidChargeRange() {
        Charger charger = new AcCharger("C1", "S1", ConnectorType.TYPE2, 22);
        Vehicle vehicle = new Vehicle("V1", "Test", 50, 11, 50, Set.of(ConnectorType.TYPE2));

        assertThrows(IllegalArgumentException.class, () -> charger.estimateDuration(vehicle, 80, 80));
        assertThrows(IllegalArgumentException.class, () -> charger.estimateDuration(vehicle, -1, 50));
        assertThrows(IllegalArgumentException.class, () -> charger.estimateDuration(vehicle, 50, 101));
    }
}
