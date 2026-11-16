package com.ivanna.evcharging.domain.pricing;

import com.ivanna.evcharging.domain.model.reservation.TimeSlot;

import java.math.BigDecimal;

public class FlatPricing implements PricingPolicy {

    private final BigDecimal pricePerKwh;

    public FlatPricing(BigDecimal pricePerKwh) {
        this.pricePerKwh = Money.requireRate(pricePerKwh, "pricePerKwh");
    }

    @Override
    public BigDecimal priceFor(TimeSlot slot, double energyKwh) {
        return Money.round(pricePerKwh.multiply(BigDecimal.valueOf(energyKwh)));
    }

    @Override
    public String describe() {
        return "flat " + pricePerKwh + " " + Money.CURRENCY + "/kWh";
    }
}
