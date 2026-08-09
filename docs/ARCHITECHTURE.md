Absolutely. Below is a **complete `docs/ARCHITECTURE.md`** you can paste directly into your repository. 

````markdown
# RescueRoute Architecture

**Project Name:** RescueRoute  
**Tagline:** Helping people find the right medical help at the right time.  
**Project Type:** Full-stack web application  
**Frontend:** React + TypeScript  
**Backend:** Java Spring Boot  
**Database:** PostgreSQL  
**Maps:** OpenStreetMap   
**Deployment:** Vercel + Render/Railway + Neon PostgreSQL  
**Version:** 1.0

---

## 1. Purpose of This Document

This document defines the technical architecture of RescueRoute.

Its purpose is to clearly explain:

- what the system does,
- how the application is structured,
- how frontend, backend, database, and map services interact,
- what modules and layers exist,
- what data structures and algorithms are used,
- how the system should be implemented phase by phase,
- and how the architecture supports future Agentic AI integration.

This document is a living technical reference for the project.

It should remain aligned with `MASTER_SPEC.md`, `DATABASE_SCHEMA.md`, `API_SPEC.md`, and `DEVELOPMENT_ROADMAP.md`.

---

## 2. Architectural Vision

RescueRoute is designed as a professional, production-style web application that solves a real-world emergency resource discovery problem.

The architecture must support:

- clean separation of concerns,
- maintainable backend services,
- reusable frontend components,
- secure authentication and authorization,
- scalable data modeling,
- graph-based route optimization,
- easy deployment,
- and future AI extension without a major rewrite.

The system must feel like a real product, not a classroom demo.

---

## 3. System Overview

RescueRoute helps users find the most suitable medical resources during emergencies.

Instead of simply showing the nearest place, it recommends the best option using multiple criteria:

- distance,
- travel time,
- emergency type,
- facility type,
- availability,
- verification status,
- suitability.

The system provides:

- search and filtering,
- facility ranking,
- route optimization,
- map integration,
- verified facility data,
- and a clean browser-based interface.

---

## 4. High-Level Architecture

The application follows a layered web architecture.

