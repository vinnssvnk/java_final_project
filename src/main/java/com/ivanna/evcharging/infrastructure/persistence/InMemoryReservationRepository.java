package com.ivanna.evcharging.infrastructure.persistence;

import com.ivanna.evcharging.domain.model.reservation.Reservation;
import com.ivanna.evcharging.domain.repository.ReservationRepository;

import java.util.List;
import java.util.function.Predicate;

public class InMemoryReservationRepository extends InMemoryRepository<Reservation, String>
        implements ReservationRepository {

    public InMemoryReservationRepository() {
        super(Reservation::getId);
    }

    @Override
    public List<Reservation> findByCharger(String chargerId) {
        return findWhere(reservation -> reservation.getChargerId().equals(chargerId));
    }

    @Override
    public List<Reservation> findByVehicle(String vehicleId) {
        return findWhere(reservation -> reservation.getVehicleId().equals(vehicleId));
    }

    private List<Reservation> findWhere(Predicate<Reservation> condition) {
        return findAll().stream().filter(condition).toList();
    }
}
