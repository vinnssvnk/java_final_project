package com.ivanna.evcharging.domain.pricing;

import com.ivanna.evcharging.domain.model.reservation.TimeSlot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeOfUsePricingTest {

    private static final LocalDate DAY = LocalDate.of(2026, 12, 14);

    private final PricingPolicy pricing = new TimeOfUsePricing(new BigDecimal("0.30"), new BigDecimal("0.50"));

    private static TimeSlot slot(int hour, int minute, int durationMinutes) {
        return TimeSlot.of(LocalDateTime.of(DAY, LocalTime.of(hour, minute)), Duration.ofMinutes(durationMinutes));
    }

    @Test
    void shouldUseBasePriceForFullyOffPeakSession() {
        assertEquals(new BigDecimal("6.00"), pricing.priceFor(slot(10, 0, 60), 20));
    }

    @Test
    void shouldUsePeakPriceForFullyPeakSession() {
        assertEquals(new BigDecimal("10.00"), pricing.priceFor(slot(18, 0, 60), 20));
    }

    @Test
    void shouldSplitSessionCrossingFivePm() {
        // 16:30-17:30: half the energy off-peak, half at peak: 10 * 0.30 + 10 * 0.50
        assertEquals(new BigDecimal("8.00"), pricing.priceFor(slot(16, 30, 60), 20));
    }

    @Test
    void shouldPriceEachBlockByItsStartTime() {
        // Blocks start at 16:50 (off-peak), 17:05, 17:20, 17:35 (peak): 15 min off-peak, 45 min peak
        assertEquals(new BigDecimal("9.00"), pricing.priceFor(slot(16, 50, 60), 20));
    }

    @Test
    void shouldReturnToBasePriceAtNinePm() {
        assertEquals(new BigDecimal("8.00"), pricing.priceFor(slot(20, 30, 60), 20));
    }

    @Test
    void shouldWeightShorterLastBlockByItsLength() {
        // 16:45-17:05: a 15 min off-peak block and a 5 min peak block
        // 12 kWh: 9 kWh * 0.30 + 3 kWh * 0.50 = 2.70 + 1.50
        assertEquals(new BigDecimal("4.20"), pricing.priceFor(slot(16, 45, 20), 12));
    }
}
