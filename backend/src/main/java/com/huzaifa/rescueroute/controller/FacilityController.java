package com.huzaifa.rescueroute.controller;

import com.huzaifa.rescueroute.common.response.ApiResponse;
import com.huzaifa.rescueroute.dto.FacilityResponseDto;
import com.huzaifa.rescueroute.dto.RankedFacilityResponseDto;
import com.huzaifa.rescueroute.service.FacilityService;
import com.huzaifa.rescueroute.service.RankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for querying and ranking medical facilities.
 */
@RestController
@RequestMapping("/api/v1/facilities")
public class FacilityController {

    private final FacilityService facilityService;
    private final RankingService rankingService;
    private final com.huzaifa.rescueroute.service.VerificationWorkflowService verificationWorkflowService;

    public FacilityController(
            FacilityService facilityService,
            RankingService rankingService,
            com.huzaifa.rescueroute.service.VerificationWorkflowService verificationWorkflowService) {
        this.facilityService = facilityService;
        this.rankingService = rankingService;
        this.verificationWorkflowService = verificationWorkflowService;
    }

    /**
     * Creates a new medical facility submitted by an authenticated provider.
     * Restricted strictly to authenticated PROVIDER users.
     */
    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<FacilityResponseDto>> createFacility(
            @jakarta.validation.Valid @RequestBody com.huzaifa.rescueroute.dto.CreateFacilityRequestDto request,
            java.security.Principal principal) {

        String providerEmail = principal != null ? principal.getName() : null;
        FacilityResponseDto response = verificationWorkflowService.createFacilityWithVerification(
                request, providerEmail);

        return ResponseEntity
                .status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success("Facility created successfully and submitted for verification", response));
    }

    /**
     * Performs DSA-driven spatial distance calculation and multi-criteria priority ranking.
     *
     * @param lat       User latitude in degrees (required)
     * @param lng       User longitude in degrees (required)
     * @param radiusKm  Search radius in kilometers (default 10.0)
     * @param type      Optional facility category filter (Hospital, Clinic, Blood Bank, Pharmacy, Ambulance)
     * @param emergency Optional urgency flag prioritizing 24/7 and emergency readiness
     * @param k         Optional limit on top ranked facilities returned (default 10)
     * @return List of ranked and scored facilities wrapped in standard ApiResponse
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RankedFacilityResponseDto>>> searchFacilities(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "false") Boolean emergency,
            @RequestParam(required = false, defaultValue = "10") Integer k) {

        List<RankedFacilityResponseDto> rankedFacilities =
                rankingService.rankFacilities(lat, lng, radiusKm, type, emergency, k);

        return ResponseEntity.ok(
                ApiResponse.success("Ranked facilities retrieved successfully", rankedFacilities));
    }

    /**
     * Retrieves facilities with optional query filters (type, city, active).
     *
     * @param type   Optional filter by facility type (Hospital, Blood Bank, Pharmacy, Ambulance)
     * @param city   Optional filter by city (e.g. Karachi)
     * @param active Optional filter by active status (true/false)
     * @return List of matching facilities wrapped in standard ApiResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FacilityResponseDto>>> getAllFacilities(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean active) {

        List<FacilityResponseDto> facilities = facilityService.getAllFacilities(type, city, active);
        return ResponseEntity.ok(ApiResponse.success("Facilities retrieved successfully", facilities));
    }

    /**
     * Retrieves a single facility by ID along with its subtype details.
     *
     * @param id Unique UUID of the facility
     * @return Detailed facility information wrapped in standard ApiResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FacilityResponseDto>> getFacilityById(@PathVariable UUID id) {
        FacilityResponseDto facility = facilityService.getFacilityById(id);
        return ResponseEntity.ok(ApiResponse.success("Facility retrieved successfully", facility));
    }

    /**
     * Submits an update to an existing facility. Staged for administrator re-verification.
     * Restricted to authenticated PROVIDER (facility owner) or ADMIN.
     */
    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public ResponseEntity<ApiResponse<FacilityResponseDto>> updateFacility(
            @PathVariable UUID id,
            @jakarta.validation.Valid @RequestBody com.huzaifa.rescueroute.dto.UpdateFacilityRequestDto request,
            java.security.Principal principal) {

        String callerEmail = principal != null ? principal.getName() : null;
        FacilityResponseDto response = verificationWorkflowService.submitFacilityUpdate(id, request, callerEmail);

        return ResponseEntity.ok(
                ApiResponse.success("Facility update submitted successfully for administrator verification", response));
    }

    /**
     * Safely deactivates (soft-deletes) a facility, excluding it from normal public search and routing.
     * Restricted to authenticated PROVIDER (facility owner) or ADMIN.
     */
    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public ResponseEntity<ApiResponse<FacilityResponseDto>> deactivateFacility(
            @PathVariable UUID id,
            java.security.Principal principal) {

        String callerEmail = principal != null ? principal.getName() : null;
        FacilityResponseDto response = facilityService.deactivateFacility(id, callerEmail);

        return ResponseEntity.ok(
                ApiResponse.success("Facility deactivated successfully", response));
    }

    /**
     * Restores/reactivates a previously soft-deleted facility.
     * Restricted strictly to ADMIN role.
     */
    @PostMapping("/{id}/reactivate")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FacilityResponseDto>> reactivateFacility(
            @PathVariable UUID id,
            java.security.Principal principal) {

        String adminEmail = principal != null ? principal.getName() : null;
        FacilityResponseDto response = facilityService.reactivateFacility(id, adminEmail);

        return ResponseEntity.ok(
                ApiResponse.success("Facility restored and reactivated successfully", response));
    }

    /**
     * Retrieves all facilities owned/managed by the currently authenticated provider.
     * Restricted to authenticated PROVIDER or ADMIN.
     */
    @GetMapping("/my")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<FacilityResponseDto>>> getMyFacilities(java.security.Principal principal) {
        String providerEmail = principal != null ? principal.getName() : null;
        List<FacilityResponseDto> facilities = facilityService.getFacilitiesByProvider(providerEmail);
        return ResponseEntity.ok(ApiResponse.success("Provider facilities retrieved successfully", facilities));
    }

    /**
     * Retrieves all facilities for administrator moderation and lifecycle management with comprehensive filters.
     * Restricted strictly to ADMIN role.
     */
    @GetMapping("/admin")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<FacilityResponseDto>>> getAdminFacilities(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean verified) {

        List<FacilityResponseDto> facilities = facilityService.getAdminFacilities(type, city, active, verified);
        return ResponseEntity.ok(ApiResponse.success("Admin facilities retrieved successfully", facilities));
    }
}
