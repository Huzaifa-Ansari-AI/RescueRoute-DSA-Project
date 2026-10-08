/**
 * Geocoding Service for RescueRoute
 * Provides search-as-you-type place suggestions across Karachi localities and points of interest.
 * Combines an instant high-fidelity Karachi locality index with OpenStreetMap Nominatim queries.
 */

export interface PlaceSuggestion {
  id: string;
  name: string;
  displayName: string;
  lat: number;
  lng: number;
  locality?: string;
}

/**
 * Curated dictionary of major Karachi towns, neighborhoods, and landmarks.
 * Provides instant zero-latency suggestions and bulletproof fallback when offline or rate-limited.
 */
export const KARACHI_LOCALITIES: PlaceSuggestion[] = [
  { id: "khi-nazimabad", name: "Nazimabad", displayName: "Nazimabad, Karachi", lat: 24.9107, lng: 67.0311, locality: "Central" },
  { id: "khi-n-nazimabad", name: "North Nazimabad", displayName: "North Nazimabad, Karachi", lat: 24.9388, lng: 67.0422, locality: "Central" },
  { id: "khi-saddar", name: "Saddar", displayName: "Saddar, Karachi", lat: 24.8607, lng: 67.0011, locality: "South" },
  { id: "khi-clifton", name: "Clifton", displayName: "Clifton, Karachi", lat: 24.8236, lng: 67.0332, locality: "South" },
  { id: "khi-dha", name: "DHA (Defence)", displayName: "Defence Housing Authority (DHA), Karachi", lat: 24.8155, lng: 67.0676, locality: "South" },
  { id: "khi-gulshan", name: "Gulshan-e-Iqbal", displayName: "Gulshan-e-Iqbal, Karachi", lat: 24.8934, lng: 67.0722, locality: "East" },
  { id: "khi-jauhar", name: "Gulistan-e-Jauhar", displayName: "Gulistan-e-Jauhar, Karachi", lat: 24.9192, lng: 67.1353, locality: "East" },
  { id: "khi-shershah", name: "Shershah", displayName: "Shershah, SITE Town, Karachi", lat: 24.8872, lng: 66.9744, locality: "West" },
  { id: "khi-pechs", name: "PECHS", displayName: "PECHS, Jamshed Town, Karachi", lat: 24.8715, lng: 67.0622, locality: "East" },
  { id: "khi-bahadurabad", name: "Bahadurabad", displayName: "Bahadurabad, Karachi", lat: 24.8812, lng: 67.0645, locality: "East" },
  { id: "khi-korangi", name: "Korangi", displayName: "Korangi Industrial Area, Karachi", lat: 24.8258, lng: 67.1352, locality: "Korangi" },
  { id: "khi-landhi", name: "Landhi", displayName: "Landhi Town, Karachi", lat: 24.8436, lng: 67.1864, locality: "Malir" },
  { id: "khi-malir", name: "Malir", displayName: "Malir Cantt / Town, Karachi", lat: 24.9082, lng: 67.1994, locality: "Malir" },
  { id: "khi-lyari", name: "Lyari", displayName: "Lyari Town, Karachi", lat: 24.8693, lng: 66.9897, locality: "South" },
  { id: "khi-site", name: "SITE Area", displayName: "SITE Industrial Area, Karachi", lat: 24.9042, lng: 66.9984, locality: "West" },
  { id: "khi-fb-area", name: "Federal B Area (FB Area)", displayName: "Federal B Area, Gulberg Town, Karachi", lat: 24.9312, lng: 67.0734, locality: "Central" },
  { id: "khi-liaquatabad", name: "Liaquatabad", displayName: "Liaquatabad (Laloo Khet), Karachi", lat: 24.9022, lng: 67.0394, locality: "Central" },
  { id: "khi-n-karachi", name: "North Karachi", displayName: "North Karachi / New Karachi, Karachi", lat: 24.9822, lng: 67.0648, locality: "Central" },
  { id: "khi-orangi", name: "Orangi Town", displayName: "Orangi Town, Karachi", lat: 24.9452, lng: 66.9934, locality: "West" },
  { id: "khi-kemari", name: "Kemari", displayName: "Kemari Port Area, Karachi", lat: 24.8193, lng: 66.9781, locality: "South" },
  { id: "khi-tariq-road", name: "Tariq Road", displayName: "Tariq Road Commercial Area, Karachi", lat: 24.8719, lng: 67.0592, locality: "East" },
  { id: "khi-shahrah-e-faisal", name: "Shahrah-e-Faisal", displayName: "Shahrah-e-Faisal (FTC / Baloch Colony), Karachi", lat: 24.8601, lng: 67.0512, locality: "East" },
  { id: "khi-soldier-bazaar", name: "Soldier Bazaar", displayName: "Soldier Bazaar, Garden East, Karachi", lat: 24.8703, lng: 67.0219, locality: "South" },
  { id: "khi-civic-centre", name: "Civic Centre / Hasan Square", displayName: "Civic Centre, University Road, Karachi", lat: 24.8967, lng: 67.0744, locality: "East" },
  { id: "khi-airport", name: "Jinnah International Airport", displayName: "Jinnah International Airport (OPKC), Karachi", lat: 24.9065, lng: 67.1608, locality: "Malir" },
  { id: "khi-stadium-road", name: "Stadium Road", displayName: "National Stadium Road, Karachi", lat: 24.8937, lng: 67.0748, locality: "East" },
  { id: "khi-boat-basin", name: "Boat Basin", displayName: "Boat Basin, Clifton Block 5, Karachi", lat: 24.8252, lng: 67.0264, locality: "South" },
  { id: "khi-sea-view", name: "Sea View / Clifton Beach", displayName: "Sea View Beach, DHA Phase 6, Karachi", lat: 24.7891, lng: 67.0544, locality: "South" },
  { id: "khi-i-i-chundrigar", name: "I.I. Chundrigar Road", displayName: "I.I. Chundrigar Financial District, Karachi", lat: 24.8512, lng: 67.0041, locality: "South" },
  { id: "khi-sohrab-goth", name: "Sohrab Goth", displayName: "Sohrab Goth / Super Highway, Karachi", lat: 24.9542, lng: 67.0911, locality: "East" },
];

