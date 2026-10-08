package com.huzaifa.rescueroute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Historical record of emergency route calculations.
 */
@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_latitude", nullable = false)
    private Double sourceLatitude;

    @Column(name = "source_longitude", nullable = false)
    private Double sourceLongitude;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_facility_id", nullable = false)
    private Facility destinationFacility;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "travel_time_minutes")
    private Double travelTimeMinutes;

    @Column(name = "route_polyline", columnDefinition = "TEXT")
    private String routePolyline;

    @Column(name = "emergency_type", length = 100)
    private String emergencyType;

    @Column(name = "calculated_at", nullable = false, updatable = false)
    private Instant calculatedAt;

    @PrePersist
    protected void onCreate() {
        if (calculatedAt == null) {
            calculatedAt = Instant.now();
        }
    }
}
