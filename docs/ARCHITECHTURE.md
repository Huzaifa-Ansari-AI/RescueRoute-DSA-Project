# RescueRoute Architecture

## Overview
RescueRoute is a full-stack web application that helps users find nearby medical resources during emergencies.

It displays and ranks:
- Hospitals
- Blood Banks
- Pharmacies
- Ambulance Services

The app is designed to be simple, fast, and easy to extend later with Agentic AI.

---

## Core Stack
### Frontend
- React
- TypeScript
- Vite
- Leaflet
- OpenStreetMap

### Backend
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Maven Wrapper

### Database
- PostgreSQL (Neon)

### Deployment
- Vercel for frontend
- Render for backend

---

## High-Level Architecture

```text
User
  ↓
React Frontend
  ↓
Spring Boot REST API
  ↓
Service Layer
  ↓
Repository Layer
  ↓
PostgreSQL Database
  ↓
OpenStreetMap / Leaflet
Frontend Architecture

The frontend is organized into reusable parts:

pages/ for screens
components/ for reusable UI
routes/ for route handling
services/ for API calls
types/ for TypeScript types
utils/ for helper functions
Current Frontend Features
Home page
OpenStreetMap map
Karachi-centered view
Facility markers
Search by name
Category filter
Click marker to view details
Backend Architecture

The backend follows a simple layered structure:

controller/ for REST endpoints
service/ for business logic
repository/ for database access
entity/ for database models
dto/ for request/response objects
exception/ for global error handling
response/ for common API response format
validation/ for validation error models
Current Backend Features
Global API response format
Custom exception handling
Health check endpoint
Spring Boot base setup
Data Flow
User opens the frontend.
Frontend loads the map and facility data.
User searches or filters facilities.
Frontend updates markers and list instantly.
Later, frontend will call backend APIs for live data.
Backend will return standardized API responses.
Database will store facility and user data.
Map Architecture

The map module uses:

Leaflet for map rendering
OpenStreetMap tiles
Custom markers for each facility type
Popups for facility details
Facility Types
Hospital
Blood Bank
Pharmacy
Ambulance
Current MVP Scope

The current version focuses on:

working map UI
facility markers
search/filter
backend foundation
reusable components
simple, maintainable code
Future Scope

Later versions may add:

authentication
provider dashboard
admin dashboard
live backend facility data
route optimization
ranking logic
Agentic AI assistant
Architecture Principles
Keep it simple
Reuse components
Separate UI from business logic
Keep API responses consistent
Build small, working features first
Avoid unnecessary complexity