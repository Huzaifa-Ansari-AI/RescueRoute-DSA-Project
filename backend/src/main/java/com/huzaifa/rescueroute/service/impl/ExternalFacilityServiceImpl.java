package com.huzaifa.rescueroute.service.impl;

import com.huzaifa.rescueroute.client.OsmElement;
import com.huzaifa.rescueroute.client.OverpassClient;
import com.huzaifa.rescueroute.common.exception.ResourceNotFoundException;
import com.huzaifa.rescueroute.dto.ExternalFacilityResponseDto;
import com.huzaifa.rescueroute.dto.ExternalImportRequestDto;
import com.huzaifa.rescueroute.dto.ExternalImportSummaryDto;
import com.huzaifa.rescueroute.entity.ExternalFacility;
import com.huzaifa.rescueroute.entity.Facility;
import com.huzaifa.rescueroute.repository.ExternalFacilityRepository;
import com.huzaifa.rescueroute.repository.FacilityRepository;
import com.huzaifa.rescueroute.service.ExternalDuplicateDetector;
import com.huzaifa.rescueroute.service.ExternalFacilityService;
import com.huzaifa.rescueroute.util.HaversineUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ExternalFacilityService handling Overpass querying,
 * data normalization, multi-level duplicate candidate detection, and idempotent
 * persistence.
 */
@Service
public class ExternalFacilityServiceImpl implements ExternalFacilityService {

    private static final Logger log = LoggerFactory.getLogger(ExternalFacilityServiceImpl.class);

    private final OverpassClient overpassClient;
    private final ExternalFacilityRepository externalFacilityRepository;
    private final FacilityRepository facilityRepository;
    private final ExternalDuplicateDetector duplicateDetector;

    public ExternalFacilityServiceImpl(
            OverpassClient overpassClient,
            ExternalFacilityRepository externalFacilityRepository,
            FacilityRepository facilityRepository,
            ExternalDuplicateDetector duplicateDetector) {
        this.overpassClient = overpassClient;
        this.externalFacilityRepository = externalFacilityRepository;
        this.facilityRepository = facilityRepository;
        this.duplicateDetector = duplicateDetector;
    }

