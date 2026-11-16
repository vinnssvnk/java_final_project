package com.ivanna.evcharging.infrastructure.io;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Parsing helpers for the mappers. Every failure becomes an IllegalArgumentException
 * naming the column, so CsvLoader can report a readable reason for each skipped line.
 */
final class CsvFields {

    private CsvFields() {
    }

    static void requireColumns(String[] fields, int expected) {
        if (fields.length != expected) {
            throw new IllegalArgumentException("expected " + expected + " columns but found " + fields.length);
        }
    }

    static int parseInt(String value, String column) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw invalid(column, value);
        }
    }

    static double parseDouble(String value, String column) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw invalid(column, value);
        }
    }

    static BigDecimal parseDecimal(String value, String column) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw invalid(column, value);
        }
    }

    static LocalDateTime parseDateTime(String value, String column) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid(column, value);
        }
    }

    static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String column) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw invalid(column, value);
        }
    }

    private static IllegalArgumentException invalid(String column, String value) {
        return new IllegalArgumentException("invalid " + column + " '" + value + "'");
    }
}
