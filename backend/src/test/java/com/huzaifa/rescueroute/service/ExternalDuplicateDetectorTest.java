package com.huzaifa.rescueroute.service;

import com.huzaifa.rescueroute.entity.Facility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ExternalDuplicateDetectorTest {

    private ExternalDuplicateDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ExternalDuplicateDetector();
    }

    private Facility buildFacility(String name, String type, double lat, double lon) {
        return Facility.builder()
                .id(UUID.randomUUID())
                .name(name)
                .facilityType(type)
                .latitude(lat)
                .longitude(lon)
                .isVerified(true)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should detect high-confidence duplicate when name and location are within 150m and same category")
    void shouldDetectDuplicateWithinProximity() {
        Facility existing = buildFacility("Liaquat National Hospital", "Hospital", 24.8940, 67.0700);

        // 50 meters away
        Optional<Facility> match = detector.findPotentialDuplicate(
                24.8942, 67.0702,
                "Liaquat National Hospital Karachi",
                "Hospital",
                List.of(existing)
        );

        assertTrue(match.isPresent());
        assertEquals(existing.getId(), match.get().getId());
    }

    @Test
    @DisplayName("Should NOT flag as duplicate if distance exceeds 150 meters even with identical name")
    void shouldNotDetectDuplicateIfDistanceExceedsThreshold() {
        // ~1 km away
        Facility existing = buildFacility("Indus Hospital", "Hospital", 24.8400, 67.0800);

        Optional<Facility> match = detector.findPotentialDuplicate(
                24.8500, 67.0800,
                "Indus Hospital",
                "Hospital",
                List.of(existing)
        );

        assertTrue(match.isEmpty(), "Should not match facilities located 1 km apart");
    }

    @Test
    @DisplayName("Should NOT flag as duplicate if facility category differs even if at the exact same location")
    void shouldNotDetectDuplicateIfCategoryDiffers() {
        Facility existing = buildFacility("Medicare Clinic", "Clinic", 24.8700, 67.0500);

        Optional<Facility> match = detector.findPotentialDuplicate(
                24.8700, 67.0500,
                "Medicare Pharmacy",
                "Pharmacy",
                List.of(existing)
        );

        assertTrue(match.isEmpty(), "Pharmacy and Clinic should not be conflated as duplicates");
    }

    @Test
    @DisplayName("Should handle string normalization with punctuation, case, and extra whitespace")
    void shouldNormalizeStringsRobustly() {
        assertTrue(detector.isNameSimilar(
                "Dr. Ziauddin Hospital (Clifton Campus)",
                "dr ziauddin hospital clifton"
        ));

        assertTrue(detector.isNameSimilar(
                "AL-MUSTAFA MEDICAL CENTRE",
                "Al Mustafa Medical Center"
        ));

        assertFalse(detector.isNameSimilar(
                "City Diagnostic Lab",
                "Sunrise Maternity Home"
        ));
    }

    @Test
    @DisplayName("Should safely return empty when candidate list is empty or coordinates are null")
    void shouldHandleEmptyOrNullGracefully() {
        Optional<Facility> match1 = detector.findPotentialDuplicate(
                24.86, 67.00, "Civil Hospital", "Hospital", Collections.emptyList()
        );
        assertTrue(match1.isEmpty());

        Optional<Facility> match2 = detector.findPotentialDuplicate(
                null, 67.00, "Civil Hospital", "Hospital", List.of(buildFacility("Civil Hospital", "Hospital", 24.86, 67.00))
        );
        assertTrue(match2.isEmpty());
    }
}
