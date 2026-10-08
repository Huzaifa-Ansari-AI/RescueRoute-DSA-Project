import { useState, useCallback } from "react";

export interface Coordinates {
  lat: number;
  lng: number;
}

export interface GeolocationState {
  coordinates: Coordinates | null;
  loading: boolean;
  error: string | null;
  source: "browser" | "manual" | "preset" | null;
}

export interface PresetLocation {
  name: string;
  lat: number;
  lng: number;
}

export const KARACHI_PRESETS: PresetLocation[] = [
  { name: "Saddar / Civil", lat: 24.8607, lng: 67.0011 },
  { name: "Clifton / South City", lat: 24.8236, lng: 67.0332 },
  { name: "Gulshan / Liaquat", lat: 24.8934, lng: 67.0722 },
  { name: "Korangi / Indus", lat: 24.8258, lng: 67.1352 },
];

export function useGeolocation() {
  const [state, setState] = useState<GeolocationState>({
    coordinates: null,
    loading: false,
    error: null,
    source: null,
  });

  const requestBrowserLocation = useCallback(() => {
    if (!navigator.geolocation) {
      setState((prev) => ({
        ...prev,
        loading: false,
        error: "Geolocation is not supported by your browser.",
      }));
      return;
    }

    setState((prev) => ({ ...prev, loading: true, error: null }));

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setState({
          coordinates: {
            lat: position.coords.latitude,
            lng: position.coords.longitude,
          },
          loading: false,
          error: null,
          source: "browser",
        });
      },
      (geoError) => {
        let message = "Unable to retrieve your location.";
        switch (geoError.code) {
          case geoError.PERMISSION_DENIED:
            message = "Location permission denied. Please enter coordinates manually.";
            break;
          case geoError.POSITION_UNAVAILABLE:
            message = "Location information is currently unavailable.";
            break;
          case geoError.TIMEOUT:
            message = "Location request timed out. Please try again or select a preset.";
            break;
        }
        setState((prev) => ({
          ...prev,
          loading: false,
          error: message,
        }));
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 60000,
      }
    );
  }, []);

  const setManualLocation = useCallback((lat: number, lng: number, source: "manual" | "preset" = "manual") => {
    if (isNaN(lat) || isNaN(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
      setState((prev) => ({
        ...prev,
        error: "Invalid coordinates. Latitude [-90, 90] and Longitude [-180, 180].",
      }));
      return;
    }

    setState({
      coordinates: { lat, lng },
      loading: false,
      error: null,
      source,
    });
  }, []);

  const clearLocation = useCallback(() => {
    setState({
      coordinates: null,
      loading: false,
      error: null,
      source: null,
    });
  }, []);

  return {
    ...state,
    requestBrowserLocation,
    setManualLocation,
    clearLocation,
    presets: KARACHI_PRESETS,
  };
}

export default useGeolocation;
