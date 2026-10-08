import type { Facility } from "../../types/facility";

interface FacilityCardProps {
  facility: Facility;
}

export default function FacilityCard({ facility }: FacilityCardProps) {
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

  const badge = getTypeBadgeColor(facility.type);

  return (
    <div
      style={{
        backgroundColor: "#ffffff",
        borderRadius: "12px",
        border: "1px solid #e2e8f0",
        padding: "20px",
        boxShadow: "0 2px 8px rgba(0, 0, 0, 0.04)",
        display: "flex",
        flexDirection: "column",
        justifyContent: "space-between",
        transition: "transform 0.2s ease, box-shadow 0.2s ease",
      }}
      onMouseOver={(e) => {
        e.currentTarget.style.transform = "translateY(-2px)";
        e.currentTarget.style.boxShadow = "0 6px 16px rgba(0, 0, 0, 0.08)";
      }}
      onMouseOut={(e) => {
        e.currentTarget.style.transform = "translateY(0)";
        e.currentTarget.style.boxShadow = "0 2px 8px rgba(0, 0, 0, 0.04)";
      }}
    >
      <div>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "flex-start",
            marginBottom: "10px",
            gap: "12px",
          }}
        >
          <h3
            style={{
              margin: 0,
              fontSize: "18px",
              fontWeight: 600,
              color: "#0f172a",
              lineHeight: 1.3,
            }}
          >
            {facility.name}
          </h3>

          <span
            style={{
              padding: "4px 10px",
              borderRadius: "9999px",
              fontSize: "12px",
              fontWeight: 600,
              backgroundColor: badge.bg,
              color: badge.text,
              border: `1px solid ${badge.border}`,
              whiteSpace: "nowrap",
            }}
          >
            {facility.type}
          </span>
        </div>

        <p
          style={{
            margin: "0 0 8px 0",
            fontSize: "14px",
            color: "#475569",
            display: "flex",
            alignItems: "center",
            gap: "6px",
          }}
        >
          <span>📍</span> {facility.address}, {facility.city}
        </p>

        {facility.phoneNumber && (
          <p
            style={{
              margin: "0 0 12px 0",
              fontSize: "14px",
              color: "#0284c7",
              fontWeight: 500,
            }}
          >
            <a
              href={`tel:${facility.phoneNumber}`}
              style={{ color: "inherit", textDecoration: "none" }}
            >
              📞 {facility.phoneNumber}
            </a>
          </p>
        )}

        {/* Verification badge */}
        <div style={{ marginBottom: "14px" }}>
          {facility.isVerified ? (
            <span
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: "4px",
                fontSize: "12px",
                color: "#16a34a",
                fontWeight: 600,
                backgroundColor: "#f0fdf4",
                padding: "2px 8px",
                borderRadius: "6px",
              }}
            >
              ✓ Verified Provider
            </span>
          ) : (
            <span
              style={{
                fontSize: "12px",
                color: "#64748b",
                backgroundColor: "#f8fafc",
                padding: "2px 8px",
                borderRadius: "6px",
              }}
            >
              Community Registered
            </span>
          )}
        </div>

        {/* Specific Subtype details */}
        {facility.hospitalDetails && (
          <div
            style={{
              borderTop: "1px solid #f1f5f9",
              paddingTop: "12px",
              marginTop: "12px",
              fontSize: "13px",
              color: "#334155",
            }}
          >
            <div style={{ display: "flex", flexWrap: "wrap", gap: "6px", marginBottom: "8px" }}>
              {facility.hospitalDetails.emergencyAvailable && (
                <span style={tagStyle("#fee2e2", "#991b1b")}>🚨 24/7 Emergency</span>
              )}
              {facility.hospitalDetails.icuAvailable && (
                <span style={tagStyle("#e0f2fe", "#0369a1")}>ICU Available</span>
              )}
              {facility.hospitalDetails.traumaCenterAvailable && (
                <span style={tagStyle("#fef3c7", "#92400e")}>Trauma Center</span>
              )}
              {facility.hospitalDetails.cardiologyAvailable && (
                <span style={tagStyle("#f3e8ff", "#6b21a8")}>Cardiology</span>
              )}
            </div>
            {facility.hospitalDetails.notes && (
              <p style={{ margin: "6px 0 0 0", color: "#64748b", fontStyle: "italic" }}>
                {facility.hospitalDetails.notes}
              </p>
            )}
          </div>
        )}

        {facility.bloodBankDetails && (
          <div
            style={{
              borderTop: "1px solid #f1f5f9",
              paddingTop: "12px",
              marginTop: "12px",
              fontSize: "13px",
              color: "#334155",
            }}
          >
            <strong style={{ fontSize: "12px", color: "#64748b" }}>Blood Inventory:</strong>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "4px", marginTop: "6px" }}>
              {facility.bloodBankDetails.oPositive && <span style={tagStyle("#fee2e2", "#b91c1c")}>O+</span>}
              {facility.bloodBankDetails.oNegative && <span style={tagStyle("#fee2e2", "#b91c1c")}>O-</span>}
              {facility.bloodBankDetails.aPositive && <span style={tagStyle("#fee2e2", "#b91c1c")}>A+</span>}
              {facility.bloodBankDetails.aNegative && <span style={tagStyle("#fee2e2", "#b91c1c")}>A-</span>}
              {facility.bloodBankDetails.bPositive && <span style={tagStyle("#fee2e2", "#b91c1c")}>B+</span>}
              {facility.bloodBankDetails.bNegative && <span style={tagStyle("#fee2e2", "#b91c1c")}>B-</span>}
              {facility.bloodBankDetails.abPositive && <span style={tagStyle("#fee2e2", "#b91c1c")}>AB+</span>}
              {facility.bloodBankDetails.abNegative && <span style={tagStyle("#fee2e2", "#b91c1c")}>AB-</span>}
            </div>
            {facility.bloodBankDetails.notes && (
              <p style={{ margin: "6px 0 0 0", color: "#64748b", fontStyle: "italic" }}>
                {facility.bloodBankDetails.notes}
              </p>
            )}
          </div>
        )}

        {facility.pharmacyDetails && (
          <div
            style={{
              borderTop: "1px solid #f1f5f9",
              paddingTop: "12px",
              marginTop: "12px",
              fontSize: "13px",
              color: "#334155",
            }}
          >
            <div style={{ display: "flex", flexWrap: "wrap", gap: "6px", marginBottom: "8px" }}>
              {facility.pharmacyDetails.open24Hours && (
                <span style={tagStyle("#dcfce7", "#166534")}>🕒 Open 24 Hours</span>
              )}
              {facility.pharmacyDetails.emergencyMedicinesAvailable && (
                <span style={tagStyle("#fee2e2", "#991b1b")}>Emergency Meds</span>
              )}
              {facility.pharmacyDetails.deliveryAvailable && (
                <span style={tagStyle("#e0f2fe", "#0369a1")}>Delivery Available</span>
              )}
            </div>
            {facility.pharmacyDetails.notes && (
              <p style={{ margin: "6px 0 0 0", color: "#64748b", fontStyle: "italic" }}>
                {facility.pharmacyDetails.notes}
              </p>
            )}
          </div>
        )}

        {facility.ambulanceDetails && (
          <div
            style={{
              borderTop: "1px solid #f1f5f9",
              paddingTop: "12px",
              marginTop: "12px",
              fontSize: "13px",
              color: "#334155",
            }}
          >
            <div style={{ display: "flex", flexWrap: "wrap", gap: "6px", marginBottom: "8px" }}>
              {facility.ambulanceDetails.availableNow ? (
                <span style={tagStyle("#dcfce7", "#166534")}>🟢 Available Now</span>
              ) : (
                <span style={tagStyle("#f1f5f9", "#475569")}>Occupied / On Call</span>
              )}
              {facility.ambulanceDetails.responseTimeMinutes && (
                <span style={tagStyle("#fef3c7", "#92400e")}>
                  ⏱ ~{facility.ambulanceDetails.responseTimeMinutes} mins ETA
                </span>
              )}
              {facility.ambulanceDetails.equippedForIcuTransfer && (
                <span style={tagStyle("#e0f2fe", "#0369a1")}>ICU Equipped</span>
              )}
            </div>
            {facility.ambulanceDetails.coverageArea && (
              <p style={{ margin: "4px 0 0 0", color: "#64748b", fontSize: "12px" }}>
                <strong>Coverage:</strong> {facility.ambulanceDetails.coverageArea}
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

const tagStyle = (bg: string, color: string) => ({
  backgroundColor: bg,
  color: color,
  padding: "3px 8px",
  borderRadius: "6px",
  fontSize: "11px",
  fontWeight: 600 as const,
});
