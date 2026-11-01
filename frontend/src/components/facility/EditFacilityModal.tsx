import React, { useState } from "react";
import type { Facility } from "../../types/facility";
import type { UpdateFacilityPayload } from "../../types/verification";
import { facilityService } from "../../services/facilityService";

interface EditFacilityModalProps {
  facility: Facility;
  onClose: () => void;
  onSuccess: (facilityName: string) => void;
}

export const EditFacilityModal: React.FC<EditFacilityModalProps> = ({
  facility,
  onClose,
  onSuccess,
}) => {
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Common fields
  const [name, setName] = useState(facility.name);
  const [address, setAddress] = useState(facility.address);
  const [city, setCity] = useState(facility.city || "Karachi");
  const [latitude, setLatitude] = useState(String(facility.latitude));
  const [longitude, setLongitude] = useState(String(facility.longitude));
  const [phoneNumber, setPhoneNumber] = useState(facility.phoneNumber || "");

  // Subtype - Hospital
  const [emergencyAvailable, setEmergencyAvailable] = useState(
    facility.hospitalDetails?.emergencyAvailable ?? true
  );
  const [icuAvailable, setIcuAvailable] = useState(
    facility.hospitalDetails?.icuAvailable ?? false
  );
  const [traumaCenterAvailable, setTraumaCenterAvailable] = useState(
    facility.hospitalDetails?.traumaCenterAvailable ?? false
  );
  const [cardiologyAvailable, setCardiologyAvailable] = useState(
    facility.hospitalDetails?.cardiologyAvailable ?? false
  );
  const [pediatricsAvailable, setPediatricsAvailable] = useState(
    facility.hospitalDetails?.pediatricsAvailable ?? false
  );
  const [maternityAvailable, setMaternityAvailable] = useState(
    facility.hospitalDetails?.maternityAvailable ?? false
  );
  const [hospitalNotes, setHospitalNotes] = useState(
    facility.hospitalDetails?.notes || ""
  );

  // Subtype - Blood Bank
  const [oPos, setOPos] = useState(facility.bloodBankDetails?.oPositive ?? true);
  const [oNeg, setONeg] = useState(facility.bloodBankDetails?.oNegative ?? false);
  const [aPos, setAPos] = useState(facility.bloodBankDetails?.aPositive ?? true);
  const [aNeg, setANeg] = useState(facility.bloodBankDetails?.aNegative ?? false);
  const [bPos, setBPos] = useState(facility.bloodBankDetails?.bPositive ?? true);
  const [bNeg, setBNeg] = useState(facility.bloodBankDetails?.bNegative ?? false);
  const [abPos, setAbPos] = useState(facility.bloodBankDetails?.abPositive ?? false);
  const [abNeg, setAbNeg] = useState(facility.bloodBankDetails?.abNegative ?? false);
  const [bloodNotes, setBloodNotes] = useState(
    facility.bloodBankDetails?.notes || ""
  );

  // Subtype - Pharmacy
  const [open24Hours, setOpen24Hours] = useState(
    facility.pharmacyDetails?.open24Hours ?? true
  );
  const [emergencyMedicines, setEmergencyMedicines] = useState(
    facility.pharmacyDetails?.emergencyMedicinesAvailable ?? true
  );
  const [prescriptionRequired, setPrescriptionRequired] = useState(
    facility.pharmacyDetails?.prescriptionRequired ?? true
  );
  const [deliveryAvailable, setDeliveryAvailable] = useState(
    facility.pharmacyDetails?.deliveryAvailable ?? false
  );
  const [pharmacyNotes, setPharmacyNotes] = useState(
    facility.pharmacyDetails?.notes || ""
  );

  // Subtype - Ambulance
  const [availableNow, setAvailableNow] = useState(
    facility.ambulanceDetails?.availableNow ?? true
  );
  const [responseTime, setResponseTime] = useState(
    String(facility.ambulanceDetails?.responseTimeMinutes ?? 15)
  );
  const [coverageArea, setCoverageArea] = useState(
    facility.ambulanceDetails?.coverageArea || "Karachi Central"
  );
  const [icuTransfer, setIcuTransfer] = useState(
    facility.ambulanceDetails?.equippedForIcuTransfer ?? false
  );
  const [ambulanceNotes, setAmbulanceNotes] = useState(
    facility.ambulanceDetails?.notes || ""
  );

  // Subtype - Clinic
  const [consultationAvailable, setConsultationAvailable] = useState(
    facility.clinicDetails?.consultationAvailable ?? true
  );
  const [specialties, setSpecialties] = useState(
    facility.clinicDetails?.specialties || "General Medicine"
  );
  const [openWeekends, setOpenWeekends] = useState(
    facility.clinicDetails?.openWeekends ?? false
  );
  const [clinicNotes, setClinicNotes] = useState(
    facility.clinicDetails?.notes || ""
  );

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!name.trim()) {
      setErrorMessage("Facility name is required.");
      return;
    }
    if (!address.trim()) {
      setErrorMessage("Facility address is required.");
      return;
    }

    const latNum = parseFloat(latitude);
    const lngNum = parseFloat(longitude);
    if (isNaN(latNum) || latNum < -90 || latNum > 90) {
      setErrorMessage("Latitude must be between -90 and 90.");
      return;
    }
    if (isNaN(lngNum) || lngNum < -180 || lngNum > 180) {
      setErrorMessage("Longitude must be between -180 and 180.");
      return;
    }

    const payload: UpdateFacilityPayload = {
      name: name.trim(),
      address: address.trim(),
      city: city.trim() || "Karachi",
      latitude: latNum,
      longitude: lngNum,
      phoneNumber: phoneNumber.trim() || undefined,
    };

    if (facility.facilityType === "Hospital") {
      payload.hospitalDetails = {
        emergencyAvailable,
        icuAvailable,
        traumaCenterAvailable,
        cardiologyAvailable,
        pediatricsAvailable,
        maternityAvailable,
        notes: hospitalNotes.trim() || undefined,
      };
    } else if (facility.facilityType === "Blood Bank") {
      payload.bloodBankDetails = {
        oPositive: oPos,
        oNegative: oNeg,
        aPositive: aPos,
        aNegative: aNeg,
        bPositive: bPos,
        bNegative: bNeg,
        abPositive: abPos,
        abNegative: abNeg,
        notes: bloodNotes.trim() || undefined,
      };
    } else if (facility.facilityType === "Pharmacy") {
      payload.pharmacyDetails = {
        open24Hours,
        emergencyMedicinesAvailable: emergencyMedicines,
        prescriptionRequired,
        deliveryAvailable,
        notes: pharmacyNotes.trim() || undefined,
      };
    } else if (facility.facilityType === "Ambulance") {
      payload.ambulanceDetails = {
        availableNow,
        responseTimeMinutes: parseInt(responseTime, 10) || 15,
        coverageArea: coverageArea.trim() || "Karachi Central",
        equippedForIcuTransfer: icuTransfer,
        notes: ambulanceNotes.trim() || undefined,
      };
    } else if (facility.facilityType === "Clinic") {
      payload.clinicDetails = {
        consultationAvailable,
        specialties: specialties.trim() || undefined,
        openWeekends,
        notes: clinicNotes.trim() || undefined,
      };
    }

    try {
      setLoading(true);
      await facilityService.updateFacility(facility.id, payload);
      onSuccess(payload.name);
      onClose();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message ||
        (err instanceof Error ? err.message : "Failed to update facility.");
      setErrorMessage(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: "rgba(15, 23, 42, 0.65)",
        backdropFilter: "blur(4px)",
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
          borderRadius: "14px",
          padding: "28px",
          maxWidth: "680px",
          width: "100%",
          maxHeight: "90vh",
          overflowY: "auto",
          boxShadow: "0 25px 50px -12px rgba(0, 0, 0, 0.25)",
        }}
      >
        {/* Header */}
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "flex-start",
            marginBottom: "16px",
          }}
        >
          <div>
            <h2
              style={{
                fontSize: "20px",
                fontWeight: 700,
                color: "#0f172a",
                margin: 0,
              }}
            >
              ✏️ Edit Facility Details
            </h2>
            <p style={{ fontSize: "13px", color: "#64748b", margin: "4px 0 0 0" }}>
              Category: <strong>{facility.facilityType}</strong> · ID: {facility.id}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            style={{
              background: "none",
              border: "none",
              fontSize: "22px",
              cursor: "pointer",
              color: "#94a3b8",
            }}
          >
            ✕
          </button>
        </div>

        {/* Re-verification Information Notice */}
        <div
          style={{
            backgroundColor: "#eff6ff",
            border: "1px solid #bfdbfe",
            borderRadius: "10px",
            padding: "12px 16px",
            marginBottom: "18px",
            fontSize: "13px",
            color: "#1e40af",
            lineHeight: 1.5,
          }}
        >
          🛡️ <strong>Staged Re-Verification:</strong> Proposed updates to clinical details,
          addresses, or coordinates are staged for Medical Board Administrator review.
          Your existing directory listing remains active in public search until the changes are approved.
        </div>

        {errorMessage && (
          <div
            style={{
              backgroundColor: "#fef2f2",
              border: "1px solid #fecaca",
              color: "#991b1b",
              padding: "10px 14px",
              borderRadius: "8px",
              fontSize: "13px",
              marginBottom: "16px",
            }}
          >
            ⚠️ {errorMessage}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          {/* General info */}
          <div style={{ marginBottom: "16px" }}>
            <label
              style={{
                display: "block",
                fontSize: "13px",
                fontWeight: 600,
                color: "#1e293b",
                marginBottom: "4px",
              }}
            >
              Facility Name *
            </label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              style={{
                width: "100%",
                padding: "8px 12px",
                borderRadius: "6px",
                border: "1px solid #cbd5e1",
                fontSize: "14px",
                boxSizing: "border-box",
              }}
            />
          </div>

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "2fr 1fr",
              gap: "12px",
              marginBottom: "16px",
            }}
          >
            <div>
              <label
                style={{
                  display: "block",
                  fontSize: "13px",
                  fontWeight: 600,
                  color: "#1e293b",
                  marginBottom: "4px",
                }}
              >
                Street Address *
              </label>
              <input
                type="text"
                required
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                style={{
                  width: "100%",
                  padding: "8px 12px",
                  borderRadius: "6px",
                  border: "1px solid #cbd5e1",
                  fontSize: "14px",
                  boxSizing: "border-box",
                }}
              />
            </div>

            <div>
              <label
                style={{
                  display: "block",
                  fontSize: "13px",
                  fontWeight: 600,
                  color: "#1e293b",
                  marginBottom: "4px",
                }}
              >
                City
              </label>
              <input
                type="text"
                value={city}
                onChange={(e) => setCity(e.target.value)}
                style={{
                  width: "100%",
                  padding: "8px 12px",
                  borderRadius: "6px",
                  border: "1px solid #cbd5e1",
                  fontSize: "14px",
                  boxSizing: "border-box",
                }}
              />
            </div>
          </div>

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "1fr 1fr 1.5fr",
              gap: "12px",
              marginBottom: "16px",
            }}
          >
            <div>
              <label
                style={{
                  display: "block",
                  fontSize: "12px",
                  fontWeight: 600,
                  color: "#475569",
                  marginBottom: "4px",
                }}
              >
                Latitude
              </label>
              <input
                type="text"
                value={latitude}
                onChange={(e) => setLatitude(e.target.value)}
                style={{
                  width: "100%",
                  padding: "8px 10px",
                  borderRadius: "6px",
                  border: "1px solid #cbd5e1",
                  fontSize: "13px",
                  boxSizing: "border-box",
                }}
              />
            </div>

            <div>
              <label
                style={{
                  display: "block",
                  fontSize: "12px",
                  fontWeight: 600,
                  color: "#475569",
                  marginBottom: "4px",
                }}
              >
                Longitude
              </label>
              <input
                type="text"
                value={longitude}
                onChange={(e) => setLongitude(e.target.value)}
                style={{
                  width: "100%",
                  padding: "8px 10px",
                  borderRadius: "6px",
                  border: "1px solid #cbd5e1",
                  fontSize: "13px",
                  boxSizing: "border-box",
                }}
              />
            </div>

            <div>
              <label
                style={{
                  display: "block",
                  fontSize: "12px",
                  fontWeight: 600,
                  color: "#475569",
                  marginBottom: "4px",
                }}
              >
                Phone Number
              </label>
              <input
                type="text"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="e.g. 021-99215740"
                style={{
                  width: "100%",
                  padding: "8px 10px",
                  borderRadius: "6px",
                  border: "1px solid #cbd5e1",
                  fontSize: "13px",
                  boxSizing: "border-box",
                }}
              />
            </div>
          </div>

          <hr
            style={{
              border: "none",
              borderTop: "1px solid #e2e8f0",
              margin: "18px 0",
            }}
          />

          {/* Subtype specifics */}
          <h4
            style={{
              fontSize: "15px",
              fontWeight: 600,
              color: "#0f172a",
              marginBottom: "12px",
            }}
          >
            {facility.facilityType} Clinical Capabilities
          </h4>

          {facility.facilityType === "Hospital" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "10px",
                  fontSize: "13px",
                }}
              >
                <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <input
                    type="checkbox"
                    checked={emergencyAvailable}
                    onChange={(e) => setEmergencyAvailable(e.target.checked)}
                  />
                  Emergency 24/7
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <input
                    type="checkbox"
                    checked={icuAvailable}
                    onChange={(e) => setIcuAvailable(e.target.checked)}
                  />
                  ICU / Critical Care
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <input
                    type="checkbox"
                    checked={traumaCenterAvailable}
                    onChange={(e) => setTraumaCenterAvailable(e.target.checked)}
                  />
                  Trauma Center
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <input
                    type="checkbox"
                    checked={cardiologyAvailable}
                    onChange={(e) => setCardiologyAvailable(e.target.checked)}
                  />
                  Cardiology
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <input
                    type="checkbox"
                    checked={pediatricsAvailable}
                    onChange={(e) => setPediatricsAvailable(e.target.checked)}
                  />
                  Pediatrics
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <input
                    type="checkbox"
                    checked={maternityAvailable}
                    onChange={(e) => setMaternityAvailable(e.target.checked)}
                  />
                  Maternity
                </label>
              </div>

              <div>
                <label
                  style={{
                    display: "block",
                    fontSize: "12px",
                    fontWeight: 600,
                    color: "#475569",
                    marginBottom: "4px",
                  }}
                >
                  Notes
                </label>
                <textarea
                  rows={2}
                  value={hospitalNotes}
                  onChange={(e) => setHospitalNotes(e.target.value)}
                  style={{
                    width: "100%",
                    padding: "8px 10px",
                    borderRadius: "6px",
                    border: "1px solid #cbd5e1",
                    fontSize: "13px",
                    boxSizing: "border-box",
                  }}
                />
              </div>
            </div>
          )}

          {facility.facilityType === "Blood Bank" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(4, 1fr)",
                  gap: "10px",
                  fontSize: "13px",
                }}
              >
                <label style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  <input
                    type="checkbox"
                    checked={oPos}
                    onChange={(e) => setOPos(e.target.checked)}
                  />{" "}
                  O+
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  <input
                    type="checkbox"
                    checked={oNeg}
                    onChange={(e) => setONeg(e.target.checked)}
                  />{" "}
                  O-
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  <input
                    type="checkbox"
                    checked={aPos}
                    onChange={(e) => setAPos(e.target.checked)}
                  />{" "}
                  A+
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  <input
                    type="checkbox"
                    checked={aNeg}
                    onChange={(e) => setANeg(e.target.checked)}
                  />{" "}
                  A-
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  <input
                    type="checkbox"
                    checked={bPos}
                    onChange={(e) => setBPos(e.target.checked)}
                  />{" "}
                  B+
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  <input
                    type="checkbox"
                    checked={bNeg}
                    onChange={(e) => setBNeg(e.target.checked)}
                  />{" "}
                  B-
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  <input
                    type="checkbox"
                    checked={abPos}
                    onChange={(e) => setAbPos(e.target.checked)}
                  />{" "}
                  AB+
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  <input
                    type="checkbox"
                    checked={abNeg}
                    onChange={(e) => setAbNeg(e.target.checked)}
                  />{" "}
                  AB-
                </label>
              </div>

              <div>
                <label
                  style={{
                    display: "block",
                    fontSize: "12px",
                    fontWeight: 600,
                    color: "#475569",
                    marginBottom: "4px",
                  }}
                >
                  Notes
                </label>
                <textarea
                  rows={2}
                  value={bloodNotes}
                  onChange={(e) => setBloodNotes(e.target.value)}
                  style={{
                    width: "100%",
                    padding: "8px 10px",
                    borderRadius: "6px",
                    border: "1px solid #cbd5e1",
                    fontSize: "13px",
                    boxSizing: "border-box",
                  }}
                />
              </div>
            </div>
          )}

          {facility.facilityType === "Pharmacy" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "10px",
                  fontSize: "13px",
                }}
              >
                <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <input
                    type="checkbox"
                    checked={open24Hours}
                    onChange={(e) => setOpen24Hours(e.target.checked)}
                  />
                  Open 24 Hours
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <input
                    type="checkbox"
                    checked={emergencyMedicines}
                    onChange={(e) => setEmergencyMedicines(e.target.checked)}
                  />
                  Emergency Medicines
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <input
                    type="checkbox"
                    checked={prescriptionRequired}
                    onChange={(e) => setPrescriptionRequired(e.target.checked)}
                  />
                  Prescription Required
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <input
                    type="checkbox"
                    checked={deliveryAvailable}
                    onChange={(e) => setDeliveryAvailable(e.target.checked)}
                  />
                  Delivery Available
                </label>
              </div>

              <div>
                <label
                  style={{
                    display: "block",
                    fontSize: "12px",
                    fontWeight: 600,
                    color: "#475569",
                    marginBottom: "4px",
                  }}
                >
                  Notes
                </label>
                <textarea
                  rows={2}
                  value={pharmacyNotes}
                  onChange={(e) => setPharmacyNotes(e.target.value)}
                  style={{
                    width: "100%",
                    padding: "8px 10px",
                    borderRadius: "6px",
                    border: "1px solid #cbd5e1",
                    fontSize: "13px",
                    boxSizing: "border-box",
                  }}
                />
              </div>
            </div>
          )}

          {facility.facilityType === "Ambulance" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "10px",
                  fontSize: "13px",
                }}
              >
                <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <input
                    type="checkbox"
                    checked={availableNow}
                    onChange={(e) => setAvailableNow(e.target.checked)}
                  />
                  Available Now
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <input
                    type="checkbox"
                    checked={icuTransfer}
                    onChange={(e) => setIcuTransfer(e.target.checked)}
                  />
                  Equipped for ICU Transfer
                </label>
              </div>

              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "10px",
                }}
              >
                <div>
                  <label
                    style={{
                      display: "block",
                      fontSize: "12px",
                      fontWeight: 600,
                      color: "#475569",
                      marginBottom: "4px",
                    }}
                  >
                    Avg. Response Time (Mins)
                  </label>
                  <input
                    type="number"
                    value={responseTime}
                    onChange={(e) => setResponseTime(e.target.value)}
                    style={{
                      width: "100%",
                      padding: "8px 10px",
                      borderRadius: "6px",
                      border: "1px solid #cbd5e1",
                      fontSize: "13px",
                      boxSizing: "border-box",
                    }}
                  />
                </div>

                <div>
                  <label
                    style={{
                      display: "block",
                      fontSize: "12px",
                      fontWeight: 600,
                      color: "#475569",
                      marginBottom: "4px",
                    }}
                  >
                    Coverage Area
                  </label>
                  <input
                    type="text"
                    value={coverageArea}
                    onChange={(e) => setCoverageArea(e.target.value)}
                    style={{
                      width: "100%",
                      padding: "8px 10px",
                      borderRadius: "6px",
                      border: "1px solid #cbd5e1",
                      fontSize: "13px",
                      boxSizing: "border-box",
                    }}
                  />
                </div>
              </div>

              <div>
                <label
                  style={{
                    display: "block",
                    fontSize: "12px",
                    fontWeight: 600,
                    color: "#475569",
                    marginBottom: "4px",
                  }}
                >
                  Notes
                </label>
                <textarea
                  rows={2}
                  value={ambulanceNotes}
                  onChange={(e) => setAmbulanceNotes(e.target.value)}
                  style={{
                    width: "100%",
                    padding: "8px 10px",
                    borderRadius: "6px",
                    border: "1px solid #cbd5e1",
                    fontSize: "13px",
                    boxSizing: "border-box",
                  }}
                />
              </div>
            </div>
          )}

          {facility.facilityType === "Clinic" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "10px",
                  fontSize: "13px",
                }}
              >
                <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <input
                    type="checkbox"
                    checked={consultationAvailable}
                    onChange={(e) => setConsultationAvailable(e.target.checked)}
                  />
                  Consultation Available
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <input
                    type="checkbox"
                    checked={openWeekends}
                    onChange={(e) => setOpenWeekends(e.target.checked)}
                  />
                  Open Weekends
                </label>
              </div>

              <div>
                <label
                  style={{
                    display: "block",
                    fontSize: "12px",
                    fontWeight: 600,
                    color: "#475569",
                    marginBottom: "4px",
                  }}
                >
                  Clinical Specialties
                </label>
                <input
                  type="text"
                  value={specialties}
                  onChange={(e) => setSpecialties(e.target.value)}
                  style={{
                    width: "100%",
                    padding: "8px 10px",
                    borderRadius: "6px",
                    border: "1px solid #cbd5e1",
                    fontSize: "13px",
                    boxSizing: "border-box",
                  }}
                />
              </div>

              <div>
                <label
                  style={{
                    display: "block",
                    fontSize: "12px",
                    fontWeight: 600,
                    color: "#475569",
                    marginBottom: "4px",
                  }}
                >
                  Notes
                </label>
                <textarea
                  rows={2}
                  value={clinicNotes}
                  onChange={(e) => setClinicNotes(e.target.value)}
                  style={{
                    width: "100%",
                    padding: "8px 10px",
                    borderRadius: "6px",
                    border: "1px solid #cbd5e1",
                    fontSize: "13px",
                    boxSizing: "border-box",
                  }}
                />
              </div>
            </div>
          )}

          {/* Action buttons */}
          <div
            style={{
              display: "flex",
              justifyContent: "flex-end",
              gap: "12px",
              marginTop: "24px",
            }}
          >
            <button
              type="button"
              onClick={onClose}
              disabled={loading}
              style={{
                padding: "10px 18px",
                borderRadius: "8px",
                border: "1px solid #cbd5e1",
                backgroundColor: "#f8fafc",
                color: "#475569",
                fontSize: "14px",
                fontWeight: 600,
                cursor: "pointer",
              }}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              style={{
                padding: "10px 22px",
                borderRadius: "8px",
                border: "none",
                backgroundColor: loading ? "#94a3b8" : "#2563eb",
                color: "#ffffff",
                fontSize: "14px",
                fontWeight: 600,
                cursor: loading ? "not-allowed" : "pointer",
                boxShadow: "0 2px 6px rgba(37, 99, 235, 0.25)",
              }}
            >
              {loading ? "Submitting..." : "Submit Updates for Review"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default EditFacilityModal;
