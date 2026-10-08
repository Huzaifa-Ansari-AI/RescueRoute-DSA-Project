import { useEffect, useMemo, useState, useCallback } from "react";

import MapView from "../../components/map/MapView";
import CategoryFilter from "../../components/common/CategoryFilter";
import LocationSearchInput from "../../components/common/LocationSearchInput";
import DestinationSearchInput from "../../components/common/DestinationSearchInput";
import RankedFacilityCard from "../../components/facility/RankedFacilityCard";
import RouteSummary from "../../components/route/RouteSummary";
import Loading from "../../components/ui/Loading";
import ErrorMessage from "../../components/ui/ErrorMessage";
import facilityService from "../../services/facilityService";
import routeService from "../../services/routeService";
import useGeolocation from "../../hooks/useGeolocation";
import { facilities as fallbackFacilities } from "../../data/facilities";
import type { Facility, RankedFacility } from "../../types/facility";
import type { RouteInfo } from "../../types/route";

/**
 * Client-side Haversine distance and heuristic ranking used when backend is offline
 */
function computeClientSideRanking(
  list: Facility[],
  userLat: number,
  userLng: number,
  emergencyMode: boolean
): RankedFacility[] {
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const R = 6371; // Earth radius in km

  const ranked = list.map((f) => {
    const dLat = toRad(f.latitude - userLat);
    const dLng = toRad(f.longitude - userLng);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(toRad(userLat)) * Math.cos(toRad(f.latitude)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const dist = Math.round(R * c * 100) / 100;

    const maxDist = 50;
    const normDist = Math.max(0, 1 - dist / maxDist);
    const isVerif = f.isVerified ? 1 : 0;
    const isEmerg =
      f.type === "Ambulance" ||
      f.hospitalDetails?.emergencyAvailable ||
      f.pharmacyDetails?.open24Hours
        ? 1
        : 0;

    let score = 0.5 * normDist + 0.3 * isVerif + 0.2 * isEmerg;
    if (emergencyMode && isEmerg) {
      score = Math.min(1.0, score + 0.15);
    }
    score = Math.round(score * 1000) / 1000;

    return {
      rank: 1,
      distanceKm: dist,
      score,
      isVerified: Boolean(f.isVerified),
      emergencyAvailable: Boolean(isEmerg),
      facility: f,
    };
  });

  ranked.sort((a, b) => b.score - a.score || a.distanceKm - b.distanceKm);
  return ranked.map((item, idx) => ({ ...item, rank: idx + 1 }));
}

export default function HomePage() {
  const [facilities, setFacilities] = useState<Facility[]>([]);
  const [rankedFacilities, setRankedFacilities] = useState<RankedFacility[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [isOffline, setIsOffline] = useState<boolean>(false);
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("All");
  const [emergencyMode, setEmergencyMode] = useState<boolean>(false);
  const [selectedFacilityId, setSelectedFacilityId] = useState<string | null>(null);
  const [selectedDestination, setSelectedDestination] = useState<Facility | null>(null);
  const [originLocationName, setOriginLocationName] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState<number>(0);

  // OSRM Road Routing state
  const [route, setRoute] = useState<RouteInfo | null>(null);
  const [routeLoading, setRouteLoading] = useState<boolean>(false);
  const [routeError, setRouteError] = useState<string | null>(null);
  const [routingFacility, setRoutingFacility] = useState<Facility | null>(null);

  const {
    coordinates,
    loading: geoLoading,
    error: geoError,
    requestBrowserLocation,
    setManualLocation,
    clearLocation,
  } = useGeolocation();

  // Load facility directory or Top-K DSA ranking based on coordinates
  useEffect(() => {
    let isMounted = true;

    const applyFallback = () => {
      if (!isMounted) return;
      setIsOffline(true);
      setError(null);
      const filteredFallback = fallbackFacilities.filter(
        (f) => category === "All" || f.type === category
      );
      if (coordinates) {
        const localRanked = computeClientSideRanking(
          filteredFallback,
          coordinates.lat,
          coordinates.lng,
          emergencyMode
        );
        setRankedFacilities(localRanked);
        setFacilities(localRanked.map((r) => r.facility));
      } else {
        setRankedFacilities([]);
        setFacilities(filteredFallback);
      }
      setLoading(false);
    };

    if (coordinates) {
      facilityService
        .searchRankedFacilities({
          lat: coordinates.lat,
          lng: coordinates.lng,
          type: category !== "All" ? category : undefined,
          emergency: emergencyMode,
          k: 25,
        })
        .then((rankedList) => {
          if (isMounted) {
            setRankedFacilities(rankedList);
            setFacilities(rankedList.map((r) => r.facility));
            setIsOffline(false);
            setError(null);
            setLoading(false);
          }
        })
        .catch(() => {
          applyFallback();
        });
    } else {
      facilityService
        .getFacilities({
          type: category !== "All" ? category : undefined,
        })
        .then((data) => {
          if (isMounted) {
            setRankedFacilities([]);
            setFacilities(data);
            setIsOffline(false);
            setError(null);
            setLoading(false);
          }
        })
        .catch(() => {
          applyFallback();
        });
    }

    return () => {
      isMounted = false;
    };
  }, [coordinates, category, emergencyMode, reloadKey]);

  // Route calculation helper
  const calculateRoadRoute = useCallback(
    async (originCoords: { lat: number; lng: number }, destFacility: Facility) => {
      setRouteLoading(true);
      setRouteError(null);
      setRoutingFacility(destFacility);
      setSelectedFacilityId(destFacility.id);

      try {
        const routeData = await routeService.calculateRoute(
          originCoords,
          { lat: destFacility.latitude, lng: destFacility.longitude },
          destFacility.id,
          destFacility.name
        );
        setRoute(routeData);
        setRouteError(null);
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : "Failed to calculate road route.";
        setRouteError(message);
        setRoute(null);
      } finally {
        setRouteLoading(false);
      }
    },
    []
  );

  // Recalculate route when origin or destination changes asynchronously
  useEffect(() => {
    if (coordinates && selectedDestination) {
      const timer = setTimeout(() => {
        calculateRoadRoute(coordinates, selectedDestination);
      }, 0);
      return () => clearTimeout(timer);
    }
  }, [coordinates, selectedDestination, calculateRoadRoute]);

  // Point A: Select origin location (from search autocomplete, GPS, or manual)
  const handleSelectOrigin = useCallback(
    (lat: number, lng: number, placeName: string) => {
      setOriginLocationName(placeName);
      setManualLocation(lat, lng, "manual");
    },
    [setManualLocation]
  );

  // Point A: Geolocation GPS request
  const handleRequestBrowserLocation = useCallback(() => {
    setOriginLocationName("Current GPS Location");
    requestBrowserLocation();
  }, [requestBrowserLocation]);

  // Point A: Clear origin
  const handleClearOrigin = useCallback(() => {
    setOriginLocationName(null);
    clearLocation();
    setRoute(null);
    setRouteError(null);
    routeService.cancelPendingRequest();
  }, [clearLocation]);

  // Point B: Select Destination
  const handleSelectDestination = useCallback(
    (dest: Facility) => {
      setSelectedDestination(dest);
      setSelectedFacilityId(dest.id);
      if (coordinates) {
        calculateRoadRoute(coordinates, dest);
      } else {
        setRouteError("Select or detect 'Your Location' (Point A) to calculate the driving route.");
      }
    },
    [coordinates, calculateRoadRoute]
  );

  // Point B: Clear Destination
  const handleClearDestination = useCallback(() => {
    setSelectedDestination(null);
    setRoutingFacility(null);
    setRoute(null);
    setRouteError(null);
    routeService.cancelPendingRequest();
  }, []);

  // Direction handler from Ranked Card or Facility Card
  const handleGetDirections = useCallback(
    (rf: RankedFacility) => {
      handleSelectDestination(rf.facility);
    },
    [handleSelectDestination]
  );

  const handleClearRoute = useCallback(() => {
    setRoute(null);
    setRouteError(null);
    setSelectedDestination(null);
    setRoutingFacility(null);
    routeService.cancelPendingRequest();
  }, []);

  const handleRetryRoute = useCallback(() => {
    if (coordinates && routingFacility) {
      calculateRoadRoute(coordinates, routingFacility);
    }
  }, [coordinates, routingFacility, calculateRoadRoute]);

  const handleRetry = () => {
    setLoading(true);
    setError(null);
    setReloadKey((k) => k + 1);
  };

  // Filter ranked facilities by quick search query
  const filteredRanked = useMemo(() => {
    if (!coordinates || rankedFacilities.length === 0) return [];
    if (!search.trim()) return rankedFacilities;
    const q = search.toLowerCase();
    return rankedFacilities.filter((rf) => {
      const f = rf.facility;
      return (
        f.name.toLowerCase().includes(q) ||
        f.address.toLowerCase().includes(q) ||
        f.type.toLowerCase().includes(q)
      );
    });
  }, [coordinates, rankedFacilities, search]);

  // Filter unranked facilities by quick search query
  const filteredUnranked = useMemo(() => {
    if (!search.trim()) return facilities;
    const q = search.toLowerCase();
    return facilities.filter((f) => {
      return (
        f.name.toLowerCase().includes(q) ||
        f.address.toLowerCase().includes(q) ||
        f.type.toLowerCase().includes(q)
      );
    });
  }, [facilities, search]);

  // Facilities array to render on the map
  const mapFacilities = useMemo(() => {
    if (coordinates && filteredRanked.length > 0) {
      return filteredRanked.map((r) => r.facility);
    }
    return filteredUnranked;
  }, [coordinates, filteredRanked, filteredUnranked]);

  const isRankedMode = Boolean(coordinates && filteredRanked.length > 0);

  return (
    <div style={{ maxWidth: "1500px", margin: "0 auto", padding: "16px 20px" }}>
      {/* Header Banner */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          flexWrap: "wrap",
          gap: "12px",
          marginBottom: "16px",
        }}
      >
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <h1
              style={{
                margin: 0,
                fontSize: "26px",
                fontWeight: 800,
                color: "#0f172a",
                letterSpacing: "-0.02em",
              }}
            >
              🚑 RescueRoute
            </h1>
            <span
              style={{
                backgroundColor: "#eff6ff",
                color: "#2563eb",
                padding: "3px 10px",
                borderRadius: "20px",
                fontSize: "12px",
                fontWeight: 700,
                border: "1px solid #bfdbfe",
              }}
            >
              Karachi Emergency Network
            </span>
          </div>
          <p style={{ margin: "4px 0 0 0", color: "#64748b", fontSize: "14px" }}>
            Human-readable location search, DSA Top-K Min-Heap Ranking, and OSRM Road Navigation.
          </p>
        </div>

        <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
          {isOffline ? (
            <span
              style={{
                backgroundColor: "#fef3c7",
                color: "#92400e",
                padding: "4px 12px",
                borderRadius: "20px",
                fontSize: "12px",
                fontWeight: 600,
                border: "1px solid #fde68a",
              }}
            >
              🟡 Offline Demo Mode
            </span>
          ) : (
            <span
              style={{
                backgroundColor: "#dcfce7",
                color: "#15803d",
                padding: "4px 12px",
                borderRadius: "20px",
                fontSize: "12px",
                fontWeight: 600,
                border: "1px solid #bbf7d0",
              }}
            >
              🟢 Backend Live (Port 8080)
            </span>
          )}
        </div>
      </div>

      {/* Google Maps-Style Navigation Hub */}
      <div
        style={{
          backgroundColor: "#ffffff",
          borderRadius: "16px",
          border: "1px solid #e2e8f0",
          boxShadow: "0 4px 20px rgba(0, 0, 0, 0.06)",
          padding: "20px",
          marginBottom: "20px",
        }}
      >
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "20px" }}>
          {/* Point A: Origin Search */}
          <LocationSearchInput
            currentCoordinates={coordinates}
            locationName={originLocationName}
            loadingGps={geoLoading}
            gpsError={geoError}
            onRequestGps={handleRequestBrowserLocation}
            onSelectLocation={handleSelectOrigin}
            onClearLocation={handleClearOrigin}
            emergencyMode={emergencyMode}
            onToggleEmergency={setEmergencyMode}
          />

          {/* Point B: Destination Search */}
          <DestinationSearchInput
            facilities={facilities}
            selectedFacility={selectedDestination}
            originCoordinates={coordinates}
            onSelectDestination={handleSelectDestination}
            onClearDestination={handleClearDestination}
          />
        </div>

        {/* Categories Bar & Quick Emergency Mode Indicator */}
        <div
          style={{
            marginTop: "16px",
            paddingTop: "14px",
            borderTop: "1px solid #f1f5f9",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            flexWrap: "wrap",
            gap: "12px",
          }}
        >
          <div style={{ flex: 1, minWidth: "280px" }}>
            <CategoryFilter value={category} onChange={setCategory} />
          </div>

          {selectedDestination && (
            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              <span
                style={{
                  fontSize: "13px",
                  color: "#166534",
                  fontWeight: 600,
                  backgroundColor: "#f0fdf4",
                  padding: "4px 10px",
                  borderRadius: "6px",
                  border: "1px solid #bbf7d0",
                }}
              >
                🎯 Destination: {selectedDestination.name}
              </span>
              <button
                type="button"
                onClick={handleClearDestination}
                style={{
                  backgroundColor: "#fee2e2",
                  color: "#dc2626",
                  border: "none",
                  padding: "4px 8px",
                  borderRadius: "6px",
                  fontSize: "12px",
                  fontWeight: 600,
                  cursor: "pointer",
                }}
              >
                Clear Route
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Offline Alert if Backend is Unreachable */}
      {isOffline && (
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            backgroundColor: "#fffbeb",
            border: "1px solid #fef3c7",
            color: "#92400e",
            padding: "12px 18px",
            borderRadius: "10px",
            marginBottom: "16px",
            fontSize: "14px",
            gap: "12px",
            flexWrap: "wrap",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <span style={{ fontSize: "18px" }}>⚠️</span>
            <span>
              <strong>Offline Demo Mode:</strong> Backend server at port 8080 is offline. Displaying
              Karachi emergency healthcare facilities with client-side DSA distance &amp; ranking.
            </span>
          </div>
          <button
            type="button"
            onClick={handleRetry}
            style={{
              backgroundColor: "#d97706",
              color: "#ffffff",
              border: "none",
              padding: "7px 14px",
              borderRadius: "6px",
              fontSize: "13px",
              fontWeight: 600,
              cursor: "pointer",
            }}
          >
            Retry Live Connection
          </button>
        </div>
      )}

      {/* Error Message */}
      {error && !loading && !isOffline && (
        <ErrorMessage
          title="Could not retrieve emergency facilities"
          message={error}
          onRetry={handleRetry}
        />
      )}

      {/* Active Route Summary if route or route calculation is ongoing */}
      {(route || routeLoading || routeError) && (
        <div style={{ marginBottom: "16px" }}>
          <RouteSummary
            route={route}
            loading={routeLoading}
            error={routeError}
            targetFacilityName={selectedDestination?.name || routingFacility?.name}
            onClearRoute={handleClearRoute}
            onRetry={handleRetryRoute}
          />
        </div>
      )}

      {/* Main Content Area: Split View */}
      {loading ? (
        <Loading message="Computing distances and priority rankings..." />
      ) : (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "minmax(360px, 440px) 1fr",
            gap: "20px",
            alignItems: "start",
          }}
        >
          {/* Left Column: Facilities List & Quick Filter */}
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              gap: "12px",
            }}
          >
            {/* List Header & Quick Filter */}
            <div
              style={{
                backgroundColor: "#ffffff",
                padding: "14px 16px",
                borderRadius: "12px",
                border: "1px solid #e2e8f0",
                boxShadow: "0 2px 6px rgba(0,0,0,0.03)",
              }}
            >
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: "10px",
                }}
              >
                <div>
                  <h3 style={{ margin: 0, fontSize: "16px", fontWeight: 700, color: "#0f172a" }}>
                    {isRankedMode ? "🎯 Ranked Facilities" : "📋 Karachi Facilities"}
                  </h3>
                  <div style={{ fontSize: "12px", color: "#64748b", marginTop: "2px" }}>
                    {isRankedMode
                      ? `Top-${filteredRanked.length} sorted by Min-Heap Score & Distance`
                      : `${filteredUnranked.length} facilities in directory`}
                  </div>
                </div>

                {isRankedMode && (
                  <span
                    style={{
                      fontSize: "11px",
                      color: "#0369a1",
                      backgroundColor: "#e0f2fe",
                      padding: "3px 8px",
                      borderRadius: "6px",
                      fontWeight: 700,
                    }}
                  >
                    DSA O(N log K)
                  </span>
                )}
              </div>

              {/* Quick filter input */}
              <input
                type="text"
                placeholder="Filter by facility name or area..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                style={{
                  width: "100%",
                  padding: "8px 12px",
                  borderRadius: "8px",
                  border: "1px solid #cbd5e1",
                  fontSize: "13px",
                  color: "#0f172a",
                  backgroundColor: "#ffffff",
                  outline: "none",
                  boxSizing: "border-box",
                }}
              />
            </div>

            {/* Prompt to set Point A if coordinates are not set */}
            {!coordinates && (
              <div
                style={{
                  backgroundColor: "#f8fafc",
                  border: "1px dashed #cbd5e1",
                  borderRadius: "12px",
                  padding: "14px 16px",
                  fontSize: "13px",
                  color: "#475569",
                  lineHeight: "1.5",
                }}
              >
                <strong style={{ color: "#1e293b" }}>💡 Want real-time distances &amp; ranking?</strong>
                <p style={{ margin: "4px 0 0 0" }}>
                  Search your Karachi area (e.g. <em>Nazimabad</em>, <em>Clifton</em>, <em>Saddar</em>)
                  or click <strong>Use GPS</strong> in <em>Point A</em> above to activate the DSA Priority
                  Ranking Engine.
                </p>
              </div>
            )}

            {/* Scrollable Facility Cards */}
            <div
              style={{
                maxHeight: "680px",
                overflowY: "auto",
                paddingRight: "6px",
              }}
            >
              {isRankedMode ? (
                filteredRanked.length > 0 ? (
                  filteredRanked.map((rf) => (
                    <RankedFacilityCard
                      key={rf.facility.id}
                      rankedFacility={rf}
                      isSelected={selectedFacilityId === rf.facility.id}
                      hasActiveRoute={route?.destinationFacilityId === rf.facility.id}
                      isRoutingThis={
                        routeLoading && routingFacility?.id === rf.facility.id
                      }
                      onSelect={(item) => setSelectedFacilityId(item.facility.id)}
                      onGetDirections={handleGetDirections}
                    />
                  ))
                ) : (
                  <div
                    style={{
                      padding: "30px 16px",
                      textAlign: "center",
                      color: "#64748b",
                      fontSize: "14px",
                    }}
                  >
                    No facilities found matching &quot;{search}&quot;.
                  </div>
                )
              ) : filteredUnranked.length > 0 ? (
                filteredUnranked.map((facility) => {
                  const isSelected = selectedFacilityId === facility.id;
                  const isDestination = selectedDestination?.id === facility.id;
                  return (
                    <div
                      key={facility.id}
                      onClick={() => setSelectedFacilityId(facility.id)}
                      style={{
                        backgroundColor: "#ffffff",
                        borderRadius: "12px",
                        border: isDestination
                          ? "2px solid #16a34a"
                          : isSelected
                          ? "2px solid #2563eb"
                          : "1px solid #e2e8f0",
                        padding: "16px",
                        marginBottom: "12px",
                        boxShadow: isDestination
                          ? "0 4px 14px rgba(22, 163, 74, 0.15)"
                          : "0 2px 6px rgba(0,0,0,0.04)",
                        cursor: "pointer",
                        transition: "all 0.2s ease",
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "flex-start",
                          marginBottom: "8px",
                          gap: "8px",
                        }}
                      >
                        <h4
                          style={{
                            margin: 0,
                            fontSize: "15px",
                            fontWeight: 700,
                            color: "#0f172a",
                          }}
                        >
                          {facility.name}
                        </h4>
                        <span
                          style={{
                            padding: "2px 8px",
                            borderRadius: "4px",
                            fontSize: "11px",
                            fontWeight: 600,
                            backgroundColor:
                              facility.type === "Hospital"
                                ? "#eff6ff"
                                : facility.type === "Blood Bank"
                                ? "#fef2f2"
                                : facility.type === "Pharmacy"
                                ? "#f0fdf4"
                                : "#fffbeb",
                            color:
                              facility.type === "Hospital"
                                ? "#1d4ed8"
                                : facility.type === "Blood Bank"
                                ? "#b91c1c"
                                : facility.type === "Pharmacy"
                                ? "#15803d"
                                : "#b45309",
                          }}
                        >
                          {facility.type}
                        </span>
                      </div>

                      <p
                        style={{
                          margin: "0 0 10px 0",
                          fontSize: "13px",
                          color: "#475569",
                        }}
                      >
                        📍 {facility.address}, {facility.city}
                      </p>

                      <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center",
                          paddingTop: "10px",
                          borderTop: "1px solid #f1f5f9",
                        }}
                      >
                        {facility.phoneNumber ? (
                          <a
                            href={`tel:${facility.phoneNumber}`}
                            onClick={(e) => e.stopPropagation()}
                            style={{
                              fontSize: "12px",
                              color: "#16a34a",
                              fontWeight: 600,
                              textDecoration: "none",
                            }}
                          >
                            📞 {facility.phoneNumber}
                          </a>
                        ) : (
                          <span style={{ fontSize: "12px", color: "#94a3b8" }}>No phone listed</span>
                        )}

                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleSelectDestination(facility);
                          }}
                          style={{
                            backgroundColor: isDestination ? "#16a34a" : "#2563eb",
                            color: "#ffffff",
                            border: "none",
                            borderRadius: "6px",
                            padding: "6px 12px",
                            fontSize: "12px",
                            fontWeight: 600,
                            cursor: "pointer",
                          }}
                        >
                          {isDestination ? "🎯 Active Destination" : "🚗 Get Directions"}
                        </button>
                      </div>
                    </div>
                  );
                })
              ) : (
                <div
                  style={{
                    padding: "30px 16px",
                    textAlign: "center",
                    color: "#64748b",
                    fontSize: "14px",
                  }}
                >
                  No facilities found matching &quot;{search}&quot;.
                </div>
              )}
            </div>
          </div>

          {/* Right Column: Google Maps Style Leaflet Map */}
          <div>
            <MapView
              facilities={mapFacilities}
              userLocation={coordinates}
              selectedFacilityId={selectedFacilityId}
              route={route}
              height="720px"
            />
          </div>
        </div>
      )}
    </div>
  );
}