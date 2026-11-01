package com.huzaifa.rescueroute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating external facility moderation status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalFacilityStatusUpdateDto {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "DISCOVERED|DUPLICATE_CANDIDATE|REVIEWED|SUPPRESSED",
            message = "Status must be one of: DISCOVERED, DUPLICATE_CANDIDATE, REVIEWED, SUPPRESSED")
    private String status;

    private String notes;
}
