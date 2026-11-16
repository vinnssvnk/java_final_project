package com.ivanna.evcharging.ui;

import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;

/** Reads typed values from the console and keeps asking until the input is valid. */
public class InputReader {

    static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    // Single 'H' also accepts a one-digit hour, so users may type "8:00" as well as "08:00".
    private static final DateTimeFormatter DATE_TIME_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm");
    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Scanner scanner;
    private final PrintStream out;

    public InputReader(Scanner scanner, PrintStream out) {
        this.scanner = scanner;
        this.out = out;
    }

    public String readText(String prompt) {
        return readUntilValid(prompt, line -> line.isEmpty() ? Optional.empty() : Optional.of(line),
                "Please enter a value.");
    }

    public int readInt(String prompt, int min, int max) {
        return readUntilValid(prompt + " [" + min + "-" + max + "]", line -> parseInt(line, min, max),
                "Please enter a whole number between " + min + " and " + max + ".");
    }

    public LocalDateTime readDateTime(String prompt) {
        return readUntilValid(prompt + " (yyyy-MM-dd HH:mm)", InputReader::parseDateTime,
                "Please use the format 2026-12-14 17:30.");
    }

    public LocalDate readDate(String prompt) {
        return readUntilValid(prompt + " (yyyy-MM-dd)", InputReader::parseDate, "Please use the format 2026-12-14.");
    }

    public boolean confirm(String prompt) {
        return readUntilValid(prompt + " (y/n)", InputReader::parseYesNo, "Please answer y or n.");
    }

    private <T> T readUntilValid(String prompt, Function<String, Optional<T>> parser, String hint) {
        while (true) {
            out.print(prompt + ": ");
            // nextLine throws NoSuchElementException when input ends; the menu treats that as "exit".
            Optional<T> value = parser.apply(scanner.nextLine().trim());
            if (value.isPresent()) {
                return value.get();
            }
            out.println(hint);
        }
    }

    private static Optional<Integer> parseInt(String text, int min, int max) {
        try {
            int value = Integer.parseInt(text);
            return value >= min && value <= max ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<LocalDateTime> parseDateTime(String text) {
        try {
            return Optional.of(LocalDateTime.parse(text, DATE_TIME_INPUT));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static Optional<LocalDate> parseDate(String text) {
        try {
            return Optional.of(LocalDate.parse(text, DATE_FORMAT));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static Optional<Boolean> parseYesNo(String text) {
        return switch (text.toLowerCase()) {
            case "y", "yes" -> Optional.of(true);
            case "n", "no" -> Optional.of(false);
            default -> Optional.empty();
        };
    }
}
