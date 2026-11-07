package com.huzaifa.rescueroute.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huzaifa.rescueroute.common.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Resilient client for querying medical points of interest from the OpenStreetMap Overpass API.
 * Encapsulates query generation, HTTP execution with backoff, response parsing, and category normalization.
 */
@Component
public class OverpassClient {

    private static final Logger log = LoggerFactory.getLogger(OverpassClient.class);

    private final String overpassUrl;
    private final int timeoutMs;
    private final double defaultRadiusMeters;
    private final double maxRadiusMeters;
    private final String userAgent;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OverpassClient() {
        this("https://overpass-api.de/api/interpreter", 120000, 5000.0, 10000.0,
                "RescueRoute/1.0 (rescueroute-emergency-app)", null, new ObjectMapper());
    }

    @Autowired
    public OverpassClient(
            @Value("${app.overpass.url:https://overpass-api.de/api/interpreter}") String overpassUrl,
            @Value("${app.overpass.timeout-ms:120000}") int timeoutMs,
            @Value("${app.overpass.default-radius-meters:5000}") double defaultRadiusMeters,
            @Value("${app.overpass.max-radius-meters:10000}") double maxRadiusMeters,
            @Value("${app.overpass.user-agent:RescueRoute/1.0 (rescueroute-emergency-app)}") String userAgent) {
        this(overpassUrl, timeoutMs, defaultRadiusMeters, maxRadiusMeters, userAgent, null, new ObjectMapper());
    }

    public OverpassClient(
            String overpassUrl,
            int timeoutMs,
            double defaultRadiusMeters,
            double maxRadiusMeters,
            String userAgent,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.overpassUrl = (overpassUrl != null && !overpassUrl.isBlank())
                ? overpassUrl.trim()
                : "https://overpass-api.de/api/interpreter";
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : 120000;
        this.defaultRadiusMeters = defaultRadiusMeters > 0 ? defaultRadiusMeters : 5000.0;
        this.maxRadiusMeters = maxRadiusMeters > 0 ? maxRadiusMeters : 10000.0;
        this.userAgent = (userAgent != null && !userAgent.isBlank())
                ? userAgent.trim()
                : "RescueRoute/1.0 (rescueroute-emergency-app)";
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(this.timeoutMs))
                .build();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Queries the Overpass API for medical facilities around a coordinate and radius.
     *
     * @param lat Center latitude
     * @param lng Center longitude
     * @param radiusMeters Radius in meters (capped at maxRadiusMeters)
     * @param targetCategory Optional specific category filter (e.g. 'Hospital', 'Clinic', etc.)
     * @return List of parsed and normalized OsmElement POIs
     */
    public List<OsmElement> queryMedicalFacilities(Double lat, Double lng, Double radiusMeters, String targetCategory) {
        validateCoordinates(lat, lng);

        double effectiveRadius = (radiusMeters != null && radiusMeters > 0)
                ? Math.min(radiusMeters, maxRadiusMeters)
                : defaultRadiusMeters;

        String query = buildOverpassQuery(lat, lng, effectiveRadius, targetCategory);
        log.info("[OverpassClient] Executing Overpass query around ({}, {}) with radius {}m (timeout {}ms)",
                lat, lng, effectiveRadius, timeoutMs);

        String responseBody = executeWithRetries(query);
        if (responseBody == null || responseBody.isBlank()) {
            throw new ExternalServiceException("Received empty response body from OpenStreetMap Overpass API.", HttpStatus.BAD_GATEWAY);
        }

        return parseOverpassResponse(responseBody, targetCategory);
    }

