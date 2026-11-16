package com.ivanna.evcharging.ui;

import com.ivanna.evcharging.domain.model.charger.Charger;
import com.ivanna.evcharging.domain.model.reservation.Offer;
import com.ivanna.evcharging.domain.model.reservation.Reservation;
import com.ivanna.evcharging.domain.model.reservation.TimeSlot;
import com.ivanna.evcharging.domain.model.station.Station;
import com.ivanna.evcharging.domain.model.vehicle.Vehicle;
import com.ivanna.evcharging.domain.pricing.Money;
import com.ivanna.evcharging.domain.selection.ChargerSelectionStrategy;
import com.ivanna.evcharging.exception.EvChargingException;
import com.ivanna.evcharging.exception.SlotConflictException;
import com.ivanna.evcharging.service.CancellationService;
import com.ivanna.evcharging.service.CatalogService;
import com.ivanna.evcharging.service.ReportService;
import com.ivanna.evcharging.service.ReportService.VehicleEnergy;
import com.ivanna.evcharging.service.ReservationService;
import com.ivanna.evcharging.service.SessionEstimate;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/** The interactive console. It only collects input, calls services and prints results. */
public class ConsoleMenu {

    private static final int TOP_VEHICLES = 3;

    private record MenuOption(String label, Runnable action) {
    }

    /** What the user wants to charge; the charger is asked separately because "find best charger" chooses it. */
    private record SessionRequest(String vehicleId, LocalDateTime start, int fromPct, int toPct) {
    }

    private final InputReader input;
    private final PrintStream out;
    private final CatalogService catalog;
    private final ReservationService reservationService;
    private final CancellationService cancellationService;
    private final ReportService reportService;
    private final List<Map.Entry<String, ChargerSelectionStrategy>> strategies;
    private final List<MenuOption> options;

    /** Strategies are keyed by display name, so adding a new one needs no change in this class. */
    public ConsoleMenu(InputReader input, PrintStream out, CatalogService catalog,
                       ReservationService reservationService, CancellationService cancellationService,
                       ReportService reportService, Map<String, ChargerSelectionStrategy> strategies) {
        this.input = input;
        this.out = out;
        this.catalog = catalog;
        this.reservationService = reservationService;
        this.cancellationService = cancellationService;
        this.reportService = reportService;
        this.strategies = List.copyOf(strategies.entrySet());
        this.options = List.of(
                new MenuOption("List stations & chargers", this::listStations),
                new MenuOption("List vehicles", this::listVehicles),
                new MenuOption("Estimate session", this::estimateSession),
                new MenuOption("Book a charger", this::bookCharger),
                new MenuOption("Find best charger", this::findBestCharger),
                new MenuOption("Cancel reservation", this::cancelReservation),
                new MenuOption("Complete reservation", this::completeReservation),
                new MenuOption("Show schedule of a charger", this::showSchedule),
                new MenuOption("Reports", this::showReports));
    }

    public void run() {
        try {
            int choice;
            do {
                printMenu();
                choice = input.readInt("Choose an option", 0, options.size());
                if (choice > 0) {
                    execute(options.get(choice - 1));
                }
            } while (choice != 0);
            out.println("Goodbye!");
        } catch (NoSuchElementException e) {
            out.println();
            out.println("Input closed. Goodbye!");
        }
    }

    private void printMenu() {
        out.println();
        out.println("=== EV Charging Scheduler ===");
        for (int i = 0; i < options.size(); i++) {
            out.println((i + 1) + "  " + options.get(i).label());
        }
        out.println("0  Exit");
    }

    /** One place turns rule violations into readable messages, so a mistake never ends the program. */
    private void execute(MenuOption option) {
        try {
            option.action().run();
        } catch (EvChargingException | IllegalArgumentException e) {
            out.println("! " + e.getMessage());
        }
    }

    private void listStations() {
        for (Station station : catalog.stations()) {
            out.println(station.id() + "  " + station.name() + ", " + station.city() + "  -  " + station.pricing().describe());
            for (Charger charger : catalog.chargersAt(station.id())) {
                out.println(format("    %-4s %-3s %-8s %6.1f kW", charger.getId(), charger.getType(),
                        charger.getConnector(), charger.getMaxPowerKw()));
            }
        }
    }

    private void listVehicles() {
        for (Vehicle vehicle : catalog.vehicles()) {
            List<String> connectors = vehicle.connectors().stream().map(Enum::name).sorted().toList();
            out.println(format("%-4s %-26s %5.1f kWh  AC %-8s DC %-8s %s", vehicle.id(), vehicle.model(),
                    vehicle.batteryKwh(), powerLimit(vehicle.maxAcKw()), powerLimit(vehicle.maxDcKw()),
                    String.join("|", connectors)));
        }
    }

    private void estimateSession() {
        String chargerId = input.readText("Charger id");
        SessionRequest request = readSessionRequest();
        SessionEstimate estimate = reservationService.estimate(request.vehicleId(), chargerId, request.fromPct(),
                request.toPct(), request.start());
        out.println(format("Duration %s, energy %.1f kWh, price %s", formatDuration(estimate.duration()),
                estimate.energyKwh(), formatMoney(estimate.price())));
    }

    private void bookCharger() {
        String chargerId = input.readText("Charger id");
        SessionRequest request = readSessionRequest();
        try {
            book(chargerId, request.start(), request);
        } catch (SlotConflictException e) {
            out.println("! " + e.getMessage());
            offerNextFreeSlot(chargerId, request);
        }
    }

