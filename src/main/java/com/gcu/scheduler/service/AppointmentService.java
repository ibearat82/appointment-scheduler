package com.gcu.scheduler.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gcu.scheduler.dto.BookingForm;
import com.gcu.scheduler.exception.AccessDeniedBusinessException;
import com.gcu.scheduler.exception.BookingException;
import com.gcu.scheduler.exception.ResourceNotFoundException;
import com.gcu.scheduler.model.Appointment;
import com.gcu.scheduler.model.AppointmentStatus;
import com.gcu.scheduler.model.Role;
import com.gcu.scheduler.model.TimeSlot;
import com.gcu.scheduler.model.User;
import com.gcu.scheduler.model.VisitType;
import com.gcu.scheduler.repository.AppointmentRepository;
import com.gcu.scheduler.repository.TimeSlotRepository;

/**
 * Business service holding the booking rules: no double booking, no past
 * dates, only active visit types, and patients may change only their own appointments.
 */
@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final VisitTypeService visitTypeService;
    private final UserService userService;
    private final Clock clock;

    /**
     * @param appointmentRepository appointment DAO (injected)
     * @param timeSlotRepository    time slot DAO (injected)
     * @param visitTypeService      visit type service (injected)
     * @param userService           user service (injected)
     * @param clock                 clock in the office time zone (injected)
     */
    public AppointmentService(AppointmentRepository appointmentRepository, TimeSlotRepository timeSlotRepository,
                              VisitTypeService visitTypeService, UserService userService, Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.visitTypeService = visitTypeService;
        this.userService = userService;
        this.clock = clock;
    }

    /**
     * Books an appointment for the logged-in patient.
     *
     * @param patientEmail the logged-in patient's email
     * @param form         validated booking form
     * @return the saved appointment
     * @throws BookingException if the slot is taken, in the past, or the visit type is inactive
     */
    @Transactional
    public Appointment book(String patientEmail, BookingForm form) {
        log.info("Entering book() for {} slot {}", patientEmail, form.getSlotId());
        User patient = userService.findByEmail(patientEmail);
        VisitType visitType = requireActiveVisitType(form.getVisitTypeId());
        TimeSlot slot = lockOpenFutureSlot(form.getSlotId());

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setVisitType(visitType);
        appointment.setTimeSlot(slot);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setNotes(cleanNotes(form.getNotes()));

        slot.setAvailable(false);
        Appointment saved = saveCatchingDoubleBooking(appointment);
        timeSlotRepository.save(slot);
        log.info("Exiting book() - appointment id {}", saved.getId());
        return saved;
    }

    /**
     * Moves an appointment to a new slot and/or visit type. The old slot is
     * released only after the new slot is confirmed free.
     *
     * @param appointmentId the appointment to change
     * @param userEmail     the logged-in user's email
     * @param form          validated booking form with the new choices
     * @return the updated appointment
     */
    @Transactional
    public Appointment reschedule(Long appointmentId, String userEmail, BookingForm form) {
        log.info("Entering reschedule() for appointment {} by {}", appointmentId, userEmail);
        Appointment appointment = findForUser(appointmentId, userEmail);
        requireChangeable(appointment);

        VisitType visitType = requireActiveVisitType(form.getVisitTypeId());
        appointment.setVisitType(visitType);
        appointment.setNotes(cleanNotes(form.getNotes()));

        TimeSlot oldSlot = appointment.getTimeSlot();
        if (!oldSlot.getId().equals(form.getSlotId())) {
            TimeSlot newSlot = lockOpenFutureSlot(form.getSlotId());
            newSlot.setAvailable(false);
            appointment.setTimeSlot(newSlot);
            oldSlot.setAvailable(true);
            timeSlotRepository.save(newSlot);
            timeSlotRepository.save(oldSlot);
        }
        Appointment saved = saveCatchingDoubleBooking(appointment);
        log.info("Exiting reschedule() for appointment {}", appointmentId);
        return saved;
    }

    /**
     * Cancels an appointment: the row is deleted and its slot reopened.
     *
     * @param appointmentId the appointment to cancel
     * @param userEmail     the logged-in user's email
     */
    @Transactional
    public void cancel(Long appointmentId, String userEmail) {
        log.info("Entering cancel() for appointment {} by {}", appointmentId, userEmail);
        Appointment appointment = findForUser(appointmentId, userEmail);
        requireChangeable(appointment);
        TimeSlot slot = appointment.getTimeSlot();
        appointmentRepository.delete(appointment);
        appointmentRepository.flush();
        slot.setAvailable(true);
        timeSlotRepository.save(slot);
        log.info("Exiting cancel() - slot {} reopened", slot.getId());
    }

    /**
     * Marks a past or current appointment as completed (admin only).
     *
     * @param appointmentId the appointment id
     */
    @Transactional
    public void markCompleted(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
        log.info("Appointment {} marked completed", appointmentId);
    }

    /**
     * Loads an appointment, checking that the user owns it or is an admin.
     *
     * @param appointmentId the appointment id
     * @param userEmail     the logged-in user's email
     * @return the appointment
     * @throws AccessDeniedBusinessException if a patient asks for someone else's appointment
     */
    @Transactional(readOnly = true)
    public Appointment findForUser(Long appointmentId, String userEmail) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        User user = userService.findByEmail(userEmail);
        boolean owner = appointment.getPatient().getId().equals(user.getId());
        if (!owner && user.getRole() != Role.ADMIN) {
            log.warn("User {} tried to access appointment {} owned by someone else", userEmail, appointmentId);
            throw new AccessDeniedBusinessException("You can only view or change your own appointments.");
        }
        return appointment;
    }

    /**
     * @param patientEmail the logged-in patient's email
     * @return the patient's appointments
     */
    @Transactional(readOnly = true)
    public List<Appointment> findForPatient(String patientEmail) {
        User patient = userService.findByEmail(patientEmail);
        return appointmentRepository.findByPatientIdOrdered(patient.getId());
    }

    /**
     * @param patientEmail the logged-in patient's email
     * @return the patient's upcoming booked appointments
     */
    @Transactional(readOnly = true)
    public List<Appointment> findUpcomingForPatient(String patientEmail) {
        LocalDateTime now = LocalDateTime.now(clock);
        return findForPatient(patientEmail).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.BOOKED && !a.getTimeSlot().isInPast(now))
                .toList();
    }

    /** @return all appointments (admin view) */
    @Transactional(readOnly = true)
    public List<Appointment> findAll() {
        return appointmentRepository.findAllOrdered();
    }

    /**
     * @param date the day
     * @return appointments on that day (office schedule)
     */
    @Transactional(readOnly = true)
    public List<Appointment> findByDate(LocalDate date) {
        return appointmentRepository.findByDate(date);
    }

    /** @return today's date in the office time zone */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    // ---------------------------------------------------------------- helpers

    private VisitType requireActiveVisitType(Long visitTypeId) {
        VisitType visitType = visitTypeService.findById(visitTypeId);
        if (!visitType.isActive()) {
            throw new BookingException("That visit type is no longer offered. Please choose another.");
        }
        return visitType;
    }

    private TimeSlot lockOpenFutureSlot(Long slotId) {
        TimeSlot slot = timeSlotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot", slotId));
        if (slot.isInPast(LocalDateTime.now(clock))) {
            throw new BookingException("That time has already passed. Please choose a future time.");
        }
        if (!slot.isAvailable() || appointmentRepository.existsByTimeSlotId(slotId)) {
            throw new BookingException("Sorry, that time slot was just taken. Please choose another time.");
        }
        return slot;
    }

    private void requireChangeable(Appointment appointment) {
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BookingException("Completed appointments cannot be changed.");
        }
        if (appointment.getTimeSlot().isInPast(LocalDateTime.now(clock))) {
            throw new BookingException("Past appointments cannot be changed.");
        }
    }

    private Appointment saveCatchingDoubleBooking(Appointment appointment) {
        try {
            return appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException e) {
            // UNIQUE key on appointments.slot_id caught a race the service check missed
            log.warn("Double booking blocked by database constraint: {}", e.getMostSpecificCause().getMessage());
            throw new BookingException("Sorry, that time slot was just taken. Please choose another time.");
        }
    }

    private String cleanNotes(String notes) {
        return (notes == null || notes.isBlank()) ? null : notes.trim();
    }
}
