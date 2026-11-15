package com.gcu.scheduler.exception;

/**
 * Thrown when a patient tries to view or change an appointment that belongs to someone else.
 */
public class AccessDeniedBusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message description of the denied action
     */
    public AccessDeniedBusinessException(String message) {
        super(message);
    }
}
