/**
 * Types and interfaces for OSRM Road Routing and Map Visualization.
 */

export interface LatLng {
  lat: number;
  lng: number;
}

export interface RouteInfo {
  destinationFacilityId: string;
  destinationFacilityName: string;
  distanceKm: number;
  durationMinutes: number;
  coordinates: [number, number][]; // Array of [lat, lng] pairs for Leaflet Polyline
}

export interface RouteState {
  data: RouteInfo | null;
  loading: boolean;
  error: string | null;
}

/**
 * Raw OSRM API Response structures
 */
export interface OsrmWaypoint {
  hint: string;
  distance: number;
  name: string;
  location: [number, number]; // [lng, lat]
}

export interface OsrmRouteGeometry {
  coordinates: [number, number][]; // Array of [lng, lat]
  type: string;
}

export interface OsrmRouteLeg {
  distance: number;
  duration: number;
  summary: string;
  weight: number;
  steps: unknown[];
}

export interface OsrmRouteItem {
  distance: number; // in meters
  duration: number; // in seconds
  weight: number;
  weight_name: string;
  geometry: OsrmRouteGeometry;
  legs: OsrmRouteLeg[];
}

export interface OsrmRouteResponse {
  code: string;
  message?: string;
  routes: OsrmRouteItem[];
  waypoints: OsrmWaypoint[];
}
