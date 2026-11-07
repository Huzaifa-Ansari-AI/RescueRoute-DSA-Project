package com.huzaifa.rescueroute.common.controller;

import com.huzaifa.rescueroute.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check endpoint.
 */
@RestController
public class HealthController {

    @GetMapping({"/api/health", "/api/v1/health", "/healthz"})
    public ApiResponse<String> health() {

        return ApiResponse.success(
                "Backend is running successfully.");
    }
}