# 03 — Auditoria técnica e achados

Cada ficha registra ID, área/componente, evidência, causa, impactos, criticidade, probabilidade, risco, recomendação, exemplo, prioridade, esforço e dependências.

## SEC-001 — Credenciais demo hardcoded no endpoint de login

- Área/componente: Segurança; Apex-Gestordemo/src/main/java/Controller/AuthController.java, linhas 29–53.
- Estado: confirmado.
- Problema: TEST_PASSWORD é uma constante fixa e DEMO_USERS contém seis contas fixas. O login não consulta AuthService, usuário persistido ou BCrypt.
- Evidência: o controller aceita login quando o usuário está no mapa e a senha coincide; SECURITY.md descreve outro modelo de JWT/refresh seguro.
- Conceito/causa: credencial embutida, autenticação paralela e caminho de demonstração presente no runtime principal; CWE-798/CWE-287.
- Impacto técnico: qualquer pessoa que conheça o código pode autenticar contas privilegiadas; tokens são emitidos fora do fluxo persistido.
- Impacto operacional/negócio: acesso a dados e operações de qualquer tenant configurado no token; impossibilidade de revogação consistente e risco de fraude.
- Criticidade: crítica. Probabilidade: alta. Risco: crítico.
- Recomendação: remover o mapa demo do build de produção; delegar login ao AuthService; exigir usuário ativo, BCrypt, consentimento e escopo de tenant; manter demo somente em profile/teste isolado.
- Exemplo: tentativa com v5_dono e a senha constante chega ao token sem validar credencial no banco.
- Prioridade: P0. Esforço: médio. Dependências: desenho de identidade, migração de usuários, testes HTTP de 401/403 e revisão de SECURITY.md.

## SEC-002 — Segredo JWT previsível e não obrigatório

- Área/componente: Segurança/configuração; Apex-Gestordemo/src/main/java/Service/JwtTokenService.java, linha 30; application.properties.
- Estado: confirmado para o fallback; impacto produtivo provável se a variável não for fornecida.
- Problema: o segredo padrão apex-dev-secret-change-me é usado quando a propriedade não existe; não há validação explícita de entropia/tamanho no startup.
- Conceito/causa: default de desenvolvimento carregável em produção; configuração fail-open.
- Impacto técnico: possibilidade de forjar tokens se o fallback estiver ativo; claims de usuário/roles/tenant passam a ser confiáveis apenas pela assinatura.
- Impacto operacional/negócio: bypass de identidade e autorização, além de invalidação difícil de incidentes.
- Criticidade: crítica se ativo em produção; alta no desenho. Probabilidade: média, dependendo do deploy. Risco: alto/crítico.
- Recomendação: remover default; exigir segredo/keystore por secret manager; validar tamanho/entropia e issuer/audience; rotacionar por kid com janela controlada.
- Exemplo: startup de produção sem APEX_JWT_SECRET deveria falhar antes de expor a API.
- Prioridade: P0. Esforço: médio. Dependências: gestão de secrets e testes de configuração.

## SEC-003 — Autorização de método não comprovadamente ativada

- Área/componente: Segurança; SecurityConfig, FinancialComplianceController e PrivacyAdminController.
- Estado: confirmado quanto à ausência de EnableMethodSecurity; bypass efetivo é provável conforme profile.
- Problema: FinancialComplianceController usa vários @PreAuthorize, mas não foi encontrado EnableMethodSecurity. SecurityConfig protege URLs de forma incompleta e a rota /api/financeiro não aparece no bloco de regras específicas.
- Conceito/causa: duas camadas de autorização sem teste de integração; dependência de anotação sem habilitação explícita.
- Impacto técnico: usuário autenticado pode alcançar endpoints financeiros sem o papel pretendido; controllers de privacidade também não fazem require explícito em todos os métodos.
- Impacto operacional/negócio: exposição de cálculos, documentos, auditoria e dados pessoais a perfis indevidos.
- Criticidade: alta. Probabilidade: alta quando require-auth=true. Risco: alto.
- Recomendação: habilitar method security ou retirar anotações e centralizar em AuthorizationService; mapear todas as rotas; criar matriz de 401/403 por endpoint e role.
- Exemplo: um endpoint com @PreAuthorize não deve depender apenas de configuração incidental para negar acesso.
- Prioridade: P0. Esforço: médio. Dependências: SEC-001, SEC-004 e suíte de autorização HTTP.

