package com.huzaifa.rescueroute.service.impl;

import com.huzaifa.rescueroute.client.OsmElement;
import com.huzaifa.rescueroute.client.OsrmRoutingClient;
import com.huzaifa.rescueroute.client.OverpassClient;
import com.huzaifa.rescueroute.client.RoadMetric;
import com.huzaifa.rescueroute.client.TableMatrixResult;
import com.huzaifa.rescueroute.dsa.TopKRankingEngine;
import com.huzaifa.rescueroute.dto.ClinicDetailsDto;
import com.huzaifa.rescueroute.dto.FacilityResponseDto;
import com.huzaifa.rescueroute.dto.HospitalDetailsDto;
import com.huzaifa.rescueroute.dto.PharmacyDetailsDto;
import com.huzaifa.rescueroute.dto.RankedFacilityResponseDto;
import com.huzaifa.rescueroute.entity.ExternalFacility;
import com.huzaifa.rescueroute.entity.Facility;
import com.huzaifa.rescueroute.mapper.FacilityMapper;
import com.huzaifa.rescueroute.repository.*;
import com.huzaifa.rescueroute.repository.specification.FacilitySpecifications;
import com.huzaifa.rescueroute.service.RankingService;
import com.huzaifa.rescueroute.util.HaversineUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Two-Stage Hybrid Ranking Engine implementation:
 * <ul>
 *   <li><strong>Candidate Ingestion:</strong> Unifies internally verified facilities with externally
 *       discovered OpenStreetMap facilities. Performs transparent on-demand discovery and DB caching
 *       when querying unpopulated areas.</li>
 *   <li><strong>Stage 1 (DSA Candidate Shortlist):</strong> Fast geographical prefiltering using
 *       geodesic Haversine calculation and a bounded Min-Heap (O(N log M)).</li>
 *   <li><strong>Stage 2 (Road Network Re-ranking):</strong> Evaluates shortlisted candidates via
 *       the OSRM Table API, scoring candidates by road travel time ETA and road distance. Fastest
 *       driving travel time is the #1 ranking factor. Verification status is an informational badge
 *       and does not artificially push closer facilities down.</li>
 *   <li><strong>Resilient Fallback:</strong> If OSRM fails, times out, or returns partial errors,
 *       the system falls back safely to Stage 1 Haversine ranking without fabricated numbers.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class RankingServiceImpl implements RankingService {

    private static final Logger log = LoggerFactory.getLogger(RankingServiceImpl.class);

    public static final double DEFAULT_WEIGHT_DISTANCE = 0.80;
    public static final double DEFAULT_WEIGHT_VERIFICATION = 0.00; // Verification provides no artificial distance boost
    public static final double DEFAULT_WEIGHT_AVAILABILITY = 0.20;
    public static final double DEFAULT_MAX_DISTANCE_KM = 50.0;
    public static final double DEFAULT_MAX_TRAVEL_MINUTES = 60.0;

    private final FacilityRepository facilityRepository;
    private final HospitalRepository hospitalRepository;
    private final BloodBankRepository bloodBankRepository;
    private final PharmacyRepository pharmacyRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final ClinicRepository clinicRepository;
    private final FacilityMapper facilityMapper;
    private final OsrmRoutingClient osrmRoutingClient;
    private final ExternalFacilityRepository externalFacilityRepository;
    private final OverpassClient overpassClient;

    @Value("${app.osrm.max-shortlist-size:10}")
    private int maxShortlistSize = 10;

    @org.springframework.beans.factory.annotation.Autowired
    public RankingServiceImpl(
            FacilityRepository facilityRepository,
            HospitalRepository hospitalRepository,
            BloodBankRepository bloodBankRepository,
            PharmacyRepository pharmacyRepository,
            AmbulanceRepository ambulanceRepository,
            ClinicRepository clinicRepository,
            FacilityMapper facilityMapper,
            @org.springframework.beans.factory.annotation.Autowired(required = false) OsrmRoutingClient osrmRoutingClient,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ExternalFacilityRepository externalFacilityRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) OverpassClient overpassClient) {
        this.facilityRepository = facilityRepository;
        this.hospitalRepository = hospitalRepository;
        this.bloodBankRepository = bloodBankRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.ambulanceRepository = ambulanceRepository;
        this.clinicRepository = clinicRepository;
        this.facilityMapper = facilityMapper;
        this.osrmRoutingClient = osrmRoutingClient;
        this.externalFacilityRepository = externalFacilityRepository;
        this.overpassClient = overpassClient;
    }

    public RankingServiceImpl(
            FacilityRepository facilityRepository,
            HospitalRepository hospitalRepository,
            BloodBankRepository bloodBankRepository,
            PharmacyRepository pharmacyRepository,
            AmbulanceRepository ambulanceRepository,
            ClinicRepository clinicRepository,
            FacilityMapper facilityMapper,
            OsrmRoutingClient osrmRoutingClient) {
        this(facilityRepository, hospitalRepository, bloodBankRepository,
             pharmacyRepository, ambulanceRepository, clinicRepository,
             facilityMapper, osrmRoutingClient, null, null);
    }

    public RankingServiceImpl(
            FacilityRepository facilityRepository,
            HospitalRepository hospitalRepository,
            BloodBankRepository bloodBankRepository,
            PharmacyRepository pharmacyRepository,
            AmbulanceRepository ambulanceRepository,
            ClinicRepository clinicRepository,
            FacilityMapper facilityMapper) {
        this(facilityRepository, hospitalRepository, bloodBankRepository,
             pharmacyRepository, ambulanceRepository, clinicRepository,
             facilityMapper, null, null, null);
    }

    public void setMaxShortlistSize(int maxShortlistSize) {
        this.maxShortlistSize = maxShortlistSize;
    }

    @Override
    public List<RankedFacilityResponseDto> rankFacilities(
            Double userLat,
            Double userLng,
            String facilityType,
            Boolean emergency,
            Integer k) {
        return rankFacilities(userLat, userLng, null, facilityType, emergency, k);
    }

    @Override
    @Transactional
    public List<RankedFacilityResponseDto> rankFacilities(
            Double userLat,
            Double userLng,
            Double radiusKm,
            String facilityType,
            Boolean emergency,
            Integer k) {

        // Validate coordinates prior to execution
        HaversineUtil.validateCoordinate(userLat, userLng, "User");

        boolean isEmergencyMode = Boolean.TRUE.equals(emergency);
        int topK = (k != null && k > 0) ? k : 10;
        double effectiveRadius = (radiusKm != null && radiusKm > 0) ? Math.min(radiusKm, 100.0) : DEFAULT_MAX_DISTANCE_KM;
        String normalizedType = (facilityType != null && !facilityType.isBlank() && !"All".equalsIgnoreCase(facilityType))
                ? facilityType : null;

        // Bounding box for fast spatial filtering
        double latDelta = (effectiveRadius / 111.0) * 1.05;
        double cosLat = Math.max(0.01, Math.cos(Math.toRadians(userLat)));
        double lonDelta = (effectiveRadius / (111.0 * cosLat)) * 1.05;

        double minLat = Math.max(-90.0, userLat - latDelta);
        double maxLat = Math.min(90.0, userLat + latDelta);
        double minLon = Math.max(-180.0, userLng - lonDelta);
        double maxLon = Math.min(180.0, userLng + lonDelta);

        // 1. Fetch internal candidate facilities
        Specification<Facility> spec = FacilitySpecifications.withFilters(normalizedType, null, true);
        List<Facility> internalEntities = facilityRepository.findAll(spec);

        // 2. Fetch external facilities from PostgreSQL
        List<ExternalFacility> externalList = (externalFacilityRepository != null)
                ? externalFacilityRepository.findByLatitudeBetweenAndLongitudeBetween(minLat, maxLat, minLon, maxLon)
                : Collections.emptyList();

        // 3. Count matching internal facilities within radius
        long matchingInternalCount = internalEntities.stream()
                .filter(f -> HaversineUtil.calculateDistance(userLat, userLng, f.getLatitude(), f.getLongitude()) <= effectiveRadius)
                .count();

        // 4. On-demand discovery if the target area is unpopulated in our database
        if ((matchingInternalCount + externalList.size()) < 3 && overpassClient != null && externalFacilityRepository != null) {
            try {
                double searchRadiusMeters = Math.min(effectiveRadius * 1000.0, 10000.0);
                List<OsmElement> elements = overpassClient.queryMedicalFacilities(userLat, userLng, searchRadiusMeters, normalizedType);
                if (elements != null && !elements.isEmpty()) {
                    List<ExternalFacility> toSave = new ArrayList<>();
                    for (OsmElement elem : elements) {
                        if (elem.getLatitude() == null || elem.getLongitude() == null) continue;
                        Optional<ExternalFacility> existing = externalFacilityRepository
                                .findBySourceAndOsmElementTypeAndOsmElementId("OPENSTREETMAP", elem.getElementType(), elem.getElementId());
                        if (existing.isEmpty()) {
                            ExternalFacility newFacility = mapOsmElementToExternalFacility(elem);
                            toSave.add(newFacility);
                        }
                    }
                    if (!toSave.isEmpty()) {
                        externalFacilityRepository.saveAll(toSave);
                    }
                    externalList = externalFacilityRepository.findByLatitudeBetweenAndLongitudeBetween(minLat, maxLat, minLon, maxLon);
                }
            } catch (Exception ex) {
                log.warn("On-demand Overpass discovery failed: {}. Continuing with existing directory records.", ex.getMessage());
            }
        }

        // 5. Build unified candidate pool
        List<ScoredCandidate> scoredCandidates = new ArrayList<>();
        Set<UUID> addedInternalIds = new HashSet<>();
        double maxObservedDistance = effectiveRadius;

        // Add internal facilities within radius
        for (Facility entity : internalEntities) {
            double distance = HaversineUtil.calculateDistance(
                    userLat, userLng, entity.getLatitude(), entity.getLongitude());

            if (distance <= effectiveRadius) {
                enrichSubtypeIfMissing(entity);
                boolean isAvail = checkEmergencyAvailability(entity);
                FacilityResponseDto dto = facilityMapper.toDto(entity);
                scoredCandidates.add(new ScoredCandidate(dto, distance, isAvail, entity.isVerified()));
                if (entity.getId() != null) {
                    addedInternalIds.add(entity.getId());
                }
                if (distance > maxObservedDistance) {
                    maxObservedDistance = distance;
                }
            }
        }

        // Add external facilities within radius (excluding suppressed or already matched)
        for (ExternalFacility ef : externalList) {
            if ("SUPPRESSED".equalsIgnoreCase(ef.getImportStatus())) {
                continue;
            }
            if (normalizedType != null && !normalizedType.equalsIgnoreCase(ef.getFacilityType())) {
                continue;
            }
            if (ef.getMatchedFacilityId() != null && addedInternalIds.contains(ef.getMatchedFacilityId())) {
                continue;
            }

            double distance = HaversineUtil.calculateDistance(
                    userLat, userLng, ef.getLatitude(), ef.getLongitude());

            if (distance <= effectiveRadius) {
                FacilityResponseDto dto = mapExternalFacilityToDto(ef);
                boolean isAvail = checkExternalEmergencyAvailability(ef);
                scoredCandidates.add(new ScoredCandidate(dto, distance, isAvail, false));
                if (distance > maxObservedDistance) {
                    maxObservedDistance = distance;
                }
            }
        }

        // Fallback: If strictly within radius no candidates found, expand up to DEFAULT_MAX_DISTANCE_KM for internal facilities
        if (scoredCandidates.isEmpty()) {
            for (Facility entity : internalEntities) {
                double distance = HaversineUtil.calculateDistance(
                        userLat, userLng, entity.getLatitude(), entity.getLongitude());
                if (distance <= DEFAULT_MAX_DISTANCE_KM) {
                    enrichSubtypeIfMissing(entity);
                    boolean isAvail = checkEmergencyAvailability(entity);
                    FacilityResponseDto dto = facilityMapper.toDto(entity);
                    scoredCandidates.add(new ScoredCandidate(dto, distance, isAvail, entity.isVerified()));
                    if (distance > maxObservedDistance) {
                        maxObservedDistance = distance;
                    }
                }
            }
        }

        if (scoredCandidates.isEmpty()) {
            return List.of();
        }

        // Compute normalized multi-criteria Stage 1 scores
        List<RankedFacilityResponseDto> stage1Dtos = new ArrayList<>(scoredCandidates.size());
        for (ScoredCandidate sc : scoredCandidates) {
            FacilityResponseDto facilityDto = sc.dto;
            double distance = sc.distance;
            boolean emergencyAvailable = sc.emergencyAvailable;
            boolean isVerified = sc.isVerified;

            double score = calculateScore(
                    distance, isVerified, emergencyAvailable, isEmergencyMode, maxObservedDistance);

            double roundedDistance = Math.round(distance * 100.0) / 100.0;
            double roundedScore = Math.round(score * 10000.0) / 10000.0;

            stage1Dtos.add(RankedFacilityResponseDto.builder()
                    .facility(facilityDto)
                    .distanceKm(roundedDistance)
                    .straightLineDistanceKm(roundedDistance)
                    .drivingDistanceKm(null)
                    .estimatedTravelMinutes(null)
                    .distanceMetricUsed("STRAIGHT_LINE")
                    .routingAvailable(false)
                    .score(roundedScore)
                    .isVerified(isVerified)
                    .emergencyAvailable(emergencyAvailable)
                    .build());
        }

        // =========================================================================
        // STAGE 1 — Candidate Shortlist via DSA Min-Heap: O(N log M)
        // Select M candidates where M = min(eligible, max(topK, min(2 * topK, maxShortlistSize)))
        // =========================================================================
        int boundedShortlistCap = Math.max(topK, maxShortlistSize);
        int shortlistM = Math.min(stage1Dtos.size(), Math.max(topK, Math.min(2 * topK, boundedShortlistCap)));
        List<RankedFacilityResponseDto> shortlist = TopKRankingEngine.getTopK(stage1Dtos, shortlistM);

        if (shortlist.isEmpty()) {
            return List.of();
        }

        // =========================================================================
        // STAGE 2 — Road-Network Evaluation via OSRM Table API
        // =========================================================================
        if (osrmRoutingClient != null) {
            TableMatrixResult matrixResult = osrmRoutingClient.calculateDistanceMatrixForRanked(
                    userLat, userLng, shortlist);

            if (matrixResult != null && matrixResult.isSuccessful() && matrixResult.getMetrics() != null && !matrixResult.getMetrics().isEmpty()) {
                List<RoadMetric> metrics = matrixResult.getMetrics();

                // Find max observed duration among reachable candidates to normalize travel time
                int maxObservedDuration = 0;
                boolean anyReachable = false;
                for (RoadMetric m : metrics) {
                    if (m != null && m.isRouteAvailable() && m.getDurationMinutes() != null) {
                        anyReachable = true;
                        if (m.getDurationMinutes() > maxObservedDuration) {
                            maxObservedDuration = m.getDurationMinutes();
                        }
                    }
                }

                if (anyReachable) {
                    double effectiveMaxDuration = Math.max(DEFAULT_MAX_TRAVEL_MINUTES, (double) maxObservedDuration);

                    for (int i = 0; i < shortlist.size(); i++) {
                        RankedFacilityResponseDto dto = shortlist.get(i);
                        RoadMetric metric = (i < metrics.size()) ? metrics.get(i) : null;

                        if (metric != null && metric.isRouteAvailable() && metric.getDurationMinutes() != null) {
                            double timeFactor = Math.max(0.0, 1.0 - (metric.getDurationMinutes() / effectiveMaxDuration));
                            double roadScore = calculateScoreWithFactor(
                                     timeFactor, dto.isVerified(), dto.isEmergencyAvailable(), isEmergencyMode);

                            dto.setScore(Math.round(roadScore * 10000.0) / 10000.0);
                            dto.setDrivingDistanceKm(metric.getDistanceKm());
                            dto.setEstimatedTravelMinutes(metric.getDurationMinutes());
                            dto.setDistanceMetricUsed("ROAD_NETWORK");
                            dto.setRoutingAvailable(true);
                        } else {
                            // Partial matrix / unreachable destination policy:
                            // Impose a 50% penalty so reachable candidates decisively rank ahead
                            double penalizedScore = dto.getScore() * 0.50;
                            dto.setScore(Math.round(penalizedScore * 10000.0) / 10000.0);
                            dto.setDrivingDistanceKm(null);
                            dto.setEstimatedTravelMinutes(null);
                            dto.setDistanceMetricUsed("STRAIGHT_LINE");
                            dto.setRoutingAvailable(false);
                        }
                    }

                    // Re-rank shortlist by Road metrics (fastest ETA #1) and truncate to topK
                    return TopKRankingEngine.rankRoadCandidates(shortlist, topK);
                }
            }

            String errorDetail = (matrixResult != null && matrixResult.getErrorMessage() != null)
                    ? matrixResult.getErrorMessage() : "no response from routing client";
            log.warn("OSRM road ranking unavailable or failed ({}). Falling back to straight-line Haversine ranking.",
                    errorDetail);
        }

        // =========================================================================
        // RESILIENT FALLBACK: Pure Haversine Stage 1 Ranking
        // =========================================================================
        for (RankedFacilityResponseDto dto : shortlist) {
            dto.setDistanceMetricUsed("STRAIGHT_LINE");
            dto.setRoutingAvailable(false);
            dto.setDrivingDistanceKm(null);
            dto.setEstimatedTravelMinutes(null);
        }

        int finalK = Math.min(topK, shortlist.size());
        List<RankedFacilityResponseDto> fallbackResult = new ArrayList<>(shortlist.subList(0, finalK));
        for (int i = 0; i < fallbackResult.size(); i++) {
            fallbackResult.get(i).setRank(i + 1);
        }

        return fallbackResult;
    }

    /**
     * Calculates the multi-criteria ranking suitability score using a normalized proximity factor.
     */
    public double calculateScoreWithFactor(
            double proximityFactor,
            boolean isVerified,
            boolean emergencyAvailable,
            boolean emergencyMode) {

        double wDist = emergencyMode ? 0.70 : DEFAULT_WEIGHT_DISTANCE;
        double wAvail = emergencyMode ? 0.30 : DEFAULT_WEIGHT_AVAILABILITY;
        double wVerif = emergencyMode ? 0.0 : DEFAULT_WEIGHT_VERIFICATION;

        double verifFactor = isVerified ? 1.0 : 0.0;
        double availFactor = emergencyAvailable ? 1.0 : 0.0;

        double totalScore = (wDist * proximityFactor) + (wVerif * verifFactor) + (wAvail * availFactor);
        return Math.max(0.0, Math.min(1.0, totalScore));
    }

    /**
     * Calculates the multi-criteria ranking suitability score using straight-line distance.
     */
    public double calculateScore(
            double distance,
            boolean isVerified,
            boolean emergencyAvailable,
            boolean emergencyMode,
            double maxDistance) {

        double effectiveMax = Math.max(1.0, maxDistance);
        double distFactor = Math.max(0.0, 1.0 - (distance / effectiveMax));
        return calculateScoreWithFactor(distFactor, isVerified, emergencyAvailable, emergencyMode);
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
        } else if ("Clinic".equalsIgnoreCase(facility.getFacilityType()) && facility.getClinic() != null) {
            return facility.getClinic().isConsultationAvailable() && facility.getClinic().isOpenWeekends();
        }

        return false;
    }

    public boolean checkExternalEmergencyAvailability(ExternalFacility ef) {
        if (ef == null) return false;
        String type = ef.getFacilityType();
        String tags = ef.getOsmTags() != null ? ef.getOsmTags().toLowerCase() : "";
        String hours = ef.getOpeningHours() != null ? ef.getOpeningHours().toLowerCase() : "";

        if ("Hospital".equalsIgnoreCase(type)) {
            return tags.contains("emergency=yes") || hours.contains("24/7");
        } else if ("Pharmacy".equalsIgnoreCase(type)) {
            return hours.contains("24/7") || hours.contains("24 hours");
        } else if ("Ambulance".equalsIgnoreCase(type)) {
            return true;
        } else if ("Blood Bank".equalsIgnoreCase(type)) {
            return true;
        } else if ("Clinic".equalsIgnoreCase(type)) {
            return hours.contains("weekend") || hours.contains("24/7");
        }
        return false;
    }

    private FacilityResponseDto mapExternalFacilityToDto(ExternalFacility ef) {
        boolean isHospital = "Hospital".equalsIgnoreCase(ef.getFacilityType());
        boolean isPharmacy = "Pharmacy".equalsIgnoreCase(ef.getFacilityType());
        boolean isClinic = "Clinic".equalsIgnoreCase(ef.getFacilityType());
        String tags = ef.getOsmTags() != null ? ef.getOsmTags().toLowerCase() : "";
        String hours = ef.getOpeningHours() != null ? ef.getOpeningHours().toLowerCase() : "";

        return FacilityResponseDto.builder()
                .id(ef.getId())
                .name(ef.getName())
                .facilityType(ef.getFacilityType())
                .address(ef.getAddress() != null && !ef.getAddress().isBlank() ? ef.getAddress() : "OpenStreetMap Location")
                .city(ef.getCity() != null ? ef.getCity() : "Karachi")
                .latitude(ef.getLatitude())
                .longitude(ef.getLongitude())
                .phoneNumber(ef.getPhoneNumber())
                .isVerified(false)
                .isActive(true)
                .hasPendingUpdate(false)
                .createdAt(ef.getCreatedAt())
                .updatedAt(ef.getUpdatedAt())
                .hospitalDetails(isHospital ? HospitalDetailsDto.builder()
                        .emergencyAvailable(tags.contains("emergency=yes") || hours.contains("24/7"))
                        .icuAvailable(tags.contains("icu=yes"))
                        .traumaCenterAvailable(tags.contains("trauma=yes"))
                        .build() : null)
                .pharmacyDetails(isPharmacy ? PharmacyDetailsDto.builder()
                        .open24Hours(hours.contains("24/7") || hours.contains("24 hours"))
                        .emergencyMedicinesAvailable(hours.contains("24/7"))
                        .build() : null)
                .clinicDetails(isClinic ? ClinicDetailsDto.builder()
                        .consultationAvailable(true)
                        .openWeekends(hours.contains("sat") || hours.contains("sun") || hours.contains("24/7"))
                        .build() : null)
                .build();
    }

    private ExternalFacility mapOsmElementToExternalFacility(OsmElement elem) {
        return ExternalFacility.builder()
                .source("OPENSTREETMAP")
                .osmElementType(elem.getElementType())
                .osmElementId(elem.getElementId())
                .sourceUrl("https://www.openstreetmap.org/" + elem.getElementType() + "/" + elem.getElementId())
                .name(elem.getName() != null && !elem.getName().isBlank() ? elem.getName() : "Unnamed " + elem.getFacilityType())
                .facilityType(elem.getFacilityType())
                .osmTags(elem.getRawTagsJson())
                .latitude(elem.getLatitude())
                .longitude(elem.getLongitude())
                .address(elem.getAddress())
                .city(elem.getCity() != null ? elem.getCity() : "Karachi")
                .phoneNumber(elem.getPhoneNumber())
                .website(elem.getWebsite())
                .openingHours(elem.getOpeningHours())
                .importStatus("DISCOVERED")
                .importedAt(Instant.now())
                .lastSeenAt(Instant.now())
                .build();
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
        } else if (facility.getClinic() == null && "Clinic".equalsIgnoreCase(facility.getFacilityType())) {
            clinicRepository.findByFacilityId(facility.getId()).ifPresent(facility::setClinic);
        }
    }

    private static class ScoredCandidate {
        final FacilityResponseDto dto;
        final double distance;
        final boolean emergencyAvailable;
        final boolean isVerified;

        ScoredCandidate(FacilityResponseDto dto, double distance, boolean emergencyAvailable, boolean isVerified) {
            this.dto = dto;
            this.distance = distance;
            this.emergencyAvailable = emergencyAvailable;
            this.isVerified = isVerified;
        }
    }
}
