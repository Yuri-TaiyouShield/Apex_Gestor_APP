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
- [Guia de Seguranca](SECURITY.md)

## Desenvolvimento local

```bash
cd Apex_Gestor
npm install
npm run start:web
npm run build:web
npm run test -- --watch=false
```

Backend:

```bash
cd Apex-Gestordemo
./mvnw test
./mvnw -DskipTests package
```

## Releases automatizadas

O workflow `.github/workflows/main.yml` compila e publica:

- Web Cliente (`.zip`)
- Desktop Windows Electron (`.exe`)
- Mobile Empresa Android (`.apk`)
- Mobile Cliente Android (`.apk`)

Push em `main` gera artifacts na aba Actions. Tags `v*`, como `v3.0.0`, publicam os arquivos na aba Releases.
