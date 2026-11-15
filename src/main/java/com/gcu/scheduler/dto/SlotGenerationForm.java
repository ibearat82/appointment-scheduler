package com.gcu.scheduler.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Form backing object the admin uses to open time slots for a day.
 * Slots of {@code intervalMinutes} are created from {@code fromTime} up to {@code toTime}.
 * For a single slot, set the interval equal to the length of the window.
 */
public class SlotGenerationForm {

    @NotNull(message = "Date is required")
    @FutureOrPresent(message = "Date cannot be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @NotNull(message = "Start time is required")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime fromTime = LocalTime.of(9, 0);

    @NotNull(message = "End time is required")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime toTime = LocalTime.of(17, 0);

    @Min(value = 10, message = "Slot length must be at least 10 minutes")
    @Max(value = 240, message = "Slot length must be 240 minutes or fewer")
    private int intervalMinutes = 30;

    /** @return true when the end time is after the start time */
    @AssertTrue(message = "End time must be after start time")
    public boolean isTimeRangeValid() {
        return fromTime == null || toTime == null || toTime.isAfter(fromTime);
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getFromTime() { return fromTime; }
    public void setFromTime(LocalTime fromTime) { this.fromTime = fromTime; }

    public LocalTime getToTime() { return toTime; }
    public void setToTime(LocalTime toTime) { this.toTime = toTime; }

    public int getIntervalMinutes() { return intervalMinutes; }
    public void setIntervalMinutes(int intervalMinutes) { this.intervalMinutes = intervalMinutes; }
}
