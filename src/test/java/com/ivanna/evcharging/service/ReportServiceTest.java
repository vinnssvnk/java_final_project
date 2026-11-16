package com.ivanna.evcharging.service;

import com.ivanna.evcharging.domain.model.reservation.Reservation;
import com.ivanna.evcharging.service.ReportService.ChargerUtilization;
import com.ivanna.evcharging.service.ReportService.StationRevenue;
import com.ivanna.evcharging.service.ReportService.VehicleEnergy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportServiceTest {

    private final TestNetwork network = new TestNetwork();
    private final ReservationService reservationService = network.reservationService();
    private final ReportService reportService = network.reportService();

    @Test
    void shouldCountBookedMinutesWithinTheDayOnly() {
        // 23:00 + 164 min: 60 minutes fall on the 14th, the rest on the 15th
        reservationService.book("V1", "AC1", TestNetwork.NOW.withHour(23), 20, 80);

        Map<String, Double> percentByCharger = reportService.utilization(TestNetwork.NOW.toLocalDate()).stream()
                .collect(Collectors.toMap(utilization -> utilization.charger().getId(), ChargerUtilization::percent));

        assertEquals(60 * 100.0 / 1440, percentByCharger.get("AC1"), 1e-9);
        assertEquals(0.0, percentByCharger.get("DC1"));
    }

    @Test
    void shouldIgnoreCancelledReservationsInUtilization() {
        Reservation reservation = reservationService.book("V1", "AC1", TestNetwork.NOW.plusHours(4), 20, 80);
        network.cancellationServiceAt(TestNetwork.NOW).cancel(reservation.getId());

        double percent = reportService.utilization(TestNetwork.NOW.toLocalDate()).get(0).percent();

        assertEquals(0.0, percent);
    }

    @Test
    void shouldAddPricesAndCancellationFeesPerStation() {
        LocalDateTime noon = TestNetwork.NOW.withHour(12);
        reservationService.book("V1", "AC1", noon, 20, 80);                        // 12.00 at S1
        Reservation late = reservationService.book("V2", "AC2", TestNetwork.NOW.plusMinutes(30), 20, 80); // 9.60 at S1
        network.cancellationServiceAt(TestNetwork.NOW).cancel(late.getId());       // fee 1.92
        reservationService.book("V1", "DC1", noon, 20, 80);                        // 18.00 at S2

        List<StationRevenue> revenue = reportService.revenueByStation();

        assertEquals(new BigDecimal("13.92"), revenue.get(0).revenue());
        assertEquals(new BigDecimal("18.00"), revenue.get(1).revenue());
    }

    @Test
    void shouldRankVehiclesByDeliveredEnergy() {
        reservationService.book("V2", "AC1", TestNetwork.NOW.plusHours(1), 0, 90);   // 36 kWh
        reservationService.book("V1", "DC1", TestNetwork.NOW.plusHours(1), 20, 80);  // 30 kWh
        reservationService.book("V1", "DC1", TestNetwork.NOW.plusHours(5), 50, 70);  // 10 kWh

        List<VehicleEnergy> top = reportService.topVehiclesByEnergy(3);

        assertEquals(List.of("V1", "V2"), top.stream().map(entry -> entry.vehicle().id()).toList());
        assertEquals(40.0, top.get(0).energyKwh(), 1e-9);
    }
}
