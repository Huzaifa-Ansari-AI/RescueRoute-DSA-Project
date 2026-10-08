import { useEffect } from "react";
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  Polyline,
  useMap,
} from "react-leaflet";

import L from "leaflet";

import type { Facility } from "../../types/facility";
import type { RouteInfo } from "../../types/route";

type Props = {
  facilities: Facility[];
  userLocation?: { lat: number; lng: number } | null;
  selectedFacilityId?: string | null;
  route?: RouteInfo | null;
  height?: string;
};

const defaultCenter: [number, number] = [24.8607, 67.0011];

const createIcon = (icon: string) =>
  new L.Icon({
    iconUrl: icon,
    iconSize: [35, 35],
    iconAnchor: [17, 35],
    popupAnchor: [0, -35],
  });

const icons = {
  Hospital: createIcon("/icons/hospital.png"),
  "Blood Bank": createIcon("/icons/blood-bank.png"),
  Pharmacy: createIcon("/icons/pharmacy.png"),
  Ambulance: createIcon("/icons/ambulance.png"),
};

const userIcon = new L.DivIcon({
  className: "custom-user-marker",
  html: `<div style="width: 20px; height: 20px; background-color: #2563eb; border: 3px solid #ffffff; border-radius: 50%; box-shadow: 0 0 0 5px rgba(37, 99, 235, 0.4); animation: pulse 2s infinite;"></div>`,
  iconSize: [20, 20],
  iconAnchor: [10, 10],
});

const destinationHighlightIcon = (iconUrl: string) =>
  new L.DivIcon({
    className: "destination-highlight-marker",
    html: `
      <div style="position: relative; width: 44px; height: 44px; display: flex; align-items: center; justify-content: center;">
        <div style="position: absolute; width: 44px; height: 44px; border-radius: 50%; background-color: rgba(22, 163, 74, 0.25); border: 2px solid #16a34a; animation: pulse 1.5s infinite;"></div>
        <img src="${iconUrl}" style="width: 32px; height: 32px; position: relative; z-index: 2;" alt="Destination" />
      </div>
    `,
    iconSize: [44, 44],
    iconAnchor: [22, 22],
    popupAnchor: [0, -22],
  });

function MapViewController({
  targetCenter,
  userLocation,
  route,
}: {
  targetCenter?: [number, number] | null;
  userLocation?: { lat: number; lng: number } | null;
  route?: RouteInfo | null;
}) {
  const map = useMap();

  useEffect(() => {
    if (route && route.coordinates && route.coordinates.length > 0) {
      // Fit map view to encompass the entire road route, origin, and destination
      const bounds = L.latLngBounds(route.coordinates);
      map.fitBounds(bounds, { padding: [50, 50], maxZoom: 16, duration: 1 });
    } else if (targetCenter) {
      map.flyTo(targetCenter, 14, { duration: 0.8 });
    } else if (userLocation) {
      map.flyTo([userLocation.lat, userLocation.lng], 13, { duration: 0.8 });
    }
  }, [targetCenter, userLocation, route, map]);

  return null;
}

export default function MapView({
  facilities,
  userLocation,
  selectedFacilityId,
  route,
  height = "600px",
}: Props) {
  const selectedFacility = facilities.find((f) => f.id === selectedFacilityId);
  const targetCenter: [number, number] | null = selectedFacility
    ? [selectedFacility.latitude, selectedFacility.longitude]
    : null;

  const initialCenter: [number, number] = userLocation
    ? [userLocation.lat, userLocation.lng]
    : defaultCenter;

  return (
    <MapContainer
      center={initialCenter}
      zoom={13}
      style={{ height, width: "100%", borderRadius: "12px", overflow: "hidden" }}
    >
      <TileLayer
        attribution="&copy; OpenStreetMap contributors"
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      <MapViewController
        targetCenter={targetCenter}
        userLocation={userLocation}
        route={route}
      />

      {/* Render OSRM Driving Polyline */}
      {route && route.coordinates && route.coordinates.length > 0 && (
        <Polyline
          positions={route.coordinates}
          pathOptions={{
            color: "#2563eb",
            weight: 6,
            opacity: 0.85,
            lineCap: "round",
            lineJoin: "round",
          }}
        />
      )}

      {/* User Origin Marker */}
      {userLocation && (
        <Marker position={[userLocation.lat, userLocation.lng]} icon={userIcon}>
          <Popup>
            <div style={{ textAlign: "center", padding: "4px" }}>
              <strong style={{ color: "#1e40af" }}>🎯 Your Location (Origin)</strong>
              <div style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>
                {userLocation.lat.toFixed(4)}, {userLocation.lng.toFixed(4)}
              </div>
            </div>
          </Popup>
        </Marker>
      )}

      {/* Facility Markers */}
      {facilities.map((facility) => {
        const isDestination = route?.destinationFacilityId === facility.id;
        const baseIconUrl =
          facility.type === "Hospital"
            ? "/icons/hospital.png"
            : facility.type === "Blood Bank"
            ? "/icons/blood-bank.png"
            : facility.type === "Pharmacy"
            ? "/icons/pharmacy.png"
            : "/icons/ambulance.png";

        const markerIcon = isDestination
          ? destinationHighlightIcon(baseIconUrl)
          : icons[facility.type] || icons.Hospital;

        return (
          <Marker
            key={facility.id}
            position={[facility.latitude, facility.longitude]}
            icon={markerIcon}
          >
            <Popup>
              <div style={{ minWidth: "180px" }}>
                {isDestination && (
                  <div
                    style={{
                      backgroundColor: "#dcfce7",
                      color: "#15803d",
                      padding: "2px 8px",
                      borderRadius: "4px",
                      fontSize: "11px",
                      fontWeight: 700,
                      marginBottom: "6px",
                      display: "inline-block",
                    }}
                  >
                    🎯 Active Route Destination
                  </div>
                )}
                <h4 style={{ margin: "0 0 4px 0", fontSize: "14px", color: "#0f172a" }}>
                  {facility.name}
                </h4>
                <span
                  style={{
                    display: "inline-block",
                    padding: "2px 6px",
                    borderRadius: "4px",
                    fontSize: "11px",
                    fontWeight: 600,
                    backgroundColor: "#eff6ff",
                    color: "#1d4ed8",
                    marginBottom: "6px",
                  }}
                >
                  {facility.type}
                </span>
                <p style={{ margin: "0 0 4px 0", fontSize: "12px", color: "#475569" }}>
                  📍 {facility.address}
                  {facility.city ? `, ${facility.city}` : ""}
                </p>
                {facility.phoneNumber && (
                  <p style={{ margin: 0, fontSize: "12px" }}>
                    <a
                      href={`tel:${facility.phoneNumber}`}
                      style={{ color: "#16a34a", fontWeight: 600, textDecoration: "none" }}
                    >
                      📞 {facility.phoneNumber}
                    </a>
                  </p>
                )}
                {isDestination && route && (
                  <div
                    style={{
                      marginTop: "6px",
                      paddingTop: "6px",
                      borderTop: "1px dashed #cbd5e1",
                      fontSize: "11px",
                      color: "#166534",
                      fontWeight: 600,
                    }}
                  >
                    🚗 {route.distanceKm} km · ⏱️ {route.durationMinutes} min drive
                  </div>
                )}
              </div>
            </Popup>
          </Marker>
        );
      })}
    </MapContainer>
  );
}