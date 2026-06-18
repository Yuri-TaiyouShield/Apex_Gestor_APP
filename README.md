# Apex Gestor

Sistema de gestao para lojas de materiais de construcao, com backend Java/Spring Boot, banco MySQL e artefatos de frontend/mobile/desktop baseados em Angular/Ionic/Capacitor/Electron.

Este repositorio esta sendo organizado para uma evolucao de produto comercial no Brasil. A meta e sair de uma base de demonstracao para uma plataforma confiavel de operacao, suporte, auditoria, fiscal, financeiro, estoque, vendas e gestao.

## Estado Executivo

Status em 2026-06-18: **v1 operacional homologada localmente; ainda nao aprovada para lancamento fiscal nacional**.

O backend versionado possui modulos essenciais de cadastro, estoque, vendas, nota fiscal de entrada, despesas e relatorios. Ele compila e os testes atuais passam. Ainda existem bloqueios de produto e governanca antes de vender como software oficial:

- o frontend `Apex_Gestor/` possui fluxo operacional Vendedor -> Caixa, financeiro inicial e E2E Playwright;
- JWT, RBAC, tenant scope, auditoria e LGPD transversal estao protegidos por testes H2, MySQL/Testcontainers e navegador;
- emissao fiscal brasileira, NFC-e/NF-e/SAT quando aplicavel, SPED e integracoes contabeis ainda precisam de especificacao, homologacao e validacao por UF;
- CI/CD atual do repositorio estava focado em GitHub Pages, nao em qualidade de produto;
- migrations Flyway V2-V5 e variante MySQL estao validadas; backup/restore e observabilidade de producao continuam pendentes.

Leia primeiro:

- [Roadmap versionado ERP/PDV](docs/VERSIONED_EVOLUTION_ROADMAP.md)
- [Matriz RBAC, auditoria e operacoes criticas](docs/RBAC_AUDIT_MATRIX.md)
- [Rastreabilidade dos casos de uso](docs/USE_CASE_TRACEABILITY.md)
- [Plano CTO de lancamento Brasil](docs/CTO_LAUNCH_PLAN_BR.md)
- [Auditoria de prontidao 2026-05-26](docs/LAUNCH_READINESS_AUDIT_2026-05-26.md)
- [Checklist de go-live Brasil](docs/GO_LIVE_CHECKLIST_BR.md)
- [Arquitetura e operacao](docs/ARCHITECTURE_AND_OPERATIONS.md)
- [Como contribuir](CONTRIBUTING.md)
- [Homologacao operacional v1](docs/HOMOLOGACAO_V1.md)

## Estrutura

```text
.
├── Apex-Gestordemo/          # Backend Spring Boot
├── Apex_Gestor/              # Artefatos frontend/mobile/desktop no checkout atual
├── docs/                     # Documentacao executiva, tecnica e operacional
├── tools/audit/              # Scripts de auditoria local
├── docker-compose.yml        # MySQL + backend API para desenvolvimento
└── .github/workflows/        # CI e workflows GitHub
```

## Backend

Principais dominios versionados:

- produtos, categorias, fornecedores e enderecos;
- clientes e usuarios;
- vendas, itens de venda e pagamentos;
- nota fiscal de entrada e custo medio;
- despesas, tipos de despesa e relatorio financeiro;
- seguranca configuravel por ambiente;
- auditoria automatizada basica em PowerShell.

### Rodar testes

```powershell
cd Apex-Gestordemo
cmd /c mvnw.cmd -q test
```

Com Docker Desktop ativo, o mesmo comando executa o fluxo isolado em MySQL 8.4 via Testcontainers. Sem Docker, somente essa classe e marcada como indisponivel.

### Frontend e E2E

```powershell
cd Apex_Gestor
npm test -- --watch=false --browsers=ChromeHeadless
npm run build:web
npm run e2e
```

O E2E usa Chrome instalado e espera frontend em `127.0.0.1:4200` e backend em `127.0.0.1:8080`.

### Auditoria local

```powershell
powershell -ExecutionPolicy Bypass -File tools\audit\apex-enterprise-audit.ps1 -SkipBuild
```

Para incluir build/test no script:

```powershell
powershell -ExecutionPolicy Bypass -File tools\audit\apex-enterprise-audit.ps1
```

## Ambiente Local

Crie um `.env` a partir de `.env.example` e ajuste as variaveis.

```powershell
copy .env.example .env
docker compose up --build
```

Endpoints principais:

- API: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- MySQL local: porta `2705` por padrao

## Decisoes de Produto

Para competir com ERPs estabelecidos de material de construcao, o Apex Gestor deve ser lancado por fases:

1. **v1-desktop-core**: PDV, caixa, financeiro basico, estoque, inventario, cadastros, RBAC, auditoria, soft delete e multiempresa preparado.
2. **v2-admCalc**: motor de calculos empresariais, precificacao, simulacoes, dashboard gerencial e alertas operacionais.
3. **v3-logistica-delivery**: delivery, entregadores, rotas, frete, status em tempo real e comprovante digital.
4. **Piloto pago controlado**: 3 a 5 lojas reais, suporte proximo, sem promessa nacional.
5. **Release Brasil v1 comercial**: fiscal, LGPD, backup, suporte, contrato, onboarding e estabilidade.

O objetivo nao e apenas copiar ADM Santri ou SoftCosmos. O objetivo e entregar profundidade de varejo de construcao com produto mais simples de implantar, mais moderno de usar e mais confiavel de operar.

## Padrao de Qualidade

Toda mudanca relevante deve passar por:

- testes automatizados do backend;
- auditoria local em `tools/audit`;
- revisao de seguranca para autenticacao, dados pessoais e permissao;
- atualizacao de documentacao quando houver mudanca de produto, operacao ou API;
- plano de rollback quando mexer em banco, pagamento, fiscal ou estoque.

## Licenca e Comercializacao

Antes do lancamento oficial, validar:

- licenca do codigo e dependencias;
- contratos de uso e suporte;
- politica de privacidade LGPD;
- termos de responsabilidade fiscal;
- estrategia de marca, dominio e canal de atendimento.
