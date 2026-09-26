<<<<<<< HEAD
# RescueRoute API Spec

## Overview
RescueRoute uses a REST API for all frontend and backend communication.

All endpoints return JSON.
=======
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

- the API design principles,
- common request/response behavior,
- authentication flow,
- resource endpoints,
- role-based access patterns,
- and error handling conventions.

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

## 3. API Design Principles

### General Principles

- use nouns for resource names,
- use standard HTTP methods,
- return JSON responses,
- validate request bodies,
- return meaningful HTTP status codes,
- keep endpoints predictable,
- do not expose internal implementation details.

### Example REST Style

- `GET /api/facilities`
- `POST /api/auth/register`
- `PUT /api/facilities/{id}`
- `DELETE /api/facilities/{id}`

---

## 4. Base API Structure

All version 1 APIs should use a common prefix:
>>>>>>> 009bf5089ad60622a13d96b9ce11d514c2d0d3cd

Base path:
```text
/api/v1
Response Format
Success
{
  "success": true,
  "message": "Request successful",
  "data": {},
  "timestamp": "2026-08-02T12:00:00"
}
Error
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
Status Codes
200 OK — success
201 Created — resource created
400 Bad Request — invalid input
401 Unauthorized — not logged in
403 Forbidden — not allowed
404 Not Found — resource missing
500 Internal Server Error — unexpected failure
Health API
GET /api/v1/health

Checks whether the backend is running.

Response

{
  "success": true,
  "message": "Backend is running",
  "data": {
    "status": "UP"
  }
}
Authentication APIs
POST /api/v1/auth/register

Creates a new user account.

Request

{
  "fullName": "Huzaifa Ansari",
  "email": "huzaifa@example.com",
  "password": "password123",
  "role": "USER"
}
POST /api/v1/auth/login

Logs in a user and returns a token.

Request

{
  "email": "huzaifa@example.com",
  "password": "password123"
}
Facility APIs
GET /api/v1/facilities

Returns all facilities with optional filters.

Query params

name
type
city
verified
page
size
GET /api/v1/facilities/{id}

Returns one facility by ID.

POST /api/v1/facilities

Creates a new facility.

PUT /api/v1/facilities/{id}

Updates a facility.

DELETE /api/v1/facilities/{id}

Deletes or deactivates a facility.

Category-Specific APIs
Hospitals
GET /api/v1/hospitals
GET /api/v1/hospitals/{id}
POST /api/v1/hospitals
PUT /api/v1/hospitals/{id}
DELETE /api/v1/hospitals/{id}
Blood Banks
GET /api/v1/blood-banks
GET /api/v1/blood-banks/{id}
POST /api/v1/blood-banks
PUT /api/v1/blood-banks/{id}
DELETE /api/v1/blood-banks/{id}
Pharmacies
GET /api/v1/pharmacies
GET /api/v1/pharmacies/{id}
POST /api/v1/pharmacies
PUT /api/v1/pharmacies/{id}
DELETE /api/v1/pharmacies/{id}
Ambulances
GET /api/v1/ambulances
GET /api/v1/ambulances/{id}
POST /api/v1/ambulances
PUT /api/v1/ambulances/{id}
DELETE /api/v1/ambulances/{id}
Search APIs
GET /api/v1/search

Searches facilities by name, type, city, and verification status.

GET /api/v1/search/map

Returns facilities in a map-friendly format for marker display.

Route APIs
POST /api/v1/routes/calculate

Calculates route details from user location to a selected facility.

Request

{
  "sourceLatitude": 24.8607,
  "sourceLongitude": 67.0011,
  "destinationFacilityId": 1
}

Response

{
  "success": true,
  "message": "Route calculated successfully",
  "data": {
    "distanceKm": 8.5,
    "travelTimeMinutes": 22,
    "destinationFacilityId": 1
  }
}
Verification APIs
POST /api/v1/verification-requests

Creates a verification request for a facility.

GET /api/v1/admin/verification-requests

Returns all pending verification requests.

PUT /api/v1/admin/verification-requests/{id}/approve

Approves a verification request.

PUT /api/v1/admin/verification-requests/{id}/reject

Rejects a verification request.

Admin APIs
GET /api/v1/admin/dashboard

Returns basic admin summary data.

GET /api/v1/admin/logs

Returns admin activity logs.

<<<<<<< HEAD
API Rules
Use nouns, not verbs, in endpoint names.
Keep request and response JSON consistent.
Use validation for all writable endpoints.
Keep controllers thin.
Return ApiResponse for all responses.
Protect write endpoints with authentication and roles.
=======
19. Error Handling

All errors should return standardized JSON.

Example Validation Error
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Email is required"
    }
  ]
}
Common Status Codes
200 OK
201 Created
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
500 Internal Server Error
20. Validation Rules
Authentication
email required
password required
password length requirements apply
Facility
name required
type required
coordinates required
contact number recommended
Search
emergency type required in most search contexts
coordinates should be valid decimal values if provided
21. Pagination Format

For list endpoints, response metadata should support pagination.

Example
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
22. Frontend Contract Expectations

The React frontend should be able to consume the API easily.

Requirements
predictable JSON structures,
clean DTOs,
stable endpoint contracts,
consistent status codes,
no leaking of entity internals,
no dependency on database schema details.
23. Future API Expansion

Later versions may include:

AI recommendation endpoints,
emergency triage endpoints,
live availability endpoints,
notification endpoints,
review endpoints,
analytics endpoints.

These are not part of Version 1.

24. Summary

This API specification defines a secure, structured, and extensible REST API for RescueRoute.

It supports:

user authentication,
facility management,
search and ranking,
route optimization,
admin verification,
and future AI expansion.
>>>>>>> 009bf5089ad60622a13d96b9ce11d514c2d0d3cd
