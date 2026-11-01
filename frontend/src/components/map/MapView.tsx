import { useEffect } from "react";
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  Polyline,
  Circle,
  useMap,
} from "react-leaflet";

import L from "leaflet";

import type { Facility } from "../../types/facility";
import type { RouteInfo } from "../../types/route";

type Props = {
  facilities: Facility[];
  userLocation?: { lat: number; lng: number } | null;
  radiusKm?: number;
  selectedFacilityId?: string | null;
  route?: RouteInfo | null;
  height?: string;
  isSearching?: boolean;
};

const defaultCenter: [number, number] = [24.8607, 67.0011];

// Category styling and emoji resolver matching OSM Discovery
const getFacilityMarkerConfig = (type: string, isDestination: boolean = false) => {
  const norm = (type || "").toLowerCase();
  let emoji = "📍";
  let bgColor = "#2563eb";
  let label = type || "Medical Facility";

  if (norm.includes("hospital")) {
    emoji = "🏥";
    bgColor = isDestination ? "#be123c" : "#e11d48";
    label = "Hospital";
  } else if (norm.includes("pharmacy")) {
    emoji = "💊";
    bgColor = isDestination ? "#15803d" : "#16a34a";
    label = "Pharmacy";
  } else if (norm.includes("clinic")) {
    emoji = "🩺";
    bgColor = isDestination ? "#0e7490" : "#0891b2";
    label = "Clinic";
  } else if (norm.includes("blood")) {
    emoji = "🩸";
    bgColor = isDestination ? "#7f1d1d" : "#991b1b";
    label = "Blood Bank";
  } else if (norm.includes("ambulance")) {
    emoji = "🚑";
    bgColor = isDestination ? "#b45309" : "#d97706";
    label = "Ambulance";
  }

  return { emoji, bgColor, label };
};

// Create precise pinpoint needle marker pointing directly at exact coordinates
const createFacilityPinIcon = (type: string, isDestination: boolean = false) => {
  const { emoji, bgColor } = getFacilityMarkerConfig(type, isDestination);
  const size = isDestination ? 44 : 36;
  const borderColor = isDestination ? "#facc15" : "#ffffff";
  const borderWidth = isDestination ? 3 : 2;

  return new L.DivIcon({
    className: "custom-facility-pin",
    html: `
      <div style="
        position: relative;
        width: ${size}px;
        height: ${size}px;
        background: ${bgColor};
        border: ${borderWidth}px solid ${borderColor};
        border-radius: 50% 50% 50% 0;
        transform: rotate(-45deg);
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: ${
          isDestination
            ? "0 0 0 5px rgba(250, 204, 21, 0.45), 0 8px 16px rgba(0,0,0,0.4)"
            : "0 3px 8px rgba(0,0,0,0.3)"
        };
        cursor: pointer;
        transition: transform 0.2s ease, box-shadow 0.2s ease;
      ">
        <span style="
          transform: rotate(45deg);
          font-size: ${isDestination ? 20 : 16}px;
          line-height: 1;
          display: inline-block;
          user-select: none;
        ">${emoji}</span>
        ${
          isDestination
            ? `
          <div style="
            position: absolute;
            top: -4px;
            right: -4px;
            width: 12px;
            height: 12px;
            background: #22c55e;
            border: 2px solid #ffffff;
            border-radius: 50%;
            transform: rotate(45deg);
          "></div>
        `
            : ""
        }
      </div>
    `,
    iconSize: [size, size],
    iconAnchor: [size / 2, size],
    popupAnchor: [0, -size],
  });
};

