package com.todoapp.backend.controller;

import com.todoapp.backend.dto.request.LoginRequest;
import com.todoapp.backend.dto.request.RegisterRequest;
import com.todoapp.backend.dto.response.ApiResponse;
import com.todoapp.backend.dto.response.AuthResponse;
import com.todoapp.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);

        // Optionally provide location header for created user resource (not exposing user endpoint here)
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/register").build().toUri();

        ApiResponse<AuthResponse> body = ApiResponse.success("User registered successfully", authResponse);
        return ResponseEntity.created(location).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        ApiResponse<AuthResponse> body = ApiResponse.success("Login successful", authResponse);
        return ResponseEntity.ok(body);
    }
}

