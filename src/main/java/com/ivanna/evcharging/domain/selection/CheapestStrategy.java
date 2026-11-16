package com.ivanna.evcharging.domain.selection;

import com.ivanna.evcharging.domain.model.reservation.Offer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Lowest price wins; among equally priced offers the one finishing first is preferred. */
public class CheapestStrategy implements ChargerSelectionStrategy {

    private static final Comparator<Offer> BY_PRICE_THEN_END =
            Comparator.comparing(Offer::price).thenComparing(offer -> offer.slot().end());

    @Override
    public Optional<Offer> select(List<Offer> offers) {
        return offers.stream().min(BY_PRICE_THEN_END);
    }
}
