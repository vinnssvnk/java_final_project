package com.ivanna.evcharging.exception;

public class ReservationNotFoundException extends EvChargingException {

    public ReservationNotFoundException(String reservationId) {
        super("Reservation not found: " + reservationId);
    }
}
