package com.huzaifa.rescueroute.service.impl;

import com.huzaifa.rescueroute.common.exception.ResourceNotFoundException;
import com.huzaifa.rescueroute.common.security.JwtService;
import com.huzaifa.rescueroute.dto.AuthResponseDto;
import com.huzaifa.rescueroute.dto.LoginRequestDto;
import com.huzaifa.rescueroute.dto.RegisterRequestDto;
import com.huzaifa.rescueroute.dto.UserResponseDto;
import com.huzaifa.rescueroute.entity.User;
import com.huzaifa.rescueroute.entity.UserRole;
import com.huzaifa.rescueroute.repository.UserRepository;
import com.huzaifa.rescueroute.service.AuthService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of AuthService for secure registration, authentication, and JWT lifecycle.
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public AuthResponseDto register(RegisterRequestDto request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }

        // Enforce role assignment rules: unauthenticated clients cannot register as ADMIN
        UserRole assignedRole = request.getRole() != null ? request.getRole() : UserRole.USER;
        if (assignedRole == UserRole.ADMIN) {
            throw new IllegalArgumentException("Self-registration as ADMIN is not permitted.");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(
                savedUser.getEmail(),
                savedUser.getId(),
                savedUser.getRole()
        );

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .user(mapToUserResponseDto(savedUser))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        if (!user.isActive()) {
            throw new DisabledException("Your account has been deactivated. Please contact support.");
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getId(),
                user.getRole()
        );

        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .user(mapToUserResponseDto(user))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return mapToUserResponseDto(user);
    }

    private UserResponseDto mapToUserResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
