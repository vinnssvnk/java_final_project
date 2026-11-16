package com.ivanna.evcharging.domain.model.reservation;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeSlotTest {

    private static final LocalDateTime TEN = LocalDateTime.of(2026, 12, 14, 10, 0);

    @Test
    void shouldDetectOverlap() {
        TimeSlot first = TimeSlot.of(TEN, Duration.ofHours(2));
        TimeSlot second = TimeSlot.of(TEN.plusHours(1), Duration.ofHours(2));

        assertTrue(first.overlaps(second));
        assertTrue(second.overlaps(first));
    }

    @Test
    void shouldDetectSlotContainedInAnother() {
        TimeSlot outer = TimeSlot.of(TEN, Duration.ofHours(4));
        TimeSlot inner = TimeSlot.of(TEN.plusHours(1), Duration.ofMinutes(30));

        assertTrue(outer.overlaps(inner));
        assertTrue(inner.overlaps(outer));
    }

    @Test
    void shouldNotTreatAdjacentSlotsAsOverlapping() {
        TimeSlot first = TimeSlot.of(TEN, Duration.ofHours(1));
        TimeSlot second = TimeSlot.of(TEN.plusHours(1), Duration.ofHours(1));

        assertFalse(first.overlaps(second));
        assertFalse(second.overlaps(first));
    }

    @Test
    void shouldRejectStartEqualToEnd() {
        assertThrows(IllegalArgumentException.class, () -> new TimeSlot(TEN, TEN));
    }

    @Test
    void shouldRejectStartAfterEnd() {
        assertThrows(IllegalArgumentException.class, () -> new TimeSlot(TEN, TEN.minusMinutes(1)));
    }

    @Test
    void shouldMeasureOverlapWithAnotherSlot() {
        TimeSlot slot = TimeSlot.of(TEN, Duration.ofHours(2));
        TimeSlot later = TimeSlot.of(TEN.plusMinutes(90), Duration.ofHours(2));

        assertEquals(Duration.ofMinutes(30), slot.overlapWith(later));
        assertEquals(Duration.ZERO, slot.overlapWith(TimeSlot.of(TEN.plusHours(2), Duration.ofHours(1))));
    }
}
