# 07 — Auditoria arquitetural atual

Data: 2026-10-20  
Checkout: `F:\Apex_Gestor_APP`  
Branch: `codex/consolida-v1-operacional`  
HEAD: `de662e36` — `integra ambiente completo e organiza v1 operacional`  
Upstream: `origin/codex/consolida-v1-operacional-homologada`  
Decisão: **NÃO APTO PARA PRODUÇÃO; APTO SOMENTE PARA HOMOLOGAÇÃO CONTROLADA, após bloquear os achados P0 e repetir os gates reais.**

Este documento separa fatos confirmados por inspeção estática/execução local, riscos prováveis e validações que não puderam ser executadas. Nenhum commit, push, deploy, reset ou alteração destrutiva foi feito. A única alteração funcional desta auditoria foi o guardrail `tools/audit/apex-enterprise-audit.ps1`, que passou a considerar o working tree e a detectar versões Flyway duplicadas e tabelas JPA sem referência nas migrations; este relatório foi adicionado como artefato documental.

## A. Resumo executivo

O projeto já tem uma base operacional significativa: backend Spring Boot modular, frontend Angular/Ionic, autenticação JWT com refresh persistido e rotação, method security, escopo de empresa/filial em vários fluxos, locks/versionamento em operações críticas, idempotência para venda/recebimento/entrada de NF, outbox com retry/DLQ lógica, auditoria, Docker Compose endurecido e scripts de backup/restore.

O estado atual, porém, não é um release reproduzível. No início da auditoria, o checkout continha 83 caminhos rastreados modificados e 17 caminhos untracked; entre os artefatos necessários estão `V1`, `V4` enterprise, `V6` e `V7`. O HEAD/upstream não contém esses arquivos. Uma build de checkout limpo, portanto, não reproduz o estado que está sendo auditado. A auditoria adicionou somente este relatório e o guardrail estático.

Os bloqueios mais importantes encontrados são:

1. `db/migration` contém duas migrations `V4` e é a localização padrão explícita da aplicação.
2. Cinco entidades JPA têm nomes de tabela que não aparecem na migration principal, incluindo `Notificacao`; com `ddl-auto=validate`, isso precisa ser comprovado como falha de inicialização em MySQL antes de qualquer aprovação.
3. Devolução não possui `Idempotency-Key` e não consolida linhas repetidas da mesma mercadoria antes de validar quantidade.
4. Abertura de caixa usa consulta seguida de insert sem uma restrição/invariante de banco que impeça duas aberturas concorrentes.
5. O fallback do outbox chama entrega externa dentro da transação quando RabbitMQ está desativado.
6. A tela de entrada de NF-e declara importar XML, mas envia fornecedor fixo `1` e `itens: []`; o contrato backend exige itens não vazios.
7. `npm audit` falha com 8 vulnerabilidades, incluindo 4 altas; o fix automático exige upgrade breaking para Angular 22.
8. Não há execução atual de backend, MySQL/Testcontainers, E2E, concorrência, carga ou restore porque Java configurado não existe neste host e o daemon Docker está indisponível.

## B. Arquitetura e inventário confirmado

- Backend: monólito Spring Boot/JPA com Controllers, Services, Repositories, Models e DTOs; 30 controllers, 45 services, 43 repositories e 263 classes Java de produção.
- Frontend: Angular/Ionic com rotas lazy-loaded, web, Electron e Capacitor; 54 arquivos TypeScript de aplicação contabilizados na inspeção.
- Persistência: MySQL 8.4 como alvo operacional, H2 para alguns testes/perfil local, Flyway para migrations, Hibernate Envers para histórico.
- Integrações: JavaMailSender opcional e RabbitTemplate/RabbitListener opcionais. Não há integração Sefaz/NF-e/NFC-e, PKI ou certificado fiscal encontrada.
- Operação: `docker-compose.yml` raiz constrói o backend e sobe MySQL; a interface é iniciada separadamente pelo workflow. Existe também `tools/fullstack/docker-compose.yml`, antigo e permissivo.
- Observabilidade: actuator health/info/metrics, `X-Trace-Id`, MDC e logs HTTP/outbox; não há stack Prometheus/Grafana/OpenTelemetry provisionada.
- Cache: cache de listas em memória no frontend e estados em storage do navegador; não há Redis/Caffeine/CacheManager no backend.
- Contratos: não foi encontrada dependência Springdoc/OpenAPI. Os contratos estão distribuídos entre DTOs, controllers e frontend.
- Tempo real: não foram encontrados SSE, WebSocket ou `EventSource`.
- Fiscal: o escopo implementado é entrada operacional de estoque e cálculo/financeiro interno; não há evidência de emissão nacional homologada.

