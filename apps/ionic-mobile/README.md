# Apex Gestor Ionic Mobile

Branch: `codex/ui/ionic-mobile-admcalc`

Camada front-end Ionic Mobile criada a partir da branch `codex/legacy/pre-v3-apex-gestor-2`.

## Entregas

- App Ionic Angular em `apps/ionic-mobile`.
- Layout mobile com navegação lateral retrátil e fallback responsivo.
- Alternância entre Light Mode e Dark Mode.
- Cards operacionais para equipe em movimento.
- AdmCalc financeiro com cálculo local e integração REST em `/api/admcalc/calcular`.

## Executar

```bash
cd apps/ionic-mobile
npm install
npm run start
```

Backend legado com endpoint AdmCalc:

```bash
cd Apex-Gestordemo
mvnw.cmd spring-boot:run
```
