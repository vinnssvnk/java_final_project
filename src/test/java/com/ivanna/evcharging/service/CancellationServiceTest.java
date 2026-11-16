package com.ivanna.evcharging.service;

import com.ivanna.evcharging.domain.model.reservation.Reservation;
import com.ivanna.evcharging.domain.model.reservation.ReservationStatus;
import com.ivanna.evcharging.exception.CancellationNotAllowedException;
import com.ivanna.evcharging.exception.ReservationNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CancellationServiceTest {

    private static final LocalDateTime START = TestNetwork.NOW.plusHours(4);

    private final TestNetwork network = new TestNetwork();
    // Priced at 12.00 EUR (30 kWh * 0.40), so the late fee is 2.40 EUR.
    private final Reservation reservation = network.reservationService().book("V1", "AC1", START, 20, 80);

    private Reservation cancelAt(LocalDateTime now) {
        return network.cancellationServiceAt(now).cancel(reservation.getId());
    }

    @Test
    void shouldCancelForFreeWellBeforeStart() {
        Reservation cancelled = cancelAt(START.minusHours(3));

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(new BigDecimal("0.00"), cancelled.getCancellationFee());
        assertTrue(network.charger("AC1").getSchedule().isFree(reservation.getSlot()));
    }

    @Test
    void shouldCancelForFreeExactlySixtyMinutesBeforeStart() {
        assertEquals(new BigDecimal("0.00"), cancelAt(START.minusMinutes(60)).getCancellationFee());
    }

    @Test
    void shouldChargeTwentyPercentFiftyNineMinutesBeforeStart() {
        assertEquals(new BigDecimal("2.40"), cancelAt(START.minusMinutes(59)).getCancellationFee());
    }

    @Test
    void shouldPreviewFeeWithoutCancelling() {
        BigDecimal fee = network.cancellationServiceAt(START.minusMinutes(10)).previewFee(reservation.getId());

        assertEquals(new BigDecimal("2.40"), fee);
        assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
    }

    @Test
    void shouldRejectCancellationAfterStart() {
        assertThrows(CancellationNotAllowedException.class, () -> cancelAt(START.plusMinutes(5)));
    }

    @Test
    void shouldRejectCancellationExactlyAtStart() {
        assertThrows(CancellationNotAllowedException.class, () -> cancelAt(START));
    }

    @Test
    void shouldRejectCancellingCancelledReservation() {
        cancelAt(START.minusHours(2));

        assertThrows(CancellationNotAllowedException.class, () -> cancelAt(START.minusHours(1)));
    }

    @Test
    void shouldRejectCancellingCompletedReservation() {
        network.reservationService().complete(reservation.getId());

        assertThrows(CancellationNotAllowedException.class, () -> cancelAt(START.minusHours(2)));
    }

    @Test
    void shouldRejectUnknownReservation() {
        assertThrows(ReservationNotFoundException.class,
                () -> network.cancellationServiceAt(TestNetwork.NOW).cancel("R999"));
    }
}
