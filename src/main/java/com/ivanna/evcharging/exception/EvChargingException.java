package com.ivanna.evcharging.exception;

/**
 * Base class of all business rule violations.
 * Unchecked because the caller usually cannot recover locally; the UI catches it once
 * and turns it into a friendly message.
 */
public class EvChargingException extends RuntimeException {

    public EvChargingException(String message) {
        super(message);
    }
}