    /**
     * Constructs the Overpass QL query searching nodes, ways, and relations.
     * Uses optimized 'nwr' syntax for high performance across large metropolitan areas.
     */
    public String buildOverpassQuery(double lat, double lng, double radiusMeters, String targetCategory) {
        StringBuilder sb = new StringBuilder();
        int queryTimeoutSeconds = Math.max(120, timeoutMs / 1000);
        sb.append(String.format(Locale.US, "[out:json][timeout:%d];\n(\n", queryTimeoutSeconds));

        String rLatLon = String.format(Locale.US, "(around:%.1f,%.6f,%.6f)", radiusMeters, lat, lng);

        String normalizedCategory = (targetCategory == null || targetCategory.isBlank()) ? "ALL" : targetCategory.trim();
        boolean queryAll = "ALL".equalsIgnoreCase(normalizedCategory);

        if (queryAll || "Hospital".equalsIgnoreCase(normalizedCategory)) {
            sb.append("  nwr[\"amenity\"=\"hospital\"]").append(rLatLon).append(";\n");
            sb.append("  nwr[\"healthcare\"=\"hospital\"]").append(rLatLon).append(";\n");
        }

        if (queryAll || "Clinic".equalsIgnoreCase(normalizedCategory)) {
            sb.append("  nwr[\"amenity\"=\"clinic\"]").append(rLatLon).append(";\n");
            sb.append("  nwr[\"amenity\"=\"doctors\"]").append(rLatLon).append(";\n");
            sb.append("  nwr[\"amenity\"=\"dentist\"]").append(rLatLon).append(";\n");
            sb.append("  nwr[\"healthcare\"=\"clinic\"]").append(rLatLon).append(";\n");
            sb.append("  nwr[\"healthcare\"=\"doctor\"]").append(rLatLon).append(";\n");
            sb.append("  nwr[\"healthcare\"=\"dentist\"]").append(rLatLon).append(";\n");
        }

        if (queryAll || "Pharmacy".equalsIgnoreCase(normalizedCategory)) {
            sb.append("  nwr[\"amenity\"=\"pharmacy\"]").append(rLatLon).append(";\n");
            sb.append("  nwr[\"healthcare\"=\"pharmacy\"]").append(rLatLon).append(";\n");
        }

        if (queryAll || "Blood Bank".equalsIgnoreCase(normalizedCategory)) {
            sb.append("  nwr[\"amenity\"=\"blood_donation\"]").append(rLatLon).append(";\n");
            sb.append("  nwr[\"healthcare\"=\"blood_bank\"]").append(rLatLon).append(";\n");
        }

        if (queryAll || "Ambulance".equalsIgnoreCase(normalizedCategory)) {
            sb.append("  nwr[\"emergency\"=\"ambulance_station\"]").append(rLatLon).append(";\n");
        }

        sb.append(");\nout center tags;\n");
        return sb.toString();
    }

