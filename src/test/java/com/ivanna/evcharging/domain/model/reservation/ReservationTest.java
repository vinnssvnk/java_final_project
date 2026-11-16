package com.ivanna.evcharging.domain.model.reservation;

import com.ivanna.evcharging.exception.IllegalStatusTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationTest {

    private static Reservation activeReservation() {
        TimeSlot slot = TimeSlot.of(LocalDateTime.of(2026, 12, 14, 10, 0), Duration.ofHours(1));
        return new Reservation("R1", "V1", "C1", slot, 20, 80, 30, new BigDecimal("12.00"), ReservationStatus.ACTIVE);
    }

    @Test
    void shouldStoreFeeWhenCancelled() {
        Reservation reservation = activeReservation();

        reservation.cancel(new BigDecimal("2.4"));

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(new BigDecimal("2.40"), reservation.getCancellationFee());
        assertEquals(new BigDecimal("2.40"), reservation.revenue());
    }

    @Test
    void shouldEarnFullPriceWhenCompleted() {
        Reservation reservation = activeReservation();

        reservation.complete();

        assertEquals(ReservationStatus.COMPLETED, reservation.getStatus());
        assertEquals(new BigDecimal("12.00"), reservation.revenue());
    }

    @Test
    void shouldRejectCompletingCancelledReservation() {
        Reservation reservation = activeReservation();
        reservation.cancel(BigDecimal.ZERO);

        assertThrows(IllegalStatusTransitionException.class, reservation::complete);
    }

    @Test
    void shouldRejectCancellingCompletedReservation() {
        Reservation reservation = activeReservation();
        reservation.complete();

        assertThrows(IllegalStatusTransitionException.class, () -> reservation.cancel(BigDecimal.ZERO));
    }
}
