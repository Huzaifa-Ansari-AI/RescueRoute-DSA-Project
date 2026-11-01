-- =============================================================================
-- Migration: Add Facility Lifecycle, Staged Updates, and Re-Verification
-- =============================================================================

-- 1. Add proposed_changes and request_type to verification_requests
ALTER TABLE verification_requests ADD COLUMN IF NOT EXISTS proposed_changes TEXT;
ALTER TABLE verification_requests ADD COLUMN IF NOT EXISTS request_type VARCHAR(50) NOT NULL DEFAULT 'CREATION';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'verification_requests_request_type_check'
    ) THEN
        ALTER TABLE verification_requests ADD CONSTRAINT verification_requests_request_type_check 
            CHECK (request_type IN ('CREATION', 'UPDATE'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_verification_requests_request_type ON verification_requests(request_type);

-- 2. Add has_pending_update and deleted_at to facilities
ALTER TABLE facilities ADD COLUMN IF NOT EXISTS has_pending_update BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE facilities ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_facilities_has_pending_update ON facilities(has_pending_update);
CREATE INDEX IF NOT EXISTS idx_facilities_deleted_at ON facilities(deleted_at);
