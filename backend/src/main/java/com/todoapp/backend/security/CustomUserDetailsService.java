package com.todoapp.backend.security;

import com.todoapp.backend.entity.User;
import com.todoapp.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Custom implementation of UserDetailsService
 * Loads user details from database by username or email
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Load user by username or email
     * @param usernameOrEmail the username or email
     * @return UserDetails
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Try to find by username first
        User user = userRepository.findByUsername(usernameOrEmail)
                .orElseGet(() ->
                        // If not found by username, try by email
                        userRepository.findByEmail(usernameOrEmail)
                                .orElseThrow(() ->
                                        new UsernameNotFoundException(
                                                "User not found with username or email: " + usernameOrEmail)));

        // Build UserDetails object
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    /**
     * Load user details by user ID (helper method)
     * @param userId the user ID
     * @return User entity
     * @throws Exception if user not found
     */
    public User loadUserById(Long userId) throws Exception {
        return userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found with id: " + userId));
    }
}

