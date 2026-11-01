package com.huzaifa.rescueroute.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for provider facility update requests.
 * Only explicitly editable fields are exposed. Immutable fields (ID, owner, verification status,
 * creation timestamp, audit logs) cannot be modified through this request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFacilityRequestDto {

    @NotBlank(message = "Facility name is required")
    @Size(max = 255, message = "Facility name cannot exceed 255 characters")
    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    @Builder.Default
    private String city = "Karachi";

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90.0 and +90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90.0 and +90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180.0 and +180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180.0 and +180.0")
    private Double longitude;

    @Size(max = 50, message = "Phone number cannot exceed 50 characters")
    private String phoneNumber;

    // Optional subtype-specific data payloads
    private HospitalDetailsDto hospitalDetails;
    private BloodBankDetailsDto bloodBankDetails;
    private PharmacyDetailsDto pharmacyDetails;
    private AmbulanceDetailsDto ambulanceDetails;
    private ClinicDetailsDto clinicDetails;
}
