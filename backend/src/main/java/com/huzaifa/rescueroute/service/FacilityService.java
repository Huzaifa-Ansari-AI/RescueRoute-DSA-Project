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

    /**
     * Safely deactivates (soft-deletes) a facility, excluding it from search, maps, and ranking.
     *
     * @param id          UUID of the facility to deactivate
     * @param callerEmail Email of authenticated provider or admin
     * @return Updated FacilityResponseDto
     */
    FacilityResponseDto deactivateFacility(UUID id, String callerEmail);

    /**
     * Restores/reactivates a previously soft-deleted facility (Admin only).
     *
     * @param id         UUID of the facility to reactivate
     * @param adminEmail Email of authenticated admin
     * @return Updated FacilityResponseDto
     */
    FacilityResponseDto reactivateFacility(UUID id, String adminEmail);

    /**
     * Retrieves all facilities owned/managed by a specific provider.
     *
     * @param providerEmail Email of authenticated provider
     * @return List of FacilityResponseDto records owned by the provider
     */
    List<FacilityResponseDto> getFacilitiesByProvider(String providerEmail);

    /**
     * Retrieves all facilities for administrator management with comprehensive filters.
     *
     * @param type     Optional facility type filter
     * @param city     Optional city filter
     * @param active   Optional active status filter (null for all)
     * @param verified Optional verification status filter (null for all)
     * @return List of matching FacilityResponseDto records
     */
    List<FacilityResponseDto> getAdminFacilities(String type, String city, Boolean active, Boolean verified);
}