    /**
     * Executes the HTTP request against Overpass with up to 2 retries on transient errors.
     * Throws ExternalServiceException on persistent timeout, rate limit, or server errors.
     */
    private String executeWithRetries(String query) {
        int maxAttempts = 3;
        int attempt = 0;
        long backoffMs = 500L;
        ExternalServiceException lastFailureException = null;

        while (attempt < maxAttempts) {
            attempt++;
            try {
                String formBody = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(overpassUrl))
                        .timeout(Duration.ofMillis(timeoutMs))
                        .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .header("User-Agent", userAgent)
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = response.statusCode();

                if (status == 200) {
                    return response.body();
                } else if (status == 429) {
                    log.warn("[OverpassClient] HTTP 429 Too Many Requests from Overpass (attempt {}/{})", attempt, maxAttempts);
                    if (response.headers() != null) {
                        Optional<String> retryAfterHeader = response.headers().firstValue("Retry-After");
                        if (retryAfterHeader != null && retryAfterHeader.isPresent()) {
                            try {
                                long seconds = Long.parseLong(retryAfterHeader.get().trim());
                                backoffMs = Math.min(seconds * 1000L, 5000L);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    lastFailureException = new ExternalServiceException(
                            "OpenStreetMap Overpass API rate limit exceeded. Please wait a moment before trying again.",
                            HttpStatus.TOO_MANY_REQUESTS
                    );
                } else if (status >= 500) {
                    log.warn("[OverpassClient] HTTP {} Server Error from Overpass (attempt {}/{})", status, attempt, maxAttempts);
                    lastFailureException = new ExternalServiceException(
                            "OpenStreetMap Overpass API is temporarily unavailable (HTTP " + status + "). Please retry shortly.",
                            HttpStatus.BAD_GATEWAY
                    );
                } else {
                    log.error("[OverpassClient] Permanent HTTP error {} from Overpass: {}", status, response.body());
                    throw new ExternalServiceException(
                            "OpenStreetMap Overpass API returned HTTP " + status + ".",
                            HttpStatus.BAD_GATEWAY
                    );
                }
            } catch (HttpTimeoutException e) {
                log.warn("[OverpassClient] HTTP timeout calling Overpass (attempt {}/{}): {}", attempt, maxAttempts, e.getMessage());
                lastFailureException = new ExternalServiceException(
                        "OpenStreetMap Overpass API request timed out after " + (timeoutMs / 1000) + " seconds. Please try a smaller search radius (e.g. 2–3 km) or retry.",
                        HttpStatus.GATEWAY_TIMEOUT
                );
            } catch (IOException e) {
                log.warn("[OverpassClient] Network I/O error calling Overpass (attempt {}/{}): {}", attempt, maxAttempts, e.getMessage());
                lastFailureException = new ExternalServiceException(
                        "Network error connecting to OpenStreetMap Overpass service.",
                        HttpStatus.SERVICE_UNAVAILABLE
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[OverpassClient] Interrupted while waiting for Overpass response");
                throw new ExternalServiceException("Query to OpenStreetMap Overpass was interrupted.", HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (ExternalServiceException e) {
                throw e;
            } catch (Exception e) {
                log.error("[OverpassClient] Unexpected exception calling Overpass: {}", e.getMessage(), e);
                throw new ExternalServiceException("Unexpected error communicating with OpenStreetMap Overpass.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ExternalServiceException("Query to OpenStreetMap Overpass was interrupted during backoff.", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }

        log.error("[OverpassClient] All {} attempts to query Overpass failed.", maxAttempts);
        if (lastFailureException != null) {
            throw lastFailureException;
        }
        throw new ExternalServiceException("Unable to contact OpenStreetMap Overpass API after " + maxAttempts + " attempts.", HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * Parses the Overpass JSON response into normalized OsmElement objects.
     */
    public List<OsmElement> parseOverpassResponse(String jsonString, String targetCategory) {
        List<OsmElement> results = new ArrayList<>();
        if (jsonString == null || jsonString.isBlank()) {
            return results;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(jsonString);
        } catch (Exception e) {
            log.error("[OverpassClient] Failed to parse Overpass JSON: {}", e.getMessage(), e);
            throw new ExternalServiceException("Invalid or malformed JSON received from OpenStreetMap Overpass.", HttpStatus.BAD_GATEWAY);
        }

        // Check for Overpass inline error/remark
        if (root.has("remark")) {
            String remark = root.get("remark").asText();
            if (remark.toLowerCase().contains("timeout") || remark.toLowerCase().contains("timed out")) {
                throw new ExternalServiceException("OpenStreetMap Overpass server query timed out: " + remark, HttpStatus.GATEWAY_TIMEOUT);
            } else if (remark.toLowerCase().contains("quota") || remark.toLowerCase().contains("rate_limited") || remark.toLowerCase().contains("too many")) {
                throw new ExternalServiceException("OpenStreetMap Overpass rate limit reached: " + remark, HttpStatus.TOO_MANY_REQUESTS);
            }
        }

        JsonNode elementsNode = root.get("elements");
        if (elementsNode == null || !elementsNode.isArray()) {
            log.warn("[OverpassClient] Overpass response did not contain 'elements' array.");
            return results;
        }

        String normalizedCategory = (targetCategory == null || targetCategory.isBlank()) ? "ALL" : targetCategory.trim();
        boolean queryAll = "ALL".equalsIgnoreCase(normalizedCategory);

        for (JsonNode element : elementsNode) {
            OsmElement parsed = parseElement(element);
            if (parsed != null) {
                if (queryAll || normalizedCategory.equalsIgnoreCase(parsed.getFacilityType())) {
                    results.add(parsed);
                }
            }
        }

        return results;
    }

    /**
     * Parses an individual Overpass element node/way/relation.
     */
    private OsmElement parseElement(JsonNode element) {
        try {
            String type = element.has("type") ? element.get("type").asText() : null;
            Long id = element.has("id") ? element.get("id").asLong() : null;

            if (type == null || id == null) {
                return null;
            }

            // Extract coordinates: nodes have 'lat'/'lon', ways/relations have 'center.lat'/'center.lon'
            Double lat = null;
            Double lon = null;

            if (element.has("lat") && element.has("lon")) {
                lat = element.get("lat").asDouble();
                lon = element.get("lon").asDouble();
            } else if (element.has("center") && element.get("center").isObject()) {
                JsonNode center = element.get("center");
                if (center.has("lat") && center.has("lon")) {
                    lat = center.get("lat").asDouble();
                    lon = center.get("lon").asDouble();
                }
            }

            if (lat == null || lon == null || !isValidCoordinate(lat, lon)) {
                log.debug("[OverpassClient] Skipping element {}/{} due to missing/invalid coordinates", type, id);
                return null;
            }

            JsonNode tags = element.get("tags");
            if (tags == null || !tags.isObject()) {
                return null;
            }

            String facilityType = mapTagsToCategory(tags);
            if (facilityType == null) {
                // Ambiguous or unsupported tag
                return null;
            }

            String name = extractName(tags);
            if (name == null || name.isBlank()) {
                // Default to normalized category fallback
                name = "Unnamed " + facilityType + " (" + type + " #" + id + ")";
            }

            String address = extractAddress(tags);
            String city = tags.has("addr:city") ? tags.get("addr:city").asText().trim() : "Karachi";
            String phone = extractPhone(tags);
            String website = extractWebsite(tags);
            String openingHours = tags.has("opening_hours") ? tags.get("opening_hours").asText().trim() : null;
            String sourceUrl = "https://www.openstreetmap.org/" + type + "/" + id;
            String rawTagsJson = tags.toString();

            return OsmElement.builder()
                    .elementType(type)
                    .elementId(id)
                    .name(name)
                    .facilityType(facilityType)
                    .latitude(lat)
                    .longitude(lon)
                    .address(address)
                    .city(city)
                    .phoneNumber(phone)
                    .website(website)
                    .openingHours(openingHours)
                    .sourceUrl(sourceUrl)
                    .rawTagsJson(rawTagsJson)
                    .build();

        } catch (Exception e) {
            log.warn("[OverpassClient] Error parsing element: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Defensible mapping from OSM tags to RescueRoute facility categories.
     * Rejects ambiguous tags to prevent erroneous clinical classifications.
     */
    public String mapTagsToCategory(JsonNode tags) {
        String amenity = tags.has("amenity") ? tags.get("amenity").asText().toLowerCase(Locale.ROOT) : "";
        String healthcare = tags.has("healthcare") ? tags.get("healthcare").asText().toLowerCase(Locale.ROOT) : "";
        String emergency = tags.has("emergency") ? tags.get("emergency").asText().toLowerCase(Locale.ROOT) : "";

        // 1. Hospital
        if ("hospital".equals(amenity) || "hospital".equals(healthcare)) {
            return "Hospital";
        }

        // 2. Pharmacy
        if ("pharmacy".equals(amenity) || "pharmacy".equals(healthcare)) {
            return "Pharmacy";
        }

        // 3. Blood Bank
        if ("blood_donation".equals(amenity) || "blood_bank".equals(healthcare)) {
            return "Blood Bank";
        }

        // 4. Ambulance Station
        if ("ambulance_station".equals(emergency)) {
            return "Ambulance";
        }

        // 5. Clinic (Doctors, Dentist, Outpatient Clinics)
        if ("clinic".equals(amenity) || "doctors".equals(amenity) || "dentist".equals(amenity)
                || "clinic".equals(healthcare) || "doctor".equals(healthcare) || "dentist".equals(healthcare)) {
            return "Clinic";
        }

        return null; // Ambiguous or unsupported
    }

    private String extractName(JsonNode tags) {
        if (tags.has("name") && !tags.get("name").asText().isBlank()) {
            return tags.get("name").asText().trim();
        }
        if (tags.has("name:en") && !tags.get("name:en").asText().isBlank()) {
            return tags.get("name:en").asText().trim();
        }
        if (tags.has("operator") && !tags.get("operator").asText().isBlank()) {
            return tags.get("operator").asText().trim();
        }
        return null;
    }

    private String extractAddress(JsonNode tags) {
        if (tags.has("addr:full") && !tags.get("addr:full").asText().isBlank()) {
            return tags.get("addr:full").asText().trim();
        }

        List<String> parts = new ArrayList<>();
        if (tags.has("addr:housenumber")) parts.add(tags.get("addr:housenumber").asText().trim());
        if (tags.has("addr:street")) parts.add(tags.get("addr:street").asText().trim());
        if (tags.has("addr:suburb")) parts.add(tags.get("addr:suburb").asText().trim());
        if (tags.has("addr:district")) parts.add(tags.get("addr:district").asText().trim());

        if (!parts.isEmpty()) {
            return String.join(", ", parts);
        }

        return null;
    }

    private String extractPhone(JsonNode tags) {
        if (tags.has("phone")) return tags.get("phone").asText().trim();
        if (tags.has("contact:phone")) return tags.get("contact:phone").asText().trim();
        if (tags.has("contact:mobile")) return tags.get("contact:mobile").asText().trim();
        return null;
    }

    private String extractWebsite(JsonNode tags) {
        if (tags.has("website")) return tags.get("website").asText().trim();
        if (tags.has("contact:website")) return tags.get("contact:website").asText().trim();
        return null;
    }

    private boolean isValidCoordinate(Double lat, Double lon) {
        return lat != null && lon != null
                && !Double.isNaN(lat) && !Double.isNaN(lon)
                && !Double.isInfinite(lat) && !Double.isInfinite(lon)
                && lat >= -90.0 && lat <= 90.0
                && lon >= -180.0 && lon <= 180.0;
    }

    private void validateCoordinates(Double lat, Double lng) {
        if (lat == null || lng == null) {
            throw new IllegalArgumentException("Center coordinates cannot be null.");
        }
        if (!isValidCoordinate(lat, lng)) {
            throw new IllegalArgumentException("Coordinates out of range: lat [-90, 90], lng [-180, 180].");
        }
    }
}
