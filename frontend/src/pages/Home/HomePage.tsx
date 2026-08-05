import { useMemo, useState } from "react";

import MapView from "../../components/map/MapView";
import SearchBar from "../../components/common/SearchBar";
import CategoryFilter from "../../components/common/CategoryFilter";

import { facilities } from "../../data/facilities";

export default function HomePage() {
  const [search, setSearch] = useState("");

  const [category, setCategory] = useState("All");

  const filteredFacilities = useMemo(() => {
    return facilities.filter((facility) => {
      const matchesSearch = facility.name
        .toLowerCase()
        .includes(search.toLowerCase());

      const matchesCategory =
        category === "All" ||
        facility.type === category;

      return matchesSearch && matchesCategory;
    });
  }, [search, category]);

  return (
    <div
      style={{
        maxWidth: "1200px",
        margin: "0 auto",
        padding: "20px",
      }}
    >
      <h1>🚑 RescueRoute</h1>

      <SearchBar
        value={search}
        onChange={setSearch}
      />

      <CategoryFilter
        value={category}
        onChange={setCategory}
      />

      <p>
        Facilities Found:{" "}
        <strong>{filteredFacilities.length}</strong>
      </p>

      <MapView facilities={filteredFacilities} />
    </div>
  );
}