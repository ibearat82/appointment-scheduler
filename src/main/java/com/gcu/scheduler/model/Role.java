package com.gcu.scheduler.model;

/**
 * Security roles supported by the application.
 */
public enum Role {
    /** A patient who books and manages their own appointments. */
    PATIENT,
    /** Office staff who manage visit types, time slots, and all appointments. */
    ADMIN
}