## C. Baseline reexecutada

| Verificação | Resultado atual | Leitura correta |
|---|---|---|
| `npm --prefix Apex_Gestor run lint` | PASS | Frontend compila a verificação estática atual. |
| `npm --prefix Apex_Gestor run test -- --watch=false --browsers=ChromeHeadless` | PASS — 11 sucessos | Testes unitários/componentes frontend; não é E2E. |
| `npm --prefix Apex_Gestor run build:web:client` | PASS — bundle inicial 840,71 kB | Build web concluído; não prova backend integrado. |
| `npm --prefix Apex_Gestor run test:coverage` | PASS — 49,31% statements; 48,66% lines; 23,62% branches; 33,33% functions | Muito abaixo do gate solicitado de cobertura. |
| `npm --prefix Apex_Gestor audit --omit=dev --audit-level=high` | FAIL — 8 vulnerabilidades: 4 moderadas e 4 altas | Gate de dependências não passa. Não foi aplicado `--force`. |
| `node scripts/audit-spring-data-jpa.cjs` | PASS | Não foram encontrados padrões manuais bloqueados pelo script. |
| `node scripts/audit-no-secrets.cjs` | PASS | Nenhum segredo de alta confiança detectado pelo script. |
| `tools/audit/apex-enterprise-audit.ps1 -SkipBuild -Strict` | FAIL após correção do guardrail | Detectou V4 duplicado e tabelas JPA sem migration. O resultado antigo verde ignorava untracked. |
| parser PowerShell de scripts de operação | PASS | Sintaxe de backup/restore/auditoria válida; execução real não feita. |
| `docker compose -f docker-compose.yml config` | PASS | YAML raiz resolve com placeholders; daemon não foi necessário. |
| `docker compose -f tools/fullstack/docker-compose.yml config` | PASS | YAML resolve, mas paths/envs permissivos estão funcionalmente suspeitos. |
| `git diff --check` | PASS com avisos LF/CRLF | Sem erro de whitespace bloqueante. |
| Maven/backend | NÃO EXECUTÁVEL neste host | `JAVA_HOME` aponta para caminho inexistente; `java` não está disponível. |
| Docker daemon | NÃO DISPONÍVEL | Cliente 29.5.3 existe, mas `desktop-linux` não conecta ao engine. |
| MySQL/Testcontainers | NÃO VALIDADO | Há cliente MySQL, mas não há servidor/daemon disponível. |
| Playwright E2E | NÃO EXECUTADO | Requer API e frontend rodando; o workflow local não foi iniciado. |
| k6/JMeter/sonar-scanner/lizard | NÃO DISPONÍVEIS | Não há medição P95/P99, carga ou análise formal de complexidade. |

## D. O que já foi feito

### Segurança e identidade

- `SecurityConfig` habilita `@EnableMethodSecurity`, usa sessão stateless, 401 para não autenticado, CSP, frame deny, referrer policy e HSTS.
- A autenticação é obrigatória por default e a configuração permissiva lança erro quando `APEX_SECURITY_REQUIRE_AUTH=false`.
- `AuthService` valida usuário ativo, senha BCrypt, empresa/filial, aceite de privacidade e persiste refresh token apenas como hash.
- Refresh token tem rotação, expiração, logout e revogação de tokens ativos em reutilização.
- `JwtTokenService` exige segredo não vazio com pelo menos 32 caracteres, valida HS256, `typ`, issuer, audience, `iat` e `exp`.
- `PrivacyGuardService` prioriza claims do contexto de segurança; os fluxos modernos de privacidade usam buscas tenant-scoped.
- Há testes unitários de autorização, JWT, privacy guard, tenant scope e segurança.

