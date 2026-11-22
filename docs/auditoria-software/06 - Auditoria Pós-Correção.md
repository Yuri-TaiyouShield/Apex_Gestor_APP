# 06 — Auditoria pós-correção

Data: 2026-09-17  
Branch verificada: `codex/consolida-v1-operacional`  
Escopo: revalidação direcionada dos achados da auditoria técnica e registro das limitações de ambiente.

## Resumo

Foram aplicadas correções reais no backend Spring Boot, frontend Angular, migrations/configuração, CI e operação. O caminho demo foi retirado do runtime produtivo; autenticação, claims JWT, method security e escopo tenant passaram a ser requisitos explícitos. Venda, finalização no caixa e entrada de NF agora aceitam `Idempotency-Key`; estoque/caixa usam versionamento e locks; a Outbox diferencia envio confirmado, ignorado, retry e dead-letter; os fallbacks mock dos fluxos comerciais foram removidos.

Não há aprovação de produção nesta etapa. A validação MySQL/Testcontainers, E2E com stack completa e restore real continuam bloqueadas pela indisponibilidade do Docker neste host. O `npm audit` ainda reporta oito vulnerabilidades na linha Angular 19; a correção automática proposta exige upgrade breaking para Angular 22.

## Matriz de evidências

| ID | Estado anterior | Ação realizada | Estado atual | Teste/evidência | Limitação |
|---|---|---|---|---|---|
| SEC-001 | Login paralelo com usuários/senha demo hardcoded | `AuthController` delega ao `AuthService`; login persistido, BCrypt, usuário ativo, tenant e refresh persistido; demo só em variante de desenvolvimento | CORRIGIDO | `AuthServiceTests` (5 cenários), teste backend integral | E2E de login não executado |
| SEC-002 | Secret JWT previsível por default | Propriedade obrigatória, mínimo de 32 caracteres, sem espaços; issuer/audience/algoritmo/expiração validados | CORRIGIDO | `JwtTokenServiceTests`; contexto Flyway inicia com secret explícito | Não foi feito teste de deploy externo |
| SEC-003 | `@PreAuthorize` sem ativação explícita comprovada | `@EnableMethodSecurity`; regras HTTP críticas mantidas; teste de presença da anotação | PARCIALMENTE CORRIGIDO | `SecurityConfigTests`; `AuthorizationServiceTests` | Matriz HTTP completa 401/403 requer stack de integração |
| SEC-004 | Auth desligável e headers de identidade confiados | auth obrigatória por default/manifests; `PrivacyGuardService` usa JWT/SecurityContext; headers removidos do interceptor/CORS | PARCIALMENTE CORRIGIDO | `PrivacyGuardServiceTests` com spoofing; `TenantScopeServiceTests` | Não executado contra gateway/deploy real |
| SEC-005 | Refresh fallback em `localStorage` | Backend tem hash, rotação, expiração, logout, revogação e detecção de reutilização; cliente mantém compatibilidade de armazenamento | PARCIALMENTE CORRIGIDO | `AuthServiceTests` cobre rotação, expiração, revogação/reuso e logout | Cookie HttpOnly/keystore obrigatório ainda depende do desenho de distribuição |
| SEC-006 | Documentos, cálculos, auditoria e LGPD usavam IDs globais | Entidades/repositories/serviços críticos receberam `empresa_id`/`filial_id` e buscas escopadas; exportação/anonimização moderna é tenant-scoped | PARCIALMENTE CORRIGIDO | testes de tenant e escopo de produto; audit JPA passa | Há serviços legados com overloads globais que exigem retirada gradual |
| DB-001 | Tenant nullable e poucas constraints | V6 adiciona índices/uniques de idempotência, versionamento e constraint de conta por venda; campos legados continuam nullable para backfill seguro | PARCIALMENTE CORRIGIDO | migration aplicada em H2; diff/migration revisados | Backfill/FKs e `NOT NULL` ainda precisam de dados reais e janela controlada |
| MIG-001 | `ddl-auto=update` em deploy e tabelas sem migration | deploys usam `validate`; V6 cobre tabelas hardening; Flyway com `validate-on-migrate` | PARCIALMENTE CORRIGIDO | `FlywayRuntimeTests` passou | A matriz de nomes/case entre base antiga e migrations MySQL ainda precisa ser validada no banco alvo |
| CONC-001 | Read-modify-write de estoque sem proteção | `@Version`, lock pessimista scoped e transações nos fluxos de produto/venda/NF | PARCIALMENTE CORRIGIDO | compilação/testes unitários; teste concorrente MySQL não executado | Docker/Testcontainers indisponível |
| CONC-002 | Abertura/totais de caixa sem lock/version | `CaixaSessao` versionada, consultas de sessão aberta com lock e transações | PARCIALMENTE CORRIGIDO | compilação/testes backend | Concorrência real com duas conexões não executada |
| IDEMP-001 | Retry podia duplicar venda/NF/conta | `Idempotency-Key` + hash de payload em venda, recebimento e NF; unique tenant em conta a receber | PARCIALMENTE CORRIGIDO | compilação; contratos de serviço preservados | Cenários de timeout com banco real ainda pendentes |
| FIN-001 | Outbox marcava `SENT` sem confirmação | `SENT` só após retorno de envio; ausência de canal vira `SKIPPED` | CORRIGIDO | `FinancialDocumentDeliveryServiceTests` | SMTP real não configurado |
| MSG-001 | Sem retry/backoff/DLQ/reprocessamento | estados `PROCESSING/FAILED/SKIPPED/DEAD_LETTER`, backoff exponencial com jitter, limite e reprocessamento | PARCIALMENTE CORRIGIDO | teste de falha, retry e DLQ passou | Broker RabbitMQ não executado |
| FISC-001 | Entrada operacional não era NF-e/NFC-e nacional | Nenhuma promessa fiscal foi ampliada; fluxo permanece explicitamente operacional | NÃO CORRIGIDO | achado continua documentado | Requer provedor, certificado, XML, protocolos e homologação por UF |
| RES-001 | UI substituía falha por dados fictícios | mocks removidos de PDV, checkout, vitrine, dashboard, relatórios, entidades, pagamentos e NF; escrita falha sem confirmação real | CORRIGIDO | lint/build frontend passam | E2E offline/indisponibilidade não executado |
| DR-001 | Backup/restore apenas recomendado | scripts `backup-mysql.ps1`/`restore-mysql.ps1`, retenção, checksum, confirmação de restore e runbook versionados | PARCIALMENTE CORRIGIDO | parse PowerShell OK; procedimento reproduzível | Nenhuma restauração foi executada sem MySQL/Docker |
| DEVOPS-001 | write global e `push --force` em Pages | `contents: read` global; write apenas em publicação; Pages usa fluxo com `force-with-lease`/primeiro push controlado | PARCIALMENTE CORRIGIDO | workflow revisado estaticamente | Proteções do repositório remoto não são observáveis localmente |
| QUALITY-001 | lint vermelho, JPA audit vermelho e backend sem execução local | lint e audit JPA corrigidos; testes backend executados com JDK 21 override; dependências mantidas sem upgrade breaking | PARCIALMENTE CORRIGIDO | `npm run lint` 0; JPA audit PASS; backend 49 testes, 0 falhas, 1 skip | JDK oficial do projeto é 25/release 24; cobertura backend não configurada |
| API-001 | RuntimeException amplo e contrato inconsistente | Problem Details, traceId, handlers específicos para 404/409/503 e conflitos de idempotência | PARCIALMENTE CORRIGIDO | compilação/testes backend | OpenAPI e matriz completa 422/429 ainda não adicionados |
| PERF-001 | listas globais/filtros em memória | listagens financeiras/documentais críticas passaram a ter paginação/escopo e limites nos fluxos alterados | PARCIALMENTE CORRIGIDO | revisão de código | profiling, planos MySQL e P95/P99 não executados |
| OBS-001 | trace/health sem operação de negócio | correlation/trace ID, actuator metrics, sanitização de erro e logs de outbox/retry adicionados | PARCIALMENTE CORRIGIDO | compilação e testes | Prometheus/Grafana/OpenTelemetry/alertas não provisionados |

