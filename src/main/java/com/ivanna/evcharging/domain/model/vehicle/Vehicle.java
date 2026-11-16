package com.ivanna.evcharging.domain.model.vehicle;

import com.ivanna.evcharging.domain.model.Validation;
import com.ivanna.evcharging.domain.model.charger.ConnectorType;

import java.util.Objects;
import java.util.Set;

import static com.ivanna.evcharging.domain.model.Validation.requireNonNegative;
import static com.ivanna.evcharging.domain.model.Validation.requirePositive;
import static com.ivanna.evcharging.domain.model.Validation.requireText;

/**
 * An electric vehicle. A max power of 0 kW means the vehicle cannot charge on that kind of current.
 */
public record Vehicle(String id, String model, double batteryKwh, double maxAcKw, double maxDcKw,
                      Set<ConnectorType> connectors) {

    public Vehicle {
        requireText(id, "id");
        requireText(model, "model");
        requirePositive(batteryKwh, "batteryKwh");
        requireNonNegative(maxAcKw, "maxAcKw");
        requireNonNegative(maxDcKw, "maxDcKw");
        Objects.requireNonNull(connectors, "connectors must not be null");
        if (connectors.isEmpty()) {
            throw new IllegalArgumentException("Vehicle " + id + " needs at least one connector");
        }
        connectors = Set.copyOf(connectors);
    }

    public boolean supports(ConnectorType connector) {
        return connectors.contains(connector);
    }

    public double energyNeededKwh(int fromPct, int toPct) {
        Validation.requireValidChargeRange(fromPct, toPct);
        return batteryKwh * (toPct - fromPct) / 100;
    }
}
