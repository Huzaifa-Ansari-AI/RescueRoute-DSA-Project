package com.huzaifa.rescueroute.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huzaifa.rescueroute.common.exception.GlobalExceptionHandler;
import com.huzaifa.rescueroute.common.exception.ResourceNotFoundException;
import com.huzaifa.rescueroute.dto.VerificationDecisionRequestDto;
import com.huzaifa.rescueroute.dto.VerificationRequestResponseDto;
import com.huzaifa.rescueroute.service.VerificationWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AdminVerificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VerificationWorkflowService verificationWorkflowService;

    @InjectMocks
    private AdminVerificationController adminVerificationController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private UUID requestId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminVerificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Admin retrieves pending verification requests")
    void testGetVerificationRequests() throws Exception {
        VerificationRequestResponseDto dto = VerificationRequestResponseDto.builder()
                .id(requestId)
                .facilityName("Jinnah Hospital")
                .status("PENDING")
                .build();

        when(verificationWorkflowService.getVerificationRequests("PENDING"))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/admin/verification-requests")
                        .param("status", "PENDING")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].facilityName").value("Jinnah Hospital"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("Admin retrieves single verification request by ID")
    void testGetVerificationRequestById() throws Exception {
        VerificationRequestResponseDto dto = VerificationRequestResponseDto.builder()
                .id(requestId)
                .facilityName("Indus Hospital")
                .status("PENDING")
                .build();

        when(verificationWorkflowService.getVerificationRequestById(requestId))
                .thenReturn(dto);

        mockMvc.perform(get("/api/v1/admin/verification-requests/" + requestId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.facilityName").value("Indus Hospital"));
    }

    @Test
    @DisplayName("Admin approves request via PUT /{id}")
    void testProcessDecision_Approve() throws Exception {
        VerificationDecisionRequestDto decisionDto = VerificationDecisionRequestDto.builder()
                .decision("APPROVED")
                .reviewNotes("Verified by MOH")
                .build();

        VerificationRequestResponseDto responseDto = VerificationRequestResponseDto.builder()
                .id(requestId)
                .status("APPROVED")
                .facilityVerified(true)
                .build();

        Principal mockPrincipal = () -> "admin@rescueroute.com";

        when(verificationWorkflowService.processDecision(eq(requestId), any(), eq("admin@rescueroute.com")))
                .thenReturn(responseDto);

        mockMvc.perform(put("/api/v1/admin/verification-requests/" + requestId)
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decisionDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.facilityVerified").value(true));
    }

    @Test
    @DisplayName("Admin rejects request via convenience endpoint PUT /{id}/reject")
    void testRejectRequest_ConvenienceEndpoint() throws Exception {
        VerificationDecisionRequestDto decisionDto = VerificationDecisionRequestDto.builder()
                .reviewNotes("Incomplete address")
                .build();

        VerificationRequestResponseDto responseDto = VerificationRequestResponseDto.builder()
                .id(requestId)
                .status("REJECTED")
                .facilityVerified(false)
                .reviewNotes("Incomplete address")
                .build();

        Principal mockPrincipal = () -> "admin@rescueroute.com";

        when(verificationWorkflowService.processDecision(eq(requestId), any(), eq("admin@rescueroute.com")))
                .thenReturn(responseDto);

        mockMvc.perform(put("/api/v1/admin/verification-requests/" + requestId + "/reject")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decisionDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @DisplayName("Non-existent verification request returns 404 Not Found")
    void testGetVerificationRequestById_NotFound() throws Exception {
        when(verificationWorkflowService.getVerificationRequestById(requestId))
                .thenThrow(new ResourceNotFoundException("Verification request not found with ID: " + requestId));

        mockMvc.perform(get("/api/v1/admin/verification-requests/" + requestId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Verification request not found with ID: " + requestId));
    }
}
