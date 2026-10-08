package com.huzaifa.rescueroute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Ambulance service details entity.
 */
@Entity
@Table(name = "ambulances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ambulance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", unique = true, nullable = false)
    private Facility facility;

    @Column(name = "available_now", nullable = false)
    @Builder.Default
    private boolean availableNow = false;

    @Column(name = "response_time_minutes")
    private Integer responseTimeMinutes;

    @Column(name = "coverage_area")
    private String coverageArea;

    @Column(name = "equipped_for_icu_transfer", nullable = false)
    @Builder.Default
    private boolean equippedForIcuTransfer = false;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
