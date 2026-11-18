# 01 — Escopo e inventário

## Componentes encontrados

| Componente | Evidência | Leitura |
|---|---|---|
| Backend | Apex-Gestordemo, 263 arquivos Java, Spring Boot, JPA, Flyway, Envers, Actuator | Monólito modular em camadas |
| Frontend | Apex_Gestor, Angular 19, Ionic 8, Capacitor 8, Electron 42 | Cliente web, desktop e móvel |
| Banco | MySQL 8.4 em Docker Compose; H2/Testcontainers nos testes | OLTP relacional |
| CI/CD | .github/workflows/main.yml e backend-ci.yml | Build, testes, artefatos, Pages e releases |
| Documentação | docs/, README.md, SECURITY.md, runbooks e roadmaps | Intenção de produto e operação mais ampla que a implementação atual |
| Módulos vazios/transicionais | apps/ e packages/shared | Não são fonte funcional principal no commit auditado |

## Funcionalidades identificadas

Cadastros de produtos, clientes, fornecedores, usuários, categorias, grupos, formas e condições de pagamento; PDV; caixa; vendas; devoluções; estoque; entrada de nota fiscal; contas a receber/pagar; relatórios; privacidade/LGPD; auditoria; licenciamento; documentos financeiros com workflow; frontend e artefatos desktop/mobile.

## Arquitetura observada

O backend é um monólito modular com Controllers, Services, Repositories, Models e DTOs. O frontend usa rotas lazy-loaded e serviços HTTP. Há state machine para documentos financeiros e outbox para entrega, mas não há evidência de CQRS amplo, event sourcing, GraphQL, gRPC, SSE ou WebSockets.

Essa escolha é adequada para a etapa v1, desde que sejam reforçados os limites de tenant, a consistência transacional e a operação. A complexidade de um ecossistema distribuído não deve ser adicionada antes de fechar esses fundamentos.

## Dívidas arquiteturais relevantes

1. O contexto multiempresa existe em muitas entidades e filtros, mas não é uniforme: documentos financeiros, cálculos e operações de privacidade usam buscas globais em pontos sensíveis.
2. A autenticação persistida e segura existe em AuthService, porém AuthController usa outro caminho de demonstração.
3. Flyway V2–V5 existe em duas variantes, mas as entidades atuais incluem tabelas sem migration correspondente.
4. Há mistura de injeção por campo e construtor, controllers em uma linha e regras de autorização distribuídas entre SecurityConfig, anotações e serviços manuais.
5. A camada web mascara indisponibilidade com dados mock em várias telas, incompatível com operações financeiras, estoque e PDV.

## Documentação divergente

docs/ARQUITETURA_ENTERPRISE_V4.md cita V1__apex_schema_baseline.sql e V4__enterprise_compliance_audit_outbox.sql. Esses arquivos não existem no conjunto de migrations encontrado; existem somente V2, V3, V4 e V5, cada uma em migration e migration-mysql. Isso reduz a confiabilidade do runbook de implantação.
