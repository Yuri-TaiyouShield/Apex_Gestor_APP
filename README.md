# Apex Gestor 3.0

Sistema hibrido de ERP, PDV e e-commerce multi-nicho para empresas de comercio em geral. A arquitetura atual usa Angular/Ionic para web e mobile, Electron para desktop e Spring Boot/MySQL no backend.

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

## Ambiente completo com Docker

```bash
docker compose up -d --build
```

Depois abra:

- Web: `http://localhost:4200`
- API: `http://localhost:8080/actuator/health`
- MySQL: `localhost:2705`

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
./mvnw -DskipTests package
```

## Releases automatizadas

O workflow `.github/workflows/main.yml` compila e publica:

- App Site Web (`.zip` com a pasta `App Site Web`)
- App Desktop (`.zip` com a pasta `App Desktop` e instalador Windows `.exe`)
- App Mobile Empresa (`.zip` com a pasta `App Mobile Empresa` e APK Android)
- App Mobile Cliente (`.zip` com a pasta `App Mobile Cliente` e APK Android)

Push em `main` gera artifacts na aba Actions. Tags `v*`, como `v3.1.0`, publicam os arquivos na aba Releases.
