package com.huzaifa.rescueroute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Dedicated entity for storing externally discovered medical points of interest (e.g. from OpenStreetMap Overpass).
 * External listings are stored separately from internally verified facilities to prevent conflation.
 */
@Entity
@Table(
    name = "external_facilities",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_external_facilities_source_element", columnNames = {"source", "osm_element_type", "osm_element_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String source = "OPENSTREETMAP";

    @Column(name = "osm_element_type", nullable = false, length = 20)
    private String osmElementType; // 'node', 'way', 'relation'

    @Column(name = "osm_element_id", nullable = false)
    private Long osmElementId;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(nullable = false)
    private String name;

    @Column(name = "facility_type", nullable = false, length = 50)
    private String facilityType; // 'Hospital', 'Clinic', 'Pharmacy', 'Blood Bank', 'Ambulance'

    @Column(name = "osm_tags", columnDefinition = "TEXT")
    private String osmTags;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    @Builder.Default
    private String city = "Karachi";

    @Column(name = "phone_number", length = 100)
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String website;

    @Column(name = "opening_hours", columnDefinition = "TEXT")
    private String openingHours;

    @Column(name = "import_status", nullable = false, length = 50)
    @Builder.Default
    private String importStatus = "DISCOVERED"; // 'DISCOVERED', 'DUPLICATE_CANDIDATE', 'REVIEWED', 'SUPPRESSED'

    @Column(name = "matched_facility_id")
    private UUID matchedFacilityId;

    @Column(name = "imported_at")
    @Builder.Default
    private Instant importedAt = Instant.now();

    @Column(name = "last_seen_at")
    @Builder.Default
    private Instant lastSeenAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (importedAt == null) {
            importedAt = now;
        }
        if (lastSeenAt == null) {
            lastSeenAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
