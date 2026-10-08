package com.huzaifa.rescueroute.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for administrator approval or rejection of a pending verification request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationDecisionRequestDto {

    @Pattern(regexp = "^(?i)(APPROVED|REJECTED|APPROVE|REJECT)$",
            message = "Decision must be either 'APPROVED' or 'REJECTED'")
    private String decision;

    /**
     * Optional alias for decision to support both payload formats.
     */
    private String status;

    private String reviewNotes;

    /**
     * Resolves the canonical status ("APPROVED" or "REJECTED").
     */
    public String getResolvedDecision() {
        String val = decision != null ? decision : status;
        if (val == null) {
            return null;
        }
        val = val.trim().toUpperCase();
        if ("APPROVE".equals(val)) {
            return "APPROVED";
        }
        if ("REJECT".equals(val)) {
            return "REJECTED";
        }
        return val;
    }
}
