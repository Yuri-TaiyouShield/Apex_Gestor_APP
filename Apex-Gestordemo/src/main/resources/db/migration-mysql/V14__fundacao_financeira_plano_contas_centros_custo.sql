-- V14: Fundacao financeira gerencial - Plano de Contas Gerencial, Centros de Custo,
-- Regras de Classificacao Financeira e Resumo de Competencia Gerencial.
-- Dialeto: MySQL 8.4 (idempotente para bases novas e upgrades).
--
-- Estrategia de validacao pos-migracao:
--   SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('plano_contas_gerencial', 'centro_custo', 'regra_classificacao_financeira', 'resumo_competencia_gerencial');
--   SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND column_name = 'plano_conta_id' AND table_name IN ('tipo_despesa', 'despesas', 'conta_pagar', 'conta_receber', 'categoria', 'nota_fiscal_entrada', 'venda');
--
-- Estrategia de rollback manual (apenas mediante aprovacao humana explicita):
--   1. Remover indices idx_*_classificacao e colunas plano_conta_id, centro_custo_id das tabelas e _aud existentes.
--   2. Dropar tabelas resumo_competencia_gerencial, regra_classificacao_financeira_aud, regra_classificacao_financeira,
--      centro_custo_aud, centro_custo, plano_contas_gerencial_aud, plano_contas_gerencial.

-- 1. Plano de Contas Gerencial + trilha Envers
CREATE TABLE IF NOT EXISTS plano_contas_gerencial (
  id_plano_conta BIGINT PRIMARY KEY AUTO_INCREMENT,
  empresa_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  conta_pai_id BIGINT,
  codigo VARCHAR(32) NOT NULL,
  nome VARCHAR(120) NOT NULL,
  descricao VARCHAR(255),
  tipo_conta VARCHAR(32) NOT NULL,
  natureza VARCHAR(16) NOT NULL,
  classificacao_custo VARCHAR(24) NOT NULL,
  grupo_dre VARCHAR(40) NOT NULL,
  ordem_exibicao INT NOT NULL DEFAULT 0,
  aceita_lancamento BOOLEAN NOT NULL DEFAULT TRUE,
  sistema_padrao BOOLEAN NOT NULL DEFAULT FALSE,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em DATETIME NOT NULL,
  atualizado_em DATETIME,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_plano_contas_tenant_codigo UNIQUE (empresa_id, filial_id, codigo),
  INDEX idx_plano_contas_tenant_grupo (empresa_id, filial_id, grupo_dre, ativo)
);

CREATE TABLE IF NOT EXISTS plano_contas_gerencial_aud (
  id_plano_conta BIGINT NOT NULL,
  REV INT NOT NULL,
  REVTYPE TINYINT NOT NULL,
  empresa_id BIGINT,
  filial_id BIGINT,
  conta_pai_id BIGINT,
  codigo VARCHAR(32),
  nome VARCHAR(120),
  descricao VARCHAR(255),
  tipo_conta VARCHAR(32),
  natureza VARCHAR(16),
  classificacao_custo VARCHAR(24),
  grupo_dre VARCHAR(40),
  ordem_exibicao INT,
  aceita_lancamento BOOLEAN,
  sistema_padrao BOOLEAN,
  ativo BOOLEAN,
  criado_em DATETIME,
  atualizado_em DATETIME,
  version BIGINT,
  PRIMARY KEY (id_plano_conta, REV),
  CONSTRAINT fk_plano_contas_aud_revision FOREIGN KEY (REV) REFERENCES audit_revision(revision_id)
);

-- 2. Centro de Custo + trilha Envers
CREATE TABLE IF NOT EXISTS centro_custo (
  id_centro_custo BIGINT PRIMARY KEY AUTO_INCREMENT,
  empresa_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  codigo VARCHAR(32) NOT NULL,
  nome VARCHAR(120) NOT NULL,
  descricao VARCHAR(255),
  tipo_operacional VARCHAR(32) NOT NULL,
  responsavel_usuario_id BIGINT,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em DATETIME NOT NULL,
  atualizado_em DATETIME,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_centro_custo_tenant_codigo UNIQUE (empresa_id, filial_id, codigo),
  INDEX idx_centro_custo_tenant_ativo (empresa_id, filial_id, ativo)
);

