package com.huzaifa.rescueroute.service;

import com.huzaifa.rescueroute.entity.Facility;
import com.huzaifa.rescueroute.util.HaversineUtil;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Evaluates candidate duplication between incoming external OSM records and internal RescueRoute facilities.
 * Utilizes multi-factor spatial proximity, category alignment, and normalized token matching.
 */
@Component
public class ExternalDuplicateDetector {

    public static final double PROXIMITY_THRESHOLD_KM = 0.15; // 150 meters

    private static final Set<String> STOP_WORDS = Set.of(
            "hospital", "clinic", "pharmacy", "medical", "centre", "center",
            "care", "healthcare", "dr", "doctor", "the", "and", "of"
    );

    /**
     * Identifies if an external POI is a probable duplicate of any internal facility.
     *
     * @param externalLat External facility latitude
     * @param externalLon External facility longitude
     * @param externalName External facility name
     * @param externalType External facility category
     * @param internalCandidates Candidate internal facilities
     * @return The matched internal facility if a high-confidence match is detected, otherwise empty
     */
    public Optional<Facility> findPotentialDuplicate(
            Double externalLat,
            Double externalLon,
            String externalName,
            String externalType,
            List<Facility> internalCandidates) {

        if (externalLat == null || externalLon == null || externalName == null || internalCandidates == null || internalCandidates.isEmpty()) {
            return Optional.empty();
        }

        Facility bestMatch = null;
        double minDistance = Double.MAX_VALUE;

        for (Facility candidate : internalCandidates) {
            if (candidate.getLatitude() == null || candidate.getLongitude() == null) {
                continue;
            }

            // 1. Proximity check via Haversine distance
            double distanceKm = HaversineUtil.calculateDistance(
                    externalLat, externalLon,
                    candidate.getLatitude(), candidate.getLongitude()
            );

            if (distanceKm > PROXIMITY_THRESHOLD_KM) {
                continue;
            }

            // 2. Category compatibility (must belong to the same medical category)
            if (externalType != null && candidate.getFacilityType() != null) {
                if (!externalType.equalsIgnoreCase(candidate.getFacilityType())) {
                    continue;
                }
            }

            // 3. Name similarity comparison
            boolean nameMatches = isNameSimilar(externalName, candidate.getName());
            if (nameMatches) {
                if (distanceKm < minDistance) {
                    minDistance = distanceKm;
                    bestMatch = candidate;
                }
            }
        }

        return Optional.ofNullable(bestMatch);
    }

    /**
     * Determines whether two facility names are significantly similar after normalization.
     */
    public boolean isNameSimilar(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return false;
        }

        String norm1 = normalizeString(name1);
        String norm2 = normalizeString(name2);

        if (norm1.isEmpty() || norm2.isEmpty()) {
            return false;
        }

        // Direct normalized string equality
        if (norm1.equals(norm2)) {
            return true;
        }

        // Substring match if sufficiently descriptive
        if (norm1.length() >= 5 && norm2.length() >= 5) {
            if (norm1.contains(norm2) || norm2.contains(norm1)) {
                return true;
            }
        }

        // Tokenized set overlap excluding generic medical stop words
        Set<String> tokens1 = extractKeywords(norm1);
        Set<String> tokens2 = extractKeywords(norm2);

        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return false;
        }

        // Check token intersection
        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);

        if (intersection.isEmpty()) {
            return false;
        }

        Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);

        double jaccard = (double) intersection.size() / union.size();
        return jaccard >= 0.4 || intersection.size() >= 2;
    }

    public String normalizeString(String input) {
        if (input == null) return "";
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Set<String> extractKeywords(String normalized) {
        String[] parts = normalized.split("\\s+");
        Set<String> keywords = new HashSet<>();
        for (String part : parts) {
            if (part.length() > 2 && !STOP_WORDS.contains(part)) {
                keywords.add(part);
            }
        }
        return keywords;
    }
}
