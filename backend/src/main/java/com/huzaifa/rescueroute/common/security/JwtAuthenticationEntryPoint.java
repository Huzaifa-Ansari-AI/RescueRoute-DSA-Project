package com.huzaifa.rescueroute.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Handles unauthenticated requests by returning standardized ApiResponse JSON with HTTP 401.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String json = "{\"success\":false,\"message\":\"Unauthorized: Authentication token is missing or invalid\",\"data\":null,\"timestamp\":\""
                + Instant.now() + "\"}";

        response.getWriter().write(json);
    }
}
