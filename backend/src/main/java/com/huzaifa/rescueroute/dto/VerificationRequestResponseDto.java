package com.huzaifa.rescueroute.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a verification request for administrator review and moderation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerificationRequestResponseDto {

    private UUID id;
    private UUID facilityId;
    private String facilityName;
    private String facilityType;
    private String facilityAddress;
    private String facilityCity;
    private Double facilityLatitude;
    private Double facilityLongitude;
    private String facilityPhoneNumber;
    private boolean facilityVerified;

    private UserResponseDto submittedBy;
    private String status; // PENDING, APPROVED, REJECTED
    private UserResponseDto reviewedBy;
    private String reviewNotes;

    private Instant submittedAt;
    private Instant reviewedAt;

    private FacilityResponseDto facilityDetails;
}
