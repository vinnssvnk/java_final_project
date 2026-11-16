package com.ivanna.evcharging.infrastructure.io;

import com.ivanna.evcharging.domain.model.charger.AcCharger;
import com.ivanna.evcharging.domain.model.charger.Charger;
import com.ivanna.evcharging.domain.model.charger.ChargerType;
import com.ivanna.evcharging.domain.model.charger.ConnectorType;
import com.ivanna.evcharging.domain.model.charger.DcFastCharger;

import java.util.function.Function;
import java.util.function.Predicate;

/** id,stationId,type,connector,maxPowerKw; the type column decides which subclass is created. */
public class ChargerMapper implements Function<String[], Charger> {

    private static final int COLUMNS = 5;

    private final Predicate<String> stationExists;

    public ChargerMapper(Predicate<String> stationExists) {
        this.stationExists = stationExists;
    }

    @Override
    public Charger apply(String[] fields) {
        CsvFields.requireColumns(fields, COLUMNS);
        String id = fields[0];
        String stationId = fields[1];
        if (!stationExists.test(stationId)) {
            throw new IllegalArgumentException("unknown station '" + stationId + "'");
        }
        ChargerType type = CsvFields.parseEnum(ChargerType.class, fields[2], "type");
        ConnectorType connector = CsvFields.parseEnum(ConnectorType.class, fields[3], "connector");
        double maxPowerKw = CsvFields.parseDouble(fields[4], "maxPowerKw");
        return switch (type) {
            case AC -> new AcCharger(id, stationId, connector, maxPowerKw);
            case DC -> new DcFastCharger(id, stationId, connector, maxPowerKw);
        };
    }
}
