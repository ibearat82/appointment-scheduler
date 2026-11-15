package com.gcu.scheduler.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gcu.scheduler.dto.SlotGenerationForm;
import com.gcu.scheduler.exception.BookingException;
import com.gcu.scheduler.exception.ResourceNotFoundException;
import com.gcu.scheduler.model.TimeSlot;
import com.gcu.scheduler.repository.AppointmentRepository;
import com.gcu.scheduler.repository.TimeSlotRepository;

/**
 * Business service for the doctor's available time slots.
 */
@Service
public class TimeSlotService {

    private static final Logger log = LoggerFactory.getLogger(TimeSlotService.class);

    /** How many days ahead patients can see open slots. */
    public static final int BOOKING_WINDOW_DAYS = 30;

    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    /**
     * @param timeSlotRepository    time slot DAO (injected)
     * @param appointmentRepository appointment DAO (injected)
     * @param clock                 clock in the office time zone (injected, replaceable in tests)
     */
    public TimeSlotService(TimeSlotRepository timeSlotRepository, AppointmentRepository appointmentRepository,
                           Clock clock) {
        this.timeSlotRepository = timeSlotRepository;
        this.appointmentRepository = appointmentRepository;
        this.clock = clock;
    }

    /**
     * Open slots on one day, excluding any that already started.
     *
     * @param date the day
     * @return open future slots on that day
     */
    @Transactional(readOnly = true)
    public List<TimeSlot> findOpenSlots(LocalDate date) {
        LocalDateTime now = LocalDateTime.now(clock);
        return timeSlotRepository.findBySlotDateAndAvailableTrueOrderByStartTimeAsc(date).stream()
                .filter(slot -> !slot.isInPast(now))
                .toList();
    }

    /**
     * Days in the booking window that still have at least one open slot.
     *
     * @return distinct dates with open slots, earliest first
     */
    @Transactional(readOnly = true)
    public List<LocalDate> findDaysWithOpenSlots() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        return timeSlotRepository
                .findBySlotDateBetweenAndAvailableTrueOrderBySlotDateAscStartTimeAsc(today, today.plusDays(BOOKING_WINDOW_DAYS))
                .stream()
                .filter(slot -> !slot.isInPast(now))
                .map(TimeSlot::getSlotDate)
                .distinct()
                .toList();
    }

    /** @return all slots from today on, open or booked (admin view) */
    @Transactional(readOnly = true)
    public List<TimeSlot> findUpcomingSlots() {
        return timeSlotRepository.findBySlotDateGreaterThanEqualOrderBySlotDateAscStartTimeAsc(LocalDate.now(clock));
    }

    /**
     * @param id the slot id
     * @return the slot
     * @throws ResourceNotFoundException if it does not exist
     */
    @Transactional(readOnly = true)
    public TimeSlot findById(Long id) {
        return timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot", id));
    }

    /**
     * Opens slots for a day. Slots that would run past the end time, start in
     * the past, or already exist are skipped.
     *
     * @param form validated generation form
     * @return the number of slots created
     * @throws BookingException if no new slots could be created
     */
    @Transactional
    public int generateSlots(SlotGenerationForm form) {
        log.info("Entering generateSlots() for {} {}-{} every {} min",
                form.getDate(), form.getFromTime(), form.getToTime(), form.getIntervalMinutes());
        LocalDateTime now = LocalDateTime.now(clock);
        int created = 0;
        LocalTime start = form.getFromTime();
        while (!start.plusMinutes(form.getIntervalMinutes()).isAfter(form.getToTime())) {
            LocalTime end = start.plusMinutes(form.getIntervalMinutes());
            if (end.isBefore(start)) {
                break; // would run past midnight
            }
            boolean inPast = LocalDateTime.of(form.getDate(), start).isBefore(now);
            if (!inPast && !timeSlotRepository.existsBySlotDateAndStartTime(form.getDate(), start)) {
                TimeSlot slot = new TimeSlot();
                slot.setSlotDate(form.getDate());
                slot.setStartTime(start);
                slot.setEndTime(end);
                slot.setAvailable(true);
                timeSlotRepository.save(slot);
                created++;
            }
            start = end;
        }
        if (created == 0) {
            throw new BookingException("No new slots were created. They may already exist, be in the past, "
                    + "or the window is shorter than one slot.");
        }
        log.info("Exiting generateSlots() - created {} slots", created);
        return created;
    }

    /**
     * Deletes a slot that has no appointment.
     *
     * @param id the slot id
     * @throws BookingException if the slot is booked
     */
    @Transactional
    public void deleteSlot(Long id) {
        TimeSlot slot = findById(id);
        if (appointmentRepository.existsByTimeSlotId(id)) {
            throw new BookingException("This slot has an appointment. Cancel the appointment first.");
        }
        timeSlotRepository.delete(slot);
        log.info("Time slot {} deleted", id);
    }
}
