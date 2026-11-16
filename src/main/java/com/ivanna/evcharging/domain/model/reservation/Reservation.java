package com.ivanna.evcharging.domain.model.reservation;

import com.ivanna.evcharging.domain.model.Validation;
import com.ivanna.evcharging.domain.pricing.Money;
import com.ivanna.evcharging.exception.IllegalStatusTransitionException;

import java.math.BigDecimal;
import java.util.Objects;

import static com.ivanna.evcharging.domain.model.Validation.requireNonNegative;
import static com.ivanna.evcharging.domain.model.Validation.requireText;

/**
 * A booked charging session. Vehicle and charger are referenced by id so a reservation can be
 * stored and loaded independently of those objects.
 */
public class Reservation {

    private final String id;
    private final String vehicleId;
    private final String chargerId;
    private final TimeSlot slot;
    private final int fromPct;
    private final int targetPct;
    private final double energyKwh;
    private final BigDecimal price;
    private ReservationStatus status;
    private BigDecimal cancellationFee = Money.ZERO;

    public Reservation(String id, String vehicleId, String chargerId, TimeSlot slot, int fromPct, int targetPct,
                       double energyKwh, BigDecimal price, ReservationStatus status) {
        this.id = requireText(id, "id");
        this.vehicleId = requireText(vehicleId, "vehicleId");
        this.chargerId = requireText(chargerId, "chargerId");
        this.slot = Objects.requireNonNull(slot, "slot must not be null");
        Validation.requireValidChargeRange(fromPct, targetPct);
        this.fromPct = fromPct;
        this.targetPct = targetPct;
        this.energyKwh = requireNonNegative(energyKwh, "energyKwh");
        this.price = Money.round(requireNonNegativeAmount(price, "price"));
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public void cancel(BigDecimal fee) {
        requireNonNegativeAmount(fee, "fee");
        transitionTo(ReservationStatus.CANCELLED);
        cancellationFee = Money.round(fee);
    }

    public void complete() {
        transitionTo(ReservationStatus.COMPLETED);
    }

    private void transitionTo(ReservationStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStatusTransitionException(
                    "Reservation " + id + " cannot change from " + status + " to " + target);
        }
        status = target;
    }

    private static BigDecimal requireNonNegativeAmount(BigDecimal amount, String name) {
        Objects.requireNonNull(amount, name + " must not be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative but was " + amount);
        }
        return amount;
    }

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    public boolean isCancelled() {
        return status == ReservationStatus.CANCELLED;
    }

    /** A cancelled session still earns its cancellation fee; any other session earns its price. */
    public BigDecimal revenue() {
        return isCancelled() ? cancellationFee : price;
    }

    public String getId() {
        return id;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getChargerId() {
        return chargerId;
    }

    public TimeSlot getSlot() {
        return slot;
    }

    public int getFromPct() {
        return fromPct;
    }

    public int getTargetPct() {
        return targetPct;
    }

    public double getEnergyKwh() {
        return energyKwh;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public BigDecimal getCancellationFee() {
        return cancellationFee;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Reservation reservation && id.equals(reservation.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " " + status + " " + chargerId + " " + slot.start() + " -> " + slot.end();
    }
}
