package com.huzaifa.rescueroute.service;

import com.huzaifa.rescueroute.client.OsmElement;
import com.huzaifa.rescueroute.client.OverpassClient;
import com.huzaifa.rescueroute.dto.ExternalFacilityResponseDto;
import com.huzaifa.rescueroute.dto.ExternalImportRequestDto;
import com.huzaifa.rescueroute.dto.ExternalImportSummaryDto;
import com.huzaifa.rescueroute.entity.ExternalFacility;
import com.huzaifa.rescueroute.entity.Facility;
import com.huzaifa.rescueroute.repository.ExternalFacilityRepository;
import com.huzaifa.rescueroute.repository.FacilityRepository;
import com.huzaifa.rescueroute.service.impl.ExternalFacilityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExternalFacilityServiceTest {

    @Mock
    private OverpassClient overpassClient;

    @Mock
    private ExternalFacilityRepository externalFacilityRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private ExternalDuplicateDetector duplicateDetector;

    private ExternalFacilityServiceImpl externalFacilityService;

    @BeforeEach
    void setUp() {
        externalFacilityService = new ExternalFacilityServiceImpl(
                overpassClient,
                externalFacilityRepository,
                facilityRepository,
                duplicateDetector
        );
    }

    @Test
    @DisplayName("Should successfully import new external POI and mark DISCOVERED when no duplicate detected")
    void shouldImportNewExternalFacility() {
        OsmElement elem = OsmElement.builder()
                .elementType("node")
                .elementId(7001L)
                .name("New Community Clinic")
                .facilityType("Clinic")
                .latitude(24.8600)
                .longitude(67.0100)
                .address("Block 5, Clifton")
                .sourceUrl("https://www.openstreetmap.org/node/7001")
                .build();

        when(overpassClient.queryMedicalFacilities(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.of(elem));
        when(facilityRepository.findAll()).thenReturn(Collections.emptyList());
        when(externalFacilityRepository.findBySourceAndOsmElementTypeAndOsmElementId("OPENSTREETMAP", "node", 7001L))
                .thenReturn(Optional.empty());
        when(duplicateDetector.findPotentialDuplicate(anyDouble(), anyDouble(), anyString(), anyString(), anyList()))
                .thenReturn(Optional.empty());

        ExternalImportRequestDto request = ExternalImportRequestDto.builder()
                .latitude(24.86)
                .longitude(67.01)
                .radiusMeters(3000.0)
                .build();

        ExternalImportSummaryDto summary = externalFacilityService.importFacilities(request);

        assertNotNull(summary);
        assertEquals(1, summary.getTotalQueried());
        assertEquals(1, summary.getImported());
        assertEquals(0, summary.getUpdated());
        assertEquals(0, summary.getDuplicateCandidates());

        ArgumentCaptor<ExternalFacility> captor = ArgumentCaptor.forClass(ExternalFacility.class);
        verify(externalFacilityRepository).save(captor.capture());

        ExternalFacility saved = captor.getValue();
        assertEquals("New Community Clinic", saved.getName());
        assertEquals("Clinic", saved.getFacilityType());
        assertEquals("DISCOVERED", saved.getImportStatus());
        assertNull(saved.getMatchedFacilityId());
    }

    @Test
    @DisplayName("Should flag DUPLICATE_CANDIDATE when potential duplicate internal facility exists without destroying records")
    void shouldFlagDuplicateCandidate() {
        OsmElement elem = OsmElement.builder()
                .elementType("node")
                .elementId(7002L)
                .name("Aga Khan Hospital Clifton")
                .facilityType("Hospital")
                .latitude(24.8200)
                .longitude(67.0300)
                .build();

        UUID internalId = UUID.randomUUID();
        Facility internalMatch = Facility.builder()
                .id(internalId)
                .name("The Aga Khan Hospital Clifton")
                .facilityType("Hospital")
                .latitude(24.8205)
                .longitude(67.0305)
                .build();

        when(overpassClient.queryMedicalFacilities(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.of(elem));
        when(facilityRepository.findAll()).thenReturn(List.of(internalMatch));
        when(externalFacilityRepository.findBySourceAndOsmElementTypeAndOsmElementId("OPENSTREETMAP", "node", 7002L))
                .thenReturn(Optional.empty());
        when(duplicateDetector.findPotentialDuplicate(anyDouble(), anyDouble(), anyString(), anyString(), anyList()))
                .thenReturn(Optional.of(internalMatch));

        ExternalImportRequestDto request = ExternalImportRequestDto.builder()
                .latitude(24.82)
                .longitude(67.03)
                .radiusMeters(2000.0)
                .build();

        ExternalImportSummaryDto summary = externalFacilityService.importFacilities(request);

        assertEquals(1, summary.getDuplicateCandidates());

        ArgumentCaptor<ExternalFacility> captor = ArgumentCaptor.forClass(ExternalFacility.class);
        verify(externalFacilityRepository).save(captor.capture());

        ExternalFacility saved = captor.getValue();
        assertEquals("DUPLICATE_CANDIDATE", saved.getImportStatus());
        assertEquals(internalId, saved.getMatchedFacilityId());
    }

    @Test
    @DisplayName("Should perform idempotent update of existing external record and preserve admin REVIEWED status")
    void shouldUpdateExistingExternalFacilityIdempotently() {
        OsmElement elem = OsmElement.builder()
                .elementType("way")
                .elementId(8001L)
                .name("Updated Hospital Name")
                .facilityType("Hospital")
                .latitude(24.8700)
                .longitude(67.0200)
                .build();

        ExternalFacility existing = ExternalFacility.builder()
                .id(UUID.randomUUID())
                .source("OPENSTREETMAP")
                .osmElementType("way")
                .osmElementId(8001L)
                .name("Old Hospital Name")
                .facilityType("Hospital")
                .latitude(24.8700)
                .longitude(67.0200)
                .importStatus("REVIEWED") // Pre-existing admin review
                .build();

        when(overpassClient.queryMedicalFacilities(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.of(elem));
        when(facilityRepository.findAll()).thenReturn(Collections.emptyList());
        when(externalFacilityRepository.findBySourceAndOsmElementTypeAndOsmElementId("OPENSTREETMAP", "way", 8001L))
                .thenReturn(Optional.of(existing));

        ExternalImportRequestDto request = ExternalImportRequestDto.builder()
                .latitude(24.87)
                .longitude(67.02)
                .radiusMeters(1000.0)
                .build();

        ExternalImportSummaryDto summary = externalFacilityService.importFacilities(request);

        assertEquals(0, summary.getImported());
        assertEquals(1, summary.getUpdated());

        ArgumentCaptor<ExternalFacility> captor = ArgumentCaptor.forClass(ExternalFacility.class);
        verify(externalFacilityRepository).save(captor.capture());

        ExternalFacility updated = captor.getValue();
        assertEquals("Updated Hospital Name", updated.getName());
        assertEquals("REVIEWED", updated.getImportStatus(), "Admin REVIEWED status must be preserved");
    }

    @Test
    @DisplayName("Should search external facilities, filter by radius, and strictly set isVerified=false")
    void shouldSearchExternalFacilitiesWithinRadius() {
        ExternalFacility f1 = ExternalFacility.builder()
                .id(UUID.randomUUID())
                .name("Close Clinic")
                .facilityType("Clinic")
                .latitude(24.8610)
                .longitude(67.0010)
                .importStatus("DISCOVERED")
                .build();

        ExternalFacility f2 = ExternalFacility.builder()
                .id(UUID.randomUUID())
                .name("Distant Clinic")
                .facilityType("Clinic")
                .latitude(25.1000)
                .longitude(67.5000)
                .importStatus("DISCOVERED")
                .build();

        when(externalFacilityRepository.findByLatitudeBetweenAndLongitudeBetween(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(f1, f2));
        when(facilityRepository.findAll()).thenReturn(Collections.emptyList());

        List<ExternalFacilityResponseDto> results = externalFacilityService.searchExternalFacilities(
                24.8600, 67.0000, 2000.0, "Clinic", null
        );

        assertNotNull(results);
        assertEquals(1, results.size(), "Only f1 is within 2000m radius");

        ExternalFacilityResponseDto dto = results.get(0);
        assertEquals("Close Clinic", dto.getName());
        assertFalse(dto.isVerified(), "External listing MUST NEVER be verified");
        assertEquals("© OpenStreetMap contributors", dto.getAttribution());
        assertTrue(dto.getDisclaimer().contains("Not verified by RescueRoute"));
    }

    @Test
    @DisplayName("Should return genuine empty summary with totalQueried=0 when Overpass returns empty collection")
    void shouldReturnZeroSummaryOnGenuineEmptyOverpassResult() {
        when(overpassClient.queryMedicalFacilities(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(Collections.emptyList());

        ExternalImportRequestDto request = ExternalImportRequestDto.builder()
                .latitude(24.86)
                .longitude(67.01)
                .radiusMeters(3000.0)
                .build();

        ExternalImportSummaryDto summary = externalFacilityService.importFacilities(request);

        assertNotNull(summary);
        assertEquals(0, summary.getTotalQueried());
        assertEquals(0, summary.getImported());
        assertEquals(0, summary.getUpdated());
        assertTrue(summary.getMessage().contains("No matching medical facilities"));
        verify(externalFacilityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should propagate ExternalServiceException without masking when Overpass query fails")
    void shouldPropagateExternalServiceExceptionOnOverpassFailure() {
        when(overpassClient.queryMedicalFacilities(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenThrow(new com.huzaifa.rescueroute.common.exception.ExternalServiceException(
                        "Overpass request timed out",
                        org.springframework.http.HttpStatus.GATEWAY_TIMEOUT));

        ExternalImportRequestDto request = ExternalImportRequestDto.builder()
                .latitude(24.86)
                .longitude(67.01)
                .radiusMeters(3000.0)
                .build();

        com.huzaifa.rescueroute.common.exception.ExternalServiceException ex =
                assertThrows(com.huzaifa.rescueroute.common.exception.ExternalServiceException.class, () ->
                        externalFacilityService.importFacilities(request));

        assertEquals(org.springframework.http.HttpStatus.GATEWAY_TIMEOUT, ex.getStatus());
        assertTrue(ex.getMessage().contains("timed out"));
        verify(externalFacilityRepository, never()).save(any());
    }
}
