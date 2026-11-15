package com.gcu.scheduler.model;

/**
 * Lifecycle status of an appointment.
 * <p>
 * Cancelled appointments are deleted so their time slot can be booked again
 * (the UNIQUE key on appointments.slot_id allows only one row per slot).
 */
public enum AppointmentStatus {
    /** The appointment is booked and upcoming. */
    BOOKED,
    /** The visit took place (set by office staff). */
    COMPLETED
}