## SEC-004 — Autenticação desativada e headers de identidade confiados

- Área/componente: Segurança/tenant; SecurityConfig, application-prod.properties, railway.toml, render.yaml, PrivacyGuardService e auth.interceptor.ts.
- Estado: confirmado no deploy configurado: Railway/Render definem APEX_SECURITY_REQUIRE_AUTH=false; trust de headers é confirmado.
- Problema: com auth desligada, qualquer requisição passa; PrivacyGuardService usa X-Apex-User-Id, X-Apex-Roles, X-Apex-Empresa-Id e X-Apex-Filial-Id como fallback.
- Conceito/causa: modo demo não isolado; contexto de segurança derivado de dados controláveis pelo cliente; CWE-863.
- Impacto técnico: spoofing de role e tenant; chamadas manuais podem satisfazer AuthorizationService sem JWT válido.
- Impacto operacional/negócio: leitura, criação ou alteração em escopo alheio; comprometimento total da segregação entre empresas.
- Criticidade: crítica. Probabilidade: alta nas implantações atuais. Risco: crítico.
- Recomendação: APEX_SECURITY_REQUIRE_AUTH=true por padrão e em todos os manifests; remover fallback de headers em produção; se houver gateway interno, aceitar contexto somente com assinatura/mTLS e allowlist de rede.
- Exemplo: X-Apex-Roles: ROLE_SYSADMIN combinado com IDs de empresa/filial pode satisfazer guardas manuais quando a API está permit-all.
- Prioridade: P0. Esforço: médio. Dependências: SEC-001, SEC-003, gestão de configuração e testes negativos.

## SEC-005 — Fallback de refresh token em localStorage

- Área/componente: Segurança cliente; SecureStorageService e AuthService.
- Estado: confirmado como desenho; exploração depende de XSS ou comprometimento do dispositivo.
- Problema: armazenamento nativo seguro é usado quando disponível, mas o fallback grava refresh token em localStorage.
- Conceito/causa: compatibilidade web/mobile sem estratégia de cookie HttpOnly ou keystore obrigatório.
- Impacto técnico: scripts no mesmo origin podem ler o token; cópia de perfil/dispositivo amplia a janela de sequestro de sessão.
- Impacto operacional/negócio: sessão persistente comprometida, especialmente em contas administrativas.
- Criticidade: média/alta. Probabilidade: média. Risco: alto em web exposta.
- Recomendação: preferir cookie HttpOnly/SameSite para web; keystore obrigatório em mobile/desktop; reduzir escopo e vida do refresh; CSP forte e rotação/revogação efetivas.
- Exemplo: o código assume localStorage quando secure storage não está disponível.
- Prioridade: P1. Esforço: médio. Dependências: arquitetura de sessão e hardening de CSP.

## SEC-006 — Isolamento multiempresa ausente em documentos financeiros e privacidade

- Área/componente: Segurança de dados/DB; FinancialDigitalDocument, FinancialCalculation, FinancialAuditEvent, FinancialDocumentDeliveryOutbox e DireitosTitularService.
- Estado: confirmado no código.
- Problema: entidades financeiras não têm empresaId/filialId; listRecent usa findAll paginado e findDocument usa findById. Exportação/anonymização de Cliente também usa findById sem escopo.
- Conceito/causa: tenant aplicado em parte do ERP, não como invariável transversal; IDOR/cross-tenant.
- Impacto técnico: IDs globais podem consultar ou alterar dados fora do tenant atual, mesmo com usuário autenticado.
- Impacto operacional/negócio: vazamento de dados financeiros e pessoais entre empresas; quebra de LGPD e confiança comercial.
- Criticidade: crítica. Probabilidade: média/alta se os endpoints estiverem expostos. Risco: crítico.
- Recomendação: adicionar tenant obrigatório às tabelas e entidades; repositories sempre recebem empresa/filial; derivar contexto do JWT; rejeitar null em produção; cobrir IDOR com dois tenants em testes.
- Exemplo: GET/POST de documento financeiro com ID de outro tenant não deve retornar o documento nem alterar seu estado.
- Prioridade: P0. Esforço: alto. Dependências: MIG-001, DB-001 e migração/backfill controlado.

## MIG-001 — Deploy permite ddl-auto=update e schema não é determinístico

