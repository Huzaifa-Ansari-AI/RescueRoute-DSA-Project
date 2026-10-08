package com.huzaifa.rescueroute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Hospital-specific details entity.
 */
@Entity
@Table(name = "hospitals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", unique = true, nullable = false)
    private Facility facility;

    @Column(name = "emergency_available", nullable = false)
    @Builder.Default
    private boolean emergencyAvailable = false;

    @Column(name = "icu_available", nullable = false)
    @Builder.Default
    private boolean icuAvailable = false;

    @Column(name = "trauma_center_available", nullable = false)
    @Builder.Default
    private boolean traumaCenterAvailable = false;

    @Column(name = "cardiology_available", nullable = false)
    @Builder.Default
    private boolean cardiologyAvailable = false;

    @Column(name = "pediatrics_available", nullable = false)
    @Builder.Default
    private boolean pediatricsAvailable = false;

    @Column(name = "maternity_available", nullable = false)
    @Builder.Default
    private boolean maternityAvailable = false;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
