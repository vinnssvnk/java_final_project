package com.ivanna.evcharging.infrastructure.io;

import com.ivanna.evcharging.domain.model.charger.ConnectorType;
import com.ivanna.evcharging.domain.model.vehicle.Vehicle;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** id,model,batteryKwh,maxAcKw,maxDcKw,connectors with connectors separated by '|'. */
public class VehicleMapper implements Function<String[], Vehicle> {

    private static final int COLUMNS = 6;
    private static final String CONNECTOR_SEPARATOR = "\\|";

    @Override
    public Vehicle apply(String[] fields) {
        CsvFields.requireColumns(fields, COLUMNS);
        return new Vehicle(
                fields[0],
                fields[1],
                CsvFields.parseDouble(fields[2], "batteryKwh"),
                CsvFields.parseDouble(fields[3], "maxAcKw"),
                CsvFields.parseDouble(fields[4], "maxDcKw"),
                parseConnectors(fields[5]));
    }

    private static Set<ConnectorType> parseConnectors(String value) {
        return Arrays.stream(value.split(CONNECTOR_SEPARATOR))
                .map(String::trim)
                .map(connector -> CsvFields.parseEnum(ConnectorType.class, connector, "connector"))
                .collect(Collectors.toSet());
    }
}
