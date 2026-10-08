package com.huzaifa.rescueroute.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HaversineUtilTest {

    @Test
    @DisplayName("Zero distance for identical coordinates")
    void testZeroDistance() {
        double dist = HaversineUtil.calculateDistance(24.8607, 67.0011, 24.8607, 67.0011);
        assertEquals(0.0, dist, 0.0001);
    }

    @Test
    @DisplayName("Known distance between Karachi landmarks (Saddar to AKUH)")
    void testKarachiLandmarksDistance() {
        // Saddar: 24.8607, 67.0011
        // Aga Khan University Hospital: 24.8937, 67.0748
        double dist = HaversineUtil.calculateDistance(24.8607, 67.0011, 24.8937, 67.0748);
        // Approximately 8.2 - 8.4 km
        assertTrue(dist > 8.0 && dist < 8.6, "Distance should be approximately 8.3 km, got: " + dist);
    }

    @Test
    @DisplayName("Equator 1-degree longitude difference (~111.19 km)")
    void testEquatorDistance() {
        double dist = HaversineUtil.calculateDistance(0.0, 0.0, 0.0, 1.0);
        assertEquals(111.19, dist, 0.5);
    }

    @Test
    @DisplayName("Pole to pole distance (~20015 km)")
    void testPoleToPoleDistance() {
        double dist = HaversineUtil.calculateDistance(90.0, 0.0, -90.0, 0.0);
        assertEquals(20015.0, dist, 20.0);
    }

    @Test
    @DisplayName("Boundary valid coordinates (-90, -180) to (90, 180)")
    void testBoundaryCoordinates() {
        assertDoesNotThrow(() -> HaversineUtil.calculateDistance(-90.0, -180.0, 90.0, 180.0));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException for null coordinates")
    void testNullCoordinates() {
        assertThrows(IllegalArgumentException.class, () ->
                HaversineUtil.calculateDistance(null, 67.0, 24.8, 67.0));
        assertThrows(IllegalArgumentException.class, () ->
                HaversineUtil.calculateDistance(24.8, null, 24.8, 67.0));
        assertThrows(IllegalArgumentException.class, () ->
                HaversineUtil.calculateDistance(24.8, 67.0, null, 67.0));
        assertThrows(IllegalArgumentException.class, () ->
                HaversineUtil.calculateDistance(24.8, 67.0, 24.8, null));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException for NaN or Infinite coordinates")
    void testNanAndInfiniteCoordinates() {
        assertThrows(IllegalArgumentException.class, () ->
                HaversineUtil.calculateDistance(Double.NaN, 67.0, 24.8, 67.0));
        assertThrows(IllegalArgumentException.class, () ->
                HaversineUtil.calculateDistance(24.8, Double.POSITIVE_INFINITY, 24.8, 67.0));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException for out-of-range coordinates")
    void testOutOfRangeCoordinates() {
        assertThrows(IllegalArgumentException.class, () ->
                HaversineUtil.calculateDistance(91.0, 67.0, 24.8, 67.0));
        assertThrows(IllegalArgumentException.class, () ->
                HaversineUtil.calculateDistance(-90.1, 67.0, 24.8, 67.0));
        assertThrows(IllegalArgumentException.class, () ->
                HaversineUtil.calculateDistance(24.8, 180.1, 24.8, 67.0));
        assertThrows(IllegalArgumentException.class, () ->
                HaversineUtil.calculateDistance(24.8, -180.1, 24.8, 67.0));
    }
}
