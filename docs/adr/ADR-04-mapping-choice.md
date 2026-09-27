# ADR-04: Mapping Library

## Decision
Use **Leaflet + React Leaflet with OpenStreetMap** for RescueRoute maps.

## Why
- Open-source
- Lightweight
- React integration
- No proprietary map SDK required for the MVP
- Supports markers, popups, zoom, and map interactions

## Current Status
Implemented.

## Current Features
- OpenStreetMap
- Karachi-centered map
- Zoom and drag
- Facility markers
- Marker popups
- Facility search and type filtering

## Scope
All frontend map visualization and facility location display will use this solution.