import { useEffect, useState } from "react";
import { verificationService } from "../../services/verificationService";
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

  const handleApprove = async (id: string, name: string) => {
    const confirmed = window.confirm(
      `Are you sure you want to APPROVE '${name}'?\n\nThis will mark the facility as Verified in the system and boost its priority in emergency ranking.`
    );
    if (!confirmed) return;

    try {
      setActionLoadingId(id);
      setErrorMessage(null);
      setSuccessMessage(null);
      await verificationService.approveRequest(id, "Verified by Medical Board Administrator");
      setSuccessMessage(`Facility '${name}' has been APPROVED and marked as Verified!`);
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
                    <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "4px" }}>
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
                    </div>

                    <p style={{ color: "#475569", fontSize: "14px", margin: "0 0 6px 0" }}>
                      📍 {item.facilityAddress}, {item.facilityCity} ({item.facilityLatitude?.toFixed(4)}, {item.facilityLongitude?.toFixed(4)})
                    </p>
                    {item.facilityPhoneNumber && (
                      <p style={{ color: "#64748b", fontSize: "13px", margin: 0 }}>
                        📞 Phone: {item.facilityPhoneNumber}
                      </p>
                    )}
                  </div>

                  {/* Action Buttons for Pending */}
                  {isPending && (
                    <div style={{ display: "flex", gap: "8px" }}>
                      <button
                        onClick={() => handleApprove(item.id, item.facilityName)}
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
    </div>
  );
}
