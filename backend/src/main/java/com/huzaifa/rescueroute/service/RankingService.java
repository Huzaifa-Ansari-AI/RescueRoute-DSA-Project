package com.huzaifa.rescueroute.service;

import com.huzaifa.rescueroute.dto.RankedFacilityResponseDto;

import java.util.List;

/**
 * Service interface for spatial distance computation and multi-criteria priority ranking.
 */
public interface RankingService {

    /**
     * Ranks facilities relative to user coordinates and coverage radius.
     *
     * @param userLat      User's latitude in degrees
     * @param userLng      User's longitude in degrees
     * @param radiusKm     Search and ranking coverage radius in kilometers (e.g. 5.0, 10.0, 25.0)
     * @param facilityType Optional filter for facility category (Hospital, Clinic, Blood Bank, Pharmacy, Ambulance)
     * @param emergency    Whether this is an emergency search prioritizing 24/7/emergency readiness
     * @param k            Maximum number of top results to return (default 10)
     * @return List of Top-K ranked facilities with distance and match scores
     */
    List<RankedFacilityResponseDto> rankFacilities(
            Double userLat,
            Double userLng,
            Double radiusKm,
            String facilityType,
            Boolean emergency,
            Integer k);

    default List<RankedFacilityResponseDto> rankFacilities(
            Double userLat,
            Double userLng,
            String facilityType,
            Boolean emergency,
            Integer k) {
        return rankFacilities(userLat, userLng, 10.0, facilityType, emergency, k);
    }
}
