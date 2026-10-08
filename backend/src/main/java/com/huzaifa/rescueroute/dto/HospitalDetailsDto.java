package com.huzaifa.rescueroute.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object representing detailed hospital capabilities and facilities.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalDetailsDto {
    private UUID id;
    private boolean emergencyAvailable;
    private boolean icuAvailable;
    private boolean traumaCenterAvailable;
    private boolean cardiologyAvailable;
    private boolean pediatricsAvailable;
    private boolean maternityAvailable;
    private String notes;
}
