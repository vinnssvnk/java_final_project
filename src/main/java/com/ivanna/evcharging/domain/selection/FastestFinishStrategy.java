package com.ivanna.evcharging.domain.selection;

import com.ivanna.evcharging.domain.model.reservation.Offer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** The driver wants to leave as early as possible: a slower charger that is free now can beat a busy fast one. */
public class FastestFinishStrategy implements ChargerSelectionStrategy {

    @Override
    public Optional<Offer> select(List<Offer> offers) {
        return offers.stream().min(Comparator.comparing(offer -> offer.slot().end()));
    }
}
