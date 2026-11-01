import React, { useEffect } from "react";
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  Circle,
  useMap,
  useMapEvents,
} from "react-leaflet";
import L from "leaflet";
import type { ExternalFacility } from "../../types/externalFacility";

interface Props {
  facilities: ExternalFacility[];
  userLocation?: { lat: number; lng: number } | null;
  searchCenter?: { lat: number; lng: number } | null;
  searchRadiusMeters?: number;
  selectedFacilityId?: string | null;
  onSelectFacility?: (facility: ExternalFacility) => void;
  onMapClick?: (lat: number, lng: number) => void;
  height?: string;
}

const defaultCenter: [number, number] = [24.8607, 67.0011];

// Category emoji icon resolver
const getCategoryEmoji = (type: string) => {
  switch (type.toLowerCase()) {
    case "hospital":
      return "🏥";
    case "clinic":
      return "🩺";
    case "pharmacy":
      return "💊";
    case "blood bank":
      return "🩸";
    case "ambulance":
      return "🚑";
    default:
      return "📍";
  }
};

// Create a distinctive cyan/teal external POI marker
const createExternalMarkerIcon = (type: string, isDuplicateCandidate: boolean, isSelected: boolean) => {
  const emoji = getCategoryEmoji(type);
  const bgColor = isSelected ? "#0f766e" : isDuplicateCandidate ? "#d97706" : "#0891b2";
  const borderColor = isSelected ? "#fef08a" : "#ffffff";
  const size = isSelected ? 42 : 36;

  return new L.DivIcon({
    className: "external-facility-marker",
    html: `
      <div style="
        position: relative;
        width: ${size}px;
        height: ${size}px;
        background: ${bgColor};
        border: 2px solid ${borderColor};
        border-radius: 50% 50% 50% 0;
        transform: rotate(-45deg);
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4px 10px rgba(0,0,0,0.35);
        cursor: pointer;
        transition: transform 0.2s ease, background 0.2s ease;
      ">
        <span style="
          transform: rotate(45deg);
          font-size: ${isSelected ? 20 : 16}px;
          line-height: 1;
        ">${emoji}</span>
        ${isDuplicateCandidate ? `
          <div style="
            position: absolute;
            top: -4px;
            right: -4px;
            width: 12px;
            height: 12px;
            background: #ef4444;
            border: 2px solid #fff;
            border-radius: 50%;
            transform: rotate(45deg);
          "></div>
        ` : ""}
      </div>
    `,
    iconSize: [size, size],
    iconAnchor: [size / 2, size],
    popupAnchor: [0, -size],
  });
};

const userIcon = new L.DivIcon({
  className: "custom-user-marker",
  html: `<div style="width: 20px; height: 20px; background-color: #2563eb; border: 3px solid #ffffff; border-radius: 50%; box-shadow: 0 0 0 5px rgba(37, 99, 235, 0.4); animation: pulse 2s infinite;"></div>`,
  iconSize: [20, 20],
  iconAnchor: [10, 10],
});

function MapInteractionController({
  onMapClick,
  searchCenter,
}: {
  onMapClick?: (lat: number, lng: number) => void;
  searchCenter?: { lat: number; lng: number } | null;
}) {
  const map = useMap();

  useMapEvents({
    click(e) {
      if (onMapClick) {
        onMapClick(e.latlng.lat, e.latlng.lng);
      }
    },
  });

  useEffect(() => {
    if (searchCenter) {
      map.flyTo([searchCenter.lat, searchCenter.lng], Math.max(map.getZoom(), 13), {
        duration: 0.8,
      });
    }
  }, [searchCenter, map]);

  return null;
}

