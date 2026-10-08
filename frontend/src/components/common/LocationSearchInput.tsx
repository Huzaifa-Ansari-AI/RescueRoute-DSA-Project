import React, { useState, useEffect, useRef } from "react";
import geocodingService, { type PlaceSuggestion } from "../../services/geocodingService";
import type { Coordinates } from "../../hooks/useGeolocation";

interface LocationSearchInputProps {
  currentCoordinates: Coordinates | null;
  locationName: string | null;
  loadingGps: boolean;
  gpsError: string | null;
  onRequestGps: () => void;
  onSelectLocation: (lat: number, lng: number, placeName: string, source: "gps" | "search" | "manual") => void;
  onClearLocation: () => void;
  emergencyMode: boolean;
  onToggleEmergency: (emergency: boolean) => void;
}

export const LocationSearchInput: React.FC<LocationSearchInputProps> = ({
  currentCoordinates,
  locationName,
  loadingGps,
  gpsError,
  onRequestGps,
  onSelectLocation,
  onClearLocation,
  emergencyMode,
  onToggleEmergency,
}) => {
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState<PlaceSuggestion[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [manualLat, setManualLat] = useState("");
  const [manualLng, setManualLng] = useState("");
  const [manualError, setManualError] = useState<string | null>(null);

  const containerRef = useRef<HTMLDivElement>(null);
  const debounceTimerRef = useRef<number | null>(null);

  // Close suggestions dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Debounced search for places
  const handleQueryChange = (text: string) => {
    setQuery(text);
    setSelectedIndex(-1);

    if (debounceTimerRef.current) {
      window.clearTimeout(debounceTimerRef.current);
    }

    if (!text.trim() || text.trim().length < 2) {
      setSuggestions([]);
      setIsOpen(false);
      setIsSearching(false);
      return;
    }

    setIsSearching(true);
    debounceTimerRef.current = window.setTimeout(async () => {
      try {
        const results = await geocodingService.searchPlaces(text);
        setSuggestions(results);
        setIsOpen(results.length > 0);
      } catch {
        setSuggestions([]);
      } finally {
        setIsSearching(false);
      }
    }, 280);
  };

  const handleSelectSuggestion = (place: PlaceSuggestion) => {
    onSelectLocation(place.lat, place.lng, place.displayName, "search");
    setQuery("");
    setSuggestions([]);
    setIsOpen(false);
  };

  const handleApplyManual = (e: React.FormEvent) => {
    e.preventDefault();
    const lat = parseFloat(manualLat);
    const lng = parseFloat(manualLng);

    if (isNaN(lat) || isNaN(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
      setManualError("Please enter valid coordinates (Lat: -90 to 90, Lng: -180 to 180).");
      return;
    }

    setManualError(null);
    onSelectLocation(lat, lng, `Custom Point (${lat.toFixed(4)}, ${lng.toFixed(4)})`, "manual");
  };

  // Keyboard navigation inside suggestions list
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
      handleSelectSuggestion(suggestions[selectedIndex]);
    } else if (e.key === "Escape") {
      setIsOpen(false);
    }
  };

  const popularAreas = [
    { name: "Nazimabad", lat: 24.9107, lng: 67.0311 },
    { name: "Saddar", lat: 24.8607, lng: 67.0011 },
    { name: "Clifton", lat: 24.8236, lng: 67.0332 },
    { name: "Gulshan", lat: 24.8934, lng: 67.0722 },
    { name: "DHA", lat: 24.8155, lng: 67.0676 },
    { name: "Shershah", lat: 24.8872, lng: 66.9744 },
    { name: "Korangi", lat: 24.8258, lng: 67.1352 },
  ];

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
      {/* Top Header & Emergency Mode Switch */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          flexWrap: "wrap",
          gap: "10px",
          marginBottom: "14px",
        }}
      >
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
            <span style={{ fontSize: "16px" }}>📍</span>
            <span style={{ fontSize: "14px", fontWeight: 700, color: "#0f172a", textTransform: "uppercase", letterSpacing: "0.5px" }}>
              Point A — Your Location
            </span>
          </div>
          <p style={{ margin: "2px 0 0 0", fontSize: "12px", color: "#64748b" }}>
            Search your Karachi area or detect your current GPS location
          </p>
        </div>

        {/* Emergency Mode Toggle */}
        <button
          type="button"
          onClick={() => onToggleEmergency(!emergencyMode)}
          style={{
            display: "flex",
            alignItems: "center",
            gap: "8px",
            padding: "6px 14px",
            borderRadius: "20px",
            border: emergencyMode ? "1px solid #dc2626" : "1px solid #cbd5e1",
            backgroundColor: emergencyMode ? "#fef2f2" : "#f8fafc",
            color: emergencyMode ? "#b91c1c" : "#475569",
            fontWeight: 600,
            fontSize: "12px",
            cursor: "pointer",
            transition: "all 0.15s ease",
          }}
        >
          <span style={{ fontSize: "14px" }}>{emergencyMode ? "🚨" : "🛡️"}</span>
          <span>{emergencyMode ? "Emergency 24/7 Boost: ON" : "Standard Mode"}</span>
        </button>
      </div>

      {/* Selected Location Banner or Place Search Input */}
      {currentCoordinates ? (
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            backgroundColor: "#f0fdf4",
            border: "1px solid #bbf7d0",
            borderRadius: "10px",
            padding: "10px 14px",
            gap: "12px",
            flexWrap: "wrap",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: "8px", flex: 1, minWidth: "220px" }}>
            <div
              style={{
                width: "28px",
                height: "28px",
                borderRadius: "50%",
                backgroundColor: "#22c55e",
                color: "#ffffff",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontSize: "14px",
                flexShrink: 0,
              }}
            >
              ✓
            </div>
            <div>
              <div style={{ fontSize: "13px", fontWeight: 700, color: "#15803d" }}>
                {locationName || "Selected Origin Location"}
              </div>
              <div style={{ fontSize: "11px", color: "#166534" }}>
                Origin set to ({currentCoordinates.lat.toFixed(4)}, {currentCoordinates.lng.toFixed(4)})
              </div>
            </div>
          </div>

          <div style={{ display: "flex", gap: "8px" }}>
            <button
              type="button"
              onClick={onRequestGps}
              disabled={loadingGps}
              title="Refresh GPS location"
              style={{
                backgroundColor: "#ffffff",
                color: "#166534",
                border: "1px solid #bbf7d0",
                padding: "6px 12px",
                borderRadius: "6px",
                fontSize: "12px",
                fontWeight: 600,
                cursor: "pointer",
              }}
            >
              {loadingGps ? "Locating..." : "📍 Re-detect GPS"}
            </button>
            <button
              type="button"
              onClick={onClearLocation}
              title="Clear selected location"
              style={{
                backgroundColor: "#ffffff",
                color: "#b91c1c",
                border: "1px solid #fecaca",
                padding: "6px 12px",
                borderRadius: "6px",
                fontSize: "12px",
                fontWeight: 600,
                cursor: "pointer",
              }}
            >
              Change Location
            </button>
          </div>
        </div>
      ) : (
        <div>
          {/* Search + GPS Button Row */}
          <div style={{ display: "flex", gap: "10px", position: "relative" }}>
            <div style={{ position: "relative", flex: 1 }}>
              <input
                type="text"
                value={query}
                onChange={(e) => handleQueryChange(e.target.value)}
                onKeyDown={handleKeyDown}
                onFocus={() => suggestions.length > 0 && setIsOpen(true)}
                placeholder="🔍 Search your location (e.g. Nazimabad, Clifton, Saddar, Shershah)..."
                style={{
                  width: "100%",
                  padding: "11px 36px 11px 14px",
                  fontSize: "14px",
                  borderRadius: "8px",
                  border: "1px solid #cbd5e1",
                  boxSizing: "border-box",
                }}
              />
              {isSearching && (
                <span
                  style={{
                    position: "absolute",
                    right: "12px",
                    top: "50%",
                    transform: "translateY(-50%)",
                    fontSize: "12px",
                    color: "#94a3b8",
                  }}
                >
                  ⏳
                </span>
              )}
              {query && !isSearching && (
                <button
                  type="button"
                  onClick={() => {
                    setQuery("");
                    setSuggestions([]);
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
                    maxHeight: "260px",
                    overflowY: "auto",
                  }}
                >
                  {suggestions.map((item, idx) => (
                    <li
                      key={item.id}
                      onMouseDown={() => handleSelectSuggestion(item)}
                      style={{
                        padding: "10px 14px",
                        fontSize: "13px",
                        cursor: "pointer",
                        backgroundColor: selectedIndex === idx ? "#e0f2fe" : "transparent",
                        borderBottom: idx === suggestions.length - 1 ? "none" : "1px solid #f1f5f9",
                        display: "flex",
                        alignItems: "center",
                        gap: "10px",
                        color: "#0f172a",
                      }}
                    >
                      <span style={{ fontSize: "16px" }}>📍</span>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 600, color: "#0f172a" }}>{item.name}</div>
                        <div style={{ fontSize: "11px", color: "#64748b" }}>{item.displayName}</div>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            {/* GPS Detection Button */}
            <button
              type="button"
              onClick={onRequestGps}
              disabled={loadingGps}
              style={{
                display: "flex",
                alignItems: "center",
                gap: "8px",
                padding: "0 18px",
                backgroundColor: "#0284c7",
                color: "#ffffff",
                border: "none",
                borderRadius: "8px",
                fontSize: "13px",
                fontWeight: 600,
                cursor: loadingGps ? "not-allowed" : "pointer",
                whiteSpace: "nowrap",
                boxShadow: "0 2px 4px rgba(2, 132, 199, 0.2)",
              }}
            >
              <span>{loadingGps ? "⏳" : "📍"}</span>
              <span>{loadingGps ? "Detecting GPS..." : "Use My Location"}</span>
            </button>
          </div>

          {/* Quick Area Shortcuts */}
          <div style={{ display: "flex", alignItems: "center", gap: "6px", marginTop: "10px", flexWrap: "wrap" }}>
            <span style={{ fontSize: "11px", color: "#64748b", fontWeight: 600 }}>Quick Areas:</span>
            {popularAreas.map((area) => (
              <button
                key={area.name}
                type="button"
                onClick={() => onSelectLocation(area.lat, area.lng, `${area.name}, Karachi`, "search")}
                style={{
                  padding: "4px 10px",
                  borderRadius: "6px",
                  border: "1px solid #e2e8f0",
                  backgroundColor: "#f8fafc",
                  color: "#334155",
                  fontSize: "12px",
                  fontWeight: 500,
                  cursor: "pointer",
                  transition: "all 0.15s ease",
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = "#e0f2fe";
                  e.currentTarget.style.borderColor = "#bae6fd";
                  e.currentTarget.style.color = "#0369a1";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = "#f8fafc";
                  e.currentTarget.style.borderColor = "#e2e8f0";
                  e.currentTarget.style.color = "#334155";
                }}
              >
                {area.name}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* GPS Error Feedback */}
      {gpsError && (
        <div
          style={{
            marginTop: "10px",
            padding: "8px 12px",
            backgroundColor: "#fef2f2",
            border: "1px solid #fecaca",
            borderRadius: "6px",
            color: "#991b1b",
            fontSize: "12px",
          }}
        >
          ⚠️ {gpsError}
        </div>
      )}

      {/* Advanced Manual Coordinates Collapse */}
      <div style={{ marginTop: "12px", borderTop: "1px dashed #e2e8f0", paddingTop: "8px" }}>
        <button
          type="button"
          onClick={() => setShowAdvanced(!showAdvanced)}
          style={{
            background: "none",
            border: "none",
            color: "#64748b",
            fontSize: "11px",
            fontWeight: 600,
            cursor: "pointer",
            padding: 0,
            display: "flex",
            alignItems: "center",
            gap: "4px",
          }}
        >
          <span>{showAdvanced ? "▼" : "▶"}</span>
          <span>Advanced: Enter specific coordinates manually</span>
        </button>

        {showAdvanced && (
          <form onSubmit={handleApplyManual} style={{ marginTop: "8px", display: "flex", gap: "8px", alignItems: "center", flexWrap: "wrap" }}>
            <input
              type="text"
              placeholder="Latitude (-90 to 90)"
              value={manualLat}
              onChange={(e) => setManualLat(e.target.value)}
              style={{ width: "160px", padding: "6px 10px", fontSize: "12px", borderRadius: "6px", border: "1px solid #cbd5e1" }}
            />
            <input
              type="text"
              placeholder="Longitude (-180 to 180)"
              value={manualLng}
              onChange={(e) => setManualLng(e.target.value)}
              style={{ width: "160px", padding: "6px 10px", fontSize: "12px", borderRadius: "6px", border: "1px solid #cbd5e1" }}
            />
            <button
              type="submit"
              style={{
                backgroundColor: "#0284c7",
                color: "#ffffff",
                border: "none",
                borderRadius: "6px",
                padding: "6px 12px",
                fontSize: "12px",
                fontWeight: 600,
                cursor: "pointer",
              }}
            >
              Apply Coordinates
            </button>
            {manualError && (
              <span style={{ fontSize: "11px", color: "#dc2626", width: "100%" }}>{manualError}</span>
            )}
          </form>
        )}
      </div>
    </div>
  );
};

export default LocationSearchInput;
