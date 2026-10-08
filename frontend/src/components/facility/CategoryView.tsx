import { useEffect, useMemo, useState } from "react";
import type { Facility, FacilityType } from "../../types/facility";
import facilityService from "../../services/facilityService";
import { facilities as fallbackFacilities } from "../../data/facilities";
import Loading from "../ui/Loading";
import ErrorMessage from "../ui/ErrorMessage";
import FacilityList from "./FacilityList";
import SearchBar from "../common/SearchBar";

interface CategoryViewProps {
  title: string;
  facilityType: FacilityType;
  icon: string;
  description: string;
}

export default function CategoryView({
  title,
  facilityType,
  icon,
  description,
}: CategoryViewProps) {
  const [facilities, setFacilities] = useState<Facility[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [isOffline, setIsOffline] = useState<boolean>(false);
  const [search, setSearch] = useState<string>("");
  const [reloadKey, setReloadKey] = useState<number>(0);

  useEffect(() => {
    let isMounted = true;

    facilityService
      .getFacilities({ type: facilityType })
      .then((data) => {
        if (isMounted) {
          setFacilities(data);
          setIsOffline(false);
          setError(null);
          setLoading(false);
        }
      })
      .catch(() => {
        if (isMounted) {
          const fallback = fallbackFacilities.filter((f) => f.type === facilityType);
          setFacilities(fallback);
          setIsOffline(true);
          setError(null);
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [facilityType, reloadKey]);

  const handleRetry = () => {
    setLoading(true);
    setError(null);
    setReloadKey((k) => k + 1);
  };

  const filtered = useMemo(() => {
    if (!search.trim()) return facilities;
    const lower = search.toLowerCase();
    return facilities.filter(
      (f) =>
        f.name.toLowerCase().includes(lower) ||
        f.address.toLowerCase().includes(lower) ||
        (f.city ? f.city.toLowerCase().includes(lower) : false)
    );
  }, [facilities, search]);

  return (
    <div style={{ maxWidth: "1200px", margin: "0 auto", padding: "20px" }}>
      <div style={{ marginBottom: "24px" }}>
        <h1 style={{ margin: "0 0 8px 0", fontSize: "28px", color: "#0f172a" }}>
          {icon} {title}
        </h1>
        <p style={{ margin: 0, color: "#64748b", fontSize: "16px" }}>{description}</p>
      </div>

      <SearchBar value={search} onChange={setSearch} />

      {loading && <Loading message={`Loading verified ${facilityType.toLowerCase()} facilities...`} />}

      {isOffline && (
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            backgroundColor: "#fffbeb",
            border: "1px solid #fef3c7",
            color: "#92400e",
            padding: "10px 16px",
            borderRadius: "8px",
            margin: "16px 0",
            fontSize: "14px",
            gap: "12px",
            flexWrap: "wrap",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
            <span>⚠️</span>
            <span>
              <strong>Offline Demo Mode:</strong> Backend server is unreachable. Displaying local Karachi {facilityType.toLowerCase()} records.
            </span>
          </div>
          <button
            type="button"
            onClick={handleRetry}
            style={{
              backgroundColor: "#d97706",
              color: "#ffffff",
              border: "none",
              padding: "6px 12px",
              borderRadius: "6px",
              fontSize: "13px",
              fontWeight: 600,
              cursor: "pointer",
            }}
          >
            Retry Connection
          </button>
        </div>
      )}

      {error && !loading && !isOffline && (
        <ErrorMessage
          title={`Error loading ${facilityType.toLowerCase()} records`}
          message={error}
          onRetry={handleRetry}
        />
      )}

      {!loading && (!error || isOffline) && (
        <>
          <p style={{ color: "#475569", fontSize: "14px", margin: "16px 0 8px 0" }}>
            Showing <strong>{filtered.length}</strong> of <strong>{facilities.length}</strong> verified locations
          </p>
          <FacilityList
            facilities={filtered}
            emptyMessage={`No ${facilityType.toLowerCase()} facilities match "${search}".`}
          />
        </>
      )}
    </div>
  );
}
