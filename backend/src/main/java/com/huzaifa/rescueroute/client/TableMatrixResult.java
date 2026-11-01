package com.huzaifa.rescueroute.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of an OSRM Table API matrix batch calculation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableMatrixResult {

    /**
     * True if the external OSRM service returned a successful 200 response with valid matrix data.
     */
    private boolean successful;

    /**
     * Metrics corresponding 1-to-1 with the requested candidate list.
     */
    private List<RoadMetric> metrics;

    /**
     * Diagnostic error or warning message if the request failed or was partially degraded.
     */
    private String errorMessage;

    public static TableMatrixResult failure(String errorMessage) {
        return TableMatrixResult.builder()
                .successful(false)
                .errorMessage(errorMessage)
                .build();
    }

    public static TableMatrixResult success(List<RoadMetric> metrics) {
        return TableMatrixResult.builder()
                .successful(true)
                .metrics(metrics)
                .build();
    }
}
