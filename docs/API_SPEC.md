# RescueRoute API Specification

**Project Name:** RescueRoute  
**API Type:** REST API  
**Backend:** Java Spring Boot  
**Frontend:** React + TypeScript  
**Version:** 1.0  
**Base Format:** JSON over HTTP  

---

## 1. Purpose of This Document

This document defines the API structure for RescueRoute.

It describes:
- API design principles and rules,
- common request/response behavior,
- authentication flow,
- resource endpoints,
- role-based access patterns,
- error handling conventions.

This document is intended to keep frontend and backend implementation aligned.

---

## 2. API Design Goals

The API must be:
- clear,
- consistent,
- RESTful,
- secure,
- easy to consume from React,
- easy to extend in future versions,
- and suitable for production use.

---

## 3. API Design Principles & Rules

### General Principles
- Use nouns for resource names, not verbs.
- Use standard HTTP methods (`GET`, `POST`, `PUT`, `DELETE`).
- Return JSON responses for all endpoints.
- Return `ApiResponse<T>` wrapper for all successful responses.
- Validate request bodies on all writable endpoints.
- Return meaningful HTTP status codes.
- Keep controllers thin and place business logic in services.
- Protect write endpoints with authentication and roles.
- Do not expose internal entity implementation details.

### Example REST Style
- `GET /api/v1/facilities`
- `POST /api/v1/auth/register`
- `PUT /api/v1/facilities/{id}`
- `DELETE /api/v1/facilities/{id}`

---

## 4. Base API Structure

All version 1 APIs use a common prefix:
```text
/api/v1
```

### Common Response Format

#### Success
```json
{
  "success": true,
  "message": "Request successful",
  "data": {},
  "timestamp": "2026-08-02T12:00:00"
}
```

