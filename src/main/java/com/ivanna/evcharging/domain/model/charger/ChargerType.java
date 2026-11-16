package com.ivanna.evcharging.domain.model.charger;

import com.ivanna.evcharging.domain.model.vehicle.Vehicle;

public enum ChargerType {
    AC,
    DC;

    /** Highest power the vehicle accepts on this kind of current; 0 means the vehicle does not support it. */
    public double vehicleLimitKw(Vehicle vehicle) {
        return switch (this) {
            case AC -> vehicle.maxAcKw();
            case DC -> vehicle.maxDcKw();
        };
    }
}
