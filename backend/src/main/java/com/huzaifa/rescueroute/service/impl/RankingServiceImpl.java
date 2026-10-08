package com.huzaifa.rescueroute.service.impl;

import com.huzaifa.rescueroute.dsa.TopKRankingEngine;
import com.huzaifa.rescueroute.dto.FacilityResponseDto;
import com.huzaifa.rescueroute.dto.RankedFacilityResponseDto;
import com.huzaifa.rescueroute.entity.Facility;
import com.huzaifa.rescueroute.mapper.FacilityMapper;
import com.huzaifa.rescueroute.repository.*;
import com.huzaifa.rescueroute.repository.specification.FacilitySpecifications;
import com.huzaifa.rescueroute.service.RankingService;
import com.huzaifa.rescueroute.util.HaversineUtil;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Production implementation of RankingService executing distance calculation
 * and Top-K priority queue ranking.
 */
@Service
@Transactional(readOnly = true)
public class RankingServiceImpl implements RankingService {

    public static final double DEFAULT_WEIGHT_DISTANCE = 0.50;
    public static final double DEFAULT_WEIGHT_VERIFICATION = 0.25;
    public static final double DEFAULT_WEIGHT_AVAILABILITY = 0.25;
    public static final double DEFAULT_MAX_DISTANCE_KM = 50.0;

    private final FacilityRepository facilityRepository;
    private final HospitalRepository hospitalRepository;
    private final BloodBankRepository bloodBankRepository;
    private final PharmacyRepository pharmacyRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final FacilityMapper facilityMapper;

    public RankingServiceImpl(
            FacilityRepository facilityRepository,
            HospitalRepository hospitalRepository,
            BloodBankRepository bloodBankRepository,
            PharmacyRepository pharmacyRepository,
            AmbulanceRepository ambulanceRepository,
            FacilityMapper facilityMapper) {
        this.facilityRepository = facilityRepository;
        this.hospitalRepository = hospitalRepository;
        this.bloodBankRepository = bloodBankRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.ambulanceRepository = ambulanceRepository;
        this.facilityMapper = facilityMapper;
    }

    @Override
    public List<RankedFacilityResponseDto> rankFacilities(
            Double userLat,
            Double userLng,
            String facilityType,
            Boolean emergency,
            Integer k) {

        // Validate coordinates prior to execution
        HaversineUtil.validateCoordinate(userLat, userLng, "User");

        boolean isEmergencyMode = Boolean.TRUE.equals(emergency);
        int topK = (k != null && k > 0) ? k : 10;

        // Fetch candidate facilities (active facilities of requested category, or all active)
        Specification<Facility> spec = FacilitySpecifications.withFilters(facilityType, null, true);
        List<Facility> candidateEntities = facilityRepository.findAll(spec);

        if (candidateEntities.isEmpty()) {
            return List.of();
        }

        // Calculate distance for all candidates to dynamically determine max distance scale
        double maxObservedDistance = DEFAULT_MAX_DISTANCE_KM;
        List<ScoredCandidate> scoredCandidates = new ArrayList<>(candidateEntities.size());

        for (Facility entity : candidateEntities) {
            enrichSubtypeIfMissing(entity);

            double distance = HaversineUtil.calculateDistance(
                    userLat, userLng, entity.getLatitude(), entity.getLongitude());

            if (distance > maxObservedDistance) {
                maxObservedDistance = distance;
            }

            boolean isAvail = checkEmergencyAvailability(entity);

            scoredCandidates.add(new ScoredCandidate(entity, distance, isAvail));
        }

        // Compute normalized multi-criteria scores
        List<RankedFacilityResponseDto> scoredDtos = new ArrayList<>(scoredCandidates.size());
        for (ScoredCandidate sc : scoredCandidates) {
            Facility entity = sc.facility;
            double distance = sc.distance;
            boolean emergencyAvailable = sc.emergencyAvailable;

            double score = calculateScore(
                    distance, entity.isVerified(), emergencyAvailable, isEmergencyMode, maxObservedDistance);

            FacilityResponseDto facilityDto = facilityMapper.toDto(entity);

            // Round distance to 2 decimal places and score to 4 decimal places for clean API response
            double roundedDistance = Math.round(distance * 100.0) / 100.0;
            double roundedScore = Math.round(score * 10000.0) / 10000.0;

            scoredDtos.add(RankedFacilityResponseDto.builder()
                    .facility(facilityDto)
                    .distanceKm(roundedDistance)
                    .score(roundedScore)
                    .isVerified(entity.isVerified())
                    .emergencyAvailable(emergencyAvailable)
                    .build());
        }

        // Execute DSA Top-K Min-Heap selection: O(N log K) time, O(K) space
        return TopKRankingEngine.getTopK(scoredDtos, topK);
    }

