-- V13: Higiene financeira, versionamento otimista, multi-tenancy em tipo_despesa,
-- correcao de truncamento Envers (_aud), trilha de auditoria financeira e indices operacionais.
-- Dialeto: MySQL 8.4 (idempotente para bases novas e upgrades).
--
-- Estrategia de validacao pos-migracao:
--   SELECT DATA_TYPE FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'financial_calculation_aud' AND column_name = 'input_snapshot';
--   SELECT COUNT(*) FROM conta_receber WHERE criado_em IS NOT NULL AND data_competencia IS NULL;
--   SELECT COUNT(*) FROM conta_pagar WHERE criado_em IS NOT NULL AND data_competencia IS NULL;
--   SELECT COUNT(*) FROM despesas WHERE data_vencimento IS NOT NULL AND data_competencia IS NULL;
--   SELECT COUNT(*) FROM venda WHERE (finalizado_em IS NOT NULL OR data_venda IS NOT NULL) AND data_competencia IS NULL;
--
-- Estrategia de rollback manual (apenas mediante aprovacao humana explicita):
--   1. Remover indices e constraints adicionados (uk_nf_entrada_tenant_natural, idx_*, uk_tipo_despesa_tenant_nome).
--   2. Restaurar uk_tipo_despesa_nome caso nao existam duplicidades entre tenants.
--   3. Dropar tabelas conta_pagar_aud, conta_receber_aud, despesas_aud, tipo_despesa_aud, credito_cliente_aud.
--   4. Remover colunas adicionadas (version, data_competencia, data_emissao, custo_*, empresa_id/filial_id/natureza_despesa).

-- 1. Correcao de truncamento nas tabelas Envers existentes (V7 criou como VARCHAR(255))
ALTER TABLE financial_calculation_aud
  MODIFY COLUMN input_snapshot TEXT,
  MODIFY COLUMN resultado_snapshot TEXT;

ALTER TABLE financial_digital_document_aud
  MODIFY COLUMN conteudo TEXT,
  MODIFY COLUMN mensagem_email TEXT;

-- 2. Adicao idempotente de colunas de versionamento, multi-tenancy, competencia e custos
DROP PROCEDURE IF EXISTS upgrade_v13_hygiene_columns;

