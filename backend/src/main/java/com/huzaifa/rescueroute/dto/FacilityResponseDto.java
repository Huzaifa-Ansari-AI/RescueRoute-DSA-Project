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
 * Data Transfer Object representing medical facility details returned to API consumers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FacilityResponseDto {

    private UUID id;
    private String name;
    private String facilityType;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private String phoneNumber;

    @JsonProperty("isVerified")
    private boolean isVerified;

    @JsonProperty("isActive")
    private boolean isActive;

    private Instant createdAt;
    private Instant updatedAt;

    private HospitalDetailsDto hospitalDetails;
    private BloodBankDetailsDto bloodBankDetails;
    private PharmacyDetailsDto pharmacyDetails;
    private AmbulanceDetailsDto ambulanceDetails;
}
