package com.huzaifa.rescueroute.service;

import com.huzaifa.rescueroute.dto.AuthResponseDto;
import com.huzaifa.rescueroute.dto.LoginRequestDto;
import com.huzaifa.rescueroute.dto.RegisterRequestDto;
import com.huzaifa.rescueroute.dto.UserResponseDto;

/**
 * Service contract for user registration, authentication, and session identity retrieval.
 */
public interface AuthService {

    /**
     * Registers a new user account with hashed password and returns an authentication token.
     *
     * @param request Registration payload containing name, email, password, and optional role
     * @return AuthResponseDto containing JWT token and safe user details
     */
    AuthResponseDto register(RegisterRequestDto request);

    /**
     * Authenticates existing user credentials and generates a signed JWT.
     *
     * @param request Login credentials (email, password)
     * @return AuthResponseDto containing JWT token and safe user details
     */
    AuthResponseDto login(LoginRequestDto request);

    /**
     * Retrieves current authenticated user profile by email.
     *
     * @param email Authenticated user email
     * @return UserResponseDto safe profile details
     */
    UserResponseDto getCurrentUser(String email);
}
