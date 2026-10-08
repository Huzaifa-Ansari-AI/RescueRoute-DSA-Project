package com.huzaifa.rescueroute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Pharmacy-specific details entity.
 */
@Entity
@Table(name = "pharmacies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pharmacy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", unique = true, nullable = false)
    private Facility facility;

    @Column(name = "emergency_medicines_available", nullable = false)
    @Builder.Default
    private boolean emergencyMedicinesAvailable = false;

    @Column(name = "open_24_hours", nullable = false)
    @Builder.Default
    private boolean open24Hours = false;

    @Column(name = "prescription_required", nullable = false)
    @Builder.Default
    private boolean prescriptionRequired = true;

    @Column(name = "delivery_available", nullable = false)
    @Builder.Default
    private boolean deliveryAvailable = false;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
