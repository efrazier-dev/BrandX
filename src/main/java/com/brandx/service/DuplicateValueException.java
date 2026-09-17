package com.brandx.service;

/**
 * Thrown when a value that must be unique (product SKU, customer email) is already
 * taken. Carries the offending property name so the controller can attach the
 * message to that specific form field instead of showing a generic error.
 */
public class DuplicateValueException extends RuntimeException {

    private final String field;

    public DuplicateValueException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
