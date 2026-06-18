# Apex Gestor

Sistema de gestao para lojas de materiais de construcao, com backend Java/Spring Boot, banco MySQL e artefatos de frontend/mobile/desktop baseados em Angular/Ionic/Capacitor/Electron.

Este repositorio esta sendo organizado para uma evolucao de produto comercial no Brasil. A meta e sair de uma base de demonstracao para uma plataforma confiavel de operacao, suporte, auditoria, fiscal, financeiro, estoque, vendas e gestao.

## Estado Executivo

Status em 2026-05-26: **nao aprovado para lancamento oficial nacional**.

O backend versionado possui modulos essenciais de cadastro, estoque, vendas, nota fiscal de entrada, despesas e relatorios. Ele compila e os testes atuais passam. Ainda existem bloqueios de produto e governanca antes de vender como software oficial:

- o frontend `Apex_Gestor/` aparece no checkout como diretorio nao versionado e sem fonte raiz evidente;
- autenticacao real, multiempresa, auditoria de usuario e trilhas LGPD ainda precisam virar comportamento padrao de producao;
- emissao fiscal brasileira, NFC-e/NF-e/SAT quando aplicavel, SPED e integracoes contabeis ainda precisam de especificacao, homologacao e validacao por UF;
- CI/CD atual do repositorio estava focado em GitHub Pages, nao em qualidade de produto;
- faltam migrations formais de banco, rotina de backup/restore, observabilidade operacional e testes de fluxos criticos.

Leia primeiro:

- [Roadmap versionado ERP/PDV](docs/VERSIONED_EVOLUTION_ROADMAP.md)
- [Matriz RBAC, auditoria e operacoes criticas](docs/RBAC_AUDIT_MATRIX.md)
- [Rastreabilidade dos casos de uso](docs/USE_CASE_TRACEABILITY.md)
- [Plano CTO de lancamento Brasil](docs/CTO_LAUNCH_PLAN_BR.md)
- [Auditoria de prontidao 2026-05-26](docs/LAUNCH_READINESS_AUDIT_2026-05-26.md)
- [Checklist de go-live Brasil](docs/GO_LIVE_CHECKLIST_BR.md)
- [Arquitetura e operacao](docs/ARCHITECTURE_AND_OPERATIONS.md)
- [Como contribuir](CONTRIBUTING.md)

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
