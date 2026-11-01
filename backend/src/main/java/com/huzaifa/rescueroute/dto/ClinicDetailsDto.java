package com.huzaifa.rescueroute.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object representing detailed outpatient clinic capabilities and consultation offerings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicDetailsDto {
    private UUID id;

    @Builder.Default
    private boolean consultationAvailable = true;

    private String specialties;

    @Builder.Default
    private boolean openWeekends = false;

    private String notes;
}
