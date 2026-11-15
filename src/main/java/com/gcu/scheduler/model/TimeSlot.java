package com.gcu.scheduler.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One bookable block of the doctor's time on a given day.
 * Maps to the {@code time_slots} table.
 */
@Entity
@Table(name = "time_slots",
       uniqueConstraints = @UniqueConstraint(name = "uk_slot_date_start", columnNames = {"slot_date", "start_time"}))
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private boolean available = true;

    /** @return the date and start time combined */
    public LocalDateTime getStartDateTime() {
        return LocalDateTime.of(slotDate, startTime);
    }

    /**
     * @param now the current date and time
     * @return true if the slot starts before {@code now}
     */
    public boolean isInPast(LocalDateTime now) {
        return getStartDateTime().isBefore(now);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
