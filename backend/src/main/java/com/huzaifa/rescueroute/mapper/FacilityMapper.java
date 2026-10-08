package com.huzaifa.rescueroute.mapper;

import com.huzaifa.rescueroute.dto.*;
import com.huzaifa.rescueroute.entity.*;
import org.springframework.stereotype.Component;

/**
 * Explicit mapper for converting JPA entities to API response DTOs.
 */
@Component
public class FacilityMapper {

    /**
     * Maps a Facility entity and its associated subtype details to FacilityResponseDto.
     */
    public FacilityResponseDto toDto(Facility facility) {
        if (facility == null) {
            return null;
        }

        FacilityResponseDto.FacilityResponseDtoBuilder builder = FacilityResponseDto.builder()
                .id(facility.getId())
                .name(facility.getName())
                .facilityType(facility.getFacilityType())
                .address(facility.getAddress())
                .city(facility.getCity())
                .latitude(facility.getLatitude())
                .longitude(facility.getLongitude())
                .phoneNumber(facility.getPhoneNumber())
                .isVerified(facility.isVerified())
                .isActive(facility.isActive())
                .createdAt(facility.getCreatedAt())
                .updatedAt(facility.getUpdatedAt());

        if (facility.getHospital() != null) {
            builder.hospitalDetails(toHospitalDetailsDto(facility.getHospital()));
        }
        if (facility.getBloodBank() != null) {
            builder.bloodBankDetails(toBloodBankDetailsDto(facility.getBloodBank()));
        }
        if (facility.getPharmacy() != null) {
            builder.pharmacyDetails(toPharmacyDetailsDto(facility.getPharmacy()));
        }
        if (facility.getAmbulance() != null) {
            builder.ambulanceDetails(toAmbulanceDetailsDto(facility.getAmbulance()));
        }

        return builder.build();
    }

    /**
     * Maps Hospital entity to HospitalDetailsDto.
     */
    public HospitalDetailsDto toHospitalDetailsDto(Hospital hospital) {
        if (hospital == null) {
            return null;
        }
        return HospitalDetailsDto.builder()
                .id(hospital.getId())
                .emergencyAvailable(hospital.isEmergencyAvailable())
                .icuAvailable(hospital.isIcuAvailable())
                .traumaCenterAvailable(hospital.isTraumaCenterAvailable())
                .cardiologyAvailable(hospital.isCardiologyAvailable())
                .pediatricsAvailable(hospital.isPediatricsAvailable())
                .maternityAvailable(hospital.isMaternityAvailable())
                .notes(hospital.getNotes())
                .build();
    }

    /**
     * Maps BloodBank entity to BloodBankDetailsDto.
     */
    public BloodBankDetailsDto toBloodBankDetailsDto(BloodBank bloodBank) {
        if (bloodBank == null) {
            return null;
        }
        return BloodBankDetailsDto.builder()
                .id(bloodBank.getId())
                .oPositive(bloodBank.isOPositive())
                .oNegative(bloodBank.isONegative())
                .aPositive(bloodBank.isAPositive())
                .aNegative(bloodBank.isANegative())
                .bPositive(bloodBank.isBPositive())
                .bNegative(bloodBank.isBNegative())
                .abPositive(bloodBank.isAbPositive())
                .abNegative(bloodBank.isAbNegative())
                .stockUpdatedAt(bloodBank.getStockUpdatedAt())
                .notes(bloodBank.getNotes())
                .build();
    }

    /**
     * Maps Pharmacy entity to PharmacyDetailsDto.
     */
    public PharmacyDetailsDto toPharmacyDetailsDto(Pharmacy pharmacy) {
        if (pharmacy == null) {
            return null;
        }
        return PharmacyDetailsDto.builder()
                .id(pharmacy.getId())
                .emergencyMedicinesAvailable(pharmacy.isEmergencyMedicinesAvailable())
                .open24Hours(pharmacy.isOpen24Hours())
                .prescriptionRequired(pharmacy.isPrescriptionRequired())
                .deliveryAvailable(pharmacy.isDeliveryAvailable())
                .notes(pharmacy.getNotes())
                .build();
    }

    /**
     * Maps Ambulance entity to AmbulanceDetailsDto.
     */
    public AmbulanceDetailsDto toAmbulanceDetailsDto(Ambulance ambulance) {
        if (ambulance == null) {
            return null;
        }
        return AmbulanceDetailsDto.builder()
                .id(ambulance.getId())
                .availableNow(ambulance.isAvailableNow())
                .responseTimeMinutes(ambulance.getResponseTimeMinutes())
                .coverageArea(ambulance.getCoverageArea())
                .equippedForIcuTransfer(ambulance.isEquippedForIcuTransfer())
                .notes(ambulance.getNotes())
                .build();
    }
}
