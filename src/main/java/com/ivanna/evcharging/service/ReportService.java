package com.ivanna.evcharging.service;

import com.ivanna.evcharging.domain.model.charger.Charger;
import com.ivanna.evcharging.domain.model.reservation.Reservation;
import com.ivanna.evcharging.domain.model.reservation.TimeSlot;
import com.ivanna.evcharging.domain.model.station.Station;
import com.ivanna.evcharging.domain.model.vehicle.Vehicle;
import com.ivanna.evcharging.domain.pricing.Money;
import com.ivanna.evcharging.domain.repository.Repository;
import com.ivanna.evcharging.domain.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Read-only statistics over all reservations. */
public class ReportService {

    private static final double MINUTES_PER_DAY = 24 * 60;
    // Cancelled sessions never used the charger and delivered no energy.
    private static final Predicate<Reservation> NOT_CANCELLED = reservation -> !reservation.isCancelled();

    private final Repository<Station, String> stations;
    private final Repository<Charger, String> chargers;
    private final Repository<Vehicle, String> vehicles;
    private final ReservationRepository reservations;

    public record ChargerUtilization(Charger charger, double percent) {
    }

    public record StationRevenue(Station station, BigDecimal revenue) {
    }

    public record VehicleEnergy(Vehicle vehicle, double energyKwh) {
    }

    public ReportService(Repository<Station, String> stations, Repository<Charger, String> chargers,
                         Repository<Vehicle, String> vehicles, ReservationRepository reservations) {
        this.stations = stations;
        this.chargers = chargers;
        this.vehicles = vehicles;
        this.reservations = reservations;
    }

    /** Share of the given day each charger is booked (active or completed sessions). */
    public List<ChargerUtilization> utilization(LocalDate date) {
        TimeSlot day = new TimeSlot(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        return chargers.findAll().stream()
                .map(charger -> new ChargerUtilization(charger, bookedMinutes(charger, day) * 100 / MINUTES_PER_DAY))
                .toList();
    }

    private long bookedMinutes(Charger charger, TimeSlot day) {
        return reservations.findByCharger(charger.getId()).stream()
                .filter(NOT_CANCELLED)
                .mapToLong(reservation -> reservation.getSlot().overlapWith(day).toMinutes())
                .sum();
    }

    /** Prices of sessions that went ahead plus fees of cancelled ones, for every station. */
    public List<StationRevenue> revenueByStation() {
        Map<String, BigDecimal> revenueByStationId = reservations.findAll().stream()
                .collect(Collectors.groupingBy(this::stationIdOf,
                        Collectors.reducing(Money.ZERO, Reservation::revenue, BigDecimal::add)));
        return stations.findAll().stream()
                .map(station -> new StationRevenue(station, revenueByStationId.getOrDefault(station.id(), Money.ZERO)))
                .toList();
    }

    public List<VehicleEnergy> topVehiclesByEnergy(int limit) {
        Map<String, Double> energyByVehicleId = reservations.findAll().stream()
                .filter(NOT_CANCELLED)
                .collect(Collectors.groupingBy(Reservation::getVehicleId,
                        Collectors.summingDouble(Reservation::getEnergyKwh)));
        return energyByVehicleId.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(entry -> new VehicleEnergy(requireVehicle(entry.getKey()), entry.getValue()))
                .toList();
    }

    private String stationIdOf(Reservation reservation) {
        return chargers.findById(reservation.getChargerId())
                .map(Charger::getStationId)
                .orElseThrow(() -> new IllegalStateException("Unknown charger " + reservation.getChargerId()));
    }

    private Vehicle requireVehicle(String vehicleId) {
        return vehicles.findById(vehicleId)
                .orElseThrow(() -> new IllegalStateException("Unknown vehicle " + vehicleId));
    }
}