- Área/componente: Banco/entrega; application-prod.properties, railway.toml, render.yaml e migrations V2–V5.
- Estado: confirmado.
- Problema: manifests de deploy definem JPA_DDL_AUTO=update, enquanto o profile prod sugere validate. Entidades atuais incluem tabelas sem migration correspondente, como financial_digital_document, financial_audit_event, financial_document_outbox, financial_calculation e license_activation.
- Conceito/causa: schema gerenciado por Hibernate e Flyway ao mesmo tempo; ausência de baseline/migrations completas.
- Impacto técnico: diferenças entre ambientes, alterações implícitas, falhas tardias e rollback não reprodutível.
- Impacto operacional/negócio: deploy pode iniciar com banco parcialmente criado ou produzir perda/deriva de dados.
- Criticidade: alta. Probabilidade: alta em banco novo ou upgrade. Risco: alto.
- Recomendação: criar migrations versionadas para todas as entidades; validar em banco vazio e upgrade real; usar ddl-auto=validate em produção; remover baseline-on-migrate como compensação sem procedimento documentado; registrar checksum e rollback.
- Exemplo: uma tabela usada pelo workflow financeiro deve existir por migration antes de o controller ser publicado.
- Prioridade: P0. Esforço: alto. Dependências: inventário de schema, DB-001 e testes MySQL.

## DB-001 — Tenant nullable, FKs e invariantes insuficientes

- Área/componente: Banco; migrations V2–V5 e modelos operacionais.
- Estado: confirmado.
- Problema: empresa_id/filial_id aparecem como nullable em várias tabelas; constraints de integridade e unique por tenant são incompletas. Barcode, caixa aberto e conta por venda dependem de verificações na aplicação.
- Conceito/causa: backfill legado preservado sem fase final de endurecimento.
- Impacto técnico: registros órfãos ou invisíveis por filtros de igualdade; concorrência pode criar duplicatas.
- Impacto operacional/negócio: relatórios divergentes, estoque/financeiro incorretos e impossibilidade de confiar no banco como segunda barreira.
- Criticidade: alta. Probabilidade: alta com carga concorrente/migração. Risco: alto.
- Recomendação: backfill auditado; tornar tenant NOT NULL; FKs para empresa/filial; índices compostos; unique constraints por escopo; checks para valores e status.
- Exemplo: código de barras deve ser único em empresa/filial no banco, não apenas após um SELECT.
- Prioridade: P0. Esforço: alto. Dependências: MIG-001 e janela de migração.

## CONC-001 — Race condition no saldo de estoque

- Área/componente: Consistência/estoque; VendaService, EstoqueMovimentacaoService e NotaFiscalService.
- Estado: confirmado no código; impacto concorrente não executado por falta de ambiente.
- Problema: o fluxo lê quantidadeEstoque, calcula posterior e salva; Produto não tem @Version e não há lock pessimista na atualização.
- Conceito/causa: read-modify-write sem controle de concorrência.
- Impacto técnico: overselling, lost update e saldoAnterior/posterior inconsistente no histórico.
- Impacto operacional/negócio: venda acima do estoque e divergência de inventário/faturamento.
- Criticidade: crítica. Probabilidade: média em uma loja; alta com múltiplos caixas/importações. Risco: crítico.
- Recomendação: @Version com retry controlado ou SELECT FOR UPDATE; preferir UPDATE atômico com condição de saldo; teste concorrente com vendas/NF/ajustes.
- Exemplo: duas vendas que leem estoque 1 podem ambas gravar estoque 0 depois de vender duas unidades.
- Prioridade: P0. Esforço: médio/alto. Dependências: DB-001 e suíte de concorrência.

## CONC-002 — Abertura e totais de caixa sem exclusão/versão

- Área/componente: Consistência/caixa; CaixaService e CaixaSessao.
- Estado: confirmado no código.
- Problema: abrir faz consulta e depois insert, sem unique constraint para um caixa aberto; suprimento, sangria e venda fazem incremento read-modify-write sem @Version/lock.
- Conceito/causa: invariantes somente na camada de serviço.
- Impacto técnico: dois caixas abertos ou totais perdidos em operações simultâneas.
- Impacto operacional/negócio: fechamento incorreto e reconciliação financeira manual.
- Criticidade: alta. Probabilidade: média. Risco: alto.
- Recomendação: constraint de uma sessão aberta por operador/filial, lock transacional ou coluna de versão; operações idempotentes por evento de venda.
- Exemplo: duas requisições simultâneas passam pelo if da linha 57 e criam sessões distintas.
- Prioridade: P0. Esforço: médio. Dependências: DB-001 e IDEMP-001.

