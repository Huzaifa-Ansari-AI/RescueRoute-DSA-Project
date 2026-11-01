package com.huzaifa.rescueroute.service;

import com.huzaifa.rescueroute.dto.ExternalFacilityResponseDto;
import com.huzaifa.rescueroute.dto.ExternalImportRequestDto;
import com.huzaifa.rescueroute.dto.ExternalImportSummaryDto;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing external OpenStreetMap facility imports, search, and moderation.
 */
public interface ExternalFacilityService {

    /**
     * Executes a bounded geographic import from Overpass API, normalizes POIs, detects duplicates,
     * and performs idempotent persistence.
     */
    ExternalImportSummaryDto importFacilities(ExternalImportRequestDto request);

    /**
     * Searches discovered external facilities within a geographic radius with optional category and status filters.
     */
    List<ExternalFacilityResponseDto> searchExternalFacilities(
            Double lat,
            Double lng,
            Double radiusMeters,
            String facilityType,
            String status);

    /**
     * Retrieves an external facility by its internal UUID.
     */
    ExternalFacilityResponseDto getExternalFacilityById(UUID id);

    /**
     * Updates an external facility's moderation status (e.g. REVIEWED, SUPPRESSED, DISCOVERED).
     */
    ExternalFacilityResponseDto updateFacilityStatus(UUID id, String status);
}