export const ExternalMapView: React.FC<Props> = ({
  facilities,
  userLocation,
  searchCenter,
  searchRadiusMeters = 5000,
  selectedFacilityId,
  onSelectFacility,
  onMapClick,
  height = "600px",
}) => {
  const centerLat = searchCenter?.lat ?? userLocation?.lat ?? defaultCenter[0];
  const centerLng = searchCenter?.lng ?? userLocation?.lng ?? defaultCenter[1];

  return (
    <div style={{ position: "relative", width: "100%", height, borderRadius: "12px", overflow: "hidden", border: "1px solid #cbd5e1" }}>
      <MapContainer
        center={[centerLat, centerLng]}
        zoom={13}
        style={{ width: "100%", height: "100%" }}
        scrollWheelZoom={true}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors (ODbL)'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          maxZoom={19}
        />

        <MapInteractionController
          onMapClick={onMapClick}
          searchCenter={searchCenter}
        />

        {/* User Current Location Marker */}
        {userLocation && (
          <Marker position={[userLocation.lat, userLocation.lng]} icon={userIcon}>
            <Popup>
              <div style={{ fontWeight: 600, color: "#1e3a8a" }}>Your Location</div>
            </Popup>
          </Marker>
        )}

        {/* Search Center & Bounded Radius Visualizer */}
        {searchCenter && (
          <Circle
            center={[searchCenter.lat, searchCenter.lng]}
            radius={searchRadiusMeters}
            pathOptions={{
              color: "#0891b2",
              fillColor: "#06b6d4",
              fillOpacity: 0.08,
              dashArray: "4, 6",
              weight: 2,
            }}
          />
        )}

        {/* External Facility Markers */}
        {facilities.map((facility) => {
          if (
            typeof facility.latitude !== "number" ||
            typeof facility.longitude !== "number" ||
            isNaN(facility.latitude) ||
            isNaN(facility.longitude)
          ) {
            return null;
          }

          const isSelected = selectedFacilityId === facility.id;
          const isDuplicateCandidate = facility.importStatus === "DUPLICATE_CANDIDATE";
          const icon = createExternalMarkerIcon(facility.facilityType, isDuplicateCandidate, isSelected);

          return (
            <Marker
              key={facility.id}
              position={[facility.latitude, facility.longitude]}
              icon={icon}
              eventHandlers={{
                click: () => {
                  if (onSelectFacility) {
                    onSelectFacility(facility);
                  }
                },
              }}
            >
              <Popup>
                <div style={{ maxWidth: "280px", fontFamily: "system-ui, sans-serif" }}>
                  {/* Category & Status Badges */}
                  <div style={{ display: "flex", gap: "6px", alignItems: "center", marginBottom: "6px", flexWrap: "wrap" }}>
                    <span style={{
                      backgroundColor: "#ecfeff",
                      color: "#0e7490",
                      border: "1px solid #a5f3fc",
                      borderRadius: "4px",
                      fontSize: "11px",
                      fontWeight: 700,
                      padding: "2px 6px",
                      display: "flex",
                      alignItems: "center",
                      gap: "3px",
                    }}>
                      {getCategoryEmoji(facility.facilityType)} {facility.facilityType}
                    </span>

                    {isDuplicateCandidate ? (
                      <span style={{
                        backgroundColor: "#fef3c7",
                        color: "#92400e",
                        border: "1px solid #fde68a",
                        borderRadius: "4px",
                        fontSize: "10px",
                        fontWeight: 600,
                        padding: "2px 6px",
                      }}>
                        ⚠️ Possible Internal Duplicate
                      </span>
                    ) : (
                      <span style={{
                        backgroundColor: "#f1f5f9",
                        color: "#475569",
                        borderRadius: "4px",
                        fontSize: "10px",
                        fontWeight: 600,
                        padding: "2px 6px",
                      }}>
                        OSM POI
                      </span>
                    )}
                  </div>

                  {/* Facility Name */}
                  <h4 style={{ margin: "0 0 6px 0", fontSize: "14px", fontWeight: 700, color: "#0f172a", lineHeight: 1.3 }}>
                    {facility.name}
                  </h4>

                  {/* Distance */}
                  {facility.distanceKm !== undefined && (
                    <div style={{ fontSize: "12px", color: "#0891b2", fontWeight: 600, marginBottom: "6px" }}>
                      📏 {facility.distanceKm.toFixed(2)} km away
                    </div>
                  )}

                  {/* Address */}
                  {facility.address && (
                    <div style={{ fontSize: "11px", color: "#64748b", marginBottom: "4px" }}>
                      📍 {facility.address}
                    </div>
                  )}

                  {/* Phone */}
                  {facility.phoneNumber && (
                    <div style={{ fontSize: "11px", color: "#166534", marginBottom: "4px" }}>
                      📞 {facility.phoneNumber}
                    </div>
                  )}

                  {/* Opening Hours */}
                  {facility.openingHours && (
                    <div style={{ fontSize: "11px", color: "#1e293b", marginBottom: "4px" }}>
                      🕒 {facility.openingHours}
                    </div>
                  )}

                  {/* Matched Internal Facility Reference */}
                  {facility.matchedFacilityName && (
                    <div style={{
                      backgroundColor: "#fffbeb",
                      border: "1px solid #fef08a",
                      borderRadius: "6px",
                      padding: "6px 8px",
                      fontSize: "11px",
                      color: "#854d0e",
                      marginTop: "6px",
                      marginBottom: "6px",
                    }}>
                      <strong>Matched internal facility:</strong> {facility.matchedFacilityName}
                    </div>
                  )}

                  {/* Mandatory Unverified Clinical Notice */}
                  <div style={{
                    backgroundColor: "#fef2f2",
                    border: "1px solid #fecaca",
                    borderRadius: "6px",
                    padding: "6px 8px",
                    fontSize: "10px",
                    color: "#991b1b",
                    marginTop: "8px",
                    lineHeight: 1.3,
                  }}>
                    ⚠️ <strong>Unverified Listing:</strong> {facility.disclaimer}
                  </div>

                  {/* OpenStreetMap Attribution & Canonical Link */}
                  <div style={{
                    marginTop: "8px",
                    paddingTop: "6px",
                    borderTop: "1px solid #e2e8f0",
                    fontSize: "10px",
                    color: "#64748b",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}>
                    <span>{facility.attribution}</span>
                    {facility.sourceUrl && (
                      <a
                        href={facility.sourceUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        style={{ color: "#0284c7", textDecoration: "none", fontWeight: 600 }}
                      >
                        View on OSM ↗
                      </a>
                    )}
                  </div>
                </div>
              </Popup>
            </Marker>
          );
        })}
      </MapContainer>
    </div>
  );
};
