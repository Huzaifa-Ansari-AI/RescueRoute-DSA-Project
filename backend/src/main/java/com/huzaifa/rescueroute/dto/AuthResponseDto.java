package com.huzaifa.rescueroute.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO returned after successful registration or authentication containing JWT token and user info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserResponseDto user;
}
