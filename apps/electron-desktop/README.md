# Apex Gestor Electron Desktop

Branch: `codex/ui/electron-desktop-admcalc`

Camada front-end Electron Desktop criada a partir da branch `codex/legacy/pre-v3-apex-gestor-2`.

## Entregas

- App Electron em `apps/electron-desktop`.
- Hardening básico: `contextIsolation: true`, `sandbox: true`, `nodeIntegration: false`.
- Layout desktop clean com menu lateral retrátil.
- Alternância entre Light Mode e Dark Mode.
- PDV funcional com carrinho local.
- AdmCalc financeiro com cálculo local e integração REST em `${APEX_API_URL}/api/admcalc/calcular`.

## Executar

```bash
cd apps/electron-desktop
npm install
npm run start
```

Backend legado com endpoint AdmCalc:

```bash
cd Apex-Gestordemo
mvnw.cmd spring-boot:run
```

Opcionalmente aponte o desktop para outra API:

```bash
set APEX_API_URL=http://localhost:8080
npm run start
```
