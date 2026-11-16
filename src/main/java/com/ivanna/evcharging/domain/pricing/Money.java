package com.ivanna.evcharging.domain.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * One place for the money conventions: EUR, two decimals, half-up rounding.
 * BigDecimal is used because double cannot represent amounts like 0.10 exactly.
 */
public final class Money {

    public static final Currency CURRENCY = Currency.getInstance("EUR");
    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING);

    private Money() {
    }

    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(SCALE, ROUNDING);
    }

    static BigDecimal requireRate(BigDecimal rate, String name) {
        if (rate == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        if (rate.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative but was " + rate);
        }
        return rate;
    }
}
