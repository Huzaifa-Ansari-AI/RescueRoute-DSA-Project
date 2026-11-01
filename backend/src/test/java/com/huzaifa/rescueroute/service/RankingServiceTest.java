package com.huzaifa.rescueroute.service;

import com.huzaifa.rescueroute.client.OsrmRoutingClient;
import com.huzaifa.rescueroute.client.RoadMetric;
import com.huzaifa.rescueroute.client.TableMatrixResult;
import com.huzaifa.rescueroute.dto.RankedFacilityResponseDto;
import com.huzaifa.rescueroute.entity.Facility;
import com.huzaifa.rescueroute.entity.Hospital;
import com.huzaifa.rescueroute.mapper.FacilityMapper;
import com.huzaifa.rescueroute.repository.*;
import com.huzaifa.rescueroute.service.impl.RankingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RankingServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private BloodBankRepository bloodBankRepository;

    @Mock
    private PharmacyRepository pharmacyRepository;

    @Mock
    private AmbulanceRepository ambulanceRepository;

    @Mock
    private ClinicRepository clinicRepository;

    @Mock
    private OsrmRoutingClient osrmRoutingClient;

    private RankingServiceImpl rankingService;

    @BeforeEach
    void setUp() {
        FacilityMapper mapper = new FacilityMapper();
        rankingService = new RankingServiceImpl(
                facilityRepository,
                hospitalRepository,
                bloodBankRepository,
                pharmacyRepository,
                ambulanceRepository,
                clinicRepository,
                mapper,
                osrmRoutingClient
        );
    }

    @Test
    @DisplayName("Verify multi-criteria score formula calculations")
    void testScoreFormula() {
        // Distance = 0 km, isVerified = true, isEmergencyAvailable = true, emergencyMode = false, maxDist = 50 km
        // Score = (0.80 * (1 - 0)) + (0.20 * 1) + (0.0 * 1) = 1.0
        double perfectScore = rankingService.calculateScore(0.0, true, true, false, 50.0);
        assertEquals(1.0, perfectScore, 0.0001);

        // Distance = 25 km, isVerified = false, isEmergencyAvailable = false, maxDist = 50 km
        // Score = 0.80 * (1 - 25/50) = 0.40
        double halfDistScore = rankingService.calculateScore(25.0, false, false, false, 50.0);
        assertEquals(0.40, halfDistScore, 0.0001);

        // Distance > maxDist (60 km), isVerified = false, isEmergencyAvailable = false
        // Distance factor clamped to 0 -> Score = 0.0
        double outOfRangeScore = rankingService.calculateScore(60.0, false, false, false, 50.0);
        assertEquals(0.0, outOfRangeScore, 0.0001);
    }

    @Test
    @DisplayName("Emergency mode boosts emergency-ready facilities")
    void testEmergencyModeWeights() {
        // In emergency mode: Wavail is 0.30, Wdist is 0.70, Wverif is 0.0
        double emergencyReadyScore = rankingService.calculateScore(10.0, true, true, true, 50.0);
        double nonEmergencyReadyScore = rankingService.calculateScore(10.0, true, false, true, 50.0);

        assertTrue(emergencyReadyScore > nonEmergencyReadyScore,
                "Emergency ready facility must score significantly higher in emergency mode");
        assertEquals(0.30, emergencyReadyScore - nonEmergencyReadyScore, 0.0001);
    }

    @Test
    @DisplayName("Two-Stage Hybrid Ranking: JPMC ranks ahead of Indus Hospital when road ETA is shorter despite greater straight-line distance")
    void testTwoStageRoadRankingJpmcVsIndus() {
        // Origin: Saddar Karachi (24.8607, 67.0011)
        double userLat = 24.8607;
        double userLng = 67.0011;

        // Indus Hospital: ~6.3 km straight-line, but driving route is 9.0 km and 18 mins
        Facility indus = Facility.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .name("Indus Hospital")
                .facilityType("Hospital")
                .latitude(24.8517)
                .longitude(67.0412)
                .isVerified(true)
                .isActive(true)
                .build();
        indus.setHospital(Hospital.builder().facility(indus).emergencyAvailable(true).build());

        // JPMC: ~6.6 km straight-line (further straight-line), but faster direct driving highway route: 8.7 km, 11 mins
        Facility jpmc = Facility.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .name("JPMC")
                .facilityType("Hospital")
                .latitude(24.8524)
                .longitude(67.0456)
                .isVerified(true)
                .isActive(true)
                .build();
        jpmc.setHospital(Hospital.builder().facility(jpmc).emergencyAvailable(true).build());

        when(facilityRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(indus, jpmc));

        // Mock OSRM Table API batch response: JPMC duration 11 min, Indus duration 18 min
        when(osrmRoutingClient.calculateDistanceMatrixForRanked(eq(userLat), eq(userLng), any()))
                .thenAnswer(invocation -> {
                    List<RankedFacilityResponseDto> candidates = invocation.getArgument(2);
                    List<RoadMetric> metrics = candidates.stream().map(c -> {
                        if ("JPMC".equals(c.getFacility().getName())) {
                            return RoadMetric.builder().distanceKm(8.7).durationMinutes(11).routeAvailable(true).build();
                        } else {
                            return RoadMetric.builder().distanceKm(9.0).durationMinutes(18).routeAvailable(true).build();
                        }
                    }).toList();
                    return TableMatrixResult.success(metrics);
                });

        List<RankedFacilityResponseDto> result =
                rankingService.rankFacilities(userLat, userLng, "Hospital", false, 5);

        assertNotNull(result);
        assertEquals(2, result.size());

        // JPMC must rank #1 because road ETA is 11 mins vs 18 mins
        RankedFacilityResponseDto first = result.get(0);
        assertEquals("JPMC", first.getFacility().getName());
        assertEquals(1, first.getRank());
        assertEquals(8.7, first.getDrivingDistanceKm());
        assertEquals(11, first.getEstimatedTravelMinutes());
        assertEquals("ROAD_NETWORK", first.getDistanceMetricUsed());
        assertTrue(first.isRoutingAvailable());

        // Indus Hospital ranks #2
        RankedFacilityResponseDto second = result.get(1);
        assertEquals("Indus Hospital", second.getFacility().getName());
        assertEquals(2, second.getRank());
        assertEquals(9.0, second.getDrivingDistanceKm());
        assertEquals(18, second.getEstimatedTravelMinutes());
        assertEquals("ROAD_NETWORK", second.getDistanceMetricUsed());
        assertTrue(second.isRoutingAvailable());
    }

    @Test
    @DisplayName("Falls back safely to Haversine ranking when OSRM fails without fabricating metrics")
    void testFallbackToHaversineWhenOsrmFails() {
        double userLat = 24.8607;
        double userLng = 67.0011;

        Facility nearFacility = Facility.builder()
                .id(UUID.randomUUID())
                .name("Near Hospital")
                .facilityType("Hospital")
                .latitude(24.8650)
                .longitude(67.0100)
                .isVerified(true)
                .isActive(true)
                .build();
        nearFacility.setHospital(Hospital.builder().facility(nearFacility).emergencyAvailable(true).build());

        Facility farFacility = Facility.builder()
                .id(UUID.randomUUID())
                .name("Far Hospital")
                .facilityType("Hospital")
                .latitude(24.9500)
                .longitude(67.0700)
                .isVerified(true)
                .isActive(true)
                .build();
        farFacility.setHospital(Hospital.builder().facility(farFacility).emergencyAvailable(true).build());

        when(facilityRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(farFacility, nearFacility));

        // Mock OSRM failure (e.g. timeout)
        when(osrmRoutingClient.calculateDistanceMatrixForRanked(eq(userLat), eq(userLng), any()))
                .thenReturn(TableMatrixResult.failure("OSRM request timed out after 2000ms"));

        List<RankedFacilityResponseDto> result =
                rankingService.rankFacilities(userLat, userLng, "Hospital", false, 5);

        assertNotNull(result);
        assertEquals(2, result.size());

        // Must still rank Near Hospital #1 based on Haversine distance
        assertEquals("Near Hospital", result.get(0).getFacility().getName());
        assertEquals(1, result.get(0).getRank());

        // Both must be explicitly marked as fallback STRAIGHT_LINE with null driving metrics
        for (RankedFacilityResponseDto dto : result) {
            assertEquals("STRAIGHT_LINE", dto.getDistanceMetricUsed());
            assertFalse(dto.isRoutingAvailable());
            assertNull(dto.getDrivingDistanceKm());
            assertNull(dto.getEstimatedTravelMinutes());
            assertNotNull(dto.getStraightLineDistanceKm());
        }
    }

    @Test
    @DisplayName("Partial matrix result: candidate with road route ranks ahead of unreachable candidate")
    void testPartialMatrixReachableBeforeUnreachable() {
        double userLat = 24.8607;
        double userLng = 67.0011;

        Facility reachable = Facility.builder()
                .id(UUID.randomUUID())
                .name("Reachable Hospital")
                .facilityType("Hospital")
                .latitude(24.8700)
                .longitude(67.0200)
                .isVerified(true)
                .isActive(true)
                .build();
        reachable.setHospital(Hospital.builder().facility(reachable).emergencyAvailable(true).build());

        Facility unreachable = Facility.builder()
                .id(UUID.randomUUID())
                .name("Island Clinic")
                .facilityType("Hospital")
                .latitude(24.8610)
                .longitude(67.0020)
                .isVerified(true)
                .isActive(true)
                .build();
        unreachable.setHospital(Hospital.builder().facility(unreachable).emergencyAvailable(true).build());

        when(facilityRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(unreachable, reachable));

        // Reachable has valid metrics; unreachable has routeAvailable = false
        when(osrmRoutingClient.calculateDistanceMatrixForRanked(eq(userLat), eq(userLng), any()))
                .thenAnswer(inv -> {
                    List<RankedFacilityResponseDto> dtos = inv.getArgument(2);
                    List<RoadMetric> metrics = dtos.stream().map(d -> {
                        if ("Reachable Hospital".equals(d.getFacility().getName())) {
                            return RoadMetric.builder().distanceKm(3.0).durationMinutes(8).routeAvailable(true).build();
                        } else {
                            return RoadMetric.builder().routeAvailable(false).build();
                        }
                    }).toList();
                    return TableMatrixResult.success(metrics);
                });

        List<RankedFacilityResponseDto> result =
                rankingService.rankFacilities(userLat, userLng, "Hospital", false, 5);

        assertEquals(2, result.size());
        assertEquals("Reachable Hospital", result.get(0).getFacility().getName());
        assertTrue(result.get(0).isRoutingAvailable());

        assertEquals("Island Clinic", result.get(1).getFacility().getName());
        assertFalse(result.get(1).isRoutingAvailable());
        assertEquals("STRAIGHT_LINE", result.get(1).getDistanceMetricUsed());
    }

    @Test
    @DisplayName("Throws IllegalArgumentException on invalid coordinates")
    void testInvalidCoordinates() {
        assertThrows(IllegalArgumentException.class, () ->
                rankingService.rankFacilities(null, 67.0, null, false, 5));
        assertThrows(IllegalArgumentException.class, () ->
                rankingService.rankFacilities(95.0, 67.0, null, false, 5));
    }
}
