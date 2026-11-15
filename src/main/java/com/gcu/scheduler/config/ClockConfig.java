package com.gcu.scheduler.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gcu.scheduler.AppointmentSchedulerApplication;

/**
 * Provides a {@link Clock} in the office time zone. Services take the clock by
 * dependency injection so unit tests can use a fixed time.
 */
@Configuration
public class ClockConfig {

    /** @return the system clock in America/Phoenix */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(AppointmentSchedulerApplication.OFFICE_TIME_ZONE));
    }
}
