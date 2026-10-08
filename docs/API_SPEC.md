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
Returns all facilities with optional filters.

**Query Parameters:**
- `name` (string, optional)
- `type` (string, optional: `Hospital`, `Blood Bank`, `Pharmacy`, `Ambulance`)
- `city` (string, optional)
- `verified` (boolean, optional)
- `page` (integer, optional)
- `size` (integer, optional)

### `GET /api/v1/facilities/{id}`
Returns a single facility by ID along with its subtype details.

### `POST /api/v1/facilities`
Creates a new facility (requires `PROVIDER` or `ADMIN` role).

### `PUT /api/v1/facilities/{id}`
Updates an existing facility (requires ownership or `ADMIN` role).

### `DELETE /api/v1/facilities/{id}`
Deletes or deactivates a facility (requires `ADMIN` role).

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
- **Response:** HTTP 200 OK with list of `VerificationRequestResponseDto`.

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
- **Response:** HTTP 200 OK with updated `VerificationRequestResponseDto` (`facilityVerified = true/false`).
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

## 16. Future Version 2 Expansion

Later versions may include:
- AI recommendation endpoints,
- Emergency triage guidance,
- Live bed/blood inventory WebSockets,
- Notification endpoints.

These are planned for Version 2 and are not part of Version 1.
