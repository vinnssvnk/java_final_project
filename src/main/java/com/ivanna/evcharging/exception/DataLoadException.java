package com.ivanna.evcharging.exception;

/**
 * Raised when start-up data cannot be read at all (missing file, I/O failure).
 * Checked on purpose: the composition root must decide how to react, because the
 * application is useless without its data.
 */
public class DataLoadException extends Exception {

    public DataLoadException(String message) {
        super(message);
    }

    public DataLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
