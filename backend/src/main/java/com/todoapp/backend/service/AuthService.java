package com.todoapp.backend.service;

import com.todoapp.backend.dto.request.RegisterRequest;
import com.todoapp.backend.dto.request.LoginRequest;
import com.todoapp.backend.dto.response.AuthResponse;

/**
 * Authentication service interface
 * Handles user registration and login
 */
public interface AuthService {

    /**
     * Register a new user
     * @param registerRequest the registration request with username, email, password
     * @return AuthResponse with JWT token
     * @throws com.todoapp.backend.exception.BadRequestException if validation fails or user already exists
     */
    AuthResponse register(RegisterRequest registerRequest);

    /**
     * Login user with username or email
     * @param loginRequest the login request with usernameOrEmail and password
     * @return AuthResponse with JWT token
     * @throws com.todoapp.backend.exception.BadRequestException if credentials are invalid
     */
    AuthResponse login(LoginRequest loginRequest);
}

