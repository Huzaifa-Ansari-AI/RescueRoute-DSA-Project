package com.huzaifa.rescueroute.service;

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
                mapper
        );
    }

    @Test
    @DisplayName("Verify multi-criteria score formula calculations")
    void testScoreFormula() {
        // Distance = 0 km, isVerified = true, isEmergencyAvailable = true, emergencyMode = false, maxDist = 50 km
        // Score = (0.50 * (1 - 0)) + (0.25 * 1) + (0.25 * 1) = 1.0
        double perfectScore = rankingService.calculateScore(0.0, true, true, false, 50.0);
        assertEquals(1.0, perfectScore, 0.0001);

        // Distance = 25 km, isVerified = false, isEmergencyAvailable = false, maxDist = 50 km
        // Score = 0.50 * (1 - 25/50) = 0.25
        double halfDistScore = rankingService.calculateScore(25.0, false, false, false, 50.0);
        assertEquals(0.25, halfDistScore, 0.0001);

        // Distance > maxDist (60 km), isVerified = false, isEmergencyAvailable = false
        // Distance factor clamped to 0 -> Score = 0.0
        double outOfRangeScore = rankingService.calculateScore(60.0, false, false, false, 50.0);
        assertEquals(0.0, outOfRangeScore, 0.0001);
    }

    @Test
    @DisplayName("Emergency mode boosts emergency-ready facilities")
    void testEmergencyModeWeights() {
        // In emergency mode: Wavail is 0.45, Wdist is 0.40, Wverif is 0.15
        double emergencyReadyScore = rankingService.calculateScore(10.0, true, true, true, 50.0);
        double nonEmergencyReadyScore = rankingService.calculateScore(10.0, true, false, true, 50.0);

        assertTrue(emergencyReadyScore > nonEmergencyReadyScore,
                "Emergency ready facility must score significantly higher in emergency mode");
        assertEquals(0.45, emergencyReadyScore - nonEmergencyReadyScore, 0.0001);
    }

    @Test
    @DisplayName("Rank facilities relative to user position")
    void testRankFacilities() {
        // User at Karachi Saddar: 24.8607, 67.0011
        double userLat = 24.8607;
        double userLng = 67.0011;

        // Near hospital: ~1.5 km
        Facility nearFacility = Facility.builder()
                .id(UUID.randomUUID())
                .name("Near Hospital")
                .facilityType("Hospital")
                .latitude(24.8650)
                .longitude(67.0100)
                .isVerified(true)
                .isActive(true)
                .build();
        Hospital nearHospital = Hospital.builder()
                .facility(nearFacility)
                .emergencyAvailable(true)
                .build();
        nearFacility.setHospital(nearHospital);

        // Far hospital: ~10 km
        Facility farFacility = Facility.builder()
                .id(UUID.randomUUID())
                .name("Far Hospital")
                .facilityType("Hospital")
                .latitude(24.9500)
                .longitude(67.0700)
                .isVerified(false)
                .isActive(true)
                .build();
        Hospital farHospital = Hospital.builder()
                .facility(farFacility)
                .emergencyAvailable(false)
                .build();
        farFacility.setHospital(farHospital);

        when(facilityRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(farFacility, nearFacility));

        List<RankedFacilityResponseDto> result =
                rankingService.rankFacilities(userLat, userLng, "Hospital", false, 5);

        assertNotNull(result);
        assertEquals(2, result.size());

        // Top rank must be Near Hospital
        assertEquals("Near Hospital", result.get(0).getFacility().getName());
        assertEquals(1, result.get(0).getRank());
        assertTrue(result.get(0).getDistanceKm() < result.get(1).getDistanceKm());
        assertTrue(result.get(0).getScore() > result.get(1).getScore());
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
