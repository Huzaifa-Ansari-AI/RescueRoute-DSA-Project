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
```

---

## Frontend Architecture

The frontend is organized into reusable modules:
- `pages/` for page-level views
- `components/` for reusable UI
- `routes/` for route handling
- `services/` for API calls
- `types/` for TypeScript types
- `utils/` for helper functions

### Current Frontend Features
- Home page
- OpenStreetMap map
- Karachi-centered view
- Facility markers
- Search by name
- Category filter
- Click marker to view details

---

## Backend Architecture

The backend follows a simple layered structure:
- `controller/` for REST endpoints
- `service/` for business logic
- `repository/` for database access
- `entity/` for database models
- `dto/` for request/response objects
- `exception/` for global error handling
- `response/` for common API response format
- `validation/` for validation error models

### Current Backend Features
- Global API response format (`ApiResponse<T>`)
- Custom exception handling (`GlobalExceptionHandler`)
- Health check endpoint (`GET /api/health`)
- Spring Boot base setup

---

## Data Flow
1. User opens the frontend.
2. Frontend loads the map and facility data.
3. User searches or filters facilities.
4. Frontend updates markers and list instantly.
5. In full milestones, frontend calls backend APIs for live data.
6. Backend returns standardized API responses.
7. Database stores facility and user data.

---

## Map Architecture

The map module uses:
- Leaflet for map rendering
- OpenStreetMap tiles
- Custom markers for each facility type
- Popups for facility details

### Facility Types
- Hospital
- Blood Bank
- Pharmacy
- Ambulance

---

## Current MVP Scope

The current version focuses on:
- working map UI
- facility markers
- search/filter
- backend foundation
- reusable components
- simple, maintainable code

---

## Future Scope

Later versions add:
- authentication
- provider dashboard
- admin dashboard
- live backend facility data
- route optimization
- ranking logic
- Agentic AI assistant

---

## Architecture Principles
- Keep it simple
- Reuse components
- Separate UI from business logic
- Keep API responses consistent
- Build small, working features first
- Avoid unnecessary complexity

---

## Future Version 2 Direction

Future AI-enabled enhancements may include:
- Agentic AI assistant
- emergency triage support
- natural language emergency guidance
- smarter facility ranking
- medical logistics routing
- predictive recommendation layer

The architecture must remain flexible enough to support these additions later.

---

## Coding Constraints for Generated Code

When implementation begins, generated code must:
- be complete,
- compile cleanly,
- include imports,
- include package declarations,
- include required configuration,
- follow project structure,
- avoid placeholders,
- avoid unfinished stubs,
- match the architecture in this document.

---

## Architecture Success Criteria

The architecture is successful if it enables a project that is:
- useful,
- technically strong,
- visually professional,
- modular,
- scalable,
- secure,
- maintainable,
- and impressive in a GitHub portfolio or interview.

---

## Final Instruction

This document is the architectural reference for the RescueRoute project.
Any future implementation should remain aligned with this architecture unless explicitly approved.
Before generating code, always check the current milestone against this architecture and follow it strictly.
