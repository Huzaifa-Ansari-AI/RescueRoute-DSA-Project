package com.huzaifa.rescueroute.util;

/**
 * Utility class implementing the Haversine formula for calculating
 * great-circle distances between points on the Earth's surface.
 *
 * <p>Formula:
 * a = sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlon/2)
 * c = 2 * atan2(√a, √(1-a))
 * d = R * c
 *
 * where R is Earth's mean spherical radius (6371.0 km).
 */
public final class HaversineUtil {

    /**
     * Mean radius of Earth in kilometers.
     */
    public static final double EARTH_RADIUS_KM = 6371.0;

    private HaversineUtil() {
        // Prevent instantiation of utility class
    }

    /**
     * Calculates great-circle distance between two coordinate pairs in kilometers.
     *
     * @param lat1 Source latitude in degrees [-90.0, 90.0]
     * @param lon1 Source longitude in degrees [-180.0, 180.0]
     * @param lat2 Destination latitude in degrees [-90.0, 90.0]
     * @param lon2 Destination longitude in degrees [-180.0, 180.0]
     * @return Distance in kilometers (>= 0.0)
     * @throws IllegalArgumentException if coordinates are null, NaN, infinite, or out of geographic range
     */
    public static double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        validateCoordinate(lat1, lon1, "Source");
        validateCoordinate(lat2, lon2, "Destination");

        // Fast path for identical coordinates
        if (lat1.equals(lat2) && lon1.equals(lon2)) {
            return 0.0;
        }

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);

        double sinDLat2 = Math.sin(dLat / 2.0);
        double sinDLon2 = Math.sin(dLon / 2.0);

        double a = (sinDLat2 * sinDLat2)
                + Math.cos(radLat1) * Math.cos(radLat2) * (sinDLon2 * sinDLon2);

        // Guard against floating-point inaccuracies pushing 'a' outside [0.0, 1.0]
        a = Math.min(1.0, Math.max(0.0, a));

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Validates that latitude and longitude are non-null, finite, and within valid Earth boundaries.
     */
    public static void validateCoordinate(Double lat, Double lon, String label) {
        if (lat == null || lon == null) {
            throw new IllegalArgumentException(label + " coordinates cannot be null.");
        }
        if (Double.isNaN(lat) || Double.isInfinite(lat) || Double.isNaN(lon) || Double.isInfinite(lon)) {
            throw new IllegalArgumentException(label + " coordinates must be valid finite numbers.");
        }
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException(
                    label + " latitude must be between -90.0 and 90.0 degrees. Provided: " + lat);
        }
        if (lon < -180.0 || lon > 180.0) {
            throw new IllegalArgumentException(
                    label + " longitude must be between -180.0 and 180.0 degrees. Provided: " + lon);
        }
    }
}
