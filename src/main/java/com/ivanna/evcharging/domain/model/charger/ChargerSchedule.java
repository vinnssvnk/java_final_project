package com.ivanna.evcharging.domain.model.charger;

import com.ivanna.evcharging.domain.model.Validation;
import com.ivanna.evcharging.domain.model.reservation.Reservation;
import com.ivanna.evcharging.domain.model.reservation.TimeSlot;
import com.ivanna.evcharging.exception.SlotConflictException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The active reservations of one charger, ordered by start time.
 * Because active slots never overlap, a new slot can only clash with the reservation starting
 * at or just before it, or the first one starting after it
 */
public class ChargerSchedule {

    static final Duration SEARCH_HORIZON = Duration.ofDays(7);

    private final String chargerId;
    private final TreeMap<LocalDateTime, Reservation> reservationsByStart = new TreeMap<>();

    public ChargerSchedule(String chargerId) {
        this.chargerId = Validation.requireText(chargerId, "chargerId");
    }

    public void add(Reservation reservation) {
        if (!reservation.getChargerId().equals(chargerId)) {
            throw new IllegalArgumentException("Reservation " + reservation.getId() + " belongs to charger "
                    + reservation.getChargerId() + ", not " + chargerId);
        }
        if (!reservation.isActive()) {
            throw new IllegalArgumentException("Only active reservations occupy a schedule: " + reservation.getId());
        }
        findConflict(reservation.getSlot()).ifPresent(existing -> {
            throw new SlotConflictException("Charger " + chargerId + " is already booked by " + existing.getId()
                    + " from " + existing.getSlot().start() + " to " + existing.getSlot().end());
        });
        reservationsByStart.put(reservation.getSlot().start(), reservation);
    }

    public void remove(Reservation reservation) {
        reservationsByStart.remove(reservation.getSlot().start(), reservation);
    }

    public boolean isFree(TimeSlot slot) {
        return findConflict(slot).isEmpty();
    }

    public Optional<Reservation> findConflict(TimeSlot slot) {
        Map.Entry<LocalDateTime, Reservation> before = reservationsByStart.floorEntry(slot.start());
        if (before != null && before.getValue().getSlot().overlaps(slot)) {
            return Optional.of(before.getValue());
        }
        Map.Entry<LocalDateTime, Reservation> after = reservationsByStart.ceilingEntry(slot.start());
        if (after != null && after.getValue().getSlot().overlaps(slot)) {
            return Optional.of(after.getValue());
        }
        return Optional.empty();
    }

    /**
     * Earliest slot of the given length starting at or after from that fits between existing
     * reservations and ends within SEARCH_HORIZON; empty if there is none.
     */
    public Optional<TimeSlot> nextFreeSlot(LocalDateTime from, Duration duration) {
        Objects.requireNonNull(from, "from must not be null");
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Duration must be positive but was " + duration);
        }
        LocalDateTime horizon = from.plus(SEARCH_HORIZON);
        LocalDateTime candidate = from;

        Map.Entry<LocalDateTime, Reservation> running = reservationsByStart.floorEntry(from);
        if (running != null && running.getValue().getSlot().end().isAfter(candidate)) {
            candidate = running.getValue().getSlot().end();
        }
        // Reservations are visited in start order, so every gap is examined exactly once.
        for (Reservation next : reservationsByStart.tailMap(candidate, true).values()) {
            boolean gapIsLongEnough = !next.getSlot().start().isBefore(candidate.plus(duration));
            if (gapIsLongEnough || candidate.isAfter(horizon)) {
                break;
            }
            candidate = next.getSlot().end();
        }
        TimeSlot slot = TimeSlot.of(candidate, duration);
        return slot.end().isAfter(horizon) ? Optional.empty() : Optional.of(slot);
    }

    /** Active reservations in chronological order. */
    public List<Reservation> reservations() {
        return List.copyOf(reservationsByStart.values());
    }

    public String getChargerId() {
        return chargerId;
    }
}