    /**
     * Calculates the multi-criteria ranking suitability score.
     *
     * <p>Formula:
     * Score = (Wdist * (1 - dist/maxDist)) + (Wverif * isVerified) + (Wavail * isAvailable)
     */
    public double calculateScore(
            double distance,
            boolean isVerified,
            boolean emergencyAvailable,
            boolean emergencyMode,
            double maxDistance) {

        double wDist = emergencyMode ? 0.40 : DEFAULT_WEIGHT_DISTANCE;
        double wAvail = emergencyMode ? 0.45 : DEFAULT_WEIGHT_AVAILABILITY;
        double wVerif = emergencyMode ? 0.15 : DEFAULT_WEIGHT_VERIFICATION;

        double effectiveMax = Math.max(1.0, maxDistance);
        double distFactor = Math.max(0.0, 1.0 - (distance / effectiveMax));
        double verifFactor = isVerified ? 1.0 : 0.0;
        double availFactor = emergencyAvailable ? 1.0 : 0.0;

        double totalScore = (wDist * distFactor) + (wVerif * verifFactor) + (wAvail * availFactor);
        return Math.max(0.0, Math.min(1.0, totalScore));
    }

    /**
     * Determines emergency readiness and 24/7 service capability across facility subtypes.
     */
    public boolean checkEmergencyAvailability(Facility facility) {
        if (facility == null) {
            return false;
        }

        if ("Hospital".equalsIgnoreCase(facility.getFacilityType()) && facility.getHospital() != null) {
            return facility.getHospital().isEmergencyAvailable();
        } else if ("Pharmacy".equalsIgnoreCase(facility.getFacilityType()) && facility.getPharmacy() != null) {
            return facility.getPharmacy().isEmergencyMedicinesAvailable() || facility.getPharmacy().isOpen24Hours();
        } else if ("Ambulance".equalsIgnoreCase(facility.getFacilityType()) && facility.getAmbulance() != null) {
            return facility.getAmbulance().isAvailableNow();
        } else if ("Blood Bank".equalsIgnoreCase(facility.getFacilityType()) && facility.getBloodBank() != null) {
            return facility.getBloodBank().isOPositive() || facility.getBloodBank().isONegative();
        }

        return false;
    }

    private void enrichSubtypeIfMissing(Facility facility) {
        if (facility == null) {
            return;
        }
        if (facility.getHospital() == null && "Hospital".equalsIgnoreCase(facility.getFacilityType())) {
            hospitalRepository.findByFacilityId(facility.getId()).ifPresent(facility::setHospital);
        } else if (facility.getBloodBank() == null && "Blood Bank".equalsIgnoreCase(facility.getFacilityType())) {
            bloodBankRepository.findByFacilityId(facility.getId()).ifPresent(facility::setBloodBank);
        } else if (facility.getPharmacy() == null && "Pharmacy".equalsIgnoreCase(facility.getFacilityType())) {
            pharmacyRepository.findByFacilityId(facility.getId()).ifPresent(facility::setPharmacy);
        } else if (facility.getAmbulance() == null && "Ambulance".equalsIgnoreCase(facility.getFacilityType())) {
            ambulanceRepository.findByFacilityId(facility.getId()).ifPresent(facility::setAmbulance);
        }
    }

    private static class ScoredCandidate {
        final Facility facility;
        final double distance;
        final boolean emergencyAvailable;

        ScoredCandidate(Facility facility, double distance, boolean emergencyAvailable) {
            this.facility = facility;
            this.distance = distance;
            this.emergencyAvailable = emergencyAvailable;
        }
    }
}
