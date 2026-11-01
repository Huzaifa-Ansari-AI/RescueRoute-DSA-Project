-- =============================================================================
-- Migration V3: External Facilities Table (OpenStreetMap / Overpass Integration)
-- =============================================================================

CREATE TABLE IF NOT EXISTS external_facilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(50) NOT NULL DEFAULT 'OPENSTREETMAP',
    osm_element_type VARCHAR(20) NOT NULL, -- 'node', 'way', 'relation'
    osm_element_id BIGINT NOT NULL,
    source_url TEXT,
    name VARCHAR(255) NOT NULL,
    facility_type VARCHAR(50) NOT NULL, -- 'Hospital', 'Clinic', 'Pharmacy', 'Blood Bank', 'Ambulance'
    osm_tags TEXT, -- JSON string of raw OpenStreetMap tags
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    address TEXT,
    city VARCHAR(100) DEFAULT 'Karachi',
    phone_number VARCHAR(100),
    website TEXT,
    opening_hours TEXT,
    import_status VARCHAR(50) NOT NULL DEFAULT 'DISCOVERED', -- 'DISCOVERED', 'DUPLICATE_CANDIDATE', 'REVIEWED', 'SUPPRESSED'
    matched_facility_id UUID REFERENCES facilities(id) ON DELETE SET NULL,
    imported_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_external_facilities_source_element UNIQUE (source, osm_element_type, osm_element_id)
);

CREATE INDEX IF NOT EXISTS idx_ext_facilities_source_osm ON external_facilities(source, osm_element_type, osm_element_id);
CREATE INDEX IF NOT EXISTS idx_ext_facilities_status ON external_facilities(import_status);
CREATE INDEX IF NOT EXISTS idx_ext_facilities_type ON external_facilities(facility_type);
CREATE INDEX IF NOT EXISTS idx_ext_facilities_coords ON external_facilities(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_ext_facilities_matched_facility ON external_facilities(matched_facility_id);
