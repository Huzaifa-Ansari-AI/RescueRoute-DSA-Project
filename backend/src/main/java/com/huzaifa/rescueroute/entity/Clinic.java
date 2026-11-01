package com.huzaifa.rescueroute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Clinic-specific details entity representing outpatient consultations and diagnostic centers.
 */
@Entity
@Table(name = "clinics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clinic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", unique = true, nullable = false)
    private Facility facility;

    @Column(name = "consultation_available", nullable = false)
    @Builder.Default
    private boolean consultationAvailable = true;

    @Column(columnDefinition = "TEXT")
    private String specialties;

    @Column(name = "open_weekends", nullable = false)
    @Builder.Default
    private boolean openWeekends = false;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
