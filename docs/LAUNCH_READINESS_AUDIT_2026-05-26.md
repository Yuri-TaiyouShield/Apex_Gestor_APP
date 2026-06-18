# Auditoria de Prontidao para Lancamento

Data: 2026-05-26
Workspace: `E:\Arquivo do pen drive yuri\Apex_Gestor_APP`

## Veredito

**Nao lancar oficialmente no Brasil ainda.**

O projeto tem uma base promissora de backend e ja recebeu alguns hardenings, mas ainda nao esta pronto para venda nacional responsavel. Ele deve entrar em fase de organizacao tecnica, recuperacao/versionamento do frontend e piloto controlado.

## Evidencias do Checkout

- Branch atual: `codex/legacy/pre-v3-apex-gestor-2`
- Remote: `https://github.com/Yuri-TaiyouShield/Apex_Gestor_APP.git`
- Ultimo commit observado: `8973eb9 feat: adiciona hardening enterprise e auditoria automatizada`
- Itens nao rastreados: `Apex_Gestor/` e `Apex-Gestordemo/demo/`
- Backend versionado: `Apex-Gestordemo/`
- Documentacao anterior: README descrevia Servlet/JSP/PostgreSQL, mas o codigo atual usa Spring Boot/MySQL e artefatos Angular/Ionic/Capacitor/Electron.

## Verificacoes Executadas

```powershell
powershell -ExecutionPolicy Bypass -File tools\audit\apex-enterprise-audit.ps1 -SkipBuild
```

Resultado: passou sem falhas bloqueantes nos arquivos versionados.

```powershell
cd Apex-Gestordemo
cmd /c mvnw.cmd -q test
```

Resultado: testes passaram.

Observacao: uma tentativa anterior a partir da raiz falhou porque nao ha `pom.xml` na raiz. A execucao correta e dentro de `Apex-Gestordemo/`.

## Stack Encontrada

Backend:

- Spring Boot 4.0.0
- Java configurado como 25 e compiler release 24
- Spring Data JPA, JDBC, Web, Validation, Actuator, Security
- MySQL Connector/J
- Lombok
- Testes JUnit/Spring Security

Infra local:

- `docker-compose.yml` com MySQL 8.4 e backend API
- healthcheck do banco e da API
- variaveis de ambiente para banco, CORS, seguranca e JPA

Frontend/mobile/desktop:

- `Apex_Gestor/` contem diretorios `.angular`, `.electron-package`, `android`, `dist`, `node_modules`, `release`, `release-check`
- nao foi encontrado source root versionado evidente no checkout atual
- como o diretorio esta nao rastreado, ele nao deve ser considerado base confiavel de release

## Principais Riscos

1. **Fonte frontend nao governado**

   Sem fonte limpo e rastreado, nao ha como garantir build reproduzivel, revisao, rollback ou suporte.

2. **Fiscal incompleto**

   Existe nota fiscal de entrada, mas lancamento oficial no Brasil exige decisao sobre NF-e, NFC-e, SAT quando aplicavel, contingencia, certificado digital, XML, SPED e regras por UF.

3. **Seguranca ainda condicional**

   O backend tem `APEX_SECURITY_REQUIRE_AUTH`, mas ambiente de desenvolvimento fica permissivo. Producao precisa exigir autenticacao e autorizacao sempre.

4. **Banco sem migrations**

   `JPA_DDL_AUTO=update` e aceitavel para desenvolvimento, mas nao para producao comercial.

5. **Testes insuficientes para dominio**

   Os testes atuais validam carregamento basico e encoder de senha. Faltam testes de venda, cancelamento, custo medio, estoque, financeiro, permissao e erros fiscais.

6. **CI historicamente desalinhado**

   Workflows existentes eram de GitHub Pages. Produto comercial precisa de CI de backend, auditoria, empacotamento, seguranca e release.

7. **LGPD nao fechada**

   O sistema trata nome, CPF/CNPJ, endereco, dados de usuarios e historico comercial. Faltam artefatos de privacidade, base legal, retencao e atendimento a titulares.

## Pontos Positivos

- O backend compila e os testes atuais passam.
- Ja existe configuracao de seguranca com headers, CORS configuravel e BCrypt.
- Ja existe Docker Compose para ambiente local.
- Ja existe script de auditoria com scan de segredos, guardrail Spring Data e postura de seguranca.
- O dominio inicial cobre cadastros, vendas, estoque, despesas e relatorio financeiro.

## Decisao Recomendada

Congelar a promessa de "lancamento oficial nacional" ate concluir:

- fonte frontend versionado e build reproduzivel;
- migrations e perfil de producao;
- autenticacao obrigatoria e RBAC real;
- trilha de auditoria de operacoes criticas;
- cobertura de testes de fluxos de loja;
- fiscal por UF ou integracao fiscal terceirizada homologada;
- LGPD, contrato, suporte e backup/restore.