### Operação comercial

- Venda usa máquina de estados operacional e escopo empresa/filial.
- Venda e recebimento usam `Idempotency-Key` com hash de payload; entrada de nota fiscal também possui a estrutura de idempotência.
- Produto, venda e caixa têm `@Version` em alterações locais; produto e venda possuem consultas pessimistas scoped em operações críticas.
- Caixa, estoque, pagamentos, financeiro operacional, devolução e crédito têm serviços transacionais e trilha de auditoria.
- O frontend deixou de usar os mocks comerciais principais e invalida cache após mutações de venda/caixa/estoque.

### Outbox e operação

- Outbox possui estados `QUEUED`, `PROCESSING`, `SENT`, `SKIPPED`, `FAILED` e `DEAD_LETTER`, contador de tentativas, backoff com jitter e reprocessamento de DLQ.
- Mensagens de Rabbit são publicadas após commit quando Rabbit está habilitado.
- Scripts de backup e restore exigem variáveis de processo, checksum e confirmação explícita para restore.
- Workflow reduz permissões globais para `contents: read`; permissões de escrita ficam em jobs de publicação.

### Qualidade já observada

- Lint, build web, testes frontend e scans estáticos atuais passam.
- A existência de testes backend é boa: há cenários de autenticação, JWT, autorização, Flyway, produto tenant-scoped, venda operacional, segurança e Testcontainers.
- O guardrail de auditoria foi fortalecido nesta execução para não produzir falso verde em working tree com arquivos untracked.

## E. Achados prioritários

### MIG-001 — P0 — versões Flyway duplicadas e cadeia de migrations não canônica

- Estado: confirmado estaticamente; falha de startup ainda não executada por falta de Java/MySQL.
- Evidência: `Apex-Gestordemo/src/main/resources/application.properties:19-22` define Flyway ativo, `spring.flyway.locations` padrão como `classpath:db/migration`; `Apex-Gestordemo/src/main/java/com/Apex/Apex_Gestordemo/FlywayRuntimeConfig.java:43-65` só escolhe `migration-mysql` quando a localização não é explicitamente definida. A pasta principal contém `V4__enterprise_compliance_audit_outbox.sql` e `V4__venda_caixa_devolucao_financeiro.sql`.
- Cenário: inicialização com o default atual coleta dois scripts da mesma versão `V4`; Flyway pode rejeitar a validação antes de JPA.
- Impacto: aplicação pode não iniciar e banco pode não migrar.
- Causa raiz: duas linhas de evolução foram misturadas; a localização explícita impede a seleção automática da família MySQL.
- Recomendação: escolher uma cadeia canônica, preservar histórico já aplicado e consolidar/renumerar somente com estratégia de upgrade documentada; não simplesmente apagar ou renomear migration já publicada.
- Teste de regressão: Flyway em banco MySQL vazio e upgrade de banco com histórico V2–V5, seguido de `ddl-auto=validate`.
- Aceite objetivo: uma única versão por location, histórico íntegro, migrations limpas e de upgrade executadas sem intervenção manual.

### DB-001 — P0 — drift entre entidades JPA e schema versionado

