package com.brandx.service;

/**
 * Thrown when a requested record does not exist. Translated to a 404 page by
 * {@link com.brandx.web.GlobalExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
