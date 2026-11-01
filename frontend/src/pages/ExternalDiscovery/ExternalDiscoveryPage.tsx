import React, { useState, useEffect } from "react";
import axios from "axios";
import { ExternalMapView } from "../../components/map/ExternalMapView";
import { externalFacilityService } from "../../services/externalFacilityService";
import type {
  ExternalFacility,
  ExternalImportSummary,
} from "../../types/externalFacility";
import { useAuth } from "../../hooks/useAuth";

const DEFAULT_LAT = 24.8607;
const DEFAULT_LNG = 67.0011;

const CATEGORIES = ["All", "Hospital", "Clinic", "Pharmacy", "Blood Bank", "Ambulance"];

export const ExternalDiscoveryPage: React.FC = () => {
  const { user } = useAuth();
  const canImport = user?.role === "ADMIN" || user?.role === "PROVIDER";

  // Coordinates and query filters
  const [lat, setLat] = useState<number>(DEFAULT_LAT);
  const [lng, setLng] = useState<number>(DEFAULT_LNG);
  const [radiusKm, setRadiusKm] = useState<number>(5.0);
  const [selectedCategory, setSelectedCategory] = useState<string>("All");
  const [selectedStatus, setSelectedStatus] = useState<string>("All");

  // State
  const [facilities, setFacilities] = useState<ExternalFacility[]>([]);
  const [selectedFacilityId, setSelectedFacilityId] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);

  // Import state
  const [importing, setImporting] = useState<boolean>(false);
  const [importSummary, setImportSummary] = useState<ExternalImportSummary | null>(null);
  const [importError, setImportError] = useState<string | null>(null);
  const [showImportModal, setShowImportModal] = useState<boolean>(false);

  // Acquire browser geolocation if available
  const handleUseMyLocation = () => {
    if (!navigator.geolocation) {
      alert("Geolocation is not supported by your browser.");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const uLat = Number(pos.coords.latitude.toFixed(6));
        const uLng = Number(pos.coords.longitude.toFixed(6));
        setUserLocation({ lat: uLat, lng: uLng });
        setLat(uLat);
        setLng(uLng);
      },
      (err) => {
        console.warn("Unable to fetch user location:", err.message);
      }
    );
  };

  const [reloadTrigger, setReloadTrigger] = useState<number>(0);

  // Search external facilities
  useEffect(() => {
    let isMounted = true;
    const radiusMeters = Math.round(radiusKm * 1000);

    externalFacilityService.searchExternalFacilities({
      lat,
      lng,
      radiusMeters,
      type: selectedCategory !== "All" ? selectedCategory : undefined,
      status: selectedStatus !== "All" ? selectedStatus : undefined,
    })
      .then((data) => {
        if (isMounted) {
          setFacilities(data);
          setLoading(false);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          let message = "Failed to load external facilities.";
          if (axios.isAxiosError(err) && err.response?.data?.message) {
            message = err.response.data.message;
          } else if (err instanceof Error) {
            message = err.message;
          }
          setError(message);
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [lat, lng, radiusKm, selectedCategory, selectedStatus, reloadTrigger]);

  const handleManualSearch = () => {
    setLoading(true);
    setError(null);
    setReloadTrigger((prev) => prev + 1);
  };

  // Handle map click to reposition search origin
  const handleMapClick = (clickLat: number, clickLng: number) => {
    setLat(Number(clickLat.toFixed(6)));
    setLng(Number(clickLng.toFixed(6)));
  };

  // Execute Overpass Import
  const handleTriggerImport = async () => {
    if (importing) return; // Prevent duplicate concurrent requests
    setImporting(true);
    setImportError(null);
    setImportSummary(null);
    try {
      const summary = await externalFacilityService.importFacilities({
        latitude: lat,
        longitude: lng,
        radiusMeters: Math.round(radiusKm * 1000),
        facilityType: selectedCategory !== "All" ? selectedCategory : undefined,
      });
      setImportSummary(summary);
      // Refresh list to include newly imported records
      setReloadTrigger((prev) => prev + 1);
    } catch (err: unknown) {
      let msg = "External import from OpenStreetMap Overpass failed.";
      if (axios.isAxiosError(err)) {
        if (err.response?.data?.message) {
          msg = err.response.data.message;
        } else if (err.code === "ECONNABORTED" || err.message?.toLowerCase().includes("timeout")) {
          msg = "Import timed out waiting for OpenStreetMap Overpass. Please try a smaller search radius (e.g. 2–3 km) or retry.";
        } else if (err.response?.status === 504 || err.response?.status === 502) {
          msg = "OpenStreetMap Overpass service is temporarily overloaded or unavailable (HTTP " + err.response.status + "). Please retry shortly.";
        } else if (err.response?.status === 429) {
          msg = "OpenStreetMap Overpass rate limit reached. Please wait a moment before trying again.";
        } else if (err.message) {
          msg = err.message;
        }
      } else if (err instanceof Error) {
        msg = err.message;
      }
      setImportError(msg);
    } finally {
      setImporting(false);
    }
  };

  return (
    <div style={{ maxWidth: "1400px", margin: "0 auto", padding: "20px 24px", fontFamily: "system-ui, sans-serif" }}>
      {/* Page Header & External Notice Banner */}
      <div style={{
        background: "linear-gradient(135deg, #0e7490 0%, #155e75 100%)",
        color: "#ffffff",
        borderRadius: "16px",
        padding: "24px 28px",
        marginBottom: "24px",
        boxShadow: "0 4px 14px rgba(14, 116, 144, 0.2)",
      }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "16px" }}>
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "6px" }}>
              <span style={{ fontSize: "24px" }}>🌐</span>
              <h1 style={{ margin: 0, fontSize: "24px", fontWeight: 800, letterSpacing: "-0.5px" }}>
                OpenStreetMap Medical Facility Discovery
              </h1>
            </div>
            <p style={{ margin: "0 0 12px 0", fontSize: "14px", color: "#cffafe", maxWidth: "800px", lineHeight: 1.5 }}>
              Discover crowd-sourced healthcare points of interest from OpenStreetMap via the Overpass API.
              External listings are stored separately from RescueRoute verified clinical facilities.
            </p>
            <div style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "6px",
              backgroundColor: "rgba(255, 255, 255, 0.15)",
              border: "1px solid rgba(255, 255, 255, 0.25)",
              borderRadius: "8px",
              padding: "4px 10px",
              fontSize: "12px",
              color: "#f0fdfa",
            }}>
              <span>⚠️</span>
              <span><strong>Notice:</strong> External listings are unverified. Operating status and medical licensing are not guaranteed.</span>
            </div>
          </div>

          {canImport && (
            <button
              onClick={() => setShowImportModal(true)}
              disabled={importing}
              style={{
                backgroundColor: "#f59e0b",
                color: "#78350f",
                fontWeight: 700,
                fontSize: "13px",
                padding: "10px 18px",
                borderRadius: "10px",
                border: "none",
                cursor: "pointer",
                boxShadow: "0 2px 8px rgba(0,0,0,0.15)",
                display: "flex",
                alignItems: "center",
                gap: "8px",
                transition: "all 0.2s ease",
              }}
            >
              <span>📥</span>
              <span>{importing ? "Importing Area..." : "Import from OpenStreetMap"}</span>
            </button>
          )}
        </div>
      </div>

      {/* Filter and Control Bar */}
      <div style={{
        backgroundColor: "#ffffff",
        borderRadius: "12px",
        padding: "16px 20px",
        marginBottom: "20px",
        border: "1px solid #e2e8f0",
        boxShadow: "0 2px 6px rgba(0,0,0,0.03)",
        display: "flex",
        flexDirection: "column",
        gap: "14px",
      }}>
        {/* Top Controls: Search Origin Coordinates & Radius Slider */}
        <div style={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "16px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
            <label style={{ fontSize: "13px", fontWeight: 600, color: "#334155" }}>Center:</label>
            <input
              type="number"
              step="0.0001"
              value={lat}
              onChange={(e) => setLat(parseFloat(e.target.value) || 0)}
              style={{ width: "95px", padding: "6px 8px", fontSize: "13px", borderRadius: "6px", border: "1px solid #cbd5e1" }}
              title="Latitude"
            />
            <input
              type="number"
              step="0.0001"
              value={lng}
              onChange={(e) => setLng(parseFloat(e.target.value) || 0)}
              style={{ width: "95px", padding: "6px 8px", fontSize: "13px", borderRadius: "6px", border: "1px solid #cbd5e1" }}
              title="Longitude"
            />
            <button
              onClick={handleUseMyLocation}
              style={{
                backgroundColor: "#f1f5f9",
                color: "#1e293b",
                border: "1px solid #cbd5e1",
                borderRadius: "6px",
                padding: "6px 12px",
                fontSize: "12px",
                fontWeight: 600,
                cursor: "pointer",
              }}
            >
              📍 My Location
            </button>
          </div>

          {/* Radius Slider */}
          <div style={{ display: "flex", alignItems: "center", gap: "10px", flexGrow: 1, minWidth: "220px" }}>
            <label style={{ fontSize: "13px", fontWeight: 600, color: "#334155", whiteSpace: "nowrap" }}>
              Radius: {radiusKm} km
            </label>
            <input
              type="range"
              min="1"
              max="10"
              step="0.5"
              value={radiusKm}
              onChange={(e) => setRadiusKm(parseFloat(e.target.value))}
              style={{ flexGrow: 1, accentColor: "#0891b2" }}
            />
          </div>

          {/* Status Filter */}
          <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
            <label style={{ fontSize: "13px", fontWeight: 600, color: "#334155" }}>Status:</label>
            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              style={{ padding: "6px 10px", fontSize: "13px", borderRadius: "6px", border: "1px solid #cbd5e1" }}
            >
              <option value="All">All Statuses</option>
              <option value="DISCOVERED">Discovered</option>
              <option value="DUPLICATE_CANDIDATE">Duplicate Candidate</option>
              <option value="REVIEWED">Reviewed</option>
            </select>
          </div>

          <button
            onClick={handleManualSearch}
            disabled={loading}
            style={{
              backgroundColor: "#0891b2",
              color: "#ffffff",
              border: "none",
              borderRadius: "6px",
              padding: "7px 16px",
              fontSize: "13px",
              fontWeight: 600,
              cursor: "pointer",
            }}
          >
            {loading ? "Searching..." : "Search"}
          </button>
        </div>

        {/* Category Tabs */}
        <div style={{ display: "flex", flexWrap: "wrap", gap: "8px" }}>
          {CATEGORIES.map((cat) => {
            const isSelected = selectedCategory === cat;
            return (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                style={{
                  padding: "6px 12px",
                  borderRadius: "20px",
                  fontSize: "12px",
                  fontWeight: 600,
                  border: isSelected ? "1px solid #0891b2" : "1px solid #e2e8f0",
                  backgroundColor: isSelected ? "#ecfeff" : "#ffffff",
                  color: isSelected ? "#0e7490" : "#475569",
                  cursor: "pointer",
                  transition: "all 0.15s ease",
                }}
              >
                {cat}
              </button>
            );
          })}
        </div>
      </div>

      {/* Main Content Layout: Map + Facility List */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 420px", gap: "20px", alignItems: "start" }}>
        {/* Map View */}
        <div>
          <ExternalMapView
            facilities={facilities}
            userLocation={userLocation}
            searchCenter={{ lat, lng }}
            searchRadiusMeters={Math.round(radiusKm * 1000)}
            selectedFacilityId={selectedFacilityId}
            onSelectFacility={(fac) => setSelectedFacilityId(fac.id)}
            onMapClick={handleMapClick}
            height="640px"
          />
          <div style={{ fontSize: "11px", color: "#64748b", marginTop: "8px", display: "flex", justifyContent: "space-between" }}>
            <span>💡 Click anywhere on the map to reposition the search origin.</span>
            <span>
              Map data &copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer" style={{ color: "#0891b2" }}>OpenStreetMap contributors</a> (ODbL)
            </span>
          </div>
        </div>

        {/* Results Sidebar */}
        <div style={{
          backgroundColor: "#ffffff",
          borderRadius: "12px",
          border: "1px solid #e2e8f0",
          padding: "16px",
          maxHeight: "640px",
          overflowY: "auto",
        }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "14px" }}>
            <h3 style={{ margin: 0, fontSize: "16px", fontWeight: 700, color: "#1e293b" }}>
              Discovered Facilities ({facilities.length})
            </h3>
            {loading && <span style={{ fontSize: "12px", color: "#0891b2" }}>Loading...</span>}
          </div>

          {error && (
            <div style={{
              backgroundColor: "#fef2f2",
              border: "1px solid #fecaca",
              borderRadius: "8px",
              padding: "10px 12px",
              color: "#991b1b",
              fontSize: "12px",
              marginBottom: "12px",
            }}>
              {error}
            </div>
          )}

          {facilities.length === 0 && !loading && !error && (
            <div style={{ textAlign: "center", padding: "40px 16px", color: "#64748b" }}>
              <div style={{ fontSize: "32px", marginBottom: "8px" }}>🔍</div>
              <div style={{ fontWeight: 600, fontSize: "14px" }}>No external POIs found</div>
              <div style={{ fontSize: "12px", marginTop: "4px" }}>
                Try expanding the search radius or {canImport ? "triggering an import from OpenStreetMap above." : "selecting another location."}
              </div>
            </div>
          )}

          {/* Facility Cards */}
          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            {facilities.map((facility) => {
              const isSelected = selectedFacilityId === facility.id;
              const isDuplicate = facility.importStatus === "DUPLICATE_CANDIDATE";

              return (
                <div
                  key={facility.id}
                  onClick={() => setSelectedFacilityId(facility.id)}
                  style={{
                    padding: "12px",
                    borderRadius: "8px",
                    border: isSelected ? "2px solid #0891b2" : "1px solid #e2e8f0",
                    backgroundColor: isSelected ? "#f0fdfa" : "#ffffff",
                    cursor: "pointer",
                    transition: "all 0.15s ease",
                  }}
                >
                  {/* Category & Status */}
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "6px" }}>
                    <span style={{
                      backgroundColor: "#ecfeff",
                      color: "#0e7490",
                      padding: "2px 6px",
                      borderRadius: "4px",
                      fontSize: "11px",
                      fontWeight: 700,
                    }}>
                      {facility.facilityType}
                    </span>

                    {facility.distanceKm !== undefined && (
                      <span style={{ fontSize: "12px", color: "#0891b2", fontWeight: 600 }}>
                        {facility.distanceKm.toFixed(2)} km
                      </span>
                    )}
                  </div>

                  {/* Name */}
                  <div style={{ fontWeight: 700, fontSize: "14px", color: "#0f172a", marginBottom: "4px" }}>
                    {facility.name}
                  </div>

                  {/* Address */}
                  {facility.address && (
                    <div style={{ fontSize: "11px", color: "#64748b", marginBottom: "4px" }}>
                      📍 {facility.address}
                    </div>
                  )}

                  {/* Phone */}
                  {facility.phoneNumber && (
                    <div style={{ fontSize: "11px", color: "#166534", marginBottom: "4px" }}>
                      📞 {facility.phoneNumber}
                    </div>
                  )}

                  {/* Duplicate Candidate Banner */}
                  {isDuplicate && (
                    <div style={{
                      backgroundColor: "#fffbeb",
                      border: "1px solid #fde68a",
                      borderRadius: "6px",
                      padding: "4px 8px",
                      fontSize: "11px",
                      color: "#854d0e",
                      marginTop: "6px",
                    }}>
                      ⚠️ Possible internal duplicate
                      {facility.matchedFacilityName && (
                        <span>: <strong>{facility.matchedFacilityName}</strong></span>
                      )}
                    </div>
                  )}

                  {/* Unverified Disclaimer */}
                  <div style={{
                    fontSize: "10px",
                    color: "#991b1b",
                    marginTop: "6px",
                    lineHeight: 1.2,
                  }}>
                    * External listing · Not verified by RescueRoute
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Import Modal */}
      {showImportModal && (
        <div style={{
          position: "fixed",
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: "rgba(0, 0, 0, 0.5)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          zIndex: 2000,
          padding: "16px",
        }}>
          <div style={{
            backgroundColor: "#ffffff",
            borderRadius: "14px",
            padding: "24px",
            maxWidth: "500px",
            width: "100%",
            boxShadow: "0 10px 25px rgba(0,0,0,0.2)",
          }}>
            <h3 style={{ margin: "0 0 12px 0", fontSize: "18px", fontWeight: 700, color: "#0f172a" }}>
              Import Medical Facilities from OpenStreetMap
            </h3>

            <p style={{ fontSize: "13px", color: "#475569", lineHeight: 1.5, margin: "0 0 16px 0" }}>
              This will query the public Overpass API for medical facilities within <strong>{radiusKm} km</strong> of (
              {lat.toFixed(4)}, {lng.toFixed(4)}), normalize the categories, and detect potential duplicates against existing RescueRoute facilities.
            </p>

            <div style={{
              backgroundColor: "#f8fafc",
              border: "1px solid #e2e8f0",
              borderRadius: "8px",
              padding: "12px",
              fontSize: "12px",
              color: "#334155",
              marginBottom: "16px",
            }}>
              <div><strong>Category:</strong> {selectedCategory}</div>
              <div><strong>Coordinates:</strong> {lat}, {lng}</div>
              <div><strong>Radius:</strong> {radiusKm} km ({Math.round(radiusKm * 1000)} meters)</div>
            </div>

            {importError && (
              <div style={{
                backgroundColor: "#fef2f2",
                border: "1px solid #fecaca",
                borderRadius: "8px",
                padding: "10px",
                fontSize: "12px",
                color: "#991b1b",
                marginBottom: "14px",
              }}>
                {importError}
              </div>
            )}

            {importSummary && (
              <div style={{
                backgroundColor: importSummary.totalQueried === 0 ? "#eff6ff" : "#f0fdf4",
                border: importSummary.totalQueried === 0 ? "1px solid #bfdbfe" : "1px solid #bbf7d0",
                borderRadius: "8px",
                padding: "12px",
                fontSize: "12px",
                color: importSummary.totalQueried === 0 ? "#1e40af" : "#166534",
                marginBottom: "14px",
              }}>
                <div style={{ fontWeight: 700, marginBottom: "4px" }}>
                  {importSummary.totalQueried === 0 ? "ℹ️ No Medical Facilities Found" : "✅ Import Complete!"}
                </div>
                {importSummary.message && (
                  <div style={{ marginBottom: "6px", lineHeight: 1.4 }}>
                    {importSummary.message}
                  </div>
                )}
                <div>Total Queried: <strong>{importSummary.totalQueried}</strong></div>
                <div>New Imported: <strong>{importSummary.imported}</strong></div>
                <div>Existing Updated: <strong>{importSummary.updated}</strong></div>
                <div>Duplicate Candidates Flagged: <strong>{importSummary.duplicateCandidates}</strong></div>
                <div>Execution Time: <strong>{importSummary.executionTimeMs} ms</strong></div>
              </div>
            )}

            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px" }}>
              <button
                onClick={() => {
                  setShowImportModal(false);
                  setImportSummary(null);
                  setImportError(null);
                }}
                disabled={importing}
                style={{
                  padding: "8px 16px",
                  borderRadius: "6px",
                  border: "1px solid #cbd5e1",
                  backgroundColor: "#ffffff",
                  color: "#475569",
                  fontSize: "13px",
                  fontWeight: 600,
                  cursor: "pointer",
                }}
              >
                Close
              </button>

              {!importSummary && (
                <button
                  onClick={handleTriggerImport}
                  disabled={importing}
                  style={{
                    padding: "8px 18px",
                    borderRadius: "6px",
                    border: "none",
                    backgroundColor: "#0891b2",
                    color: "#ffffff",
                    fontSize: "13px",
                    fontWeight: 700,
                    cursor: "pointer",
                  }}
                >
                  {importing ? "Querying Overpass..." : "Confirm & Import"}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ExternalDiscoveryPage;
