package com.huzaifa.rescueroute.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huzaifa.rescueroute.common.exception.GlobalExceptionHandler;
import com.huzaifa.rescueroute.dto.CreateFacilityRequestDto;
import com.huzaifa.rescueroute.dto.FacilityResponseDto;
import com.huzaifa.rescueroute.service.FacilityService;
import com.huzaifa.rescueroute.service.RankingService;
import com.huzaifa.rescueroute.service.VerificationWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ProviderFacilityControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FacilityService facilityService;

    @Mock
    private RankingService rankingService;

    @Mock
    private VerificationWorkflowService verificationWorkflowService;

    @InjectMocks
    private FacilityController facilityController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(facilityController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Provider creates facility successfully with 201 Created and unverified state")
    void testCreateFacility_Success() throws Exception {
        CreateFacilityRequestDto request = CreateFacilityRequestDto.builder()
                .name("Aga Khan Medical Complex")
                .facilityType("Hospital")
                .address("Stadium Road, Karachi")
                .latitude(24.8920)
                .longitude(67.0740)
                .build();

        FacilityResponseDto responseDto = FacilityResponseDto.builder()
                .id(UUID.randomUUID())
                .name("Aga Khan Medical Complex")
                .facilityType("Hospital")
                .address("Stadium Road, Karachi")
                .isVerified(false)
                .isActive(true)
                .build();

        Principal mockPrincipal = () -> "provider@rescueroute.com";

        when(verificationWorkflowService.createFacilityWithVerification(any(), eq("provider@rescueroute.com")))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/facilities")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Aga Khan Medical Complex"))
                .andExpect(jsonPath("$.data.isVerified").value(false));
    }

    @Test
    @DisplayName("Validation fails when required facility name is missing")
    void testCreateFacility_MissingName_BadRequest() throws Exception {
        CreateFacilityRequestDto request = CreateFacilityRequestDto.builder()
                .facilityType("Hospital")
                .address("Stadium Road, Karachi")
                .latitude(24.8920)
                .longitude(67.0740)
                .build();

        Principal mockPrincipal = () -> "provider@rescueroute.com";

        mockMvc.perform(post("/api/v1/facilities")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Validation fails when latitude is out of bounds")
    void testCreateFacility_InvalidLatitude_BadRequest() throws Exception {
        CreateFacilityRequestDto request = CreateFacilityRequestDto.builder()
                .name("Test Center")
                .facilityType("Hospital")
                .address("Stadium Road, Karachi")
                .latitude(99.0) // Invalid
                .longitude(67.0740)
                .build();

        Principal mockPrincipal = () -> "provider@rescueroute.com";

        mockMvc.perform(post("/api/v1/facilities")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
