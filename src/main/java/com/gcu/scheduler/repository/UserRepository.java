package com.gcu.scheduler.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gcu.scheduler.model.Role;
import com.gcu.scheduler.model.User;

/**
 * Data access object (DAO) for {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * @param email the login email (case-insensitive)
     * @return the matching user, if any
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * @param email the email to check
     * @return true if an account already uses the email
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * @param role the role to count
     * @return the number of users with the role
     */
    long countByRole(Role role);
}
