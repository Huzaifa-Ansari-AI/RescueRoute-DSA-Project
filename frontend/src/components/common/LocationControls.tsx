import React, { useState } from "react";
import type { Coordinates, PresetLocation } from "../../hooks/useGeolocation";

interface LocationControlsProps {
  coordinates: Coordinates | null;
  loading: boolean;
  error: string | null;
  source: "browser" | "manual" | "preset" | null;
  onRequestBrowserLocation: () => void;
  onSetLocation: (lat: number, lng: number, source?: "manual" | "preset") => void;
  onClearLocation: () => void;
  emergencyMode: boolean;
  onToggleEmergency: (emergency: boolean) => void;
  presets: PresetLocation[];
}

export const LocationControls: React.FC<LocationControlsProps> = ({
  coordinates,
  loading,
  error,
  source,
  onRequestBrowserLocation,
  onSetLocation,
  onClearLocation,
  emergencyMode,
  onToggleEmergency,
  presets,
}) => {
  const [manualLat, setManualLat] = useState("");
  const [manualLng, setManualLng] = useState("");
  const [showManualInputs, setShowManualInputs] = useState(false);
  const [manualError, setManualError] = useState<string | null>(null);

  const handleApplyManual = (e: React.FormEvent) => {
    e.preventDefault();
    const lat = parseFloat(manualLat);
    const lng = parseFloat(manualLng);

    if (isNaN(lat) || isNaN(lng)) {
      setManualError("Please enter valid numeric latitude and longitude values.");
      return;
    }
    if (lat < -90 || lat > 90) {
      setManualError("Latitude must be between -90 and 90 degrees.");
      return;
    }
    if (lng < -180 || lng > 180) {
      setManualError("Longitude must be between -180 and 180 degrees.");
      return;
    }

    setManualError(null);
    onSetLocation(lat, lng, "manual");
  };

  const handleSelectPreset = (preset: PresetLocation) => {
    setManualLat(preset.lat.toString());
    setManualLng(preset.lng.toString());
    setManualError(null);
    onSetLocation(preset.lat, preset.lng, "preset");
  };

  return (
    <div
      style={{
        backgroundColor: "#ffffff",
        borderRadius: "12px",
        border: "1px solid #e2e8f0",
        padding: "16px",
        marginBottom: "20px",
        boxShadow: "0 2px 8px rgba(0, 0, 0, 0.04)",
      }}
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          flexWrap: "wrap",
          gap: "12px",
          marginBottom: "12px",
        }}
      >
        <div>
          <h3 style={{ margin: "0 0 4px 0", fontSize: "16px", color: "#0f172a" }}>
            📍 Origin & Priority Ranking Controls
          </h3>
          <p style={{ margin: 0, fontSize: "13px", color: "#64748b" }}>
            Provide your location to calculate real-time distances and rank facilities via DSA Priority Queue.
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
            padding: "8px 14px",
            borderRadius: "8px",
            fontSize: "13px",
            fontWeight: 700,
            cursor: "pointer",
            border: emergencyMode ? "2px solid #dc2626" : "1px solid #cbd5e1",
            backgroundColor: emergencyMode ? "#fef2f2" : "#f8fafc",
            color: emergencyMode ? "#b91c1c" : "#475569",
            transition: "all 0.2s ease",
            boxShadow: emergencyMode ? "0 0 10px rgba(220, 38, 38, 0.2)" : "none",
          }}
        >
          <span style={{ fontSize: "16px" }}>{emergencyMode ? "🚨" : "⏱️"}</span>
          <span>Emergency Mode: {emergencyMode ? "ACTIVE (24/7 Boost)" : "Standard"}</span>
        </button>
      </div>

      {/* Main Buttons Row */}
      <div
        style={{
          display: "flex",
          flexWrap: "wrap",
          gap: "10px",
          alignItems: "center",
          marginBottom: "12px",
        }}
      >
        <button
          type="button"
          onClick={onRequestBrowserLocation}
          disabled={loading}
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "6px",
            padding: "8px 16px",
            backgroundColor: "#2563eb",
            color: "#ffffff",
            border: "none",
            borderRadius: "8px",
            fontSize: "13px",
            fontWeight: 600,
            cursor: loading ? "not-allowed" : "pointer",
            opacity: loading ? 0.7 : 1,
            transition: "background-color 0.2s",
          }}
        >
          {loading ? "⏳ Detecting Location..." : "🎯 Use My Location (GPS)"}
        </button>

        <button
          type="button"
          onClick={() => setShowManualInputs(!showManualInputs)}
          style={{
            padding: "8px 14px",
            backgroundColor: "#f1f5f9",
            color: "#334155",
            border: "1px solid #cbd5e1",
            borderRadius: "8px",
            fontSize: "13px",
            fontWeight: 500,
            cursor: "pointer",
          }}
        >
          {showManualInputs ? "Hide Custom Inputs" : "✏️ Custom Lat/Lng"}
        </button>

        {coordinates && (
          <button
            type="button"
            onClick={onClearLocation}
            style={{
              padding: "8px 14px",
              backgroundColor: "#fff1f2",
              color: "#be123c",
              border: "1px solid #fecdd3",
              borderRadius: "8px",
              fontSize: "13px",
              fontWeight: 500,
              cursor: "pointer",
            }}
          >
            ✕ Reset Location
          </button>
        )}
      </div>

      {/* Karachi Zone Presets */}
      <div style={{ display: "flex", alignItems: "center", flexWrap: "wrap", gap: "8px", marginBottom: "8px" }}>
        <span style={{ fontSize: "12px", fontWeight: 600, color: "#64748b" }}>
          Karachi Presets:
        </span>
        {presets.map((preset) => {
          const isActive =
            coordinates &&
            Math.abs(coordinates.lat - preset.lat) < 0.001 &&
            Math.abs(coordinates.lng - preset.lng) < 0.001;

          return (
            <button
              key={preset.name}
              type="button"
              onClick={() => handleSelectPreset(preset)}
              style={{
                padding: "4px 10px",
                borderRadius: "6px",
                fontSize: "12px",
                fontWeight: isActive ? 700 : 500,
                backgroundColor: isActive ? "#dbeafe" : "#f8fafc",
                color: isActive ? "#1e40af" : "#475569",
                border: isActive ? "1px solid #3b82f6" : "1px solid #e2e8f0",
                cursor: "pointer",
              }}
            >
              {preset.name}
            </button>
          );
        })}
      </div>

      {/* Manual Input Form */}
      {showManualInputs && (
        <form
          onSubmit={handleApplyManual}
          style={{
            marginTop: "12px",
            padding: "12px",
            backgroundColor: "#f8fafc",
            borderRadius: "8px",
            border: "1px solid #e2e8f0",
            display: "flex",
            flexWrap: "wrap",
            gap: "10px",
            alignItems: "flex-end",
          }}
        >
          <div>
            <label style={{ display: "block", fontSize: "11px", fontWeight: 600, color: "#475569", marginBottom: "4px" }}>
              Latitude (-90 to 90)
            </label>
            <input
              type="text"
              placeholder="e.g. 24.8607"
              value={manualLat}
              onChange={(e) => setManualLat(e.target.value)}
              style={{
                padding: "6px 10px",
                fontSize: "13px",
                borderRadius: "6px",
                border: "1px solid #cbd5e1",
                width: "140px",
              }}
            />
          </div>

          <div>
            <label style={{ display: "block", fontSize: "11px", fontWeight: 600, color: "#475569", marginBottom: "4px" }}>
              Longitude (-180 to 180)
            </label>
            <input
              type="text"
              placeholder="e.g. 67.0011"
              value={manualLng}
              onChange={(e) => setManualLng(e.target.value)}
              style={{
                padding: "6px 10px",
                fontSize: "13px",
                borderRadius: "6px",
                border: "1px solid #cbd5e1",
                width: "140px",
              }}
            />
          </div>

          <button
            type="submit"
            style={{
              padding: "7px 14px",
              backgroundColor: "#059669",
              color: "#ffffff",
              border: "none",
              borderRadius: "6px",
              fontSize: "13px",
              fontWeight: 600,
              cursor: "pointer",
            }}
          >
            Apply Coordinates
          </button>
        </form>
      )}

      {/* Error Feedback */}
      {(error || manualError) && (
        <div
          style={{
            marginTop: "10px",
            padding: "8px 12px",
            backgroundColor: "#fef2f2",
            color: "#b91c1c",
            borderRadius: "6px",
            fontSize: "12px",
            border: "1px solid #fecaca",
          }}
        >
          ⚠️ {error || manualError}
        </div>
      )}

      {/* Active Location Info Banner */}
      {coordinates && (
        <div
          style={{
            marginTop: "10px",
            padding: "8px 12px",
            backgroundColor: "#f0fdf4",
            borderRadius: "6px",
            border: "1px solid #bbf7d0",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            fontSize: "12px",
            color: "#166534",
          }}
        >
          <span>
            📍 <strong>Active Origin:</strong> {coordinates.lat.toFixed(4)}, {coordinates.lng.toFixed(4)}
            {" "}(Source: {source || "manual"})
          </span>
          <span style={{ fontWeight: 600, color: "#059669" }}>
            DSA Priority Engine Active
          </span>
        </div>
      )}
    </div>
  );
};

export default LocationControls;