#### Error
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {
      "field": "name",
      "message": "Name is required"
    }
  ],
  "timestamp": "2026-08-02T12:00:00"
}
```

### Common Status Codes
- `200 OK` — Request succeeded
- `201 Created` — Resource created successfully
- `400 Bad Request` — Invalid input or validation failure
- `401 Unauthorized` — Authentication required or invalid token
- `403 Forbidden` — Authenticated user lacks permission
- `404 Not Found` — Resource does not exist
- `409 Conflict` — Resource already exists (e.g. duplicate email)
- `500 Internal Server Error` — Unexpected server failure

---

## 5. Health API

### `GET /api/v1/health`
Checks whether the backend is running. (Note: initial foundation also supports `/api/health`).

**Response:**
```json
{
  "success": true,
  "message": "Backend is running successfully",
  "data": {
    "status": "UP"
  },
  "timestamp": "2026-08-02T12:00:00"
}
```

---

## 6. Authentication APIs

### `POST /api/v1/auth/register`
Creates a new user account.

**Request:**
```json
{
  "fullName": "Huzaifa Ansari",
  "email": "huzaifa@example.com",
  "password": "password123",
  "role": "USER"
}
```

### `POST /api/v1/auth/login`
Logs in a user and returns a JWT token.

**Request:**
```json
{
  "email": "huzaifa@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "jwt-token-string",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": "uuid",
      "email": "huzaifa@example.com",
      "role": "USER"
    }
  }
}
```

---

## 7. Facility APIs

### `GET /api/v1/facilities`
Returns all active facilities with optional filters (`type`, `city`, `active`).
- Soft-deleted facilities (`isActive = false`) are excluded by default from public queries.

### `GET /api/v1/facilities/{id}`
Returns a single facility by ID along with its subtype details.
- Returns `404 Not Found` if the facility is deactivated/soft-deleted.

### `GET /api/v1/facilities/my`
Retrieves facilities owned and managed by the authenticated provider (`ROLE_PROVIDER`), or all facilities if called by `ROLE_ADMIN`.
- Returns both active and deactivated facilities, as well as `hasPendingUpdate` indicators.
- **Authorization:** `Bearer <JWT>` (Role: `PROVIDER` or `ADMIN`)

### `GET /api/v1/facilities/admin`
Retrieves all registered facilities for administrative registry management, independent of the verification queue.
- **Authorization:** `Bearer <JWT>` (Role: `ADMIN` strictly; other roles receive `403 Forbidden`)
- **Query Parameters:**
  - `type` (string, optional): Filter by facility type (`Hospital`, `Clinic`, `Blood Bank`, `Pharmacy`, `Ambulance`)
  - `city` (string, optional): Filter by city (e.g. `Karachi`)
  - `active` (boolean, optional): Filter by activation status (`true` for active, `false` for soft-deleted/deactivated; omit for all)
  - `verified` (boolean, optional): Filter by verification status (`true` for verified, `false` for unverified; omit for all)
- **Response:** HTTP 200 OK with list of `FacilityResponseDto` including live lifecycle fields (`isActive`, `deletedAt`, `isVerified`, `hasPendingUpdate`).

### `POST /api/v1/facilities`
Creates a new facility and automatically creates an unreviewed verification request.
- **Authorization:** `Bearer <JWT>` (Role: `PROVIDER` or `ADMIN`)
- Returns `201 Created` with `FacilityResponseDto` (`isVerified = false`).

### `PUT /api/v1/facilities/{id}`
Submits staged updates for an existing facility.
- **Authorization:** `Bearer <JWT>` (Role: `PROVIDER` owning the facility, or `ADMIN`)
- **Staged Re-Verification Workflow:**
  - Material changes (name, address, city, coordinates, contact, emergency readiness, ICU/trauma capabilities) are captured in `proposedChanges` JSON and submitted as an `UPDATE` verification request.
  - Sets `hasPendingUpdate = true` on the facility.
  - The live public facility directory continues serving the verified data until an administrator reviews and approves the staged update.
  - Prevents IDOR: A provider attempting to edit another provider's facility receives `403 Forbidden`.
  - Protects immutable and sensitive fields: IDs, ownership, audit timestamps, and active flags cannot be altered.

### `DELETE /api/v1/facilities/{id}`
Soft-deletes a facility (`isActive = false`, records `deletedAt` timestamp).
- **Authorization:** `Bearer <JWT>` (Role: `PROVIDER` owning the facility, or `ADMIN`)
- Allows administrators to deactivate any approved facility directly from the admin management interface.
- Preserves historical verification status (`isVerified = true` remains).
- Immediately cancels any pending verification requests for this facility.
- Audited with `FACILITY_DEACTIVATED` in `admin_logs`.
- Immediately excludes the facility from public search, maps, Top-K ranking, and direct lookup.

### `POST /api/v1/facilities/{id}/reactivate`
Restores a previously soft-deleted facility (`isActive = true`, clears `deletedAt`).
- **Authorization:** `Bearer <JWT>` (Role: `ADMIN` strictly)
- Audited with `FACILITY_REACTIVATED` in `admin_logs`.
- Makes the facility discoverable again in public search and direct lookups.

---

## 8. Category-Specific APIs

### Hospitals
- `GET /api/v1/hospitals`
- `GET /api/v1/hospitals/{id}`
- `POST /api/v1/hospitals`
- `PUT /api/v1/hospitals/{id}`
- `DELETE /api/v1/hospitals/{id}`

### Blood Banks
- `GET /api/v1/blood-banks`
- `GET /api/v1/blood-banks/{id}`
- `POST /api/v1/blood-banks`
- `PUT /api/v1/blood-banks/{id}`
- `DELETE /api/v1/blood-banks/{id}`

### Pharmacies
- `GET /api/v1/pharmacies`
- `GET /api/v1/pharmacies/{id}`
- `POST /api/v1/pharmacies`
- `PUT /api/v1/pharmacies/{id}`
- `DELETE /api/v1/pharmacies/{id}`

### Ambulances
- `GET /api/v1/ambulances`
- `GET /api/v1/ambulances/{id}`
- `POST /api/v1/ambulances`
- `PUT /api/v1/ambulances/{id}`
- `DELETE /api/v1/ambulances/{id}`

---

## 9. Search & Discovery APIs

### `GET /api/v1/facilities/search`
Performs two-stage hybrid ranking (Haversine Min-Heap candidate shortlist + OSRM road matrix evaluation) returning top-K prioritized facilities.

**Query Parameters:**
- `lat` (double, required): User latitude
- `lng` (double, required): User longitude
- `type` (string, optional): Facility category filter (`Hospital`, `Clinic`, `Blood Bank`, `Pharmacy`, `Ambulance`)
- `emergency` (boolean, optional, default: `false`): Prioritizes 24/7 emergency readiness, ICU, and trauma centers
- `k` (integer, optional, default: `10`): Limit on top-ranked facilities returned

**Response Schema (`RankedFacilityResponseDto`):**
```json
{
  "success": true,
  "message": "Ranked facilities retrieved successfully",
  "data": [
    {
      "id": "c1a2b3c4-d5e6-7890-abcd-ef1234567890",
      "name": "Jinnah Postgraduate Medical Centre (JPMC)",
      "facilityType": "Hospital",
      "address": "Rafiqui Shaheed Road, Karachi",
      "city": "Karachi",
      "latitude": 24.8524,
      "longitude": 67.0456,
      "phoneNumber": "021-99201300",
      "isVerified": true,
      "isActive": true,
      "distanceKm": 6.6,
      "straightLineDistanceKm": 6.6,
      "drivingDistanceKm": 8.7,
      "estimatedTravelMinutes": 11,
      "distanceMetricUsed": "ROAD_TRAVEL_TIME",
      "routingAvailable": true,
      "score": 85.5,
      "isEmergencyReady": true,
      "hospitalDetails": {
        "emergencyAvailable": true,
        "icuAvailable": true,
        "traumaCenterAvailable": true
      }
    }
  ],
  "timestamp": "2026-10-29T12:00:00"
}
```

**Dual-Distance & Routing Metric Fields:**
- `distanceKm`: Preserved for backward compatibility (straight-line Haversine distance in km, rounded to 1 decimal).
- `straightLineDistanceKm`: Explicit straight-line Haversine distance in km (rounded to 1 decimal).
- `drivingDistanceKm`: Real-world OSRM driving distance in km (rounded to 1 decimal; `null` if routing unavailable).
- `estimatedTravelMinutes`: OSRM road network estimated travel duration in minutes (`null` if routing unavailable).
- `distanceMetricUsed`: `ROAD_TRAVEL_TIME` when OSRM road matrix succeeds; `HAVERSINE_FALLBACK` when OSRM fails, times out, or candidates are unreachable.
- `routingAvailable`: Boolean indicating whether road-network driving metrics were successfully obtained.

### `GET /api/v1/search`
Searches facilities by keyword, type, city, and verification status.

### `GET /api/v1/search/map`
Returns lightweight facility markers for map rendering (`id`, `name`, `type`, `latitude`, `longitude`, `isVerified`).

---

## 10. Route APIs

### `POST /api/v1/routes/calculate`
Calculates route details from user location to a selected facility using OSRM.

**Request:**
```json
{
  "sourceLatitude": 24.8607,
  "sourceLongitude": 67.0011,
  "destinationFacilityId": 1
}
```

**Response:**
```json
{
  "success": true,
  "message": "Route calculated successfully",
  "data": {
    "distanceKm": 8.5,
    "travelTimeMinutes": 22,
    "destinationFacilityId": 1
  }
}
```

---

## 11. Verification APIs

### `POST /api/v1/facilities`
Creates a new facility and automatically creates an unreviewed verification request (Provider only).
- **Authorization:** `Bearer <JWT>` (Role: `PROVIDER`)
- **Request:**
```json
{
  "name": "Clifton Surgical Hospital",
  "facilityType": "Hospital",
  "address": "Block 5, Clifton",
  "city": "Karachi",
  "latitude": 24.8235,
  "longitude": 67.0312,
  "phoneNumber": "021-35870000",
  "hospitalDetails": {
    "emergencyAvailable": true,
    "icuAvailable": true,
    "traumaCenterAvailable": true,
    "notes": "Tertiary trauma unit"
  }
}
```
- **Response:** HTTP 201 Created with `FacilityResponseDto` (`isVerified = false`).

### `GET /api/v1/admin/verification-requests`
Returns pending or filtered verification requests with full facility and submitter context (Admin only).
- **Authorization:** `Bearer <JWT>` (Role: `ADMIN`)
- **Query Params:** `status` (e.g. `PENDING`, `APPROVED`, `REJECTED`)
- **Response:** HTTP 200 OK with list of `VerificationRequestResponseDto` including:
  - `requestType`: `"CREATION"` (new facility registration) or `"UPDATE"` (staged facility modification).
  - `proposedChanges`: Serialized JSON string of the proposed fields when `requestType = "UPDATE"`.
  - `facilityActive`: Boolean flag indicating live directory activation status (`true` = active, `false` = soft-deleted/deactivated).
  - `facilityDeletedAt`: ISO-8601 deletion timestamp if deactivated, or `null` if active.
  - `facilityHasPendingUpdate`: Boolean flag indicating if unapproved staged modifications are pending review.

### `PUT /api/v1/admin/verification-requests/{id}`
Approves or rejects a verification request with audit logging (Admin only).
- **Authorization:** `Bearer <JWT>` (Role: `ADMIN`)
- **Request:**
```json
{
  "decision": "APPROVED",
  "reviewNotes": "Sindh Healthcare Commission license verified"
}
```
- **Response:** HTTP 200 OK with updated `VerificationRequestResponseDto`.
- **Workflow Mechanics:**
  - For `CREATION` requests: Approving marks the facility as `isVerified = true`.
  - For `UPDATE` requests: Approving unpacks `proposedChanges`, applies the approved fields to the live facility, marks the facility as `isVerified = true`, and clears `hasPendingUpdate = false`. Rejecting discards the proposed changes and clears `hasPendingUpdate = false` without corrupting live data.
- **Convenience routes:** Also accessible via `PUT /api/v1/admin/verification-requests/{id}/approve` and `PUT /api/v1/admin/verification-requests/{id}/reject`.

---

## 12. Admin APIs

### `GET /api/v1/admin/dashboard`
Returns high-level admin metrics and count summary.

### `GET /api/v1/admin/logs`
Returns admin activity audit logs.

---

## 13. Validation Rules

- **Authentication:** Valid email format; password required (minimum length 8).
- **Facility:** Name, facility type, latitude, and longitude required.
- **Search:** Coordinates must be valid decimal values (-90 to +90 lat, -180 to +180 lng).

---

## 14. Pagination Format

List endpoints support pagination:
```json
{
  "success": true,
  "message": "Facilities retrieved successfully",
  "data": {
    "items": [],
    "page": 0,
    "size": 10,
    "totalItems": 52,
    "totalPages": 6
  }
}
```

---

## 15. Frontend Contract Expectations

The React frontend consumes the API following these contracts:
- Predictable JSON structures matching `ApiResponse<T>`,
- Clean DTOs with no internal JPA entity leakage,
- Consistent HTTP status codes,
- Standard authorization header: `Authorization: Bearer <token>`.

---

---

## 17. External Facilities API (Phase 4: OpenStreetMap / Overpass Integration)

Endpoints for querying, importing, and moderating crowd-sourced medical points of interest from OpenStreetMap Overpass. External listings are stored in a dedicated `external_facilities` table, clearly labeled with attribution and clinical disclaimers, and excluded from trusted emergency ranking.

### `GET /api/v1/external-facilities`
Searches discovered external facilities within a geographic radius.
- **Authorization:** Public (Permit All)
- **Query Parameters:**
  - `lat` (double, required): Latitude [-90.0, 90.0]
  - `lng` (double, required): Longitude [-180.0, 180.0]
  - `radiusMeters` (double, optional, default: 5000): Bounded radius in meters (100 to 10,000)
  - `type` (string, optional): Facility category filter (`Hospital`, `Clinic`, `Pharmacy`, `Blood Bank`, `Ambulance`)
  - `status` (string, optional): Import status filter (`DISCOVERED`, `DUPLICATE_CANDIDATE`, `REVIEWED`, `SUPPRESSED`)
- **Response Shape (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "External facilities retrieved successfully",
  "data": [
    {
      "id": "e4f8d2a1-...",
      "source": "OPENSTREETMAP",
      "osmElementType": "node",
      "osmElementId": 1001,
      "sourceUrl": "https://www.openstreetmap.org/node/1001",
      "name": "Civil Hospital Karachi",
      "facilityType": "Hospital",
      "osmTags": "{\"amenity\":\"hospital\",\"name\":\"Civil Hospital Karachi\"}",
      "latitude": 24.8615,
      "longitude": 67.0025,
      "address": "Mission Road",
      "city": "Karachi",
      "phoneNumber": "+92 21 99215740",
      "website": null,
      "openingHours": "24/7",
      "importStatus": "DISCOVERED",
      "matchedFacilityId": null,
      "matchedFacilityName": null,
      "distanceKm": 0.85,
      "attribution": "© OpenStreetMap contributors",
      "disclaimer": "External listing · OpenStreetMap · Not verified by RescueRoute",
      "isVerified": false,
      "importedAt": "2026-10-30T00:30:00Z",
      "lastSeenAt": "2026-10-30T00:30:00Z"
    }
  ]
}
```

