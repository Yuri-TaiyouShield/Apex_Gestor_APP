# 05 — Síntese, plano e roadmap

## Nota por categoria

| Categoria | Nota /10 | Justificativa curta |
|---|---:|---|
| Requisitos e produto | 5,0 | Documentação rica, mas requisitos fiscais/aceitação ainda abertos |
| Arquitetura | 7,0 | Monólito modular adequado; fronteiras de tenant incompletas |
| Qualidade de código | 5,5 | Camadas claras, mas mistura de DI, duplicidade de caminhos e lint vermelho |
| Segurança | 2,0 | Credenciais demo, auth false e headers confiados são bloqueadores |
| Dados e migrations | 4,0 | V2–V5 existem, mas entidades/schema e constraints não fecham |
| Regras e integridade | 4,5 | Fluxos úteis, porém concorrência/idempotência abertas |
| Performance | 4,0 | Listas completas, filtros em memória e N+1 |
| Escalabilidade | 4,0 | OLTP único e outbox sem ciclo operacional completo |
| Testes e validação | 4,5 | Frontend unitário positivo; backend/E2E/carga/segurança incompletos |
| Observabilidade | 3,5 | Health/trace existem; falta operação orientada por SLI/SLO |
| Resiliência e DR | 2,5 | Backup/restore não comprovados e falhas de entrega sem reprocessamento |
| DevOps e release | 5,0 | Actions pinadas e artefatos; permissões excessivas |
| UX e prontidão operacional | 5,5 | Muitas rotas funcionais, mas mock em falha confunde a operação |

Nota global indicativa: 4,4/10. A nota não substitui os bloqueadores P0.

## Matriz de risco

| Probabilidade \ impacto | Alto/crítico | Médio | Baixo |
|---|---|---|---|
| Alta | SEC-001, SEC-004, MIG-001, CONC-001, RES-001 | QUALITY-001, PERF-001 | — |
| Média | SEC-006, CONC-002, IDEMP-001, FIN-001, DR-001 | SEC-002, SEC-003, MSG-001, OBS-001, DEVOPS-001 | — |
| Baixa/não verificada | FISC-001 | API-001, SEC-005 | — |

DR-001 está na matriz por impacto alto e ausência de evidência de recuperação; a probabilidade de desastre não foi estimada. FISC-001 é bloqueador de lançamento nacional, embora o endpoint operacional de entrada exista.

## Plano P0 — antes de qualquer produção externa

1. Remover AuthController demo do runtime e conectar login ao AuthService.
2. Ativar autenticação obrigatória nos manifests e remover confiança em headers de cliente.
3. Validar segredo JWT obrigatório, issuer/audience, rotação e startup fail-fast.
4. Fechar method security/AuthorizationService e testar 401/403 por rota e role.
5. Aplicar tenant obrigatório a documentos, cálculos, auditoria e privacidade; eliminar buscas globais em escopo sensível.
6. Completar migrations, fazer backfill auditado, trocar produção para ddl-auto=validate e validar banco vazio/upgrade.
7. Corrigir concorrência de estoque/caixa com lock/version e constraints.
8. Implementar idempotência para finalização de venda, conta a receber e entrada de NF.
9. Corrigir estado SENT falso da outbox e criar backup/restore testado.
10. Retirar mocks de sucesso do PDV, checkout, estoque e financeiro em builds de produção.

## Plano P1 — piloto controlado

1. Corrigir lint e o gate de @Query com allowlist documentada.
2. Tratar as vulnerabilidades Angular com upgrade planejado, testes de regressão e revisão de breaking changes.
3. Adicionar retry/backoff/DLQ/reprocessamento e métricas de outbox.
4. Paginar e filtrar no banco; remover N+1; medir P95/P99 por operação.
5. Publicar OpenAPI e padronizar Problem Details/409/422/500/503.
6. Definir LGPD: retenção, acesso, exportação, anonimização, auditoria e base legal.
7. Reduzir permissões CI por job e eliminar force push quando possível.
8. Configurar logs estruturados, alertas, dashboards, retenção e correlação.

## Plano P2 — release Brasil

1. Definir provedor/estratégia fiscal por UF.
2. Implementar e homologar NF-e/NFC-e, XML, certificado, assinatura PKI, protocolo, cancelamento e contingência.
3. Validar integração contábil/SPED conforme escopo comercial.
4. Executar UAT com lojas reais, contador e suporte.
5. Assinar instaladores/release, fechar canais de atualização e plano de rollback.

## Roadmap recomendado

### Semana 0–1 — contenção

Não expor os manifests atuais; aplicar SEC-001 a SEC-004; congelar promessa fiscal; fixar ambiente Java/Docker para validação reproduzível.

### Semana 2–4 — integridade

Migrations completas, backfill, constraints, locks, idempotência, autorização cross-tenant, status de entrega correto e testes negativos/concor­rentes.

### Semana 5–8 — piloto

Paginação, observabilidade, backup/restore, E2E real com MySQL, contrato API, sem mocks em operações críticas, suporte e runbooks.

### Depois do piloto — nacional

Fiscal homologado, LGPD revisada, carga/volume, release assinado, RPO/RTO medido e aceite formal de produto/contador/operação.

## Conclusão

O Apex Gestor é uma base técnica promissora para uma v1 operacional controlada. A decisão responsável é manter o posicionamento de piloto/homologação e bloquear lançamento comercial ou fiscal até os P0 serem demonstrados por código, migration, testes e evidência operacional. 