```text
React + TypeScript Frontend
        ↓
REST API Layer (Spring Boot Controllers)
        ↓
Application / Service Layer
        ↓
Domain Layer
        ↓
Repository / Persistence Layer
        ↓
PostgreSQL Database
        ↓
OpenStreetMap / Nominatim / OSRM Services
````

### Key Idea

* The frontend handles presentation.
* The backend handles business logic and security.
* The database stores persistent records.
* OpenStreetMap services provide location and route data.
* The graph engine performs route and ranking logic.

---

## 5. Architectural Style

RescueRoute uses a combination of:

* **Clean Architecture**
* **Layered Architecture**
* **Modular Monolith Design** for Version 1
* **REST-based Client-Server Architecture**

### Why this approach?

This gives the project:

* clear separation of responsibility,
* easy debugging,
* easier testing,
* simpler deployment,
* and a strong foundation for later modular expansion.

Version 1 should remain a single coherent product rather than many disconnected microservices.

---

## 6. Core System Layers

---

### 6.1 Presentation Layer

This layer is the browser-facing part of the application.

#### Responsibilities

* render pages and components,
* collect user input,
* display maps and search results,
* manage navigation,
* show forms, lists, dashboards, and authentication screens.

#### Technology

* React
* TypeScript
* React Router
* Axios
* UI component system of choice

#### Example Screens

* login page,
* registration page,
* home page,
* search page,
* facility results page,
* facility detail page,
* admin dashboard,
* facility registration page.

---

### 6.2 API Layer

This layer exposes backend endpoints to the frontend.

#### Responsibilities

* receive HTTP requests,
* validate request structure,
* return JSON responses,
* coordinate with service layer,
* map DTOs to responses.

#### Technology

* Spring Boot REST Controllers

#### Key Design Requirements

* use clear endpoint naming,
* use proper HTTP methods,
* return meaningful status codes,
* keep controllers thin.

---

### 6.3 Application / Service Layer

This is the business workflow layer.

#### Responsibilities

* execute use cases,
* coordinate multiple repositories and services,
* process search, filter, and ranking logic,
* call route optimization logic,
* enforce application rules,
* manage transaction boundaries if needed.

#### Examples

* authenticate user,
* register facility,
* verify facility,
* search medical resources,
* rank facilities,
* calculate route,
* return optimized recommendations.

#### Rule

Controllers must not contain business logic.
Business logic belongs here.

---

### 6.4 Domain Layer

This layer contains the business concepts of RescueRoute.

#### Responsibilities

* define core business entities,
* hold domain rules,
* represent facility types,
* define emergency types,
* model routes,
* support ranking and route logic.

#### Domain Concepts

* User
* Facility
* Hospital
* BloodBank
* Pharmacy
* Ambulance
* Route
* VerificationRequest
* AdminLog

#### Rule

The domain layer should remain independent of web and database details as much as practical.

---

### 6.5 Repository / Persistence Layer

This layer handles database access.

#### Responsibilities

* store and retrieve entities,
* perform CRUD operations,
* manage query methods,
* isolate persistence concerns from service logic.

#### Technology

* Spring Data JPA
* Hibernate
* PostgreSQL

#### Rule

Services should not contain raw SQL logic unless absolutely necessary.

---

### 6.6 External Services Layer

This layer handles outside integrations.

#### Responsibilities

* geocoding through Nominatim,
* route lookups through OSRM or compatible service,
* map coordinate conversion,
* future external API integration.

#### Rule

External API code should not be mixed directly into controllers or business logic.

---

## 7. Frontend Architecture

The frontend should follow a component-based architecture.

### Recommended Structure

```text
frontend/
├── src/
│   ├── app/
│   ├── assets/
│   ├── components/
│   ├── features/
│   ├── hooks/
│   ├── layouts/
│   ├── pages/
│   ├── routes/
│   ├── services/
│   ├── store/
│   ├── styles/
│   ├── types/
│   └── utils/
```

### Frontend Responsibilities

* user authentication screens,
* public landing page,
* location and search forms,
* filter controls,
* result cards,
* route visualization,
* facility detail view,
* admin controls.

### Frontend Design Principles

* reusable components,
* typed API contracts,
* clean routing,
* responsive layout,
* separation of shared and feature-specific logic.

---

## 8. Backend Architecture

The backend should be organized in a standard Spring Boot structure.

### Recommended Structure

```text
backend/
├── src/main/java/com/rescueroute/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── exception/
│   ├── repository/
│   ├── security/
│   ├── service/
│   ├── service/impl/
│   ├── mapper/
│   ├── config/
│   ├── integration/
│   ├── graph/
│   ├── algorithm/
│   └── utils/
```

### Backend Responsibilities

* authentication,
* authorization,
* facility management,
* route optimization,
* ranking,
* verification workflows,
* clean API responses,
* database integration,
* external service integration.

### Backend Design Principles

* controllers remain thin,
* services hold business logic,
* repositories handle persistence,
* DTOs isolate API contracts from entity models,
* exception handling is centralized.

---

## 9. Security Architecture

RescueRoute requires secure user and admin access.

### Security Mechanisms

* JWT authentication,
* BCrypt password hashing,
* role-based authorization,
* input validation,
* secure API access,
* protected admin endpoints.

### Roles

* USER
* PROVIDER
* ADMIN

### Security Rules

* never expose passwords,
* never expose internal tokens,
* never trust client-side data,
* validate all incoming requests,
* restrict sensitive operations to authorized roles only.

---

## 10. DSA Architecture

RescueRoute must demonstrate practical DSA usage.

### Data Structures

#### Graph

Represents locations and roads.

#### Weighted Graph

Represents route cost such as distance or time.

#### Adjacency List

Efficient representation of graph connections.

#### HashMap

For fast lookup of users, facilities, and graph nodes.

#### Priority Queue

For shortest-path processing and ranking.

#### ArrayList

For storing neighbors and facility collections.

#### Sorting

For ordering ranked facility recommendations.

---

### Algorithms

#### Dijkstra’s Algorithm

Used to compute the best route between locations.

#### Facility Ranking Algorithm

Used to rank results by:

* distance,
* verification,
* suitability,
* availability.

#### Search and Filter Algorithms

Used for:

* facility search,
* emergency filtering,
* type-based filtering.

---

## 11. Domain Model Architecture

### Primary Business Entities

* User
* Facility
* Hospital
* BloodBank
* Pharmacy
* Ambulance
* Route
* VerificationRequest
* AdminLog

### Facility Subtypes

A facility may be represented as a base type with specialized fields depending on category.

For example:

* Hospital-specific fields:

  * emergency department,
  * ICU,
  * cardiology,
  * trauma support.

* Blood bank-specific fields:

  * blood groups available,
  * stock status,
  * open hours.

* Pharmacy-specific fields:

  * medicines available,
  * emergency medicines,
  * open status.

* Ambulance-specific fields:

  * coverage area,
  * estimated response time,
  * availability.

---

## 12. Data Flow Architecture

### Typical User Flow

```text
User opens app
    ↓
