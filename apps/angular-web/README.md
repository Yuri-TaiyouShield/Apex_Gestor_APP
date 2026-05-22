# Apex Gestor Angular Web

Branch: `codex/ui/angular-web-admcalc`

Camada front-end Angular Web criada a partir da branch `codex/legacy/pre-v3-apex-gestor-2`.

## Entregas

- App Angular standalone em `apps/angular-web`.
- Layout clean com menu lateral retrátil.
- Alternância nativa entre Light Mode e Dark Mode.
- Dashboard web, estoque inteligente e módulo AdmCalc financeiro.
- AdmCalc tenta usar `/api/admcalc/calcular` e usa cálculo local como fallback de demonstração.

## Executar

```bash
cd apps/angular-web
npm install
npm run start
```

Backend legado com endpoint AdmCalc:

```bash
cd Apex-Gestordemo
mvnw.cmd spring-boot:run
```
