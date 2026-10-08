package com.huzaifa.rescueroute.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Handles unauthorized access attempts by returning standardized ApiResponse JSON with HTTP 403.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        String json = "{\"success\":false,\"message\":\"Forbidden: You do not have permission to access this resource\",\"data\":null,\"timestamp\":\""
                + Instant.now() + "\"}";

        response.getWriter().write(json);
    }
}