CREATE TABLE IF NOT EXISTS centro_custo_aud (
  id_centro_custo BIGINT NOT NULL,
  REV INT NOT NULL,
  REVTYPE TINYINT NOT NULL,
  empresa_id BIGINT,
  filial_id BIGINT,
  codigo VARCHAR(32),
  nome VARCHAR(120),
  descricao VARCHAR(255),
  tipo_operacional VARCHAR(32),
  responsavel_usuario_id BIGINT,
  ativo BOOLEAN,
  criado_em DATETIME,
  atualizado_em DATETIME,
  version BIGINT,
  PRIMARY KEY (id_centro_custo, REV),
  CONSTRAINT fk_centro_custo_aud_revision FOREIGN KEY (REV) REFERENCES audit_revision(revision_id)
);

-- 3. Regra de Classificacao Financeira + trilha Envers
CREATE TABLE IF NOT EXISTS regra_classificacao_financeira (
  id_regra_classificacao BIGINT PRIMARY KEY AUTO_INCREMENT,
  empresa_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  nome VARCHAR(120) NOT NULL,
  origem_tipo VARCHAR(32) NOT NULL,
  tipo_despesa_id BIGINT,
  categoria_id BIGINT,
  forma_pagamento_id BIGINT,
  fornecedor_id BIGINT,
  palavra_chave VARCHAR(120),
  plano_conta_id BIGINT NOT NULL,
  centro_custo_id BIGINT,
  prioridade INT NOT NULL DEFAULT 100,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em DATETIME NOT NULL,
  atualizado_em DATETIME,
  version BIGINT NOT NULL DEFAULT 0,
  INDEX idx_regra_class_tenant_origem (empresa_id, filial_id, origem_tipo, ativo, prioridade)
);

CREATE TABLE IF NOT EXISTS regra_classificacao_financeira_aud (
  id_regra_classificacao BIGINT NOT NULL,
  REV INT NOT NULL,
  REVTYPE TINYINT NOT NULL,
  empresa_id BIGINT,
  filial_id BIGINT,
  nome VARCHAR(120),
  origem_tipo VARCHAR(32),
  tipo_despesa_id BIGINT,
  categoria_id BIGINT,
  forma_pagamento_id BIGINT,
  fornecedor_id BIGINT,
  palavra_chave VARCHAR(120),
  plano_conta_id BIGINT,
  centro_custo_id BIGINT,
  prioridade INT,
  ativo BOOLEAN,
  criado_em DATETIME,
  atualizado_em DATETIME,
  version BIGINT,
  PRIMARY KEY (id_regra_classificacao, REV),
  CONSTRAINT fk_regra_class_aud_revision FOREIGN KEY (REV) REFERENCES audit_revision(revision_id)
);

-- 4. Resumo de Competencia Gerencial (snapshot agregado para DRE/DMB)
CREATE TABLE IF NOT EXISTS resumo_competencia_gerencial (
  id_resumo_competencia BIGINT PRIMARY KEY AUTO_INCREMENT,
  empresa_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  competencia_ano_mes VARCHAR(7) NOT NULL,
  plano_conta_id BIGINT NOT NULL,
  centro_custo_id BIGINT NOT NULL DEFAULT 0,
  valor_competencia DECIMAL(15,2) NOT NULL DEFAULT 0,
  valor_realizado_caixa DECIMAL(15,2) NOT NULL DEFAULT 0,
  quantidade_lancamentos BIGINT NOT NULL DEFAULT 0,
  atualizado_em DATETIME NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_resumo_comp_chave UNIQUE (empresa_id, filial_id, competencia_ano_mes, plano_conta_id, centro_custo_id),
  INDEX idx_resumo_comp_tenant_mes (empresa_id, filial_id, competencia_ano_mes)
);

-- 5. Vinculo nullable de plano_conta_id e centro_custo_id nas tabelas operacionais + _aud e indices
DROP PROCEDURE IF EXISTS upgrade_v14_fundacao_financeira;

