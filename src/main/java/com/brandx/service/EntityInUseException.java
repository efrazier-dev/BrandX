package com.brandx.service;

/**
 * Thrown when a delete is refused because other records still reference the row.
 *
 * <p>Checked in the service layer rather than left to the database: a foreign-key
 * violation surfacing from the driver would be an opaque 500, whereas this carries
 * a message the user can act on.
 */
public class EntityInUseException extends RuntimeException {

    public EntityInUseException(String message) {
        super(message);
    }
}
