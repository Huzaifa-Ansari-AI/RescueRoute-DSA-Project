package com.huzaifa.rescueroute.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for bounded geographic imports from OpenStreetMap Overpass.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalImportRequestDto {

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90.0 and 90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180.0 and 180.0")
    private Double longitude;

    @NotNull(message = "Radius in meters is required")
    @DecimalMin(value = "100.0", message = "Radius must be at least 100 meters")
    @DecimalMax(value = "10000.0", message = "Radius cannot exceed 10,000 meters (10 km)")
    private Double radiusMeters;

    private String facilityType;
}
