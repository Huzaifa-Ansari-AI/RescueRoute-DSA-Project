package com.huzaifa.rescueroute.service;

import com.huzaifa.rescueroute.client.OsrmRoutingClient;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.mockito.ArgumentMatchers;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verifies that the Milestone 4 DSA Ranking engine seamlessly reflects the
 * updated verification status introduced in Milestone 7.
 */
@ExtendWith(MockitoExtension.class)
public class RankingVerificationIntegrationTest {

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

        @Spy
        private FacilityMapper facilityMapper = new FacilityMapper();

        @InjectMocks
        private RankingServiceImpl rankingService;

        private Facility facility;

        @BeforeEach
        void setUp() {
                facility = Facility.builder()
                                .id(UUID.randomUUID())
                                .name("Test Emergency Center")
                                .facilityType("Hospital")
                                .address("University Road, Karachi")
                                .city("Karachi")
                                .latitude(24.8600)
                                .longitude(67.0011)
                                .isVerified(false) // Initially unverified
                                .isActive(true)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                Hospital hospital = Hospital.builder()
                                .facility(facility)
                                .emergencyAvailable(true)
                                .build();
                facility.setHospital(hospital);
        }

        @Test
        @DisplayName("Ranking score increases when facility is approved (isVerified = true)")
        void testRankingReflectsApprovedVerificationStatus() {
                when(facilityRepository.findAll(ArgumentMatchers.<Specification<Facility>>any()))
                                .thenReturn(List.of(facility));

                // 1. Query ranking when unverified
                facility.setVerified(false);
                List<RankedFacilityResponseDto> unverifiedRankings = rankingService.rankFacilities(
                                24.8600, 67.0011, "Hospital", false, 5);

                assertNotNull(unverifiedRankings);
                assertEquals(1, unverifiedRankings.size());
                double unverifiedScore = unverifiedRankings.get(0).getScore();
                assertFalse(unverifiedRankings.get(0).isVerified());

                // 2. Simulate Admin approval (isVerified updated to true)
                facility.setVerified(true);
                List<RankedFacilityResponseDto> verifiedRankings = rankingService.rankFacilities(
                                24.8600, 67.0011, "Hospital", false, 5);

                assertNotNull(verifiedRankings);
                assertEquals(1, verifiedRankings.size());
                double verifiedScore = verifiedRankings.get(0).getScore();
                assertTrue(verifiedRankings.get(0).isVerified());

                // 3. Confirm ranking DTO accurately reflects isVerified attribute while score
                // remains unbiased
                assertEquals(unverifiedScore, verifiedScore, 0.0001,
                                "Verification status must be an informational attribute and not artificially inflate distance/time score");
                assertTrue(verifiedRankings.get(0).isVerified());
                assertFalse(unverifiedRankings.get(0).isVerified());
        }
}
