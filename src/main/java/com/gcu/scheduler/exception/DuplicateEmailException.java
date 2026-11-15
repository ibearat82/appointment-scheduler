package com.gcu.scheduler.exception;

/**
 * Thrown when someone registers with an email that already has an account.
 */
public class DuplicateEmailException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param email the email that is already in use
     */
    public DuplicateEmailException(String email) {
        super("An account with email " + email + " already exists.");
    }
}
