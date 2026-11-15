package com.gcu.scheduler.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.gcu.scheduler.service.VisitTypeService;

/**
 * Public home page, role-based dashboard redirect, and the access-denied page.
 */
@Controller
public class HomeController {

    private final VisitTypeService visitTypeService;

    /**
     * @param visitTypeService visit type service (injected)
     */
    public HomeController(VisitTypeService visitTypeService) {
        this.visitTypeService = visitTypeService;
    }

    /**
     * Home page: shows the visit types the office offers.
     *
     * @param model view model
     * @return the home view
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("visitTypes", visitTypeService.findActive());
        return "index";
    }

    /**
     * Sends a logged-in user to the dashboard for their role.
     *
     * @param authentication the logged-in user
     * @return a redirect to the admin or patient dashboard
     */
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return admin ? "redirect:/admin" : "redirect:/patient/dashboard";
    }

    /**
     * Access denied page.
     *
     * @param model view model
     * @return the error view
     */
    @GetMapping("/error/403")
    public String accessDenied(Model model) {
        model.addAttribute("status", 403);
        model.addAttribute("message", "You do not have permission to view that page.");
        return "error";
    }
}
