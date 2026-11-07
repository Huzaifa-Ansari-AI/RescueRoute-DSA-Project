package com.huzaifa.rescueroute.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OverpassClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private OverpassClient overpassClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        overpassClient = new OverpassClient(
                "https://overpass-api.de/api/interpreter",
                2000,
                5000.0,
                10000.0,
                "RescueRoute-Test/1.0",
                httpClient,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should build valid Overpass QL query containing all medical categories and bounded radius")
    void shouldBuildOverpassQuery() {
        String query = overpassClient.buildOverpassQuery(24.8607, 67.0011, 3000.0, null);

        assertNotNull(query);
        assertTrue(query.contains("[out:json]"));
        assertTrue(query.contains("(around:3000.0,24.860700,67.001100)"));
        assertTrue(query.contains("amenity\"=\"hospital"));
        assertTrue(query.contains("amenity\"=\"clinic"));
        assertTrue(query.contains("amenity\"=\"pharmacy"));
        assertTrue(query.contains("amenity\"=\"blood_donation"));
        assertTrue(query.contains("emergency\"=\"ambulance_station"));
        assertTrue(query.contains("out center tags;"));
    }

    @Test
    @DisplayName("Should parse node elements with coordinates, tags, and category mapping")
    void shouldParseNodeElements() throws IOException, InterruptedException {
        String mockJson = """
        {
          "version": 0.6,
          "elements": [
            {
              "type": "node",
              "id": 1001,
              "lat": 24.8615,
              "lon": 67.0025,
              "tags": {
                "name": "Civil Hospital Karachi",
                "amenity": "hospital",
                "addr:street": "Mission Road",
                "phone": "+92 21 99215740",
                "opening_hours": "24/7"
              }
            },
            {
              "type": "node",
              "id": 1002,
              "lat": 24.8650,
              "lon": 67.0050,
              "tags": {
                "name": "Al-Khidmat Pharmacy",
                "healthcare": "pharmacy",
                "addr:street": "MA Jinnah Road"
              }
            }
          ]
        }
        """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockJson);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        List<OsmElement> results = overpassClient.queryMedicalFacilities(24.86, 67.00, 2000.0, null);

        assertNotNull(results);
        assertEquals(2, results.size());

        OsmElement hospital = results.get(0);
        assertEquals("node", hospital.getElementType());
        assertEquals(1001L, hospital.getElementId());
        assertEquals("Civil Hospital Karachi", hospital.getName());
        assertEquals("Hospital", hospital.getFacilityType());
        assertEquals(24.8615, hospital.getLatitude());
        assertEquals(67.0025, hospital.getLongitude());
        assertEquals("Mission Road", hospital.getAddress());
        assertEquals("+92 21 99215740", hospital.getPhoneNumber());
        assertEquals("24/7", hospital.getOpeningHours());
        assertEquals("https://www.openstreetmap.org/node/1001", hospital.getSourceUrl());

        OsmElement pharmacy = results.get(1);
        assertEquals(1002L, pharmacy.getElementId());
        assertEquals("Al-Khidmat Pharmacy", pharmacy.getName());
        assertEquals("Pharmacy", pharmacy.getFacilityType());
    }

    @Test
    @DisplayName("Should parse way and relation elements using center coordinates")
    void shouldParseWaysWithCenterCoordinates() throws IOException, InterruptedException {
        String mockJson = """
        {
          "version": 0.6,
          "elements": [
            {
              "type": "way",
              "id": 5001,
              "center": {
                "lat": 24.8933,
                "lon": 67.0754
              },
              "tags": {
                "name": "Aga Khan University Hospital Complex",
                "amenity": "hospital",
                "website": "https://hospitals.aku.edu"
              }
            }
          ]
        }
        """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockJson);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        List<OsmElement> results = overpassClient.queryMedicalFacilities(24.89, 67.07, 3000.0, "Hospital");

        assertEquals(1, results.size());
        OsmElement way = results.get(0);
        assertEquals("way", way.getElementType());
        assertEquals(5001L, way.getElementId());
        assertEquals(24.8933, way.getLatitude());
        assertEquals(67.0754, way.getLongitude());
        assertEquals("Aga Khan University Hospital Complex", way.getName());
        assertEquals("https://hospitals.aku.edu", way.getWebsite());
    }

    @Test
    @DisplayName("Should skip elements with missing coordinates or unsupported tags")
    void shouldSkipInvalidOrUnsupportedElements() {
        String mockJson = """
        {
          "version": 0.6,
          "elements": [
            {
              "type": "node",
              "id": 9001,
              "tags": {
                "name": "No Coordinates Clinic",
                "amenity": "clinic"
              }
            },
            {
              "type": "node",
              "id": 9002,
              "lat": 24.85,
              "lon": 67.01,
              "tags": {
                "name": "Fast Food Corner",
                "amenity": "fast_food"
              }
            }
          ]
        }
        """;

        List<OsmElement> parsed = overpassClient.parseOverpassResponse(mockJson, null);
        assertTrue(parsed.isEmpty(), "Should skip element missing lat/lon and element with non-medical amenity");
    }

    @Test
    @DisplayName("Should handle genuine empty response from Overpass returning empty list")
    void shouldHandleGenuineEmptyResponse() throws IOException, InterruptedException {
        String mockEmptyJson = """
        {
          "version": 0.6,
          "elements": []
        }
        """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockEmptyJson);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        List<OsmElement> results = overpassClient.queryMedicalFacilities(24.86, 67.00, 2000.0, null);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should handle Overpass HTTP timeouts and throw ExternalServiceException with GATEWAY_TIMEOUT after retries")
    void shouldHandleTimeoutWithRetriesAndThrow() throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new HttpTimeoutException("Overpass request timed out"))
                .thenThrow(new HttpTimeoutException("Overpass request timed out again"))
                .thenThrow(new HttpTimeoutException("Overpass final timeout"));

        com.huzaifa.rescueroute.common.exception.ExternalServiceException ex =
                assertThrows(com.huzaifa.rescueroute.common.exception.ExternalServiceException.class, () ->
                        overpassClient.queryMedicalFacilities(24.86, 67.00, 2000.0, null));

        assertEquals(org.springframework.http.HttpStatus.GATEWAY_TIMEOUT, ex.getStatus());
        assertTrue(ex.getMessage().contains("timed out"));
        verify(httpClient, times(3)).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("Should handle HTTP 429 rate limit and throw ExternalServiceException with TOO_MANY_REQUESTS")
    void shouldHandleRateLimitAndThrow() throws IOException, InterruptedException {
        when(httpResponse.statusCode()).thenReturn(429);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        com.huzaifa.rescueroute.common.exception.ExternalServiceException ex =
                assertThrows(com.huzaifa.rescueroute.common.exception.ExternalServiceException.class, () ->
                        overpassClient.queryMedicalFacilities(24.86, 67.00, 2000.0, null));

        assertEquals(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        assertTrue(ex.getMessage().contains("rate limit"));
    }

    @Test
    @DisplayName("Should handle HTTP 504 server error and throw ExternalServiceException with BAD_GATEWAY")
    void shouldHandle5xxServerErrorAndThrow() throws IOException, InterruptedException {
        when(httpResponse.statusCode()).thenReturn(504);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        com.huzaifa.rescueroute.common.exception.ExternalServiceException ex =
                assertThrows(com.huzaifa.rescueroute.common.exception.ExternalServiceException.class, () ->
                        overpassClient.queryMedicalFacilities(24.86, 67.00, 2000.0, null));

        assertEquals(org.springframework.http.HttpStatus.BAD_GATEWAY, ex.getStatus());
        assertTrue(ex.getMessage().contains("temporarily unavailable"));
    }

    @Test
    @DisplayName("Should handle network connection failure and throw ExternalServiceException with SERVICE_UNAVAILABLE")
    void shouldHandleConnectionFailureAndThrow() throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new IOException("Connection reset by peer"));

        com.huzaifa.rescueroute.common.exception.ExternalServiceException ex =
                assertThrows(com.huzaifa.rescueroute.common.exception.ExternalServiceException.class, () ->
                        overpassClient.queryMedicalFacilities(24.86, 67.00, 2000.0, null));

        assertEquals(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertTrue(ex.getMessage().contains("Network error"));
    }

    @Test
    @DisplayName("Should throw ExternalServiceException on invalid or malformed JSON")
    void shouldThrowOnMalformedJson() {
        assertThrows(com.huzaifa.rescueroute.common.exception.ExternalServiceException.class, () ->
                overpassClient.parseOverpassResponse("<html><head><title>502 Bad Gateway</title></head></html>", null));
    }

    @Test
    @DisplayName("Should reject invalid coordinates")
    void shouldRejectInvalidCoordinates() {
        assertThrows(IllegalArgumentException.class, () ->
                overpassClient.queryMedicalFacilities(95.0, 67.0, 1000.0, null));
        assertThrows(IllegalArgumentException.class, () ->
                overpassClient.queryMedicalFacilities(24.0, 190.0, 1000.0, null));
        assertThrows(IllegalArgumentException.class, () ->
                overpassClient.queryMedicalFacilities(null, 67.0, 1000.0, null));
    }

    @Test
    @DisplayName("Category filtering: should build query for specific categories correctly")
    void shouldBuildQueryForSpecificCategories() {
        String hospitalQuery = overpassClient.buildOverpassQuery(24.86, 67.00, 1000.0, "Hospital");
        assertTrue(hospitalQuery.contains("amenity\"=\"hospital"));
        assertFalse(hospitalQuery.contains("emergency\"=\"ambulance_station"));

        String pharmacyQuery = overpassClient.buildOverpassQuery(24.86, 67.00, 1000.0, "Pharmacy");
        assertTrue(pharmacyQuery.contains("amenity\"=\"pharmacy"));
        assertFalse(pharmacyQuery.contains("amenity\"=\"hospital"));

        String ambulanceQuery = overpassClient.buildOverpassQuery(24.86, 67.00, 1000.0, "Ambulance");
        assertTrue(ambulanceQuery.contains("emergency\"=\"ambulance_station"));
        assertFalse(ambulanceQuery.contains("amenity\"=\"clinic"));

        String clinicQuery = overpassClient.buildOverpassQuery(24.86, 67.00, 1000.0, "Clinic");
        assertTrue(clinicQuery.contains("amenity\"=\"clinic"));
        assertFalse(clinicQuery.contains("amenity\"=\"blood_donation"));

        String bloodBankQuery = overpassClient.buildOverpassQuery(24.86, 67.00, 1000.0, "Blood Bank");
        assertTrue(bloodBankQuery.contains("amenity\"=\"blood_donation"));
        assertFalse(bloodBankQuery.contains("amenity\"=\"hospital"));
    }

    @Test
    @DisplayName("Category null-safety: null, blank, ALL, and unsupported categories")
    void shouldHandleNullAndUnsupportedCategoriesSafely() {
        // null category -> queries all
        String nullQuery = overpassClient.buildOverpassQuery(24.86, 67.00, 1000.0, null);
        assertTrue(nullQuery.contains("amenity\"=\"hospital"));
        assertTrue(nullQuery.contains("emergency\"=\"ambulance_station"));

        // blank category -> queries all
        String blankQuery = overpassClient.buildOverpassQuery(24.86, 67.00, 1000.0, "   ");
        assertTrue(blankQuery.contains("amenity\"=\"hospital"));
        assertTrue(blankQuery.contains("emergency\"=\"ambulance_station"));

        // ALL category -> queries all
        String allQuery = overpassClient.buildOverpassQuery(24.86, 67.00, 1000.0, "ALL");
        assertTrue(allQuery.contains("amenity\"=\"hospital"));
        assertTrue(allQuery.contains("emergency\"=\"ambulance_station"));

        // Unsupported category -> query contains no medical clauses (only outer wrapper)
        String unsupportedQuery = overpassClient.buildOverpassQuery(24.86, 67.00, 1000.0, "Gymnasium");
        assertFalse(unsupportedQuery.contains("amenity\"=\"hospital"));
        assertFalse(unsupportedQuery.contains("emergency\"=\"ambulance_station"));
    }

    @Test
    @DisplayName("Response parsing with category filtering and null-safety")
    void shouldFilterResponseByCategorySafely() {
        String mockJson = """
        {
          "elements": [
            {
              "type": "node",
              "id": 101,
              "lat": 24.86,
              "lon": 67.00,
              "tags": { "name": "Hospital A", "amenity": "hospital" }
            },
            {
              "type": "node",
              "id": 102,
              "lat": 24.87,
              "lon": 67.01,
              "tags": { "name": "Pharmacy B", "amenity": "pharmacy" }
            }
          ]
        }
        """;

        // null category returns all
        List<OsmElement> all = overpassClient.parseOverpassResponse(mockJson, null);
        assertEquals(2, all.size());

        // specific category returns only matching
        List<OsmElement> hospitalsOnly = overpassClient.parseOverpassResponse(mockJson, "Hospital");
        assertEquals(1, hospitalsOnly.size());
        assertEquals("Hospital A", hospitalsOnly.get(0).getName());

        // unsupported category returns empty list safely
        List<OsmElement> none = overpassClient.parseOverpassResponse(mockJson, "NonExistentCategory");
        assertTrue(none.isEmpty());
    }
}
