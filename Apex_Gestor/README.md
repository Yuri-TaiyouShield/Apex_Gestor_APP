# Apex Gestor

Sistema híbrido de gestão, PDV e e-commerce com Angular, Ionic, Capacitor e Electron.

## Plataformas

- Web: Angular/Ionic em `http://localhost:4200`, com loja B2C e área interna.
- Desktop: Electron para PDV e administração pesada.
- Mobile: Ionic + Capacitor para staff e cliente.

## Requisitos

- Node.js 22.12 ou superior com `npm` no PATH.
- Java 21+ para o backend Spring Boot.
- MySQL com o banco `apex_db` na porta `2705`.
- Android SDK apenas para rodar `cap:run:android`.

## Scripts

```bash
npm install
npm run start:web
npm run build:web
npm run start:electron
npm run build:electron
npm run cap:sync
npm run cap:run:android
npm run test -- --watch=false
```

O desenvolvimento web usa `proxy.conf.json` para encaminhar `/api` ao backend em `http://localhost:8080`.

## Arquitetura

Veja [docs/architecture.md](docs/architecture.md) para a estrutura de pastas, principais entidades e fluxo de integração entre estoque e e-commerce.
