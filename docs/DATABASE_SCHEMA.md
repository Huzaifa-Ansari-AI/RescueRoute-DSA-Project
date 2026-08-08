# RescueRoute Database Schema

## Overview
RescueRoute uses PostgreSQL to store users, facilities, verification data, route data, and admin activity.

The schema is designed for:
- fast facility lookup
- clean role management
- verification workflow
- future routing and ranking
- simple extension later

---

## Core Tables

### 1. users
Stores all application users.

**Main fields**
- id
- full_name
- email
- password_hash
- role
- is_active
- created_at
- updated_at

**Roles**
- USER
- PROVIDER
- ADMIN

---

### 2. facilities
Stores shared data for every medical resource.

**Main fields**
- id
- name
- facility_type
- address
- city
- latitude
- longitude
- phone_number
- is_verified
- is_active
- created_by
- created_at
- updated_at

**Facility types**
- Hospital
- Blood Bank
- Pharmacy
- Ambulance

---

### 3. hospitals
Stores hospital-specific details.

**Main fields**
- id
- facility_id
- emergency_available
- icu_available
- trauma_center_available
- cardiology_available
- pediatrics_available
- maternity_available
- notes

---

### 4. blood_banks
Stores blood-bank-specific details.

**Main fields**
- id
- facility_id
- o_positive
- o_negative
- a_positive
- a_negative
- b_positive
- b_negative
- ab_positive
- ab_negative
- stock_updated_at
- notes

---

### 5. pharmacies
Stores pharmacy-specific details.

**Main fields**
- id
- facility_id
- emergency_medicines_available
- open_24_hours
- prescription_required
- delivery_available
- notes

---

### 6. ambulances
Stores ambulance-specific details.

**Main fields**
- id
- facility_id
- available_now
- response_time_minutes
- coverage_area
- equipped_for_icu_transfer
- notes

---

### 7. verification_requests
Tracks provider verification requests.

**Main fields**
- id
- facility_id
- submitted_by
- status
- reviewed_by
- review_notes
- submitted_at
- reviewed_at

**Status**
- PENDING
- APPROVED
- REJECTED

---

### 8. admin_logs
Tracks important admin actions.

**Main fields**
- id
- admin_id
- action_type
- target_entity_type
- target_entity_id
- details
- created_at

---

### 9. routes
Stores route results if route history is saved.

**Main fields**
- id
- source_latitude
- source_longitude
- destination_facility_id
- distance_km
- travel_time_minutes
- route_polyline
- emergency_type
- calculated_at

---

## Relationships

- one user can create many facilities
- one facility belongs to one subtype table
- one facility can have many verification requests
- one admin can review many verification requests
- one admin can create many admin logs
- one route belongs to one destination facility

---

## Indexes

Recommended indexes:
- users.email
- users.role
- facilities.facility_type
- facilities.city
- facilities.is_verified
- facilities.is_active
- verification_requests.status
- routes.destination_facility_id

---

## Schema Notes
- Use UUIDs for primary keys.
- Keep shared facility data in `facilities`.
- Keep type-specific fields in separate tables.
- Use timestamps for auditability.
- Keep the schema normalized and easy to extend.
- Store only the data needed for Version 1.

---

## Future Additions
Later versions may add:
- facility reviews
- live availability
- search history
- emergency alerts
- AI recommendation logs