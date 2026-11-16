package com.ivanna.evcharging.ui;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputReaderTest {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    private InputReader readerFor(String typed) {
        return new InputReader(new Scanner(typed), new PrintStream(output, true));
    }

    @Test
    void shouldAcceptOneDigitHour() {
        LocalDateTime start = readerFor("2026-12-14 8:00\n").readDateTime("Start");

        assertEquals(LocalDateTime.of(2026, 12, 14, 8, 0), start);
    }

    @Test
    void shouldAcceptTwoDigitHour() {
        LocalDateTime start = readerFor("2026-12-14 08:00\n").readDateTime("Start");

        assertEquals(LocalDateTime.of(2026, 12, 14, 8, 0), start);
    }

    @Test
    void shouldRepromptUntilDateTimeIsValid() {
        LocalDateTime start = readerFor("14.12.2026 8:00\n2026-12-14 17:30\n").readDateTime("Start");

        assertEquals(LocalDateTime.of(2026, 12, 14, 17, 30), start);
        assertTrue(output.toString().contains("Please use the format"));
    }

    @Test
    void shouldRepromptUntilNumberIsInRange() {
        int value = readerFor("abc\n150\n42\n").readInt("Target battery %", 1, 100);

        assertEquals(42, value);
    }
}
