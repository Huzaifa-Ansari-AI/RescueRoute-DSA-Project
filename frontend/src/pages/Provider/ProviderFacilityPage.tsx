import { useState, type FormEvent, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { verificationService } from "../../services/verificationService";
import geocodingService, { type PlaceSuggestion } from "../../services/geocodingService";
import type { FacilityType } from "../../types/facility";
import type { CreateFacilityPayload } from "../../types/verification";

const POPULAR_ZONES = [
  { name: "Saddar / Civil", lat: 24.8607, lng: 67.0011 },
  { name: "Clifton / South City", lat: 24.8236, lng: 67.0332 },
  { name: "Gulshan / Liaquat", lat: 24.8934, lng: 67.0722 },
  { name: "North Nazimabad", lat: 24.9333, lng: 67.0347 },
  { name: "Korangi / Indus", lat: 24.8258, lng: 67.1352 },
];

export default function ProviderFacilityPage() {
  const navigate = useNavigate();

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Base Form Fields
  const [name, setName] = useState("");
  const [facilityType, setFacilityType] = useState<FacilityType>("Hospital");
  const [address, setAddress] = useState("");
  const [city, setCity] = useState("Karachi");
  const [phoneNumber, setPhoneNumber] = useState("");

  // Location & Geocoding State (Point of facility)
  const [locationSearchQuery, setLocationSearchQuery] = useState("");
  const [locationSuggestions, setLocationSuggestions] = useState<PlaceSuggestion[]>([]);
  const [isSearchingLocation, setIsSearchingLocation] = useState(false);
  const [showLocationDropdown, setShowLocationDropdown] = useState(false);
  const [selectedLocationLabel, setSelectedLocationLabel] = useState<string>("Saddar / Central Karachi");
  const [latitude, setLatitude] = useState<string>("24.8607");
  const [longitude, setLongitude] = useState<string>("67.0011");
  const [showAdvancedCoords, setShowAdvancedCoords] = useState(false);
  const [gpsDetecting, setGpsDetecting] = useState(false);

  const searchBoxRef = useRef<HTMLDivElement>(null);
  const debounceRef = useRef<number | null>(null);

  // Subtype - Hospital
  const [emergencyAvailable, setEmergencyAvailable] = useState(true);
  const [icuAvailable, setIcuAvailable] = useState(false);
  const [traumaCenterAvailable, setTraumaCenterAvailable] = useState(false);
  const [cardiologyAvailable, setCardiologyAvailable] = useState(false);
  const [pediatricsAvailable, setPediatricsAvailable] = useState(false);
  const [maternityAvailable, setMaternityAvailable] = useState(false);
  const [hospitalNotes, setHospitalNotes] = useState("");

  // Subtype - Blood Bank
  const [oPos, setOPos] = useState(true);
  const [oNeg, setONeg] = useState(false);
  const [aPos, setAPos] = useState(true);
  const [aNeg, setANeg] = useState(false);
  const [bPos, setBPos] = useState(true);
  const [bNeg, setBNeg] = useState(false);
  const [abPos, setAbPos] = useState(false);
  const [abNeg, setAbNeg] = useState(false);
  const [bloodNotes, setBloodNotes] = useState("");

  // Subtype - Pharmacy
  const [open24Hours, setOpen24Hours] = useState(true);
  const [emergencyMedicines, setEmergencyMedicines] = useState(true);
  const [prescriptionRequired, setPrescriptionRequired] = useState(true);
  const [deliveryAvailable, setDeliveryAvailable] = useState(false);
  const [pharmacyNotes, setPharmacyNotes] = useState("");

  // Subtype - Ambulance
  const [availableNow, setAvailableNow] = useState(true);
  const [responseTime, setResponseTime] = useState<string>("15");
  const [coverageArea, setCoverageArea] = useState("Karachi Central");
  const [icuTransfer, setIcuTransfer] = useState(false);
  const [ambulanceNotes, setAmbulanceNotes] = useState("");

  // Close suggestions on click outside
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (searchBoxRef.current && !searchBoxRef.current.contains(e.target as Node)) {
        setShowLocationDropdown(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Handle location search typing
  const handleLocationSearchChange = (val: string) => {
    setLocationSearchQuery(val);
    setErrorMessage(null);

    if (debounceRef.current) {
      window.clearTimeout(debounceRef.current);
    }

    if (!val.trim()) {
      setLocationSuggestions([]);
      setShowLocationDropdown(false);
      return;
    }

    debounceRef.current = window.setTimeout(async () => {
      setIsSearchingLocation(true);
      try {
        const results = await geocodingService.searchPlaces(val);
        setLocationSuggestions(results);
        setShowLocationDropdown(true);
      } catch {
        setLocationSuggestions([]);
      } finally {
        setIsSearchingLocation(false);
      }
    }, 250);
  };

  const handleSelectPlace = (place: PlaceSuggestion) => {
    setLatitude(place.lat.toFixed(6));
    setLongitude(place.lng.toFixed(6));
    setSelectedLocationLabel(place.name);
    setLocationSearchQuery("");
    setShowLocationDropdown(false);

    // If address is empty, provide default from selected place
    if (!address.trim()) {
      setAddress(place.name + ", Karachi");
    }
  };

  const handleSelectPresetZone = (zone: { name: string; lat: number; lng: number }) => {
    setLatitude(zone.lat.toFixed(6));
    setLongitude(zone.lng.toFixed(6));
    setSelectedLocationLabel(zone.name);
    if (!address.trim()) {
      setAddress(zone.name + ", Karachi");
    }
  };

  const handleUseCurrentLocation = () => {
    if (!navigator.geolocation) {
      setErrorMessage("Geolocation is not supported by your browser.");
      return;
    }

    setGpsDetecting(true);
    setErrorMessage(null);

    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const lat = pos.coords.latitude;
        const lng = pos.coords.longitude;
        setLatitude(lat.toFixed(6));
        setLongitude(lng.toFixed(6));

        try {
          const revName = await geocodingService.reverseGeocode(lat, lng);
          setSelectedLocationLabel(revName || `GPS (${lat.toFixed(4)}, ${lng.toFixed(4)})`);
          if (!address.trim() && revName) {
            setAddress(revName);
          }
        } catch {
          setSelectedLocationLabel(`GPS (${lat.toFixed(4)}, ${lng.toFixed(4)})`);
        } finally {
          setGpsDetecting(false);
        }
      },
      (err) => {
        setGpsDetecting(false);
        setErrorMessage("Unable to fetch location: " + err.message);
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    // Validation
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
      setErrorMessage("Latitude must be a valid number between -90 and +90.");
      return;
    }
    if (isNaN(lngNum) || lngNum < -180 || lngNum > 180) {
      setErrorMessage("Longitude must be a valid number between -180 and +180.");
      return;
    }

    const payload: CreateFacilityPayload = {
      name: name.trim(),
      facilityType,
      address: address.trim(),
      city: city.trim() || "Karachi",
      latitude: latNum,
      longitude: lngNum,
      phoneNumber: phoneNumber.trim() || undefined,
    };

    if (facilityType === "Hospital") {
      payload.hospitalDetails = {
        emergencyAvailable,
        icuAvailable,
        traumaCenterAvailable,
        cardiologyAvailable,
        pediatricsAvailable,
        maternityAvailable,
        notes: hospitalNotes.trim() || undefined,
      };
    } else if (facilityType === "Blood Bank") {
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
    } else if (facilityType === "Pharmacy") {
      payload.pharmacyDetails = {
        open24Hours,
        emergencyMedicinesAvailable: emergencyMedicines,
        prescriptionRequired,
        deliveryAvailable,
        notes: pharmacyNotes.trim() || undefined,
      };
    } else if (facilityType === "Ambulance") {
      payload.ambulanceDetails = {
        availableNow,
        responseTimeMinutes: parseInt(responseTime, 10) || 15,
        coverageArea: coverageArea.trim() || "Karachi Central",
        equippedForIcuTransfer: icuTransfer,
        notes: ambulanceNotes.trim() || undefined,
      };
    }

    try {
      setLoading(true);
      const created = await verificationService.createFacility(payload);
      setSuccessMessage(
        `Facility '${created.name}' submitted successfully! A verification request has been queued for Administrator review.`
      );
      // Reset main fields
      setName("");
      setAddress("");
      setPhoneNumber("");
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (err instanceof Error ? err.message : "Failed to submit facility.");
      setErrorMessage(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: "860px", margin: "0 auto", padding: "24px 16px" }}>
      {/* Header */}
      <div style={{ marginBottom: "24px" }}>
        <h1 style={{ fontSize: "28px", fontWeight: "800", color: "#0f172a", marginBottom: "8px", letterSpacing: "-0.02em" }}>
          🏥 Medical Provider Portal
        </h1>
        <p style={{ color: "#64748b", fontSize: "15px", lineHeight: "1.5", margin: 0 }}>
          Register your hospital, pharmacy, blood bank, or ambulance fleet in Karachi’s emergency response registry.
        </p>
      </div>

      {/* Verification Notice */}
      <div
        style={{
          backgroundColor: "#eff6ff",
          border: "1px solid #bfdbfe",
          borderRadius: "12px",
          padding: "16px",
          marginBottom: "24px",
          display: "flex",
          gap: "12px",
          alignItems: "flex-start",
          boxShadow: "0 2px 8px rgba(37, 99, 235, 0.05)",
        }}
      >
        <span style={{ fontSize: "22px" }}>🛡️</span>
        <div style={{ fontSize: "13px", color: "#1e40af", lineHeight: "1.6" }}>
          <strong>Verification Process:</strong> All newly submitted facilities enter the registry as{" "}
          <span style={{ fontWeight: "700", textDecoration: "underline" }}>Unverified</span>. A verification
          ticket is automatically submitted to Administrator review. Upon approval, your facility receives the Verified
          badge and highest priority in emergency navigation.
        </div>
      </div>

      {/* Error / Success Feedback */}
      {errorMessage && (
        <div
          style={{
            backgroundColor: "#fef2f2",
            border: "1px solid #fecaca",
            color: "#991b1b",
            padding: "14px 18px",
            borderRadius: "10px",
            marginBottom: "20px",
            fontSize: "14px",
            fontWeight: 500,
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
            padding: "18px",
            borderRadius: "12px",
            marginBottom: "24px",
            fontSize: "14px",
            display: "flex",
            flexDirection: "column",
            gap: "12px",
          }}
        >
          <div style={{ fontWeight: 600, fontSize: "15px" }}>✅ {successMessage}</div>
          <div>
            <button
              onClick={() => navigate("/")}
              style={{
                backgroundColor: "#16a34a",
                color: "#ffffff",
                border: "none",
                borderRadius: "8px",
                padding: "8px 18px",
                fontSize: "13px",
                fontWeight: "600",
                cursor: "pointer",
              }}
            >
              Return to Map View
            </button>
          </div>
        </div>
      )}

      {/* Main Submission Form */}
      <form
        onSubmit={handleSubmit}
        style={{
          backgroundColor: "#ffffff",
          borderRadius: "16px",
          padding: "32px",
          boxShadow: "0 4px 20px rgba(0, 0, 0, 0.06)",
          border: "1px solid #e2e8f0",
        }}
      >
        <h2 style={{ fontSize: "18px", fontWeight: "700", color: "#0f172a", marginBottom: "20px" }}>
          General Facility Information
        </h2>

        {/* Row 1: Name and Category */}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px", marginBottom: "16px" }}>
          <div>
            <label style={{ display: "block", fontSize: "13px", fontWeight: "700", color: "#1e293b", marginBottom: "6px" }}>
              Facility Name *
            </label>
            <input
              type="text"
              required
              placeholder="e.g. Civil Hospital Trauma Center"
              value={name}
              onChange={(e) => setName(e.target.value)}
              style={{
                width: "100%",
                padding: "10px 14px",
                borderRadius: "8px",
                border: "1px solid #cbd5e1",
                fontSize: "14px",
                color: "#0f172a",
                backgroundColor: "#ffffff",
                outline: "none",
                boxSizing: "border-box",
              }}
            />
          </div>

          <div>
            <label style={{ display: "block", fontSize: "13px", fontWeight: "700", color: "#1e293b", marginBottom: "6px" }}>
              Facility Category *
            </label>
            <select
              value={facilityType}
              onChange={(e) => setFacilityType(e.target.value as FacilityType)}
              style={{
                width: "100%",
                padding: "10px 14px",
                borderRadius: "8px",
                border: "1px solid #cbd5e1",
                fontSize: "14px",
                color: "#0f172a",
                backgroundColor: "#ffffff",
                outline: "none",
                boxSizing: "border-box",
                cursor: "pointer",
              }}
            >
              <option value="Hospital">Hospital</option>
              <option value="Blood Bank">Blood Bank</option>
              <option value="Pharmacy">Pharmacy</option>
              <option value="Ambulance">Ambulance Service</option>
            </select>
          </div>
        </div>

        {/* Human-Friendly Location & Geocoding Section */}
        <div
          style={{
            backgroundColor: "#f8fafc",
            borderRadius: "12px",
            border: "1px solid #e2e8f0",
            padding: "18px",
            marginBottom: "20px",
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "10px" }}>
            <label style={{ fontSize: "14px", fontWeight: "700", color: "#0f172a" }}>
              📍 Facility Geographic Location
            </label>
            <button
              type="button"
              onClick={handleUseCurrentLocation}
              disabled={gpsDetecting}
              style={{
                display: "flex",
                alignItems: "center",
                gap: "6px",
                backgroundColor: "#ffffff",
                color: "#2563eb",
                border: "1px solid #bfdbfe",
                borderRadius: "6px",
                padding: "5px 12px",
                fontSize: "12px",
                fontWeight: 600,
                cursor: gpsDetecting ? "wait" : "pointer",
              }}
            >
              {gpsDetecting ? "📡 Detecting GPS..." : "🎯 Detect Current Coordinates (GPS)"}
            </button>
          </div>

          <p style={{ margin: "0 0 12px 0", fontSize: "13px", color: "#64748b" }}>
            Search your Karachi area, town, or hospital landmark to automatically calibrate navigation coordinates.
          </p>

          {/* Autocomplete Input Container */}
          <div ref={searchBoxRef} style={{ position: "relative", marginBottom: "12px" }}>
            <input
              type="text"
              placeholder="Search area (e.g. Nazimabad, Clifton, Saddar, Gulshan, Korangi)..."
              value={locationSearchQuery}
              onChange={(e) => handleLocationSearchChange(e.target.value)}
              onFocus={() => {
                if (locationSuggestions.length > 0) setShowLocationDropdown(true);
              }}
              style={{
                width: "100%",
                padding: "10px 14px",
                borderRadius: "8px",
                border: "1px solid #cbd5e1",
                fontSize: "14px",
                color: "#0f172a",
                backgroundColor: "#ffffff",
                outline: "none",
                boxSizing: "border-box",
              }}
            />

            {isSearchingLocation && (
              <span
                style={{
                  position: "absolute",
                  right: "12px",
                  top: "10px",
                  fontSize: "13px",
                  color: "#64748b",
                }}
              >
                Searching...
              </span>
            )}

            {/* Dropdown Suggestions */}
            {showLocationDropdown && locationSuggestions.length > 0 && (
              <div
                style={{
                  position: "absolute",
                  top: "100%",
                  left: 0,
                  right: 0,
                  marginTop: "4px",
                  backgroundColor: "#ffffff",
                  border: "1px solid #cbd5e1",
                  borderRadius: "8px",
                  boxShadow: "0 10px 25px rgba(0, 0, 0, 0.1)",
                  zIndex: 50,
                  maxHeight: "220px",
                  overflowY: "auto",
                }}
              >
                {locationSuggestions.map((place) => (
                  <div
                    key={place.id}
                    onClick={() => handleSelectPlace(place)}
                    style={{
                      padding: "10px 14px",
                      cursor: "pointer",
                      borderBottom: "1px solid #f1f5f9",
                      fontSize: "13px",
                      color: "#1e293b",
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = "#f8fafc")}
                    onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = "#ffffff")}
                  >
                    <div style={{ fontWeight: 600 }}>{place.name}</div>
                    <div style={{ fontSize: "11px", color: "#64748b" }}>
                      {place.locality || "Karachi"} · {place.lat.toFixed(4)}, {place.lng.toFixed(4)}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Quick Zone Chips */}
          <div style={{ display: "flex", alignItems: "center", gap: "6px", flexWrap: "wrap", marginBottom: "12px" }}>
            <span style={{ fontSize: "12px", color: "#64748b", fontWeight: 600 }}>Quick Zones:</span>
            {POPULAR_ZONES.map((zone) => (
              <button
                key={zone.name}
                type="button"
                onClick={() => handleSelectPresetZone(zone)}
                style={{
                  backgroundColor: selectedLocationLabel === zone.name ? "#dbeafe" : "#ffffff",
                  color: selectedLocationLabel === zone.name ? "#1d4ed8" : "#334155",
                  border: selectedLocationLabel === zone.name ? "1px solid #93c5fd" : "1px solid #cbd5e1",
                  borderRadius: "6px",
                  padding: "4px 10px",
                  fontSize: "12px",
                  fontWeight: 500,
                  cursor: "pointer",
                }}
              >
                {zone.name}
              </button>
            ))}
          </div>

          {/* Selected Coordinates Status Badge */}
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              backgroundColor: "#ffffff",
              border: "1px solid #bfdbfe",
              borderRadius: "8px",
              padding: "10px 14px",
              fontSize: "13px",
            }}
          >
            <div>
              <span style={{ color: "#166534", fontWeight: 700 }}>📍 Selected Location: </span>
              <span style={{ color: "#0f172a", fontWeight: 600 }}>{selectedLocationLabel}</span>
              <span style={{ color: "#64748b", fontSize: "12px", marginLeft: "8px" }}>
                ({latitude}, {longitude})
              </span>
            </div>

            <button
              type="button"
              onClick={() => setShowAdvancedCoords(!showAdvancedCoords)}
              style={{
                background: "none",
                border: "none",
                color: "#2563eb",
                fontSize: "12px",
                fontWeight: 600,
                cursor: "pointer",
                textDecoration: "underline",
              }}
            >
              {showAdvancedCoords ? "Hide Raw Coordinates" : "Fine-tune Coordinates"}
            </button>
          </div>

          {/* Collapsed Advanced Coordinate Input */}
          {showAdvancedCoords && (
            <div
              style={{
                marginTop: "12px",
                padding: "12px",
                backgroundColor: "#ffffff",
                border: "1px dashed #cbd5e1",
                borderRadius: "8px",
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: "12px",
              }}
            >
              <div>
                <label style={{ display: "block", fontSize: "12px", fontWeight: "600", color: "#475569", marginBottom: "4px" }}>
                  Latitude (decimal)
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
                    color: "#0f172a",
                    backgroundColor: "#ffffff",
                    boxSizing: "border-box",
                  }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: "12px", fontWeight: "600", color: "#475569", marginBottom: "4px" }}>
                  Longitude (decimal)
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
                    color: "#0f172a",
                    backgroundColor: "#ffffff",
                    boxSizing: "border-box",
                  }}
                />
              </div>
            </div>
          )}
        </div>

        {/* Row 2: Address, City, Phone */}
        <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr 1.5fr", gap: "16px", marginBottom: "24px" }}>
          <div>
            <label style={{ display: "block", fontSize: "13px", fontWeight: "700", color: "#1e293b", marginBottom: "6px" }}>
              Street Address *
            </label>
            <input
              type="text"
              required
              placeholder="e.g. Mission Road, New Dehli Colony"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              style={{
                width: "100%",
                padding: "10px 14px",
                borderRadius: "8px",
                border: "1px solid #cbd5e1",
                fontSize: "14px",
                color: "#0f172a",
                backgroundColor: "#ffffff",
                boxSizing: "border-box",
              }}
            />
          </div>

          <div>
            <label style={{ display: "block", fontSize: "13px", fontWeight: "700", color: "#1e293b", marginBottom: "6px" }}>
              City
            </label>
            <input
              type="text"
              value={city}
              onChange={(e) => setCity(e.target.value)}
              style={{
                width: "100%",
                padding: "10px 14px",
                borderRadius: "8px",
                border: "1px solid #cbd5e1",
                fontSize: "14px",
                color: "#0f172a",
                backgroundColor: "#ffffff",
                boxSizing: "border-box",
              }}
            />
          </div>

          <div>
            <label style={{ display: "block", fontSize: "13px", fontWeight: "700", color: "#1e293b", marginBottom: "6px" }}>
              Emergency Phone Number
            </label>
            <input
              type="text"
              placeholder="e.g. 021-99215740"
              value={phoneNumber}
              onChange={(e) => setPhoneNumber(e.target.value)}
              style={{
                width: "100%",
                padding: "10px 14px",
                borderRadius: "8px",
                border: "1px solid #cbd5e1",
                fontSize: "14px",
                color: "#0f172a",
                backgroundColor: "#ffffff",
                boxSizing: "border-box",
              }}
            />
          </div>
        </div>

        <hr style={{ border: "none", borderTop: "1px solid #e2e8f0", margin: "24px 0" }} />

        {/* Dynamic Category Specific Fields */}
        <h2 style={{ fontSize: "18px", fontWeight: "700", color: "#0f172a", marginBottom: "16px" }}>
          {facilityType} Specific Capabilities
        </h2>

        {/* Hospital Details Form */}
        {facilityType === "Hospital" && (
          <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
              <label style={{ display: "flex", alignItems: "center", gap: "10px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input
                  type="checkbox"
                  checked={emergencyAvailable}
                  onChange={(e) => setEmergencyAvailable(e.target.checked)}
                />
                Emergency Department (24/7)
              </label>

              <label style={{ display: "flex", alignItems: "center", gap: "10px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input
                  type="checkbox"
                  checked={icuAvailable}
                  onChange={(e) => setIcuAvailable(e.target.checked)}
                />
                ICU / Critical Care Available
              </label>

              <label style={{ display: "flex", alignItems: "center", gap: "10px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input
                  type="checkbox"
                  checked={traumaCenterAvailable}
                  onChange={(e) => setTraumaCenterAvailable(e.target.checked)}
                />
                Trauma Center Equipped
              </label>

              <label style={{ display: "flex", alignItems: "center", gap: "10px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input
                  type="checkbox"
                  checked={cardiologyAvailable}
                  onChange={(e) => setCardiologyAvailable(e.target.checked)}
                />
                Cardiology Unit
              </label>

              <label style={{ display: "flex", alignItems: "center", gap: "10px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input
                  type="checkbox"
                  checked={pediatricsAvailable}
                  onChange={(e) => setPediatricsAvailable(e.target.checked)}
                />
                Pediatrics Ward
              </label>

              <label style={{ display: "flex", alignItems: "center", gap: "10px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input
                  type="checkbox"
                  checked={maternityAvailable}
                  onChange={(e) => setMaternityAvailable(e.target.checked)}
                />
                Maternity / Labor Room
              </label>
            </div>

            <div style={{ marginTop: "12px" }}>
              <label style={{ display: "block", fontSize: "13px", fontWeight: "700", color: "#1e293b", marginBottom: "6px" }}>
                Special Clinical Notes
              </label>
              <textarea
                rows={3}
                placeholder="Mention available specialist doctors, bed capacities, or parking instructions..."
                value={hospitalNotes}
                onChange={(e) => setHospitalNotes(e.target.value)}
                style={{
                  width: "100%",
                  padding: "10px 14px",
                  borderRadius: "8px",
                  border: "1px solid #cbd5e1",
                  fontSize: "14px",
                  color: "#0f172a",
                  backgroundColor: "#ffffff",
                  boxSizing: "border-box",
                }}
              />
            </div>
          </div>
        )}

        {/* Blood Bank Details Form */}
        {facilityType === "Blood Bank" && (
          <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
            <p style={{ fontSize: "13px", color: "#64748b", margin: 0 }}>
              Select blood groups currently maintained in stock:
            </p>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "12px" }}>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input type="checkbox" checked={oPos} onChange={(e) => setOPos(e.target.checked)} /> O+
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input type="checkbox" checked={oNeg} onChange={(e) => setONeg(e.target.checked)} /> O- (Universal)
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input type="checkbox" checked={aPos} onChange={(e) => setAPos(e.target.checked)} /> A+
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input type="checkbox" checked={aNeg} onChange={(e) => setANeg(e.target.checked)} /> A-
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input type="checkbox" checked={bPos} onChange={(e) => setBPos(e.target.checked)} /> B+
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input type="checkbox" checked={bNeg} onChange={(e) => setBNeg(e.target.checked)} /> B-
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input type="checkbox" checked={abPos} onChange={(e) => setAbPos(e.target.checked)} /> AB+
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input type="checkbox" checked={abNeg} onChange={(e) => setAbNeg(e.target.checked)} /> AB-
              </label>
            </div>

            <div style={{ marginTop: "12px" }}>
              <label style={{ display: "block", fontSize: "13px", fontWeight: "700", color: "#1e293b", marginBottom: "6px" }}>
                Blood Bank / Screening Notes
              </label>
              <textarea
                rows={2}
                placeholder="Platelets, plasma, screening tests performed..."
                value={bloodNotes}
                onChange={(e) => setBloodNotes(e.target.value)}
                style={{
                  width: "100%",
                  padding: "10px 14px",
                  borderRadius: "8px",
                  border: "1px solid #cbd5e1",
                  fontSize: "14px",
                  color: "#0f172a",
                  backgroundColor: "#ffffff",
                  boxSizing: "border-box",
                }}
              />
            </div>
          </div>
        )}

        {/* Pharmacy Details Form */}
        {facilityType === "Pharmacy" && (
          <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input type="checkbox" checked={open24Hours} onChange={(e) => setOpen24Hours(e.target.checked)} />
                Open 24 Hours
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input type="checkbox" checked={emergencyMedicines} onChange={(e) => setEmergencyMedicines(e.target.checked)} />
                Emergency Medicines Stocked
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input type="checkbox" checked={prescriptionRequired} onChange={(e) => setPrescriptionRequired(e.target.checked)} />
                Prescription Required
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input type="checkbox" checked={deliveryAvailable} onChange={(e) => setDeliveryAvailable(e.target.checked)} />
                Home Delivery Available
              </label>
            </div>

            <div style={{ marginTop: "12px" }}>
              <label style={{ display: "block", fontSize: "13px", fontWeight: "700", color: "#1e293b", marginBottom: "6px" }}>
                Pharmacy Notes
              </label>
              <textarea
                rows={2}
                placeholder="Insulin cold-chain, oxygen cylinder availability..."
                value={pharmacyNotes}
                onChange={(e) => setPharmacyNotes(e.target.value)}
                style={{
                  width: "100%",
                  padding: "10px 14px",
                  borderRadius: "8px",
                  border: "1px solid #cbd5e1",
                  fontSize: "14px",
                  color: "#0f172a",
                  backgroundColor: "#ffffff",
                  boxSizing: "border-box",
                }}
              />
            </div>
          </div>
        )}

        {/* Ambulance Details Form */}
        {facilityType === "Ambulance" && (
          <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input type="checkbox" checked={availableNow} onChange={(e) => setAvailableNow(e.target.checked)} />
                Fleet Available Now
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", color: "#1e293b", cursor: "pointer" }}>
                <input type="checkbox" checked={icuTransfer} onChange={(e) => setIcuTransfer(e.target.checked)} />
                Equipped for ICU Transfer / Ventilator
              </label>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
              <div>
                <label style={{ display: "block", fontSize: "13px", fontWeight: "700", color: "#1e293b", marginBottom: "6px" }}>
                  Avg. Response Time (Minutes)
                </label>
                <input
                  type="number"
                  min="1"
                  max="120"
                  value={responseTime}
                  onChange={(e) => setResponseTime(e.target.value)}
                  style={{
                    width: "100%",
                    padding: "10px 14px",
                    borderRadius: "8px",
                    border: "1px solid #cbd5e1",
                    fontSize: "14px",
                    color: "#0f172a",
                    backgroundColor: "#ffffff",
                    boxSizing: "border-box",
                  }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: "13px", fontWeight: "700", color: "#1e293b", marginBottom: "6px" }}>
                  Coverage Area
                </label>
                <input
                  type="text"
                  placeholder="e.g. South Karachi, Clifton, Defence"
                  value={coverageArea}
                  onChange={(e) => setCoverageArea(e.target.value)}
                  style={{
                    width: "100%",
                    padding: "10px 14px",
                    borderRadius: "8px",
                    border: "1px solid #cbd5e1",
                    fontSize: "14px",
                    color: "#0f172a",
                    backgroundColor: "#ffffff",
                    boxSizing: "border-box",
                  }}
                />
              </div>
            </div>

            <div>
              <label style={{ display: "block", fontSize: "13px", fontWeight: "700", color: "#1e293b", marginBottom: "6px" }}>
                Fleet / Dispatch Notes
              </label>
              <textarea
                rows={2}
                placeholder="Driver emergency contact, dispatch center details..."
                value={ambulanceNotes}
                onChange={(e) => setAmbulanceNotes(e.target.value)}
                style={{
                  width: "100%",
                  padding: "10px 14px",
                  borderRadius: "8px",
                  border: "1px solid #cbd5e1",
                  fontSize: "14px",
                  color: "#0f172a",
                  backgroundColor: "#ffffff",
                  boxSizing: "border-box",
                }}
              />
            </div>
          </div>
        )}

        {/* Submit Button */}
        <div style={{ marginTop: "32px", display: "flex", justifyContent: "flex-end" }}>
          <button
            type="submit"
            disabled={loading}
            style={{
              backgroundColor: loading ? "#94a3b8" : "#dc2626",
              color: "#ffffff",
              padding: "12px 32px",
              borderRadius: "8px",
              border: "none",
              fontSize: "15px",
              fontWeight: "700",
              cursor: loading ? "not-allowed" : "pointer",
              boxShadow: "0 2px 8px rgba(220, 38, 38, 0.25)",
              transition: "all 0.2s ease",
            }}
          >
            {loading ? "Submitting Facility..." : "Submit Facility for Verification"}
          </button>
        </div>
      </form>
    </div>
  );
}
