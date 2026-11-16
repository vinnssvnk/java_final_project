package com.ivanna.evcharging.domain.model.reservation;

public enum ReservationStatus {
    ACTIVE,
    CANCELLED,
    COMPLETED;

    public boolean canTransitionTo(ReservationStatus target) {
        // Cancelled and completed are final states; only an active reservation can still change.
        return this == ACTIVE && target != ACTIVE;
    }
}
