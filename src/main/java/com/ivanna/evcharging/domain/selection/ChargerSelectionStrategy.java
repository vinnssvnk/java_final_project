package com.ivanna.evcharging.domain.selection;

import com.ivanna.evcharging.domain.model.reservation.Offer;

import java.util.List;
import java.util.Optional;

/** Picks the best offer according to one criterion; empty when there are no offers. */
public interface ChargerSelectionStrategy {

    Optional<Offer> select(List<Offer> offers);
}
