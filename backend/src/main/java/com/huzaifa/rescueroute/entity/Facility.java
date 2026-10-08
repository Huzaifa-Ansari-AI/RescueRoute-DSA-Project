package com.huzaifa.rescueroute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Base medical facility entity storing shared metadata across all resource types.
 */
@Entity
@Table(name = "facilities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "facility_type", nullable = false, length = 50)
    private String facilityType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String city = "Karachi";

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean isVerified = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToOne(mappedBy = "facility", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Hospital hospital;

    @OneToOne(mappedBy = "facility", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private BloodBank bloodBank;

    @OneToOne(mappedBy = "facility", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Pharmacy pharmacy;

    @OneToOne(mappedBy = "facility", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Ambulance ambulance;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
