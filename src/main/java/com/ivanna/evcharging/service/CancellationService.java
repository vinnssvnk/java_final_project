package com.ivanna.evcharging.service;

import com.ivanna.evcharging.domain.model.charger.Charger;
import com.ivanna.evcharging.domain.model.reservation.Reservation;
import com.ivanna.evcharging.domain.pricing.CancellationPolicy;
import com.ivanna.evcharging.domain.repository.Repository;
import com.ivanna.evcharging.domain.repository.ReservationRepository;
import com.ivanna.evcharging.exception.ReservationNotFoundException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

public class CancellationService {

    private final ReservationRepository reservations;
    private final Repository<Charger, String> chargers;
    private final CancellationPolicy policy;
    private final Clock clock;

    public CancellationService(ReservationRepository reservations, Repository<Charger, String> chargers,
                               CancellationPolicy policy, Clock clock) {
        this.reservations = reservations;
        this.chargers = chargers;
        this.policy = policy;
        this.clock = clock;
    }

    /** The fee that cancelling now would cost, so the user can decide before committing. */
    public BigDecimal previewFee(String reservationId) {
        return policy.feeFor(requireReservation(reservationId), LocalDateTime.now(clock));
    }

    /** Cancels the reservation, stores the fee on it and frees its slot. */
    public Reservation cancel(String reservationId) {
        Reservation reservation = requireReservation(reservationId);
        BigDecimal fee = policy.feeFor(reservation, LocalDateTime.now(clock));
        reservation.cancel(fee);
        chargers.findById(reservation.getChargerId())
                .orElseThrow(() -> new IllegalStateException("Unknown charger " + reservation.getChargerId()))
                .getSchedule()
                .remove(reservation);
        return reservations.save(reservation);
    }

    private Reservation requireReservation(String reservationId) {
        return reservations.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }
}
