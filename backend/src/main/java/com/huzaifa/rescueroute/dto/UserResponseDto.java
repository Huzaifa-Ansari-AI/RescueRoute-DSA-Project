package com.huzaifa.rescueroute.dto;

import com.huzaifa.rescueroute.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe DTO exposing public user identity without password credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private UUID id;
    private String fullName;
    private String email;
    private UserRole role;
    private boolean isActive;
    private Instant createdAt;
}
