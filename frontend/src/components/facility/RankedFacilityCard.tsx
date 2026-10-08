import React from "react";
import type { RankedFacility } from "../../types/facility";

interface RankedFacilityCardProps {
  rankedFacility: RankedFacility;
  isSelected?: boolean;
  hasActiveRoute?: boolean;
  isRoutingThis?: boolean;
  onSelect?: (facility: RankedFacility) => void;
  onGetDirections?: (facility: RankedFacility) => void;
}

export const RankedFacilityCard: React.FC<RankedFacilityCardProps> = ({
  rankedFacility,
  isSelected = false,
  hasActiveRoute = false,
  isRoutingThis = false,
  onSelect,
  onGetDirections,
}) => {
  const { rank, distanceKm, score, isVerified, emergencyAvailable, facility } = rankedFacility;

  const getTypeBadgeColor = (type: string) => {
    switch (type) {
      case "Hospital":
        return { bg: "#eff6ff", text: "#1d4ed8", border: "#bfdbfe" };
      case "Blood Bank":
        return { bg: "#fef2f2", text: "#b91c1c", border: "#fecaca" };
      case "Pharmacy":
        return { bg: "#f0fdf4", text: "#15803d", border: "#bbf7d0" };
      case "Ambulance":
        return { bg: "#fffbeb", text: "#b45309", border: "#fde68a" };
      default:
        return { bg: "#f1f5f9", text: "#475569", border: "#cbd5e1" };
    }
  };

  const getRankBadgeStyle = (r: number) => {
    if (r === 1) return { bg: "#fef3c7", text: "#92400e", border: "#fcd34d", label: "🥇 #1" };
    if (r === 2) return { bg: "#f1f5f9", text: "#334155", border: "#cbd5e1", label: "🥈 #2" };
    if (r === 3) return { bg: "#ffedd5", text: "#9a3412", border: "#fdba74", label: "🥉 #3" };
    return { bg: "#f8fafc", text: "#64748b", border: "#e2e8f0", label: `#${r}` };
  };

  const typeBadge = getTypeBadgeColor(facility.type);
  const rankStyle = getRankBadgeStyle(rank);
  const formattedScore = Math.round(score * 100);

  const getCardBorder = () => {
    if (hasActiveRoute) return "2px solid #16a34a";
    if (isSelected) return "2px solid #2563eb";
    return "1px solid #e2e8f0";
  };

  return (
    <div
      onClick={() => onSelect?.(rankedFacility)}
      style={{
        backgroundColor: "#ffffff",
        borderRadius: "12px",
        border: getCardBorder(),
        padding: "16px",
        boxShadow: hasActiveRoute
          ? "0 4px 14px rgba(22, 163, 74, 0.18)"
          : isSelected
          ? "0 4px 14px rgba(37, 99, 235, 0.15)"
          : "0 2px 6px rgba(0, 0, 0, 0.04)",
        cursor: "pointer",
        transition: "all 0.2s ease",
        marginBottom: "12px",
      }}
      onMouseOver={(e) => {
        if (!isSelected && !hasActiveRoute) {
          e.currentTarget.style.borderColor = "#93c5fd";
          e.currentTarget.style.transform = "translateY(-1px)";
        }
      }}
      onMouseOut={(e) => {
        if (!isSelected && !hasActiveRoute) {
          e.currentTarget.style.borderColor = "#e2e8f0";
          e.currentTarget.style.transform = "translateY(0)";
        }
      }}
    >
      {/* Top Header: Rank, Type, Distance */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "8px",
          gap: "8px",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
          <span
            style={{
              padding: "2px 8px",
              borderRadius: "6px",
              fontSize: "12px",
              fontWeight: 700,
              backgroundColor: rankStyle.bg,
              color: rankStyle.text,
              border: `1px solid ${rankStyle.border}`,
            }}
          >
            {rankStyle.label}
          </span>
          <span
            style={{
              padding: "2px 8px",
              borderRadius: "9999px",
              fontSize: "11px",
              fontWeight: 600,
              backgroundColor: typeBadge.bg,
              color: typeBadge.text,
              border: `1px solid ${typeBadge.border}`,
            }}
          >
            {facility.type}
          </span>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
          <span
            style={{
              fontSize: "12px",
              fontWeight: 600,
              color: "#0284c7",
              backgroundColor: "#f0f9ff",
              padding: "2px 8px",
              borderRadius: "6px",
              border: "1px solid #bae6fd",
            }}
          >
            📍 {distanceKm.toFixed(1)} km
          </span>
        </div>
      </div>

      {/* Facility Name & Address */}
      <div style={{ marginBottom: "6px" }}>
        <h4
          style={{
            margin: 0,
            fontSize: "16px",
            fontWeight: 600,
            color: "#0f172a",
            lineHeight: 1.3,
          }}
        >
          {facility.name}
        </h4>
        <p
          style={{
            margin: "3px 0 0 0",
            fontSize: "12px",
            color: "#64748b",
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
          }}
        >
          {facility.address}
        </p>
      </div>

      {/* Badges / Metrics: Score, Verification, Emergency */}
      <div
        style={{
          display: "flex",
          flexWrap: "wrap",
          alignItems: "center",
          gap: "6px",
          marginTop: "10px",
          paddingTop: "10px",
          borderTop: "1px dashed #f1f5f9",
        }}
      >
        <span
          style={{
            fontSize: "11px",
            fontWeight: 700,
            color: formattedScore >= 70 ? "#15803d" : "#b45309",
            backgroundColor: formattedScore >= 70 ? "#f0fdf4" : "#fffbeb",
            padding: "2px 7px",
            borderRadius: "4px",
            border: formattedScore >= 70 ? "1px solid #bbf7d0" : "1px solid #fde68a",
          }}
        >
          ⭐ Score: {formattedScore}%
        </span>

        {isVerified && (
          <span
            style={{
              fontSize: "11px",
              fontWeight: 600,
              color: "#047857",
              backgroundColor: "#ecfdf5",
              padding: "2px 7px",
              borderRadius: "4px",
              border: "1px solid #a7f3d0",
            }}
          >
            ✓ Verified
          </span>
        )}

        {emergencyAvailable && (
          <span
            style={{
              fontSize: "11px",
              fontWeight: 600,
              color: "#b91c1c",
              backgroundColor: "#fef2f2",
              padding: "2px 7px",
              borderRadius: "4px",
              border: "1px solid #fecaca",
            }}
          >
            🚨 Emergency Ready
          </span>
        )}

        {facility.phoneNumber && (
          <span
            style={{
              marginLeft: "auto",
              fontSize: "11px",
              color: "#475569",
              fontWeight: 500,
            }}
          >
            📞 {facility.phoneNumber}
          </span>
        )}
      </div>

      {/* Actions: Directions Button */}
      {onGetDirections && (
        <div
          style={{
            marginTop: "10px",
            paddingTop: "8px",
            borderTop: "1px solid #f8fafc",
            display: "flex",
            justifyContent: "flex-end",
            alignItems: "center",
          }}
        >
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onGetDirections(rankedFacility);
            }}
            disabled={isRoutingThis}
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "6px",
              padding: "6px 14px",
              borderRadius: "6px",
              fontSize: "12px",
              fontWeight: 600,
              backgroundColor: hasActiveRoute ? "#16a34a" : "#2563eb",
              color: "#ffffff",
              border: "none",
              cursor: isRoutingThis ? "wait" : "pointer",
              boxShadow: hasActiveRoute
                ? "0 2px 6px rgba(22, 163, 74, 0.25)"
                : "0 2px 6px rgba(37, 99, 235, 0.2)",
              transition: "all 0.2s ease",
            }}
            onMouseOver={(e) => {
              if (!hasActiveRoute) e.currentTarget.style.backgroundColor = "#1d4ed8";
            }}
            onMouseOut={(e) => {
              if (!hasActiveRoute) e.currentTarget.style.backgroundColor = "#2563eb";
            }}
          >
            {isRoutingThis ? "⏳ Routing..." : hasActiveRoute ? "✓ Active Route" : "🧭 Get Directions"}
          </button>
        </div>
      )}
    </div>
  );
};

export default RankedFacilityCard;
