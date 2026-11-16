package com.ivanna.evcharging.domain.model;

import java.util.Objects;

/** Small guard helpers shared by the model classes so every constructor validates the same way. */
public final class Validation {

    private Validation() {
    }

    public static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public static double requirePositive(double value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive but was " + value);
        }
        return value;
    }

    public static double requireNonNegative(double value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative but was " + value);
        }
        return value;
    }

    public static void requireValidChargeRange(int fromPct, int toPct) {
        if (fromPct < 0 || toPct > 100 || fromPct >= toPct) {
            throw new IllegalArgumentException(
                    "Charge range must satisfy 0 <= from < to <= 100 but was " + fromPct + " -> " + toPct);
        }
    }
}