- Estado: confirmado por guardrail estático; runtime não validado.
- Evidência: `application.properties:16-22` usa `ddl-auto=validate`; o guardrail atual reporta `AuditLog -> Auditoria_Log`, `ConsentimentoPrivacidade -> Consentimento_Privacidade`, `EstoqueMovimentacao -> Estoque_Movimentacao`, `Notificacao -> Notificacao` e `SolicitacaoTitularDados -> Solicitacao_Titular_Dados` sem referência na migration principal. A baseline cria, por exemplo, `audit_log`, `consent_audit` e `estoque_movimento`, não os nomes mapeados nas entidades.
- Cenário: banco limpo recebe migrations, Hibernate valida entidades e encontra tabela ausente ou nome incompatível.
- Impacto: falha de boot, endpoints de auditoria/privacidade/notificação indisponíveis e risco de upgrade divergente.
- Causa raiz: entidades foram endurecidas/renomeadas sem uma migration de compatibilidade correspondente, enquanto há duas famílias de schema.
- Recomendação: gerar matriz entidade→tabela→colunas→índices contra MySQL real; decidir nomes canônicos; criar renames/backfill/FKs idempotentes; manter `validate`.
- Teste de regressão: `FlywayRuntimeTests` com `ddl-auto=validate` e `MySqlOperationalFlowTests` contra MySQL 8.4 real, incluindo todas as entidades.
- Aceite objetivo: contexto sobe em banco vazio e upgrade, sem `SchemaManagementException`, e todas as tabelas mapeadas existem com colunas/constraints compatíveis.

### REL-001 — P0 — hardening local não reproduzível a partir do HEAD

- Estado: confirmado no Git.
- Evidência: `git status` mostra 83 caminhos rastreados modificados e 17 untracked; `git ls-tree HEAD` contém apenas V2–V5 nas duas famílias. `V1__apex_schema_baseline.sql`, `V4__enterprise_compliance_audit_outbox.sql`, `V6__v1_operational_hardening.sql`, `V7__envers_audit_schema.sql` e testes importantes estão untracked.
- Cenário: CI, clone limpo ou deploy baseado em HEAD não recebe os artefatos que a inspeção local está usando.
- Impacto: falsa confiança, migrations ausentes e divergência entre homologação local e release.
- Causa raiz: grande conjunto de alterações foi acumulado sem fechamento de revisão/commit.
- Recomendação: antes de release, revisar cada diff, resolver duplicidades, incluir apenas artefatos aprovados e fazer uma validação em checkout limpo. Nenhum commit foi feito nesta tarefa porque não foi autorizado.
- Teste de regressão: criar checkout limpo a partir do commit candidato e repetir todos os comandos da seção C.
- Aceite objetivo: checkout limpo reproduz a mesma árvore, testes e migrations; `git status` limpo no candidato de release.

### SEC-001 — P1 — endpoint de notificações não aplica escopo tenant

- Estado: confirmado por código; exploração HTTP não executada.
- Evidência: `Apex-Gestordemo/src/main/java/Controller/NotificacaoController.java:20-22` chama `findByStatusOrderByCriadoEmDesc("PENDENTE")`; `Repository/NotificacaoRepository.java:11` não recebe empresa/filial. O modelo possui `empresaId` e `filialId` em `Model/Notificacao.java:41-45`, mas eles não participam da consulta.
- Cenário: usuário autenticado de uma empresa chama `GET /api/notificacoes/pendentes` e recebe pendências de todas as empresas.
- Impacto: exposição cross-tenant de mensagens e metadados operacionais.
- Causa raiz: endpoint legado usa repository global, sem `TenantScopeService`/DTO scoped.
- Recomendação: consulta por `empresaId` e `filialId` vindos do JWT, limitar paginação e não devolver entidade JPA diretamente.
- Teste de regressão: usuário A não vê notificações de B; 401/403 e tenant mismatch devem ser cobertos por MockMvc/MySQL.
- Aceite objetivo: nenhuma linha de outro tenant é retornada em qualquer combinação de status/paginação.

### CONC-001 — P1 — abertura concorrente de caixa sem invariante no banco

