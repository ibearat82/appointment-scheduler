package com.gcu.scheduler.controller;

import java.security.Principal;
import java.time.LocalDate;

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

import com.gcu.scheduler.dto.SlotGenerationForm;
import com.gcu.scheduler.exception.BookingException;
import com.gcu.scheduler.model.VisitType;
import com.gcu.scheduler.service.AppointmentService;
import com.gcu.scheduler.service.TimeSlotService;
import com.gcu.scheduler.service.UserService;
import com.gcu.scheduler.service.VisitTypeService;

import jakarta.validation.Valid;

/**
 * Office staff (ROLE_ADMIN) pages: dashboard, visit types, time slots, all appointments.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

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
    public AdminController(AppointmentService appointmentService, TimeSlotService timeSlotService,
                           VisitTypeService visitTypeService, UserService userService) {
        this.appointmentService = appointmentService;
        this.timeSlotService = timeSlotService;
        this.visitTypeService = visitTypeService;
        this.userService = userService;
    }

    /**
     * Admin dashboard: today's schedule and quick counts.
     *
     * @param model view model
     * @return the admin dashboard view
     */
    @GetMapping
    public String dashboard(Model model) {
        LocalDate today = appointmentService.today();
        model.addAttribute("today", today);
        model.addAttribute("todaysAppointments", appointmentService.findByDate(today));
        model.addAttribute("patientCount", userService.countPatients());
        model.addAttribute("openDays", timeSlotService.findDaysWithOpenSlots().size());
        model.addAttribute("visitTypeCount", visitTypeService.findActive().size());
        return "admin/dashboard";
    }

    // ------------------------------------------------------------ visit types

    /**
     * Lists all visit types.
     *
     * @param model view model
     * @return the visit types view
     */
    @GetMapping("/visit-types")
    public String visitTypes(Model model) {
        model.addAttribute("visitTypes", visitTypeService.findAll());
        return "admin/visit-types";
    }

    /**
     * Blank form for a new visit type.
     *
     * @param model view model
     * @return the visit type form view
     */
    @GetMapping("/visit-types/new")
    public String newVisitType(Model model) {
        model.addAttribute("visitType", new VisitType());
        return "admin/visit-type-form";
    }

    /**
     * Form to edit an existing visit type.
     *
     * @param id    the visit type id
     * @param model view model
     * @return the visit type form view
     */
    @GetMapping("/visit-types/{id}/edit")
    public String editVisitType(@PathVariable Long id, Model model) {
        model.addAttribute("visitType", visitTypeService.findById(id));
        return "admin/visit-type-form";
    }

    /**
     * Creates or updates a visit type.
     *
     * @param visitType submitted visit type
     * @param result    validation results
     * @param redirect  flash attributes
     * @return the visit type list, or the form with errors
     */
    @PostMapping("/visit-types/save")
    public String saveVisitType(@Valid @ModelAttribute("visitType") VisitType visitType, BindingResult result,
                                RedirectAttributes redirect) {
        log.info("Entering saveVisitType() for '{}'", visitType.getName());
        if (result.hasErrors()) {
            return "admin/visit-type-form";
        }
        try {
            visitTypeService.save(visitType);
        } catch (BookingException e) {
            result.rejectValue("name", "duplicate", e.getMessage());
            return "admin/visit-type-form";
        }
        redirect.addFlashAttribute("success", "Visit type saved.");
        return "redirect:/admin/visit-types";
    }

    /**
     * Deletes a visit type, or deactivates it if it has appointments.
     *
     * @param id       the visit type id
     * @param redirect flash attributes
     * @return the visit type list
     */
    @PostMapping("/visit-types/{id}/delete")
    public String deleteVisitType(@PathVariable Long id, RedirectAttributes redirect) {
        boolean deleted = visitTypeService.deleteOrDeactivate(id);
        redirect.addFlashAttribute("success", deleted
                ? "Visit type deleted."
                : "Visit type has appointments, so it was deactivated instead of deleted.");
        return "redirect:/admin/visit-types";
    }

    // ------------------------------------------------------------- time slots

    /**
     * Lists upcoming slots and shows the slot generation form.
     *
     * @param model view model
     * @return the time slots view
     */
    @GetMapping("/slots")
    public String slots(Model model) {
        SlotGenerationForm form = new SlotGenerationForm();
        form.setDate(appointmentService.today().plusDays(1));
        model.addAttribute("form", form);
        model.addAttribute("slots", timeSlotService.findUpcomingSlots());
        return "admin/slots";
    }

    /**
     * Opens new slots for a day.
     *
     * @param form     submitted generation form
     * @param result   validation results
     * @param model    view model
     * @param redirect flash attributes
     * @return the time slots page
     */
    @PostMapping("/slots/generate")
    public String generateSlots(@Valid @ModelAttribute("form") SlotGenerationForm form, BindingResult result,
                                Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                int created = timeSlotService.generateSlots(form);
                redirect.addFlashAttribute("success", created + " time slot(s) opened for " + form.getDate() + ".");
                return "redirect:/admin/slots";
            } catch (BookingException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        model.addAttribute("slots", timeSlotService.findUpcomingSlots());
        return "admin/slots";
    }

    /**
     * Deletes an unbooked slot.
     *
     * @param id       the slot id
     * @param redirect flash attributes
     * @return the time slots page
     */
    @PostMapping("/slots/{id}/delete")
    public String deleteSlot(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            timeSlotService.deleteSlot(id);
            redirect.addFlashAttribute("success", "Time slot removed.");
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/slots";
    }

    // ----------------------------------------------------------- appointments

    /**
     * Lists all appointments, or one day's if a date is given.
     *
     * @param date  optional day filter
     * @param model view model
     * @return the all-appointments view
     */
    @GetMapping("/appointments")
    public String appointments(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               Model model) {
        model.addAttribute("date", date);
        model.addAttribute("appointments", date == null
                ? appointmentService.findAll()
                : appointmentService.findByDate(date));
        return "admin/appointments";
    }

    /**
     * Cancels any appointment.
     *
     * @param id        the appointment id
     * @param principal the logged-in admin
     * @param redirect  flash attributes
     * @return the all-appointments page
     */
    @PostMapping("/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            appointmentService.cancel(id, principal.getName());
            redirect.addFlashAttribute("success", "Appointment cancelled and slot reopened.");
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/appointments";
    }

    /**
     * Marks an appointment as completed.
     *
     * @param id       the appointment id
     * @param redirect flash attributes
     * @return the all-appointments page
     */
    @PostMapping("/appointments/{id}/complete")
    public String completeAppointment(@PathVariable Long id, RedirectAttributes redirect) {
        appointmentService.markCompleted(id);
        redirect.addFlashAttribute("success", "Appointment marked completed.");
        return "redirect:/admin/appointments";
    }
}
