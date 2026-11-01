import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { verificationService } from "../../services/verificationService";
import { facilityService } from "../../services/facilityService";
import type { VerificationRequest, VerificationStatus } from "../../types/verification";

export default function AdminVerificationPage() {
  const [requests, setRequests] = useState<VerificationRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoadingId, setActionLoadingId] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<string>("PENDING");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Reject Modal State
  const [rejectingItem, setRejectingItem] = useState<VerificationRequest | null>(null);
  const [rejectionNotes, setRejectionNotes] = useState("");
  const [rejectError, setRejectError] = useState<string | null>(null);

  // Facility Lifecycle Modals
  const [confirmDeactivateItem, setConfirmDeactivateItem] = useState<VerificationRequest | null>(null);
  const [confirmReactivateItem, setConfirmReactivateItem] = useState<VerificationRequest | null>(null);

  const [reloadTrigger, setReloadTrigger] = useState(0);

  useEffect(() => {
    let isMounted = true;

    verificationService
      .getVerificationRequests(activeTab === "ALL" ? undefined : activeTab)
      .then((data) => {
        if (isMounted) {
          setRequests(data);
          setLoading(false);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          const msg =
            (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
            (err instanceof Error ? err.message : "Failed to load verification requests.");
          setErrorMessage(msg);
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [activeTab, reloadTrigger]);

  const handleManualRefresh = () => {
    setLoading(true);
    setReloadTrigger((prev) => prev + 1);
  };

  const handleApprove = async (item: VerificationRequest) => {
    const isUpdate = item.requestType === "UPDATE";
    const confirmed = window.confirm(
      `Are you sure you want to APPROVE ${isUpdate ? "the UPDATED DETAILS for" : ""} '${item.facilityName}'?\n\nThis will ${
        isUpdate
          ? "apply the proposed changes to the live facility directory and mark it as Verified."
          : "mark the facility as Verified in the system and boost its priority in emergency ranking."
      }`
    );
    if (!confirmed) return;

    try {
      setActionLoadingId(item.id);
      setErrorMessage(null);
      setSuccessMessage(null);
      await verificationService.approveRequest(item.id, "Verified by Medical Board Administrator");
      setSuccessMessage(
        isUpdate
          ? `Facility '${item.facilityName}' updates have been APPROVED and applied to live directory!`
          : `Facility '${item.facilityName}' has been APPROVED and marked as Verified!`
      );
      // Refresh current view
      setReloadTrigger((prev) => prev + 1);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (err instanceof Error ? err.message : "Failed to approve request.");
      setErrorMessage(msg);
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleOpenRejectModal = (item: VerificationRequest) => {
    setRejectingItem(item);
    setRejectionNotes("");
    setRejectError(null);
  };

  const handleConfirmReject = async () => {
    if (!rejectingItem) return;
    if (!rejectionNotes.trim()) {
      setRejectError("Rejection notes/reason are mandatory.");
      return;
    }

    try {
      setActionLoadingId(rejectingItem.id);
      setErrorMessage(null);
      setSuccessMessage(null);
      await verificationService.rejectRequest(rejectingItem.id, rejectionNotes.trim());
      setSuccessMessage(`Facility '${rejectingItem.facilityName}' has been REJECTED.`);
      setRejectingItem(null);
      setReloadTrigger((prev) => prev + 1);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (err instanceof Error ? err.message : "Failed to reject verification request.");
      setRejectError(msg);
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleExecuteDeactivateFacility = async () => {
    if (!confirmDeactivateItem) return;
    try {
      setActionLoadingId(confirmDeactivateItem.id);
      setErrorMessage(null);
      setSuccessMessage(null);
      await facilityService.deactivateFacility(confirmDeactivateItem.facilityId);
      setSuccessMessage(
        `Facility '${confirmDeactivateItem.facilityName}' has been soft-deleted and deactivated. It is now excluded from public discovery.`
      );
      setConfirmDeactivateItem(null);
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

  const handleExecuteReactivateFacility = async () => {
    if (!confirmReactivateItem) return;
    try {
      setActionLoadingId(confirmReactivateItem.id);
      setErrorMessage(null);
      setSuccessMessage(null);
      await facilityService.reactivateFacility(confirmReactivateItem.facilityId);
      setSuccessMessage(
        `Facility '${confirmReactivateItem.facilityName}' has been restored and reactivated! It is once again publicly discoverable.`
      );
      setConfirmReactivateItem(null);
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

  const getStatusBadgeStyle = (status: VerificationStatus) => {
    switch (status) {
      case "APPROVED":
        return { backgroundColor: "#dcfce7", color: "#15803d", border: "1px solid #bbf7d0" };
      case "REJECTED":
        return { backgroundColor: "#fee2e2", color: "#b91c1c", border: "1px solid #fecaca" };
      default:
        return { backgroundColor: "#fef3c7", color: "#b45309", border: "1px solid #fde68a" };
    }
  };

  return (
    <div style={{ maxWidth: "1000px", margin: "0 auto", padding: "24px 16px" }}>
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
            fontWeight: 700,
            color: "#0f172a",
            textDecoration: "none",
            borderBottom: "3px solid #7e22ce",
            backgroundColor: "#f3e8ff",
            borderRadius: "8px 8px 0 0",
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
            fontWeight: 600,
            color: "#64748b",
            textDecoration: "none",
            borderRadius: "8px 8px 0 0",
            transition: "all 0.15s ease",
          }}
        >
          🏥 Facility Registry Management
        </Link>
      </div>

      {/* Header */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "24px" }}>
        <div>
          <h1 style={{ fontSize: "28px", fontWeight: "700", color: "#0f172a", marginBottom: "6px" }}>
            🛡️ Verification Moderation Queue
          </h1>
          <p style={{ color: "#64748b", fontSize: "14px", margin: 0 }}>
            Review, inspect, and approve medical facility submissions submitted by verified providers.
          </p>
        </div>
        <button
          onClick={handleManualRefresh}
          style={{
            backgroundColor: "#f1f5f9",
            color: "#334155",
            border: "1px solid #cbd5e1",
            padding: "8px 16px",
            borderRadius: "6px",
            fontSize: "13px",
            fontWeight: "600",
            cursor: "pointer",
          }}
        >
          🔄 Refresh
        </button>
      </div>

      {/* Tabs */}
      <div style={{ display: "flex", gap: "8px", borderBottom: "1px solid #e2e8f0", marginBottom: "20px" }}>
        {[
          { label: "Pending Review", value: "PENDING" },
          { label: "Approved", value: "APPROVED" },
          { label: "Rejected", value: "REJECTED" },
          { label: "All Requests", value: "ALL" },
        ].map((tab) => {
          const isActive = activeTab === tab.value;
          return (
            <button
              key={tab.value}
              onClick={() => setActiveTab(tab.value)}
              style={{
                padding: "10px 18px",
                border: "none",
                backgroundColor: "transparent",
                borderBottom: isActive ? "2px solid #dc2626" : "2px solid transparent",
                color: isActive ? "#dc2626" : "#64748b",
                fontWeight: isActive ? "600" : "500",
                fontSize: "14px",
                cursor: "pointer",
              }}
            >
              {tab.label}
            </button>
          );
        })}
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
          }}
        >
          ⚠️ {errorMessage}
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

      {/* Content Area */}
      {loading ? (
        <div style={{ textAlign: "center", padding: "48px 0", color: "#64748b" }}>
          <div style={{ fontSize: "24px", marginBottom: "8px" }}>⏳</div>
          Loading verification requests...
        </div>
      ) : requests.length === 0 ? (
        <div
          style={{
            textAlign: "center",
            padding: "60px 24px",
            backgroundColor: "#ffffff",
            borderRadius: "12px",
            border: "1px solid #e2e8f0",
          }}
        >
          <div style={{ fontSize: "36px", marginBottom: "12px" }}>🎉</div>
          <h3 style={{ fontSize: "18px", fontWeight: "600", color: "#1e293b", marginBottom: "6px" }}>
            No verification requests found
          </h3>
          <p style={{ color: "#64748b", fontSize: "14px" }}>
            There are currently no requests matching the '{activeTab}' filter.
          </p>
        </div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          {requests.map((item) => {
            const isPending = item.status === "PENDING";
            const isProcessing = actionLoadingId === item.id;

            return (
              <div
                key={item.id}
                style={{
                  backgroundColor: "#ffffff",
                  borderRadius: "10px",
                  border: "1px solid #e2e8f0",
                  padding: "20px",
                  boxShadow: "0 2px 4px rgba(0, 0, 0, 0.03)",
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "12px" }}>
                  <div>
                    <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "4px", flexWrap: "wrap" }}>
                      <h3 style={{ fontSize: "18px", fontWeight: "600", color: "#0f172a", margin: 0 }}>
                        {item.facilityName}
                      </h3>
                      <span
                        style={{
                          fontSize: "12px",
                          fontWeight: "600",
                          padding: "2px 8px",
                          borderRadius: "12px",
                          ...getStatusBadgeStyle(item.status),
                        }}
                      >
                        {item.status}
                      </span>
                      <span
                        style={{
                          fontSize: "12px",
                          color: "#475569",
                          backgroundColor: "#f1f5f9",
                          padding: "2px 8px",
                          borderRadius: "4px",
                        }}
                      >
                        {item.facilityType}
                      </span>
                      {item.requestType === "UPDATE" ? (
                        <span
                          style={{
                            fontSize: "12px",
                            fontWeight: 700,
                            color: "#4338ca",
                            backgroundColor: "#e0e7ff",
                            border: "1px solid #c7d2fe",
                            padding: "2px 8px",
                            borderRadius: "12px",
                          }}
                        >
                          🔄 UPDATE & RE-VERIFY
                        </span>
                      ) : (
                        <span
                          style={{
                            fontSize: "12px",
                            fontWeight: 600,
                            color: "#0369a1",
                            backgroundColor: "#e0f2fe",
                            border: "1px solid #bae6fd",
                            padding: "2px 8px",
                            borderRadius: "12px",
                          }}
                        >
                          ✨ NEW REGISTRATION
                        </span>
                      )}

                      {/* Live Facility Activation Status Badge */}
                      {item.facilityActive === false ? (
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
                      ) : item.facilityActive === true ? (
                        <span
                          style={{
                            fontSize: "12px",
                            fontWeight: 600,
                            padding: "2px 8px",
                            borderRadius: "12px",
                            backgroundColor: "#dcfce7",
                            color: "#15803d",
                            border: "1px solid #bbf7d0",
                          }}
                        >
                          🟢 ACTIVE
                        </span>
                      ) : null}
                    </div>

                    <p style={{ color: "#475569", fontSize: "14px", margin: "0 0 6px 0" }}>
                      📍 {item.facilityAddress}, {item.facilityCity} ({item.facilityLatitude?.toFixed(4)}, {item.facilityLongitude?.toFixed(4)})
                    </p>
                    {item.facilityPhoneNumber && (
                      <p style={{ color: "#64748b", fontSize: "13px", margin: "0 0 6px 0" }}>
                        📞 Phone: {item.facilityPhoneNumber}
                      </p>
                    )}

                    {/* Facility Inactive Notice */}
                    {item.facilityActive === false && (
                      <div
                        style={{
                          backgroundColor: "#fef2f2",
                          border: "1px solid #fecaca",
                          color: "#991b1b",
                          padding: "8px 12px",
                          borderRadius: "6px",
                          fontSize: "12px",
                          marginTop: "8px",
                          marginBottom: "8px",
                          display: "flex",
                          alignItems: "center",
                          gap: "6px",
                        }}
                      >
                        <span>⚠️</span>
                        <span>
                          <strong>Facility Inactive:</strong> This facility has been soft-deleted and is currently excluded from all public searches, map markers, and emergency ranking.
                        </span>
                      </div>
                    )}

                    {/* Proposed Changes Diff Comparison for UPDATE Requests */}
                    {item.requestType === "UPDATE" && item.proposedChanges && (
                      <div
                        style={{
                          marginTop: "12px",
                          marginBottom: "12px",
                          padding: "14px",
                          backgroundColor: "#f8fafc",
                          borderRadius: "8px",
                          border: "1px solid #e2e8f0",
                        }}
                      >
                        <div style={{ fontSize: "13px", fontWeight: 700, color: "#1e293b", marginBottom: "8px" }}>
                          📋 Proposed Staged Changes vs Current Live Facility Info:
                        </div>
                        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px", fontSize: "13px" }}>
                          <div style={{ padding: "10px", backgroundColor: "#ffffff", borderRadius: "6px", border: "1px solid #e2e8f0" }}>
                            <div style={{ fontSize: "11px", fontWeight: 700, color: "#64748b", textTransform: "uppercase", marginBottom: "6px" }}>
                              Current Live Directory
                            </div>
                            <div><strong>Name:</strong> {item.facilityName}</div>
                            <div><strong>Address:</strong> {item.facilityAddress}, {item.facilityCity}</div>
                            <div><strong>Coordinates:</strong> {item.facilityLatitude?.toFixed(4)}, {item.facilityLongitude?.toFixed(4)}</div>
                            {item.facilityPhoneNumber && <div><strong>Phone:</strong> {item.facilityPhoneNumber}</div>}
                          </div>

                          <div style={{ padding: "10px", backgroundColor: "#eff6ff", borderRadius: "6px", border: "1px solid #bfdbfe" }}>
                            <div style={{ fontSize: "11px", fontWeight: 700, color: "#1d4ed8", textTransform: "uppercase", marginBottom: "6px" }}>
                              Proposed Updates (Under Review)
                            </div>
                            <div style={{ color: item.proposedChanges.name && item.proposedChanges.name !== item.facilityName ? "#b45309" : undefined }}>
                              <strong>Name:</strong> {item.proposedChanges.name || item.facilityName}
                              {item.proposedChanges.name && item.proposedChanges.name !== item.facilityName && " (CHANGED)"}
                            </div>
                            <div style={{ color: item.proposedChanges.address && item.proposedChanges.address !== item.facilityAddress ? "#b45309" : undefined }}>
                              <strong>Address:</strong> {item.proposedChanges.address || item.facilityAddress}, {item.proposedChanges.city || item.facilityCity}
                              {item.proposedChanges.address && item.proposedChanges.address !== item.facilityAddress && " (CHANGED)"}
                            </div>
                            <div style={{ color: item.proposedChanges.latitude && item.proposedChanges.latitude !== item.facilityLatitude ? "#b45309" : undefined }}>
                              <strong>Coordinates:</strong> {item.proposedChanges.latitude?.toFixed(4)}, {item.proposedChanges.longitude?.toFixed(4)}
                              {item.proposedChanges.latitude && item.proposedChanges.latitude !== item.facilityLatitude && " (CHANGED)"}
                            </div>
                            {item.proposedChanges.phoneNumber && (
                              <div style={{ color: item.proposedChanges.phoneNumber !== item.facilityPhoneNumber ? "#b45309" : undefined }}>
                                <strong>Phone:</strong> {item.proposedChanges.phoneNumber}
                                {item.proposedChanges.phoneNumber !== item.facilityPhoneNumber && " (CHANGED)"}
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    )}

                    {item.facilityType === "Clinic" && item.facilityDetails?.clinicDetails && (
                      <div style={{ display: "flex", flexWrap: "wrap", gap: "6px", marginTop: "6px" }}>
                        {item.facilityDetails.clinicDetails.consultationAvailable && (
                          <span style={{ fontSize: "11px", fontWeight: 600, padding: "2px 8px", borderRadius: "4px", backgroundColor: "#ecfeff", color: "#0891b2" }}>
                            🩺 Consultation Available
                          </span>
                        )}
                        {item.facilityDetails.clinicDetails.openWeekends && (
                          <span style={{ fontSize: "11px", fontWeight: 600, padding: "2px 8px", borderRadius: "4px", backgroundColor: "#dcfce7", color: "#166534" }}>
                            📅 Open Weekends
                          </span>
                        )}
                        {item.facilityDetails.clinicDetails.specialties && (
                          <span style={{ fontSize: "11px", fontWeight: 600, padding: "2px 8px", borderRadius: "4px", backgroundColor: "#f3e8ff", color: "#6b21a8" }}>
                            {item.facilityDetails.clinicDetails.specialties}
                          </span>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Action Buttons */}
                  {isPending ? (
                    <div style={{ display: "flex", gap: "8px" }}>
                      <button
                        onClick={() => handleApprove(item)}
                        disabled={isProcessing}
                        style={{
                          backgroundColor: "#16a34a",
                          color: "#ffffff",
                          border: "none",
                          borderRadius: "6px",
                          padding: "8px 14px",
                          fontSize: "13px",
                          fontWeight: "600",
                          cursor: isProcessing ? "not-allowed" : "pointer",
                        }}
                      >
                        {isProcessing ? "Processing..." : "✓ Approve"}
                      </button>
                      <button
                        onClick={() => handleOpenRejectModal(item)}
                        disabled={isProcessing}
                        style={{
                          backgroundColor: "#dc2626",
                          color: "#ffffff",
                          border: "none",
                          borderRadius: "6px",
                          padding: "8px 14px",
                          fontSize: "13px",
                          fontWeight: "600",
                          cursor: isProcessing ? "not-allowed" : "pointer",
                        }}
                      >
                        ✕ Reject
                      </button>
                    </div>
                  ) : (
                    <div style={{ display: "flex", gap: "8px", alignItems: "center", flexWrap: "wrap" }}>
                      {item.facilityActive === false ? (
                        <button
                          onClick={() => setConfirmReactivateItem(item)}
                          disabled={isProcessing}
                          style={{
                            backgroundColor: "#dcfce7",
                            color: "#15803d",
                            border: "1px solid #86efac",
                            borderRadius: "6px",
                            padding: "6px 12px",
                            fontSize: "12px",
                            fontWeight: 600,
                            cursor: isProcessing ? "not-allowed" : "pointer",
                          }}
                        >
                          ♻️ Reactivate Facility
                        </button>
                      ) : (
                        <button
                          onClick={() => setConfirmDeactivateItem(item)}
                          disabled={isProcessing}
                          style={{
                            backgroundColor: "#fee2e2",
                            color: "#b91c1c",
                            border: "1px solid #fca5a5",
                            borderRadius: "6px",
                            padding: "6px 12px",
                            fontSize: "12px",
                            fontWeight: 600,
                            cursor: isProcessing ? "not-allowed" : "pointer",
                          }}
                        >
                          🛑 Deactivate Facility
                        </button>
                      )}
                      <Link
                        to="/admin/facilities"
                        style={{
                          fontSize: "12px",
                          color: "#2563eb",
                          textDecoration: "underline",
                          fontWeight: 500,
                          padding: "4px 8px",
                        }}
                      >
                        Manage in Registry →
                      </Link>
                    </div>
                  )}
                </div>

                {/* Submitter & Reviewer Meta */}
                <div
                  style={{
                    backgroundColor: "#f8fafc",
                    padding: "10px 14px",
                    borderRadius: "6px",
                    fontSize: "12px",
                    color: "#64748b",
                    display: "flex",
                    justifyContent: "space-between",
                    flexWrap: "wrap",
                    gap: "8px",
                    marginTop: "12px",
                  }}
                >
                  <div>
                    <strong>Submitted by:</strong> {item.submittedBy?.fullName || "Provider"} (
                    {item.submittedBy?.email || "Unknown"}) on{" "}
                    {new Date(item.submittedAt).toLocaleString()}
                  </div>
                  {item.reviewedBy && (
                    <div>
                      <strong>Reviewed by:</strong> {item.reviewedBy.fullName} ({item.reviewedBy.email}) on{" "}
                      {item.reviewedAt ? new Date(item.reviewedAt).toLocaleString() : "N/A"}
                    </div>
                  )}
                </div>

                {/* Review Notes if available */}
                {item.reviewNotes && (
                  <div
                    style={{
                      marginTop: "10px",
                      padding: "8px 12px",
                      backgroundColor: item.status === "APPROVED" ? "#f0fdf4" : "#fef2f2",
                      border: `1px solid ${item.status === "APPROVED" ? "#bbf7d0" : "#fecaca"}`,
                      borderRadius: "6px",
                      fontSize: "13px",
                      color: item.status === "APPROVED" ? "#166534" : "#991b1b",
                    }}
                  >
                    <strong>Review Notes:</strong> {item.reviewNotes}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Reject Modal */}
      {rejectingItem && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: "rgba(15, 23, 42, 0.6)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 9999,
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
            <h3 style={{ fontSize: "18px", fontWeight: "700", color: "#991b1b", marginBottom: "8px" }}>
              Reject Verification: {rejectingItem.facilityName}
            </h3>
            <p style={{ color: "#64748b", fontSize: "14px", marginBottom: "16px" }}>
              Please provide clear clinical or documentation reasons for rejecting this facility submission.
            </p>

            {rejectError && (
              <div
                style={{
                  backgroundColor: "#fef2f2",
                  color: "#991b1b",
                  padding: "8px 12px",
                  borderRadius: "6px",
                  fontSize: "13px",
                  marginBottom: "12px",
                }}
              >
                ⚠️ {rejectError}
              </div>
            )}

            <textarea
              rows={4}
              required
              placeholder="e.g. Invalid medical license, incorrect coordinates, or unresponsive contact number..."
              value={rejectionNotes}
              onChange={(e) => setRejectionNotes(e.target.value)}
              style={{
                width: "100%",
                padding: "10px",
                borderRadius: "6px",
                border: "1px solid #cbd5e1",
                fontSize: "14px",
                marginBottom: "20px",
                boxSizing: "border-box",
              }}
            />

            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px" }}>
              <button
                type="button"
                onClick={() => setRejectingItem(null)}
                style={{
                  padding: "8px 16px",
                  borderRadius: "6px",
                  border: "1px solid #cbd5e1",
                  backgroundColor: "#f8fafc",
                  color: "#475569",
                  fontSize: "14px",
                  fontWeight: "500",
                  cursor: "pointer",
                }}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmReject}
                style={{
                  padding: "8px 16px",
                  borderRadius: "6px",
                  border: "none",
                  backgroundColor: "#dc2626",
                  color: "#ffffff",
                  fontSize: "14px",
                  fontWeight: "600",
                  cursor: "pointer",
                }}
              >
                Confirm Rejection
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Modal - Deactivate Facility */}
      {confirmDeactivateItem && (
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
                <strong>Name:</strong> {confirmDeactivateItem.facilityName}
              </div>
              <div>
                <strong>Type:</strong> {confirmDeactivateItem.facilityType}
              </div>
              <div>
                <strong>Location:</strong> {confirmDeactivateItem.facilityAddress}, {confirmDeactivateItem.facilityCity}
              </div>
            </div>
            <p style={{ color: "#64748b", fontSize: "13px", lineHeight: "1.5", marginBottom: "20px" }}>
              ⚠️ <em>Lifecycle Effect:</em> The facility will be marked as soft-deleted. It will be immediately
              hidden from all public discovery, map markers, and emergency routing ranking.
            </p>
            <div style={{ display: "flex", justifyContent: "flex-end", gap: "12px" }}>
              <button
                onClick={() => setConfirmDeactivateItem(null)}
                disabled={actionLoadingId === confirmDeactivateItem.id}
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
                onClick={handleExecuteDeactivateFacility}
                disabled={actionLoadingId === confirmDeactivateItem.id}
                style={{
                  padding: "8px 16px",
                  borderRadius: "6px",
                  border: "none",
                  backgroundColor: "#dc2626",
                  color: "#ffffff",
                  fontWeight: 600,
                  fontSize: "14px",
                  cursor: actionLoadingId === confirmDeactivateItem.id ? "not-allowed" : "pointer",
                }}
              >
                {actionLoadingId === confirmDeactivateItem.id ? "Deactivating..." : "Yes, Deactivate Facility"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Modal - Reactivate Facility */}
      {confirmReactivateItem && (
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
                <strong>Name:</strong> {confirmReactivateItem.facilityName}
              </div>
              <div>
                <strong>Type:</strong> {confirmReactivateItem.facilityType}
              </div>
              <div>
                <strong>Location:</strong> {confirmReactivateItem.facilityAddress}, {confirmReactivateItem.facilityCity}
              </div>
            </div>
            <p style={{ color: "#64748b", fontSize: "13px", lineHeight: "1.5", marginBottom: "20px" }}>
              ℹ️ <em>Lifecycle Effect:</em> The facility's soft-deletion timestamp will be cleared and its active status
              restored. It will become discoverable again in public search results and map views according to its verification state.
            </p>
            <div style={{ display: "flex", justifyContent: "flex-end", gap: "12px" }}>
              <button
                onClick={() => setConfirmReactivateItem(null)}
                disabled={actionLoadingId === confirmReactivateItem.id}
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
                onClick={handleExecuteReactivateFacility}
                disabled={actionLoadingId === confirmReactivateItem.id}
                style={{
                  padding: "8px 16px",
                  borderRadius: "6px",
                  border: "none",
                  backgroundColor: "#16a34a",
                  color: "#ffffff",
                  fontWeight: 600,
                  fontSize: "14px",
                  cursor: actionLoadingId === confirmReactivateItem.id ? "not-allowed" : "pointer",
                }}
              >
                {actionLoadingId === confirmReactivateItem.id ? "Reactivating..." : "Yes, Reactivate Facility"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
