# Arquitetura Enterprise Apex Gestor v4.0

## Decisao de monorepo

O repositorio passa a ser governado como monorepo `Apex_Gestor`. Para nao quebrar
Capacitor, Electron Builder, Maven e GitHub Releases, a v4 introduz a topologia
alvo em `apps/` e `packages/` sem mover fisicamente os projetos ainda:

| Topologia v4 | Fonte atual | Responsabilidade |
| --- | --- | --- |
| `apps/backend-api` | `Apex-Gestordemo` | Spring Boot API, dominio, seguranca, Flyway e Envers |
| `apps/desktop-pdv` | `Apex_Gestor/electron` | Shell Electron seguro para PDV |
| `apps/web-cliente` | `Apex_Gestor` | Angular/Ionic B2C |
| `apps/mobile-cliente` | `Apex_Gestor` | Build Ionic Android cliente |
| `apps/mobile-gestao` | `Apex_Gestor` | Build Ionic Android equipe |
| `packages/shared` | DTOs Java e modelos Angular atuais | Contratos compartilhados |

## Clean Architecture incremental

A regra de transicao da v4 e simples: primeiro estabilizar portas tecnicas e
qualidade automatizada; depois mover modulos por contexto de dominio.

Contextos principais:

- `sales`: venda, PDV, checkout, pagamentos e baixa de estoque.
- `catalog`: produtos, categorias, fornecedores e precificacao.
- `finance-compliance`: AdmCalc, documentos digitais, auditoria e mensageria.
- `identity-access`: JWT, RBAC, licencas e LGPD.
- `operations`: NF, entregas e rastreio.

## SecDevOps

Gates obrigatorios:

- `scripts/audit-no-secrets.cjs`: bloqueia segredos de alta confianca.
- `scripts/audit-spring-data-jpa.cjs`: bloqueia `@Query`, `EntityManager`,
  `JdbcTemplate` e query manual em Java.
- GitHub Actions com actions fixadas por SHA completo.
- Builds web/mobile/desktop so publicam release apos backend, E2E, compose e
  testes frontend passarem.

## Dados e auditoria

- Flyway controla schema em `Apex-Gestordemo/src/main/resources/db/migration`.
- `V1__apex_schema_baseline.sql` inicializa bases novas.
- `V4__enterprise_compliance_audit_outbox.sql` adiciona Envers, outbox e perfis
  enterprise.
- Envers grava tabelas `_AUD`; `audit_revision` adiciona usuario, IP, user-agent
  e correlation-id.
- Documentos financeiros usam state machine `DRAFT -> PENDING_SIGNATURE -> SIGNED -> SENT`.

## Observabilidade

O backend esta preparado para log estruturado JSON via:

```properties
logging.structured.format.console=logstash
```

Em producao, direcione stdout do container para ELK, Datadog ou OpenTelemetry
Collector. O endpoint operacional continua em `/actuator/health`.
