package com.ivanna.evcharging.service;

import com.ivanna.evcharging.domain.model.reservation.Offer;
import com.ivanna.evcharging.domain.model.reservation.Reservation;
import com.ivanna.evcharging.domain.model.reservation.ReservationStatus;
import com.ivanna.evcharging.domain.model.reservation.TimeSlot;
import com.ivanna.evcharging.domain.selection.CheapestStrategy;
import com.ivanna.evcharging.domain.selection.FastestFinishStrategy;
import com.ivanna.evcharging.exception.IllegalStatusTransitionException;
import com.ivanna.evcharging.exception.IncompatibleConnectorException;
import com.ivanna.evcharging.exception.ReservationNotFoundException;
import com.ivanna.evcharging.exception.SlotConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationServiceTest {

    private static final LocalDateTime TEN = TestNetwork.NOW.plusHours(2);

    private final TestNetwork network = new TestNetwork();
    private final ReservationService service = network.reservationService();

    @Test
    void shouldEstimateDurationEnergyAndPrice() {
        SessionEstimate estimate = service.estimate("V1", "AC1", 20, 80, TEN);

        // 30 kWh at 11 kW = 163.6 min; 30 kWh * 0.40 EUR
        assertEquals(new SessionEstimate(Duration.ofMinutes(164), 30.0, new BigDecimal("12.00")), estimate);
    }

    @Test
    void shouldBookFreeSlot() {
        Reservation reservation = service.book("V1", "AC1", TEN, 20, 80);

        assertEquals("R001", reservation.getId());
        assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());
        assertEquals(TimeSlot.of(TEN, Duration.ofMinutes(164)), reservation.getSlot());
        assertEquals(new BigDecimal("12.00"), reservation.getPrice());
        assertEquals(30.0, reservation.getEnergyKwh());
        assertEquals(Optional.of(reservation), network.reservations.findById("R001"));
        assertEquals(List.of(reservation), network.charger("AC1").getSchedule().reservations());
    }

    @Test
    void shouldThrowWhenSlotOverlapsExistingReservationAndSaveNothing() {
        service.book("V1", "AC1", TEN, 20, 80);

        assertThrows(SlotConflictException.class, () -> service.book("V2", "AC1", TEN.plusHours(1), 20, 80));
        assertEquals(1, network.reservations.findAll().size());
    }

    @Test
    void shouldAllowBookingThatStartsWhenPreviousEnds() {
        Reservation first = service.book("V1", "AC1", TEN, 20, 80);

        Reservation second = service.book("V2", "AC1", first.getSlot().end(), 20, 80);

        assertEquals("R002", second.getId());
    }

    @Test
    void shouldSuggestSlotRightAfterConflictingReservation() {
        Reservation existing = service.book("V1", "AC1", TEN, 20, 80);

        Optional<TimeSlot> suggestion = service.suggestNextSlot("V2", "AC1", TEN.plusMinutes(30), 20, 80);

        assertEquals(existing.getSlot().end(), suggestion.orElseThrow().start());
    }

    @Test
    void shouldRejectUnknownVehicle() {
        assertThrows(IllegalArgumentException.class, () -> service.book("V99", "AC1", TEN, 20, 80));
    }

    @Test
    void shouldRejectUnknownCharger() {
        assertThrows(IllegalArgumentException.class, () -> service.book("V1", "XX", TEN, 20, 80));
    }

    @Test
    void shouldRejectStartInThePast() {
        assertThrows(IllegalArgumentException.class,
                () -> service.book("V1", "AC1", TestNetwork.NOW.minusMinutes(1), 20, 80));
    }

    @Test
    void shouldRejectStartExactlyNow() {
        assertThrows(IllegalArgumentException.class, () -> service.book("V1", "AC1", TestNetwork.NOW, 20, 80));
    }

    @Test
    void shouldRejectFromPctNotBelowToPct() {
        assertThrows(IllegalArgumentException.class, () -> service.book("V1", "AC1", TEN, 80, 80));
        assertThrows(IllegalArgumentException.class, () -> service.book("V1", "AC1", TEN, 90, 80));
    }

    @Test
    void shouldRejectVehicleWithoutDcOnDcCharger() {
        assertThrows(IncompatibleConnectorException.class, () -> service.book("V2", "DC1", TEN, 20, 80));
    }

    @Test
    void fastestFinishShouldPickDcCharger() {
        Offer best = service.findBestCharger("V1", TEN, 20, 80, new FastestFinishStrategy()).orElseThrow();

        // 30 kWh at the vehicle's 100 kW DC limit = 18 min
        assertEquals("DC1", best.charger().getId());
        assertEquals(TimeSlot.of(TEN, Duration.ofMinutes(18)), best.slot());
        assertEquals(new BigDecimal("18.00"), best.price());
    }

    @Test
    void cheapestShouldPickAcChargerAtCheaperStation() {
        Offer best = service.findBestCharger("V1", TEN, 20, 80, new CheapestStrategy()).orElseThrow();

        assertEquals("AC1", best.charger().getId());
        assertEquals(new BigDecimal("12.00"), best.price());
    }

    @Test
    void cheapestShouldPreferChargerThatFinishesEarlierWhenPricesAreEqual() {
        service.book("V2", "AC1", TEN, 20, 80);

        Offer best = service.findBestCharger("V1", TEN, 20, 80, new CheapestStrategy()).orElseThrow();

        assertEquals("AC2", best.charger().getId());
    }

    @Test
    void bestChargerShouldIgnoreIncompatibleChargers() {
        Offer best = service.findBestCharger("V2", TEN, 20, 80, new FastestFinishStrategy()).orElseThrow();

        assertEquals("AC1", best.charger().getId());
    }

    @Test
    void shouldCompleteReservationAndReleaseItsSlot() {
        Reservation reservation = service.book("V1", "AC1", TEN, 20, 80);

        service.complete(reservation.getId());

        assertEquals(ReservationStatus.COMPLETED, reservation.getStatus());
        assertTrue(network.charger("AC1").getSchedule().isFree(reservation.getSlot()));
    }

    @Test
    void shouldRejectCompletingTwice() {
        Reservation reservation = service.book("V1", "AC1", TEN, 20, 80);
        service.complete(reservation.getId());

        assertThrows(IllegalStatusTransitionException.class, () -> service.complete(reservation.getId()));
    }

    @Test
    void shouldRejectCompletingUnknownReservation() {
        assertThrows(ReservationNotFoundException.class, () -> service.complete("R999"));
    }
}
