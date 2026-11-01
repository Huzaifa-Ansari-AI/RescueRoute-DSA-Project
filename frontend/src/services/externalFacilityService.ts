import apiClient from "./api";
import type { ApiResponse } from "../types/facility";
import type {
  ExternalFacility,
  ExternalFacilitySearchParams,
  ExternalImportRequest,
  ExternalImportSummary,
} from "../types/externalFacility";

export const externalFacilityService = {
  /**
   * Search external facilities within a radius around given coordinates.
   */
  async searchExternalFacilities(params: ExternalFacilitySearchParams): Promise<ExternalFacility[]> {
    const queryParams: Record<string, string | number> = {
      lat: params.lat,
      lng: params.lng,
    };
    if (params.radiusMeters) {
      queryParams.radiusMeters = params.radiusMeters;
    }
    if (params.type && params.type !== "All") {
      queryParams.type = params.type;
    }
    if (params.status && params.status !== "All") {
      queryParams.status = params.status;
    }

    const response = await apiClient.get<ApiResponse<ExternalFacility[]>>("/external-facilities", {
      params: queryParams,
    });
    return response.data.data;
  },

  /**
   * Trigger a bounded geographic import from OpenStreetMap Overpass.
   * Requires authenticated provider or administrator credentials.
   */
  async importFacilities(request: ExternalImportRequest): Promise<ExternalImportSummary> {
    const response = await apiClient.post<ApiResponse<ExternalImportSummary>>(
      "/external-facilities/import",
      request
    );
    return response.data.data;
  },

  /**
   * Get detailed attributes of an external facility by ID.
   */
  async getExternalFacilityById(id: string): Promise<ExternalFacility> {
    const response = await apiClient.get<ApiResponse<ExternalFacility>>(`/external-facilities/${id}`);
    return response.data.data;
  },

  /**
   * Moderate external facility import status (Admin only).
   */
  async updateStatus(id: string, status: string, notes?: string): Promise<ExternalFacility> {
    const response = await apiClient.put<ApiResponse<ExternalFacility>>(
      `/external-facilities/${id}/status`,
      { status, notes }
    );
    return response.data.data;
  },
};
