package com.huzaifa.rescueroute.service;

import com.huzaifa.rescueroute.common.exception.ResourceNotFoundException;
import com.huzaifa.rescueroute.common.security.JwtService;
import com.huzaifa.rescueroute.dto.AuthResponseDto;
import com.huzaifa.rescueroute.dto.LoginRequestDto;
import com.huzaifa.rescueroute.dto.RegisterRequestDto;
import com.huzaifa.rescueroute.dto.UserResponseDto;
import com.huzaifa.rescueroute.entity.User;
import com.huzaifa.rescueroute.entity.UserRole;
import com.huzaifa.rescueroute.repository.UserRepository;
import com.huzaifa.rescueroute.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Test Citizen")
                .email("citizen@rescueroute.local")
                .passwordHash("$2a$10$encodedPasswordHash")
                .role(UserRole.USER)
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Register new user successfully and hash password")
    void testRegister_Success() {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .fullName("New Doctor")
                .email("doctor@rescueroute.local")
                .password("securePassword123")
                .role(UserRole.PROVIDER)
                .build();

        when(userRepository.existsByEmail("doctor@rescueroute.local")).thenReturn(false);
        when(passwordEncoder.encode("securePassword123")).thenReturn("$2a$10$hashedSecurePassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateToken(eq("doctor@rescueroute.local"), any(UUID.class), eq(UserRole.PROVIDER)))
                .thenReturn("mocked.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        AuthResponseDto response = authService.register(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("doctor@rescueroute.local", response.getUser().getEmail());
        assertEquals(UserRole.PROVIDER, response.getUser().getRole());
        verify(passwordEncoder).encode("securePassword123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Reject registration if email already exists")
    void testRegister_DuplicateEmail() {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .fullName("Duplicate User")
                .email("citizen@rescueroute.local")
                .password("password123")
                .build();

        when(userRepository.existsByEmail("citizen@rescueroute.local")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(request));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Reject self-registration with ADMIN role")
    void testRegister_RejectAdminSelfPromotion() {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .fullName("Attacker")
                .email("hacker@rescueroute.local")
                .password("password123")
                .role(UserRole.ADMIN)
                .build();

        when(userRepository.existsByEmail("hacker@rescueroute.local")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(request));

        assertTrue(ex.getMessage().contains("ADMIN is not permitted"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login with valid credentials returns JWT")
    void testLogin_Success() {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("citizen@rescueroute.local")
                .password("password123")
                .build();

        when(userRepository.findByEmail("citizen@rescueroute.local")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", sampleUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(sampleUser.getEmail(), sampleUser.getId(), sampleUser.getRole()))
                .thenReturn("mocked.login.jwt");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        AuthResponseDto response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked.login.jwt", response.getToken());
        assertEquals(sampleUser.getEmail(), response.getUser().getEmail());
    }

    @Test
    @DisplayName("Login with incorrect password throws BadCredentialsException")
    void testLogin_WrongPassword() {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("citizen@rescueroute.local")
                .password("wrongPassword")
                .build();

        when(userRepository.findByEmail("citizen@rescueroute.local")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPassword", sampleUser.getPasswordHash())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Login with non-existent email throws BadCredentialsException")
    void testLogin_NonExistentEmail() {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("nonexistent@rescueroute.local")
                .password("password123")
                .build();

        when(userRepository.findByEmail("nonexistent@rescueroute.local")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Login with deactivated user throws DisabledException")
    void testLogin_DeactivatedUser() {
        sampleUser.setActive(false);
        LoginRequestDto request = LoginRequestDto.builder()
                .email("citizen@rescueroute.local")
                .password("password123")
                .build();

        when(userRepository.findByEmail("citizen@rescueroute.local")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", sampleUser.getPasswordHash())).thenReturn(true);

        assertThrows(DisabledException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Get current user profile by email")
    void testGetCurrentUser_Success() {
        when(userRepository.findByEmail("citizen@rescueroute.local")).thenReturn(Optional.of(sampleUser));

        UserResponseDto user = authService.getCurrentUser("citizen@rescueroute.local");

        assertNotNull(user);
        assertEquals(sampleUser.getId(), user.getId());
        assertEquals("Test Citizen", user.getFullName());
        assertEquals("citizen@rescueroute.local", user.getEmail());
    }

    @Test
    @DisplayName("Get current user profile not found throws ResourceNotFoundException")
    void testGetCurrentUser_NotFound() {
        when(userRepository.findByEmail("unknown@rescueroute.local")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.getCurrentUser("unknown@rescueroute.local"));
    }
}
