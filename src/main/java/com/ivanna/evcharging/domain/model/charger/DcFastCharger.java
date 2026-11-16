package com.ivanna.evcharging.domain.model.charger;

import com.ivanna.evcharging.domain.model.vehicle.Vehicle;

/** DC fast charging: full power up to 80 %, then the battery protects itself and power halves. */
public class DcFastCharger extends Charger {

    static final int TAPER_START_PCT = 80;

    public DcFastCharger(String id, String stationId, ConnectorType connector, double maxPowerKw) {
        super(id, stationId, connector, maxPowerKw, ChargerType.DC);
    }

    @Override
    protected double powerAt(Vehicle vehicle, int pct) {
        double power = maxPowerFor(vehicle);
        return pct >= TAPER_START_PCT ? power / 2 : power;
    }
}
