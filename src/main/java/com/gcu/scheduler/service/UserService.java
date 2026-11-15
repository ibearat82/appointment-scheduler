package com.gcu.scheduler.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gcu.scheduler.dto.ProfileForm;
import com.gcu.scheduler.dto.RegistrationForm;
import com.gcu.scheduler.exception.DuplicateEmailException;
import com.gcu.scheduler.exception.ResourceNotFoundException;
import com.gcu.scheduler.model.Role;
import com.gcu.scheduler.model.User;
import com.gcu.scheduler.repository.UserRepository;

/**
 * Business service for user accounts: registration, profile updates, and
 * loading users for Spring Security authentication.
 */
@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param userRepository  user DAO (injected)
     * @param passwordEncoder BCrypt encoder (injected)
     */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Loads a user by email for Spring Security form login.
     *
     * @param email the email entered on the login page
     * @return Spring Security user details with the user's role
     * @throws UsernameNotFoundException if no account uses the email
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }

    /**
     * Creates a new patient account with a BCrypt-hashed password.
     *
     * @param form validated registration form
     * @return the saved user
     * @throws DuplicateEmailException if the email is already registered
     */
    @Transactional
    public User registerPatient(RegistrationForm form) {
        log.info("Entering registerPatient() for {}", form.getEmail());
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }
        User user = new User();
        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setEmail(email);
        user.setPhone(form.getPhone().trim());
        user.setDateOfBirth(form.getDateOfBirth());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.PATIENT);
        User saved = userRepository.save(user);
        log.info("Exiting registerPatient() - created user id {}", saved.getId());
        return saved;
    }

    /**
     * @param email the logged-in user's email
     * @return the user
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }

    /**
     * Builds a profile form pre-filled with the user's current details.
     *
     * @param user the user
     * @return a filled profile form
     */
    public ProfileForm toProfileForm(User user) {
        ProfileForm form = new ProfileForm();
        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setPhone(user.getPhone());
        form.setDateOfBirth(user.getDateOfBirth());
        return form;
    }

    /**
     * Updates the logged-in user's name, phone, and date of birth.
     *
     * @param email the logged-in user's email
     * @param form  validated profile form
     * @return the updated user
     */
    @Transactional
    public User updateProfile(String email, ProfileForm form) {
        log.info("Entering updateProfile() for {}", email);
        User user = findByEmail(email);
        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setPhone(form.getPhone().trim());
        user.setDateOfBirth(form.getDateOfBirth());
        User saved = userRepository.save(user);
        log.info("Exiting updateProfile() for {}", email);
        return saved;
    }

    /** @return number of patient accounts (admin dashboard) */
    @Transactional(readOnly = true)
    public long countPatients() {
        return userRepository.countByRole(Role.PATIENT);
    }

    /** @return all users (admin view) */
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }
}
