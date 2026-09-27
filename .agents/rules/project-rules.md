---
trigger: always_on
---

# RescueRoute — Workspace Agent Rules

## 1. Purpose

You are working on **RescueRoute**, a DSA-focused emergency healthcare facility discovery and routing application.

Your job is to implement requested features while preserving the existing architecture, specifications, decisions, and working functionality.

Treat this file as the project's operating rules.

---

## 2. Source of Truth

Do not recreate project specifications from memory.

Before making changes, consult the relevant project documentation.

### Core project context

Read when broader project context is required:

- `README.md`
- `docs/MASTER_SPEC.md`
- `docs/ARCHITECTURE.md`

### Architecture decisions

Before changing an architectural decision, consult:

- `docs/adr/ADR-01-frontend-framework.md`
- `docs/adr/ADR-02-backend-framework.md`
- `docs/adr/ADR-03-database-choice.md`
- `docs/adr/ADR-04-mapping-choice.md`
- `docs/adr/ADR-05-routing-strategy.md`

### Database

Before modifying database structure, entities, relationships, or persistence logic, consult:

- `docs/DATABASE_SCHEMA.md`

### Other project documentation

If a relevant specification/document exists, read it before modifying the corresponding area.

Documentation is the source of truth. Do not silently replace documented decisions with personal assumptions.

---

## 3. Spec-Driven Development

Follow this workflow for every non-trivial task:

1. Understand the requested feature.
2. Inspect the existing implementation.
3. Read the relevant specification/documentation.
4. Check existing architecture and ADR decisions.
5. Create or update the required implementation.
6. Run the appropriate validation/build/tests.
7. Fix errors caused by the implementation.
8. Verify that existing functionality still works.
9. Summarize what changed and how it was verified.

Do not implement first and investigate architecture afterward.

---

## 4. Change Policy

### Prefer

- Small, focused changes
- Reusable components
- Existing project patterns
- Simple maintainable solutions
- Clear naming
- Type-safe code
- Existing dependencies
- Existing folder structure
- Incremental implementation

### Avoid

- Unnecessary refactoring
- Unrequested features
- Duplicate utilities/components
- New frameworks without approval
- New dependencies when existing ones are sufficient
- Changing working code without a reason
- Replacing architectural decisions
- Creating speculative abstractions
- Over-engineering MVP features

Follow the **Pareto Principle**:
choose the simplest professional implementation that provides the required functionality and strong long-term maintainability.

---

## 5. Existing Functionality Is Protected

Before modifying an existing feature:

- Inspect how it currently works.
- Preserve its public behavior unless the task explicitly requires a change.
- Do not break existing routes, APIs, components, map behavior, or database integration.
- Reuse existing functionality instead of duplicating it.

After changes, verify affected functionality.

---

## 6. Frontend Rules

The frontend uses the existing React + TypeScript + Vite architecture.

Respect the existing structure:

- `pages/` → page-level UI
- `components/` → reusable UI
- `services/` → API communication
- `types/` → shared TypeScript types
- `utils/` → reusable utilities
- `routes/` → routing
- `layout/` → application layout

Use existing components and utilities whenever possible.

Do not place API calls, business logic, and large UI logic unnecessarily inside page components.

Keep components focused and reusable.

---

## 7. Map Rules

RescueRoute currently uses:

- Leaflet
- React Leaflet
- OpenStreetMap

The map must remain reusable and maintainable.

Existing map functionality must continue to work:

- Karachi-centered map
- Zoom
- Drag/pan
- Facility markers
- Marker popups
- Facility search
- Facility-type filtering

Do not replace the mapping stack without an explicit architectural decision.

Routing implementation must follow the relevant ADR.

---

## 8. Backend Rules

The backend uses the existing:

- Java
- Spring Boot
- REST API architecture

Respect the existing package structure and common utilities.

Use the existing global response and exception-handling patterns.

Do not introduce a second response format or duplicate exception-handling mechanism.

Follow existing validation, controller, service, and persistence conventions.

---

## 9. Database Rules

The project uses **Neon PostgreSQL**.

Database changes must:

1. Be consistent with the documented schema.
2. Preserve existing relationships.
3. Avoid unnecessary schema duplication.
4. Use proper PostgreSQL types and constraints.
5. Keep database logic separate from presentation logic.

Before changing the database model, consult:

`docs/DATABASE_SCHEMA.md`

Do not invent tables, columns, relationships, or constraints when the required information is already documented.

---

## 10. API Rules

Use REST APIs between frontend and backend.

Keep API communication centralized through the existing frontend API/service layer.

Maintain consistent API response structures.

Do not create multiple inconsistent response formats for similar endpoints.

When adding an endpoint, consider:

