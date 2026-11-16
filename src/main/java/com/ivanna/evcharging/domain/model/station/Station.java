package com.ivanna.evcharging.domain.model.station;

import com.ivanna.evcharging.domain.model.reservation.TimeSlot;
import com.ivanna.evcharging.domain.pricing.PricingPolicy;

import java.math.BigDecimal;
import java.util.Objects;

import static com.ivanna.evcharging.domain.model.Validation.requireText;

/** A charging location. Each station has its own tariff, which is why pricing lives here and not on the charger. */
public record Station(String id, String name, String city, PricingPolicy pricing) {

    public Station {
        requireText(id, "id");
        requireText(name, "name");
        requireText(city, "city");
        Objects.requireNonNull(pricing, "pricing must not be null");
    }

    public BigDecimal priceFor(TimeSlot slot, double energyKwh) {
        return pricing.priceFor(slot, energyKwh);
    }
}