## Comandos executados

| Comando | Resultado |
|---|---|
| `npm run lint` | PASS — 0 erros |
| `npm test -- --watch=false --browsers=ChromeHeadless` | PASS — 11 sucessos |
| `npm run build:web:client` | PASS — build Angular concluído |
| `npm audit --omit=dev --audit-level=high` | FAIL de gate — 8 vulnerabilidades (5 moderadas, 3 altas); fix automático exige Angular 22 breaking |
| `node scripts/audit-no-secrets.cjs` | PASS — nenhum segredo de alta confiança encontrado |
| `node scripts/audit-spring-data-jpa.cjs` | PASS — nenhum padrão manual `@Query` fora do gate |
| `mvnw -q -Dmaven.compiler.release=21 -Dtest=AuthServiceTests,FinancialDocumentDeliveryServiceTests,SecurityConfigTests test` | PASS |
| `mvnw -q -Dmaven.compiler.release=21 test` | PASS nos relatórios Surefire: 49 testes, 0 falhas, 1 skip por Testcontainers/Docker indisponível |
| parser dos scripts PowerShell | PASS |
| `git diff --check` | PASS; apenas avisos de conversão LF/CRLF |

## Gates de produção

| Gate | Estado | Fundamentação |
|---|---|---|
| SECURITY GATE | BLOCKED / UNVERIFIABLE | implementação e testes unitários positivos; integração HTTP completa/deploy não executados |
| TENANT GATE | BLOCKED / UNVERIFIABLE | escopo crítico implementado, mas cross-tenant completo requer MySQL real e cobertura dos legados |
| DATABASE GATE | BLOCKED / UNVERIFIABLE | Flyway H2 passou; banco vazio/upgrade MySQL e backfill não foram comprovados |
| CONCURRENCY GATE | BLOCKED / UNVERIFIABLE | locks/version existem; duas conexões concorrentes não foram executadas |
| IDEMPOTENCY GATE | PARTIAL / BLOCKED | persistência/hash/uniques implementados; timeout/retry com banco real pendente |
| TEST GATE | PARTIAL | backend/frontend unitários passam; integração MySQL, E2E e cobertura não comprovados |
| CI/CD GATE | PARTIAL | permissões reduzidas estaticamente; execução CI remota/proteções não verificadas |
| BACKUP/RESTORE GATE | BLOCKED / UNVERIFIABLE | scripts/runbook versionados; restore real não executado |
| OBSERVABILITY GATE | PARTIAL | trace/health/metrics básicos; alertas e dashboards produtivos ausentes |

Conclusão objetiva: a branch está mais segura e operacional para homologação técnica controlada, mas não atende ainda ao gate de produção comercial/fiscal. Os bloqueios principais são validação MySQL real, concorrência/E2E, backfill de tenant, restore cronometrado, cobertura e decisão de upgrade das dependências Angular.
