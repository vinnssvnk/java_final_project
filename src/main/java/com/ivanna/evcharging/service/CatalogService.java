package com.ivanna.evcharging.service;

import com.ivanna.evcharging.domain.model.charger.Charger;
import com.ivanna.evcharging.domain.model.reservation.Reservation;
import com.ivanna.evcharging.domain.model.station.Station;
import com.ivanna.evcharging.domain.model.vehicle.Vehicle;
import com.ivanna.evcharging.domain.repository.Repository;
import com.ivanna.evcharging.domain.repository.ReservationRepository;

import java.util.Comparator;
import java.util.List;

/** Read-only listings for the UI, so the UI never has to reach into repositories. */
public class CatalogService {

    private final Repository<Station, String> stations;
    private final Repository<Charger, String> chargers;
    private final Repository<Vehicle, String> vehicles;
    private final ReservationRepository reservations;

    public CatalogService(Repository<Station, String> stations, Repository<Charger, String> chargers,
                          Repository<Vehicle, String> vehicles, ReservationRepository reservations) {
        this.stations = stations;
        this.chargers = chargers;
        this.vehicles = vehicles;
        this.reservations = reservations;
    }

    public List<Station> stations() {
        return stations.findAll();
    }

    public List<Charger> chargersAt(String stationId) {
        return chargers.findAll().stream()
                .filter(charger -> charger.getStationId().equals(stationId))
                .toList();
    }

    public List<Vehicle> vehicles() {
        return vehicles.findAll();
    }

    public List<Reservation> activeReservations() {
        return reservations.findAll().stream()
                .filter(Reservation::isActive)
                .sorted(Comparator.comparing(reservation -> reservation.getSlot().start()))
                .toList();
    }
}
