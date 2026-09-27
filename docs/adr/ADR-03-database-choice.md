# ADR-03: Database Choice

## Decision
Use **Neon PostgreSQL** as the primary database for RescueRoute.

## Why
- PostgreSQL provides reliable relational data storage.
- Neon provides managed PostgreSQL with cloud hosting.
- Easy integration with Spring Boot.
- Suitable for structured healthcare facility data.
- Supports future scalability and geospatial requirements.

## Current Status
Selected and configured.

## Scope
Neon PostgreSQL will store:
- Hospitals
- Blood banks
- Pharmacies
- Ambulances
- Users
- Facility locations
- Related application data