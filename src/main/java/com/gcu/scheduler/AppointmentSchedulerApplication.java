package com.gcu.scheduler;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Doctor's Office Appointment Scheduling Application.
 * <p>
 * CST-323 CLC Project, Group A2 (Imam Bearat and Eliezer Centeno Paz).
 * The application is built as an N-Layer Spring Boot application:
 * presentation (controllers + Thymeleaf views), business services, and
 * persistence (Spring Data JPA repositories) backed by MySQL.
 */
@SpringBootApplication
public class AppointmentSchedulerApplication {

    /** The office is in Arizona, which does not observe daylight saving time. */
    public static final String OFFICE_TIME_ZONE = "America/Phoenix";

    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        // Store and display all times in one zone so the laptop, the cloud server,
        // and the database agree (risk identified in Milestone 1).
        TimeZone.setDefault(TimeZone.getTimeZone(OFFICE_TIME_ZONE));
        SpringApplication.run(AppointmentSchedulerApplication.class, args);
    }
}
