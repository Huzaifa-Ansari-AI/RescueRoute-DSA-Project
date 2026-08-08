# RescueRoute Coding Standards

## Purpose
These standards define how RescueRoute should be coded so the project stays clean, consistent, easy to maintain, and fast to finish.

---

## General Rules
- Keep code simple and readable.
- Prefer the smallest working solution first.
- Reuse existing code instead of duplicating logic.
- Keep files focused on one responsibility.
- Do not add unnecessary abstraction.
- Do not leave TODOs in production code.

---

## Frontend Standards

### React + TypeScript
- Use functional components.
- Use TypeScript for all new frontend code.
- Keep components small and reusable.
- Put page-level logic in `pages/`.
- Put reusable UI in `components/`.
- Put API calls in `services/`.
- Put helpers in `utils/`.

### Component Rules
- Use clear prop names.
- Prefer composition over deep prop drilling.
- Keep marker, popup, card, and filter components separate.
- Make layout components responsible only for layout.
- Keep UI state local unless shared state is needed.

### Styling Rules
- Use a consistent spacing system.
- Keep mobile responsiveness in mind.
- Avoid inline styles when a reusable style file/component is better.
- Keep colors and typography consistent across screens.

---

## Backend Standards

### Spring Boot
- Use layered structure:
  - controller
  - service
  - repository
  - entity
  - dto
  - exception
- Keep controllers thin.
- Put business logic in services.
- Use DTOs for request and response payloads.
- Never expose entities directly through APIs.
- Keep exceptions centralized.

### Code Style
- Use constructor-based injection.
- Keep methods short and clear.
- Name classes by responsibility.
- Keep package names organized and predictable.
- Use consistent response wrapping for all endpoints.

### Validation and Errors
- Validate all input at the API boundary.
- Return clean validation messages.
- Use one global exception handler.
- Return meaningful HTTP status codes.

---

## Database Standards
- Use PostgreSQL-friendly schema design.
- Keep shared fields in shared tables.
- Keep type-specific fields in separate tables.
- Use UUIDs where useful.
- Add indexes for fields used in search and filtering.
- Keep column names clear and consistent.
- Avoid storing repeated data in multiple tables.

---

## API Standards
- Use REST-style endpoints.
- Use nouns, not verbs, in endpoint names.
- Keep response format consistent.
- Return JSON only.
- Keep request and response contracts stable.
- Use pagination for list endpoints when needed.
- Keep API names predictable for the frontend.

---

## Naming Conventions

### Frontend
- Components: `PascalCase`
- Variables/functions: `camelCase`
- Files: match component or feature name
- Examples:
  - `MapView.tsx`
  - `SearchBar.tsx`
  - `facilityService.ts`

### Backend
- Classes: `PascalCase`
- Methods/fields: `camelCase`
- Constants: `UPPER_CASE`
- Packages: lowercase
- Examples:
  - `FacilityController`
  - `FacilityService`
  - `ResourceNotFoundException`

---

## Folder Rules
- Keep feature files grouped together.
- Do not create random folders without purpose.
- Keep shared utilities in shared folders.
- Keep docs short and focused.
- Keep tests near the code they validate when practical.

---

## Git and Commit Rules
- Use small commits.
- Commit after each working step.
- Use clear commit messages.
- Examples:
  - `feat: add map markers`
  - `fix: correct facility filter`
  - `refactor: simplify api client`
  - `docs: update roadmap`

---

## Testing Rules
- Test each feature before moving to the next.
- Verify UI changes in the browser.
- Verify backend endpoints with Postman or browser.
- Fix build errors immediately.
- Do not continue building on top of broken code.

---

## Security Rules
- Never commit secrets.
- Keep environment values in `.env` files.
- Do not expose tokens in responses.
- Validate all backend input.
- Keep protected routes secure.

---

## Performance Rules
- Do not over-render React components.
- Avoid unnecessary API calls.
- Keep map updates efficient.
- Filter data before rendering when possible.
- Prefer simple data structures first.

---

## Agent / AI Coding Rules
When using VS Code Agent or any AI tool:
- Ask for one feature at a time.
- Tell the agent to modify only required files.
- Do not allow the agent to rewrite the whole project.
- Keep generated code aligned with existing folder structure.
- Review and run code after each step.

---

## Definition of Good Code
Code is good when it is:
- clear
- working
- consistent
- easy to change
- easy to test
- easy to explain

---

## Final Rule
If a simple solution solves the problem, use it first.
Only add complexity when the project truly needs it.