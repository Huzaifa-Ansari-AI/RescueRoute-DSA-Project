export type FacilityType = "Hospital" | "Blood Bank" | "Pharmacy" | "Ambulance";

export interface HospitalDetails {
  id?: string;
  emergencyAvailable: boolean;
  icuAvailable: boolean;
  traumaCenterAvailable: boolean;
  cardiologyAvailable: boolean;
  pediatricsAvailable: boolean;
  maternityAvailable: boolean;
  notes?: string;
}

export interface BloodBankDetails {
  id?: string;
  oPositive: boolean;
  oNegative: boolean;
  aPositive: boolean;
  aNegative: boolean;
  bPositive: boolean;
  bNegative: boolean;
  abPositive: boolean;
  abNegative: boolean;
  stockUpdatedAt?: string;
  notes?: string;
}

export interface PharmacyDetails {
  id?: string;
  emergencyMedicinesAvailable: boolean;
  open24Hours: boolean;
  prescriptionRequired: boolean;
  deliveryAvailable: boolean;
  notes?: string;
}

export interface AmbulanceDetails {
  id?: string;
  availableNow: boolean;
  responseTimeMinutes?: number;
  coverageArea?: string;
  equippedForIcuTransfer: boolean;
  notes?: string;
}

export interface Facility {
  id: string;
  name: string;
  facilityType?: string;
  type: FacilityType;
  address: string;
  city?: string;
  latitude: number;
  longitude: number;
  phoneNumber?: string;
  isVerified?: boolean;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
  hospitalDetails?: HospitalDetails;
  bloodBankDetails?: BloodBankDetails;
  pharmacyDetails?: PharmacyDetails;
  ambulanceDetails?: AmbulanceDetails;
}

export interface FacilityFilterParams {
  type?: string;
  city?: string;
  active?: boolean;
}

export interface FacilitySearchParams {
  lat: number;
  lng: number;
  type?: string;
  emergency?: boolean;
  k?: number;
}

export interface RankedFacility {
  rank: number;
  distanceKm: number;
  score: number;
  isVerified: boolean;
  emergencyAvailable: boolean;
  facility: Facility;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
}