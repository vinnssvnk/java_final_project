package com.ivanna.evcharging.domain.model.charger;

import com.ivanna.evcharging.domain.model.vehicle.Vehicle;

/** AC charging: the vehicle's on-board charger limits power, which stays constant for the whole session. */
public class AcCharger extends Charger {

    public AcCharger(String id, String stationId, ConnectorType connector, double maxPowerKw) {
        super(id, stationId, connector, maxPowerKw, ChargerType.AC);
    }

    @Override
    protected double powerAt(Vehicle vehicle, int pct) {
        return maxPowerFor(vehicle);
    }
}
