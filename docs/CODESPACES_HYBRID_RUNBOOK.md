# Apex Gestor v4.0 - Runbook CDE, Prebuilds e Execucao Hibrida

Este runbook padroniza o uso do GitHub Codespaces como Cloud Development Environment do Apex Gestor. A topologia esperada e:

- Codespace: banco, RabbitMQ e backend Spring Boot.
- Host local do desenvolvedor: Electron PDV renderizado nativamente.
- Ponte: URL encaminhada pelo Codespaces ou tunel privado via GitHub CLI.

## 1. Criar o Codespace

1. Abra o repositorio no GitHub.
2. Clique em `Code`.
3. Abra a aba `Codespaces`.
4. Clique em `Create codespace on main`.
5. Aguarde o VS Code Web abrir e concluir o devcontainer.

O devcontainer instala Java 21, Node 22, Maven, Gradle, Docker-in-Docker, GitHub CLI, Angular CLI, Ionic CLI e extensoes essenciais de VS Code.

Nota: a stack atual do Apex Gestor usa MySQL, Flyway e Hibernate Envers. O container tambem inclui cliente PostgreSQL para uma futura migracao, mas a orquestracao ativa segue o `docker-compose.yml` existente com MySQL na porta `2705`.

## 2. Configurar Prebuilds no GitHub

1. No GitHub, abra `Settings`.
2. Entre em `Codespaces`.
3. Em `Prebuild configurations`, clique em `Set up prebuild`.
4. Selecione a branch `main`.
5. Mantenha o devcontainer `.devcontainer/devcontainer.json`.
6. Ative atualizacao automatica em push.

O prebuild executa:

```bash
bash .devcontainer/scripts/prebuild.sh
```

Esse processo aquece `npm`, Maven, imagens Docker de MySQL/RabbitMQ e cache da imagem do backend.

## 3. Subir Banco, Mensageria e Backend no Codespace

No terminal do Codespace:

```bash
bash scripts/start-hybrid-env.sh
```

Para tambem subir o container web Angular:

```bash
bash scripts/start-hybrid-env.sh --web
```

Valide a API no Codespace:

```bash
curl http://127.0.0.1:8080/actuator/health
```

Resultado esperado:

```json
{"status":"UP"}
```

## 4. Conectar o Electron Local ao Backend na Nuvem

### Opcao A - URL publica do Codespaces para QA rapido

1. No Codespace, abra a aba `Ports`.
2. Confirme que a porta `8080` esta publica.
3. Copie a URL da porta `8080`, no formato:

```text
https://<codespace-name>-8080.<domain>.app.github.dev
```

4. Na sua maquina local, dentro do clone do projeto:

Windows PowerShell:

```powershell
cd Apex_Gestor
Copy-Item .env.local.example .env.local
notepad .env.local
```

Linux/macOS:

```bash
cd Apex_Gestor
cp .env.local.example .env.local
${EDITOR:-nano} .env.local
```

5. Edite `Apex_Gestor/.env.local`:

```env
APEX_API_BASE_URL=https://<codespace-name>-8080.<domain>.app.github.dev
ELECTRON_START_URL=http://localhost:4200
```

6. Instale dependencias e abra o PDV:

```bash
npm ci --legacy-peer-deps
npm run start:electron
```

### Opcao B - Tunel privado via GitHub CLI

Use esta opcao quando a porta `8080` deve permanecer privada.

1. Na sua maquina local, autentique o GitHub CLI:

```bash
gh auth login
```

2. Liste os Codespaces:

```bash
gh codespace list
```

3. Abra o tunel local:

```bash
gh codespace ports forward 8080:8080 -c <codespace-name>
```

4. Configure `Apex_Gestor/.env.local` local:

```env
APEX_API_BASE_URL=http://localhost:8080
ELECTRON_START_URL=http://localhost:4200
```

5. Em outro terminal local:

```bash
cd Apex_Gestor
npm run start:electron
```

## 5. Teste E2E Basico

1. Confirme que o Codespace esta com a API `UP`.
2. Abra o Electron local.
3. Na tela de configuracoes do Apex, confirme que a URL base da API e a mesma do `.env.local`.
4. Execute login/licenca.
5. Abra PDV.
6. Registre uma venda de teste.
7. No Codespace, valide logs do backend:

```bash
docker compose logs -f apex-backend
```

8. Valide banco:

```bash
docker exec -it apex-gestor-mysql mysql -uroot -papex_dev_2026 apex_db
```

## 6. Comandos de Operacao

Subir stack hibrida:

```bash
npm run codespaces:start-hybrid
```

Preaquecer dependencias manualmente:

```bash
npm run codespaces:prebuild
```

Recriar containers:

```bash
docker compose down
bash scripts/start-hybrid-env.sh
```

Ver saude dos containers:

```bash
docker compose ps
```

Ver logs:

```bash
docker compose logs -f apex-mysql apex-rabbitmq apex-backend
```

## 7. Criterios de Pronto

- `docker compose ps` mostra MySQL e RabbitMQ `healthy`.
- `curl http://127.0.0.1:8080/actuator/health` retorna `UP`.
- Porta `8080` esta publica para QA rapido ou encaminhada por `gh codespace ports forward`.
- `Apex_Gestor/.env.local` existe no host local e aponta para a API correta.
- `npm run start:electron` abre o PDV local sem erro de `Connection refused`.