- Estado: confirmado por revisão; concorrência real não executada.
- Evidência: `CaixaService.java:56-71` consulta caixa aberto e depois insere; `CaixaSessaoRepository.java:15-20` usa lock na consulta, mas a ausência de uma linha não cria lock de predicado. `V2__erp_operacional_core.sql:1-14` não tem unique key para operador/tenant/estado aberto.
- Cenário: duas transações simultâneas observam nenhum caixa aberto e ambas inserem `ABERTO`.
- Impacto: dois caixas concorrentes, totais e recebimentos inconsistentes.
- Causa raiz: check-then-insert sem serialização em uma linha-pai ou constraint/invariante equivalente.
- Recomendação: escolher uma estratégia transacional explícita: chave de abertura por operador/tenant, lock em registro pai, ou mecanismo MySQL que represente estado aberto de forma unívoca; tratar conflito como 409.
- Teste de regressão: duas conexões executando abertura simultânea; exatamente uma deve vencer.
- Aceite objetivo: cardinalidade máxima de uma sessão aberta por operador/empresa/filial comprovada sob concorrência.

### IDEMP-001 — P1 — devolução/troca sem idempotência e sem agregação de itens

- Estado: confirmado por código; teste de repetição não executado.
- Evidência: `VendaController.java:124-128` não recebe `Idempotency-Key`; `VendaService.java:353-431` grava uma nova devolução em cada chamada. Em `VendaService.java:379-415`, `jaDevolvido` consulta apenas devoluções persistidas; duas linhas da mesma mercadoria na mesma requisição não acumulam a quantidade corrente antes da validação.
- Cenário: retry após timeout cria segunda devolução; payload com duas linhas de quantidade 3 para venda de quantidade 5 pode passar individualmente e devolver 6.
- Impacto: estoque, crédito/financeiro e status da venda podem ser ajustados acima do vendido.
- Causa raiz: ausência de chave/idempotência persistida para devolução e validação por linha, em vez de por produto agregado.
- Recomendação: exigir chave por tenant/venda, persistir hash e resultado, agregar itens por produto antes de validar, bloquear venda/devoluções e criar constraint/index de suporte.
- Teste de regressão: retry igual retorna o mesmo resultado; chave com payload diferente retorna 409; duplicidade de item excedente retorna 422/400 sem alterar estoque.
- Aceite objetivo: nenhuma repetição ou payload duplicado altera estoque/financeiro duas vezes.

### MSG-001 — P1 — efeito externo dentro da transação no fallback sem Rabbit

- Estado: confirmado por código; rollback/SMTP real não executado.
- Evidência: `FinancialDocumentDeliveryService.java:72-90` salva o outbox e, quando `rabbitEnabled=false`, chama `deliver` imediatamente; `deliver` chama `messagingService.sendSignedDocument` em `:114-149`.
- Cenário: o email é enviado e depois a transação que criou documento/outbox sofre rollback.
- Impacto: destinatário recebe mensagem sem registro durável; retry/reconciliação fica inconsistente.
- Causa raiz: o caminho sem broker executa efeito externo no mesmo fluxo transacional, em vez de publicar apenas após commit e deixar worker processar.
- Recomendação: sempre persistir `QUEUED`; executar worker após commit/por polling fora da transação original; garantir idempotência no provedor e reconciliação.
- Teste de regressão: rollback não chama canal externo; commit cria exatamente uma entrega processável; duas instâncias não enviam duplicado.
- Aceite objetivo: todo efeito externo só ocorre após confirmação durável do outbox.

### FISC-001 — P1 — tela de entrada de NF-e não executa o contrato anunciado

- Estado: confirmado por código; fluxo HTTP não executado.
- Evidência: `Apex_Gestor/src/app/pages/invoice-entry.page.ts:25-47` anuncia “Entrada de Mercadorias por XML”; `:106-123` extrai apenas `nNF`, `serie` e `vNF`; `:126-135` envia `fornecedor: { idFornecedor: 1 }` e `itens: []`. `NotaFiscalEntradaRequestDTO.java:12-16` exige `@NotEmpty` em itens.
- Cenário: usuário seleciona XML válido e clica em registrar; a API deve rejeitar o payload por itens vazios, ou o fluxo teria fornecedor incorreto se a validação fosse relaxada.
- Impacto: entrada operacional não utilizável e risco de falsa promessa fiscal.
- Causa raiz: parser está apenas demonstrativo e não há mapeamento de fornecedor/produtos do XML para o domínio.
- Recomendação: ou renomear para pré-conferência/manual e exigir dados completos, ou implementar parser/validação backend, chave de acesso, fornecedor, itens, unidades, custos e regras fiscais. Manter explícito que isso não é emissão nacional.
- Teste de regressão: XML válido com itens mapeados, XML inválido, produto não encontrado, fornecedor de outro tenant e retry.
- Aceite objetivo: nenhum clique de “registrar” envia payload incompleto; o backend só movimenta estoque após conferência válida.

