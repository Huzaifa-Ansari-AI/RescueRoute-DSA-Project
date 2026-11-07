package com.huzaifa.rescueroute.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huzaifa.rescueroute.entity.Facility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OsrmRoutingClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private OsrmRoutingClient osrmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        osrmClient = new OsrmRoutingClient(
                "https://router.project-osrm.org",
                "/table/v1/driving",
                2000,
                10,
                60000L,
                httpClient,
                objectMapper
        );
    }

    private Facility createFacility(String name, double lat, double lng) {
        return Facility.builder()
                .id(UUID.randomUUID())
                .name(name)
                .facilityType("Hospital")
                .latitude(lat)
                .longitude(lng)
                .build();
    }

    @Test
    @DisplayName("Constructs correct URL with sources=0 and longitude,latitude order")
    void testUrlConstructionAndCoordinateOrdering() throws Exception {
        Facility jpmc = createFacility("JPMC", 24.8524, 67.0456);
        Facility indus = createFacility("Indus", 24.8517, 67.0412);

        String jsonResponse = "{\"code\":\"Ok\",\"durations\":[[0,660.0,720.0]],\"distances\":[[0,8700.0,9000.0]]}";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        TableMatrixResult result = osrmClient.calculateDistanceMatrix(24.8937, 67.0748, List.of(jpmc, indus));

        assertTrue(result.isSuccessful());
        assertEquals(2, result.getMetrics().size());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any());

        String uri = requestCaptor.getValue().uri().toString();
        // Origin first in lng,lat format: 67.074800,24.893700
        assertTrue(uri.contains("67.074800,24.893700"));
        // Followed by candidates
        assertTrue(uri.contains("67.045600,24.852400"));
        assertTrue(uri.contains("67.041200,24.851700"));
        assertTrue(uri.contains("sources=0"));
        assertTrue(uri.contains("annotations=distance,duration"));
    }

    @Test
    @DisplayName("Parses matrix response correctly and converts seconds to minutes and meters to km")
    void testMetricsParsing() throws Exception {
        Facility f1 = createFacility("F1", 24.85, 67.04);
        Facility f2 = createFacility("F2", 24.86, 67.05);

        // Durations: [0, 660.0 s (11 min), 724.0 s (12 min)]
        // Distances: [0, 8740.0 m (8.7 km), 9010.0 m (9.0 km)]
        String json = "{\"code\":\"Ok\",\"durations\":[[0,660.0,724.0]],\"distances\":[[0,8740.0,9010.0]]}";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        TableMatrixResult result = osrmClient.calculateDistanceMatrix(24.89, 67.07, List.of(f1, f2));

        assertTrue(result.isSuccessful());
        List<RoadMetric> metrics = result.getMetrics();
        assertEquals(2, metrics.size());

        // F1
        assertTrue(metrics.get(0).isRouteAvailable());
        assertEquals(8.7, metrics.get(0).getDistanceKm(), 0.01);
        assertEquals(11, metrics.get(0).getDurationMinutes());

        // F2
        assertTrue(metrics.get(1).isRouteAvailable());
        assertEquals(9.0, metrics.get(1).getDistanceKm(), 0.01);
        assertEquals(12, metrics.get(1).getDurationMinutes());
    }

    @Test
    @DisplayName("Handles partial matrix when a candidate route is null or unreachable")
    void testPartialMatrixWithNullRoute() throws Exception {
        Facility reachable = createFacility("Reachable", 24.85, 67.04);
        Facility unreachable = createFacility("Island", 24.80, 66.90);

        String json = "{\"code\":\"Ok\",\"durations\":[[0,600.0,null]],\"distances\":[[0,5000.0,null]]}";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        TableMatrixResult result = osrmClient.calculateDistanceMatrix(24.89, 67.07, List.of(reachable, unreachable));

        assertTrue(result.isSuccessful());
        assertEquals(2, result.getMetrics().size());

        // First candidate is reachable
        assertTrue(result.getMetrics().get(0).isRouteAvailable());
        assertEquals(5.0, result.getMetrics().get(0).getDistanceKm(), 0.01);
        assertEquals(10, result.getMetrics().get(0).getDurationMinutes());

        // Second candidate is unreachable
        assertFalse(result.getMetrics().get(1).isRouteAvailable());
        assertNull(result.getMetrics().get(1).getDistanceKm());
        assertNull(result.getMetrics().get(1).getDurationMinutes());
    }

    @Test
    @DisplayName("Gracefully handles HTTP 500 error without throwing exceptions")
    void testHttpError() throws Exception {
        Facility f1 = createFacility("F1", 24.85, 67.04);

        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("Internal Server Error");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        TableMatrixResult result = osrmClient.calculateDistanceMatrix(24.89, 67.07, List.of(f1));

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorMessage().contains("500"));
    }

    @Test
    @DisplayName("Gracefully handles HttpTimeoutException without throwing exceptions")
    void testTimeoutHandling() throws Exception {
        Facility f1 = createFacility("F1", 24.85, 67.04);

        when(httpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new HttpTimeoutException("Request timed out"));

        TableMatrixResult result = osrmClient.calculateDistanceMatrix(24.89, 67.07, List.of(f1));

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorMessage().contains("timed out"));
    }

    @Test
    @DisplayName("Caches matrix responses and reuses without second HTTP call")
    void testCaching() throws Exception {
        Facility f1 = createFacility("F1", 24.85, 67.04);

        String json = "{\"code\":\"Ok\",\"durations\":[[0,300.0]],\"distances\":[[0,2500.0]]}";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        // First call
        TableMatrixResult res1 = osrmClient.calculateDistanceMatrix(24.89, 67.07, List.of(f1));
        assertTrue(res1.isSuccessful());

        // Second call with same origin and facility
        TableMatrixResult res2 = osrmClient.calculateDistanceMatrix(24.89, 67.07, List.of(f1));
        assertTrue(res2.isSuccessful());

        // Verify httpClient was invoked only once
        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    @DisplayName("Empty or null inputs return failure result immediately")
    void testEmptyInputs() {
        TableMatrixResult r1 = osrmClient.calculateDistanceMatrix(null, 67.0, List.of());
        assertFalse(r1.isSuccessful());

        TableMatrixResult r2 = osrmClient.calculateDistanceMatrix(24.0, null, List.of());
        assertFalse(r2.isSuccessful());

        TableMatrixResult r3 = osrmClient.calculateDistanceMatrix(24.0, 67.0, Collections.emptyList());
        assertFalse(r3.isSuccessful());
    }
}