## IDEMP-001 — Finalização de venda e entrada de NF sem idempotência de comando

- Área/componente: Integridade financeira/fiscal; FinanceiroOperacionalService e NotaFiscalService.
- Estado: confirmado no desenho; duplicação efetiva não foi executada.
- Problema: conta a receber usa find-then-create sem unique constraint em venda_id/tenant; entrada de NF não valida chave única de número/série/fornecedor/tenant.
- Conceito/causa: retry de cliente/rede não tratado como comando idempotente.
- Impacto técnico: duplicidade de contas, movimentação de estoque e entradas fiscais.
- Impacto operacional/negócio: cobrança/estoque incorretos e retrabalho de conciliação.
- Criticidade: alta. Probabilidade: média. Risco: alto.
- Recomendação: exigir Idempotency-Key; persistir resultado do comando; unique keys de negócio; transação com tratamento de DuplicateKey; guardar chave fiscal quando aplicável.
- Exemplo: timeout após resposta pode levar o PDV a reenviar a mesma finalização.
- Prioridade: P0. Esforço: alto. Dependências: modelo de comandos e migrations.

## FISC-001 — Entrada de NF não é fiscal nacional e hash não é assinatura digital

- Área/componente: Produto/compliance; NotaFiscalController/Service e FinancialDocumentWorkflowService.
- Estado: confirmado.
- Problema: a NF aceita DTO de número, série, fornecedor, produtos e quantidades; não há importação XML, validação de chave, emissão, cancelamento, protocolo, contingência ou integração por UF. A assinatura financeira é um hash.
- Conceito/causa: escopo operacional interno apresentado junto de nomenclatura fiscal/digital mais ampla.
- Impacto técnico: o sistema registra estoque, mas não comprova conformidade fiscal nem não repúdio criptográfico.
- Impacto operacional/negócio: risco de venda nacional irregular, multas e promessa comercial incompatível com capacidade técnica.
- Criticidade: alta para lançamento Brasil. Probabilidade: alta se a promessa não for restringida. Risco: alto.
- Recomendação: posicionar como entrada operacional até integração homologada; escolher provedor/estratégia por UF; implementar XML, certificado, assinatura PKI, protocolos, cancelamento, contingência e trilha; validar com contador.
- Exemplo: assinaturaDigitalHash deve ser explicitamente rotulada como integridade/hash até existir certificado e verificação.
- Prioridade: P1 para piloto; P0 antes de promessa fiscal. Esforço: muito alto. Dependências: decisão fiscal, parceiro, certificados e UAT.

## FIN-001 — Outbox marca envio como SENT quando email está desabilitado

- Área/componente: Financeiro/mensageria; FinancialDocumentDeliveryService linhas 103–110 e FinancialDocumentMessagingService linhas 25–31.
- Estado: confirmado.
- Problema: sendSignedDocument retorna false quando email está desabilitado ou sem JavaMailSender, mas deliver ignora o false e grava outbox como SENT.
- Conceito/causa: booleano sem semântica de skipped/failed e transição de estado incondicional.
- Impacto técnico: documento pode permanecer não enviado sem erro visível.
- Impacto operacional/negócio: perda de comunicação de documentos e falsa evidência de entrega.
- Criticidade: alta. Probabilidade: alta com defaults atuais de email false. Risco: alto.
- Recomendação: distinguir SENT, SKIPPED e FAILED; exigir canal configurado para marcar SENT; retornar estado operacional claro; testar email ausente, erro transitório e sucesso.
- Exemplo: no default atual, a primeira entrega pode ser considerada enviada sem mensagem.
- Prioridade: P0. Esforço: baixo/médio. Dependências: contrato de status e observabilidade.

## MSG-001 — Outbox sem retry, backoff, DLQ ou reprocessamento

