# Migrations adiadas

A migration enterprise de auditoria/outbox que existia como `V4` foi retirada do caminho ativo porque colidia com a `V4__venda_caixa_devolucao_financeiro.sql`, que pertence ao histórico homologado da v1 operacional.

Ela deve ser redesenhada e reversionada somente na etapa administrativa posterior, após validar o histórico Flyway dos ambientes publicados. Não executar `repair` nem reutilizar a versão `V4`.

O dump MariaDB legado que estava nomeado como `V1__apex_schema_baseline.sql` também não integra o caminho ativo: ele contém comandos específicos do MariaDB e não é uma migration portável/idempotente para H2 e MySQL Testcontainers. A v1 homologada inicia sua evolução estrutural em V2, conforme o baseline já existente nos ambientes-alvo.
