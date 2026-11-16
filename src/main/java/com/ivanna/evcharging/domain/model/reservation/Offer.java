package com.ivanna.evcharging.domain.model.reservation;

import com.ivanna.evcharging.domain.model.charger.Charger;

import java.math.BigDecimal;
import java.util.Objects;

/** What one charger can offer for a requested session: the earliest free slot and its price. */
public record Offer(Charger charger, TimeSlot slot, BigDecimal price) {

    public Offer {
        Objects.requireNonNull(charger, "charger must not be null");
        Objects.requireNonNull(slot, "slot must not be null");
        Objects.requireNonNull(price, "price must not be null");
    }
}
