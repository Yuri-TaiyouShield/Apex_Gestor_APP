# Migrations legadas fora do caminho Flyway ativo

Os arquivos deste diretório são preservados para auditoria histórica e não são
carregados pelo Flyway.

## Evidência de origem

- `V1__apex_schema_baseline.sql` e `V4__enterprise_compliance_audit_outbox.sql`
  foram adicionadas no commit `b98489ef` (`v4.0.0`).
- Ambos foram removidos explicitamente no commit `47adeafb`, que é ancestral do
  HEAD atual `de662e36`.
- Mantê-los em `src/main/resources/db/migration` reintroduzia uma segunda `V4`
  no checkout local, apesar de a cadeia canônica atual começar em `V2` e seguir
  até `V5`.

O conteúdo foi apenas realocado, sem renumeração ou edição. A cadeia ativa deve
ser validada como `V2`–`V9` depois que as migrations forward-only locais forem
revisadas e testadas em H2 e MySQL.
