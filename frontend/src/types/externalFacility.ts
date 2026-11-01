export type ExternalImportStatus = "DISCOVERED" | "DUPLICATE_CANDIDATE" | "REVIEWED" | "SUPPRESSED";

export interface ExternalFacility {
  id: string;
  source: string;
  osmElementType: string;
  osmElementId: number;
  sourceUrl?: string;
  name: string;
  facilityType: string;
  osmTags?: string;
  latitude: number;
  longitude: number;
  address?: string;
  city?: string;
  phoneNumber?: string;
  website?: string;
  openingHours?: string;
  importStatus: ExternalImportStatus;
  matchedFacilityId?: string;
  matchedFacilityName?: string;
  distanceKm?: number;
  attribution: string;
  disclaimer: string;
  isVerified: boolean;
  importedAt?: string;
  lastSeenAt?: string;
}

export interface ExternalFacilitySearchParams {
  lat: number;
  lng: number;
  radiusMeters?: number;
  type?: string;
  status?: string;
}

export interface ExternalImportRequest {
  latitude: number;
  longitude: number;
  radiusMeters: number;
  facilityType?: string;
}

export interface ExternalImportSummary {
  totalQueried: number;
  imported: number;
  updated: number;
  duplicateCandidates: number;
  skipped: number;
  failed: number;
  executionTimeMs: number;
  message: string;
}
