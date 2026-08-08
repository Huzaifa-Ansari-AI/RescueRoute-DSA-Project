# RescueRoute Security Guidelines

## Purpose
These guidelines define how RescueRoute should protect user data, API access, and administrative actions.

---

## Security Goals
- Protect user accounts
- Prevent unauthorized data changes
- Keep sensitive information out of the frontend
- Secure all backend endpoints
- Use safe defaults everywhere

---

## Authentication
- Use JWT for authenticated requests.
- Hash passwords before storing them.
- Never store plain text passwords.
- Keep token lifetime short enough for safety.
- Require authentication for protected actions.

---

## Authorization
- Use role-based access control.
- Supported roles:
  - USER
  - PROVIDER
  - ADMIN
- Restrict write operations to authorized roles only.
- Admin-only actions must never be exposed to normal users.

---

## Input Validation
- Validate all API input on the backend.
- Never trust frontend validation alone.
- Check required fields, formats, and value ranges.
- Reject invalid coordinates, emails, and empty strings.
- Return clean validation messages.

---

## Sensitive Data
- Do not commit secrets to GitHub.
- Keep passwords, tokens, and keys in environment variables.
- Never expose database credentials in frontend code.
- Do not return internal secrets in API responses.

---

## API Protection
- Protect all write endpoints.
- Keep public endpoints read-only where possible.
- Use proper HTTP status codes.
- Return generic error messages for server failures.
- Do not expose stack traces to users.

---

## Database Safety
- Use parameterized queries through JPA/Hibernate.
- Avoid raw SQL unless necessary.
- Keep database access inside repository classes.
- Use least-privilege database credentials.

---

## Frontend Safety
- Store only the minimum required auth data in the browser.
- Do not place secret logic in React code.
- Keep all sensitive checks on the backend.
- Sanitize user-visible text when needed.

---

## Admin Actions
- Log important admin actions.
- Require admin verification for sensitive changes.
- Protect approval and deletion endpoints.
- Keep audit records for moderation actions.

---

## Error Handling
- Show user-friendly messages.
- Hide internal exception details from users.
- Log technical details on the server only.
- Return consistent error response shapes.

---

## Environment Variables
Keep these outside the codebase:
- JWT secret
- database password
- backend URL
- frontend API URL
- deployment credentials

Use `.env.example` for reference only.

---

## Future Security Improvements
Later versions may add:
- refresh tokens
- account lockout
- multi-factor authentication
- rate limiting
- audit dashboards
- request throttling

---

## Final Rule
If a feature needs stronger security, add it on the backend first.
Do not depend on the frontend for protection.