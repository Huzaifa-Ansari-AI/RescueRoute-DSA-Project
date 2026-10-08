package com.huzaifa.rescueroute.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object representing ambulance operational status and readiness.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmbulanceDetailsDto {
    private UUID id;
    private boolean availableNow;
    private Integer responseTimeMinutes;
    private String coverageArea;
    private boolean equippedForIcuTransfer;
    private String notes;
}