DELIMITER $$
CREATE PROCEDURE upgrade_v13_hygiene_columns()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'conta_pagar' AND column_name = 'version') THEN
    ALTER TABLE conta_pagar ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'conta_pagar' AND column_name = 'data_competencia') THEN
    ALTER TABLE conta_pagar ADD COLUMN data_competencia DATE;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'conta_pagar' AND column_name = 'data_emissao') THEN
    ALTER TABLE conta_pagar ADD COLUMN data_emissao DATE;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'conta_receber' AND column_name = 'version') THEN
    ALTER TABLE conta_receber ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'conta_receber' AND column_name = 'data_competencia') THEN
    ALTER TABLE conta_receber ADD COLUMN data_competencia DATE;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'conta_receber' AND column_name = 'data_emissao') THEN
    ALTER TABLE conta_receber ADD COLUMN data_emissao DATE;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'despesas' AND column_name = 'version') THEN
    ALTER TABLE despesas ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'despesas' AND column_name = 'data_competencia') THEN
    ALTER TABLE despesas ADD COLUMN data_competencia DATE;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'despesas' AND column_name = 'data_emissao') THEN
    ALTER TABLE despesas ADD COLUMN data_emissao DATE;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'nota_fiscal_entrada' AND column_name = 'version') THEN
    ALTER TABLE nota_fiscal_entrada ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'nota_fiscal_entrada' AND column_name = 'data_competencia') THEN
    ALTER TABLE nota_fiscal_entrada ADD COLUMN data_competencia DATE;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'credito_cliente' AND column_name = 'version') THEN
    ALTER TABLE credito_cliente ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'tipo_despesa' AND column_name = 'empresa_id') THEN
    ALTER TABLE tipo_despesa ADD COLUMN empresa_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'tipo_despesa' AND column_name = 'filial_id') THEN
    ALTER TABLE tipo_despesa ADD COLUMN filial_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'tipo_despesa' AND column_name = 'natureza_despesa') THEN
    ALTER TABLE tipo_despesa ADD COLUMN natureza_despesa VARCHAR(24);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'venda' AND column_name = 'data_competencia') THEN
    ALTER TABLE venda ADD COLUMN data_competencia DATE;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'venda' AND column_name = 'data_emissao') THEN
    ALTER TABLE venda ADD COLUMN data_emissao DATE;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'venda_aud' AND column_name = 'data_competencia') THEN
    ALTER TABLE venda_aud ADD COLUMN data_competencia DATE;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'venda_aud' AND column_name = 'data_emissao') THEN
    ALTER TABLE venda_aud ADD COLUMN data_emissao DATE;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'estoque_movimentacao' AND column_name = 'custo_unitario') THEN
    ALTER TABLE estoque_movimentacao ADD COLUMN custo_unitario DECIMAL(15,4);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'estoque_movimentacao' AND column_name = 'custo_medio_anterior') THEN
    ALTER TABLE estoque_movimentacao ADD COLUMN custo_medio_anterior DECIMAL(15,4);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'estoque_movimentacao' AND column_name = 'custo_medio_posterior') THEN
    ALTER TABLE estoque_movimentacao ADD COLUMN custo_medio_posterior DECIMAL(15,4);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'estoque_movimentacao' AND column_name = 'valor_total_custo') THEN
    ALTER TABLE estoque_movimentacao ADD COLUMN valor_total_custo DECIMAL(15,2);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'venda_devolucao_item' AND column_name = 'custo_unitario') THEN
    ALTER TABLE venda_devolucao_item ADD COLUMN custo_unitario DECIMAL(15,4);
  END IF;
END $$
DELIMITER ;

CALL upgrade_v13_hygiene_columns();
DROP PROCEDURE IF EXISTS upgrade_v13_hygiene_columns;

-- 3. Backfill seguro e idempotente das datas de competencia e emissao
UPDATE conta_receber
   SET data_competencia = CAST(criado_em AS DATE)
 WHERE data_competencia IS NULL
   AND criado_em IS NOT NULL;

UPDATE conta_receber
   SET data_emissao = CAST(criado_em AS DATE)
 WHERE data_emissao IS NULL
   AND criado_em IS NOT NULL;

UPDATE conta_pagar
   SET data_competencia = CAST(criado_em AS DATE)
 WHERE data_competencia IS NULL
   AND criado_em IS NOT NULL;

UPDATE conta_pagar
   SET data_emissao = CAST(criado_em AS DATE)
 WHERE data_emissao IS NULL
   AND criado_em IS NOT NULL;

UPDATE despesas
   SET data_competencia = data_vencimento
 WHERE data_competencia IS NULL
   AND data_vencimento IS NOT NULL;

UPDATE despesas
   SET data_emissao = data_vencimento
 WHERE data_emissao IS NULL
   AND data_vencimento IS NOT NULL;

UPDATE venda
   SET data_competencia = COALESCE(CAST(finalizado_em AS DATE), CAST(data_venda AS DATE))
 WHERE data_competencia IS NULL
   AND (finalizado_em IS NOT NULL OR data_venda IS NOT NULL);

UPDATE venda
   SET data_emissao = COALESCE(CAST(data_venda AS DATE), CAST(finalizado_em AS DATE))
 WHERE data_emissao IS NULL
   AND (data_venda IS NOT NULL OR finalizado_em IS NOT NULL);

UPDATE nota_fiscal_entrada
   SET data_competencia = COALESCE(CAST(data_entrada AS DATE), CAST(data_emissao AS DATE))
 WHERE data_competencia IS NULL
   AND (data_entrada IS NOT NULL OR data_emissao IS NOT NULL);

UPDATE nota_fiscal_entrada
   SET data_emissao = data_entrada
 WHERE data_emissao IS NULL
   AND data_entrada IS NOT NULL;

