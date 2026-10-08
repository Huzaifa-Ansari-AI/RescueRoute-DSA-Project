-- =============================================================================
-- RescueRoute Development Seed Data
-- Karachi Emergency Healthcare Facilities & Demo Users
-- Note: Intended for local development, staging, and demo/testing purposes.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Demo Users
-- Password hashes represent placeholder hashes for development/testing
-- -----------------------------------------------------------------------------
INSERT INTO users (id, full_name, email, password_hash, role, is_active, created_at, updated_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'RescueRoute Admin', 'admin@rescueroute.local', '$2a$10$placeholderAdminHashForDevOnly', 'ADMIN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222222', 'Karachi Healthcare Provider', 'provider@rescueroute.local', '$2a$10$placeholderProviderHashForDevOnly', 'PROVIDER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333333', 'Public Citizen', 'citizen@rescueroute.local', '$2a$10$placeholderCitizenHashForDevOnly', 'USER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Facilities (Karachi Key Healthcare Facilities)
-- -----------------------------------------------------------------------------
INSERT INTO facilities (id, name, facility_type, address, city, latitude, longitude, phone_number, is_verified, is_active, created_by, created_at, updated_at)
VALUES
    -- 1. Aga Khan University Hospital (Hospital)
    ('a0000001-0000-0000-0000-000000000001', 'Aga Khan University Hospital', 'Hospital', 'Stadium Road, Karachi', 'Karachi', 24.8937, 67.0748, '+92-21-111-911-911', TRUE, TRUE, '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- 2. Indus Hospital (Hospital)
    ('a0000001-0000-0000-0000-000000000002', 'Indus Hospital', 'Hospital', 'Korangi Crossing, Karachi', 'Karachi', 24.8517, 67.0412, '+92-21-35112709', TRUE, TRUE, '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- 3. Jinnah Postgraduate Medical Centre - JPMC (Hospital)
    ('a0000001-0000-0000-0000-000000000003', 'Jinnah Postgraduate Medical Centre', 'Hospital', 'Rafiqui Shaheed Road, Karachi', 'Karachi', 24.8524, 67.0456, '+92-21-99201300', TRUE, TRUE, '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- 4. Husaini Blood Bank (Blood Bank)
    ('b0000001-0000-0000-0000-000000000001', 'Husaini Blood Bank', 'Blood Bank', 'Soldier Bazaar, Garden East, Karachi', 'Karachi', 24.8703, 67.0219, '+92-21-32238405', TRUE, TRUE, '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- 5. Fatimid Foundation Blood Bank (Blood Bank)
    ('b0000001-0000-0000-0000-000000000002', 'Fatimid Foundation Blood Bank', 'Blood Bank', 'Britto Road, Soldier Bazaar, Karachi', 'Karachi', 24.8732, 67.0271, '+92-21-111-328-464', TRUE, TRUE, '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- 6. Dawaai Pharmacy (Pharmacy)
    ('c0000001-0000-0000-0000-000000000001', 'Dawaai Pharmacy', 'Pharmacy', 'Bahadurabad, Karachi', 'Karachi', 24.8619, 67.0099, '+92-21-111-329-224', TRUE, TRUE, '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- 7. K-Health 24/7 Pharmacy (Pharmacy)
    ('c0000001-0000-0000-0000-000000000002', 'K-Health Emergency Pharmacy', 'Pharmacy', 'Clifton Block 2, Karachi', 'Karachi', 24.8198, 67.0298, '+92-21-35876611', TRUE, TRUE, '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- 8. Edhi Ambulance Service (Ambulance)
    ('d0000001-0000-0000-0000-000000000001', 'Edhi Ambulance Service - Central', 'Ambulance', 'Boulton Market, M.A. Jinnah Road, Karachi', 'Karachi', 24.8646, 67.0165, '115', TRUE, TRUE, '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- 9. Chhipa Ambulance Service (Ambulance)
    ('d0000001-0000-0000-0000-000000000002', 'Chhipa Ambulance HQ', 'Ambulance', 'FTC Flyover, Shahrah-e-Faisal, Karachi', 'Karachi', 24.8601, 67.0512, '1020', TRUE, TRUE, '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Hospitals Subtype Details
-- -----------------------------------------------------------------------------
INSERT INTO hospitals (id, facility_id, emergency_available, icu_available, trauma_center_available, cardiology_available, pediatrics_available, maternity_available, notes)
VALUES
    (gen_random_uuid(), 'a0000001-0000-0000-0000-000000000001', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'Level 1 Trauma Center with 24/7 emergency & cardiac care.'),
    (gen_random_uuid(), 'a0000001-0000-0000-0000-000000000002', TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, 'Free tertiary care with pediatric emergency and trauma support.'),
    (gen_random_uuid(), 'a0000001-0000-0000-0000-000000000003', TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, 'Major government trauma center and emergency ward.')
ON CONFLICT (facility_id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Blood Banks Subtype Details
-- -----------------------------------------------------------------------------
INSERT INTO blood_banks (id, facility_id, o_positive, o_negative, a_positive, a_negative, b_positive, b_negative, ab_positive, ab_negative, stock_updated_at, notes)
VALUES
    (gen_random_uuid(), 'b0000001-0000-0000-0000-000000000001', TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, CURRENT_TIMESTAMP, 'Continuous emergency blood donor registry active.'),
    (gen_random_uuid(), 'b0000001-0000-0000-0000-000000000002', TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, CURRENT_TIMESTAMP, 'Specialized in thalassemia & hemophilia transfusion support.')
ON CONFLICT (facility_id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Pharmacies Subtype Details
-- -----------------------------------------------------------------------------
INSERT INTO pharmacies (id, facility_id, emergency_medicines_available, open_24_hours, prescription_required, delivery_available, notes)
VALUES
    (gen_random_uuid(), 'c0000001-0000-0000-0000-000000000001', TRUE, TRUE, TRUE, TRUE, 'Full stock of emergency antidotes, insulin, and cardiac medications.'),
    (gen_random_uuid(), 'c0000001-0000-0000-0000-000000000002', TRUE, TRUE, FALSE, TRUE, '24/7 emergency pharmacy with drive-through pickup.')
ON CONFLICT (facility_id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Ambulances Subtype Details
-- -----------------------------------------------------------------------------
INSERT INTO ambulances (id, facility_id, available_now, response_time_minutes, coverage_area, equipped_for_icu_transfer, notes)
VALUES
    (gen_random_uuid(), 'd0000001-0000-0000-0000-000000000001', TRUE, 8, 'South Karachi, Saddar, Lyari', TRUE, 'Equipped with portable ventilators and oxygen support.'),
    (gen_random_uuid(), 'd0000001-0000-0000-0000-000000000002', TRUE, 10, 'Shahrah-e-Faisal, PECHS, Gulshan', TRUE, 'Rapid response emergency rescue fleet.')
ON CONFLICT (facility_id) DO NOTHING;
