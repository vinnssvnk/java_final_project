package com.ivanna.evcharging.domain.model.charger;

import com.ivanna.evcharging.domain.model.Validation;
import com.ivanna.evcharging.domain.model.vehicle.Vehicle;
import com.ivanna.evcharging.exception.IncompatibleConnectorException;

import java.time.Duration;
import java.util.Objects;

import static com.ivanna.evcharging.domain.model.Validation.requirePositive;
import static com.ivanna.evcharging.domain.model.Validation.requireText;

/**
 * A single charge point. The algorithm for estimating a session is the same for every charger
 * (template method estimateDuration); only the charging curve (powerAt) differs.
 */
public abstract class Charger {

    // Guards the minute rounding against floating-point noise such as 120.00000000001 minutes.
    private static final double ROUNDING_TOLERANCE_MINUTES = 1e-9;

    private final String id;
    private final String stationId;
    private final ConnectorType connector;
    private final double maxPowerKw;
    private final ChargerType type;
    private final ChargerSchedule schedule;

    protected Charger(String id, String stationId, ConnectorType connector, double maxPowerKw, ChargerType type) {
        this.id = requireText(id, "id");
        this.stationId = requireText(stationId, "stationId");
        this.connector = Objects.requireNonNull(connector, "connector must not be null");
        this.maxPowerKw = requirePositive(maxPowerKw, "maxPowerKw");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.schedule = new ChargerSchedule(id);
    }

    /**
     * Charging power in kW while the battery is at pct percent.
     * Only called for compatible vehicles, so the result is always positive.
     */
    protected abstract double powerAt(Vehicle vehicle, int pct);

    /**
     * Simulates the session one percent at a time so that curves which change with the state
     * of charge (like DC tapering above 80 %) are reflected. The result is rounded up to whole minutes.
     */
    public final Duration estimateDuration(Vehicle vehicle, int fromPct, int toPct) {
        Validation.requireValidChargeRange(fromPct, toPct);
        ensureCompatibleWith(vehicle);
        double energyPerPercentKwh = vehicle.batteryKwh() / 100;
        double hours = 0;
        for (int pct = fromPct; pct < toPct; pct++) {
            hours += energyPerPercentKwh / powerAt(vehicle, pct);
        }
        long minutes = (long) Math.ceil(hours * 60 - ROUNDING_TOLERANCE_MINUTES);
        return Duration.ofMinutes(minutes);
    }

    public boolean isCompatibleWith(Vehicle vehicle) {
        return vehicle.supports(connector) && type.vehicleLimitKw(vehicle) > 0;
    }

    public void ensureCompatibleWith(Vehicle vehicle) {
        if (!vehicle.supports(connector)) {
            throw new IncompatibleConnectorException(
                    "Vehicle " + vehicle.id() + " has no " + connector + " connector required by charger " + id);
        }
        if (type.vehicleLimitKw(vehicle) <= 0) {
            throw new IncompatibleConnectorException(
                    "Vehicle " + vehicle.id() + " does not support " + type + " charging on charger " + id);
        }
    }

    /** The lower of what this charger can deliver and what the vehicle accepts on this current type. */
    protected final double maxPowerFor(Vehicle vehicle) {
        return Math.min(maxPowerKw, type.vehicleLimitKw(vehicle));
    }

    public String getId() {
        return id;
    }

    public String getStationId() {
        return stationId;
    }

    public ConnectorType getConnector() {
        return connector;
    }

    public double getMaxPowerKw() {
        return maxPowerKw;
    }

    public ChargerType getType() {
        return type;
    }

    public ChargerSchedule getSchedule() {
        return schedule;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Charger charger && id.equals(charger.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " (" + type + " " + connector + " " + maxPowerKw + " kW)";
    }
}
