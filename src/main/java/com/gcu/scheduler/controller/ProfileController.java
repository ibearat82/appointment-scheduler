package com.gcu.scheduler.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gcu.scheduler.dto.ProfileForm;
import com.gcu.scheduler.model.User;
import com.gcu.scheduler.service.UserService;

import jakarta.validation.Valid;

/**
 * My Profile page for the logged-in user.
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    /**
     * @param userService user service (injected)
     */
    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Shows the profile form.
     *
     * @param principal the logged-in user
     * @param model     view model
     * @return the profile view
     */
    @GetMapping
    public String show(Principal principal, Model model) {
        User user = userService.findByEmail(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("form", userService.toProfileForm(user));
        return "profile";
    }

    /**
     * Saves profile changes.
     *
     * @param form      submitted profile form
     * @param result    validation results
     * @param principal the logged-in user
     * @param model     view model
     * @param redirect  flash attributes
     * @return the profile page
     */
    @PostMapping
    public String update(@Valid @ModelAttribute("form") ProfileForm form, BindingResult result,
                         Principal principal, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("user", userService.findByEmail(principal.getName()));
            return "profile";
        }
        userService.updateProfile(principal.getName(), form);
        redirect.addFlashAttribute("success", "Profile updated.");
        return "redirect:/profile";
    }
}
