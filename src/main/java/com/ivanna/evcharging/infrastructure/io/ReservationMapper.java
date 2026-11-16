package com.ivanna.evcharging.infrastructure.io;

import com.ivanna.evcharging.domain.model.reservation.Reservation;
import com.ivanna.evcharging.domain.model.reservation.ReservationStatus;
import com.ivanna.evcharging.domain.model.reservation.TimeSlot;
import com.ivanna.evcharging.domain.model.vehicle.Vehicle;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * id,vehicleId,chargerId,start,end,fromPct,targetPct,price,status.
 * The vehicle is looked up because the delivered energy is derived from its battery size.
 */
public class ReservationMapper implements Function<String[], Reservation> {

    private static final int COLUMNS = 9;

    private final Function<String, Optional<Vehicle>> vehicleLookup;
    private final Predicate<String> chargerExists;

    public ReservationMapper(Function<String, Optional<Vehicle>> vehicleLookup, Predicate<String> chargerExists) {
        this.vehicleLookup = vehicleLookup;
        this.chargerExists = chargerExists;
    }

    @Override
    public Reservation apply(String[] fields) {
        CsvFields.requireColumns(fields, COLUMNS);
        String vehicleId = fields[1];
        String chargerId = fields[2];
        Vehicle vehicle = vehicleLookup.apply(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("unknown vehicle '" + vehicleId + "'"));
        if (!chargerExists.test(chargerId)) {
            throw new IllegalArgumentException("unknown charger '" + chargerId + "'");
        }
        TimeSlot slot = new TimeSlot(
                CsvFields.parseDateTime(fields[3], "start"),
                CsvFields.parseDateTime(fields[4], "end"));
        int fromPct = CsvFields.parseInt(fields[5], "fromPct");
        int targetPct = CsvFields.parseInt(fields[6], "targetPct");
        return new Reservation(
                fields[0],
                vehicleId,
                chargerId,
                slot,
                fromPct,
                targetPct,
                vehicle.energyNeededKwh(fromPct, targetPct),
                CsvFields.parseDecimal(fields[7], "price"),
                CsvFields.parseEnum(ReservationStatus.class, fields[8], "status"));
    }
}
