package com.huzaifa.rescueroute.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huzaifa.rescueroute.common.exception.GlobalExceptionHandler;
import com.huzaifa.rescueroute.common.exception.ResourceNotFoundException;
import com.huzaifa.rescueroute.dto.ExternalFacilityResponseDto;
import com.huzaifa.rescueroute.dto.ExternalFacilityStatusUpdateDto;
import com.huzaifa.rescueroute.dto.ExternalImportRequestDto;
import com.huzaifa.rescueroute.dto.ExternalImportSummaryDto;
import com.huzaifa.rescueroute.service.ExternalFacilityService;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ExternalFacilityControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ExternalFacilityService externalFacilityService;

    @InjectMocks
    private ExternalFacilityController externalFacilityController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(externalFacilityController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/external-facilities - should return 200 with external facilities list")
    void shouldSearchExternalFacilitiesSuccessfully() throws Exception {
        UUID facilityId = UUID.randomUUID();
        ExternalFacilityResponseDto dto = ExternalFacilityResponseDto.builder()
                .id(facilityId)
                .name("Al-Mustafa Medical Center")
                .facilityType("Clinic")
                .latitude(24.8600)
                .longitude(67.0100)
                .distanceKm(0.85)
                .attribution("© OpenStreetMap contributors")
                .disclaimer("External listing · OpenStreetMap · Not verified by RescueRoute")
                .isVerified(false)
                .importStatus("DISCOVERED")
                .build();

        when(externalFacilityService.searchExternalFacilities(eq(24.86), eq(67.01), eq(5000.0), any(), any()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/external-facilities")
                        .param("lat", "24.86")
                        .param("lng", "67.01")
                        .param("radiusMeters", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Al-Mustafa Medical Center")))
                .andExpect(jsonPath("$.data[0].isVerified", is(false)))
                .andExpect(jsonPath("$.data[0].attribution", is("© OpenStreetMap contributors")));
    }

    @Test
    @DisplayName("GET /api/v1/external-facilities - should reject invalid coordinates with 400 Bad Request")
    void shouldRejectInvalidCoordinates() throws Exception {
        mockMvc.perform(get("/api/v1/external-facilities")
                        .param("lat", "120.0") // Out of range [-90, 90]
                        .param("lng", "67.01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/external-facilities/import - should return 200 with import summary metrics")
    void shouldTriggerImportSuccessfully() throws Exception {
        ExternalImportRequestDto request = ExternalImportRequestDto.builder()
                .latitude(24.86)
                .longitude(67.01)
                .radiusMeters(3000.0)
                .facilityType("Hospital")
                .build();

        ExternalImportSummaryDto summary = ExternalImportSummaryDto.builder()
                .totalQueried(12)
                .imported(8)
                .updated(4)
                .duplicateCandidates(2)
                .executionTimeMs(320)
                .message("Successfully processed 12 facilities.")
                .build();

        when(externalFacilityService.importFacilities(any(ExternalImportRequestDto.class)))
                .thenReturn(summary);

        mockMvc.perform(post("/api/v1/external-facilities/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalQueried", is(12)))
                .andExpect(jsonPath("$.data.imported", is(8)))
                .andExpect(jsonPath("$.data.duplicateCandidates", is(2)));
    }

    @Test
    @DisplayName("GET /api/v1/external-facilities/{id} - should return 200 for existing facility")
    void shouldReturnFacilityById() throws Exception {
        UUID id = UUID.randomUUID();
        ExternalFacilityResponseDto dto = ExternalFacilityResponseDto.builder()
                .id(id)
                .name("Kharadar General Hospital")
                .facilityType("Hospital")
                .latitude(24.8520)
                .longitude(66.9950)
                .isVerified(false)
                .build();

        when(externalFacilityService.getExternalFacilityById(id)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/external-facilities/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Kharadar General Hospital")));
    }

    @Test
    @DisplayName("GET /api/v1/external-facilities/{id} - should return 404 when facility does not exist")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(externalFacilityService.getExternalFacilityById(id))
                .thenThrow(new ResourceNotFoundException("External facility not found with id: " + id));

        mockMvc.perform(get("/api/v1/external-facilities/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/external-facilities/{id}/status - should update moderation status")
    void shouldUpdateStatusSuccessfully() throws Exception {
        UUID id = UUID.randomUUID();
        ExternalFacilityStatusUpdateDto request = ExternalFacilityStatusUpdateDto.builder()
                .status("REVIEWED")
                .notes("Verified with local directory")
                .build();

        ExternalFacilityResponseDto updated = ExternalFacilityResponseDto.builder()
                .id(id)
                .name("Reviewed Clinic")
                .importStatus("REVIEWED")
                .build();

        when(externalFacilityService.updateFacilityStatus(id, "REVIEWED")).thenReturn(updated);

        mockMvc.perform(put("/api/v1/external-facilities/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importStatus", is("REVIEWED")));
    }

    @Test
    @DisplayName("POST /api/v1/external-facilities/import - should return 504 Gateway Timeout when Overpass times out")
    void shouldReturn504OnOverpassTimeout() throws Exception {
        ExternalImportRequestDto request = ExternalImportRequestDto.builder()
                .latitude(24.86)
                .longitude(67.01)
                .radiusMeters(3000.0)
                .build();

        when(externalFacilityService.importFacilities(any(ExternalImportRequestDto.class)))
                .thenThrow(new com.huzaifa.rescueroute.common.exception.ExternalServiceException(
                        "OpenStreetMap Overpass API request timed out after 25 seconds.",
                        org.springframework.http.HttpStatus.GATEWAY_TIMEOUT));

        mockMvc.perform(post("/api/v1/external-facilities/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("timed out")));
    }

    @Test
    @DisplayName("POST /api/v1/external-facilities/import - should return 429 Too Many Requests when Overpass rate limited")
    void shouldReturn429OnOverpassRateLimit() throws Exception {
        ExternalImportRequestDto request = ExternalImportRequestDto.builder()
                .latitude(24.86)
                .longitude(67.01)
                .radiusMeters(3000.0)
                .build();

        when(externalFacilityService.importFacilities(any(ExternalImportRequestDto.class)))
                .thenThrow(new com.huzaifa.rescueroute.common.exception.ExternalServiceException(
                        "OpenStreetMap Overpass API rate limit exceeded.",
                        org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));

        mockMvc.perform(post("/api/v1/external-facilities/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("rate limit")));
    }
}