- Área/componente: Resiliência; FinancialDocumentDeliveryService e FinancialMessagingConfig.
- Estado: confirmado no código; falha operacional real não foi executada.
- Problema: falha vira FAILED e não há scheduler/reaper/retry/backoff/DLQ/endpoint de reprocessamento. Rabbit habilitado sem RabbitTemplate lança após commit.
- Conceito/causa: outbox criada, mas ciclo operacional de entrega incompleto.
- Impacto técnico: eventos presos, duplicidade se operador repetir manualmente e pouca visibilidade do motivo.
- Impacto operacional/negócio: documentos não entregues e suporte sem procedimento seguro.
- Criticidade: alta. Probabilidade: média. Risco: alto.
- Recomendação: retry exponencial com jitter e limite; DLQ; consumidor idempotente; métricas de idade/tentativas; reprocessamento autorizado e auditado; teste de indisponibilidade do broker/mail.
- Exemplo: status FAILED sem caminho automático de retorno a QUEUED.
- Prioridade: P1. Esforço: médio. Dependências: FIN-001, observabilidade e operação.

## PERF-001 — Listagens sem paginação e N+1/in-memory filtering

- Área/componente: Performance/escalabilidade; VendaService, VendaController, FinanceiroOperacionalController e RelatorioOperacionalService.
- Estado: confirmado no código.
- Problema: endpoints carregam listas inteiras, filtram histórico em memória e inicializam relações; financeiro busca dados relacionados por linha; relatórios agregam todos os registros na JVM.
- Conceito/causa: API orientada a coleção completa e ausência de projeções/limites.
- Impacto técnico: latência, memória, GC e round-trips crescentes; risco de indisponibilidade com tenant grande.
- Impacto operacional/negócio: PDV/relatórios lentos e custo de infraestrutura imprevisível.
- Criticidade: alta. Probabilidade: média. Risco: alto.
- Recomendação: Pageable com limite máximo; filtros no SQL; projections/queries agregadas; EntityGraph/fetch join/batch; cursor para exportações; medir P95/P99 e volume.
- Exemplo: historico deve paginar e filtrar status/query no banco antes de carregar vendas e relações.
- Prioridade: P1. Esforço: alto. Dependências: contrato API e índices DB.

## RES-001 — UI usa dados mock após falha de API

- Área/componente: Confiabilidade/UX; storefront, dashboard, checkout, PDV, relatórios e entity-list.
- Estado: confirmado.
- Problema: vários catchError retornam MOCK_PRODUCTS, MOCK_CLIENTS, MOCK_REPORT ou linhas mock; algumas telas continuam interativas.
- Conceito/causa: fallback de demonstração não separado por build/profile.
- Impacto técnico: estado local pode divergir do servidor; usuário não distingue indisponível de dado real.
- Impacto operacional/negócio: venda, estoque e decisão financeira baseados em informação fictícia.
- Criticidade: alta em operações; média em vitrine. Probabilidade: alta em qualquer falha de API. Risco: alto.
- Recomendação: remover mocks de produção; mostrar estado offline/stale explícito; bloquear escrita quando fonte não estiver confirmada; manter demo em profile separado.
- Exemplo: erro no products() do PDV substitui o catálogo por dados fixos.
- Prioridade: P0 para PDV/financeiro; P1 para dashboards. Esforço: médio. Dependências: política de offline e testes E2E.

## OBS-001 — Falha de auditoria é engolida e observabilidade é incompleta

- Área/componente: Auditoria/operação; AuditLoggingFilter, AuditTrailService e Actuator.
- Estado: confirmado.
- Problema: AuditLoggingFilter captura RuntimeException da auditoria e segue a operação; há health/métricas básicas, mas não há evidência de alertas, SLO, tracing distribuído, dashboards ou retenção.
- Conceito/causa: auditoria tratada como best effort e observabilidade apenas técnica.
- Impacto técnico: operações podem persistir sem trilha; incidentes ficam sem correlação e sem alerta de negócio.
- Impacto operacional/negócio: falha de compliance e diagnóstico lento de venda/estoque/financeiro.
- Criticidade: alta para auditoria; média para disponibilidade. Probabilidade: média. Risco: alto.
- Recomendação: separar transação de negócio e auditoria com fila/outbox durável ou política fail-closed para ações críticas; alarmar falhas; definir métricas de PDV, fila, estoque e erro; padronizar logs estruturados e tracing.
- Exemplo: erro ao salvar AuditLog não deve ser silenciosamente descartado em cancelamento ou ajuste de estoque.
- Prioridade: P1. Esforço: alto. Dependências: desenho de retenção e operação.

