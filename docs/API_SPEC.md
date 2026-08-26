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

```text
/api/v1

Example:

/api/v1/auth/login
/api/v1/facilities
/api/v1/routes/search
5. Authentication Model

RescueRoute uses:

JWT authentication,
BCrypt password hashing,
role-based authorization.
Roles
USER
PROVIDER
ADMIN
Security Rules
public endpoints require no token,
protected endpoints require valid JWT,
admin operations require ADMIN role,
provider operations require PROVIDER or ADMIN role,
user operations require USER, PROVIDER, or ADMIN as applicable.
6. Common Response Format

All responses should follow a predictable structure.

Success Response
{
  "success": true,
  "message": "Request completed successfully",
  "data": {}
}
Error Response
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
Notes
data may contain an object, array, or null.
errors should be present when validation fails.
7. Pagination and Filtering

For list endpoints, support:

pagination,
filtering,
sorting where relevant.
Common Query Parameters
page
size
sortBy
sortDir
facilityType
emergencyType
city
verifiedOnly
availableOnly
8. Authentication APIs
8.1 Register User

Endpoint

POST /api/v1/auth/register

Purpose
Create a new user account.

Request Body

{
  "fullName": "Huzaifa Ansari",
  "email": "huzaifa@example.com",
  "phoneNumber": "03001234567",
  "password": "StrongPassword123",
  "role": "USER"
}

Success Response

{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": "uuid",
    "fullName": "Huzaifa Ansari",
    "email": "huzaifa@example.com",
    "role": "USER"
  }
}

Status Codes

201 Created
400 Bad Request
409 Conflict
8.2 Login

Endpoint

POST /api/v1/auth/login

Purpose
Authenticate user and return JWT.

Request Body

{
  "email": "huzaifa@example.com",
  "password": "StrongPassword123"
}

Success Response

{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "jwt-token",
    "user": {
      "id": "uuid",
      "fullName": "Huzaifa Ansari",
      "email": "huzaifa@example.com",
      "role": "USER"
    }
  }
}

Status Codes

200 OK
401 Unauthorized
400 Bad Request
8.3 Get Current User

Endpoint

GET /api/v1/auth/me

Purpose
Return current authenticated user details.

Authorization
Bearer JWT required

9. User APIs
9.1 Get Profile

Endpoint

GET /api/v1/users/me

Purpose
Fetch user profile.

Authorization
Bearer JWT required

9.2 Update Profile

Endpoint

PUT /api/v1/users/me

Purpose
Update profile fields such as name or phone number.

Request Body

{
  "fullName": "Updated Name",
  "phoneNumber": "03009998877"
}
10. Facility APIs
10.1 Create Facility

Endpoint

POST /api/v1/facilities

Purpose
Create a new facility base record.

Authorization
Provider or Admin required

Request Body

{
  "facilityName": "City Care Hospital",
  "facilityType": "HOSPITAL",
  "address": "Main Road, Karachi",
  "city": "Karachi",
  "latitude": 24.8607,
  "longitude": 67.0011,
  "contactNumber": "02112345678",
  "email": "info@citycare.com",
  "operatingHours": "24/7"
}
10.2 Get All Facilities

Endpoint

GET /api/v1/facilities

Purpose
List facilities with filters and pagination.

Query Parameters

facilityType
city
verifiedOnly
availableOnly
page
size
10.3 Get Facility by ID

Endpoint

GET /api/v1/facilities/{id}

Purpose
Return detailed facility information.

10.4 Update Facility

Endpoint

PUT /api/v1/facilities/{id}

Purpose
Update facility data.

Authorization
Provider or Admin required

10.5 Delete Facility

Endpoint

DELETE /api/v1/facilities/{id}

Purpose
Deactivate or remove a facility.

Authorization
Admin required or strict ownership-based logic if implemented

11. Hospital APIs
11.1 Create Hospital Profile

Endpoint

POST /api/v1/hospitals

Purpose
Create hospital-specific details for a facility.

Authorization
Provider or Admin required

Request Body

{
  "facilityId": "uuid",
  "emergencyDepartmentAvailable": true,
  "icuAvailable": true,
  "traumaCenterAvailable": true,
  "cardiologyAvailable": true,
  "neurologyAvailable": false,
  "pediatricsAvailable": true,
  "maternityAvailable": true,
  "specialNotes": "24/7 emergency and trauma support"
}
11.2 Get Hospital Details

Endpoint

GET /api/v1/hospitals/{facilityId}
12. Blood Bank APIs
12.1 Create Blood Bank Profile

Endpoint

POST /api/v1/blood-banks

Purpose
Create blood-bank-specific data for a facility.

Request Body

{
  "facilityId": "uuid",
  "bloodGroupOPositive": true,
  "bloodGroupONegative": true,
  "bloodGroupAPositive": true,
  "bloodGroupANegative": false,
  "bloodGroupBPositive": true,
  "bloodGroupBNegative": false,
  "bloodGroupABPositive": true,
  "bloodGroupABNegative": false,
  "specialNotes": "Emergency stock available"
}
12.2 Get Blood Bank Details

Endpoint

GET /api/v1/blood-banks/{facilityId}
13. Pharmacy APIs
13.1 Create Pharmacy Profile

Endpoint

POST /api/v1/pharmacies

Purpose
Create pharmacy-specific data.

Request Body

{
  "facilityId": "uuid",
  "emergencyMedicinesAvailable": true,
  "prescriptionRequired": true,
  "open24Hours": true,
  "homeDeliveryAvailable": false,
  "specialNotes": "Emergency medicines available"
}
13.2 Get Pharmacy Details

Endpoint

GET /api/v1/pharmacies/{facilityId}
14. Ambulance APIs
14.1 Create Ambulance Profile

Endpoint

POST /api/v1/ambulances

Purpose
Create ambulance service data.

Request Body

{
  "facilityId": "uuid",
  "responseTimeMinutes": 15,
  "coverageArea": "Karachi",
  "availableNow": true,
  "equippedForIcuTransfer": true,
  "specialNotes": "Available within Karachi city limits"
}
14.2 Get Ambulance Details

Endpoint

GET /api/v1/ambulances/{facilityId}
15. Search and Ranking APIs
15.1 Search Facilities

Endpoint

GET /api/v1/search/facilities

Purpose
Search and rank facilities based on emergency and location.

Query Parameters

emergencyType
facilityType
latitude
longitude
city
verifiedOnly
availableOnly
radiusKm

Example

/api/v1/search/facilities?emergencyType=TRAUMA&latitude=24.86&longitude=67.00&verifiedOnly=true

Response
Returns ranked facility results with route details.

15.2 Get Ranked Recommendations

Endpoint

GET /api/v1/search/recommendations

Purpose
Return the best recommended facilities for a user context.

16. Routing APIs
16.1 Calculate Route

Endpoint

POST /api/v1/routes/calculate

Purpose
Calculate best route from user location to facility.

Request Body

{
  "sourceLatitude": 24.8607,
  "sourceLongitude": 67.0011,
  "destinationFacilityId": "uuid",
  "emergencyType": "TRAUMA"
}

Success Response

{
  "success": true,
  "message": "Route calculated successfully",
  "data": {
    "distanceKm": 8.6,
    "travelTimeMinutes": 21,
    "destinationFacilityId": "uuid"
  }
}
16.2 Get Route History

Endpoint

GET /api/v1/routes

Purpose
Return saved route records if route history is stored.

17. Verification APIs
17.1 Submit Verification Request

Endpoint

POST /api/v1/verification-requests

Purpose
Submit facility verification request.

Authorization
Provider required

17.2 Get Pending Verification Requests

Endpoint

GET /api/v1/admin/verification-requests/pending

Authorization
Admin required

17.3 Approve Verification Request

Endpoint

PUT /api/v1/admin/verification-requests/{id}/approve

Authorization
Admin required

17.4 Reject Verification Request

Endpoint

PUT /api/v1/admin/verification-requests/{id}/reject

Authorization
Admin required

18. Admin APIs
18.1 Get Admin Dashboard Data

Endpoint

GET /api/v1/admin/dashboard

Purpose
Return dashboard summary metrics.

Authorization
Admin required

18.2 Get Admin Logs

Endpoint

GET /api/v1/admin/logs

Purpose
Return audit logs for admin activity.

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
