package com.gcu.scheduler.exception;

/**
 * Thrown when a requested record (appointment, slot, visit type, user) does not exist.
 */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param resource the kind of record, e.g. "Appointment"
     * @param id       the id that was requested
     */
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " " + id + " was not found.");
    }
}
