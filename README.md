# Apex Gestor 4.0

Sistema hibrido de ERP, PDV, e-commerce e compliance financeiro para empresas de comercio em geral. A arquitetura atual usa Angular/Ionic para web e mobile, Electron para desktop e Spring Boot/MySQL no backend, com governanca v4 para monorepo enterprise, Flyway, Envers, RabbitMQ opcional, CI/CD e gates SecDevOps.

## Plataformas

- Web Cliente: e-commerce Angular/Ionic para consumidor final.
- Mobile Cliente: APK Android Ionic/Capacitor para compras, carrinho e rastreio.
- Mobile Empresa: APK Android Ionic/Capacitor para funcionarios, estoque, vendas rapidas e indicadores.
- Desktop Empresa: Electron para PDV, caixa e administracao pesada.
- Backend: Spring Boot com JWT, BCrypt, RBAC, LGPD, licenciamento e API REST.

## Documentacao

- [Documentacao Apex Gestor 3.0](docs/APEX_GESTOR_3_0_DOCUMENTACAO.md)
- [Roadmap de Producao](docs/ROADMAP_PRODUCAO.md)
- [Guia GitHub Releases](docs/GUIA_GITHUB_RELEASES.md)
- [Guia de Execucao e Testes pelo GitHub](docs/GUIA_EXECUCAO_TESTES_GITHUB.md)
- [Runbook de Orquestracao e Resiliencia](docs/RUNBOOK_ORQUESTRACAO_APEX.md)
- [Guia de Seguranca](SECURITY.md)
- [Guia de Banco de Dados Local](docs/GUIA_BANCO_DADOS_LOCAL.md)
- [Guia de Escalabilidade do Banco](docs/GUIA_ESCALABILIDADE_BANCO.md)
- [Modulo Financeiro AdmCalc](docs/MODULO_FINANCEIRO_ADMCALC.md)
- [Arquitetura Enterprise v4](docs/ARQUITETURA_ENTERPRISE_V4.md)
- [Repository Spring Data JPA v4](docs/REPOSITORY_SPRING_DATA_JPA_V4.md)
- [DevEx Bootstrap Windows](docs/DEVEX_BOOTSTRAP_WINDOWS.md)

## Ambiente completo com Docker

```bash
docker compose up -d --build
```

Depois abra:

- Web: `http://localhost:4200`
- API: `http://localhost:8080/actuator/health`
- MySQL: `localhost:2705`
- RabbitMQ Management: `http://localhost:15672`

Licencas demo para desenvolvimento:

- `APEX-DEMO-ALL`: libera todos os apps.
- `APEX-DEMO-DESKTOP`: libera somente Desktop.
- `APEX-DEMO-EMPRESA`: libera somente Mobile Empresa.
- `APEX-DEMO-CLIENTE`: libera somente Mobile Cliente.
- `APEX-DEMO-WEB`: libera somente Web Cliente.
- `APEX-DEMO-DUO`: libera Desktop + Mobile Empresa.
- `APEX-DEMO-TRIO`: libera Desktop + Mobile Empresa + Web Cliente.

Em producao, configure `APEX_LICENSE_CATALOG` no backend no formato `CHAVE|apps|limite_dispositivos|validade_dias`. Exemplo: `CLIENTE-001|desktop+mobile-staff|2|365`.

## Modulo financeiro AdmCalc

O Apex possui a tela `/finance` para calculos trabalhistas, tributarios, indicadores do AdmCalc, auditoria antifraude e workflow de documentos assinados. O backend expoe os endpoints protegidos em `/api/financeiro/**` e registra cada operacao nas tabelas `financial_calculation`, `financial_audit_event`, `financial_digital_document`, `financial_document_outbox` e nas tabelas Envers `_AUD`.

Na v4, alteracoes de schema ficam em Flyway: `Apex-Gestordemo/src/main/resources/db/migration`.

## Desenvolvimento manual

```bash
cd Apex_Gestor
npm ci --legacy-peer-deps
npm run start:web
npm run build:web
npm run test -- --watch=false
```

Backend:

```bash
docker compose up -d apex-mysql
cd Apex-Gestordemo
./mvnw spring-boot:run
```

O MySQL local sobe na porta `2705`, banco `apex_db`, usuario `root` e senha de desenvolvimento `apex_dev_2026`. Para diagnostico de conexao e variaveis de ambiente, veja [Guia de Banco de Dados Local](docs/GUIA_BANCO_DADOS_LOCAL.md).

Validacao backend:

```bash
cd Apex-Gestordemo
./mvnw test
./mvnw -Dtest=PdvVendaEstoqueEnversIntegrationTests test
./mvnw -DskipTests package
```

## Releases automatizadas

O workflow `.github/workflows/main.yml` compila e publica:

- App Site Web (`.zip` com a pasta `App Site Web`)
- App Desktop (`.zip` com a pasta `App Desktop` e instalador Windows `.exe`)
- App Mobile Empresa (`.zip` com a pasta `App Mobile Empresa` e APK Android)
- App Mobile Cliente (`.zip` com a pasta `App Mobile Cliente` e APK Android)

Push em `main` gera artifacts na aba Actions. Tags `v*`, como `v4.0.0`, publicam os arquivos na aba Releases.
