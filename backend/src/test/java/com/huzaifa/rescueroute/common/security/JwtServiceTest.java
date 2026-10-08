package com.huzaifa.rescueroute.common.security;

import com.huzaifa.rescueroute.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String testSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long testExpirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", testSecret);
        ReflectionTestUtils.setField(jwtService, "expirationMs", testExpirationMs);
    }

    @Test
    @DisplayName("Generate token and extract valid claims")
    void testGenerateTokenAndExtractClaims() {
        String email = "testuser@rescueroute.local";
        UUID userId = UUID.randomUUID();
        UserRole role = UserRole.PROVIDER;

        String token = jwtService.generateToken(email, userId, role);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(email, jwtService.extractEmail(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals(role.name(), jwtService.extractRole(token));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    @DisplayName("Validate token against UserDetails")
    void testIsTokenValid() {
        String email = "doctor@hospital.org";
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(email, userId, UserRole.USER);

        UserDetails userDetails = new User(email, "hashedPassword", Collections.emptyList());
        assertTrue(jwtService.isTokenValid(token, userDetails));

        UserDetails wrongUser = new User("other@hospital.org", "hashedPassword", Collections.emptyList());
        assertFalse(jwtService.isTokenValid(token, wrongUser));
    }

    @Test
    @DisplayName("Detect expired token")
    void testIsTokenExpired() {
        // Set negative expiration for immediate expiration
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);

        String email = "expired@rescueroute.local";
        UUID userId = UUID.randomUUID();
        String expiredToken = jwtService.generateToken(email, userId, UserRole.USER);

        assertTrue(jwtService.isTokenExpired(expiredToken));

        UserDetails userDetails = new User(email, "hashedPassword", Collections.emptyList());
        assertFalse(jwtService.isTokenValid(expiredToken, userDetails));
    }

    @Test
    @DisplayName("Reject malformed or tampered token")
    void testMalformedToken() {
        String malformedToken = "not.a.valid.jwt.token";
        UserDetails userDetails = new User("user@test.org", "pass", Collections.emptyList());

        assertFalse(jwtService.isTokenValid(malformedToken, userDetails));
        assertTrue(jwtService.isTokenExpired(malformedToken));
        assertThrows(Exception.class, () -> jwtService.extractEmail(malformedToken));
    }
}