### DEP-001 — P1 — dependências Angular vulneráveis e gate de CI vermelho

- Estado: confirmado pelo `npm audit` atual.
- Evidência: Angular 19.2.x reporta 8 vulnerabilidades transitivas/diretas: 4 moderadas e 4 altas; `npm audit fix --force` propõe Angular 22.2.3 com breaking change. O workflow executa `npm audit --omit=dev --audit-level=high` em `.github/workflows/main.yml:139-140`.
- Cenário: a job de frontend falha no CI; aplicar `--force` sem plano pode quebrar a aplicação.
- Impacto: release bloqueado ou superfície de segurança conhecida em produção.
- Causa raiz: linha Angular fixada em 19 enquanto o advisory exige upgrade maior.
- Recomendação: planejar upgrade major com branch, testes e changelog; enquanto isso avaliar mitigação de SSR/HttpTransferCache e bloquear release que não aceite risco formal.
- Teste de regressão: `npm ci`, `npm audit --omit=dev --audit-level=high`, lint, testes, build web/Electron/mobile e smoke E2E.
- Aceite objetivo: zero vulnerabilidades high/critical no lockfile de produção ou exceção formal com prazo, mitigação e aprovação.

### QUALITY-001 — P1 — cobertura e gates quantitativos insuficientes

- Estado: confirmado.
- Evidência: cobertura frontend atual é 49,31% statements, 48,66% lines e 23,62% branches; `pom.xml` não configura JaCoCo/PIT; workflows executam testes, mas não impõem cobertura backend/branch.
- Cenário: regressão em backend, concorrência ou regras de tenant passa sem um gate quantitativo.
- Impacto: confiança de release abaixo dos critérios solicitados (>85% lines/statements e metas de branch).
- Causa raiz: cobertura backend não instrumentada e frontend abaixo da meta.
- Recomendação: adicionar JaCoCo com thresholds por módulo, aumentar testes de segurança/tenant/concorrência/idempotência e separar cobertura de unidade, integração e E2E.
- Teste de regressão: pipeline falha abaixo dos thresholds e publica relatório.
- Aceite objetivo: metas acordadas (>85% lines/statements, branch definido) atingidas e verificadas no CI.

## F. Fluxos críticos: estado atual

| Fluxo | Já existe | Falta comprovar/corrigir |
|---|---|---|
| Login/refresh/logout | BCrypt, JWT, refresh hash/rotação/revogação, testes unitários | rate limit, E2E real, proxy confiável para `X-Forwarded-For`, storage web sem refresh HttpOnly/keystore |
| PDV/venda | escopo tenant, estado, locks/version, idempotência de venda, frontend real | MySQL concorrente, timeout/retry real, paginação e planos de consulta |
| Caixa | abertura, suprimento, sangria, fechamento, recebimento e auditoria | impedir duas aberturas concorrentes; MySQL real |
| Estoque | produto scoped, lock pessimista, `@Version`, movimentação | invariantes concorrentes e cobertura de entradas/devoluções |
| Pagamento/financeiro | recebimento idempotente, contas/credito, cálculos e auditoria | outbox transacional, cobertura de regras e contrato OpenAPI |
| Devolução/troca | estado, estoque de retorno/troca, crédito operacional | idempotência, agregação de itens e teste de concorrência |
| Entrada de NF | endpoint transacional e idempotência opcional | UI envia itens vazios; não há XML fiscal backend; sem unicidade de negócio comprovada |
| Privacidade/tenant | serviço moderno scoped e perfil legado isolado | notificações globais; drift de tabelas; retirar overloads globais restantes |
| Documento financeiro | workflow, assinatura hash, outbox/retry/DLQ | hash não é assinatura PKI/legal; efeito externo após commit; Rabbit/SMTP reais |

