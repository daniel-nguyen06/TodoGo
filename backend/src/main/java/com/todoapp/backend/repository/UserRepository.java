package com.todoapp.backend.repository;

import com.todoapp.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     * @param username the username
     * @return Optional of User if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     * @param email the email
     * @return Optional of User if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user exists by username
     * @param username the username
     * @return true if user exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Check if user exists by email
     * @param email the email
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);
}