    private void offerNextFreeSlot(String chargerId, SessionRequest request) {
        Optional<TimeSlot> suggestion = reservationService.suggestNextSlot(request.vehicleId(), chargerId,
                request.start(), request.fromPct(), request.toPct());
        if (suggestion.isEmpty()) {
            out.println("No free slot on " + chargerId + " within the next 7 days.");
            return;
        }
        if (input.confirm("Next free slot is " + formatSlot(suggestion.get()) + ". Book it")) {
            book(chargerId, suggestion.get().start(), request);
        }
    }

    private void findBestCharger() {
        SessionRequest request = readSessionRequest();
        ChargerSelectionStrategy strategy = readStrategy();
        Optional<Offer> best = reservationService.findBestCharger(request.vehicleId(), request.start(),
                request.fromPct(), request.toPct(), strategy);
        if (best.isEmpty()) {
            out.println("No compatible charger has a free slot within the next 7 days.");
            return;
        }
        Offer offer = best.get();
        out.println("Best offer: charger " + offer.charger() + " at station " + offer.charger().getStationId()
                + ", " + formatSlot(offer.slot()) + ", " + formatMoney(offer.price()));
        if (input.confirm("Book it")) {
            book(offer.charger().getId(), offer.slot().start(), request);
        }
    }

    private ChargerSelectionStrategy readStrategy() {
        for (int i = 0; i < strategies.size(); i++) {
            out.println((i + 1) + "  " + strategies.get(i).getKey());
        }
        return strategies.get(input.readInt("Strategy", 1, strategies.size()) - 1).getValue();
    }

    private void book(String chargerId, LocalDateTime start, SessionRequest request) {
        Reservation reservation = reservationService.book(request.vehicleId(), chargerId, start, request.fromPct(),
                request.toPct());
        out.println("Booked: " + formatReservation(reservation));
    }

    private void cancelReservation() {
        if (!printActiveReservations()) {
            return;
        }
        String reservationId = input.readText("Reservation id");
        BigDecimal fee = cancellationService.previewFee(reservationId);
        if (input.confirm("Cancellation fee is " + formatMoney(fee) + ". Cancel")) {
            Reservation reservation = cancellationService.cancel(reservationId);
            out.println("Cancelled " + reservation.getId() + ", fee charged: "
                    + formatMoney(reservation.getCancellationFee()));
        }
    }

    private void completeReservation() {
        if (!printActiveReservations()) {
            return;
        }
        Reservation reservation = reservationService.complete(input.readText("Reservation id"));
        out.println("Completed: " + formatReservation(reservation));
    }

    /** Returns false when there is nothing to choose from. */
    private boolean printActiveReservations() {
        List<Reservation> active = catalog.activeReservations();
        if (active.isEmpty()) {
            out.println("There are no active reservations.");
            return false;
        }
        out.println("Active reservations:");
        active.forEach(reservation -> out.println("  " + formatReservation(reservation)));
        return true;
    }

    private void showSchedule() {
        String chargerId = input.readText("Charger id");
        List<Reservation> schedule = reservationService.scheduleOf(chargerId);
        if (schedule.isEmpty()) {
            out.println("Charger " + chargerId + " has no active reservations.");
            return;
        }
        schedule.forEach(reservation -> out.println("  " + formatReservation(reservation)));
    }

    private void showReports() {
        LocalDate date = input.readDate("Utilization date");

        out.println("Utilization on " + date + ":");
        reportService.utilization(date).forEach(entry ->
                out.println(format("  %-4s %5.1f %%", entry.charger().getId(), entry.percent())));

        out.println("Revenue per station:");
        reportService.revenueByStation().forEach(entry ->
                out.println(format("  %-4s %-26s %10s", entry.station().id(), entry.station().name(),
                        formatMoney(entry.revenue()))));

        out.println("Top " + TOP_VEHICLES + " vehicles by energy:");
        List<VehicleEnergy> topVehicles = reportService.topVehiclesByEnergy(TOP_VEHICLES);
        if (topVehicles.isEmpty()) {
            out.println("  (no sessions yet)");
        }
        topVehicles.forEach(entry -> out.println(format("  %-4s %-26s %6.1f kWh", entry.vehicle().id(),
                entry.vehicle().model(), entry.energyKwh())));
    }

    private SessionRequest readSessionRequest() {
        String vehicleId = input.readText("Vehicle id");
        int fromPct = input.readInt("Current battery %", 0, 99);
        int toPct = input.readInt("Target battery %", 1, 100);
        LocalDateTime start = input.readDateTime("Start");
        return new SessionRequest(vehicleId, start, fromPct, toPct);
    }

    private static String formatReservation(Reservation reservation) {
        return format("%-5s %-3s on %-3s %s  %d->%d%%  %.1f kWh  %s  %s", reservation.getId(),
                reservation.getVehicleId(), reservation.getChargerId(), formatSlot(reservation.getSlot()),
                reservation.getFromPct(), reservation.getTargetPct(), reservation.getEnergyKwh(),
                formatMoney(reservation.getPrice()), reservation.getStatus());
    }

    private static String formatSlot(TimeSlot slot) {
        return slot.start().format(InputReader.DATE_TIME_FORMAT) + " - " + slot.end().format(InputReader.DATE_TIME_FORMAT);
    }

    private static String formatDuration(Duration duration) {
        return format("%d h %02d min", duration.toHours(), duration.toMinutesPart());
    }

    private static String formatMoney(BigDecimal amount) {
        return amount.toPlainString() + " " + Money.CURRENCY.getCurrencyCode();
    }

    private static String powerLimit(double kw) {
        return kw > 0 ? format("%.1f kW", kw) : "-";
    }

    // A fixed locale keeps decimal points as '.' regardless of the user's system settings.
    private static String format(String pattern, Object... args) {
        return String.format(Locale.ROOT, pattern, args);
    }
}
