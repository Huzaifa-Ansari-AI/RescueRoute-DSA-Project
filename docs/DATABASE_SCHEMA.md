# RescueRoute Database Schema

**Project Name:** RescueRoute  
**Database:** PostgreSQL  
**Version:** 1.0  
**Purpose:** Core platform data model for facility search, verification, routing, and authentication

---

## 1. Purpose of This Document

This document defines the database design for RescueRoute.

It describes:

- the entities used by the system,
- the relationships between those entities,
- the core columns required in each table,
- indexing and normalization direction,
- and the persistence structure needed for Version 1.

This schema is designed to support:

- user authentication,
- facility registration,
- facility verification,
- search and filtering,
- route optimization,
- admin auditing,
- and future AI expansion.

---

## 2. Database Design Principles

The database must follow these principles:

- use PostgreSQL,
- keep the schema normalized,
- use clear foreign keys,
- separate verification from registration,
- store location data in a query-friendly format,
- support ranking and filtering efficiently,
- keep the design extensible for future versions.

---

## 3. Naming Conventions

### General Rules

- Table names use plural lowercase with underscores.
- Column names use lowercase with underscores.
- Primary keys should be `uuid` where appropriate.
- Timestamps should use `created_at` and `updated_at`.
- Boolean fields should use meaningful names such as `is_verified`.

### Examples

- `users`
- `facilities`
- `hospitals`
- `blood_banks`
- `pharmacies`
- `ambulances`
- `routes`
- `verification_requests`
- `admin_logs`

---

## 4. Core Entities

The following are the core persistent entities for Version 1:

- users
- facilities
- hospitals
- blood_banks
- pharmacies
- ambulances
- routes
- verification_requests
- admin_logs

---

## 5. Entity Overview

---

### 5.1 users

Stores authentication and role information for system users.

#### Purpose
Represents:
- patients,
- facility providers,
- administrators.

#### Suggested Columns

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| full_name | VARCHAR(150) | User name |
| email | VARCHAR(255) | Unique login identity |
| phone_number | VARCHAR(30) | Optional, unique if used |
| password_hash | VARCHAR(255) | BCrypt hash |
| role | VARCHAR(30) | USER / PROVIDER / ADMIN |
| is_active | BOOLEAN | Account status |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update time |

#### Constraints
- `email` must be unique
- `password_hash` must never be exposed in responses

---

### 5.2 facilities

Stores common information shared by all facility types.

#### Purpose
Base table for all medical resources.

#### Suggested Columns

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| facility_name | VARCHAR(255) | Display name |
| facility_type | VARCHAR(50) | HOSPITAL / BLOOD_BANK / PHARMACY / AMBULANCE |
| address | TEXT | Full address |
| city | VARCHAR(100) | City name |
| latitude | DECIMAL(10,7) | Geographic coordinate |
| longitude | DECIMAL(10,7) | Geographic coordinate |
| contact_number | VARCHAR(30) | Contact info |
| email | VARCHAR(255) | Optional |
| operating_hours | VARCHAR(255) | Human-readable hours |
| is_verified | BOOLEAN | Verification status |
| is_active | BOOLEAN | Whether facility is visible |
| reliability_score | DECIMAL(5,2) | Ranking support |
| created_by | UUID | FK to users.id |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Update time |

#### Notes
This table stores shared information for all facility categories.

Specialized details should live in subtype tables.

---

### 5.3 hospitals

Stores hospital-specific details.

#### Purpose
Contains fields specific to hospitals.

#### Suggested Columns

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| facility_id | UUID | FK to facilities.id |
| emergency_department_available | BOOLEAN | Emergency room support |
| icu_available | BOOLEAN | ICU support |
| trauma_center_available | BOOLEAN | Trauma handling support |
| cardiology_available | BOOLEAN | Cardiology support |
| neurology_available | BOOLEAN | Neurology support |
| pediatrics_available | BOOLEAN | Pediatrics support |
| maternity_available | BOOLEAN | Maternity support |
| special_notes | TEXT | Optional details |

---

### 5.4 blood_banks

Stores blood-bank-specific details.

#### Purpose
Contains blood availability information.

#### Suggested Columns

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| facility_id | UUID | FK to facilities.id |
| blood_group_o_positive | BOOLEAN | Availability flag |
| blood_group_o_negative | BOOLEAN | Availability flag |
| blood_group_a_positive | BOOLEAN | Availability flag |
| blood_group_a_negative | BOOLEAN | Availability flag |
| blood_group_b_positive | BOOLEAN | Availability flag |
| blood_group_b_negative | BOOLEAN | Availability flag |
| blood_group_ab_positive | BOOLEAN | Availability flag |
| blood_group_ab_negative | BOOLEAN | Availability flag |
| stock_updated_at | TIMESTAMP | Last stock update |
| special_notes | TEXT | Optional details |

#### Note
In a later version, blood stock can be normalized further into a separate stock table.  
For Version 1, a simple structure is acceptable.

---

### 5.5 pharmacies

