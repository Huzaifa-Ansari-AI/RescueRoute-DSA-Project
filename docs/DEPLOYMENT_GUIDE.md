# Deployment Guide

## Purpose
This document defines how RescueRoute will be deployed from local development to a publicly accessible production application.

The deployment process should be simple, repeatable, and easy to maintain.

---

# Deployment Architecture

```text
React (Vercel)
        │
        │ HTTPS
        ▼
Spring Boot API (Render)
        │
        ▼
PostgreSQL (Neon)
```

---

# Hosting Services

## Frontend
Platform:
- Vercel

Responsibilities:
- Build React application
- Serve static assets
- Connect to backend API

---

## Backend

Platform:
- Render

Responsibilities:
- Run Spring Boot application
- Expose REST APIs
- Connect to PostgreSQL

---

## Database

Platform:
- Neon PostgreSQL

Responsibilities:
- Store application data
- Provide managed backups
- Scale independently

---

# Environment Variables

## Frontend

Required variables:

```text
VITE_API_BASE_URL
```

Example:

```text
VITE_API_BASE_URL=https://api.rescueroute.com/api/v1
```

---

## Backend

Required variables:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
SERVER_PORT
SPRING_PROFILES_ACTIVE
```

Never commit these values to Git.

---

# Build Process

## Frontend

```bash
npm install
npm run build
```

Output:

```text
dist/
```

---

## Backend

```bash
mvn clean package
```

Output:

```text
target/*.jar
```

---

# Deployment Flow

## Step 1

Push code to GitHub.

↓

## Step 2

Vercel automatically builds the frontend.

↓

## Step 3

Render automatically builds the backend.

↓

## Step 4

Backend connects to Neon PostgreSQL.

↓

## Step 5

Verify application health.

↓

## Step 6

Release to production.

---

# Pre-Deployment Checklist

Before every deployment:

- Project builds successfully
- No compilation errors
- No TypeScript errors
- No failing API endpoints
- Database connection verified
- Environment variables configured
- Responsive UI checked
- Console free of major errors

---

# Post-Deployment Checklist

After deployment verify:

- Frontend loads
- Backend responds
- Database connection works
- Map loads correctly
- Facility markers appear
- Search works
- Filters work
- APIs return expected responses

---

# Versioning

Use Semantic Versioning.

Examples:

```text
v1.0.0
v1.1.0
v1.2.0
```

- Major → breaking changes
- Minor → new features
- Patch → bug fixes

---

# Release Workflow

1. Develop feature
2. Test locally
3. Commit changes
4. Push to GitHub
5. Automatic deployment
6. Verify production
7. Create release tag

---

# Rollback Strategy

If a deployment fails:

- Roll back to the previous stable release.
- Restore the last working environment variables if needed.
- Verify database integrity.
- Fix the issue locally before redeploying.

---

# Monitoring

After deployment monitor:

- Backend availability
- API response time
- Database connectivity
- Application logs
- Build status
- Runtime errors

---

# Future Improvements

Future versions may include:

- Custom domain
- HTTPS certificate management
- Docker deployment
- CI/CD with GitHub Actions
- Automated testing before deployment
- Performance monitoring
- Error tracking
- Database migrations

---

# Definition of Successful Deployment

A deployment is successful when:

- Frontend is publicly accessible.
- Backend APIs are reachable.
- Database operations succeed.
- Core user workflows function correctly.
- No critical errors appear in logs.

---

# Final Rule

Deploy only stable, tested code.

Every deployment should leave the application in a fully working state.