package com.huzaifa.rescueroute.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a facility ranked by the DSA priority ranking engine.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankedFacilityResponseDto {

    private int rank;

    private double distanceKm;

    private double score;

    @JsonProperty("isVerified")
    private boolean isVerified;

    @JsonProperty("emergencyAvailable")
    private boolean emergencyAvailable;

    private FacilityResponseDto facility;
}
