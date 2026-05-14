# Shared Packages

Target v4 location for shared contracts, generated API DTOs and cross-platform
utility libraries.

Initial contract policy:

- Java DTOs remain authoritative in `../../Apex-Gestordemo/src/main/java/DTO`.
- Angular models remain in `../../Apex_Gestor/src/app/core/models.ts`.
- Generated OpenAPI/DTO artifacts should be committed here only after the API
  schema generator is introduced.
