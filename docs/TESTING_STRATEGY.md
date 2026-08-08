# RescueRoute Testing Strategy

## Purpose
This document defines how RescueRoute should be tested so features work correctly, remain stable, and can be safely improved over time.

---

## Testing Goals
- Catch issues early
- Verify each feature before moving forward
- Keep the app stable while building quickly
- Make sure frontend and backend work together correctly
- Prevent broken changes from reaching the main branch

---

## Testing Levels

### 1. Manual Testing
Used for:
- map behavior
- search and filter behavior
- marker clicks
- popup details
- form interactions
- responsive layout checks

### 2. Backend Testing
Used for:
- API responses
- validation
- error handling
- authentication
- facility CRUD
- route calculation

### 3. Frontend Testing
Used for:
- page rendering
- component behavior
- search input
- category filters
- loading states
- empty states
- mobile layout

### 4. Integration Testing
Used for:
- frontend calling backend
- backend returning facility data
- marker updates from API data
- auth and protected endpoints later

---

## What Must Be Tested

### Map Module
- OpenStreetMap loads correctly
- Karachi center is correct
- Zoom and drag work
- Markers appear in the right locations
- Each marker opens the correct popup
- Marker icons match the facility type

### Search Module
- Search by facility name
- Search results update immediately
- Category filter works
- Combined search and filter works
- Empty results are handled clearly

### Backend API
- Health endpoint returns success
- Response format is consistent
- Validation errors return clean messages
- Not found errors return the correct status
- Unexpected errors are handled globally

### Facility Data
- Facility names display correctly
- Facility types are correct
- Latitude and longitude values are valid
- Address and details are shown correctly

### UI/UX
- Home page renders without errors
- Search bar is usable
- Filter dropdown works
- Cards and map remain readable
- Layout is usable on smaller screens

---

## Test Priorities
Test in this order:

1. Critical user flow
2. Map rendering
3. Search and filtering
4. Popup details
5. API response correctness
6. Error handling
7. Responsive design

---

## Frontend Testing Approach
For now, use practical testing:
- open the app in the browser
- verify visible behavior
- check console for errors
- test with sample data
- confirm UI updates after search/filter changes

Later, automated frontend tests can be added.

---

## Backend Testing Approach
Test backend endpoints with:
- browser
- Postman
- curl
- API client in the frontend

Verify:
- request body
- response body
- HTTP status code
- validation output
- exception handling

---

## Smoke Test Checklist
Before every major change, confirm:
- frontend starts successfully
- backend starts successfully
- map loads
- markers appear
- popup opens
- search works
- filter works
- backend health endpoint responds

---

## Bug Handling
When a bug is found:
- reproduce it
- isolate the file or endpoint
- fix the smallest possible part
- re-test immediately
- do not move to the next feature until the bug is resolved

---

## Definition of Done
A feature is complete only when:
- it works in the browser
- it works with the backend
- it has no obvious UI issues
- errors are handled properly
- it does not break existing features

---

## Final Rule
Testing should happen continuously.
Do not wait until the end of the project to test everything.