// User Origin Point A pinpoint needle icon
const userIcon = new L.DivIcon({
  className: "custom-user-marker",
  html: `
    <div style="
      position: relative;
      width: 36px;
      height: 36px;
      background: #2563eb;
      border: 3px solid #ffffff;
      border-radius: 50% 50% 50% 0;
      transform: rotate(-45deg);
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 0 0 5px rgba(37, 99, 235, 0.4), 0 4px 12px rgba(0,0,0,0.3);
      cursor: pointer;
    ">
      <span style="
        transform: rotate(45deg);
        color: #ffffff;
        font-weight: 800;
        font-size: 14px;
        line-height: 1;
        user-select: none;
      ">A</span>
    </div>
  `,
  iconSize: [36, 36],
  iconAnchor: [18, 36],
  popupAnchor: [0, -36],
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
  radiusKm,
  selectedFacilityId,
  route,
  height = "600px",
  isSearching = false,
}: Props) {
  const selectedFacility = facilities.find((f) => f.id === selectedFacilityId);
  const targetCenter: [number, number] | null = selectedFacility
    ? [selectedFacility.latitude, selectedFacility.longitude]
    : null;

  const initialCenter: [number, number] = userLocation
    ? [userLocation.lat, userLocation.lng]
    : defaultCenter;

  return (
    <div style={{ position: "relative", width: "100%", height }}>
      {/* Inline Animation Styles for Radar and Spinner */}
      <style>{`
        @keyframes rrSpin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
        @keyframes rrPulseGlow {
          0% { box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.5); }
          70% { box-shadow: 0 0 0 14px rgba(37, 99, 235, 0); }
          100% { box-shadow: 0 0 0 0 rgba(37, 99, 235, 0); }
        }
      `}</style>

      {/* Floating Searching Indicator Banner on Map */}
      {isSearching && (
        <div
          style={{
            position: "absolute",
            top: "16px",
            left: "50%",
            transform: "translateX(-50%)",
            zIndex: 1000,
            backgroundColor: "rgba(15, 23, 42, 0.92)",
            color: "#ffffff",
            padding: "8px 20px",
            borderRadius: "30px",
            fontSize: "13px",
            fontWeight: 600,
            display: "flex",
            alignItems: "center",
            gap: "10px",
            boxShadow: "0 6px 20px rgba(0, 0, 0, 0.35)",
            backdropFilter: "blur(6px)",
            border: "1px solid rgba(255, 255, 255, 0.2)",
            pointerEvents: "none",
          }}
        >
          <div
            style={{
              width: "14px",
              height: "14px",
              border: "2px solid #38bdf8",
              borderTopColor: "transparent",
              borderRadius: "50%",
              animation: "rrSpin 0.8s linear infinite",
            }}
          />
          <span>Scanning {radiusKm || 5} km radius for medical facilities...</span>
        </div>
      )}

      <MapContainer
        center={initialCenter}
        zoom={13}
        style={{ height: "100%", width: "100%", borderRadius: "12px", overflow: "hidden" }}
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

        {/* Render Coverage Radius Circle with Searching Animation */}
        {userLocation && radiusKm && radiusKm > 0 && (
          <>
            <Circle
              center={[userLocation.lat, userLocation.lng]}
              radius={radiusKm * 1000}
              pathOptions={{
                color: isSearching ? "#2563eb" : "#0284c7",
                fillColor: isSearching ? "#60a5fa" : "#38bdf8",
                fillOpacity: isSearching ? 0.18 : 0.08,
                weight: isSearching ? 3 : 2,
                dashArray: isSearching ? "8, 8" : "6, 6",
              }}
            />
            {/* Secondary pulsing inner radar ring during active search */}
            {isSearching && (
              <Circle
                center={[userLocation.lat, userLocation.lng]}
                radius={radiusKm * 500}
                pathOptions={{
                  color: "#3b82f6",
                  fillColor: "#93c5fd",
                  fillOpacity: 0.12,
                  weight: 2,
                  dashArray: "4, 6",
                }}
              />
            )}
          </>
        )}

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

        {/* User Origin Marker (Point A) */}
        {userLocation && (
          <Marker position={[userLocation.lat, userLocation.lng]} icon={userIcon}>
            <Popup>
              <div style={{ textAlign: "center", padding: "4px" }}>
                <strong style={{ color: "#1e40af" }}>🎯 Point A — Your Origin</strong>
                <div style={{ fontSize: "11px", color: "#64748b", marginTop: "2px" }}>
                  {userLocation.lat.toFixed(4)}, {userLocation.lng.toFixed(4)}
                </div>
                {radiusKm && radiusKm > 0 && (
                  <div
                    style={{
                      fontSize: "11px",
                      color: "#0369a1",
                      fontWeight: 700,
                      marginTop: "4px",
                      backgroundColor: "#e0f2fe",
                      padding: "2px 8px",
                      borderRadius: "6px",
                      display: "inline-block",
                    }}
                  >
                    Coverage Radius: {radiusKm} km
                  </div>
                )}
              </div>
            </Popup>
          </Marker>
        )}

        {/* Facility Markers with Exact Needle Pinpoint and Category Icons */}
        {facilities.map((facility) => {
          const isDestination = route?.destinationFacilityId === facility.id;
          const markerIcon = createFacilityPinIcon(facility.type, isDestination);
          const { emoji, bgColor, label } = getFacilityMarkerConfig(facility.type, isDestination);

          return (
            <Marker
              key={facility.id}
              position={[facility.latitude, facility.longitude]}
              icon={markerIcon}
            >
              <Popup>
                <div style={{ minWidth: "190px" }}>
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
                      padding: "2px 8px",
                      borderRadius: "4px",
                      fontSize: "11px",
                      fontWeight: 700,
                      backgroundColor: bgColor,
                      color: "#ffffff",
                      marginBottom: "6px",
                    }}
                  >
                    {emoji} {label}
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
                        marginTop: "8px",
                        paddingTop: "6px",
                        borderTop: "1px dashed #cbd5e1",
                        fontSize: "12px",
                        color: "#166534",
                        fontWeight: 700,
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
    </div>
  );
}