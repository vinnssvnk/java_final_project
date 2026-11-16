package com.ivanna.evcharging;

import com.ivanna.evcharging.domain.model.charger.Charger;
import com.ivanna.evcharging.domain.model.station.Station;
import com.ivanna.evcharging.domain.model.vehicle.Vehicle;
import com.ivanna.evcharging.domain.pricing.CancellationPolicy;
import com.ivanna.evcharging.domain.repository.Repository;
import com.ivanna.evcharging.domain.repository.ReservationRepository;
import com.ivanna.evcharging.domain.selection.ChargerSelectionStrategy;
import com.ivanna.evcharging.domain.selection.CheapestStrategy;
import com.ivanna.evcharging.domain.selection.FastestFinishStrategy;
import com.ivanna.evcharging.exception.DataLoadException;
import com.ivanna.evcharging.infrastructure.io.DataInitializer;
import com.ivanna.evcharging.infrastructure.persistence.InMemoryRepository;
import com.ivanna.evcharging.infrastructure.persistence.InMemoryReservationRepository;
import com.ivanna.evcharging.service.CancellationService;
import com.ivanna.evcharging.service.CatalogService;
import com.ivanna.evcharging.service.ReportService;
import com.ivanna.evcharging.service.ReservationService;
import com.ivanna.evcharging.ui.ConsoleMenu;
import com.ivanna.evcharging.ui.InputReader;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/** Composition root: creates and connects all objects, then hands control to the console menu. */
public final class Application {

    private static final String DATA_DIRECTORY = "/data";
    private static final int EXIT_DATA_ERROR = 1;

    private Application() {
    }

    public static void main(String[] args) {
        Clock clock = Clock.systemDefaultZone();
        Repository<Station, String> stations = new InMemoryRepository<>(Station::id);
        Repository<Charger, String> chargers = new InMemoryRepository<>(Charger::getId);
        Repository<Vehicle, String> vehicles = new InMemoryRepository<>(Vehicle::id);
        ReservationRepository reservations = new InMemoryReservationRepository();

        DataInitializer.Summary summary;
        try {
            summary = new DataInitializer(DATA_DIRECTORY, stations, chargers, vehicles, reservations).initialize();
        } catch (DataLoadException e) {
            System.err.println("Cannot start EV Charging Scheduler: " + e.getMessage());
            System.exit(EXIT_DATA_ERROR);
            return;
        }
        printSummary(summary);

        Map<String, ChargerSelectionStrategy> strategies = new LinkedHashMap<>();
        strategies.put("Fastest finish", new FastestFinishStrategy());
        strategies.put("Cheapest", new CheapestStrategy());

        ConsoleMenu menu = new ConsoleMenu(
                new InputReader(new Scanner(System.in), System.out),
                System.out,
                new CatalogService(stations, chargers, vehicles, reservations),
                new ReservationService(stations, chargers, vehicles, reservations, clock),
                new CancellationService(reservations, chargers, new CancellationPolicy(), clock),
                new ReportService(stations, chargers, vehicles, reservations),
                strategies);
        menu.run();
    }

    private static void printSummary(DataInitializer.Summary summary) {
        System.out.printf("Loaded %d stations, %d chargers, %d vehicles, %d reservations.%n",
                summary.stations(), summary.chargers(), summary.vehicles(), summary.reservations());
        if (!summary.errors().isEmpty()) {
            System.out.println("Skipped " + summary.errors().size() + " invalid line(s):");
            summary.errors().forEach(error -> System.out.println("  " + error));
        }
    }
}
