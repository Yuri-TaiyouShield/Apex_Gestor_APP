# Banco de Dados

Esta pasta concentra artefatos de banco que antes ficavam espalhados na raiz.

## Arquivos

- `apex-bd-base.sql`: dump historico de referencia do schema Apex.
- `apex-bd-modelo.pdf`: exportacao visual historica do modelo.
- `der-apex-gestor.mmd`: diagrama Mermaid do DER.
- `migrations-reference/`: scripts SQL antigos ou complementares para consulta.

## Fonte oficial atual

A fonte oficial para evolucao do schema em runtime e Flyway:

```text
Apex-Gestordemo/src/main/resources/db/migration
```

Use os arquivos desta pasta como referencia, documentacao ou carga manual controlada. Mudancas de producao devem virar migration versionada no backend.