    @Override
    @Transactional
    public ExternalImportSummaryDto importFacilities(ExternalImportRequestDto request) {
        long startTime = System.currentTimeMillis();

        if (request == null) {
            throw new IllegalArgumentException("Import request body cannot be null.");
        }

        HaversineUtil.validateCoordinate(request.getLatitude(), request.getLongitude(), "Import center");
        if (request.getRadiusMeters() == null || request.getRadiusMeters() < 100.0
                || request.getRadiusMeters() > 10000.0) {
            throw new IllegalArgumentException("Radius must be between 100 and 10,000 meters.");
        }

        log.info("[ExternalFacilityService] Starting import at ({}, {}) with radius {}m",
                request.getLatitude(), request.getLongitude(), request.getRadiusMeters());

        List<OsmElement> elements = overpassClient.queryMedicalFacilities(
                request.getLatitude(),
                request.getLongitude(),
                request.getRadiusMeters(),
                request.getFacilityType());

        if (elements == null || elements.isEmpty()) {
            return ExternalImportSummaryDto.builder()
                    .totalQueried(0)
                    .imported(0)
                    .updated(0)
                    .duplicateCandidates(0)
                    .skipped(0)
                    .failed(0)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .message("No matching medical facilities returned from OpenStreetMap for this area.")
                    .build();
        }

        // Fetch internal active facilities to evaluate potential duplicate candidates
        List<Facility> internalFacilities = facilityRepository.findAll();

        int importedCount = 0;
        int updatedCount = 0;
        int duplicateCount = 0;
        int failedCount = 0;

        for (OsmElement elem : elements) {
            try {
                // Check Level 1: Exact source identity
                Optional<ExternalFacility> existingOpt = externalFacilityRepository
                        .findBySourceAndOsmElementTypeAndOsmElementId("OPENSTREETMAP", elem.getElementType(),
                                elem.getElementId());

                // Check Level 2: Potential match against internal facilities
                Optional<Facility> potentialDuplicate = duplicateDetector.findPotentialDuplicate(
                        elem.getLatitude(),
                        elem.getLongitude(),
                        elem.getName(),
                        elem.getFacilityType(),
                        internalFacilities);

                boolean isDuplicateCandidate = potentialDuplicate.isPresent();
                UUID matchedFacilityId = potentialDuplicate.map(f -> f.getId()).orElse(null);

                if (existingOpt.isPresent()) {
                    // Idempotent update of existing external POI
                    ExternalFacility existing = existingOpt.get();
                    existing.setName(elem.getName());
                    existing.setFacilityType(elem.getFacilityType());
                    existing.setLatitude(elem.getLatitude());
                    existing.setLongitude(elem.getLongitude());
                    existing.setAddress(elem.getAddress());
                    existing.setCity(elem.getCity());
                    existing.setPhoneNumber(elem.getPhoneNumber());
                    existing.setWebsite(elem.getWebsite());
                    existing.setOpeningHours(elem.getOpeningHours());
                    existing.setSourceUrl(elem.getSourceUrl());
                    existing.setOsmTags(elem.getRawTagsJson());
                    existing.setLastSeenAt(Instant.now());

                    // Preserve status if already reviewed or suppressed by admin; otherwise update
                    // candidate status
                    if (!"REVIEWED".equalsIgnoreCase(existing.getImportStatus())
                            && !"SUPPRESSED".equalsIgnoreCase(existing.getImportStatus())) {
                        existing.setImportStatus(isDuplicateCandidate ? "DUPLICATE_CANDIDATE" : "DISCOVERED");
                        existing.setMatchedFacilityId(matchedFacilityId);
                    }

                    externalFacilityRepository.save(existing);
                    updatedCount++;
                    if (isDuplicateCandidate)
                        duplicateCount++;

                } else {
                    // New external POI insertion
                    ExternalFacility newFacility = ExternalFacility.builder()
                            .source("OPENSTREETMAP")
                            .osmElementType(elem.getElementType())
                            .osmElementId(elem.getElementId())
                            .sourceUrl(elem.getSourceUrl())
                            .name(elem.getName())
                            .facilityType(elem.getFacilityType())
                            .osmTags(elem.getRawTagsJson())
                            .latitude(elem.getLatitude())
                            .longitude(elem.getLongitude())
                            .address(elem.getAddress())
                            .city(elem.getCity())
                            .phoneNumber(elem.getPhoneNumber())
                            .website(elem.getWebsite())
                            .openingHours(elem.getOpeningHours())
                            .importStatus(isDuplicateCandidate ? "DUPLICATE_CANDIDATE" : "DISCOVERED")
                            .matchedFacilityId(matchedFacilityId)
                            .importedAt(Instant.now())
                            .lastSeenAt(Instant.now())
                            .build();

                    externalFacilityRepository.save(newFacility);
                    importedCount++;
                    if (isDuplicateCandidate)
                        duplicateCount++;
                }
            } catch (Exception e) {
                log.error("[ExternalFacilityService] Error persisting OSM element {}/{}: {}",
                        elem.getElementType(), elem.getElementId(), e.getMessage());
                failedCount++;
            }
        }

        long executionTime = System.currentTimeMillis() - startTime;
        log.info(
                "[ExternalFacilityService] Import complete: {} queried, {} new, {} updated, {} duplicate candidates in {}ms",
                elements.size(), importedCount, updatedCount, duplicateCount, executionTime);

        return ExternalImportSummaryDto.builder()
                .totalQueried(elements.size())
                .imported(importedCount)
                .updated(updatedCount)
                .duplicateCandidates(duplicateCount)
                .skipped(0)
                .failed(failedCount)
                .executionTimeMs(executionTime)
                .message(String.format(
                        "Successfully processed %d facilities (%d imported, %d updated, %d duplicate candidates flagged).",
                        elements.size(), importedCount, updatedCount, duplicateCount))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalFacilityResponseDto> searchExternalFacilities(
            Double lat,
            Double lng,
            Double radiusMeters,
            String facilityType,
            String status) {

        HaversineUtil.validateCoordinate(lat, lng, "Search center");
        double effectiveRadiusMeters = (radiusMeters != null && radiusMeters > 0)
                ? Math.min(radiusMeters, 10000.0)
                : 5000.0;
        double radiusKm = effectiveRadiusMeters / 1000.0;

        // Calculate approximate bounding box degrees with safety margins
        double latDelta = (radiusKm / 111.0) * 1.05;
        double cosLat = Math.max(0.01, Math.cos(Math.toRadians(lat)));
        double lonDelta = (radiusKm / (111.0 * cosLat)) * 1.05;

        double minLat = Math.max(-90.0, lat - latDelta);
        double maxLat = Math.min(90.0, lat + latDelta);
        double minLon = Math.max(-180.0, lng - lonDelta);
        double maxLon = Math.min(180.0, lng + lonDelta);

        List<ExternalFacility> candidates = externalFacilityRepository.findByLatitudeBetweenAndLongitudeBetween(
                minLat, maxLat, minLon, maxLon);

        // Pre-fetch internal facility names to populate matchedFacilityName efficiently
        Map<UUID, String> internalFacilityNameMap = new HashMap<>();
        for (Facility f : facilityRepository.findAll()) {
            if (f != null && f.getId() != null && f.getName() != null) {
                internalFacilityNameMap.putIfAbsent(f.getId(), f.getName());
            }
        }

        List<ExternalFacilityResponseDto> results = new ArrayList<>();

        for (ExternalFacility facility : candidates) {
            // Filter by facilityType if specified
            if (facilityType != null && !facilityType.isBlank() && !facilityType.equalsIgnoreCase("ALL")) {
                if (!facility.getFacilityType().equalsIgnoreCase(facilityType)) {
                    continue;
                }
            }

            // Filter by status if specified; exclude SUPPRESSED by default unless
            // explicitly asked
            if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
                if (!facility.getImportStatus().equalsIgnoreCase(status)) {
                    continue;
                }
            } else {
                if ("SUPPRESSED".equalsIgnoreCase(facility.getImportStatus())) {
                    continue;
                }
            }

            double distanceKm = HaversineUtil.calculateDistance(lat, lng, facility.getLatitude(),
                    facility.getLongitude());
            if (distanceKm <= radiusKm) {
                ExternalFacilityResponseDto dto = mapToDto(facility, distanceKm, internalFacilityNameMap);
                results.add(dto);
            }
        }

        // Sort by distance ascending
        results.sort((d1, d2) -> Double.compare(
                (d1 != null && d1.getDistanceKm() != null) ? d1.getDistanceKm() : Double.MAX_VALUE,
                (d2 != null && d2.getDistanceKm() != null) ? d2.getDistanceKm() : Double.MAX_VALUE));
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public ExternalFacilityResponseDto getExternalFacilityById(UUID id) {
        ExternalFacility facility = externalFacilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("External facility not found with id: " + id));

        Map<UUID, String> internalFacilityNameMap = facility.getMatchedFacilityId() != null
                ? facilityRepository.findById(facility.getMatchedFacilityId())
                        .map(f -> Map.of(f.getId(), f.getName()))
                        .orElse(Collections.emptyMap())
                : Collections.emptyMap();

        return mapToDto(facility, null, internalFacilityNameMap);
    }

