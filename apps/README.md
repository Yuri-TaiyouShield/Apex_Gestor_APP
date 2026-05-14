# Apex Gestor v4 Monorepo Topology

This folder is the enterprise target topology for the Apex Gestor monorepo.

The current production code remains in the legacy-compatible folders while the
CI/CD and runbooks migrate consumers safely:

- `apps/backend-api` -> `Apex-Gestordemo`
- `apps/desktop-pdv` -> `Apex_Gestor/electron`
- `apps/web-cliente` -> `Apex_Gestor`
- `apps/mobile-cliente` -> `Apex_Gestor` with the `mobile-client` Angular configuration
- `apps/mobile-gestao` -> `Apex_Gestor` with the `mobile-staff` Angular configuration
- `packages/shared` -> shared API contracts and generated DTO snapshots

The physical move should happen module by module after v4 CI keeps all builds
green, because the current Angular, Capacitor and Electron paths are referenced
by native build tooling.
