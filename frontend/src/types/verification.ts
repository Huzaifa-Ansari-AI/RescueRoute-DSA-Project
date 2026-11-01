import type {
  FacilityType,
  HospitalDetails,
  BloodBankDetails,
  PharmacyDetails,
  AmbulanceDetails,
  ClinicDetails,
  Facility,
} from "./facility";
import type { User } from "./auth";

export type VerificationStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface CreateFacilityPayload {
  name: string;
  facilityType: FacilityType;
  address: string;
  city?: string;
  latitude: number;
  longitude: number;
  phoneNumber?: string;
  hospitalDetails?: Partial<HospitalDetails>;
  bloodBankDetails?: Partial<BloodBankDetails>;
  pharmacyDetails?: Partial<PharmacyDetails>;
  ambulanceDetails?: Partial<AmbulanceDetails>;
  clinicDetails?: Partial<ClinicDetails>;
}

export interface UpdateFacilityPayload {
  name: string;
  address: string;
  city?: string;
  latitude: number;
  longitude: number;
  phoneNumber?: string;
  hospitalDetails?: Partial<HospitalDetails>;
  bloodBankDetails?: Partial<BloodBankDetails>;
  pharmacyDetails?: Partial<PharmacyDetails>;
  ambulanceDetails?: Partial<AmbulanceDetails>;
  clinicDetails?: Partial<ClinicDetails>;
}

export interface VerificationRequest {
  id: string;
  facilityId: string;
  facilityName: string;
  facilityType: FacilityType;
  facilityAddress: string;
  facilityCity: string;
  facilityLatitude: number;
  facilityLongitude: number;
  facilityPhoneNumber?: string;
  facilityVerified: boolean;
  facilityActive?: boolean;
  facilityDeletedAt?: string | null;
  facilityHasPendingUpdate?: boolean;
  submittedBy?: User;
  status: VerificationStatus;
  requestType?: "CREATION" | "UPDATE";
  proposedChanges?: UpdateFacilityPayload | null;
  reviewedBy?: User;
  reviewNotes?: string;
  submittedAt: string;
  reviewedAt?: string;
  facilityDetails?: Facility;
}

export interface VerificationDecisionPayload {
  decision: "APPROVED" | "REJECTED";
  status?: "APPROVED" | "REJECTED";
  reviewNotes?: string;
}
