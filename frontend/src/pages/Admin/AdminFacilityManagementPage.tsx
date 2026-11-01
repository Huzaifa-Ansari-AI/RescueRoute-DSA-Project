import { useEffect, useState, useMemo } from "react";
import { Link } from "react-router-dom";
import { facilityService } from "../../services/facilityService";
import type { Facility, FacilityType } from "../../types/facility";
import { EditFacilityModal } from "../../components/facility/EditFacilityModal";

export default function AdminFacilityManagementPage() {
  const [facilities, setFacilities] = useState<Facility[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [reloadTrigger, setReloadTrigger] = useState(0);

  // Filters
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedType, setSelectedType] = useState<string>("All");
  const [activeFilter, setActiveFilter] = useState<"ALL" | "ACTIVE" | "DEACTIVATED">("ALL");
  const [verifiedFilter, setVerifiedFilter] = useState<"ALL" | "VERIFIED" | "UNVERIFIED">("ALL");

  // Modal states
  const [editingFacility, setEditingFacility] = useState<Facility | null>(null);
  const [confirmDeactivateFacility, setConfirmDeactivateFacility] = useState<Facility | null>(null);
  const [confirmReactivateFacility, setConfirmReactivateFacility] = useState<Facility | null>(null);

  useEffect(() => {
    let isMounted = true;

    facilityService
      .getAdminFacilities()
      .then((data) => {
        if (isMounted) {
          setFacilities(data);
          setLoading(false);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          const msg =
            (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
            (err instanceof Error ? err.message : "Failed to load facilities.");
          setErrorMessage(msg);
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [reloadTrigger]);

  const handleManualRefresh = () => {
    setLoading(true);
    setErrorMessage(null);
    setReloadTrigger((prev) => prev + 1);
  };

  // Comprehensive multi-criteria filtering across categories, activation, verification, and search
  const filteredFacilities = useMemo(() => {
    return facilities.filter((f) => {
      // Category filter
      if (selectedType !== "All" && f.facilityType?.toLowerCase() !== selectedType.toLowerCase()) {
        return false;
      }
      // Activation filter
      if (activeFilter === "ACTIVE" && !f.isActive) return false;
      if (activeFilter === "DEACTIVATED" && f.isActive) return false;
      // Verification filter
      if (verifiedFilter === "VERIFIED" && !f.isVerified) return false;
      if (verifiedFilter === "UNVERIFIED" && f.isVerified) return false;
      // Search filter
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matches =
          f.name.toLowerCase().includes(q) ||
          f.address.toLowerCase().includes(q) ||
          (f.city && f.city.toLowerCase().includes(q)) ||
          (f.phoneNumber && f.phoneNumber.includes(q));
        if (!matches) return false;
      }
      return true;
    });
  }, [facilities, selectedType, activeFilter, verifiedFilter, searchQuery]);

  // Global Registry Statistics (accurate across the entire registry, unaffected by active filters)
  const stats = useMemo(() => {
    const total = facilities.length;
    const active = facilities.filter((f) => f.isActive).length;
    const deactivated = facilities.filter((f) => !f.isActive).length;
    const verified = facilities.filter((f) => f.isVerified).length;
    const pendingUpdates = facilities.filter((f) => f.hasPendingUpdate).length;
    return { total, active, deactivated, verified, pendingUpdates };
  }, [facilities]);

  // Actions
  const handleExecuteDeactivate = async () => {
    if (!confirmDeactivateFacility) return;
    try {
      setActionLoadingId(confirmDeactivateFacility.id);
      setErrorMessage(null);
      setSuccessMessage(null);
      await facilityService.deactivateFacility(confirmDeactivateFacility.id);
      setSuccessMessage(
        `Facility '${confirmDeactivateFacility.name}' has been soft-deleted and deactivated. It is now excluded from public discovery.`
      );
      setConfirmDeactivateFacility(null);
      setReloadTrigger((prev) => prev + 1);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (err instanceof Error ? err.message : "Failed to deactivate facility.");
      setErrorMessage(msg);
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleExecuteReactivate = async () => {
    if (!confirmReactivateFacility) return;
    try {
      setActionLoadingId(confirmReactivateFacility.id);
      setErrorMessage(null);
      setSuccessMessage(null);
      await facilityService.reactivateFacility(confirmReactivateFacility.id);
      setSuccessMessage(
        `Facility '${confirmReactivateFacility.name}' has been restored and reactivated! It is once again publicly discoverable.`
      );
      setConfirmReactivateFacility(null);
      setReloadTrigger((prev) => prev + 1);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (err instanceof Error ? err.message : "Failed to reactivate facility.");
      setErrorMessage(msg);
    } finally {
      setActionLoadingId(null);
    }
  };

  const getCategoryIcon = (type: FacilityType) => {
    switch (type) {
      case "Hospital":
        return "🏥";
      case "Clinic":
        return "🩺";
      case "Blood Bank":
        return "🩸";
      case "Pharmacy":
        return "💊";
      case "Ambulance":
        return "🚑";
      default:
        return "📍";
    }
  };

  return (
    <div style={{ maxWidth: "1150px", margin: "0 auto", padding: "24px 16px" }}>
      {/* Admin Top Navigation Segmented Switcher */}
      <div
        style={{
          display: "flex",
          gap: "12px",
          borderBottom: "2px solid #e2e8f0",
          marginBottom: "24px",
          paddingBottom: "4px",
        }}
      >
        <Link
          to="/admin/verifications"
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "8px",
            padding: "10px 20px",
            fontSize: "15px",
            fontWeight: 600,
            color: "#64748b",
            textDecoration: "none",
            borderRadius: "8px 8px 0 0",
            transition: "all 0.15s ease",
          }}
        >
          🛡️ Verification Moderation Queue
        </Link>
        <Link
          to="/admin/facilities"
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "8px",
            padding: "10px 20px",
            fontSize: "15px",
            fontWeight: 700,
            color: "#0f172a",
            textDecoration: "none",
            borderBottom: "3px solid #2563eb",
            backgroundColor: "#eff6ff",
            borderRadius: "8px 8px 0 0",
          }}
        >
          🏥 Facility Registry Management
        </Link>
      </div>

      {/* Header Title & Refresh */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          marginBottom: "20px",
          flexWrap: "wrap",
          gap: "12px",
        }}
      >
        <div>
          <h1 style={{ fontSize: "28px", fontWeight: 700, color: "#0f172a", margin: "0 0 6px 0" }}>
            🏥 Medical Facility Registry Management
          </h1>
          <p style={{ color: "#64748b", fontSize: "14px", margin: 0 }}>
            Audit, inspect, deactivate, or restore medical facilities across Karachi's healthcare network.
          </p>
        </div>

        <button
          onClick={handleManualRefresh}
          disabled={loading}
          style={{
            backgroundColor: "#f8fafc",
            color: "#334155",
            border: "1px solid #cbd5e1",
            padding: "8px 16px",
            borderRadius: "6px",
            fontSize: "13px",
            fontWeight: 600,
            cursor: loading ? "not-allowed" : "pointer",
            display: "inline-flex",
            alignItems: "center",
            gap: "6px",
          }}
        >
          🔄 {loading ? "Refreshing..." : "Refresh Facilities"}
        </button>
      </div>

      {/* Summary KPI Badges */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
          gap: "12px",
          marginBottom: "24px",
        }}
      >
        <div
          style={{
            backgroundColor: "#ffffff",
            padding: "14px 16px",
            borderRadius: "8px",
            border: "1px solid #e2e8f0",
            boxShadow: "0 1px 3px rgba(0,0,0,0.02)",
          }}
        >
          <div style={{ fontSize: "12px", color: "#64748b", fontWeight: 600 }}>Total Facilities</div>
          <div style={{ fontSize: "24px", fontWeight: 700, color: "#0f172a", marginTop: "4px" }}>
            {stats.total}
          </div>
        </div>

        <div
          style={{
            backgroundColor: "#ffffff",
            padding: "14px 16px",
            borderRadius: "8px",
            border: "1px solid #bbf7d0",
            boxShadow: "0 1px 3px rgba(0,0,0,0.02)",
          }}
        >
          <div style={{ fontSize: "12px", color: "#166534", fontWeight: 600 }}>Active (Discoverable)</div>
          <div style={{ fontSize: "24px", fontWeight: 700, color: "#15803d", marginTop: "4px" }}>
            🟢 {stats.active}
          </div>
        </div>

        <div
          style={{
            backgroundColor: "#ffffff",
            padding: "14px 16px",
            borderRadius: "8px",
            border: "1px solid #fecaca",
            boxShadow: "0 1px 3px rgba(0,0,0,0.02)",
          }}
        >
          <div style={{ fontSize: "12px", color: "#991b1b", fontWeight: 600 }}>Deactivated (Soft-Deleted)</div>
          <div style={{ fontSize: "24px", fontWeight: 700, color: "#b91c1c", marginTop: "4px" }}>
            🔴 {stats.deactivated}
          </div>
        </div>

        <div
          style={{
            backgroundColor: "#ffffff",
            padding: "14px 16px",
            borderRadius: "8px",
            border: "1px solid #bfdbfe",
            boxShadow: "0 1px 3px rgba(0,0,0,0.02)",
          }}
        >
          <div style={{ fontSize: "12px", color: "#1e40af", fontWeight: 600 }}>Verified Facilities</div>
          <div style={{ fontSize: "24px", fontWeight: 700, color: "#2563eb", marginTop: "4px" }}>
            ✓ {stats.verified}
          </div>
        </div>

        <div
          style={{
            backgroundColor: "#ffffff",
            padding: "14px 16px",
            borderRadius: "8px",
            border: "1px solid #e9d5ff",
            boxShadow: "0 1px 3px rgba(0,0,0,0.02)",
          }}
        >
          <div style={{ fontSize: "12px", color: "#6b21a8", fontWeight: 600 }}>Pending Re-Verification</div>
          <div style={{ fontSize: "24px", fontWeight: 700, color: "#7e22ce", marginTop: "4px" }}>
            🔄 {stats.pendingUpdates}
          </div>
        </div>
      </div>

      {/* Feedback Messages */}
      {errorMessage && (
        <div
          style={{
            backgroundColor: "#fef2f2",
            border: "1px solid #fecaca",
            color: "#991b1b",
            padding: "12px 16px",
            borderRadius: "8px",
            marginBottom: "20px",
            fontSize: "14px",
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            flexWrap: "wrap",
            gap: "10px",
          }}
        >
          <span>⚠️ {errorMessage}</span>
          <button
            onClick={handleManualRefresh}
            style={{
              backgroundColor: "#b91c1c",
              color: "#ffffff",
              border: "none",
              padding: "6px 14px",
              borderRadius: "6px",
              fontSize: "12px",
              fontWeight: 600,
              cursor: "pointer",
            }}
          >
            🔄 Try Again
          </button>
        </div>
      )}

      {successMessage && (
        <div
          style={{
            backgroundColor: "#f0fdf4",
            border: "1px solid #bbf7d0",
            color: "#166534",
            padding: "12px 16px",
            borderRadius: "8px",
            marginBottom: "20px",
            fontSize: "14px",
          }}
        >
          ✅ {successMessage}
        </div>
      )}

      {/* Filter and Search Toolbar */}
      <div
        style={{
          backgroundColor: "#ffffff",
          padding: "16px",
          borderRadius: "10px",
          border: "1px solid #e2e8f0",
          marginBottom: "24px",
          display: "flex",
          flexDirection: "column",
          gap: "12px",
          boxShadow: "0 1px 3px rgba(0,0,0,0.02)",
        }}
      >
        <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", alignItems: "center" }}>
          {/* Search Input */}
          <div style={{ flex: "1 1 240px", position: "relative" }}>
            <input
              type="text"
              placeholder="🔍 Search by facility name, address, or phone..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{
                width: "100%",
                padding: "8px 12px",
                border: "1px solid #cbd5e1",
                borderRadius: "6px",
                fontSize: "14px",
                boxSizing: "border-box",
              }}
            />
          </div>

          {/* Type Selector */}
          <div style={{ flex: "0 0 160px" }}>
            <select
              value={selectedType}
              onChange={(e) => setSelectedType(e.target.value)}
              style={{
                width: "100%",
                padding: "8px 12px",
                border: "1px solid #cbd5e1",
                borderRadius: "6px",
                fontSize: "14px",
                backgroundColor: "#ffffff",
                boxSizing: "border-box",
              }}
            >
              <option value="All">All Categories</option>
              <option value="Hospital">Hospital</option>
              <option value="Clinic">Clinic</option>
              <option value="Pharmacy">Pharmacy</option>
              <option value="Blood Bank">Blood Bank</option>
              <option value="Ambulance">Ambulance</option>
            </select>
          </div>
        </div>

        {/* Quick Filter Buttons */}
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            flexWrap: "wrap",
            gap: "12px",
            borderTop: "1px solid #f1f5f9",
            paddingTop: "12px",
          }}
        >
          {/* Activation Filter */}
          <div style={{ display: "flex", alignItems: "center", gap: "6px", flexWrap: "wrap" }}>
            <span style={{ fontSize: "12px", fontWeight: 600, color: "#64748b" }}>Status:</span>
            {[
              { label: "All", value: "ALL" },
              { label: "Active Only", value: "ACTIVE" },
              { label: "Deactivated Only", value: "DEACTIVATED" },
            ].map((f) => {
              const isSelected = activeFilter === f.value;
              return (
                <button
                  key={f.value}
                  onClick={() => setActiveFilter(f.value as typeof activeFilter)}
                  style={{
                    padding: "4px 10px",
                    borderRadius: "4px",
                    border: "1px solid",
                    borderColor: isSelected ? "#2563eb" : "#cbd5e1",
                    backgroundColor: isSelected ? "#eff6ff" : "#ffffff",
                    color: isSelected ? "#1d4ed8" : "#475569",
                    fontSize: "12px",
                    fontWeight: isSelected ? 700 : 500,
                    cursor: "pointer",
                  }}
                >
                  {f.label}
                </button>
              );
            })}
          </div>

          {/* Verification Filter */}
          <div style={{ display: "flex", alignItems: "center", gap: "6px", flexWrap: "wrap" }}>
            <span style={{ fontSize: "12px", fontWeight: 600, color: "#64748b" }}>Verification:</span>
            {[
              { label: "All", value: "ALL" },
              { label: "Verified Only", value: "VERIFIED" },
              { label: "Unverified Only", value: "UNVERIFIED" },
            ].map((f) => {
              const isSelected = verifiedFilter === f.value;
              return (
                <button
                  key={f.value}
                  onClick={() => setVerifiedFilter(f.value as typeof verifiedFilter)}
                  style={{
                    padding: "4px 10px",
                    borderRadius: "4px",
                    border: "1px solid",
                    borderColor: isSelected ? "#059669" : "#cbd5e1",
                    backgroundColor: isSelected ? "#ecfdf5" : "#ffffff",
                    color: isSelected ? "#047857" : "#475569",
                    fontSize: "12px",
                    fontWeight: isSelected ? 700 : 500,
                    cursor: "pointer",
                  }}
                >
                  {f.label}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Facilities List */}
      {loading ? (
        <div style={{ textAlign: "center", padding: "48px 0", color: "#64748b" }}>
          <div style={{ fontSize: "28px", marginBottom: "8px" }}>⏳</div>
          Loading facility registry...
        </div>
      ) : filteredFacilities.length === 0 ? (
        <div
          style={{
            textAlign: "center",
            padding: "60px 24px",
            backgroundColor: "#ffffff",
            borderRadius: "12px",
            border: "1px solid #e2e8f0",
          }}
        >
          <div style={{ fontSize: "36px", marginBottom: "12px" }}>🔍</div>
          <h3 style={{ fontSize: "18px", fontWeight: 600, color: "#1e293b", marginBottom: "6px" }}>
            No matching facilities found
          </h3>
          <p style={{ color: "#64748b", fontSize: "14px" }}>
            Try adjusting your search query, category, or status filters.
          </p>
        </div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          {filteredFacilities.map((facility) => {
            const isProcessing = actionLoadingId === facility.id;

            return (
              <div
                key={facility.id}
                style={{
                  backgroundColor: "#ffffff",
                  borderRadius: "10px",
                  border: "1px solid #e2e8f0",
                  padding: "20px",
                  boxShadow: "0 1px 3px rgba(0, 0, 0, 0.04)",
                  borderLeft: facility.isActive ? "4px solid #16a34a" : "4px solid #dc2626",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "flex-start",
                    flexWrap: "wrap",
                    gap: "12px",
                  }}
                >
                  {/* Left Column: Details & Status */}
                  <div style={{ flex: "1 1 500px" }}>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "10px",
                        marginBottom: "6px",
                        flexWrap: "wrap",
                      }}
                    >
                      <span style={{ fontSize: "20px" }}>{getCategoryIcon(facility.type)}</span>
                      <h3 style={{ fontSize: "18px", fontWeight: 700, color: "#0f172a", margin: 0 }}>
                        {facility.name}
                      </h3>

                      <span
                        style={{
                          fontSize: "12px",
                          fontWeight: 600,
                          backgroundColor: "#f1f5f9",
                          color: "#475569",
                          padding: "2px 8px",
                          borderRadius: "4px",
                        }}
                      >
                        {facility.type}
                      </span>

                      {/* Activation Status Badge */}
                      {facility.isActive ? (
                        <span
                          style={{
                            fontSize: "12px",
                            fontWeight: 700,
                            padding: "2px 8px",
                            borderRadius: "12px",
                            backgroundColor: "#dcfce7",
                            color: "#15803d",
                            border: "1px solid #bbf7d0",
                          }}
                        >
                          🟢 ACTIVE
                        </span>
                      ) : (
                        <span
                          style={{
                            fontSize: "12px",
                            fontWeight: 700,
                            padding: "2px 8px",
                            borderRadius: "12px",
                            backgroundColor: "#fee2e2",
                            color: "#b91c1c",
                            border: "1px solid #fecaca",
                          }}
                        >
                          🔴 DEACTIVATED (HIDDEN)
                        </span>
                      )}

                      {/* Verification Status Badge */}
                      {facility.isVerified ? (
                        <span
                          style={{
                            fontSize: "12px",
                            fontWeight: 600,
                            padding: "2px 8px",
                            borderRadius: "12px",
                            backgroundColor: "#e0f2fe",
                            color: "#0369a1",
                            border: "1px solid #bae6fd",
                          }}
                        >
                          ✓ VERIFIED
                        </span>
                      ) : (
                        <span
                          style={{
                            fontSize: "12px",
                            fontWeight: 600,
                            padding: "2px 8px",
                            borderRadius: "12px",
                            backgroundColor: "#fef3c7",
                            color: "#b45309",
                            border: "1px solid #fde68a",
                          }}
                        >
                          ⏳ AWAITING VERIFICATION
                        </span>
                      )}

                      {/* Pending Update Badge */}
                      {facility.hasPendingUpdate && (
                        <span
                          style={{
                            fontSize: "12px",
                            fontWeight: 700,
                            padding: "2px 8px",
                            borderRadius: "12px",
                            backgroundColor: "#f3e8ff",
                            color: "#7e22ce",
                            border: "1px solid #e9d5ff",
                          }}
                        >
                          🔄 PENDING UPDATE REVIEW
                        </span>
                      )}
                    </div>

                    <p style={{ color: "#475569", fontSize: "14px", margin: "0 0 6px 0" }}>
                      📍 {facility.address}, {facility.city} ({facility.latitude.toFixed(4)},{" "}
                      {facility.longitude.toFixed(4)})
                    </p>

                    {facility.phoneNumber && (
                      <p style={{ color: "#64748b", fontSize: "13px", margin: "0 0 8px 0" }}>
                        📞 Phone: {facility.phoneNumber}
                      </p>
                    )}

                    {/* Subtype Capability Chips */}
                    <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", marginBottom: "8px" }}>
                      {facility.hospitalDetails?.emergencyAvailable && (
                        <span
                          style={{
                            fontSize: "11px",
                            fontWeight: 600,
                            backgroundColor: "#fee2e2",
                            color: "#dc2626",
                            padding: "2px 6px",
                            borderRadius: "4px",
                          }}
                        >
                          🚨 24/7 Emergency
                        </span>
                      )}
                      {facility.hospitalDetails?.icuAvailable && (
                        <span
                          style={{
                            fontSize: "11px",
                            fontWeight: 600,
                            backgroundColor: "#f1f5f9",
                            color: "#334155",
                            padding: "2px 6px",
                            borderRadius: "4px",
                          }}
                        >
                          🛏️ ICU
                        </span>
                      )}
                      {facility.hospitalDetails?.traumaCenterAvailable && (
                        <span
                          style={{
                            fontSize: "11px",
                            fontWeight: 600,
                            backgroundColor: "#fef3c7",
                            color: "#d97706",
                            padding: "2px 6px",
                            borderRadius: "4px",
                          }}
                        >
                          ⚡ Trauma Center
                        </span>
                      )}
                      {facility.clinicDetails?.consultationAvailable && (
                        <span
                          style={{
                            fontSize: "11px",
                            fontWeight: 600,
                            backgroundColor: "#eff6ff",
                            color: "#1d4ed8",
                            padding: "2px 6px",
                            borderRadius: "4px",
                          }}
                        >
                          🩺 Consultation Available
                        </span>
                      )}
                    </div>

                    {/* Deactivation Exclusion Banner */}
                    {!facility.isActive && (
                      <div
                        style={{
                          backgroundColor: "#fef2f2",
                          border: "1px solid #fecaca",
                          color: "#991b1b",
                          padding: "10px 14px",
                          borderRadius: "6px",
                          fontSize: "13px",
                          marginTop: "8px",
                          display: "flex",
                          alignItems: "center",
                          gap: "8px",
                        }}
                      >
                        <span>⚠️</span>
                        <span>
                          <strong>This facility is soft-deleted.</strong> It is completely excluded from
                          public searches, map markers, and emergency routing ranking.
                        </span>
                      </div>
                    )}
                  </div>

                  {/* Right Column: Admin Actions */}
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      alignItems: "flex-end",
                      gap: "8px",
                    }}
                  >
                    <div style={{ display: "flex", gap: "8px" }}>
                      <button
                        onClick={() => setEditingFacility(facility)}
                        disabled={isProcessing}
                        style={{
                          backgroundColor: "#f8fafc",
                          color: "#334155",
                          border: "1px solid #cbd5e1",
                          padding: "6px 12px",
                          borderRadius: "6px",
                          fontSize: "13px",
                          fontWeight: 600,
                          cursor: isProcessing ? "not-allowed" : "pointer",
                        }}
                      >
                        ✏️ Edit Details
                      </button>

                      {facility.isActive ? (
                        <button
                          onClick={() => setConfirmDeactivateFacility(facility)}
                          disabled={isProcessing}
                          style={{
                            backgroundColor: "#fee2e2",
                            color: "#b91c1c",
                            border: "1px solid #fca5a5",
                            padding: "6px 12px",
                            borderRadius: "6px",
                            fontSize: "13px",
                            fontWeight: 600,
                            cursor: isProcessing ? "not-allowed" : "pointer",
                          }}
                        >
                          🛑 Deactivate
                        </button>
                      ) : (
                        <button
                          onClick={() => setConfirmReactivateFacility(facility)}
                          disabled={isProcessing}
                          style={{
                            backgroundColor: "#dcfce7",
                            color: "#15803d",
                            border: "1px solid #86efac",
                            padding: "6px 12px",
                            borderRadius: "6px",
                            fontSize: "13px",
                            fontWeight: 600,
                            cursor: isProcessing ? "not-allowed" : "pointer",
                          }}
                        >
                          ♻️ Restore & Reactivate
                        </button>
                      )}
                    </div>

                    {facility.hasPendingUpdate && (
                      <Link
                        to="/admin/verifications"
                        style={{
                          fontSize: "12px",
                          color: "#7e22ce",
                          textDecoration: "underline",
                          fontWeight: 600,
                        }}
                      >
                        Go to Moderation Queue to Review Proposed Updates →
                      </Link>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Confirmation Modal - Deactivate Facility */}
      {confirmDeactivateFacility && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: "rgba(0, 0, 0, 0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
            padding: "16px",
          }}
        >
          <div
            style={{
              backgroundColor: "#ffffff",
              borderRadius: "12px",
              padding: "24px",
              maxWidth: "500px",
              width: "100%",
              boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.1)",
            }}
          >
            <div style={{ fontSize: "36px", marginBottom: "12px", textAlign: "center" }}>🛑</div>
            <h3
              style={{
                fontSize: "20px",
                fontWeight: 700,
                color: "#991b1b",
                textAlign: "center",
                marginBottom: "12px",
              }}
            >
              Confirm Facility Deactivation
            </h3>
            <p style={{ color: "#334155", fontSize: "14px", lineHeight: "1.6", marginBottom: "16px" }}>
              Are you sure you want to <strong>DEACTIVATE</strong> the following facility?
            </p>
            <div
              style={{
                backgroundColor: "#fef2f2",
                border: "1px solid #fecaca",
                padding: "12px",
                borderRadius: "8px",
                marginBottom: "16px",
                fontSize: "14px",
              }}
            >
              <div>
                <strong>Name:</strong> {confirmDeactivateFacility.name}
              </div>
              <div>
                <strong>Type:</strong> {confirmDeactivateFacility.type}
              </div>
              <div>
                <strong>Location:</strong> {confirmDeactivateFacility.address}, {confirmDeactivateFacility.city}
              </div>
            </div>
            <p style={{ color: "#64748b", fontSize: "13px", lineHeight: "1.5", marginBottom: "20px" }}>
              ⚠️ <em>Lifecycle Effect:</em> The facility will be marked as soft-deleted. It will be immediately
              hidden from all public discovery, map markers, and emergency routing ranking. Any pending verification
              requests for this facility will be auto-cancelled. Historical verification data will be retained.
            </p>
            <div style={{ display: "flex", justifyContent: "flex-end", gap: "12px" }}>
              <button
                onClick={() => setConfirmDeactivateFacility(null)}
                disabled={actionLoadingId === confirmDeactivateFacility.id}
                style={{
                  padding: "8px 16px",
                  borderRadius: "6px",
                  border: "1px solid #cbd5e1",
                  backgroundColor: "#ffffff",
                  color: "#475569",
                  fontWeight: 600,
                  fontSize: "14px",
                  cursor: "pointer",
                }}
              >
                Cancel
              </button>
              <button
                onClick={handleExecuteDeactivate}
                disabled={actionLoadingId === confirmDeactivateFacility.id}
                style={{
                  padding: "8px 16px",
                  borderRadius: "6px",
                  border: "none",
                  backgroundColor: "#dc2626",
                  color: "#ffffff",
                  fontWeight: 600,
                  fontSize: "14px",
                  cursor: actionLoadingId === confirmDeactivateFacility.id ? "not-allowed" : "pointer",
                }}
              >
                {actionLoadingId === confirmDeactivateFacility.id ? "Deactivating..." : "Yes, Deactivate Facility"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Modal - Reactivate Facility */}
      {confirmReactivateFacility && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: "rgba(0, 0, 0, 0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
            padding: "16px",
          }}
        >
          <div
            style={{
              backgroundColor: "#ffffff",
              borderRadius: "12px",
              padding: "24px",
              maxWidth: "500px",
              width: "100%",
              boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.1)",
            }}
          >
            <div style={{ fontSize: "36px", marginBottom: "12px", textAlign: "center" }}>♻️</div>
            <h3
              style={{
                fontSize: "20px",
                fontWeight: 700,
                color: "#15803d",
                textAlign: "center",
                marginBottom: "12px",
              }}
            >
              Confirm Facility Reactivation
            </h3>
            <p style={{ color: "#334155", fontSize: "14px", lineHeight: "1.6", marginBottom: "16px" }}>
              Are you sure you want to <strong>RESTORE & REACTIVATE</strong> the following facility?
            </p>
            <div
              style={{
                backgroundColor: "#f0fdf4",
                border: "1px solid #bbf7d0",
                padding: "12px",
                borderRadius: "8px",
                marginBottom: "16px",
                fontSize: "14px",
              }}
            >
              <div>
                <strong>Name:</strong> {confirmReactivateFacility.name}
              </div>
              <div>
                <strong>Type:</strong> {confirmReactivateFacility.type}
              </div>
              <div>
                <strong>Location:</strong> {confirmReactivateFacility.address}, {confirmReactivateFacility.city}
              </div>
            </div>
            <p style={{ color: "#64748b", fontSize: "13px", lineHeight: "1.5", marginBottom: "20px" }}>
              ℹ️ <em>Lifecycle Effect:</em> The facility's soft-deletion timestamp will be cleared and its active status
              restored. It will become discoverable again in public search results and map views according to its verification state.
            </p>
            <div style={{ display: "flex", justifyContent: "flex-end", gap: "12px" }}>
              <button
                onClick={() => setConfirmReactivateFacility(null)}
                disabled={actionLoadingId === confirmReactivateFacility.id}
                style={{
                  padding: "8px 16px",
                  borderRadius: "6px",
                  border: "1px solid #cbd5e1",
                  backgroundColor: "#ffffff",
                  color: "#475569",
                  fontWeight: 600,
                  fontSize: "14px",
                  cursor: "pointer",
                }}
              >
                Cancel
              </button>
              <button
                onClick={handleExecuteReactivate}
                disabled={actionLoadingId === confirmReactivateFacility.id}
                style={{
                  padding: "8px 16px",
                  borderRadius: "6px",
                  border: "none",
                  backgroundColor: "#16a34a",
                  color: "#ffffff",
                  fontWeight: 600,
                  fontSize: "14px",
                  cursor: actionLoadingId === confirmReactivateFacility.id ? "not-allowed" : "pointer",
                }}
              >
                {actionLoadingId === confirmReactivateFacility.id ? "Reactivating..." : "Yes, Reactivate Facility"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Facility Modal */}
      {editingFacility && (
        <EditFacilityModal
          facility={editingFacility}
          onClose={() => setEditingFacility(null)}
          onSuccess={(facilityName) => {
            setEditingFacility(null);
            setSuccessMessage(
              `Changes to '${facilityName}' have been submitted for administrator verification.`
            );
            setReloadTrigger((prev) => prev + 1);
          }}
        />
      )}
    </div>
  );
}
