package com.gcu.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gcu.scheduler.dto.BookingForm;
import com.gcu.scheduler.exception.AccessDeniedBusinessException;
import com.gcu.scheduler.exception.BookingException;
import com.gcu.scheduler.model.Appointment;
import com.gcu.scheduler.model.Role;
import com.gcu.scheduler.model.TimeSlot;
import com.gcu.scheduler.model.User;
import com.gcu.scheduler.model.VisitType;
import com.gcu.scheduler.repository.AppointmentRepository;
import com.gcu.scheduler.repository.TimeSlotRepository;

/**
 * Unit tests for the booking rules in {@link AppointmentService}.
 * Repositories are mocked and the clock is fixed at Nov 16, 2026 8:00 AM.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Phoenix");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 8, 0);

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private TimeSlotRepository timeSlotRepository;
    @Mock private VisitTypeService visitTypeService;
    @Mock private UserService userService;

    private AppointmentService service;
    private User patient;
    private VisitType checkup;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        service = new AppointmentService(appointmentRepository, timeSlotRepository, visitTypeService, userService, clock);

        patient = new User();
        patient.setId(1L);
        patient.setEmail("patient@clinic.test");
        patient.setRole(Role.PATIENT);

        checkup = new VisitType();
        checkup.setId(10L);
        checkup.setName("Annual Checkup");
        checkup.setActive(true);
    }

    private TimeSlot slot(long id, LocalDate date, int hour, boolean available) {
        TimeSlot s = new TimeSlot();
        s.setId(id);
        s.setSlotDate(date);
        s.setStartTime(LocalTime.of(hour, 0));
        s.setEndTime(LocalTime.of(hour, 30));
        s.setAvailable(available);
        return s;
    }

    private BookingForm form(long slotId) {
        BookingForm f = new BookingForm();
        f.setVisitTypeId(10L);
        f.setSlotId(slotId);
        return f;
    }

    @Test
    void bookOpenFutureSlotSucceedsAndClosesSlot() {
        TimeSlot open = slot(5L, NOW.toLocalDate(), 10, true);
        when(userService.findByEmail("patient@clinic.test")).thenReturn(patient);
        when(visitTypeService.findById(10L)).thenReturn(checkup);
        when(timeSlotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(open));
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        Appointment result = service.book("patient@clinic.test", form(5L));

        assertEquals(open, result.getTimeSlot());
        assertFalse(open.isAvailable());
    }

    @Test
    void bookTakenSlotIsRejected() {
        TimeSlot taken = slot(5L, NOW.toLocalDate(), 10, false);
        when(userService.findByEmail("patient@clinic.test")).thenReturn(patient);
        when(visitTypeService.findById(10L)).thenReturn(checkup);
        when(timeSlotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(taken));

        assertThrows(BookingException.class, () -> service.book("patient@clinic.test", form(5L)));
        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void bookPastSlotIsRejected() {
        TimeSlot past = slot(6L, NOW.toLocalDate().minusDays(1), 10, true);
        when(userService.findByEmail("patient@clinic.test")).thenReturn(patient);
        when(visitTypeService.findById(10L)).thenReturn(checkup);
        when(timeSlotRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(past));

        BookingException ex = assertThrows(BookingException.class, () -> service.book("patient@clinic.test", form(6L)));
        assertTrue(ex.getMessage().contains("passed"));
    }

    @Test
    void bookInactiveVisitTypeIsRejected() {
        checkup.setActive(false);
        when(userService.findByEmail("patient@clinic.test")).thenReturn(patient);
        when(visitTypeService.findById(10L)).thenReturn(checkup);

        assertThrows(BookingException.class, () -> service.book("patient@clinic.test", form(5L)));
    }

    @Test
    void patientCannotOpenAnotherPatientsAppointment() {
        User other = new User();
        other.setId(2L);
        Appointment theirs = new Appointment();
        theirs.setId(99L);
        theirs.setPatient(other);
        when(appointmentRepository.findById(99L)).thenReturn(Optional.of(theirs));
        when(userService.findByEmail("patient@clinic.test")).thenReturn(patient);

        assertThrows(AccessDeniedBusinessException.class, () -> service.findForUser(99L, "patient@clinic.test"));
    }

    @Test
    void cancelReopensTheSlot() {
        TimeSlot booked = slot(7L, NOW.toLocalDate().plusDays(2), 9, false);
        Appointment mine = new Appointment();
        mine.setId(50L);
        mine.setPatient(patient);
        mine.setTimeSlot(booked);
        when(appointmentRepository.findById(50L)).thenReturn(Optional.of(mine));
        when(userService.findByEmail("patient@clinic.test")).thenReturn(patient);

        service.cancel(50L, "patient@clinic.test");

        verify(appointmentRepository).delete(mine);
        assertTrue(booked.isAvailable());
    }
}
