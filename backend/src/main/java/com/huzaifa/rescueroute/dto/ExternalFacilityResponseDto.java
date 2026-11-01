package com.huzaifa.rescueroute.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object representing an externally discovered medical point of interest.
 * Clearly demarcates external provenance and unverified clinical status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalFacilityResponseDto {

    private UUID id;
    private String source;
    private String osmElementType;
    private Long osmElementId;
    private String sourceUrl;
    private String name;
    private String facilityType;
    private String osmTags;
    private Double latitude;
    private Double longitude;
    private String address;
    private String city;
    private String phoneNumber;
    private String website;
    private String openingHours;
    private String importStatus;
    private UUID matchedFacilityId;
    private String matchedFacilityName;
    private Double distanceKm;

    @Builder.Default
    private String attribution = "© OpenStreetMap contributors";

    @Builder.Default
    private String disclaimer = "External listing · OpenStreetMap · Not verified by RescueRoute";

    @JsonProperty("isVerified")
    @Builder.Default
    private boolean isVerified = false; // Always false for external OSM records

    private Instant importedAt;
    private Instant lastSeenAt;
}