DELIMITER $$
CREATE PROCEDURE upgrade_v14_fundacao_financeira()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'tipo_despesa' AND column_name = 'plano_conta_id') THEN
    ALTER TABLE tipo_despesa ADD COLUMN plano_conta_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'tipo_despesa' AND column_name = 'centro_custo_id') THEN
    ALTER TABLE tipo_despesa ADD COLUMN centro_custo_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'tipo_despesa_aud' AND column_name = 'plano_conta_id') THEN
    ALTER TABLE tipo_despesa_aud ADD COLUMN plano_conta_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'tipo_despesa_aud' AND column_name = 'centro_custo_id') THEN
    ALTER TABLE tipo_despesa_aud ADD COLUMN centro_custo_id BIGINT;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'despesas' AND column_name = 'plano_conta_id') THEN
    ALTER TABLE despesas ADD COLUMN plano_conta_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'despesas' AND column_name = 'centro_custo_id') THEN
    ALTER TABLE despesas ADD COLUMN centro_custo_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'despesas_aud' AND column_name = 'plano_conta_id') THEN
    ALTER TABLE despesas_aud ADD COLUMN plano_conta_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'despesas_aud' AND column_name = 'centro_custo_id') THEN
    ALTER TABLE despesas_aud ADD COLUMN centro_custo_id BIGINT;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'conta_pagar' AND column_name = 'plano_conta_id') THEN
    ALTER TABLE conta_pagar ADD COLUMN plano_conta_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'conta_pagar' AND column_name = 'centro_custo_id') THEN
    ALTER TABLE conta_pagar ADD COLUMN centro_custo_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'conta_pagar_aud' AND column_name = 'plano_conta_id') THEN
    ALTER TABLE conta_pagar_aud ADD COLUMN plano_conta_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'conta_pagar_aud' AND column_name = 'centro_custo_id') THEN
    ALTER TABLE conta_pagar_aud ADD COLUMN centro_custo_id BIGINT;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'conta_receber' AND column_name = 'plano_conta_id') THEN
    ALTER TABLE conta_receber ADD COLUMN plano_conta_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'conta_receber' AND column_name = 'centro_custo_id') THEN
    ALTER TABLE conta_receber ADD COLUMN centro_custo_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'conta_receber_aud' AND column_name = 'plano_conta_id') THEN
    ALTER TABLE conta_receber_aud ADD COLUMN plano_conta_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'conta_receber_aud' AND column_name = 'centro_custo_id') THEN
    ALTER TABLE conta_receber_aud ADD COLUMN centro_custo_id BIGINT;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'categoria' AND column_name = 'plano_conta_id') THEN
    ALTER TABLE categoria ADD COLUMN plano_conta_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'categoria' AND column_name = 'centro_custo_id') THEN
    ALTER TABLE categoria ADD COLUMN centro_custo_id BIGINT;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'nota_fiscal_entrada' AND column_name = 'plano_conta_id') THEN
    ALTER TABLE nota_fiscal_entrada ADD COLUMN plano_conta_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'nota_fiscal_entrada' AND column_name = 'centro_custo_id') THEN
    ALTER TABLE nota_fiscal_entrada ADD COLUMN centro_custo_id BIGINT;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'venda' AND column_name = 'plano_conta_id') THEN
    ALTER TABLE venda ADD COLUMN plano_conta_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'venda' AND column_name = 'centro_custo_id') THEN
    ALTER TABLE venda ADD COLUMN centro_custo_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'venda_aud' AND column_name = 'plano_conta_id') THEN
    ALTER TABLE venda_aud ADD COLUMN plano_conta_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'venda_aud' AND column_name = 'centro_custo_id') THEN
    ALTER TABLE venda_aud ADD COLUMN centro_custo_id BIGINT;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'tipo_despesa' AND index_name = 'idx_tipo_despesa_classificacao') THEN
    CREATE INDEX idx_tipo_despesa_classificacao ON tipo_despesa(empresa_id, filial_id, plano_conta_id, centro_custo_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'despesas' AND index_name = 'idx_despesas_classificacao') THEN
    CREATE INDEX idx_despesas_classificacao ON despesas(empresa_id, filial_id, plano_conta_id, centro_custo_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'conta_pagar' AND index_name = 'idx_conta_pagar_classificacao') THEN
    CREATE INDEX idx_conta_pagar_classificacao ON conta_pagar(empresa_id, filial_id, plano_conta_id, centro_custo_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'conta_receber' AND index_name = 'idx_conta_receber_classificacao') THEN
    CREATE INDEX idx_conta_receber_classificacao ON conta_receber(empresa_id, filial_id, plano_conta_id, centro_custo_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'categoria' AND index_name = 'idx_categoria_classificacao') THEN
    CREATE INDEX idx_categoria_classificacao ON categoria(empresa_id, filial_id, plano_conta_id, centro_custo_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'nota_fiscal_entrada' AND index_name = 'idx_nf_entrada_classificacao') THEN
    CREATE INDEX idx_nf_entrada_classificacao ON nota_fiscal_entrada(empresa_id, filial_id, plano_conta_id, centro_custo_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'venda' AND index_name = 'idx_venda_classificacao') THEN
    CREATE INDEX idx_venda_classificacao ON venda(empresa_id, filial_id, plano_conta_id, centro_custo_id);
  END IF;
END $$
DELIMITER ;

CALL upgrade_v14_fundacao_financeira();
DROP PROCEDURE IF EXISTS upgrade_v14_fundacao_financeira;