-- 4. Novas tabelas de auditoria Envers para entidades financeiras criticas
CREATE TABLE IF NOT EXISTS conta_pagar_aud (
  id_conta_pagar BIGINT NOT NULL,
  REV INT NOT NULL,
  REVTYPE TINYINT NOT NULL,
  fornecedor_id BIGINT,
  categoria_id BIGINT,
  empresa_id BIGINT,
  filial_id BIGINT,
  descricao VARCHAR(160),
  valor DECIMAL(12,2),
  vencimento DATE,
  forma_pagamento_prevista_id BIGINT,
  status VARCHAR(16),
  criado_em DATETIME,
  pago_em DATETIME,
  data_competencia DATE,
  data_emissao DATE,
  version BIGINT,
  PRIMARY KEY (id_conta_pagar, REV),
  CONSTRAINT fk_conta_pagar_aud_revision FOREIGN KEY (REV) REFERENCES audit_revision(revision_id)
);

CREATE TABLE IF NOT EXISTS conta_receber_aud (
  id_conta_receber BIGINT NOT NULL,
  REV INT NOT NULL,
  REVTYPE TINYINT NOT NULL,
  venda_id BIGINT,
  cliente_id BIGINT,
  forma_pagamento_id BIGINT,
  condicao_pagamento_id BIGINT,
  caixa_sessao_id BIGINT,
  empresa_id BIGINT,
  filial_id BIGINT,
  valor_bruto DECIMAL(12,2),
  desconto DECIMAL(12,2),
  valor_liquido DECIMAL(12,2),
  valor_pago DECIMAL(12,2),
  saldo DECIMAL(12,2),
  vencimento DATE,
  status VARCHAR(16),
  criado_em DATETIME,
  pago_em DATETIME,
  data_competencia DATE,
  data_emissao DATE,
  version BIGINT,
  PRIMARY KEY (id_conta_receber, REV),
  CONSTRAINT fk_conta_receber_aud_revision FOREIGN KEY (REV) REFERENCES audit_revision(revision_id)
);

CREATE TABLE IF NOT EXISTS despesas_aud (
  id_despesa BIGINT NOT NULL,
  REV INT NOT NULL,
  REVTYPE TINYINT NOT NULL,
  descricao VARCHAR(255),
  valor DECIMAL(10,2),
  data_vencimento DATE,
  data_pagamento DATE,
  empresa_id BIGINT,
  filial_id BIGINT,
  tipo_despesa_id BIGINT,
  status INT,
  data_competencia DATE,
  data_emissao DATE,
  version BIGINT,
  PRIMARY KEY (id_despesa, REV),
  CONSTRAINT fk_despesas_aud_revision FOREIGN KEY (REV) REFERENCES audit_revision(revision_id)
);

CREATE TABLE IF NOT EXISTS tipo_despesa_aud (
  id_tipo_despesa BIGINT NOT NULL,
  REV INT NOT NULL,
  REVTYPE TINYINT NOT NULL,
  nome VARCHAR(50),
  status INT,
  empresa_id BIGINT,
  filial_id BIGINT,
  natureza_despesa VARCHAR(24),
  PRIMARY KEY (id_tipo_despesa, REV),
  CONSTRAINT fk_tipo_despesa_aud_revision FOREIGN KEY (REV) REFERENCES audit_revision(revision_id)
);

CREATE TABLE IF NOT EXISTS credito_cliente_aud (
  id_credito BIGINT NOT NULL,
  REV INT NOT NULL,
  REVTYPE TINYINT NOT NULL,
  cliente_id BIGINT,
  venda_origem_id BIGINT,
  devolucao_id BIGINT,
  empresa_id BIGINT,
  filial_id BIGINT,
  valor_original DECIMAL(12,2),
  saldo DECIMAL(12,2),
  status VARCHAR(16),
  criado_em DATETIME,
  version BIGINT,
  PRIMARY KEY (id_credito, REV),
  CONSTRAINT fk_credito_cliente_aud_revision FOREIGN KEY (REV) REFERENCES audit_revision(revision_id)
);

