package com.gcu.scheduler.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gcu.scheduler.dto.BookingForm;
import com.gcu.scheduler.exception.BookingException;
import com.gcu.scheduler.model.Appointment;
import com.gcu.scheduler.service.AppointmentService;
import com.gcu.scheduler.service.TimeSlotService;
import com.gcu.scheduler.service.UserService;
import com.gcu.scheduler.service.VisitTypeService;

import jakarta.validation.Valid;

/**
 * Patient pages: dashboard, book, my appointments, reschedule, cancel.
 */
@Controller
@RequestMapping("/patient")
public class AppointmentController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentController.class);

    private final AppointmentService appointmentService;
    private final TimeSlotService timeSlotService;
    private final VisitTypeService visitTypeService;
    private final UserService userService;

    /**
     * @param appointmentService appointment service (injected)
     * @param timeSlotService    time slot service (injected)
     * @param visitTypeService   visit type service (injected)
     * @param userService        user service (injected)
     */
    public AppointmentController(AppointmentService appointmentService, TimeSlotService timeSlotService,
                                 VisitTypeService visitTypeService, UserService userService) {
        this.appointmentService = appointmentService;
        this.timeSlotService = timeSlotService;
        this.visitTypeService = visitTypeService;
        this.userService = userService;
    }

    /**
     * Patient dashboard with upcoming appointments.
     *
     * @param principal the logged-in user
     * @param model     view model
     * @return the dashboard view
     */
    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        model.addAttribute("user", userService.findByEmail(principal.getName()));
        model.addAttribute("upcoming", appointmentService.findUpcomingForPatient(principal.getName()));
        return "patient/dashboard";
    }

    /**
     * Booking page: pick a day, then a visit type and an open time.
     *
     * @param date  the selected day (optional; defaults to the first day with openings)
     * @param model view model
     * @return the booking view
     */
    @GetMapping("/book")
    public String showBook(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           Model model) {
        model.addAttribute("form", new BookingForm());
        populateSlotChoices(model, date);
        return "patient/book";
    }

    /**
     * Books the selected slot.
     *
     * @param date      the day being viewed (to redisplay on error)
     * @param form      submitted booking form
     * @param result    validation results
     * @param principal the logged-in user
     * @param model     view model
     * @param redirect  flash attributes
     * @return the confirmation page, or the booking page with errors
     */
    @PostMapping("/book")
    public String book(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @Valid @ModelAttribute("form") BookingForm form, BindingResult result,
                       Principal principal, Model model, RedirectAttributes redirect) {
        log.info("Entering book() for {}", principal.getName());
        if (result.hasErrors()) {
            populateSlotChoices(model, date);
            return "patient/book";
        }
        try {
            Appointment appointment = appointmentService.book(principal.getName(), form);
            redirect.addFlashAttribute("success", "Your appointment is booked.");
            log.info("Exiting book() - appointment {}", appointment.getId());
            return "redirect:/patient/appointments/" + appointment.getId();
        } catch (BookingException e) {
            model.addAttribute("error", e.getMessage());
            populateSlotChoices(model, date);
            return "patient/book";
        }
    }

    /**
     * Lists the patient's appointments.
     *
     * @param principal the logged-in user
     * @param model     view model
     * @return the my-appointments view
     */
    @GetMapping("/appointments")
    public String myAppointments(Principal principal, Model model) {
        model.addAttribute("appointments", appointmentService.findForPatient(principal.getName()));
        model.addAttribute("today", appointmentService.today());
        return "patient/appointments";
    }

    /**
     * Appointment details / booking confirmation.
     *
     * @param id        the appointment id
     * @param principal the logged-in user
     * @param model     view model
     * @return the confirmation view
     */
    @GetMapping("/appointments/{id}")
    public String details(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("appointment", appointmentService.findForUser(id, principal.getName()));
        return "patient/confirmation";
    }

    /**
     * Reschedule page, pre-filled with the current choices.
     *
     * @param id        the appointment id
     * @param date      the day to show openings for (optional)
     * @param principal the logged-in user
     * @param model     view model
     * @return the edit view
     */
    @GetMapping("/appointments/{id}/edit")
    public String showEdit(@PathVariable Long id,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           Principal principal, Model model) {
        Appointment appointment = appointmentService.findForUser(id, principal.getName());
        BookingForm form = new BookingForm();
        form.setVisitTypeId(appointment.getVisitType().getId());
        form.setSlotId(appointment.getTimeSlot().getId());
        form.setNotes(appointment.getNotes());
        model.addAttribute("form", form);
        model.addAttribute("appointment", appointment);
        populateSlotChoices(model, date != null ? date : appointment.getTimeSlot().getSlotDate());
        return "patient/edit";
    }

    /**
     * Saves a reschedule.
     *
     * @param id        the appointment id
     * @param date      the day being viewed (to redisplay on error)
     * @param form      submitted booking form
     * @param result    validation results
     * @param principal the logged-in user
     * @param model     view model
     * @param redirect  flash attributes
     * @return my appointments on success, or the edit page with errors
     */
    @PostMapping("/appointments/{id}/edit")
    public String edit(@PathVariable Long id,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @Valid @ModelAttribute("form") BookingForm form, BindingResult result,
                       Principal principal, Model model, RedirectAttributes redirect) {
        log.info("Entering edit() for appointment {}", id);
        Appointment appointment = appointmentService.findForUser(id, principal.getName());
        if (!result.hasErrors()) {
            try {
                appointmentService.reschedule(id, principal.getName(), form);
                redirect.addFlashAttribute("success", "Your appointment was updated.");
                log.info("Exiting edit() for appointment {}", id);
                return "redirect:/patient/appointments";
            } catch (BookingException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        model.addAttribute("appointment", appointment);
        populateSlotChoices(model, date != null ? date : appointment.getTimeSlot().getSlotDate());
        return "patient/edit";
    }

    /**
     * Cancels an appointment (the page asks for confirmation first).
     *
     * @param id        the appointment id
     * @param principal the logged-in user
     * @param redirect  flash attributes
     * @return my appointments
     */
    @PostMapping("/appointments/{id}/cancel")
    public String cancel(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        log.info("Entering cancel() for appointment {}", id);
        try {
            appointmentService.cancel(id, principal.getName());
            redirect.addFlashAttribute("success", "Your appointment was cancelled.");
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/patient/appointments";
    }

    /** Adds visit types, days with openings, the selected day, and its open slots. */
    private void populateSlotChoices(Model model, LocalDate requestedDate) {
        List<LocalDate> days = timeSlotService.findDaysWithOpenSlots();
        LocalDate selected = requestedDate != null ? requestedDate : (days.isEmpty() ? null : days.get(0));
        model.addAttribute("visitTypes", visitTypeService.findActive());
        model.addAttribute("days", days);
        model.addAttribute("selectedDate", selected);
        model.addAttribute("slots", selected == null ? List.of() : timeSlotService.findOpenSlots(selected));
    }
}