const cache = new Map<string, PlaceSuggestion[]>();
let activeAbortController: AbortController | null = null;

export const geocodingService = {
  /**
   * Searches Karachi places, towns, neighborhoods, and streets.
   * Debounced and safely aborts in-flight requests.
   */
  async searchPlaces(query: string, signal?: AbortSignal): Promise<PlaceSuggestion[]> {
    const cleanQuery = query.trim().toLowerCase();
    if (!cleanQuery || cleanQuery.length < 2) {
      return [];
    }

    if (cache.has(cleanQuery)) {
      return cache.get(cleanQuery)!;
    }

    // 1. Check curated local dictionary for instant matching
    const localMatches = KARACHI_LOCALITIES.filter((item) => {
      const q = cleanQuery;
      return (
        item.name.toLowerCase().includes(q) ||
        item.displayName.toLowerCase().includes(q) ||
        (item.locality && item.locality.toLowerCase().includes(q))
      );
    });

    // 2. Query OpenStreetMap Nominatim for broader street/landmark matching
    try {
      if (activeAbortController) {
        activeAbortController.abort();
      }
      activeAbortController = new AbortController();

      const combinedSignal = signal || activeAbortController.signal;
      const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(
        cleanQuery + ", Karachi, Pakistan"
      )}&format=json&addressdetails=1&limit=5&countrycodes=pk&viewbox=66.7,24.6,67.4,25.2`;

      const response = await fetch(url, {
        headers: {
          Accept: "application/json",
          "User-Agent": "RescueRoute-Emergency-Healthcare-Navigator/1.0",
        },
        signal: combinedSignal,
      });

      if (response.ok) {
        const rawResults = (await response.json()) as Array<{
          place_id: number;
          display_name: string;
          lat: string;
          lon: string;
          name?: string;
        }>;

        const onlineSuggestions: PlaceSuggestion[] = rawResults.map((item) => ({
          id: `osm-${item.place_id}`,
          name: item.name || item.display_name.split(",")[0].trim(),
          displayName: item.display_name,
          lat: parseFloat(item.lat),
          lng: parseFloat(item.lon),
        }));

        // Merge local matches with online results, prioritizing local verified Karachi towns
        const mergedMap = new Map<string, PlaceSuggestion>();
        localMatches.forEach((m) => mergedMap.set(m.name.toLowerCase(), m));
        onlineSuggestions.forEach((s) => {
          const key = s.name.toLowerCase();
          if (!mergedMap.has(key)) {
            mergedMap.set(key, s);
          }
        });

        const combined = Array.from(mergedMap.values()).slice(0, 8);
        cache.set(cleanQuery, combined);
        return combined;
      }
    } catch {
      // On network failure, abort, or rate limit: gracefully return local verified Karachi matches
    }

    cache.set(cleanQuery, localMatches.slice(0, 8));
    return localMatches.slice(0, 8);
  },

  /**
   * Reverse geocodes coordinates to a readable Karachi place name
   */
  async reverseGeocode(lat: number, lng: number): Promise<string> {
    // Check if close to any known Karachi locality (< 1.5 km)
    for (const loc of KARACHI_LOCALITIES) {
      const dLat = Math.abs(loc.lat - lat);
      const dLng = Math.abs(loc.lng - lng);
      if (dLat < 0.015 && dLng < 0.015) {
        return loc.displayName;
      }
    }

    try {
      const url = `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json`;
      const res = await fetch(url, {
        headers: {
          Accept: "application/json",
          "User-Agent": "RescueRoute-Emergency-Healthcare-Navigator/1.0",
        },
      });
      if (res.ok) {
        const data = (await res.json()) as { display_name?: string };
        if (data.display_name) {
          return data.display_name;
        }
      }
    } catch {
      // Fallback
    }

    return `Coordinates (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
  },
};

export default geocodingService;
