package com.gcu.scheduler.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gcu.scheduler.dto.RegistrationForm;
import com.gcu.scheduler.exception.DuplicateEmailException;
import com.gcu.scheduler.service.UserService;

import jakarta.validation.Valid;

/**
 * Login and patient registration pages. The login POST itself is handled by Spring Security.
 */
@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    /**
     * @param userService user service (injected)
     */
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** @return the login view */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Shows the empty registration form.
     *
     * @param model view model
     * @return the register view
     */
    @GetMapping("/register")
    public String showRegister(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    /**
     * Validates the form and creates a patient account.
     *
     * @param form     submitted registration form
     * @param result   validation results
     * @param redirect flash attributes for the login page
     * @return the login page on success, or the form again with errors
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form, BindingResult result,
                           RedirectAttributes redirect) {
        log.info("Entering register() for {}", form.getEmail());
        if (result.hasErrors()) {
            return "register";
        }
        try {
            userService.registerPatient(form);
        } catch (DuplicateEmailException e) {
            result.rejectValue("email", "duplicate", e.getMessage());
            return "register";
        }
        redirect.addFlashAttribute("success", "Account created. Please log in.");
        log.info("Exiting register() - account created for {}", form.getEmail());
        return "redirect:/login";
    }
}
