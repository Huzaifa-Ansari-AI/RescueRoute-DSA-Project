-- =============================================================================
-- RescueRoute Database Schema
-- Compatible with PostgreSQL 15+ / Neon Serverless PostgreSQL
-- Based on docs/DATABASE_SCHEMA.md
-- =============================================================================

-- Enable pgcrypto for gen_random_uuid() if not enabled
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -----------------------------------------------------------------------------
-- 1. USERS
-- Stores all application users (Public users, Medical Providers, Admins)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('USER', 'PROVIDER', 'ADMIN')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- -----------------------------------------------------------------------------
-- 2. FACILITIES
-- Shared base data for all medical resources (Hospitals, Blood Banks, etc.)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS facilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    facility_type VARCHAR(50) NOT NULL CHECK (facility_type IN ('Hospital', 'Blood Bank', 'Pharmacy', 'Ambulance')),
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL DEFAULT 'Karachi',
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    phone_number VARCHAR(50),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_facilities_type ON facilities(facility_type);
CREATE INDEX IF NOT EXISTS idx_facilities_city ON facilities(city);
CREATE INDEX IF NOT EXISTS idx_facilities_is_verified ON facilities(is_verified);
CREATE INDEX IF NOT EXISTS idx_facilities_is_active ON facilities(is_active);

-- -----------------------------------------------------------------------------
-- 3. HOSPITALS
-- Specific clinical capabilities and department availability
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hospitals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID UNIQUE NOT NULL REFERENCES facilities(id) ON DELETE CASCADE,
    emergency_available BOOLEAN NOT NULL DEFAULT FALSE,
    icu_available BOOLEAN NOT NULL DEFAULT FALSE,
    trauma_center_available BOOLEAN NOT NULL DEFAULT FALSE,
    cardiology_available BOOLEAN NOT NULL DEFAULT FALSE,
    pediatrics_available BOOLEAN NOT NULL DEFAULT FALSE,
    maternity_available BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT
);

-- -----------------------------------------------------------------------------
-- 4. BLOOD BANKS
-- Specific blood group inventory flags and metadata
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blood_banks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID UNIQUE NOT NULL REFERENCES facilities(id) ON DELETE CASCADE,
    o_positive BOOLEAN NOT NULL DEFAULT FALSE,
    o_negative BOOLEAN NOT NULL DEFAULT FALSE,
    a_positive BOOLEAN NOT NULL DEFAULT FALSE,
    a_negative BOOLEAN NOT NULL DEFAULT FALSE,
    b_positive BOOLEAN NOT NULL DEFAULT FALSE,
    b_negative BOOLEAN NOT NULL DEFAULT FALSE,
    ab_positive BOOLEAN NOT NULL DEFAULT FALSE,
    ab_negative BOOLEAN NOT NULL DEFAULT FALSE,
    stock_updated_at TIMESTAMP WITH TIME ZONE,
    notes TEXT
);

-- -----------------------------------------------------------------------------
-- 5. PHARMACIES
-- Medicine availability, operating hours, delivery capabilities
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pharmacies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID UNIQUE NOT NULL REFERENCES facilities(id) ON DELETE CASCADE,
    emergency_medicines_available BOOLEAN NOT NULL DEFAULT FALSE,
    open_24_hours BOOLEAN NOT NULL DEFAULT FALSE,
    prescription_required BOOLEAN NOT NULL DEFAULT TRUE,
    delivery_available BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT
);

-- -----------------------------------------------------------------------------
-- 6. AMBULANCES
-- Response time, fleet coverage, transfer equipment
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ambulances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID UNIQUE NOT NULL REFERENCES facilities(id) ON DELETE CASCADE,
    available_now BOOLEAN NOT NULL DEFAULT FALSE,
    response_time_minutes INTEGER,
    coverage_area VARCHAR(255),
    equipped_for_icu_transfer BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT
);

-- -----------------------------------------------------------------------------
-- 7. VERIFICATION REQUESTS
-- Audit trail and moderation queue for provider facility verification
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS verification_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID NOT NULL REFERENCES facilities(id) ON DELETE CASCADE,
    submitted_by UUID REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    review_notes TEXT,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_verification_requests_status ON verification_requests(status);

-- -----------------------------------------------------------------------------
-- 8. ADMIN LOGS
-- Immutable activity log of admin actions
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action_type VARCHAR(100) NOT NULL,
    target_entity_type VARCHAR(100) NOT NULL,
    target_entity_id UUID,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------------
-- 9. ROUTES
-- History of calculated emergency navigation routes
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS routes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_latitude DOUBLE PRECISION NOT NULL,
    source_longitude DOUBLE PRECISION NOT NULL,
    destination_facility_id UUID NOT NULL REFERENCES facilities(id) ON DELETE CASCADE,
    distance_km DOUBLE PRECISION,
    travel_time_minutes DOUBLE PRECISION,
    route_polyline TEXT,
    emergency_type VARCHAR(100),
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_routes_destination_facility_id ON routes(destination_facility_id);