## G. Plano priorizado de execução

### P0 — antes de qualquer homologação de release

1. Parar e decidir a fonte canônica das migrations; resolver V4 duplicado sem reescrever histórico aplicado.
2. Gerar matriz JPA/schema em MySQL 8.4 vazio e em cópia de upgrade; corrigir nomes, colunas, índices, FKs e tabelas ausentes.
3. Consolidar as alterações locais em um candidato reproduzível; revisar os 83 modificados e 17 untracked antes de commit autorizado.
4. Reexecutar com Java compatível, Docker ativo e banco MySQL real: `FlywayRuntimeTests`, `MySqlOperationalFlowTests`, `PdvVendaEstoqueEnversIntegrationTests` e contexto `ddl-auto=validate`.

### P1 — integridade e segurança operacional

1. Corrigir escopo tenant de notificações e auditar todos os repositories globais.
2. Implementar devolução idempotente, agregação por produto, lock e invariantes de banco.
3. Fechar corrida de abertura de caixa e criar teste com duas conexões.
4. Redesenhar fallback do outbox para efeito externo somente após commit.
5. Corrigir entrada de NF como fluxo manual explícito ou implementar parser/mapeamento XML real.
6. Planejar upgrade Angular e adicionar rate limiting para login/endpoints sensíveis.
7. Adicionar JaCoCo/thresholds e elevar testes de tenant, autorização, concorrência e retry.

### P2 — preparação de operação

1. Paginar vendas/históricos e mover busca/filtro para query/índice no banco.
2. Publicar OpenAPI e matriz 401/403/404/409/422/429/500/503.
3. Medir P50/P95/P99, throughput, pool Hikari, índices e slow queries com carga representativa.
4. Executar backup/restore isolado cronometrado, validar checksum, RPO/RTO e rollback.
5. Unificar ou retirar `tools/fullstack/docker-compose.yml`, que hoje usa `ddl-auto=update`, auth default false, `JWT_SECRET` incompatível e paths inexistentes neste checkout.
6. Provisionar métricas, alertas e dashboards de API, banco, outbox, erros de autenticação e jobs.

## H. Gates de aceitação

O projeto só deve mudar de “homologação controlada” para “produção aprovada” quando todos os itens abaixo tiverem evidência anexada ao candidato:

- migrations únicas, ordenadas e executadas em MySQL 8.4 vazio e upgrade;
- `ddl-auto=validate` sem drift em todas as entidades;
- matriz HTTP e method security com 401/403, cross-tenant negativo e perfil legado desativado;
- invariantes de estoque, caixa, contas, crédito e devolução testadas com duas conexões;
- idempotência de venda, recebimento, NF e devolução sob retry/timeout;
- outbox com commit/rollback, consumidor duplicado, retry, DLQ e reprocessamento;
- cobertura backend/frontend e branch thresholds publicados no CI;
- npm audit sem high/critical não aceitas formalmente;
- carga com P95/P99 e SLOs definidos;
- backup e restore reais, isolados e cronometrados;
- deploy, health/readiness, rollback e proteção de segredos verificados;
- escopo fiscal documentado como operacional, ou integração fiscal nacional homologada separadamente.

## I. Conclusão objetiva

O Apex Gestor já saiu de um estado de demo permissivo e tem componentes relevantes de segurança, tenant, operação e auditoria. O trabalho feito é real, mas está parcialmente apenas no working tree e ainda mistura duas famílias de migrations. A situação atual é **boa para continuar uma homologação técnica controlada**, não para afirmar prontidão de produção.

O próximo passo correto é resolver os P0 de migrations/schema/reprodutibilidade com um JDK e MySQL/Docker funcionais. Depois disso, os P1 de devolução, caixa, notificações, outbox, dependências e cobertura precisam ser fechados antes de qualquer decisão comercial/fiscal.
