package com.ivanna.evcharging.infrastructure.io;

import com.ivanna.evcharging.exception.DataLoadException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Reads comma-separated rows and turns each into an object with the given mapper.
 * One bad row should not make the whole data set unusable, so a row the mapper rejects is
 * skipped and reported, while an unreadable source fails the whole load.
 */
public class CsvLoader<T> {

    private static final String SEPARATOR = ",";

    /**
     * The reader must be positioned at the header line; the caller opened it and is responsible for closing it.
     * The mapper converts the trimmed fields of one row and throws IllegalArgumentException for bad data.
     */
    public LoadResult<T> load(Reader reader, Function<String[], T> mapper) throws DataLoadException {
        List<T> items = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        BufferedReader lines = new BufferedReader(reader);
        try {
            lines.readLine(); // header
            int lineNumber = 1;
            String line;
            while ((line = lines.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    items.add(mapper.apply(split(line)));
                } catch (IllegalArgumentException e) {
                    errors.add("line " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new DataLoadException("Could not read CSV data: " + e.getMessage(), e);
        }
        return new LoadResult<>(items, errors);
    }

    private static String[] split(String line) {
        // limit -1 keeps trailing empty fields, e.g. a missing peakPrice, so the column count stays right
        return Arrays.stream(line.split(SEPARATOR, -1)).map(String::trim).toArray(String[]::new);
    }
}
