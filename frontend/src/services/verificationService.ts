import apiClient from "./api";
import type { ApiResponse, Facility, FacilityType } from "../types/facility";
import type {
  CreateFacilityPayload,
  VerificationRequest,
  VerificationDecisionPayload,
} from "../types/verification";

interface RawFacilityResponse {
  id: string;
  name: string;
  facilityType: string;
  address: string;
  city?: string;
  latitude: number;
  longitude: number;
  phoneNumber?: string;
  isVerified?: boolean;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
  hospitalDetails?: Facility["hospitalDetails"];
  bloodBankDetails?: Facility["bloodBankDetails"];
  pharmacyDetails?: Facility["pharmacyDetails"];
  ambulanceDetails?: Facility["ambulanceDetails"];
}

function normalizeFacility(raw: RawFacilityResponse): Facility {
  return {
    ...raw,
    type: (raw.facilityType || "Hospital") as FacilityType,
  };
}

export const verificationService = {
  /**
   * Submits a new medical facility as an authenticated Provider.
   * Auto-creates a PENDING verification request on the backend.
   */
  async createFacility(payload: CreateFacilityPayload): Promise<Facility> {
    const response = await apiClient.post<ApiResponse<RawFacilityResponse>>(
      "/api/v1/facilities",
      payload
    );
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || "Failed to submit facility.");
    }
    return normalizeFacility(response.data.data);
  },

  /**
   * Fetches verification requests for moderation as an Admin.
   */
  async getVerificationRequests(status?: string): Promise<VerificationRequest[]> {
    const params = status ? { status } : {};
    const response = await apiClient.get<ApiResponse<VerificationRequest[]>>(
      "/api/v1/admin/verification-requests",
      { params }
    );
    if (!response.data.success) {
      throw new Error(response.data.message || "Failed to load verification requests.");
    }
    return response.data.data || [];
  },

  /**
   * Fetches a single verification request by ID.
   */
  async getVerificationRequestById(id: string): Promise<VerificationRequest> {
    const response = await apiClient.get<ApiResponse<VerificationRequest>>(
      `/api/v1/admin/verification-requests/${id}`
    );
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || "Failed to load verification request.");
    }
    return response.data.data;
  },

  /**
   * Processes an Admin moderation decision (APPROVE or REJECT).
   */
  async processDecision(
    id: string,
    payload: VerificationDecisionPayload
  ): Promise<VerificationRequest> {
    const response = await apiClient.put<ApiResponse<VerificationRequest>>(
      `/api/v1/admin/verification-requests/${id}`,
      payload
    );
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || "Failed to process decision.");
    }
    return response.data.data;
  },

  /**
   * Convenience method to approve a verification request.
   */
  async approveRequest(id: string, reviewNotes?: string): Promise<VerificationRequest> {
    const response = await apiClient.put<ApiResponse<VerificationRequest>>(
      `/api/v1/admin/verification-requests/${id}/approve`,
      { decision: "APPROVED", reviewNotes }
    );
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || "Failed to approve request.");
    }
    return response.data.data;
  },

  /**
   * Convenience method to reject a verification request.
   */
  async rejectRequest(id: string, reviewNotes: string): Promise<VerificationRequest> {
    const response = await apiClient.put<ApiResponse<VerificationRequest>>(
      `/api/v1/admin/verification-requests/${id}/reject`,
      { decision: "REJECTED", reviewNotes }
    );
    if (!response.data.success || !response.data.data) {
      throw new Error(response.data.message || "Failed to reject request.");
    }
    return response.data.data;
  },
};
