package com.ivanna.evcharging.infrastructure.io;

import com.ivanna.evcharging.domain.model.station.Station;
import com.ivanna.evcharging.domain.pricing.FlatPricing;
import com.ivanna.evcharging.domain.pricing.PricingPolicy;
import com.ivanna.evcharging.domain.pricing.TimeOfUsePricing;

import java.math.BigDecimal;
import java.util.function.Function;

/** id,name,city,tariff,basePrice,peakPrice where tariff is FLAT or TIME_OF_USE. */
public class StationMapper implements Function<String[], Station> {

    private static final int COLUMNS = 6;

    @Override
    public Station apply(String[] fields) {
        CsvFields.requireColumns(fields, COLUMNS);
        return new Station(fields[0], fields[1], fields[2], pricingFrom(fields[3], fields[4], fields[5]));
    }

    private static PricingPolicy pricingFrom(String tariff, String basePrice, String peakPrice) {
        BigDecimal base = CsvFields.parseDecimal(basePrice, "basePrice");
        return switch (tariff) {
            case "FLAT" -> new FlatPricing(base);
            case "TIME_OF_USE" -> new TimeOfUsePricing(base, CsvFields.parseDecimal(peakPrice, "peakPrice"));
            default -> throw new IllegalArgumentException("invalid tariff '" + tariff + "'");
        };
    }
}