- HTTP method
- URL
- request structure
- response structure
- validation
- error handling
- HTTP status
- frontend integration

---

## 11. DSA Requirement

RescueRoute is a **DSA-focused project**.

When implementing algorithmic features, prefer appropriate data structures and algorithms rather than hiding all logic behind external services.

Where applicable, clearly separate:

- Data structures
- Algorithms
- Business logic
- API layer
- UI layer

Do not add artificial DSA complexity where a simple solution is more appropriate.

When a feature specifically demonstrates DSA, preserve clear and understandable algorithmic implementation.

---

## 12. Dependencies

Before installing a new dependency:

1. Check whether the project already has an equivalent capability.
2. Check the existing `package.json` or `pom.xml`.
3. Prefer the existing technology stack.
4. Add a dependency only when it provides clear value.

Do not install libraries merely for convenience.

Do not upgrade major dependencies unless explicitly required.

---

## 13. File and Folder Rules

Follow the existing project structure.

Before creating a new file, check whether an appropriate existing file/component/service can be reused.

Use descriptive names.

Do not create temporary files, duplicate implementations, generated artifacts, or unnecessary configuration files.

Keep frontend and backend responsibilities separated.

---

## 14. Configuration and Secrets

Never hardcode:

- API keys
- database passwords
- access tokens
- private credentials
- production secrets

Use the project's existing environment/configuration mechanism.

Never commit secrets to Git.

If required configuration is missing, identify it clearly instead of inventing credentials or values.

---

## 15. Error Handling

Do not hide errors.

When a command, build, test, or application fails:

1. Read the actual error.
2. Identify the root cause.
3. Make the smallest appropriate fix.
4. Re-run the failed validation.
5. Report the result.

Do not declare a task complete while known build/runtime errors remain.

Do not suppress errors merely to make the build pass.

---

## 16. Verification

Every implementation task should be verified at the appropriate level.

### Frontend

At minimum, check:

- TypeScript/build errors
- Runtime errors
- Browser behavior
- Affected UI functionality

### Backend

Check:

- Compilation
- Tests where available
- Application startup
- Affected API endpoint

### Database

Check:

- Connection
- Schema compatibility
- Queries/migrations
- Affected application functionality

Use the project's existing commands and configuration.

---

## 17. Git Safety

Do not perform destructive Git operations unless explicitly requested.

Do not:

- Delete unrelated work
- Reset the repository
- Force-push
- Rewrite history
- Remove user changes

Before large changes, inspect the working tree.

Keep changes focused on the requested task.

---

## 18. Agent Autonomy

You may autonomously:

- Inspect project files
- Analyze existing code
- Create required files
- Modify implementation files
- Install clearly required dependencies
- Run builds/tests
- Fix implementation errors
- Perform routine refactoring required by the requested feature

You must stop and ask for confirmation when a change would:

- Alter a documented architectural decision
- Replace a major technology
- Require a new external service with significant implications
- Change the database architecture substantially
- Remove important existing functionality
- Introduce a security-sensitive design decision
- Conflict with an ADR or project specification

---

## 19. Documentation Discipline

Do not duplicate large amounts of information from project documentation into source files or this rule.

Update documentation only when the implementation changes a documented project decision, specification, API contract, schema, or important architectural behavior.

Keep documentation concise and consistent.

If documentation conflicts with implementation, identify the conflict instead of silently choosing one.

---

## 20. Ambiguity Rule

If a task is ambiguous but a safe interpretation is obvious from the existing specifications, follow the documented specification.

If multiple interpretations would materially change architecture, data model, API behavior, or user experience, ask before implementing.

Never invent important requirements.

---

## 21. Completion Criteria

A task is complete only when:

- The requested functionality is implemented.
- Existing architecture is respected.
- Relevant documentation/ADRs are respected.
- No unnecessary dependencies or complexity were introduced.
- The affected code builds successfully.
- Relevant tests/checks pass.
- Existing functionality remains intact.
- No known implementation errors remain.

At completion, provide a concise summary:

### Implemented
- What changed

### Verified
- What was tested/run

### Notes
- Any remaining limitation or required manual step

---

## 22. Priority Order

When making decisions, follow this priority:

1. Explicit user request
2. Project specifications
3. Architecture documentation
4. ADR decisions
5. Existing working implementation
6. Simplicity and maintainability
7. General engineering conventions

Do not override a higher-priority rule with a lower-priority preference.

---

## 23. Core Principle

**Understand → Inspect → Consult Docs → Plan → Implement → Verify → Report**

Build only what is required.

Preserve what already works.

Prefer simple, maintainable solutions.

Do not hallucinate requirements.

Do not make architectural decisions silently.