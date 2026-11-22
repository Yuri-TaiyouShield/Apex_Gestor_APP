# Arquitetura e Operacao

## Visao Geral

O Apex Gestor deve evoluir como produto modular:

```text
Frontend Web/Mobile/Desktop
        |
        v
Backend Spring Boot API
        |
        v
MySQL 8.4
```

O contrato de evolucao por branch esta documentado em `docs/VERSIONED_EVOLUTION_ROADMAP.md`. A matriz minima de permissoes, auditoria, soft delete e multiempresa esta em `docs/RBAC_AUDIT_MATRIX.md`.

No checkout atual, o backend versionado esta em `Apex-Gestordemo/`. O diretorio `Apex_Gestor/` existe localmente, mas aparece nao rastreado no git e parece conter artefatos gerados. Portanto, o frontend nao deve ser tratado como release confiavel ate que o source seja recuperado ou recriado.

## Backend

Frameworks e dependencias principais:

- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security
- Spring Actuator
- MySQL Connector/J
- Lombok

Pacotes atuais:

- `Controller`: endpoints REST
- `Service`: regras de negocio
- `Repository`: acesso a dados via Spring Data
- `Model`: entidades JPA
- `DTO`: objetos de relatorio
- `com.Apex.Apex_Gestordemo.web`: filtro de trace e handler de excecao

Contratos REST adicionados para a base `v1-desktop-core`:

- `GET /api/estoque/movimentacoes/produto/{produtoId}`: historico de movimentacoes do produto.
- `POST /api/estoque/movimentacoes`: registra entrada, saida, ajuste, transferencia, inventario ou estorno de venda.
- `GET /api/auditoria`: consulta trilhas de auditoria.
- `GET /api/notificacoes/pendentes`: consulta central de notificacoes pendentes.

## Banco de Dados

Padrao local:

- MySQL 8.4
- database: `apex_db`
- porta local: `2705`

Desenvolvimento pode usar:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Producao deve usar:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Antes de producao, introduzir migrations com Flyway ou Liquibase.

## Variaveis de Ambiente

| Variavel | Uso |
| --- | --- |
| `DB_URL` | URL JDBC do MySQL |
| `DB_USERNAME` | usuario do banco |
| `DB_PASSWORD` | senha do banco |
| `DB_POOL_MAX_SIZE` | tamanho maximo do pool |
| `JPA_DDL_AUTO` | modo de schema JPA |
| `APEX_SECURITY_REQUIRE_AUTH` | exige autenticacao |
| `APEX_CORS_ALLOWED_ORIGINS` | origens permitidas |
| `API_PORT` | porta exposta no Docker Compose |

## Ambiente Local

```powershell
copy .env.example .env
docker compose up --build
```

Healthcheck:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Testes:

```powershell
cd Apex-Gestordemo
cmd /c mvnw.cmd -q test
```

Auditoria:

```powershell
powershell -ExecutionPolicy Bypass -File tools\audit\apex-enterprise-audit.ps1 -SkipBuild
```

## Seguranca

Ambiente local pode manter `APEX_SECURITY_REQUIRE_AUTH=false` para demos.

Ambiente de producao deve obrigatoriamente usar:

```env
APEX_SECURITY_REQUIRE_AUTH=true
JPA_DDL_AUTO=validate
APEX_CORS_ALLOWED_ORIGINS=https://dominio-oficial.example
```

Proximas evolucoes obrigatorias:

- usuarios reais com senha inicial temporaria;
- troca de senha no primeiro acesso;
- RBAC persistente por perfil;
- catalogo de permissoes por acao critica (`sale.cancel`, `stock.change`, `finance.change`, `discount.approve`);
- expirar sessoes/tokens;
- log de auditoria com usuario, IP, acao, entidade, antes/depois quando aplicavel;
- rate limit em login e endpoints sensiveis;
- revisao de CORS por canal: web, mobile e desktop.

## Observabilidade

Estado atual:

- actuator habilitado para `health`, `info` e `metrics`;
- handler global de excecao;
- filtro de trace.

Antes de producao:

- padronizar logs estruturados;
- incluir `X-Trace-Id` em erros e operacoes criticas;
- alertar banco indisponivel, erro 5xx, uso de disco, memoria e latencia;
- criar dashboard operacional;
- registrar eventos de venda, cancelamento, entrada fiscal, ajuste de estoque e login.

## CI/CD

O workflow `backend-ci.yml` deve validar:

- checkout;
- Java;
- cache Maven;
- auditoria local;
- testes do backend;
- pacote do backend.

Expansoes recomendadas:

- dependency scan;
- SAST;
- build de frontend quando fonte estiver versionado;
- build mobile/desktop separado;
- publicacao de artefatos assinados;
- changelog automatizado.

## Politica de Release

Cada release deve ter:

- versao semantica;
- changelog;
- migracoes aplicadas e reversiveis quando possivel;
- backup antes de atualizar cliente real;
- plano de rollback;
- evidencia de testes;
- responsavel tecnico e responsavel de suporte.

## Runbook de Incidente

1. Identificar impacto: loja parada, PDV parado, fiscal parado, relatorio incorreto ou lentidao.
2. Registrar horario, usuario, versao, trace id e sintomas.
3. Preservar logs e backup.
4. Se afetar venda/fiscal/estoque, pausar atualizacoes automaticas.
5. Aplicar workaround aprovado.
6. Corrigir em branch, testar e gerar release.
7. Documentar causa raiz e prevencao.
