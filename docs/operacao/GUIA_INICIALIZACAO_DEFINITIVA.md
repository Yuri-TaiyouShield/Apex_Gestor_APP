# Guia Definitivo de Inicialização e Operação Local — Apex Gestor

**Branch de Trabalho:** `codex/consolida-v1-operacional`  
**Ambiente:** Windows 11 (Nativo / DevTools Portáteis)  
**Data:** 16 de Novembro de 2026  
**Status do Ambiente:** ✅ Totalmente Operacional e Estabilizado

---

## 1. Visão Geral da Arquitetura Local

| Componente | Tecnologia | Configuração Local | Porta |
|---|---|---|---|
| **Linguagem / Runtime** | Java OpenJDK (Temurin) | `F:\DevTools\temurin25-portable\unpacked\jdk-25.0.4.1+1` | N/A |
| **Node.js** | Node.js v24.16.0 / npm | `F:\nodejs` | N/A |
| **Build Tool Backend** | Maven 3.9.16 / Maven Wrapper | `F:\DevTools\maven\apache-maven-3.9.16` / `.\mvnw.cmd` | N/A |
| **Banco de Dados** | MySQL Server 8.4 (Serviço Windows) | `MySQL84` (`127.0.0.1:3306`), Database `apex_db` | **3306** |
| **Backend API** | Spring Boot 4.0.0 (Java 25) | `Apex-Gestordemo` (Spring Security, JWT, JPA, Flyway V2..V12) | **8080** |
| **Frontend Web** | Angular 19 (Tailwind + Angular Material) | `Apex_Gestor` (SPA / Multiplataforma) | **4200** |
| **Mensageria** | Transacional via Outbox Pattern | Armazenamento transacional em MySQL (`financial_document_delivery_outbox`) | N/A |

---

## 2. Pré-requisitos e Inicialização do Ambiente (Bootstrap)

O script `scripts/bootstrap-env.ps1` é **idempotente** e configura o PATH e variáveis de ambiente no escopo do **Usuário** (sem necessidade de privilégios de Administrador).

### Execução do Bootstrap:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\bootstrap-env.ps1
```

O script detecta automaticamente:
- `JAVA_HOME` → `F:\DevTools\temurin25-portable\unpacked\jdk-25.0.4.1+1`
- `NODE_HOME` → `F:\nodejs`
- `M2_HOME` → `F:\DevTools\maven\apache-maven-3.9.16`
- Registra os diretórios no PATH do usuário preservando todas as entradas anteriores.

---

## 3. Banco de Dados MySQL

### Serviço Windows:
- **Nome do Serviço:** `MySQL84`
- **Conta de Execução:** `LocalSystem`
- **Inicialização:** Automática (`Auto`)
- **Arquivo de Configuração:** `C:\ProgramData\MySQL\MySQL Server 8.4\my.ini`
- **DataDir:** `C:\ProgramData\MySQL\MySQL Server 8.4\Data`

### Verificação do Serviço:
```powershell
Get-Service MySQL84
```

### Configurações de Conexão:
- **Host / Porta:** `127.0.0.1:3306`
- **Base de Dados:** `apex_db`
- **Usuário:** `root`
- **Senha:** `apex_dev_2026`

### Migrações Flyway:
O sistema utiliza seleção dinâmica de diretório de migrações (`FlywayRuntimeConfig.java`):
- MySQL: `classpath:db/migration-mysql` (compatível com DDL específico do MySQL)
- H2 / Testes: `classpath:db/migration`
- Migrações aplicadas: `V2` até `V12` (Tabelas operacionais, fiscais, auditoria Envers e governança LGPD).

### Rotinas de Backup e Restauração:
- Backup completo:
  ```powershell
  powershell -ExecutionPolicy Bypass -File scripts\backup-mysql.ps1
  ```
- Restauração:
  ```powershell
  powershell -ExecutionPolicy Bypass -File scripts\restore-mysql.ps1 -BackupFile caminho_do_backup.sql
  ```

---

## 4. Inicialização do Backend (Spring Boot)

O backend deve ser inicializado a partir da pasta `Apex-Gestordemo`.

### Execução via Maven Wrapper:
```powershell
cd Apex-Gestordemo
.\mvnw.cmd spring-boot:run
```

### Variáveis de Ambiente Recomendadas (.env):
O arquivo `.env` na raiz do projeto é utilizado para definir credenciais e parâmetros:
```dotenv
DB_URL=jdbc:mysql://localhost:3306/apex_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=apex_dev_2026
DB_PORT=3306
API_PORT=8080
APEX_SECURITY_REQUIRE_AUTH=true
APEX_JWT_SECRET=apex-local-dev-jwt-secret-minimum-32-chars-key-2026
MANAGEMENT_HEALTH_RABBIT_ENABLED=false
```

### Endpoints de Diagnóstico e Validação:
- Health Check: `GET http://localhost:8080/actuator/health` → `{"status": "UP"}`
- Teste de Segurança (Autenticação Obrigatória): `GET http://localhost:8080/api/produtos` → `401 Unauthorized`
- Login com Consentimento LGPD: `POST http://localhost:8080/api/auth/login`

---

## 5. Inicialização do Frontend (Angular)

O frontend deve ser inicializado a partir da pasta `Apex_Gestor`.

### Modo Desenvolvimento:
```powershell
cd Apex_Gestor
npm start
```
ou:
```powershell
npm run start:web
```
Acesse no navegador: `http://localhost:4200`

### Build de Produção:
```powershell
cd Apex_Gestor
npm run build:web
```

---

## 6. Execução de Testes Automatizados

### Backend (JUnit 5 + Spring Boot Test):
```powershell
cd Apex-Gestordemo
.\mvnw.cmd test
```
*Total de testes: 53 executados (52 aprovados, 0 falhas, 0 erros, 1 ignorado condicionalmente sem Docker).*

---

## 7. Diagnóstico das Causas-Raiz e Soluções Aplicadas

| # | Problema Original | Causa-Raiz | Solução Definitiva |
|---|---|---|---|
| 1 | `node`, `java`, `mvn` ausentes no PATH | Instalações portáteis em `F:\` e caminhos obsoletos no PATH do sistema. | `scripts/bootstrap-env.ps1` com injeção segura e idempotente no escopo de Usuário. |
| 2 | Serviço MySQL84 parado e sem permissão | Permissões de elevação exigidas para comandos manuais; serviço configurado como manual. | Serviço configurado como automático (`LocalSystem`). Porta 3306 ativa. Scripts de backup/restore criados. |
| 3 | Erro com `APEX_SECURITY_REQUIRE_AUTH=false` | Trava de segurança no `SecurityConfig.java` para prevenir bypass acidental de autenticação. | Autenticação mantida ativa (`require-auth=true`) com JWT secret de alta entropia para desenvolvimento local. |
| 4 | Divergência de portas (2705 vs 3306) | `.env.example` mencionava 2705 enquanto MySQL rodava em 3306. | Criação de `.env` unificado e atualização de `application.properties`/`application-dev.properties` para a porta 3306. |
| 5 | RabbitMQ ausente | Starter AMQP tentava conexão com broker inexistente. | Mensageria local isolada via Transactional Outbox Pattern e propriedades `apex.messaging.rabbit.enabled=false`. |