## DR-001 — Backup e restore não comprovados

- Área/componente: Continuidade; Docker/infra/docs.
- Estado: não verificável por evidência operacional; ausência de automação versionada foi observada.
- Problema: há checklist recomendando backup diário e restore testado, mas não há script/pipeline/evidência de restauração cronometrada ou RPO/RTO executados.
- Conceito/causa: requisito operacional ainda documental.
- Impacto técnico: desconhecimento da recuperação após corrupção, exclusão ou falha de banco.
- Impacto operacional/negócio: perda de vendas, estoque, auditoria e dados pessoais.
- Criticidade: crítica. Probabilidade: não estimada; impacto alto. Risco: alto por desconhecimento.
- Recomendação: backup automático criptografado, retenção, cópia fora do host, teste periódico de restore em ambiente isolado, RPO/RTO medidos e runbook.
- Exemplo: evidenciar restauração de uma base MySQL com validação de migrations e contagem de registros.
- Prioridade: P0. Esforço: médio/alto. Dependências: infraestrutura de produção e LGPD.

## DEVOPS-001 — Permissões CI globais e force push

- Área/componente: Supply chain/CI; .github/workflows/main.yml.
- Estado: confirmado.
- Problema: workflow declara contents: write globalmente, embora vários jobs só leiam; publicação de Pages usa git push --force.
- Conceito/causa: privilégio no nível do workflow em vez de job; publicação mutável.
- Impacto técnico: código executado em jobs pode receber token de escrita desnecessário; force push pode apagar histórico de gh-pages.
- Impacto operacional/negócio: maior blast radius de dependência comprometida ou script malicioso; perda de artefatos/publicação.
- Criticidade: alta. Probabilidade: média. Risco: alto.
- Recomendação: permissions: read-all global e write apenas em jobs necessários; environments/protections; publicar por action com escopo mínimo; remover force quando possível e validar branch.
- Exemplo: jobs de lint/teste não devem herdar contents: write.
- Prioridade: P1. Esforço: baixo. Dependências: desenho do pipeline.

## QUALITY-001 — Qualidade automatizada incompleta no estado auditado

- Área/componente: QA/CI.
- Estado: confirmado.
- Problema: lint frontend falha com 6 no-unused-vars; npm audit falha com 8 vulnerabilidades (5 moderadas, 3 altas); audit-spring-data-jpa falha com 7 @Query; backend Maven não foi executado localmente por ausência de JDK válido; não há plugin JaCoCo/PIT no pom.
- Conceito/causa: gates parciais e ambiente não reprodutível para a auditoria.
- Impacto técnico: regressões e vulnerabilidades podem chegar ao pipeline; cobertura real desconhecida.
- Impacto operacional/negócio: release sem evidência suficiente para fluxos financeiros críticos.
- Criticidade: alta. Probabilidade: alta enquanto gates estiverem vermelhos. Risco: alto.
- Recomendação: corrigir lint; tratar Angular com upgrade planejado; revisar regra JPA para whitelisting justificado; fixar toolchain Java; publicar cobertura e mutation/contract tests; adicionar segurança HTTP, concorrência e carga.
- Exemplo: @Query com @Lock pode ser permitido por regra documentada, sem desligar o gate inteiro.
- Prioridade: P1. Esforço: médio/alto. Dependências: CI, JDK e política de dependências.

## API-001 — Contrato HTTP e tratamento de erros inconsistentes

- Área/componente: API; ApiExceptionHandler e ausência de OpenAPI/Springdoc encontrado.
- Estado: confirmado.
- Problema: RuntimeException de negócio é convertida genericamente em 400; falhas de infraestrutura podem ser classificadas como erro do cliente. Não há contrato OpenAPI detectado.
- Conceito/causa: handler amplo e documentação contratual ausente.
- Impacto técnico: observabilidade e clientes não distinguem validação, conflito, não encontrado, indisponibilidade e erro interno.
- Impacto operacional/negócio: retries incorretos, suporte mais lento e integração frágil.
- Criticidade: média. Probabilidade: alta. Risco: médio/alto.
- Recomendação: Problem Details por classe de erro, status 404/409/422/500/503, correlation ID e contrato OpenAPI versionado.
- Exemplo: conflito de idempotência deve ser 409, não 400 genérico.
- Prioridade: P1. Esforço: médio. Dependências: consumidores frontend e testes de contrato.
