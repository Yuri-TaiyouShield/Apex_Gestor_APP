# Guia de Execucao e Testes pelo GitHub

Este guia mostra como testar o Apex Gestor pelo GitHub Codespaces e como baixar os executaveis gerados automaticamente.

## 1. Abrir o projeto no GitHub Codespaces

1. Acesse o repositorio:

```text
https://github.com/Yuri-TaiyouShield/Apex_Gestor_APP
```

2. Clique em `Code > Codespaces > Create codespace on main`.
3. Aguarde o ambiente abrir.

## 2. Subir o banco MySQL

No terminal do Codespaces:

```bash
docker compose up -d apex-mysql
docker compose ps
```

O banco sobe com:

- Host interno: `localhost`
- Porta: `2705`
- Database: `apex_db`
- Usuario: `root`
- Senha de desenvolvimento: `apex_dev_2026`

## 3. Rodar a API Spring Boot

No terminal do Codespaces:

```bash
cd Apex-Gestordemo
chmod +x mvnw
./mvnw spring-boot:run
```

Se a porta `8080` estiver ocupada por Tomcat, GlassFish ou outro processo, rode em outra porta:

```bash
SERVER_PORT=8082 ./mvnw spring-boot:run
```

Observacao para teste local: o backend Spring Boot ja usa Tomcat embutido. Tomcat ou GlassFish instalados na maquina nao sao obrigatorios para rodar este projeto.

Abra a aba `Ports` do Codespaces e exponha a porta da API (`8080` ou `8082`). Para testar:

```bash
curl https://SEU-CODESPACE-8080.app.github.dev/api/produtos
curl https://SEU-CODESPACE-8080.app.github.dev/api/relatorios/financeiro
```

Substitua `SEU-CODESPACE-8080.app.github.dev` pela URL real mostrada na aba `Ports`.

## 4. Rodar o frontend web no Codespaces

Abra outro terminal:

```bash
cd Apex_Gestor
npm install
npm run start:web
```

Abra a porta `4200` na aba `Ports`. A URL correta do frontend fica parecida com:

```text
https://SEU-CODESPACE-4200.app.github.dev/
```

Importante: a porta `8080` e a API. A tela do app web fica na porta `4200`.

## 5. Baixar executaveis na Release

Abra:

```text
https://github.com/Yuri-TaiyouShield/Apex_Gestor_APP/releases/latest
```

Baixe os pacotes:

- `apex-gestor-app-site-web-vX.Y.Z.zip`
- `apex-gestor-app-desktop-vX.Y.Z.zip`
- `apex-gestor-app-mobile-empresa-vX.Y.Z.zip`
- `apex-gestor-app-mobile-cliente-vX.Y.Z.zip`

Cada `.zip` contem uma pasta identificada:

- `App Site Web`
- `App Desktop`
- `App Mobile Empresa`
- `App Mobile Cliente`

## 6. Testar cada pacote

### App Desktop

1. Extraia `apex-gestor-app-desktop-vX.Y.Z.zip`.
2. Abra a pasta `App Desktop`.
3. Execute o instalador `.exe`.
4. Depois de instalado, abra o Apex Gestor Desktop.

### App Mobile Empresa

1. Extraia `apex-gestor-app-mobile-empresa-vX.Y.Z.zip`.
2. Abra a pasta `App Mobile Empresa`.
3. Envie o `.apk` para um Android ou emulador.
4. Instale o APK e abra o app.

### App Mobile Cliente

1. Extraia `apex-gestor-app-mobile-cliente-vX.Y.Z.zip`.
2. Abra a pasta `App Mobile Cliente`.
3. Envie o `.apk` para um Android ou emulador.
4. Instale o APK e abra o app.

### App Site Web

1. Extraia `apex-gestor-app-site-web-vX.Y.Z.zip`.
2. Publique a pasta `App Site Web` em um servidor estatico.
3. Para teste rapido no Codespaces, prefira rodar `npm run start:web` e abrir a porta `4200`.

## 7. Validacao recomendada

Antes de criar uma nova tag:

```bash
cd Apex-Gestordemo
./mvnw test
./mvnw -DskipTests package

cd ../Apex_Gestor
npm audit --omit=dev --audit-level=high
npm run build:web
npm run test -- --watch=false --browsers=ChromeHeadless
```

Ao criar uma nova tag `v*`, o GitHub Actions gera os pacotes e publica a Release automaticamente.
