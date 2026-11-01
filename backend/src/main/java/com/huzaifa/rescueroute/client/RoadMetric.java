package com.huzaifa.rescueroute.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encapsulates road routing metrics for a single origin-destination facility pair.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadMetric {

    /**
     * Estimated driving distance in kilometers.
     * Null if destination is unreachable or routing failed.
     */
    private Double distanceKm;

    /**
     * Estimated travel duration in minutes.
     * Null if destination is unreachable or routing failed.
     */
    private Integer durationMinutes;

    /**
     * True if a valid drivable road path exists; false if unreachable or null.
     */
    private boolean routeAvailable;
}
