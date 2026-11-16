package com.ivanna.evcharging.domain.repository;

import com.ivanna.evcharging.domain.model.reservation.Reservation;

import java.util.List;

public interface ReservationRepository extends Repository<Reservation, String> {

    List<Reservation> findByCharger(String chargerId);

    List<Reservation> findByVehicle(String vehicleId);
}
