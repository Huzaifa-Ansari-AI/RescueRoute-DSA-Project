package com.huzaifa.rescueroute.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huzaifa.rescueroute.entity.Facility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Resilient client for the Open Source Routing Machine (OSRM) Table API.
 *
 * <p>Retrieves batch driving distance and duration matrices for candidate facilities
 * from a user origin point in a single network round-trip.
 */
@Component
public class OsrmRoutingClient {

    private static final Logger log = LoggerFactory.getLogger(OsrmRoutingClient.class);

    private final String baseUrl;
    private final String tablePath;
    private final int timeoutMs;
    private final int maxShortlistSize;
    private final long cacheTtlMs;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Lightweight thread-safe in-memory cache for OSRM table responses
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_ENTRIES = 100;

    public OsrmRoutingClient() {
        this("https://router.project-osrm.org", "/table/v1/driving", 2000, 10, 60000L, null, new ObjectMapper());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public OsrmRoutingClient(
            @Value("${app.osrm.base-url:https://router.project-osrm.org}") String baseUrl,
            @Value("${app.osrm.table-path:/table/v1/driving}") String tablePath,
            @Value("${app.osrm.timeout-ms:2000}") int timeoutMs,
            @Value("${app.osrm.max-shortlist-size:10}") int maxShortlistSize,
            @Value("${app.osrm.cache-ttl-ms:60000}") long cacheTtlMs) {
        this(baseUrl, tablePath, timeoutMs, maxShortlistSize, cacheTtlMs, null, new ObjectMapper());
    }

    // Constructor for testing with custom HttpClient and ObjectMapper
    public OsrmRoutingClient(
            String baseUrl,
            String tablePath,
            int timeoutMs,
            int maxShortlistSize,
            long cacheTtlMs,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "https://router.project-osrm.org";
        this.tablePath = (tablePath != null && tablePath.startsWith("/")) ? tablePath : "/" + (tablePath != null ? tablePath : "table/v1/driving");
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : 2000;
        this.maxShortlistSize = maxShortlistSize > 0 ? maxShortlistSize : 10;
        this.cacheTtlMs = cacheTtlMs > 0 ? cacheTtlMs : 60000L;
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(this.timeoutMs))
                .build();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public record CoordinateTarget(String id, Double latitude, Double longitude) {}

    /**
     * Queries OSRM Table API for batch driving metrics from user coordinates to candidates.
     *
     * @param userLat    User latitude in decimal degrees
     * @param userLng    User longitude in decimal degrees
     * @param candidates Ordered list of shortlisted facilities
     * @return TableMatrixResult containing RoadMetric for each candidate
     */
    public TableMatrixResult calculateDistanceMatrix(
            Double userLat,
            Double userLng,
            List<Facility> candidates) {

        if (candidates == null || candidates.isEmpty()) {
            return TableMatrixResult.failure("Invalid parameters: candidates are null or empty");
        }

        List<CoordinateTarget> targets = candidates.stream()
                .filter(f -> f != null && f.getLatitude() != null && f.getLongitude() != null)
                .map(f -> new CoordinateTarget(
                        f.getId() != null ? f.getId().toString() : "id",
                        f.getLatitude(),
                        f.getLongitude()))
                .toList();

        return calculateDistanceMatrixForTargets(userLat, userLng, targets);
    }

    /**
     * Queries OSRM Table API for batch driving metrics for ranked facility DTOs (supports both internal and external facilities).
     */
    public TableMatrixResult calculateDistanceMatrixForRanked(
            Double userLat,
            Double userLng,
            List<com.huzaifa.rescueroute.dto.RankedFacilityResponseDto> candidates) {

        if (candidates == null || candidates.isEmpty()) {
            return TableMatrixResult.failure("Invalid parameters: candidates are null or empty");
        }

        List<CoordinateTarget> targets = candidates.stream()
                .filter(r -> r != null && r.getFacility() != null
                        && r.getFacility().getLatitude() != null && r.getFacility().getLongitude() != null)
                .map(r -> new CoordinateTarget(
                        r.getFacility().getId() != null ? r.getFacility().getId().toString() : "id",
                        r.getFacility().getLatitude(),
                        r.getFacility().getLongitude()))
                .toList();

        return calculateDistanceMatrixForTargets(userLat, userLng, targets);
    }

    /**
     * Internal implementation executing batch OSRM Table distance matrix calculation.
     */
    public TableMatrixResult calculateDistanceMatrixForTargets(
            Double userLat,
            Double userLng,
            List<CoordinateTarget> targets) {

        if (userLat == null || userLng == null || targets == null || targets.isEmpty()) {
            return TableMatrixResult.failure("Invalid parameters: coordinates or targets are null/empty");
        }

        // Bound candidate count to avoid URL length overflow
        List<CoordinateTarget> boundedTargets = targets.size() > maxShortlistSize
                ? targets.subList(0, maxShortlistSize)
                : targets;

        // Check in-memory cache
        String cacheKey = generateTargetsCacheKey(userLat, userLng, boundedTargets);
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired(cacheTtlMs)) {
            log.debug("OSRM matrix cache hit for key {}", cacheKey);
            return TableMatrixResult.success(cached.metrics);
        }

        // Construct coordinates string: index 0 is user, index 1..M are candidates
        // Format: {lng},{lat};{lng},{lat}...
        StringBuilder coordsBuilder = new StringBuilder();
        coordsBuilder.append(String.format(Locale.US, "%.6f,%.6f", userLng, userLat));

        for (CoordinateTarget target : boundedTargets) {
            coordsBuilder.append(";");
            coordsBuilder.append(String.format(Locale.US, "%.6f,%.6f", target.longitude(), target.latitude()));
        }

        String requestUrl = String.format("%s%s/%s?sources=0&annotations=distance,duration",
                baseUrl, tablePath, coordsBuilder.toString());

        log.debug("Requesting OSRM Table API: {}", requestUrl);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("OSRM Table API returned HTTP status {}: {}", response.statusCode(), response.body());
                return TableMatrixResult.failure("OSRM returned HTTP " + response.statusCode());
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            String code = rootNode.path("code").asText();

            if (!"Ok".equalsIgnoreCase(code)) {
                log.warn("OSRM Table API returned non-Ok code: {}", code);
                return TableMatrixResult.failure("OSRM returned status code " + code);
            }

            JsonNode durationsNode = rootNode.path("durations");
            JsonNode distancesNode = rootNode.path("distances");

            if (!durationsNode.isArray() || durationsNode.isEmpty() ||
                !distancesNode.isArray() || distancesNode.isEmpty()) {
                log.warn("OSRM Table API returned malformed matrix payload");
                return TableMatrixResult.failure("OSRM returned empty or malformed matrix");
            }

            JsonNode userDurations = durationsNode.get(0);
            JsonNode userDistances = distancesNode.get(0);

            List<RoadMetric> metrics = new ArrayList<>(boundedTargets.size());

            // User is index 0 in the response columns; candidate i is at column index (i + 1)
            for (int i = 0; i < boundedTargets.size(); i++) {
                int colIdx = i + 1;
                JsonNode durNode = userDurations.path(colIdx);
                JsonNode distNode = userDistances.path(colIdx);

                if (durNode.isNumber() && distNode.isNumber()) {
                    double durSeconds = durNode.asDouble();
                    double distMeters = distNode.asDouble();

                    if (durSeconds >= 0 && distMeters >= 0) {
                        double distKm = Math.round((distMeters / 1000.0) * 10.0) / 10.0;
                        int durationMinutes = Math.max(1, (int) Math.round(durSeconds / 60.0));

                        metrics.add(RoadMetric.builder()
                                .distanceKm(distKm)
                                .durationMinutes(durationMinutes)
                                .routeAvailable(true)
                                .build());
                        continue;
                    }
                }

                // If null, negative, or missing, destination is unreachable by road network
                metrics.add(RoadMetric.builder()
                        .distanceKm(null)
                        .durationMinutes(null)
                        .routeAvailable(false)
                        .build());
            }

            // Cache valid result
            storeInCache(cacheKey, metrics);

            return TableMatrixResult.success(metrics);

        } catch (HttpTimeoutException e) {
            log.warn("OSRM Table API timed out after {}ms for URL {}", timeoutMs, requestUrl);
            return TableMatrixResult.failure("OSRM request timed out after " + timeoutMs + "ms");
        } catch (IOException e) {
            log.warn("I/O error communicating with OSRM Table API: {}", e.getMessage());
            return TableMatrixResult.failure("OSRM network failure: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("OSRM request interrupted: {}", e.getMessage());
            return TableMatrixResult.failure("OSRM request interrupted");
        } catch (Exception e) {
            log.warn("Unexpected error in OSRM routing client: {}", e.getMessage(), e);
            return TableMatrixResult.failure("Unexpected routing client error: " + e.getMessage());
        }
    }

    private String generateTargetsCacheKey(Double userLat, Double userLng, List<CoordinateTarget> targets) {
        String coordsPart = String.format(Locale.US, "%.3f,%.3f", userLat, userLng);
        String idsPart = targets.stream()
                .map(t -> t.id() != null && t.id().length() >= 8 ? t.id().substring(0, 8) : String.valueOf(t.id()))
                .collect(Collectors.joining(","));
        return coordsPart + ":" + idsPart;
    }

    private void storeInCache(String key, List<RoadMetric> metrics) {
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            // Evict expired entries or clear if full
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired(cacheTtlMs));
            if (cache.size() >= MAX_CACHE_ENTRIES) {
                cache.clear();
            }
        }
        cache.put(key, new CacheEntry(metrics, System.currentTimeMillis()));
    }

    public void clearCache() {
        cache.clear();
    }

    private static class CacheEntry {
        final List<RoadMetric> metrics;
        final long timestamp;

        CacheEntry(List<RoadMetric> metrics, long timestamp) {
            this.metrics = metrics;
            this.timestamp = timestamp;
        }

        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
    }
}
