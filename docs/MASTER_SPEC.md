# RescueRoute Master Spec

## Project Summary
RescueRoute is a full-stack web application that helps users quickly find nearby medical resources during emergencies.

It supports:
- Hospitals
- Blood Banks
- Pharmacies
- Ambulance Services

The app ranks results using:
- distance
- travel time
- facility type
- verification status
- availability
- suitability

---

## Goal
Build a fast, practical, portfolio-ready emergency medical resource navigator.

Agentic AI will be added later after the core app is complete.

---

## Tech Stack
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

## Current Progress
### Completed
- GitHub repository created
- Project documentation added
- Backend foundation set up
- Frontend foundation set up
- Common backend response/error handling added
- OpenStreetMap map integration added
- Facility markers added
- Search and category filtering added

### In Progress
- Backend facility APIs
- Map-marker details from backend
- Authentication
- Provider/admin workflows
- Deployment

---

## Core Features
- Interactive OpenStreetMap centered on Karachi
- Facility markers for hospitals, blood banks, pharmacies, ambulances
- Click marker to view facility details
- Search by facility name
- Filter by facility type
- Responsive UI
- Shared API response format
- Centralized exception handling

---

## Project Structure
```text
RescueRoute/
├── frontend/
├── backend/
├── database/
├── docs/
├── screenshots/
├── demo/
└── .github/