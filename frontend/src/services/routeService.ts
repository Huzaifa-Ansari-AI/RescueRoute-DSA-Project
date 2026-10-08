import type { LatLng, OsrmRouteResponse, RouteInfo } from "../types/route";

const OSRM_BASE_URL = "https://router.project-osrm.org/route/v1/driving";
const REQUEST_TIMEOUT_MS = 10000;

let currentAbortController: AbortController | null = null;

/**
 * Validates geographic coordinates to ensure they fall within acceptable ranges.
 */
function isValidCoordinate(coord?: LatLng | null): coord is LatLng {
  if (!coord) return false;
  const { lat, lng } = coord;
  return (
    typeof lat === "number" &&
    typeof lng === "number" &&
    !isNaN(lat) &&
    !isNaN(lng) &&
    isFinite(lat) &&
    isFinite(lng) &&
    lat >= -90 &&
    lat <= 90 &&
    lng >= -180 &&
    lng <= 180
  );
}

export const routeService = {
  /**
   * Calculates a road driving route between origin and destination using the public OSRM API.
   *
   * @param origin User's current location { lat, lng }
   * @param destination Facility location { lat, lng }
   * @param destinationFacilityId UUID of the target facility
   * @param destinationFacilityName Display name of the facility
   * @returns RouteInfo with road polyline points [lat, lng], distance in km, and duration in minutes
   */
  async calculateRoute(
    origin: LatLng,
    destination: LatLng,
    destinationFacilityId: string,
    destinationFacilityName: string
  ): Promise<RouteInfo> {
    // 1. Validate coordinates
    if (!isValidCoordinate(origin)) {
      throw new Error("Invalid origin location. Please specify valid coordinates.");
    }
    if (!isValidCoordinate(destination)) {
      throw new Error("Invalid facility location. Target coordinates are missing or out of bounds.");
    }

    // 2. Cancel any pending in-flight route request to prevent race conditions
    if (currentAbortController) {
      currentAbortController.abort();
    }
    currentAbortController = new AbortController();
    const { signal } = currentAbortController;

    // Set a timeout safety timer
    const timeoutId = setTimeout(() => {
      currentAbortController?.abort();
    }, REQUEST_TIMEOUT_MS);

    try {
      // 3. OSRM requires {longitude},{latitude};{longitude},{latitude}
      const coordinatesString = `${origin.lng},${origin.lat};${destination.lng},${destination.lat}`;
      const url = `${OSRM_BASE_URL}/${coordinatesString}?overview=full&geometries=geojson`;

      const response = await fetch(url, {
        method: "GET",
        signal,
        headers: {
          Accept: "application/json",
        },
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        throw new Error(`Routing service returned HTTP status ${response.status}.`);
      }

      const data: OsrmRouteResponse = await response.json();

      // 4. Validate OSRM response status
      if (data.code !== "Ok" || !data.routes || data.routes.length === 0) {
        if (data.code === "NoRoute") {
          throw new Error("No drivable road path could be found between your position and this facility.");
        }
        if (data.code === "InvalidQuery") {
          throw new Error("The routing request query was rejected by OSRM due to invalid points.");
        }
        throw new Error(data.message || "Unable to calculate road route with OSRM service.");
      }

      const bestRoute = data.routes[0];

      if (!bestRoute.geometry || !Array.isArray(bestRoute.geometry.coordinates)) {
        throw new Error("Routing response did not contain valid path geometry.");
      }

      // 5. Convert OSRM GeoJSON [longitude, latitude] to Leaflet [latitude, longitude]
      const leafletCoordinates: [number, number][] = bestRoute.geometry.coordinates.map(
        ([lng, lat]) => [lat, lng] as [number, number]
      );

      // 6. Convert units: meters -> km, seconds -> minutes
      const distanceKm = Math.round((bestRoute.distance / 1000) * 10) / 10;
      const durationMinutes = Math.max(1, Math.round(bestRoute.duration / 60));

      return {
        destinationFacilityId,
        destinationFacilityName,
        distanceKm,
        durationMinutes,
        coordinates: leafletCoordinates,
      };
    } catch (err: unknown) {
      clearTimeout(timeoutId);

      if (err instanceof Error) {
        if (err.name === "AbortError") {
          throw new Error("Route calculation was cancelled or timed out. Please try again.", { cause: err });
        }
        throw err;
      }
      throw new Error("An unexpected error occurred while communicating with OSRM.", { cause: err });
    } finally {
      currentAbortController = null;
    }
  },

  /**
   * Cancels any active in-flight OSRM request.
   */
  cancelPendingRequest(): void {
    if (currentAbortController) {
      currentAbortController.abort();
      currentAbortController = null;
    }
  },
};

export default routeService;
