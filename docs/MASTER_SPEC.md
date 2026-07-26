# RescueRoute — Master Specification

**Project Name:** RescueRoute  
**Tagline:** Helping people find the right medical help at the right time.  
**Project Type:** Full-stack web application  
**Version:** 1.0 (Core Platform Only)  
**Future Version:** Agentic AI integration after core completion  

---

## 1. Purpose of This Document

This document is the single source of truth for the RescueRoute project.

Its purpose is to define:

- the project vision,
- the problem being solved,
- the scope of Version 1,
- the technical stack,
- the system architecture,
- the required modules,
- the data structures and algorithms,
- the database design direction,
- the coding and architectural standards,
- the phased development workflow,
- the constraints for code generation,
- and the future direction of the project.

This document must be read and followed before writing any code.

Claude should use this file as the primary reference for all future implementation phases.

---

## 2. Project Vision

RescueRoute is a production-quality, full-stack web application designed to help users quickly find the most suitable medical resources during emergencies.

The application should not behave like a simple map search tool.

Instead, it should intelligently rank and recommend the best medical resource based on:

- distance,
- travel time,
- emergency type,
- facility type,
- availability,
- verification status,
- suitability.

The project is intended to become a flagship portfolio project that demonstrates:

- strong full-stack engineering,
- software architecture,
- system design,
- data structures and algorithms,
- backend development,
- frontend development,
- database design,
- and real-world problem solving in the healthcare domain.

Version 1 focuses only on the core platform.

Agentic AI will be added later after the core system is complete.

---

## 3. Problem Statement

During medical emergencies, people often waste valuable time because they do not know:

- which hospital is most suitable,
- which hospital supports their emergency,
- where the nearest blood bank is,
- which pharmacy is open,
- which ambulance service is nearby,
- which facility is verified,
- which facility is the best option for the situation.

Current map tools are mostly location-centric.

They show nearby places, but they do not intelligently recommend medical resources based on emergency relevance, verification, and suitability.

RescueRoute solves this problem by combining:

- location awareness,
- verified facility data,
- emergency-aware filtering,
- intelligent ranking,
- and route optimization.

---

## 4. Project Goals

### Primary Goals

- help users locate the best medical resources quickly,
- demonstrate practical use of DSA,
- demonstrate scalable backend architecture,
- demonstrate clean frontend architecture,
- create a deployment-ready web application,
- create an impressive open-source GitHub portfolio project,
- build something that feels useful in the real world.

### Secondary Goals

- keep the architecture extensible for future AI integration,
- make the project easy to explain in interviews,
- ensure the UI is clean and professional,
- ensure the codebase is modular and maintainable,
- create a strong foundation for future versions.

---

## 5. Version 1 Scope

Version 1 is the core platform only.

### Included in Version 1

- user registration,
- user login,
- role-based access,
- hospital module,
- blood bank module,
- pharmacy module,
- ambulance module,
- facility registration,
- facility verification,
- search and filtering,
- ranking engine,
- route optimization,
- OpenStreetMap integration,
- map-based UI,
- backend REST APIs,
- PostgreSQL persistence,
- admin dashboard basics,
- deployment-ready structure.

### Excluded from Version 1

The following must **not** be implemented in Version 1:

- Agentic AI,
- chatbots,
- LLM integration,
- machine learning,
- prediction models,
- traffic forecasting,
- hospital occupancy prediction,
- voice assistant,
- notification automation.

These are reserved for Version 2.

---

## 6. Target Users and Roles

### 6.1 Patient / Visitor

A user who searches for help during an emergency.

Can:

- register and log in,
- enter or share location,
- search nearby facilities,
- filter by emergency type,
- view ranked recommendations,
- view route and contact details.

### 6.2 Facility Provider

A hospital, blood bank, pharmacy, or ambulance provider.

Can:

- register facility details,
- update facility information,
- provide services offered,
- submit verification request,
- keep data updated.

### 6.3 Administrator

A trusted system user responsible for quality control.

Can:

- verify facilities,
- reject invalid listings,
- edit incorrect records,
- manage facility trust status,
- monitor admin logs.

---

## 7. Technology Stack

### Frontend
- React
- TypeScript
- Vite
- React Router
- Axios
- Responsive UI components

### Backend
- Java
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- Hibernate
- Bean Validation

### Database
- PostgreSQL

### Maps / Routing
- OpenStreetMap
- Nominatim
- OSRM or another compatible free routing service if needed

### Authentication
- JWT-based authentication
- BCrypt password hashing
- role-based authorization

### Deployment
- Frontend: Vercel
- Backend: Render or Railway
- Database: Neon PostgreSQL

### Version Control
- Git
- GitHub

### Build / Tooling
- Maven for backend
- npm or pnpm for frontend
- GitHub Actions for CI if needed later

---

## 8. Non-Functional Requirements

The system must be designed with the following qualities:

- maintainable,
- scalable,
- modular,
- secure,
- readable,
- production-oriented,
- easy to deploy,
- easy to extend,
- testable,
- well documented,
- recruiter-friendly,
- cleanly structured.

---

## 9. Core Functional Requirements

### Authentication and Users

- register a new user,
- log in existing user,
- maintain session with JWT,
- support roles,
- protect private routes.

### Facility Management

- register a facility,
- update a facility,
- view facility details,
- mark facility as verified or unverified,
- support multiple facility types.

### Search and Discovery

