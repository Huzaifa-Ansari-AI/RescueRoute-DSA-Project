package com.huzaifa.rescueroute.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object representing blood bank inventory and availability.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloodBankDetailsDto {
    private UUID id;
    private boolean oPositive;
    private boolean oNegative;
    private boolean aPositive;
    private boolean aNegative;
    private boolean bPositive;
    private boolean bNegative;
    private boolean abPositive;
    private boolean abNegative;
    private Instant stockUpdatedAt;
    private String notes;
}
