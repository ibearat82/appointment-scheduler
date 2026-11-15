package com.gcu.scheduler.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.gcu.scheduler.model.Role;
import com.gcu.scheduler.model.User;
import com.gcu.scheduler.repository.UserRepository;

/**
 * Makes sure an office-staff (admin) account exists at startup so the admin
 * pages can be reached on a fresh database. Credentials come from
 * application.properties / environment variables.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    /**
     * @param userRepository  user DAO (injected)
     * @param passwordEncoder BCrypt encoder (injected)
     * @param adminEmail      default admin email (app.admin.email)
     * @param adminPassword   default admin password (app.admin.password)
     */
    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           @Value("${app.admin.email}") String adminEmail,
                           @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.countByRole(Role.ADMIN) > 0) {
            return;
        }
        User admin = new User();
        admin.setFirstName("Office");
        admin.setLastName("Admin");
        admin.setEmail(adminEmail);
        admin.setPhone("602-555-0100");
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Created default admin account {}", adminEmail);
    }
}
