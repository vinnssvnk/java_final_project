package com.ivanna.evcharging.domain.model.reservation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A half-open interval [start, end). Half-open means a slot ending at 10:00 and one starting
 * at 10:00 do not overlap, so back-to-back bookings are possible.
 */
public record TimeSlot(LocalDateTime start, LocalDateTime end) {

    public TimeSlot {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Slot start " + start + " must be before end " + end);
        }
    }

    public static TimeSlot of(LocalDateTime start, Duration duration) {
        return new TimeSlot(start, start.plus(duration));
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    public boolean overlaps(TimeSlot other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    /** Length of the part both slots have in common, or zero if they do not overlap. */
    public Duration overlapWith(TimeSlot other) {
        LocalDateTime laterStart = start.isAfter(other.start) ? start : other.start;
        LocalDateTime earlierEnd = end.isBefore(other.end) ? end : other.end;
        return laterStart.isBefore(earlierEnd) ? Duration.between(laterStart, earlierEnd) : Duration.ZERO;
    }
}
