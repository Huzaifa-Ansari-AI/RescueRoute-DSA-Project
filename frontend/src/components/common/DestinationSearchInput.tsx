import React, { useState, useEffect, useRef, useMemo } from "react";
import type { Facility } from "../../types/facility";
import type { Coordinates } from "../../hooks/useGeolocation";

interface DestinationSearchInputProps {
  facilities: Facility[];
  selectedFacility: Facility | null;
  originCoordinates: Coordinates | null;
  onSelectDestination: (facility: Facility) => void;
  onClearDestination: () => void;
}

/**
 * Calculates straight-line distance in km between two lat/lng coordinates (Haversine formula)
 */
function calculateDistanceKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const R = 6371; // km
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Math.round(R * c * 10) / 10;
}

const getCategoryIcon = (type: string) => {
  switch (type) {
    case "Hospital":
      return "🏥";
    case "Blood Bank":
      return "🩸";
    case "Pharmacy":
      return "💊";
    case "Ambulance":
      return "🚑";
    default:
      return "🏥";
  }
};

export const DestinationSearchInput: React.FC<DestinationSearchInputProps> = ({
  facilities,
  selectedFacility,
  originCoordinates,
  onSelectDestination,
  onClearDestination,
}) => {
  const [query, setQuery] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);

  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Filter facilities based on user query
  const suggestions = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return [];

    return facilities
      .filter((f) => {
        return (
          f.name.toLowerCase().includes(q) ||
          f.type.toLowerCase().includes(q) ||
          f.address.toLowerCase().includes(q) ||
          (f.city ? f.city.toLowerCase().includes(q) : false)
        );
      })
      .slice(0, 6);
  }, [facilities, query]);

  const handleSelect = (facility: Facility) => {
    onSelectDestination(facility);
    setQuery("");
    setIsOpen(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!isOpen || suggestions.length === 0) return;

    if (e.key === "ArrowDown") {
      e.preventDefault();
      setSelectedIndex((prev) => (prev < suggestions.length - 1 ? prev + 1 : 0));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setSelectedIndex((prev) => (prev > 0 ? prev - 1 : suggestions.length - 1));
    } else if (e.key === "Enter" && selectedIndex >= 0) {
      e.preventDefault();
      handleSelect(suggestions[selectedIndex]);
    } else if (e.key === "Escape") {
      setIsOpen(false);
    }
  };

  return (
    <div
      ref={containerRef}
      style={{
        backgroundColor: "#ffffff",
        borderRadius: "14px",
        border: "1px solid #e2e8f0",
        padding: "16px 18px",
        boxShadow: "0 4px 12px rgba(15, 23, 42, 0.05)",
        marginBottom: "16px",
      }}
    >
      {/* Header */}
      <div style={{ marginBottom: "12px" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
          <span style={{ fontSize: "16px" }}>🎯</span>
          <span style={{ fontSize: "14px", fontWeight: 700, color: "#0f172a", textTransform: "uppercase", letterSpacing: "0.5px" }}>
            Point B — Choose Destination
          </span>
        </div>
        <p style={{ margin: "2px 0 0 0", fontSize: "12px", color: "#64748b" }}>
          Search hospital, blood bank, pharmacy, or ambulance facility
        </p>
      </div>

      {selectedFacility ? (
        /* Selected Destination Card */
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            backgroundColor: "#eff6ff",
            border: "1px solid #bfdbfe",
            borderRadius: "10px",
            padding: "10px 14px",
            gap: "12px",
            flexWrap: "wrap",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: "10px", flex: 1, minWidth: "220px" }}>
            <span style={{ fontSize: "24px" }}>{getCategoryIcon(selectedFacility.type)}</span>
            <div>
              <div style={{ display: "flex", alignItems: "center", gap: "6px", flexWrap: "wrap" }}>
                <span style={{ fontSize: "14px", fontWeight: 700, color: "#1e40af" }}>
                  {selectedFacility.name}
                </span>
                {selectedFacility.isVerified && (
                  <span
                    style={{
                      fontSize: "11px",
                      backgroundColor: "#dcfce7",
                      color: "#166534",
                      padding: "2px 6px",
                      borderRadius: "4px",
                      fontWeight: 600,
                    }}
                  >
                    ✓ Verified
                  </span>
                )}
              </div>
              <div style={{ fontSize: "12px", color: "#3b82f6" }}>
                {selectedFacility.address}
                {originCoordinates && (
                  <span style={{ fontWeight: 600, marginLeft: "6px", color: "#1d4ed8" }}>
                    • {calculateDistanceKm(originCoordinates.lat, originCoordinates.lng, selectedFacility.latitude, selectedFacility.longitude)} km from your location
                  </span>
                )}
              </div>
            </div>
          </div>

          <button
            type="button"
            onClick={onClearDestination}
            style={{
              backgroundColor: "#ffffff",
              color: "#1e40af",
              border: "1px solid #bfdbfe",
              padding: "6px 12px",
              borderRadius: "6px",
              fontSize: "12px",
              fontWeight: 600,
              cursor: "pointer",
            }}
          >
            Change Destination
          </button>
        </div>
      ) : (
        /* Destination Autocomplete Input */
        <div style={{ position: "relative" }}>
          <input
            type="text"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setIsOpen(true);
              setSelectedIndex(-1);
            }}
            onKeyDown={handleKeyDown}
            onFocus={() => query.trim().length > 0 && setIsOpen(true)}
            placeholder="🔍 Search hospital, clinic, pharmacy (e.g. Aga Khan, Indus, Husaini)..."
            style={{
              width: "100%",
              padding: "11px 36px 11px 14px",
              fontSize: "14px",
              borderRadius: "8px",
              border: "1px solid #cbd5e1",
              boxSizing: "border-box",
            }}
          />
          {query && (
            <button
              type="button"
              onClick={() => {
                setQuery("");
                setIsOpen(false);
              }}
              style={{
                position: "absolute",
                right: "10px",
                top: "50%",
                transform: "translateY(-50%)",
                background: "none",
                border: "none",
                color: "#94a3b8",
                fontSize: "16px",
                cursor: "pointer",
              }}
            >
              ×
            </button>
          )}

          {/* Suggestions Dropdown */}
          {isOpen && suggestions.length > 0 && (
            <ul
              style={{
                position: "absolute",
                top: "calc(100% + 4px)",
                left: 0,
                right: 0,
                backgroundColor: "#ffffff",
                border: "1px solid #cbd5e1",
                borderRadius: "8px",
                boxShadow: "0 10px 25px rgba(0, 0, 0, 0.1)",
                listStyle: "none",
                margin: 0,
                padding: "4px 0",
                zIndex: 2000,
                maxHeight: "280px",
                overflowY: "auto",
              }}
            >
              {suggestions.map((item, idx) => {
                const distKm = originCoordinates
                  ? calculateDistanceKm(originCoordinates.lat, originCoordinates.lng, item.latitude, item.longitude)
                  : null;

                const isEmergencyReady =
                  item.type === "Ambulance" ||
                  item.hospitalDetails?.emergencyAvailable ||
                  item.pharmacyDetails?.emergencyMedicinesAvailable;

                return (
                  <li
                    key={item.id}
                    onMouseDown={() => handleSelect(item)}
                    style={{
                      padding: "10px 14px",
                      fontSize: "13px",
                      cursor: "pointer",
                      backgroundColor: selectedIndex === idx ? "#eff6ff" : "transparent",
                      borderBottom: idx === suggestions.length - 1 ? "none" : "1px solid #f1f5f9",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      gap: "10px",
                    }}
                  >
                    <div style={{ display: "flex", alignItems: "center", gap: "10px", flex: 1 }}>
                      <span style={{ fontSize: "18px" }}>{getCategoryIcon(item.type)}</span>
                      <div>
                        <div style={{ fontWeight: 600, color: "#0f172a", display: "flex", alignItems: "center", gap: "6px" }}>
                          <span>{item.name}</span>
                          <span style={{ fontSize: "11px", color: "#64748b", fontWeight: 400 }}>({item.type})</span>
                          {item.isVerified && (
                            <span style={{ fontSize: "10px", color: "#16a34a", fontWeight: 600 }}>✓ Verified</span>
                          )}
                        </div>
                        <div style={{ fontSize: "11px", color: "#64748b" }}>{item.address}</div>
                      </div>
                    </div>

                    <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                      {isEmergencyReady && (
                        <span
                          style={{
                            fontSize: "11px",
                            backgroundColor: "#fef2f2",
                            color: "#b91c1c",
                            padding: "2px 6px",
                            borderRadius: "4px",
                            fontWeight: 600,
                          }}
                        >
                          🚨 Emergency Ready
                        </span>
                      )}
                      {distKm !== null && (
                        <span
                          style={{
                            fontSize: "11px",
                            fontWeight: 700,
                            color: "#0369a1",
                            backgroundColor: "#e0f2fe",
                            padding: "2px 6px",
                            borderRadius: "4px",
                            whiteSpace: "nowrap",
                          }}
                        >
                          {distKm} km
                        </span>
                      )}
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      )}
    </div>
  );
};

export default DestinationSearchInput;
