# Modulo Financeiro AdmCalc, Trabalhista e Tributario

## Objetivo

Este modulo leva o projeto `AdmCalc` para dentro do Apex Gestor e amplia o financeiro com calculos trabalhistas, tributarios, auditoria antifraude e workflow de documentos digitais. Ele roda no backend Spring Boot e e consumido pelas interfaces Angular, Ionic e Electron pela rota `/finance`.

## Controle de acesso

Endpoints protegidos em `/api/financeiro/**` exigem JWT, licenca Apex valida e perfis adequados.

- Calculos e auditoria: `ADMIN`, `ADMINISTRACAO`, `FINANCEIRO` ou `FINANCAS`.
- Documentos: geracao e rascunho por `ADMIN`, `ADMINISTRACAO`, `FINANCEIRO` ou `FINANCAS`.
- Assinatura: `ADMIN`, `ADMINISTRACAO`, `FINANCEIRO`, `FINANCAS`, `CONTADOR` ou `ADVOGADO`, respeitando o cargo exigido no documento.

## Endpoints principais

- `POST /api/financeiro/calculos/trabalhista`
- `POST /api/financeiro/calculos/tributario`
- `POST /api/financeiro/calculos/admcalc`
- `GET /api/financeiro/calculos`
- `POST /api/financeiro/documentos`
- `GET /api/financeiro/documentos`
- `POST /api/financeiro/documentos/{id}/rascunho-email`
- `POST /api/financeiro/documentos/{id}/assinaturas`
- `GET /api/financeiro/auditoria`

## Auditoria antifraude

Cada calculo gera:

- Registro em `financial_calculation`, com hash da entrada e snapshot do resultado.
- Evento em `financial_audit_event`, com usuario, tipo do evento, alvo, data/hora e metadados mascarados.

Cada documento gera eventos para:

- Criacao/retencao.
- Criacao de rascunho de e-mail.
- Assinatura digital e tentativa de envio.

## Documentos e assinatura

Fluxo padrao:

1. Financeiro gera o documento.
2. Documento fica em `PENDENTE_ASSINATURA`.
3. Contador, advogado ou cargo exigido aprova e assina.
4. O backend calcula `assinaturaDigitalHash`.
5. Se SMTP estiver habilitado, o e-mail e enviado ao funcionario. Se SMTP estiver desligado, o sistema mantem o documento assinado e o rascunho pronto.

Configuracao SMTP:

```bash
APEX_EMAIL_ENABLED=true
APEX_EMAIL_FROM=financeiro@suaempresa.com
SMTP_HOST=smtp.suaempresa.com
SMTP_PORT=587
SMTP_USERNAME=usuario
SMTP_PASSWORD=senha
SMTP_AUTH=true
SMTP_STARTTLS=true
```

## Banco de dados

Instalacoes novas recebem as tabelas pelas migrations do Flyway em `Apex-Gestordemo/src/main/resources/db/migration`. Bancos existentes que ainda usam o dump legado podem consultar:

```bash
docs/database/migrations-reference/upgrade-admcalc-financeiro.sql
```

Tabelas adicionadas:

- `financial_calculation`
- `financial_audit_event`
- `financial_digital_document`

Perfis adicionados:

- `financeiro`
- `contador`
- `advogado`

## Observacao operacional

INSS, IRRF, salario minimo, salario-familia, CCT e parametros fiscais variam por periodo e legislacao. O motor aceita tabelas e percentuais parametrizados na requisicao; em producao, mantenha esses parametros sob responsabilidade do contador da empresa.
