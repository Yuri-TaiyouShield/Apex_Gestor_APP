-- V8: idempotencia duravel de devolucoes e invariant de caixa aberto.
-- A migration deve ser aplicada somente apos a validacao da familia Flyway canonica.

ALTER TABLE venda_devolucao ADD COLUMN idempotency_key VARCHAR(128);
ALTER TABLE venda_devolucao ADD COLUMN idempotency_payload_hash VARCHAR(64);
ALTER TABLE venda_devolucao ADD CONSTRAINT uk_devolucao_tenant_idempotency
  UNIQUE (empresa_id, filial_id, idempotency_key);

ALTER TABLE caixa_sessao ADD COLUMN caixa_aberto_unico INT;
UPDATE caixa_sessao
   SET caixa_aberto_unico = 1
 WHERE status = 'ABERTO' AND caixa_aberto_unico IS NULL;
CREATE UNIQUE INDEX uk_caixa_aberto_operador_tenant
    ON caixa_sessao (operador_login, empresa_id, filial_id, caixa_aberto_unico);