User logs in or continues as visitor
    ↓
User enters emergency type and location
    ↓
Frontend sends request to backend
    ↓
Backend validates input
    ↓
Search service queries database
    ↓
Ranking engine scores results
    ↓
Routing engine computes best route
    ↓
Backend returns JSON response
    ↓
Frontend renders results and map
```

### Admin Flow

```text
Admin logs in
    ↓
Admin opens verification dashboard
    ↓
Admin reviews new facility submissions
    ↓
Admin approves or rejects facility
    ↓
System updates verification status
```

---

## 13. Routing and Mapping Architecture

RescueRoute integrates OpenStreetMap-compatible services.

### Responsibilities

* convert textual locations to coordinates,
* resolve user and facility coordinates,
* calculate route between two points,
* display map markers and paths.

### External Components

* Nominatim for geocoding,
* OSRM or another compatible routing service for route computation,
* OpenStreetMap tiles for visual mapping.

### Design Principle

External mapping logic should be wrapped inside integration services so that the rest of the application remains independent of provider-specific details.

---

## 14. Database Architecture

PostgreSQL is the source of truth for all persistent data.

### Database Design Principles

* normalized schema,
* clear foreign keys,
* UUIDs if appropriate,
* timestamps for auditability,
* separate verification records from primary facility data where needed.

### Core Tables

* users
* facilities
* hospitals
* blood_banks
* pharmacies
* ambulances
* routes
* verification_requests
* admin_logs

### Design Goal

The database should support:

* searching,
* filtering,
* ranking,
* verification,
* auditing,
* and future AI enhancement.

---

## 15. API Architecture

The backend should expose a clean REST API.

### API Design Principles

* use nouns for resources,
* use standard HTTP verbs,
* return clear response objects,
* use proper status codes,
* validate requests before processing.

### Example API Groups

* auth
* users
* facilities
* hospitals
* blood-banks
* pharmacies
* ambulances
* routes
* admin
* verification

### Response Design

Responses should be:

* predictable,
* consistent,
* easy for React to consume,
* suitable for pagination and filtering.

---

## 16. Error Handling Architecture

The backend must handle errors centrally.

### Requirements

* global exception handler,
* structured error messages,
* validation errors,
* authentication failures,
* authorization failures,
* resource-not-found handling,
* external service failure handling.

### Goal

Errors should be understandable for both developers and users.

---

## 17. Logging and Audit Architecture

The system should support meaningful logs.

### Needs

* admin actions,
* verification updates,
* route failures,
* API errors,
* authentication failures,
* integration failures.

### Admin Logs

Admin actions should be auditable to maintain trust and data quality.

---

## 18. Performance and Scalability Considerations

Version 1 should be efficient and responsive.

### Principles

* use indexed database fields for common lookups,
* avoid unnecessary frontend re-renders,
* keep controllers thin,
* keep search logic optimized,
* use efficient graph representation,
* minimize external API calls,
* cache or reuse results where sensible in later versions.

---

## 19. Deployment Architecture

The application should be deployment-ready.

### Frontend

Deploy on Vercel

### Backend

Deploy on Render or Railway

### Database

Use Neon PostgreSQL

### Build Strategy

* frontend and backend should be independently deployable,
* environment variables should be configurable,
* production and development settings should be separated.

---

## 20. Repository Strategy

The repository should be structured like a real professional project.

### Recommended Top-Level Structure

```text
RescueRoute/
├── frontend/
├── backend/
├── database/
├── docs/
├── screenshots/
├── demo/
├── .github/
├── README.md
├── LICENSE
└── CONTRIBUTING.md
```

### Documentation Folder

```text
docs/
├── MASTER_SPEC.md
├── ARCHITECTURE.md
├── DATABASE_SCHEMA.md
├── API_SPEC.md
├── DEVELOPMENT_ROADMAP.md
├── CODING_STANDARDS.md
├── DSA_DESIGN.md
├── FUTURE_SCOPE.md
└── adr/
```

---

## 21. Development Phases

The project should be implemented in phases.

### Phase 1

Architecture and documentation finalization

### Phase 2

Frontend and backend project setup

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
