package com.ivanna.evcharging.service;

import com.ivanna.evcharging.domain.model.charger.Charger;
import com.ivanna.evcharging.domain.model.reservation.Offer;
import com.ivanna.evcharging.domain.model.reservation.Reservation;
import com.ivanna.evcharging.domain.model.reservation.ReservationStatus;
import com.ivanna.evcharging.domain.model.reservation.TimeSlot;
import com.ivanna.evcharging.domain.model.station.Station;
import com.ivanna.evcharging.domain.model.vehicle.Vehicle;
import com.ivanna.evcharging.domain.repository.Repository;
import com.ivanna.evcharging.domain.repository.ReservationRepository;
import com.ivanna.evcharging.domain.selection.ChargerSelectionStrategy;
import com.ivanna.evcharging.exception.ReservationNotFoundException;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/** Use cases around planning and booking charging sessions. */
public class ReservationService {

    private final Repository<Station, String> stations;
    private final Repository<Charger, String> chargers;
    private final Repository<Vehicle, String> vehicles;
    private final ReservationRepository reservations;
    private final Clock clock;

    public ReservationService(Repository<Station, String> stations, Repository<Charger, String> chargers,
                              Repository<Vehicle, String> vehicles, ReservationRepository reservations, Clock clock) {
        this.stations = stations;
        this.chargers = chargers;
        this.vehicles = vehicles;
        this.reservations = reservations;
        this.clock = clock;
    }

    public SessionEstimate estimate(String vehicleId, String chargerId, int fromPct, int toPct, LocalDateTime start) {
        return estimateFor(requireVehicle(vehicleId), requireCharger(chargerId), fromPct, toPct, start);
    }

    /** Books the session; throws SlotConflictException if the charger is busy, leaving nothing saved. */
    public Reservation book(String vehicleId, String chargerId, LocalDateTime start, int fromPct, int toPct) {
        requireFuture(start);
        Vehicle vehicle = requireVehicle(vehicleId);
        Charger charger = requireCharger(chargerId);
        SessionEstimate estimate = estimateFor(vehicle, charger, fromPct, toPct, start);
        Reservation reservation = new Reservation(nextReservationId(), vehicleId, chargerId,
                TimeSlot.of(start, estimate.duration()), fromPct, toPct, estimate.energyKwh(), estimate.price(),
                ReservationStatus.ACTIVE);
        charger.getSchedule().add(reservation);
        return reservations.save(reservation);
    }

    /** Earliest slot on this charger, at or after the desired start, that is long enough for the session. */
    public Optional<TimeSlot> suggestNextSlot(String vehicleId, String chargerId, LocalDateTime desiredStart,
                                              int fromPct, int toPct) {
        requireFuture(desiredStart);
        Charger charger = requireCharger(chargerId);
        Duration duration = charger.estimateDuration(requireVehicle(vehicleId), fromPct, toPct);
        return charger.getSchedule().nextFreeSlot(desiredStart, duration);
    }

    /** Collects one offer per compatible charger and lets the strategy decide which one is best. */
    public Optional<Offer> findBestCharger(String vehicleId, LocalDateTime desiredStart, int fromPct, int toPct,
                                           ChargerSelectionStrategy strategy) {
        requireFuture(desiredStart);
        Vehicle vehicle = requireVehicle(vehicleId);
        double energyKwh = vehicle.energyNeededKwh(fromPct, toPct);
        List<Offer> offers = chargers.findAll().stream()
                .filter(charger -> charger.isCompatibleWith(vehicle))
                .flatMap(charger -> offerFrom(charger, vehicle, desiredStart, fromPct, toPct, energyKwh).stream())
                .toList();
        return strategy.select(offers);
    }

    private Optional<Offer> offerFrom(Charger charger, Vehicle vehicle, LocalDateTime desiredStart,
                                      int fromPct, int toPct, double energyKwh) {
        Duration duration = charger.estimateDuration(vehicle, fromPct, toPct);
        return charger.getSchedule().nextFreeSlot(desiredStart, duration)
                .map(slot -> new Offer(charger, slot, stationOf(charger).priceFor(slot, energyKwh)));
    }

    /** Marks the session as done and releases the rest of its slot on the charger. */
    public Reservation complete(String reservationId) {
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
        reservation.complete();
        requireCharger(reservation.getChargerId()).getSchedule().remove(reservation);
        return reservations.save(reservation);
    }

    /** Active reservations of a charger in chronological order. */
    public List<Reservation> scheduleOf(String chargerId) {
        return requireCharger(chargerId).getSchedule().reservations();
    }

    private SessionEstimate estimateFor(Vehicle vehicle, Charger charger, int fromPct, int toPct, LocalDateTime start) {
        Duration duration = charger.estimateDuration(vehicle, fromPct, toPct);
        double energyKwh = vehicle.energyNeededKwh(fromPct, toPct);
        TimeSlot slot = TimeSlot.of(start, duration);
        return new SessionEstimate(duration, energyKwh, stationOf(charger).priceFor(slot, energyKwh));
    }

    private void requireFuture(LocalDateTime start) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!start.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Start " + start + " must be in the future (now is " + now.truncatedTo(ChronoUnit.MINUTES) + ")");
        }
    }

    private Vehicle requireVehicle(String vehicleId) {
        return vehicles.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown vehicle: " + vehicleId));
    }

    private Charger requireCharger(String chargerId) {
        return chargers.findById(chargerId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown charger: " + chargerId));
    }

    private Station stationOf(Charger charger) {
        // Data loading rejects chargers of unknown stations, so a miss here is a programming error.
        return stations.findById(charger.getStationId())
                .orElseThrow(() -> new IllegalStateException("Charger " + charger.getId() + " has no station"));
    }

    private String nextReservationId() {
        int number = reservations.findAll().size() + 1;
        String id = "R%03d".formatted(number);
        while (reservations.findById(id).isPresent()) {
            id = "R%03d".formatted(++number);
        }
        return id;
    }
}
