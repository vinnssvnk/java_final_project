package com.ivanna.evcharging.infrastructure.io;

import com.ivanna.evcharging.domain.model.charger.Charger;
import com.ivanna.evcharging.domain.model.reservation.Reservation;
import com.ivanna.evcharging.domain.model.station.Station;
import com.ivanna.evcharging.domain.model.vehicle.Vehicle;
import com.ivanna.evcharging.domain.repository.Repository;
import com.ivanna.evcharging.domain.repository.ReservationRepository;
import com.ivanna.evcharging.exception.DataLoadException;
import com.ivanna.evcharging.exception.SlotConflictException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Fills the repositories from CSV files on the classpath. Files are loaded in dependency order
 * (stations, chargers, vehicles, reservations) so every reference can be checked against data already loaded.
 */
public class DataInitializer {

    private static final String RESERVATIONS_FILE = "reservations.csv";

    private final String resourceDirectory;
    private final Repository<Station, String> stations;
    private final Repository<Charger, String> chargers;
    private final Repository<Vehicle, String> vehicles;
    private final ReservationRepository reservations;

    /** What was loaded, plus one message per skipped row. */
    public record Summary(int stations, int chargers, int vehicles, int reservations, List<String> errors) {

        public Summary {
            errors = List.copyOf(errors);
        }
    }

    /** The resource directory is a classpath folder such as /data. */
    public DataInitializer(String resourceDirectory, Repository<Station, String> stations,
                           Repository<Charger, String> chargers, Repository<Vehicle, String> vehicles,
                           ReservationRepository reservations) {
        this.resourceDirectory = resourceDirectory;
        this.stations = stations;
        this.chargers = chargers;
        this.vehicles = vehicles;
        this.reservations = reservations;
    }

    public Summary initialize() throws DataLoadException {
        List<String> errors = new ArrayList<>();
        load("stations.csv", new StationMapper(), errors).forEach(stations::save);
        load("chargers.csv", new ChargerMapper(id -> stations.findById(id).isPresent()), errors)
                .forEach(chargers::save);
        load("vehicles.csv", new VehicleMapper(), errors).forEach(vehicles::save);
        load(RESERVATIONS_FILE, new ReservationMapper(vehicles::findById, id -> chargers.findById(id).isPresent()), errors)
                .forEach(reservation -> register(reservation, errors));
        return new Summary(stations.findAll().size(), chargers.findAll().size(), vehicles.findAll().size(),
                reservations.findAll().size(), errors);
    }

    private <T> List<T> load(String fileName, Function<String[], T> mapper, List<String> errors)
            throws DataLoadException {
        String path = resourceDirectory + "/" + fileName;
        InputStream stream = DataInitializer.class.getResourceAsStream(path);
        if (stream == null) {
            throw new DataLoadException("Missing data file on classpath: " + path);
        }
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            LoadResult<T> result = new CsvLoader<T>().load(reader, mapper);
            result.errors().forEach(error -> errors.add(fileName + " " + error));
            return result.items();
        } catch (IOException e) {
            throw new DataLoadException("Could not read " + path + ": " + e.getMessage(), e);
        }
    }

    /** Active reservations must claim their slot; a clash means the file is inconsistent, so the row is rejected. */
    private void register(Reservation reservation, List<String> errors) {
        if (reservation.isActive()) {
            Charger charger = chargers.findById(reservation.getChargerId()).orElseThrow();
            try {
                charger.getSchedule().add(reservation);
            } catch (SlotConflictException e) {
                errors.add(RESERVATIONS_FILE + " " + reservation.getId() + ": " + e.getMessage());
                return;
            }
        }
        reservations.save(reservation);
    }
}
