package com.huzaifa.rescueroute.controller;

import com.huzaifa.rescueroute.common.response.ApiResponse;
import com.huzaifa.rescueroute.dto.VerificationDecisionRequestDto;
import com.huzaifa.rescueroute.dto.VerificationRequestResponseDto;
import com.huzaifa.rescueroute.service.VerificationWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for administrator moderation of provider verification requests.
 * All endpoints are strictly restricted to authenticated users with the ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/admin/verification-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVerificationController {

    private final VerificationWorkflowService verificationWorkflowService;

    public AdminVerificationController(VerificationWorkflowService verificationWorkflowService) {
        this.verificationWorkflowService = verificationWorkflowService;
    }

    /**
     * Retrieves verification requests for moderation.
     *
     * @param status Optional status filter (default: PENDING)
     * @return List of matching VerificationRequestResponseDto records
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<VerificationRequestResponseDto>>> getVerificationRequests(
            @RequestParam(required = false) String status) {

        List<VerificationRequestResponseDto> requests =
                verificationWorkflowService.getVerificationRequests(status);

        return ResponseEntity.ok(
                ApiResponse.success("Verification requests retrieved successfully", requests));
    }

    /**
     * Retrieves a single verification request by its unique UUID.
     *
     * @param id UUID of the verification request
     * @return VerificationRequestResponseDto
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VerificationRequestResponseDto>> getVerificationRequestById(
            @PathVariable UUID id) {

        VerificationRequestResponseDto request =
                verificationWorkflowService.getVerificationRequestById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Verification request retrieved successfully", request));
    }

    /**
     * Processes an administrator decision (APPROVE or REJECT) on a pending verification request.
     *
     * @param id             UUID of the verification request
     * @param decisionDto    Decision payload (status/decision and review notes)
     * @param authentication Authenticated security context of the reviewing administrator
     * @return Updated VerificationRequestResponseDto
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VerificationRequestResponseDto>> processDecision(
            @PathVariable UUID id,
            @Valid @RequestBody VerificationDecisionRequestDto decisionDto,
            java.security.Principal principal) {

        String adminEmail = principal != null ? principal.getName() : null;
        VerificationRequestResponseDto updated = verificationWorkflowService.processDecision(
                id, decisionDto, adminEmail);

        return ResponseEntity.ok(
                ApiResponse.success("Verification request processed successfully", updated));
    }

    /**
     * Convenience endpoint for approving a verification request.
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<VerificationRequestResponseDto>> approveRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) VerificationDecisionRequestDto decisionDto,
            java.security.Principal principal) {

        VerificationDecisionRequestDto payload = decisionDto != null ? decisionDto : new VerificationDecisionRequestDto();
        payload.setDecision("APPROVED");

        String adminEmail = principal != null ? principal.getName() : null;
        VerificationRequestResponseDto updated = verificationWorkflowService.processDecision(
                id, payload, adminEmail);

        return ResponseEntity.ok(
                ApiResponse.success("Verification request approved successfully", updated));
    }

    /**
     * Convenience endpoint for rejecting a verification request.
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<VerificationRequestResponseDto>> rejectRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) VerificationDecisionRequestDto decisionDto,
            java.security.Principal principal) {

        VerificationDecisionRequestDto payload = decisionDto != null ? decisionDto : new VerificationDecisionRequestDto();
        payload.setDecision("REJECTED");

        String adminEmail = principal != null ? principal.getName() : null;
        VerificationRequestResponseDto updated = verificationWorkflowService.processDecision(
                id, payload, adminEmail);

        return ResponseEntity.ok(
                ApiResponse.success("Verification request rejected successfully", updated));
    }
}
