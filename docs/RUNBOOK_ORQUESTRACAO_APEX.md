# Runbook de Orquestracao e Resiliencia Apex Gestor

Este runbook valida a subida sincronizada do ecossistema Apex Gestor: MySQL, API Spring Boot e Web Angular/Ionic. Os apps Mobile e Desktop usam o mesmo cliente Angular e herdam a espera ativa da API antes das chamadas iniciais.

## 1. Subir tudo com um unico comando

Na raiz do repositorio:

```bash
docker compose up -d --build
```

Ordem garantida por healthchecks:

1. `apex-mysql` inicia primeiro e precisa responder ao `mysqladmin ping`.
2. `apex-backend` so inicia depois do banco saudavel.
3. `apex-web` so inicia depois do backend responder `UP` em `/actuator/health`.

Portas padrao:

- Banco MySQL: `localhost:2705`
- API Spring Boot: `http://localhost:8080`
- Web cliente/gestor: `http://localhost:4200`

Licencas demo disponiveis no Compose:

- `APEX-DEMO-ALL`: todos os apps.
- `APEX-DEMO-DESKTOP`: somente Desktop.
- `APEX-DEMO-EMPRESA`: somente Mobile Empresa.
- `APEX-DEMO-CLIENTE`: somente Mobile Cliente.
- `APEX-DEMO-WEB`: somente Web Cliente.
- `APEX-DEMO-DUO`: Desktop + Mobile Empresa.
- `APEX-DEMO-TRIO`: Desktop + Mobile Empresa + Web Cliente.

Para emitir licencas reais, defina `APEX_LICENSE_CATALOG` antes de subir o backend:

```bash
APEX_LICENSE_CATALOG="CLIENTE-GERAL|all|5|365;CLIENTE-PDV|desktop|1|365;CLIENTE-DUO|desktop+mobile-staff|2|365"
docker compose up -d --build
```

Bancos ja existentes precisam receber os upgrades em `docs/upgrade-license-entitlements.sql` e `docs/upgrade-admcalc-financeiro.sql`.

## 2. Validar saude da pilha

```bash
docker compose ps
curl http://localhost:8080/actuator/health
curl http://localhost:4200/healthz
curl http://localhost:4200/actuator/health
```

Resultado esperado para a API:

```json
{"status":"UP","components":{"database":{"status":"UP"}}}
```

O endpoint `/api/produtos` deve responder `402`, `401` ou `403` sem licenca/token. Isso confirma que a API esta online e protegida:

```bash
curl -i http://localhost:8080/api/produtos
```

O modulo financeiro avancado fica em `http://localhost:4200/finance`. Para testar a API diretamente, valide primeiro uma licenca em `/api/licenses/validate`, faca login com um perfil `financeiro` ou `admin`, e chame:

```bash
curl -i http://localhost:8080/api/financeiro/calculos/admcalc
```

Sem token ou role adequada, o esperado e `401` ou `403`.

## 3. Validar que o frontend aguarda o backend

1. Derrube somente a API:

```bash
docker compose stop apex-backend
```

2. Abra `http://localhost:4200` e navegue pelo sistema.
3. Tente login, dashboard ou qualquer tela que chame `/api`.
4. As requisicoes ficam aguardando `/actuator/health` voltar para `UP`, evitando `Connection refused` e tela branca.
5. Suba a API novamente:

```bash
docker compose up -d apex-backend
```

6. Quando a API voltar a `UP`, as novas chamadas seguem normalmente.

Para acompanhar:

```bash
docker compose logs -f apex-backend apex-web
```

## 4. Simular falhas de resiliencia

Banco indisponivel:

```bash
docker compose stop apex-mysql
docker compose ps
curl -i http://localhost:8080/actuator/health
```

Resultado esperado: backend fica `unhealthy` ou retorna `DOWN` ate o banco voltar.

Recuperacao:

```bash
docker compose up -d apex-mysql
docker compose up -d apex-backend apex-web
docker compose ps
```

API indisponivel:

```bash
docker compose stop apex-backend
curl -i http://localhost:4200/actuator/health
```

Resultado esperado: o proxy web falha enquanto a API esta fora, mas o app nao dispara login/dashboard antes da saude voltar.

## 5. Validacoes locais recomendadas

Backend:

```bash
node scripts/audit-spring-data-jpa.cjs
cd Apex-Gestordemo
./mvnw test
./mvnw -DskipTests package
```

Frontend:

```bash
cd Apex_Gestor
npm ci --legacy-peer-deps
npm audit --omit=dev --audit-level=high
npm run lint
npm run test -- --watch=false --browsers=ChromeHeadless
npm run build:web:client:secure
npm run build:electron:secure:dir
```

Compose:

```bash
docker compose config
docker compose up -d --build
docker compose ps
docker compose down -v --remove-orphans
```

## 6. Testar executaveis publicados

Baixe sempre a release mais recente:

```text
https://github.com/Yuri-TaiyouShield/Apex_Gestor_APP/releases/latest
```

Pacotes esperados:

- `apex-gestor-app-site-web-vX.Y.Z.zip` com `App Site Web`.
- `apex-gestor-app-desktop-vX.Y.Z.zip` com `App Desktop`.
- `apex-gestor-app-mobile-empresa-vX.Y.Z.zip` com `App Mobile Empresa`.
- `apex-gestor-app-mobile-cliente-vX.Y.Z.zip` com `App Mobile Cliente`.

Antes de abrir Desktop ou Mobile, confirme que a API local esta saudavel:

```bash
curl http://localhost:8080/actuator/health
```

Desktop e Mobile usam `http://localhost:8080` como padrao local. Em outro servidor, altere a URL na tela `Configuracoes`.
