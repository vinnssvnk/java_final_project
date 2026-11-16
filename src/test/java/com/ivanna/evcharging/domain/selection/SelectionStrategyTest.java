package com.ivanna.evcharging.domain.selection;

import com.ivanna.evcharging.domain.model.charger.AcCharger;
import com.ivanna.evcharging.domain.model.charger.ConnectorType;
import com.ivanna.evcharging.domain.model.charger.DcFastCharger;
import com.ivanna.evcharging.domain.model.reservation.Offer;
import com.ivanna.evcharging.domain.model.reservation.TimeSlot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionStrategyTest {

    private static final LocalDateTime TEN = LocalDateTime.of(2026, 12, 14, 10, 0);

    private final Offer slowCheap = new Offer(new AcCharger("AC1", "S1", ConnectorType.TYPE2, 11),
            TimeSlot.of(TEN, Duration.ofHours(3)), new BigDecimal("9.00"));
    private final Offer fastExpensive = new Offer(new DcFastCharger("DC1", "S2", ConnectorType.CCS2, 150),
            TimeSlot.of(TEN, Duration.ofMinutes(25)), new BigDecimal("15.00"));
    private final Offer cheapButLater = new Offer(new AcCharger("AC2", "S1", ConnectorType.TYPE2, 11),
            TimeSlot.of(TEN.plusHours(1), Duration.ofHours(3)), new BigDecimal("9.00"));

    @Test
    void fastestFinishShouldPickEarliestEnd() {
        Optional<Offer> best = new FastestFinishStrategy().select(List.of(slowCheap, fastExpensive));

        assertEquals(Optional.of(fastExpensive), best);
    }

    @Test
    void cheapestShouldPickLowestPrice() {
        Optional<Offer> best = new CheapestStrategy().select(List.of(fastExpensive, slowCheap));

        assertEquals(Optional.of(slowCheap), best);
    }

    @Test
    void cheapestShouldBreakTieByEarliestEnd() {
        Optional<Offer> best = new CheapestStrategy().select(List.of(cheapButLater, slowCheap));

        assertEquals(Optional.of(slowCheap), best);
    }

    @Test
    void shouldReturnEmptyWithoutOffers() {
        assertTrue(new CheapestStrategy().select(List.of()).isEmpty());
        assertTrue(new FastestFinishStrategy().select(List.of()).isEmpty());
    }
}
