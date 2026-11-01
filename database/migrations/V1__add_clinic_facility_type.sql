-- =============================================================================
-- Migration: Add Clinic Facility Type and Clinics Subtype Table
-- Safe, additive migration preserving existing data and referential integrity
-- =============================================================================

-- 1. Update facilities check constraint to include 'Clinic'
ALTER TABLE facilities DROP CONSTRAINT IF EXISTS facilities_facility_type_check;
ALTER TABLE facilities ADD CONSTRAINT facilities_facility_type_check 
    CHECK (facility_type IN ('Hospital', 'Blood Bank', 'Pharmacy', 'Ambulance', 'Clinic'));

-- 2. Create clinics subtype table for outpatient consultation metadata
CREATE TABLE IF NOT EXISTS clinics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID UNIQUE NOT NULL REFERENCES facilities(id) ON DELETE CASCADE,
    consultation_available BOOLEAN NOT NULL DEFAULT TRUE,
    specialties TEXT,
    open_weekends BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_clinics_facility_id ON clinics(facility_id);
