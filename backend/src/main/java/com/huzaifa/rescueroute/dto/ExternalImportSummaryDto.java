package com.huzaifa.rescueroute.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary DTO reporting results of an external facility import operation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalImportSummaryDto {

    private int totalQueried;
    private int imported;
    private int updated;
    private int duplicateCandidates;
    private int skipped;
    private int failed;
    private long executionTimeMs;
    private String message;
}
