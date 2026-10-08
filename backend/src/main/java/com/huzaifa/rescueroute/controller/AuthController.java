package com.huzaifa.rescueroute.controller;

import com.huzaifa.rescueroute.common.response.ApiResponse;
import com.huzaifa.rescueroute.dto.AuthResponseDto;
import com.huzaifa.rescueroute.dto.LoginRequestDto;
import com.huzaifa.rescueroute.dto.RegisterRequestDto;
import com.huzaifa.rescueroute.dto.UserResponseDto;
import com.huzaifa.rescueroute.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for user registration, login, and authenticated user identity.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDto>> register(
            @Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto authResponse = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", authResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto authResponse = authService.login(request);
        return ResponseEntity
                .ok(ApiResponse.success("Login successful", authResponse));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Full authentication is required"));
        }
        UserResponseDto user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity
                .ok(ApiResponse.success("User profile retrieved successfully", user));
    }
}
