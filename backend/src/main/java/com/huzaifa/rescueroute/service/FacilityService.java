package com.huzaifa.rescueroute.service;

import com.huzaifa.rescueroute.dto.FacilityResponseDto;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for medical facility operations.
 */
public interface FacilityService {

    /**
     * Retrieves all facilities with optional combined filters.
     *
     * @param type   Optional facility type filter (e.g. Hospital, Blood Bank, Pharmacy, Ambulance)
     * @param city   Optional city filter (e.g. Karachi)
     * @param active Optional active status filter (true/false)
     * @return List of matching FacilityResponseDto records
     */
    List<FacilityResponseDto> getAllFacilities(String type, String city, Boolean active);

    /**
     * Retrieves a single facility by its UUID with complete subtype details.
     *
     * @param id The unique identifier of the facility
     * @return The complete FacilityResponseDto
     * @throws com.huzaifa.rescueroute.common.exception.ResourceNotFoundException if facility does not exist
     */
    FacilityResponseDto getFacilityById(UUID id);
}