-- 5. Troca de indice global para tenant em tipo_despesa e indices operacionais/periodo
DROP PROCEDURE IF EXISTS upgrade_v13_hygiene_indexes;

DELIMITER $$
CREATE PROCEDURE upgrade_v13_hygiene_indexes()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'tipo_despesa' AND index_name = 'uk_tipo_despesa_nome') THEN
    ALTER TABLE tipo_despesa DROP INDEX uk_tipo_despesa_nome;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'tipo_despesa' AND index_name = 'uk_tipo_despesa_tenant_nome') THEN
    ALTER TABLE tipo_despesa ADD CONSTRAINT uk_tipo_despesa_tenant_nome UNIQUE (empresa_id, filial_id, nome);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'tipo_despesa' AND index_name = 'idx_tipo_despesa_tenant') THEN
    CREATE INDEX idx_tipo_despesa_tenant ON tipo_despesa(empresa_id, filial_id, status);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'produto_venda' AND index_name = 'idx_produto_venda_venda') THEN
    CREATE INDEX idx_produto_venda_venda ON produto_venda(venda_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'item_nota_fiscal' AND index_name = 'idx_item_nf_entrada') THEN
    CREATE INDEX idx_item_nf_entrada ON item_nota_fiscal(nf_entrada_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'item_nota_fiscal' AND index_name = 'idx_item_nf_produto') THEN
    CREATE INDEX idx_item_nf_produto ON item_nota_fiscal(produto_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'venda' AND index_name = 'idx_venda_tenant_data_status') THEN
    CREATE INDEX idx_venda_tenant_data_status ON venda(empresa_id, filial_id, data_venda, status);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'conta_receber' AND index_name = 'idx_conta_receber_tenant_pago') THEN
    CREATE INDEX idx_conta_receber_tenant_pago ON conta_receber(empresa_id, filial_id, pago_em);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'conta_receber' AND index_name = 'idx_conta_receber_tenant_comp') THEN
    CREATE INDEX idx_conta_receber_tenant_comp ON conta_receber(empresa_id, filial_id, data_competencia, status);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'conta_pagar' AND index_name = 'idx_conta_pagar_tenant_pago') THEN
    CREATE INDEX idx_conta_pagar_tenant_pago ON conta_pagar(empresa_id, filial_id, pago_em);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'conta_pagar' AND index_name = 'idx_conta_pagar_tenant_comp') THEN
    CREATE INDEX idx_conta_pagar_tenant_comp ON conta_pagar(empresa_id, filial_id, data_competencia, status);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'despesas' AND index_name = 'idx_despesas_tenant_pagamento') THEN
    CREATE INDEX idx_despesas_tenant_pagamento ON despesas(empresa_id, filial_id, data_pagamento);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'despesas' AND index_name = 'idx_despesas_tenant_comp') THEN
    CREATE INDEX idx_despesas_tenant_comp ON despesas(empresa_id, filial_id, data_competencia, status);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'caixa_movimentacao' AND index_name = 'idx_caixa_mov_sessao_data') THEN
    CREATE INDEX idx_caixa_mov_sessao_data ON caixa_movimentacao(caixa_sessao_id, criado_em);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'nota_fiscal_entrada' AND index_name = 'idx_nf_entrada_tenant_entrada') THEN
    CREATE INDEX idx_nf_entrada_tenant_entrada ON nota_fiscal_entrada(empresa_id, filial_id, data_entrada);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'nota_fiscal_entrada' AND index_name = 'uk_nf_entrada_tenant_natural') THEN
    ALTER TABLE nota_fiscal_entrada ADD CONSTRAINT uk_nf_entrada_tenant_natural
      UNIQUE (empresa_id, filial_id, fornecedor_id, numero, serie);
  END IF;
END $$
DELIMITER ;

CALL upgrade_v13_hygiene_indexes();
DROP PROCEDURE IF EXISTS upgrade_v13_hygiene_indexes;