Stores pharmacy-specific details.

#### Purpose
Contains pharmacy service information.

#### Suggested Columns

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| facility_id | UUID | FK to facilities.id |
| emergency_medicines_available | BOOLEAN | Critical medicines support |
| prescription_required | BOOLEAN | Pharmacy policy |
| open_24_hours | BOOLEAN | Round-the-clock support |
| home_delivery_available | BOOLEAN | Optional |
| special_notes | TEXT | Optional details |

---

### 5.6 ambulances

Stores ambulance-specific details.

#### Purpose
Contains ambulance service information.

#### Suggested Columns

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| facility_id | UUID | FK to facilities.id |
| response_time_minutes | INTEGER | Estimated response time |
| coverage_area | VARCHAR(255) | Service region |
| available_now | BOOLEAN | Availability flag |
| equipped_for_icu_transfer | BOOLEAN | Advanced support |
| special_notes | TEXT | Optional details |

---

### 5.7 routes

Stores routing results and computed paths.

#### Purpose
Can store route computations for history, auditing, or optimization.

#### Suggested Columns

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| source_latitude | DECIMAL(10,7) | User location |
| source_longitude | DECIMAL(10,7) | User location |
| destination_facility_id | UUID | FK to facilities.id |
| distance_km | DECIMAL(10,2) | Route distance |
| travel_time_minutes | DECIMAL(10,2) | Estimated time |
| route_polyline | TEXT | Optional encoded route |
| emergency_type | VARCHAR(50) | Search context |
| calculated_at | TIMESTAMP | Computation time |

#### Note
If route history is not required in Version 1, this table can still be useful for logging computations.

---

### 5.8 verification_requests

Stores verification workflow requests submitted by facilities.

#### Purpose
Tracks facility verification lifecycle.

#### Suggested Columns

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| facility_id | UUID | FK to facilities.id |
| submitted_by | UUID | FK to users.id |
| verification_status | VARCHAR(30) | PENDING / APPROVED / REJECTED |
| reviewed_by | UUID | FK to users.id, admin |
| review_notes | TEXT | Admin notes |
| submitted_at | TIMESTAMP | Submission time |
| reviewed_at | TIMESTAMP | Review time |

#### Notes
This table is critical for trust and moderation.

---

### 5.9 admin_logs

Stores admin activity for audit and transparency.

#### Purpose
Tracks important administrative actions.

#### Suggested Columns

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| admin_id | UUID | FK to users.id |
| action_type | VARCHAR(100) | Example: VERIFY_FACILITY |
| target_entity_type | VARCHAR(100) | Example: FACILITY |
| target_entity_id | UUID | Affected entity |
| action_details | TEXT | Description of action |
| created_at | TIMESTAMP | Action time |

---

## 6. Relationships

### Relationship Summary

- A `user` can create multiple `facilities`
- A `facility` can belong to exactly one subtype table:
  - hospital
  - blood_bank
  - pharmacy
  - ambulance
- A `facility` can have multiple `verification_requests`
- An `admin` can review many `verification_requests`
- An `admin` can create many `admin_logs`
- A `route` references one destination `facility`

### Relationship Style

The schema follows a hybrid approach:
- a shared base table for common facility data,
- specialized subtype tables for category-specific fields.

This avoids duplication and keeps the model extensible.

---

## 7. Normalization Strategy

The schema should follow at least Third Normal Form for Version 1.

### Benefits
- reduced duplication,
- easier updates,
- clearer business logic,
- cleaner expansion later.

### Example
Instead of storing hospital-specific fields in every facility row, store them in `hospitals`.

---

## 8. Indexing Strategy

Add indexes for fields used in search, filtering, and joining.

### Recommended Indexes
- `users.email`
- `users.role`
- `facilities.facility_type`
- `facilities.city`
- `facilities.latitude`
- `facilities.longitude`
- `facilities.is_verified`
- `facilities.is_active`
- `verification_requests.verification_status`
- `routes.destination_facility_id`

### Why
These fields support:
- login lookup,
- facility discovery,
- map-based search,
- verification workflow,
- ranking and filtering.

---

## 9. Validation Rules

### users
- email must be valid and unique
- password hash required
- role must be constrained

### facilities
- name required
- type required
- coordinates required
- contact number recommended
- active state must be explicit

### route fields
- distance and travel time must be non-negative

---

## 10. Entity Ownership and Auditability

The system should track:
- who created a facility,
- who verified it,
- who updated it,
- who reviewed it.

This improves trust and debugging.

---

## 11. Suggested Future Schema Extensions

In later versions, consider adding:

- emergency_reports
- facility_reviews
- live_availability_updates
- route_search_history
- emergency_alerts
- notification_logs
- ai_recommendation_logs

These are intentionally excluded from Version 1.

---

## 12. Summary

This schema is designed to support:

- secure authentication,
- verified medical resource management,
- search and filtering,
- route optimization,
- audit logging,
- and future AI expansion.

The schema should remain stable, normalized, and easy to extend.