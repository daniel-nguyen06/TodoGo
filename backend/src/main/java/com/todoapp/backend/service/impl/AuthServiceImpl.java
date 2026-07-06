package com.todoapp.backend.service.impl;

import com.todoapp.backend.dto.request.RegisterRequest;
import com.todoapp.backend.dto.request.LoginRequest;
import com.todoapp.backend.dto.response.AuthResponse;
import com.todoapp.backend.entity.User;
import com.todoapp.backend.exception.BadRequestException;
import com.todoapp.backend.repository.UserRepository;
import com.todoapp.backend.security.JwtUtil;
import com.todoapp.backend.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of AuthService
 * Handles user registration and login with JWT token generation
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Register a new user
     * @param registerRequest the registration request
     * @return AuthResponse with JWT token
     * @throws BadRequestException if validation fails or user already exists
     */
    @Override
    public AuthResponse register(RegisterRequest registerRequest) {
        logger.info("Attempting to register user with username: {}", registerRequest.getUsername());

        // Validate request
        validateRegisterRequest(registerRequest);

        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            logger.warn("Username already exists: {}", registerRequest.getUsername());
            throw new BadRequestException("USERNAME_TAKEN", "Username is already taken");
        }

        // Check if email already exists
        if (registerRequest.getEmail() != null && !registerRequest.getEmail().isEmpty() &&
                userRepository.existsByEmail(registerRequest.getEmail())) {
            logger.warn("Email already exists: {}", registerRequest.getEmail());
            throw new BadRequestException("EMAIL_TAKEN", "Email is already taken");
        }

        // Create new user
        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Save user to database
        user = userRepository.save(user);
        logger.info("User registered successfully with username: {}", user.getUsername());

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        long expirationSeconds = jwtUtil.getTokenExpirationSeconds();

        // Build and return response
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expirationSeconds)
                .username(user.getUsername())
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    /**
     * Login user with username or email
     * @param loginRequest the login request
     * @return AuthResponse with JWT token
     * @throws BadRequestException if credentials are invalid
     */
    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        logger.info("Attempting login for: {}", loginRequest.getUsernameOrEmail());

        // Validate request
        validateLoginRequest(loginRequest);

        // Find user by username or email
        User user = userRepository.findByUsername(loginRequest.getUsernameOrEmail())
                .orElseGet(() ->
                        userRepository.findByEmail(loginRequest.getUsernameOrEmail())
                                .orElseThrow(() -> {
                                    logger.warn("Login failed - user not found: {}", loginRequest.getUsernameOrEmail());
                                    return new BadRequestException("INVALID_CREDENTIALS", "Invalid username or email");
                                }));

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            logger.warn("Login failed - invalid password for user: {}", user.getUsername());
            throw new BadRequestException("INVALID_CREDENTIALS", "Invalid password");
        }

        logger.info("User logged in successfully: {}", user.getUsername());

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        long expirationSeconds = jwtUtil.getTokenExpirationSeconds();

        // Build and return response (without password)
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expirationSeconds)
                .username(user.getUsername())
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    /**
     * Validate registration request
     * @param registerRequest the registration request
     * @throws BadRequestException if validation fails
     */
    private void validateRegisterRequest(RegisterRequest registerRequest) {
        // Check if passwords match
        if (!registerRequest.passwordsMatch()) {
            logger.warn("Password mismatch for registration");
            throw new BadRequestException("PASSWORD_MISMATCH", "Passwords do not match");
        }

        // Validate username (done by @NotBlank, @Size in DTO, but double-check)
        if (registerRequest.getUsername() == null || registerRequest.getUsername().trim().isEmpty()) {
            throw new BadRequestException("INVALID_USERNAME", "Username cannot be empty");
        }

        if (registerRequest.getUsername().length() < 3 || registerRequest.getUsername().length() > 100) {
            throw new BadRequestException("INVALID_USERNAME", "Username must be between 3 and 100 characters");
        }

        // Validate password
        if (registerRequest.getPassword() == null || registerRequest.getPassword().trim().isEmpty()) {
            throw new BadRequestException("INVALID_PASSWORD", "Password cannot be empty");
        }

        if (registerRequest.getPassword().length() < 6) {
            throw new BadRequestException("INVALID_PASSWORD", "Password must be at least 6 characters");
        }
    }

    /**
     * Validate login request
     * @param loginRequest the login request
     * @throws BadRequestException if validation fails
     */
    private void validateLoginRequest(LoginRequest loginRequest) {
        if (loginRequest.getUsernameOrEmail() == null || loginRequest.getUsernameOrEmail().trim().isEmpty()) {
            throw new BadRequestException("INVALID_INPUT", "Username or email cannot be empty");
        }

        if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
            throw new BadRequestException("INVALID_INPUT", "Password cannot be empty");
        }
    }
}

