# 02 — Requisitos e arquitetura

## Matriz de requisitos e estado

| Área | Estado auditado | Evidência | Classificação |
|---|---|---|---|
| Operação v1 | PDV, caixa, vendas, devolução, estoque, cadastros e financeiro básico implementados | Controllers, services, frontend e docs de homologação | Parcialmente implementado |
| Multiempresa | Filtros de empresa/filial presentes em muitos fluxos | TenantScopeService, repositories e migrations | Preparado, não fechado |
| RBAC | Perfis e guards existem | ApexPermission, roleGuard, AuthorizationService | Risco alto de bypass no runtime atual |
| Auditoria | AuditTrailService, AuditLog e filtros HTTP existem | AuditTrailService e AuditLoggingFilter | Implementado com falhas de disponibilidade/escopo |
| Soft delete/LGPD | Anonimização e solicitações do titular existem | DireitosTitularService e PrivacyAdminController | Parcial; buscas globais e governança ainda abertas |
| Fiscal brasileiro | Entrada interna de NF existe | NotaFiscalController e NotaFiscalService | Não equivale a emissão NF-e/NFC-e/SAT, XML, protocolo ou contingência |
| Assinatura digital | Hash de assinatura é calculado | FinancialDocumentWorkflowService | Não comprovada como assinatura PKI/legal |
| Backup/restore | Checklist e intenção documental | docs/GO_LIVE_CHECKLIST_BR.md | Não verificado; não há evidência operacional de restore |
| Observabilidade | Health, métricas básicas e trace ID | Actuator e RequestTraceFilter | Sem SLO, alertas, tracing distribuído e dashboards evidenciados |
| Entrega assíncrona | Outbox e RabbitMQ opcional | FinancialDocumentDeliveryService | Sem retry/DLQ/reprocessamento operacional completo |
| Testabilidade | 18 classes de teste backend e 11 testes frontend executados | src/test e Karma | Bom começo; cobertura e testes de segurança/concor­rência insuficientes |

## Requisitos não fechados antes de uso comercial

- estratégia fiscal por UF e homologação por contador;
- emissão/cancelamento/consulta de NF-e ou NFC-e, certificado, XML, protocolo e contingência;
- política de backup criptografado, RPO/RTO e restore cronometrado;
- matriz de suporte, onboarding, contrato e rollback;
- segurança de produção com autenticação obrigatória, segredo JWT gerenciado e armazenamento de sessão seguro;
- dados de tenant obrigatórios, FKs, índices e invariantes no banco;
- testes de carga, concorrência, volume, recuperação e autorização HTTP;
- eliminação de mocks de sucesso quando a API está indisponível.

## Avaliação arquitetural

### Pontos positivos

- Monólito modular reduz custo operacional e é compatível com uma v1.
- Separação de DTOs, serviços e repositórios facilita evolução.
- Venda só movimenta estoque no recebimento pelo caixa, conforme regra operacional documentada.
- Outbox transacional reduz risco de dual-write no enfileiramento.
- Envers e trilha de auditoria oferecem base para histórico.
- Build seguro do frontend gera manifesto de integridade.

### Pontos de atenção

- O limite de tenant deve ser obrigatório e centralizado, não uma convenção de cada service.
- Módulos financeiros e de documentos precisam de tenant_id e consultas escopadas.
- Regras de autorização devem ter uma única fonte de verdade e testes de 401/403.
- O banco precisa ser a segunda linha de defesa com constraints e índices únicos.
- O caminho de entrega precisa ser explicitamente idempotente, reprocessável e observável.
