package com.huzaifa.rescueroute.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object representing pharmacy operational and medicine availability.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PharmacyDetailsDto {
    private UUID id;
    private boolean emergencyMedicinesAvailable;
    private boolean open24Hours;
    private boolean prescriptionRequired;
    private boolean deliveryAvailable;
    private String notes;
}
