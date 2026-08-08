# RescueRoute Development Roadmap

## Purpose
This roadmap defines the order in which RescueRoute will be built so the project can be completed quickly, tested often, and improved step by step.

The goal is to keep progress visible at every stage and avoid unnecessary complexity.

---

## Working Strategy
- Build one small feature at a time
- Keep each step working before moving forward
- Prefer simple solutions first
- Reuse existing code where possible
- Do not redesign completed parts unless necessary

---

## Milestone 1 — Frontend MVP
### Goal
Create a working emergency map experience in the browser.

### Deliverables
- Home page
- OpenStreetMap integration
- Karachi-centered map
- Sample facility markers
- Marker popups
- Search by name
- Filter by facility type
- Live marker updates

### Done When
- The map loads
- Markers appear
- Search and filtering work
- Clicking a marker shows details

---

## Milestone 2 — Backend Base APIs
### Goal
Replace hardcoded frontend data with real backend data.

### Deliverables
- Facility model
- Facility API
- Health endpoint
- Standard API response
- Validation handling
- Global exception handling

### Done When
- Frontend can fetch facilities from the backend
- Backend returns consistent JSON responses
- Basic errors are handled cleanly

---

## Milestone 3 — Facility Management
### Goal
Allow CRUD operations for medical facilities.

### Deliverables
- Hospital management
- Blood bank management
- Pharmacy management
- Ambulance management
- Create, update, delete, list, detail APIs

### Done When
- Facilities can be managed through APIs
- Data is stored in PostgreSQL
- Frontend displays backend data correctly

---

## Milestone 4 — Authentication and Roles
### Goal
Secure the application.

### Deliverables
- User registration
- Login
- JWT authentication
- Role-based access
- Provider access
- Admin access

### Done When
- Users can sign in and sign out
- Protected routes work
- Providers and admins have separate permissions

---

## Milestone 5 — Search and Ranking
### Goal
Make RescueRoute useful for emergency discovery.

### Deliverables
- Search by facility name
- Search by category
- Distance-based sorting
- Simple ranking logic
- Verification-based priority

### Done When
- Users can find the right facility quickly
- Results are sorted in a meaningful order

---

## Milestone 6 — Routing and Map Enhancements
### Goal
Improve the map experience with route information.

### Deliverables
- Route display
- Distance estimate
- Travel time estimate
- Selected facility highlighting
- Better marker interaction

### Done When
- Users can visually understand where to go
- Selected facilities are clearly shown on the map

---

## Milestone 7 — Admin and Provider Workflows
### Goal
Add operational control over the data.

### Deliverables
- Facility verification
- Provider-managed facility records
- Admin approval and rejection
- Admin log tracking

### Done When
- Facility data can be trusted and moderated
- Admin actions are traceable

---

## Milestone 8 — Polish and Deployment
### Goal
Prepare the project for public presentation.

### Deliverables
- Responsive UI cleanup
- Loading and error states
- README updates
- Screenshots
- Deployment to Vercel and Render
- Production database connection

### Done When
- The app runs online
- The UI looks polished
- The repository is ready to show in a portfolio

---

## Priority Order
If time is limited, build in this order:

1. Map UI
2. Search and filter
3. Backend facility APIs
4. Database integration
5. Authentication
6. Ranking and route logic
7. Admin tools
8. Deployment

---

## Definition of Done for Each Step
A step is complete only when:
- the code runs successfully,
- the feature is visible or testable,
- errors are handled,
- the UI or API is usable,
- and the next step can build on it safely.

---

## Current Focus
The current focus is the MVP:
- map
- markers
- search
- filtering
- details popup
- backend connection

Everything else comes after the core experience is stable.