### `POST /api/v1/external-facilities/import`
Triggers a bounded geographic query against the Overpass API, normalizes medical tags, performs Level 2 duplicate detection against internal facilities, and idempotently upserts records.
- **Authorization:** `Bearer <JWT>` (Roles: `ADMIN`, `PROVIDER`)
- **Request Body:**
```json
{
  "latitude": 24.8607,
  "longitude": 67.0011,
  "radiusMeters": 5000,
  "facilityType": "Hospital"
}
```
- **Response Shape (HTTP 200 OK):**
```json
{
  "success": true,
  "message": "Successfully processed 14 facilities (10 imported, 4 updated, 2 duplicate candidates flagged).",
  "data": {
    "totalQueried": 14,
    "imported": 10,
    "updated": 4,
    "duplicateCandidates": 2,
    "skipped": 0,
    "failed": 0,
    "executionTimeMs": 340,
    "message": "Successfully processed 14 facilities (10 imported, 4 updated, 2 duplicate candidates flagged)."
  }
}
```

### `GET /api/v1/external-facilities/{id}`
Retrieves detailed normalized attributes of an external facility by its internal UUID.
- **Authorization:** Public (Permit All)
- **Response:** HTTP 200 OK with `ExternalFacilityResponseDto` or HTTP 404 Not Found.

### `PUT /api/v1/external-facilities/{id}/status`
Moderates an external facility's status (Admin only).
- **Authorization:** `Bearer <JWT>` (Role: `ADMIN`)
- **Request Body:**
```json
{
  "status": "REVIEWED",
  "notes": "Verified facility license and active status"
}
```
- **Response Shape (HTTP 200 OK):** HTTP 200 OK with updated `ExternalFacilityResponseDto`. Valid statuses: `DISCOVERED`, `DUPLICATE_CANDIDATE`, `REVIEWED`, `SUPPRESSED`.

