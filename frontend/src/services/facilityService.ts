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
};

export default facilityService;

