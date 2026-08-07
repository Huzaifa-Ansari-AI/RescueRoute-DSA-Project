# Coding Standards 

**Project:** RescueRoute   
**Version:** 1.0 

---

# 1. General Principles

- Write clean, readable, maintainable code.
- Follow Clean Architecture.
- Keep business logic separate from UI.
- Avoid duplicate code (DRY).
- Keep methods small and focused.
- Prefer composition over duplication.
- No unfinished TODOs or placeholder code.

---

# 2. Naming Conventions

## Classes
- PascalCase
- Example: `UserService`

## Methods
- camelCase
- Example: `findNearestHospital()`

## Variables
- camelCase
- Example: `userLocation`

## Constants
- UPPER_CASE
- Example: `MAX_SEARCH_RADIUS`

## Packages/Folders
- lowercase
- Example: `service`, `controller`

---

# 3. Folder Structure

Frontend

```text
src/
components/
pages/
services/
hooks/
types/
utils/
```

Backend

```text
controller/
service/
repository/
entity/
dto/
config/
security/
exception/
utils/
```

---

# 4. Java Standards

- Java 21
- Use Spring Boot best practices.
- Use constructor injection.
- Use DTOs between Controller and Service.
- Never expose entities directly.
- Validate all inputs.
- Handle exceptions globally.

---

# 5. React Standards

- Functional components only.
- Use TypeScript everywhere.
- Keep components reusable.
- Separate UI from API logic.
- Use React Router.
- Keep state minimal.

---

# 6. Database Standards

- PostgreSQL
- Meaningful table names.
- Use foreign keys.
- Use indexes where needed.
- Avoid duplicate data.
- Use UUIDs for primary keys.

---

# 7. API Standards

- RESTful endpoints.
- Return JSON only.
- Proper HTTP status codes.
- Consistent response format.
- Validate all requests.

Example:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": {}
}
```

---

# 8. Security Standards

- JWT Authentication
- BCrypt password hashing
- Never expose passwords
- Validate every request
- Protect admin routes
- Store secrets in environment variables

---

# 9. Error Handling

- Use Global Exception Handler.
- Return meaningful error messages.
- Never expose stack traces.

---

# 10. Git Standards

Branch Names

```
feature/auth
feature/search
fix/login
```

Commit Format

```
feat: add authentication
fix: correct search bug
docs: update API specification
refactor: improve routing service
```

---

# 11. Documentation

Every public class should include:

- Purpose
- Inputs
- Outputs

Complex methods should include short comments explaining the logic.

---

# 12. Testing

- Test every API.
- Test validation.
- Test authentication.
- Test search.
- Test routing.
- Fix bugs before adding new features.

---

# 13. Performance

- Avoid unnecessary database queries.
- Use pagination.
- Reuse components.
- Keep APIs lightweight.
- Optimize search operations.

---

# 14. AI Development Rules

When generating code:

- Produce complete, runnable code.
- Include imports and package declarations.
- Do not leave placeholders.
- Follow the project architecture.
- Maintain consistency with all files in `docs/`.
- Do not modify existing functionality unless requested.
- Explain any major design decisions.

---

# 15. Definition of Done

A feature is complete only if:

- Code compiles successfully.
- No linting errors.
- API works correctly.
- Frontend integrates successfully.
- Validation is implemented.
- Error handling is included.
- Documentation is updated.
- Code follows these standards.
