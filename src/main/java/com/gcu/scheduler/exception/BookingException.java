package com.gcu.scheduler.exception;

/**
 * Thrown when a business rule blocks a booking action, for example when a
 * slot is already taken or is in the past. The message is safe to show users.
 */
public class BookingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message a user-friendly description of the rule that was broken
     */
    public BookingException(String message) {
        super(message);
    }
}
