import React from "react";
import type { RouteInfo } from "../../types/route";

interface RouteSummaryProps {
  route: RouteInfo | null;
  loading: boolean;
  error: string | null;
  targetFacilityName?: string;
  onClearRoute: () => void;
  onRetry?: () => void;
}

export const RouteSummary: React.FC<RouteSummaryProps> = ({
  route,
  loading,
  error,
  targetFacilityName,
  onClearRoute,
  onRetry,
}) => {
  if (loading) {
    return (
      <div
        style={{
          backgroundColor: "#eff6ff",
          border: "1px solid #bfdbfe",
          borderRadius: "10px",
          padding: "12px 16px",
          marginBottom: "16px",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          boxShadow: "0 2px 8px rgba(37, 99, 235, 0.08)",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
          <span style={{ fontSize: "18px", animation: "spin 1s linear infinite" }}>🔄</span>
          <div>
            <div style={{ fontSize: "14px", fontWeight: 600, color: "#1e40af" }}>
              Calculating Road Route via OSRM...
            </div>
            <div style={{ fontSize: "12px", color: "#3b82f6" }}>
              Finding the fastest street path to {targetFacilityName || "selected facility"}
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div
        style={{
          backgroundColor: "#fef2f2",
          border: "1px solid #fecaca",
          borderRadius: "10px",
          padding: "12px 16px",
          marginBottom: "16px",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          boxShadow: "0 2px 8px rgba(220, 38, 38, 0.06)",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
          <span style={{ fontSize: "18px" }}>⚠️</span>
          <div>
            <div style={{ fontSize: "14px", fontWeight: 600, color: "#b91c1c" }}>
              Routing Unavailable
            </div>
            <div style={{ fontSize: "12px", color: "#dc2626" }}>{error}</div>
          </div>
        </div>

        <div style={{ display: "flex", gap: "8px" }}>
          {onRetry && (
            <button
              type="button"
              onClick={onRetry}
              style={{
                backgroundColor: "#dc2626",
                color: "#ffffff",
                border: "none",
                borderRadius: "6px",
                padding: "6px 12px",
                fontSize: "12px",
                fontWeight: 600,
                cursor: "pointer",
              }}
            >
              Retry
            </button>
          )}
          <button
            type="button"
            onClick={onClearRoute}
            style={{
              backgroundColor: "#ffffff",
              color: "#475569",
              border: "1px solid #cbd5e1",
              borderRadius: "6px",
              padding: "6px 10px",
              fontSize: "12px",
              fontWeight: 500,
              cursor: "pointer",
            }}
          >
            Dismiss
          </button>
        </div>
      </div>
    );
  }

  if (!route) {
    return null;
  }

  return (
    <div
      style={{
        backgroundColor: "#f0fdf4",
        border: "1px solid #bbf7d0",
        borderRadius: "10px",
        padding: "14px 18px",
        marginBottom: "16px",
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        flexWrap: "wrap",
        gap: "12px",
        boxShadow: "0 2px 10px rgba(22, 101, 52, 0.08)",
      }}
    >
      {/* Route Info */}
      <div style={{ display: "flex", alignItems: "center", gap: "14px", flexWrap: "wrap" }}>
        <div
          style={{
            backgroundColor: "#22c55e",
            color: "#ffffff",
            width: "36px",
            height: "36px",
            borderRadius: "50%",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: "18px",
          }}
        >
          🚗
        </div>

        <div>
          <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
            <span style={{ fontSize: "14px", fontWeight: 700, color: "#14532d" }}>
              Active Route to {route.destinationFacilityName}
            </span>
            <span
              style={{
                fontSize: "11px",
                backgroundColor: "#dcfce7",
                color: "#15803d",
                padding: "2px 8px",
                borderRadius: "9999px",
                fontWeight: 600,
                border: "1px solid #86efac",
              }}
            >
              OSRM Navigation
            </span>
          </div>

          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "16px",
              marginTop: "4px",
              fontSize: "13px",
              color: "#166534",
            }}
          >
            <span>
              📏 Driving Distance: <strong>{route.distanceKm} km</strong>
            </span>
            <span>
              ⏱️ Estimated Travel Time: <strong>{route.durationMinutes} min</strong>
            </span>
          </div>
        </div>
      </div>

      {/* Action: Clear Route */}
      <button
        type="button"
        onClick={onClearRoute}
        style={{
          backgroundColor: "#ffffff",
          color: "#dc2626",
          border: "1px solid #fecaca",
          borderRadius: "6px",
          padding: "6px 12px",
          fontSize: "12px",
          fontWeight: 600,
          cursor: "pointer",
          display: "flex",
          alignItems: "center",
          gap: "4px",
          transition: "all 0.2s ease",
        }}
        onMouseOver={(e) => {
          e.currentTarget.style.backgroundColor = "#fef2f2";
        }}
        onMouseOut={(e) => {
          e.currentTarget.style.backgroundColor = "#ffffff";
        }}
      >
        ✕ Clear Route
      </button>
    </div>
  );
};

export default RouteSummary;
