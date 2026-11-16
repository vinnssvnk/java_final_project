package com.ivanna.evcharging.domain.pricing;

import com.ivanna.evcharging.domain.model.reservation.Reservation;
import com.ivanna.evcharging.exception.CancellationNotAllowedException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Decides whether a reservation may be cancelled and what it costs.
 * Late cancellations are charged because the slot can hardly be resold at short notice.
 */
public class CancellationPolicy {

    public static final Duration FREE_CANCELLATION_NOTICE = Duration.ofMinutes(60);
    private static final BigDecimal LATE_FEE_RATE = new BigDecimal("0.20");

    public BigDecimal feeFor(Reservation reservation, LocalDateTime now) {
        if (!reservation.isActive()) {
            throw new CancellationNotAllowedException(
                    "Reservation " + reservation.getId() + " is " + reservation.getStatus() + " and cannot be cancelled");
        }
        LocalDateTime start = reservation.getSlot().start();
        if (!now.isBefore(start)) {
            throw new CancellationNotAllowedException(
                    "Reservation " + reservation.getId() + " already started at " + start);
        }
        Duration notice = Duration.between(now, start);
        if (notice.compareTo(FREE_CANCELLATION_NOTICE) >= 0) {
            return Money.ZERO;
        }
        return Money.round(reservation.getPrice().multiply(LATE_FEE_RATE));
    }
}