    @Override
    @Transactional
    public ExternalFacilityResponseDto updateFacilityStatus(UUID id, String status) {
        ExternalFacility facility = externalFacilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("External facility not found with id: " + id));

        facility.setImportStatus(status.toUpperCase(Locale.ROOT));
        ExternalFacility updated = externalFacilityRepository.save(facility);

        return mapToDto(updated, null, Collections.emptyMap());
    }

    private ExternalFacilityResponseDto mapToDto(
            ExternalFacility entity,
            Double distanceKm,
            Map<UUID, String> internalFacilityNameMap) {

        String matchedName = null;
        if (entity.getMatchedFacilityId() != null) {
            matchedName = internalFacilityNameMap.get(entity.getMatchedFacilityId());
        }

        return ExternalFacilityResponseDto.builder()
                .id(entity.getId())
                .source(entity.getSource())
                .osmElementType(entity.getOsmElementType())
                .osmElementId(entity.getOsmElementId())
                .sourceUrl(entity.getSourceUrl())
                .name(entity.getName())
                .facilityType(entity.getFacilityType())
                .osmTags(entity.getOsmTags())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .address(entity.getAddress())
                .city(entity.getCity())
                .phoneNumber(entity.getPhoneNumber())
                .website(entity.getWebsite())
                .openingHours(entity.getOpeningHours())
                .importStatus(entity.getImportStatus())
                .matchedFacilityId(entity.getMatchedFacilityId())
                .matchedFacilityName(matchedName)
                .distanceKm(distanceKm != null ? Math.round(distanceKm * 100.0) / 100.0 : null)
                .attribution("© OpenStreetMap contributors")
                .disclaimer("External listing · OpenStreetMap · Not verified by RescueRoute")
                .isVerified(false) // Strict enforcement: external POIs are NEVER verified
                .importedAt(entity.getImportedAt())
                .lastSeenAt(entity.getLastSeenAt())
                .build();
    }
}
