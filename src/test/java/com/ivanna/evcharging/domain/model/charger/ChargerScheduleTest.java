package com.ivanna.evcharging.domain.model.charger;

import com.ivanna.evcharging.domain.model.reservation.Reservation;
import com.ivanna.evcharging.domain.model.reservation.ReservationStatus;
import com.ivanna.evcharging.domain.model.reservation.TimeSlot;
import com.ivanna.evcharging.exception.SlotConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChargerScheduleTest {

    private static final LocalDateTime TEN = LocalDateTime.of(2026, 12, 14, 10, 0);

    private final ChargerSchedule schedule = new ChargerSchedule("C1");

    private static Reservation reservation(String id, LocalDateTime start, Duration duration) {
        return new Reservation(id, "V1", "C1", TimeSlot.of(start, duration), 20, 80, 30,
                new BigDecimal("10.00"), ReservationStatus.ACTIVE);
    }

    @Test
    void shouldBookFreeSlot() {
        Reservation reservation = reservation("R1", TEN, Duration.ofHours(1));

        schedule.add(reservation);

        assertEquals(List.of(reservation), schedule.reservations());
    }

    @Test
    void shouldThrowWhenSlotOverlapsExistingReservation() {
        schedule.add(reservation("R1", TEN, Duration.ofHours(2)));

        assertThrows(SlotConflictException.class,
                () -> schedule.add(reservation("R2", TEN.plusHours(1), Duration.ofHours(1))));
    }

    @Test
    void shouldThrowWhenNewSlotStartsBeforeAndEndsInsideExisting() {
        schedule.add(reservation("R1", TEN, Duration.ofHours(1)));

        assertThrows(SlotConflictException.class,
                () -> schedule.add(reservation("R2", TEN.minusMinutes(30), Duration.ofHours(1))));
    }

    @Test
    void shouldAllowAdjacentBookings() {
        schedule.add(reservation("R1", TEN, Duration.ofHours(1)));

        schedule.add(reservation("R2", TEN.plusHours(1), Duration.ofHours(1)));
        schedule.add(reservation("R3", TEN.minusHours(1), Duration.ofHours(1)));

        assertEquals(3, schedule.reservations().size());
        assertEquals("R3", schedule.reservations().get(0).getId());
    }

    @Test
    void shouldFreeSlotWhenReservationRemoved() {
        Reservation reservation = reservation("R1", TEN, Duration.ofHours(1));
        schedule.add(reservation);

        schedule.remove(reservation);

        assertTrue(schedule.isFree(reservation.getSlot()));
    }

    @Test
    void shouldReturnRequestedStartWhenItIsFree() {
        Optional<TimeSlot> slot = schedule.nextFreeSlot(TEN, Duration.ofMinutes(45));

        assertEquals(Optional.of(TimeSlot.of(TEN, Duration.ofMinutes(45))), slot);
    }

    @Test
    void shouldFindFirstGapThatIsLongEnough() {
        schedule.add(reservation("R1", TEN, Duration.ofHours(1)));                       // 10:00-11:00
        schedule.add(reservation("R2", TEN.plusMinutes(90), Duration.ofHours(1)));       // 11:30-12:30, gap of 30 min
        schedule.add(reservation("R3", TEN.plusMinutes(210), Duration.ofHours(1)));      // 13:30-14:30, gap of 60 min

        Optional<TimeSlot> slot = schedule.nextFreeSlot(TEN.plusMinutes(15), Duration.ofMinutes(45));

        assertEquals(Optional.of(TimeSlot.of(TEN.plusMinutes(150), Duration.ofMinutes(45))), slot);
    }

    @Test
    void shouldReturnEmptyWhenFullyBookedWithinSearchHorizon() {
        schedule.add(reservation("R1", TEN, ChargerSchedule.SEARCH_HORIZON.minusMinutes(30)));

        assertFalse(schedule.nextFreeSlot(TEN, Duration.ofHours(1)).isPresent());
    }

    @Test
    void shouldRejectReservationOfAnotherCharger() {
        Reservation other = new Reservation("R1", "V1", "C2", TimeSlot.of(TEN, Duration.ofHours(1)), 20, 80, 30,
                BigDecimal.TEN, ReservationStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class, () -> schedule.add(other));
    }
}
