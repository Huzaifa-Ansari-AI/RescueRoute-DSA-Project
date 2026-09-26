<<<<<<< HEAD
# RescueRoute Architecture
=======
````markdown
# RescueRoute Architecture 
>>>>>>> 009bf5089ad60622a13d96b9ce11d514c2d0d3cd

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

<<<<<<< HEAD
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
=======
### Phase 3

Database schema and entity modeling

### Phase 4

Authentication and roles

### Phase 5

Facility registration modules

### Phase 6

Search, filtering, and ranking

### Phase 7

Graph and route optimization

### Phase 8

Admin verification and audit flow

### Phase 9

UI polish and integration

### Phase 10

Testing, documentation, and deployment readiness

Each phase should be independently understandable and buildable.

---

## 22. Future Version 2 Direction

Future AI-enabled enhancements may include:

* Agentic AI assistant,
* emergency triage support,
* natural language emergency guidance,
* smarter facility ranking,
* medical logistics routing,
* predictive recommendation layer.

The architecture must remain flexible enough to support these additions later.

---

## 23. Coding Constraints for Generated Code

When implementation begins, generated code must:

* be complete,
* compile cleanly,
* include imports,
* include package declarations,
* include required configuration,
* follow project structure,
* avoid placeholders,
* avoid unfinished stubs,
* match the architecture in this document.

---

## 24. Architecture Success Criteria

The architecture is successful if it enables a project that is:

* useful,
* technically strong,
* visually professional,
* modular,
* scalable,
* secure,
* maintainable,
* and impressive in a GitHub portfolio or interview.

---

## 25. Final Instruction

This document is the architectural reference for the RescueRoute project.

Any future implementation should remain aligned with this architecture unless I explicitly approve a change.

Before generating code, always check the current phase against this architecture and follow it strictly.
>>>>>>> 009bf5089ad60622a13d96b9ce11d514c2d0d3cd
