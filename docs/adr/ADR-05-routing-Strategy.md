# ADR-05: Routing Strategy

## Decision
Use an **OpenStreetMap-based routing service** for RescueRoute route calculation.

## Initial Strategy
Use **OSRM (Open Source Routing Machine)** for MVP route calculation.

## Why
- OpenStreetMap compatible
- Open-source
- Suitable for road-based route calculation
- Simple REST API integration
- Avoids proprietary routing SDK dependency during MVP development

## Current Status
Planned.

## Future Scope
Routing will support:
- Current location → selected facility
- Road-based route calculation
- Distance and estimated travel time
- Emergency route visualization

## Note
The routing implementation can be replaced later if project requirements require a different routing engine.