package com.huzaifa.rescueroute.controller;

import com.huzaifa.rescueroute.common.response.ApiResponse;
import com.huzaifa.rescueroute.dto.ExternalFacilityResponseDto;
import com.huzaifa.rescueroute.dto.ExternalFacilityStatusUpdateDto;
import com.huzaifa.rescueroute.dto.ExternalImportRequestDto;
import com.huzaifa.rescueroute.dto.ExternalImportSummaryDto;
import com.huzaifa.rescueroute.service.ExternalFacilityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for external facility discovery, Overpass imports, and moderation.
 * External facilities are explicitly labeled as unverified listings from OpenStreetMap.
 */
@RestController
@RequestMapping("/api/v1/external-facilities")
public class ExternalFacilityController {

    private final ExternalFacilityService externalFacilityService;

    public ExternalFacilityController(ExternalFacilityService externalFacilityService) {
        this.externalFacilityService = externalFacilityService;
    }

    /**
     * Searches discovered external medical facilities within a geographic radius.
     * Open to public discovery queries.
     *
     * @param lat Center latitude [-90.0, 90.0]
     * @param lng Center longitude [-180.0, 180.0]
     * @param radiusMeters Bounded radius in meters (100 to 10,000 meters)
     * @param type Optional facility category filter
     * @param status Optional import status filter (DISCOVERED, DUPLICATE_CANDIDATE, REVIEWED)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ExternalFacilityResponseDto>>> searchExternalFacilities(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "5000") Double radiusMeters,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {

        if (lat == null || lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90.0 and 90.0 degrees.");
        }
        if (lng == null || lng < -180.0 || lng > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180.0 and 180.0 degrees.");
        }
        if (radiusMeters == null || radiusMeters < 100.0 || radiusMeters > 10000.0) {
            throw new IllegalArgumentException("Radius must be between 100 and 10,000 meters.");
        }

        List<ExternalFacilityResponseDto> facilities = externalFacilityService.searchExternalFacilities(
                lat, lng, radiusMeters, type, status);

        return ResponseEntity.ok(ApiResponse.success("External facilities retrieved successfully", facilities));
    }

    /**
     * Executes a bounded geographic import from OpenStreetMap Overpass.
     * Restricted to authenticated providers and administrators.
     */
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    public ResponseEntity<ApiResponse<ExternalImportSummaryDto>> importFacilities(
            @Valid @RequestBody ExternalImportRequestDto request) {

        ExternalImportSummaryDto summary = externalFacilityService.importFacilities(request);
        return ResponseEntity.ok(ApiResponse.success(summary.getMessage(), summary));
    }

    /**
     * Retrieves details for a specific external facility by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExternalFacilityResponseDto>> getExternalFacilityById(
            @PathVariable UUID id) {

        ExternalFacilityResponseDto facility = externalFacilityService.getExternalFacilityById(id);
        return ResponseEntity.ok(ApiResponse.success("External facility retrieved successfully", facility));
    }

    /**
     * Moderates an external facility's import status (e.g. REVIEWED, SUPPRESSED, DISCOVERED).
     * Restricted to administrators.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ExternalFacilityResponseDto>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ExternalFacilityStatusUpdateDto request) {

        ExternalFacilityResponseDto updated = externalFacilityService.updateFacilityStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("External facility status updated successfully", updated));
    }
}
