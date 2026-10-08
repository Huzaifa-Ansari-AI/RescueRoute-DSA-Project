package com.huzaifa.rescueroute.service;

import com.huzaifa.rescueroute.dto.CreateFacilityRequestDto;
import com.huzaifa.rescueroute.dto.FacilityResponseDto;
import com.huzaifa.rescueroute.dto.VerificationDecisionRequestDto;
import com.huzaifa.rescueroute.dto.VerificationRequestResponseDto;

import java.util.List;
import java.util.UUID;

/**
 * Service interface governing the complete provider submission and administrative
 * verification lifecycle for medical facilities.
 */
public interface VerificationWorkflowService {

    /**
     * Creates a new facility submitted by an authenticated provider.
     * Forcefully initializes the facility as unverified (is_verified = false)
     * and automatically creates a corresponding PENDING verification request within a single transaction.
     *
     * @param request       Validated facility data submitted by the provider
     * @param providerEmail Email of the authenticated provider
     * @return Complete FacilityResponseDto of the persisted facility
     */
    FacilityResponseDto createFacilityWithVerification(CreateFacilityRequestDto request, String providerEmail);

    /**
     * Retrieves all verification requests, optionally filtered by status (PENDING, APPROVED, REJECTED).
     *
     * @param status Optional status filter (case-insensitive)
     * @return List of matching VerificationRequestResponseDto records
     */
    List<VerificationRequestResponseDto> getVerificationRequests(String status);

    /**
     * Retrieves a single verification request by its unique UUID.
     *
     * @param requestId Unique UUID of the verification request
     * @return VerificationRequestResponseDto
     */
    VerificationRequestResponseDto getVerificationRequestById(UUID requestId);

    /**
     * Processes an administrator decision (APPROVE or REJECT) on a pending verification request.
     * - On APPROVE: Marks request APPROVED, facility is_verified = true, logs admin audit entry.
     * - On REJECT: Marks request REJECTED, facility is_verified = false, stores notes, logs admin audit entry.
     * Entire workflow executes within a single atomic transaction.
     *
     * @param requestId   Unique UUID of the pending verification request
     * @param decisionDto Decision payload containing APPROVE/REJECT and review notes
     * @param adminEmail  Email of the authenticated administrator performing the action
     * @return Updated VerificationRequestResponseDto
     */
    VerificationRequestResponseDto processDecision(
            UUID requestId, VerificationDecisionRequestDto decisionDto, String adminEmail);
}
