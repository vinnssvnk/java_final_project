package com.ivanna.evcharging.domain.pricing;

import com.ivanna.evcharging.domain.model.reservation.TimeSlot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlatPricingTest {

    private static final TimeSlot EVENING = TimeSlot.of(LocalDateTime.of(2026, 12, 14, 18, 0), Duration.ofHours(1));

    @Test
    void shouldChargeSamePricePerKwhAtAnyTime() {
        PricingPolicy pricing = new FlatPricing(new BigDecimal("0.39"));

        assertEquals(new BigDecimal("11.70"), pricing.priceFor(EVENING, 30));
    }

    @Test
    void shouldRoundHalfUpToCents() {
        PricingPolicy pricing = new FlatPricing(new BigDecimal("0.45"));

        // 0.45 * 2.5 = 1.125 -> 1.13
        assertEquals(new BigDecimal("1.13"), pricing.priceFor(EVENING, 2.5));
    }

    @Test
    void shouldRejectNegativeRate() {
        assertThrows(IllegalArgumentException.class, () -> new FlatPricing(new BigDecimal("-0.10")));
    }
}
