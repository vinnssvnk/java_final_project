package com.ivanna.evcharging.domain.pricing;

import com.ivanna.evcharging.domain.model.reservation.TimeSlot;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Peak-hour tariff: energy delivered between 17:00 and 21:00 costs the peak price.
 *
 * Simplification: we do not know the real charging curve inside the slot, so the energy is
 * assumed to flow evenly over the slot's duration. The slot is cut into 15-minute blocks (the last
 * one may be shorter) and each block is priced by the time it starts.
 */
public class TimeOfUsePricing implements PricingPolicy {

    static final LocalTime PEAK_START = LocalTime.of(17, 0);
    static final LocalTime PEAK_END = LocalTime.of(21, 0);
    static final Duration BLOCK = Duration.ofMinutes(15);

    private final BigDecimal basePrice;
    private final BigDecimal peakPrice;

    public TimeOfUsePricing(BigDecimal basePrice, BigDecimal peakPrice) {
        this.basePrice = Money.requireRate(basePrice, "basePrice");
        this.peakPrice = Money.requireRate(peakPrice, "peakPrice");
    }

    @Override
    public BigDecimal priceFor(TimeSlot slot, double energyKwh) {
        long peakSeconds = 0;
        long offPeakSeconds = 0;
        for (LocalDateTime blockStart = slot.start(); blockStart.isBefore(slot.end()); blockStart = blockStart.plus(BLOCK)) {
            LocalDateTime blockEnd = earlier(blockStart.plus(BLOCK), slot.end());
            long seconds = Duration.between(blockStart, blockEnd).toSeconds();
            if (isPeak(blockStart.toLocalTime())) {
                peakSeconds += seconds;
            } else {
                offPeakSeconds += seconds;
            }
        }
        // energy * weighted average rate; rounding only once at the end avoids accumulating cent errors.
        BigDecimal weightedRate = peakPrice.multiply(BigDecimal.valueOf(peakSeconds))
                .add(basePrice.multiply(BigDecimal.valueOf(offPeakSeconds)));
        return BigDecimal.valueOf(energyKwh)
                .multiply(weightedRate)
                .divide(BigDecimal.valueOf(peakSeconds + offPeakSeconds), Money.SCALE, Money.ROUNDING);
    }

    private static boolean isPeak(LocalTime time) {
        return !time.isBefore(PEAK_START) && time.isBefore(PEAK_END);
    }

    private static LocalDateTime earlier(LocalDateTime first, LocalDateTime second) {
        return first.isBefore(second) ? first : second;
    }

    @Override
    public String describe() {
        return "time-of-use " + basePrice + " " + Money.CURRENCY + "/kWh, peak " + PEAK_START + "-" + PEAK_END
                + " " + peakPrice + " " + Money.CURRENCY + "/kWh";
    }
}