- search by emergency type,
- search by facility type,
- filter by verification status,
- filter by distance,
- filter by availability,
- rank results intelligently.

### Routing and Location

- accept user location,
- calculate route to facility,
- estimate distance,
- estimate travel time,
- show map-based guidance.

### Admin Operations

- review facility submissions,
- verify or reject facilities,
- maintain data quality,
- inspect admin actions.

---

## 10. System Architecture

The project must follow a layered, clean, and modular architecture.

### Recommended Layers

1. Presentation Layer  
2. API Layer  
3. Application Layer  
4. Domain Layer  
5. Infrastructure Layer  
6. Persistence Layer  
7. External Services Layer  

### Responsibilities by Layer

#### 10.1 Presentation Layer
- React UI
- forms
- pages
- maps
- dashboards
- user interaction

#### 10.2 API Layer
- REST controllers
- request/response handling
- routing HTTP calls
- input/output mapping

#### 10.3 Application Layer
- orchestration of use cases
- business workflows
- transaction coordination

#### 10.4 Domain Layer
- business entities
- core rules
- domain models
- value objects if needed

#### 10.5 Infrastructure Layer
- service implementations
- external integrations
- routing integrations
- geocoding integrations

#### 10.6 Persistence Layer
- repositories
- entities
- database access
- data mapping

#### 10.7 External Services Layer
- OpenStreetMap services
- Nominatim
- OSRM or equivalent routing service

---

## 11. DSA Design

RescueRoute must demonstrate practical and meaningful use of data structures and algorithms.

### Required Concepts

- Graph
- Weighted Graph
- Adjacency List
- HashMap
- Priority Queue
- ArrayList
- Sorting
- Dijkstra’s Algorithm

### How They Will Be Used

#### Graph
Represents medical locations and roads between them.

#### Weighted Graph
Edge weights represent:
- travel distance,
- travel time,
- optional route cost factors later.

#### Adjacency List
Represents graph connections efficiently.

#### HashMap
Used for:
- fast lookup of facilities,
- fast lookup of users,
- indexing by ID or location key.

#### Priority Queue
Used for:
- route optimization,
- shortest path processing,
- ranking candidate facilities.

#### Sorting
Used for:
- ranking nearest or most suitable facilities,
- ordering search results.

#### Dijkstra’s Algorithm
Used for:
- shortest path optimization,
- finding the best route between user and facility.

### Future DSA Expansion

Later versions may introduce:
- BFS,
- DFS,
- A*,
- dynamic route scoring,
- smarter heuristics.

---

## 12. Database Design Direction

Use a normalized PostgreSQL database.

### Core Entities

- users
- facilities
- hospitals
- blood_banks
- pharmacies
- ambulances
- routes
- verification_requests
- admin_logs

### Key Requirements

- use proper primary keys,
- use foreign keys where appropriate,
- keep data normalized,
- keep facility data extensible,
- keep verification separate from registration,
- support location coordinates.

### Data to Store

For facilities, the database should store:

- name,
- type,
- address,
- latitude,
- longitude,
- contact info,
- service details,
- emergency type support,
- verification status,
- availability,
- timestamps.

---

## 13. Frontend Architecture

The frontend must be written in React + TypeScript and designed as a modern production-grade web application.

### Requirements

- responsive layout,
- reusable components,
- clear page structure,
- route-based navigation,
- clean state management,
- API integration using Axios,
- map display area,
- facility listing cards,
- search and filter interface,
- authentication pages,
- admin dashboard pages.

### Suggested Frontend Modules

- auth
- home
- search
- map
- facility
- admin
- shared
- layout
- utils

---

## 14. Backend Architecture

The backend must be built with Spring Boot and designed for clean separation of concerns.

### Backend Layers

- controller
- service
- repository
- entity
- dto
- mapper
- config
- security
- exception
- integration

### Backend Requirements

- JWT authentication,
- role-based authorization,
- request validation,
- proper exception handling,
- service-based business logic,
- repository abstraction,
- DTO-based API communication,
- integration with OpenStreetMap services.

---

## 15. API Design Principles

The APIs must follow REST conventions.

### API Principles

- use proper HTTP verbs,
- use meaningful endpoint naming,
- return clear response structures,
- return useful error messages,
- validate all inputs,
- support role-based restrictions.

### Example API Areas

- auth
- users
- facilities
- hospitals
- blood-banks
- pharmacies
- ambulances
- routes
- admin
- verification

---

## 16. Authentication and Security

Security is required even in Version 1.

### Must Include

- JWT authentication,
- BCrypt password hashing,
- role-based authorization,
- secure API access,
- input validation,
- server-side checks,
- protected admin routes.

### Security Principles

- never expose passwords,
- never expose internal tokens,
- never trust client input,
- validate all incoming requests.

---

## 17. Repository Structure

The repository should be professional and easy to navigate.

### Recommended Structure

```text
RescueRoute/
│
├── README.md
├── LICENSE
├── CONTRIBUTING.md
├── docs/
│   ├── MASTER_SPEC.md
│   ├── ARCHITECTURE.md
│   ├── DATABASE_SCHEMA.md
│   ├── API_SPEC.md
│   ├── DEVELOPMENT_ROADMAP.md
│   ├── CODING_STANDARDS.md
│   ├── DSA_DESIGN.md
│   ├── FUTURE_SCOPE.md
│   └── adr/
├── frontend/
├── backend/
├── database/
├── screenshots/
├── demo/
└── .github/