package com.ivanna.evcharging.domain.pricing;

import com.ivanna.evcharging.domain.model.reservation.TimeSlot;

import java.math.BigDecimal;

/** How a station charges for energy. Each station plugs in its own policy. */
public interface PricingPolicy {

    /** Price in EUR (scale 2) for delivering energyKwh during slot. */
    BigDecimal priceFor(TimeSlot slot, double energyKwh);

    /** Human-readable tariff for listings. */
    String describe();
}
