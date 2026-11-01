import apiClient from "./api";
import type {
  ApiResponse,
  Facility,
  FacilityFilterParams,
  FacilitySearchParams,
  FacilityType,
  RankedFacility,
} from "../types/facility";

interface RawFacilityDto {
  id: string;
  name: string;
  facilityType: string;
  address: string;
  city: string;
  latitude: number;
  longitude: number;
  phoneNumber?: string;
  isVerified: boolean;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
  hospitalDetails?: Facility["hospitalDetails"];
  bloodBankDetails?: Facility["bloodBankDetails"];
  pharmacyDetails?: Facility["pharmacyDetails"];
  ambulanceDetails?: Facility["ambulanceDetails"];
}

interface RawRankedFacilityDto {
  rank: number;
  distanceKm: number;
  score: number;
  isVerified: boolean;
  emergencyAvailable: boolean;
  facility: RawFacilityDto;
}

/**
 * Normalizes backend DTO into frontend Facility model ensuring compatibility
 * with MapView icons, search filters, and detail views.
 */
function normalizeFacility(dto: RawFacilityDto): Facility {
  return {
    ...dto,
    type: (dto.facilityType || "Hospital") as FacilityType,
  };
}

export const facilityService = {
  /**
   * Fetches all facilities with optional query filters (type, city, active).
   */
  async getFacilities(params?: FacilityFilterParams): Promise<Facility[]> {
    const queryParams: Record<string, string | boolean> = {};

    if (params?.type && params.type !== "All") {
      queryParams.type = params.type;
    }
    if (params?.city) {
      queryParams.city = params.city;
    }
    if (params?.active !== undefined) {
      queryParams.active = params.active;
    }

    const response = await apiClient.get<ApiResponse<RawFacilityDto[]>>("/facilities", {
      params: queryParams,
    });

    const data = response.data?.data || [];
    return data.map(normalizeFacility);
  },

  /**
   * Performs DSA-driven spatial distance calculation and multi-criteria priority ranking.
   */
  async searchRankedFacilities(params: FacilitySearchParams): Promise<RankedFacility[]> {
    const queryParams: Record<string, string | number | boolean> = {
      lat: params.lat,
      lng: params.lng,
    };

    if (params.radiusKm !== undefined) {
      queryParams.radiusKm = params.radiusKm;
    }
    if (params.type && params.type !== "All") {
      queryParams.type = params.type;
    }
    if (params.emergency !== undefined) {
      queryParams.emergency = params.emergency;
    }
    if (params.k !== undefined) {
      queryParams.k = params.k;
    }

    const response = await apiClient.get<ApiResponse<RawRankedFacilityDto[]>>("/facilities/search", {
      params: queryParams,
    });

    const data = response.data?.data || [];
    return data.map((item) => ({
      ...item,
      facility: normalizeFacility(item.facility),
    }));
  },

  /**
   * Fetches a single facility by its UUID with complete subtype details.
   */
  async getFacilityById(id: string): Promise<Facility> {
    const response = await apiClient.get<ApiResponse<RawFacilityDto>>(`/facilities/${id}`);
    if (!response.data?.data) {
      throw new Error(`Facility not found with ID: ${id}`);
    }
    return normalizeFacility(response.data.data);
  },

  /**
   * Submits an update to an existing facility as an authenticated provider or admin.
   * Staged in verification_requests for administrator review.
   */
  async updateFacility(
    id: string,
    payload: import("../types/verification").UpdateFacilityPayload
  ): Promise<Facility> {
    const response = await apiClient.put<ApiResponse<RawFacilityDto>>(
      `/facilities/${id}`,
      payload
    );
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || "Failed to submit facility update.");
    }
    return normalizeFacility(response.data.data);
  },

  /**
   * Safely deactivates (soft-deletes) a facility.
   */
  async deactivateFacility(id: string): Promise<Facility> {
    const response = await apiClient.delete<ApiResponse<RawFacilityDto>>(
      `/facilities/${id}`
    );
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || "Failed to deactivate facility.");
    }
    return normalizeFacility(response.data.data);
  },

  /**
   * Restores a previously soft-deleted facility (Admin only).
   */
  async reactivateFacility(id: string): Promise<Facility> {
    const response = await apiClient.post<ApiResponse<RawFacilityDto>>(
      `/facilities/${id}/reactivate`
    );
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || "Failed to reactivate facility.");
    }
    return normalizeFacility(response.data.data);
  },

  /**
   * Fetches facilities owned/managed by the authenticated provider.
   */
  async getMyFacilities(): Promise<Facility[]> {
    const response = await apiClient.get<ApiResponse<RawFacilityDto[]>>("/facilities/my");
    const data = response.data?.data || [];
    return data.map(normalizeFacility);
  },

  /**
   * Fetches all facilities for administrator management with comprehensive filters.
   */
  async getAdminFacilities(params?: {
    type?: string;
    city?: string;
    active?: boolean;
    verified?: boolean;
  }): Promise<Facility[]> {
    const queryParams: Record<string, string | boolean> = {};

    if (params?.type && params.type !== "All") {
      queryParams.type = params.type;
    }
    if (params?.city) {
      queryParams.city = params.city;
    }
    if (params?.active !== undefined) {
      queryParams.active = params.active;
    }
    if (params?.verified !== undefined) {
      queryParams.verified = params.verified;
    }

    const response = await apiClient.get<ApiResponse<RawFacilityDto[]>>("/facilities/admin", {
      params: queryParams,
    });
    const data = response.data?.data || [];
    return data.map(normalizeFacility);
  },
};

export default facilityService;

