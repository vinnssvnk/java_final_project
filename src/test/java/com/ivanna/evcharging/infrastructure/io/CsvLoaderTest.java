package com.ivanna.evcharging.infrastructure.io;

import com.ivanna.evcharging.domain.model.charger.ConnectorType;
import com.ivanna.evcharging.domain.model.vehicle.Vehicle;
import com.ivanna.evcharging.exception.DataLoadException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvLoaderTest {

    private static final String HEADER = "id,model,batteryKwh,maxAcKw,maxDcKw,connectors\n";

    private final CsvLoader<Vehicle> loader = new CsvLoader<>();

    @Test
    void shouldLoadValidRows() throws DataLoadException {
        String csv = HEADER
                + "V1,Test Car,50,11,100,TYPE2|CCS2\n"
                + "V2, Old Leaf ,24,3.6,50,CHADEMO\n";

        LoadResult<Vehicle> result = loader.load(new StringReader(csv), new VehicleMapper());

        assertEquals(List.of(
                new Vehicle("V1", "Test Car", 50, 11, 100, Set.of(ConnectorType.TYPE2, ConnectorType.CCS2)),
                new Vehicle("V2", "Old Leaf", 24, 3.6, 50, Set.of(ConnectorType.CHADEMO))), result.items());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void shouldSkipAndReportMalformedLines() throws DataLoadException {
        String csv = HEADER
                + "V1,Test Car,50,11,100,TYPE2\n"
                + "V2,Bad Number,fifty,11,100,TYPE2\n"
                + "V3,Bad Connector,50,11,100,SCHUKO\n"
                + "V4,Too Few Columns,50\n"
                + "V5,Negative Battery,-5,11,100,TYPE2\n"
                + "V6,Another Good Car,40,11,0,TYPE2\n";

        LoadResult<Vehicle> result = loader.load(new StringReader(csv), new VehicleMapper());

        assertEquals(List.of("V1", "V6"), result.items().stream().map(Vehicle::id).toList());
        assertEquals(4, result.errors().size());
        assertEquals("line 3: invalid batteryKwh 'fifty'", result.errors().get(0));
        assertEquals("line 4: invalid connector 'SCHUKO'", result.errors().get(1));
        assertEquals("line 5: expected 6 columns but found 3", result.errors().get(2));
        assertTrue(result.errors().get(3).startsWith("line 6: batteryKwh must be positive"));
    }

    @Test
    void shouldSkipBlankLinesButKeepLineNumbers() throws DataLoadException {
        String csv = HEADER + "\n   \nV1,Bad,x,11,100,TYPE2\n";

        LoadResult<Vehicle> result = loader.load(new StringReader(csv), new VehicleMapper());

        assertTrue(result.items().isEmpty());
        assertEquals(List.of("line 4: invalid batteryKwh 'x'"), result.errors());
    }

    @Test
    void shouldReturnEmptyResultForHeaderOnlyFile() throws DataLoadException {
        LoadResult<Vehicle> result = loader.load(new StringReader(HEADER), new VehicleMapper());

        assertTrue(result.items().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void shouldWrapReadFailureInDataLoadException() {
        Reader failingReader = new Reader() {
            @Override
            public int read(char[] buffer, int offset, int length) throws IOException {
                throw new IOException("disk unplugged");
            }

            @Override
            public void close() {
            }
        };

        assertThrows(DataLoadException.class, () -> loader.load(failingReader, new VehicleMapper()));
    }
}
