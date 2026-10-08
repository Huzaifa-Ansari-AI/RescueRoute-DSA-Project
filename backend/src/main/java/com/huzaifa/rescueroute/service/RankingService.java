package com.huzaifa.rescueroute.service;

import com.huzaifa.rescueroute.dto.RankedFacilityResponseDto;

import java.util.List;

/**
 * Service interface for spatial distance computation and multi-criteria priority ranking.
 */
public interface RankingService {

    /**
     * Ranks facilities relative to user coordinates based on distance, verification, and emergency capability.
     *
     * @param userLat      User's latitude in degrees
     * @param userLng      User's longitude in degrees
     * @param facilityType Optional filter for facility category (Hospital, Blood Bank, Pharmacy, Ambulance)
     * @param emergency    Whether this is an emergency search prioritizing 24/7/emergency readiness
     * @param k            Maximum number of top results to return (default 10)
     * @return List of Top-K ranked facilities with distance and match scores
     */
    List<RankedFacilityResponseDto> rankFacilities(
            Double userLat,
            Double userLng,
            String facilityType,
            Boolean emergency,
            Integer k);
}
