# Mapa do Repositorio Apex Gestor

Este mapa existe para deixar o GitHub legivel mesmo com varias plataformas no mesmo produto.

## Pastas principais

| Caminho | Conteudo | Status |
| --- | --- | --- |
| `Apex-Gestordemo/` | Backend Spring Boot, API REST, seguranca, Flyway, Envers, testes JUnit. | Caminho ativo do CI. |
| `Apex_Gestor/` | Angular/Ionic, Electron, Capacitor Android, builds web/mobile/desktop. | Caminho ativo do CI. |
| `apps/` | Organizacao alvo por produto: backend, web, desktop, mobile cliente e mobile gestao. | Guia de transicao. |
| `packages/shared/` | Contratos de API, DTOs e utilitarios compartilhados planejados. | Guia de transicao. |
| `docs/` | Documentacao tecnica, runbooks, arquitetura, banco, releases e seguranca. | Ativo. |
| `docs/database/` | Dump historico, modelo do banco e scripts SQL de referencia. | Ativo. |
| `scripts/` | Scripts de auditoria, bootstrap, validacao e utilitarios locais. | Ativo. |
| `.github/workflows/` | GitHub Actions para testes, builds e releases. | Ativo. |

## Regras de organizacao

1. Arquivos soltos na raiz devem ser evitados.
2. Documentos ficam em `docs/`, agrupados por tema quando necessario.
3. SQL de referencia fica em `docs/database/`; migrations reais ficam no backend em `Apex-Gestordemo/src/main/resources/db/migration`.
4. Scripts executaveis ficam em `scripts/`.
5. Caminhos usados pela CI (`Apex-Gestordemo` e `Apex_Gestor`) so devem ser renomeados quando o workflow, Docker, Capacitor e Electron forem migrados juntos.

## Como abrir cada parte

```bash
# Backend
cd Apex-Gestordemo
./mvnw test

# Frontend web/mobile/desktop
cd Apex_Gestor
npm run start:web

# Ambiente completo
docker compose up -d --build
```

## Materiais movidos da raiz

| Antes | Agora |
| --- | --- |
| `Apex_BD.sql` | `docs/database/apex-bd-base.sql` |
| `Apex_BD.pdf` | `docs/database/apex-bd-modelo.pdf` |
| `docs/der-apex-gestor.mmd` | `docs/database/der-apex-gestor.mmd` |
| `docs/upgrade-admcalc-financeiro.sql` | `docs/database/migrations-reference/upgrade-admcalc-financeiro.sql` |
| `docs/upgrade-license-entitlements.sql` | `docs/database/migrations-reference/upgrade-license-entitlements.sql` |
| `webserver01.sh` | `scripts/legacy-webserver01.sh` |
