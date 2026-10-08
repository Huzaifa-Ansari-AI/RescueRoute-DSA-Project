import type { Facility } from "../../types/facility";
import FacilityCard from "./FacilityCard";

interface FacilityListProps {
  facilities: Facility[];
  emptyMessage?: string;
}

export default function FacilityList({
  facilities,
  emptyMessage = "No emergency facilities found matching your criteria.",
}: FacilityListProps) {
  if (facilities.length === 0) {
    return (
      <div
        style={{
          padding: "48px 24px",
          textAlign: "center",
          backgroundColor: "#f8fafc",
          borderRadius: "12px",
          border: "1px dashed #cbd5e1",
          margin: "24px 0",
        }}
      >
        <span style={{ fontSize: "36px", display: "block", marginBottom: "12px" }}>🔍</span>
        <h4 style={{ margin: "0 0 6px 0", color: "#334155", fontSize: "16px" }}>No Results Found</h4>
        <p style={{ margin: 0, color: "#64748b", fontSize: "14px" }}>{emptyMessage}</p>
      </div>
    );
  }

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "repeat(auto-fill, minmax(320px, 1fr))",
        gap: "20px",
        margin: "20px 0",
      }}
    >
      {facilities.map((facility) => (
        <FacilityCard key={facility.id} facility={facility} />
      ))}
    </div>
  );
}
