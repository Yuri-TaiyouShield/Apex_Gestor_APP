-- ============================================================================
-- V15: Central de Fluxo de Caixa, Contas Financeiras, Movimentacoes e Conciliacao (Lote 3)
-- Compativel com H2 (modo padrao)
-- ============================================================================

-- 1. Contas Financeiras (Caixas Fisicos, Contas Bancarias, Carteiras Digitais, Operadoras de Cartao)
CREATE TABLE IF NOT EXISTS conta_financeira (
  id_conta_financeira BIGINT PRIMARY KEY AUTO_INCREMENT,
  empresa_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  codigo VARCHAR(32) NOT NULL,
  nome VARCHAR(120) NOT NULL,
  tipo_conta VARCHAR(32) NOT NULL,
  banco_codigo VARCHAR(16),
  banco_nome VARCHAR(80),
  agencia VARCHAR(24),
  numero_conta VARCHAR(32),
  saldo_inicial DECIMAL(15,2) NOT NULL DEFAULT 0,
  saldo_atual DECIMAL(15,2) NOT NULL DEFAULT 0,
  data_saldo_inicial DATE NOT NULL,
  padrao BOOLEAN NOT NULL DEFAULT FALSE,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em DATETIME NOT NULL,
  atualizado_em DATETIME,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_conta_fin_tenant_codigo UNIQUE (empresa_id, filial_id, codigo),
  INDEX idx_conta_fin_tenant_ativo (empresa_id, filial_id, ativo, tipo_conta)
);

CREATE TABLE IF NOT EXISTS conta_financeira_aud (
  id_conta_financeira BIGINT NOT NULL,
  REV INT NOT NULL,
  REVTYPE TINYINT NOT NULL,
  empresa_id BIGINT,
  filial_id BIGINT,
  codigo VARCHAR(32),
  nome VARCHAR(120),
  tipo_conta VARCHAR(32),
  banco_codigo VARCHAR(16),
  banco_nome VARCHAR(80),
  agencia VARCHAR(24),
  numero_conta VARCHAR(32),
  saldo_inicial DECIMAL(15,2),
  saldo_atual DECIMAL(15,2),
  data_saldo_inicial DATE,
  padrao BOOLEAN,
  ativo BOOLEAN,
  criado_em DATETIME,
  atualizado_em DATETIME,
  version BIGINT,
  PRIMARY KEY (id_conta_financeira, REV),
  CONSTRAINT fk_conta_fin_aud_revision FOREIGN KEY (REV) REFERENCES audit_revision(revision_id)
);

-- 2. Movimentacoes Financeiras Realizadas (Entradas, Saidas, Transferencias Internas, Liquidacoes Cartao e Estornos)
CREATE TABLE IF NOT EXISTS movimentacao_financeira (
  id_movimentacao_financeira BIGINT PRIMARY KEY AUTO_INCREMENT,
  empresa_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  conta_financeira_id BIGINT NOT NULL,
  tipo_movimentacao VARCHAR(32) NOT NULL,
  origem_tipo VARCHAR(32) NOT NULL,
  origem_id BIGINT,
  descricao VARCHAR(200) NOT NULL,
  documento_referencia VARCHAR(80),
  data_movimentacao DATE NOT NULL,
  data_hora_registro DATETIME NOT NULL,
  valor_bruto DECIMAL(15,2) NOT NULL,
  valor_taxa DECIMAL(15,2) NOT NULL DEFAULT 0,
  valor_juros_desconto DECIMAL(15,2) NOT NULL DEFAULT 0,
  valor_liquido DECIMAL(15,2) NOT NULL,
  plano_conta_id BIGINT,
  centro_custo_id BIGINT,
  transferencia_par_id BIGINT,
  movimentacao_estornada_id BIGINT,
  estornado BOOLEAN NOT NULL DEFAULT FALSE,
  conciliado BOOLEAN NOT NULL DEFAULT FALSE,
  conciliacao_item_id BIGINT,
  idempotency_key VARCHAR(80),
  criado_por VARCHAR(80),
  version BIGINT NOT NULL DEFAULT 0,
  INDEX idx_mov_fin_tenant_data (empresa_id, filial_id, data_movimentacao, tipo_movimentacao),
  INDEX idx_mov_fin_conta_data (empresa_id, filial_id, conta_financeira_id, data_movimentacao),
  INDEX idx_mov_fin_origem (empresa_id, filial_id, origem_tipo, origem_id),
  INDEX idx_mov_fin_idempotency (empresa_id, filial_id, idempotency_key)
);

CREATE TABLE IF NOT EXISTS movimentacao_financeira_aud (
  id_movimentacao_financeira BIGINT NOT NULL,
  REV INT NOT NULL,
  REVTYPE TINYINT NOT NULL,
  empresa_id BIGINT,
  filial_id BIGINT,
  conta_financeira_id BIGINT,
  tipo_movimentacao VARCHAR(32),
  origem_tipo VARCHAR(32),
  origem_id BIGINT,
  descricao VARCHAR(200),
  documento_referencia VARCHAR(80),
  data_movimentacao DATE,
  data_hora_registro DATETIME,
  valor_bruto DECIMAL(15,2),
  valor_taxa DECIMAL(15,2),
  valor_juros_desconto DECIMAL(15,2),
  valor_liquido DECIMAL(15,2),
  plano_conta_id BIGINT,
  centro_custo_id BIGINT,
  transferencia_par_id BIGINT,
  movimentacao_estornada_id BIGINT,
  estornado BOOLEAN,
  conciliado BOOLEAN,
  conciliacao_item_id BIGINT,
  idempotency_key VARCHAR(80),
  criado_por VARCHAR(80),
  version BIGINT,
  PRIMARY KEY (id_movimentacao_financeira, REV),
  CONSTRAINT fk_mov_fin_aud_revision FOREIGN KEY (REV) REFERENCES audit_revision(revision_id)
);

-- 3. Itens de Conciliacao Financeira (Extratos OFX/CSV, Conferencia de Caixa e Operadoras de Cartao)
CREATE TABLE IF NOT EXISTS conciliacao_financeira_item (
  id_conciliacao_item BIGINT PRIMARY KEY AUTO_INCREMENT,
  empresa_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  conta_financeira_id BIGINT NOT NULL,
  tipo_conciliacao VARCHAR(32) NOT NULL,
  formato_origem VARCHAR(16) NOT NULL,
  identificador_extrato VARCHAR(120),
  data_extrato DATE NOT NULL,
  descricao_extrato VARCHAR(200) NOT NULL,
  valor_extrato DECIMAL(15,2) NOT NULL,
  natureza_extrato VARCHAR(16) NOT NULL,
  status_conciliacao VARCHAR(32) NOT NULL,
  movimentacao_vinculada_id BIGINT,
  documento_origem_tipo VARCHAR(32),
  documento_origem_id BIGINT,
  diferenca_valor DECIMAL(15,2) NOT NULL DEFAULT 0,
  observacao VARCHAR(255),
  conciliado_por VARCHAR(80),
  conciliado_em DATETIME,
  criado_em DATETIME NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  INDEX idx_conc_fin_tenant_status (empresa_id, filial_id, conta_financeira_id, status_conciliacao, data_extrato)
);

CREATE TABLE IF NOT EXISTS conciliacao_financeira_item_aud (
  id_conciliacao_item BIGINT NOT NULL,
  REV INT NOT NULL,
  REVTYPE TINYINT NOT NULL,
  empresa_id BIGINT,
  filial_id BIGINT,
  conta_financeira_id BIGINT,
  tipo_conciliacao VARCHAR(32),
  formato_origem VARCHAR(16),
  identificador_extrato VARCHAR(120),
  data_extrato DATE,
  descricao_extrato VARCHAR(200),
  valor_extrato DECIMAL(15,2),
  natureza_extrato VARCHAR(16),
  status_conciliacao VARCHAR(32),
  movimentacao_vinculada_id BIGINT,
  documento_origem_tipo VARCHAR(32),
  documento_origem_id BIGINT,
  diferenca_valor DECIMAL(15,2),
  observacao VARCHAR(255),
  conciliado_por VARCHAR(80),
  conciliado_em DATETIME,
  criado_em DATETIME,
  version BIGINT,
  PRIMARY KEY (id_conciliacao_item, REV),
  CONSTRAINT fk_conc_fin_aud_revision FOREIGN KEY (REV) REFERENCES audit_revision(revision_id)
);

-- 4. Evolucao de Conta_Pagar (pagamentos parciais, parcelamentos, recorrencia e vinculo de conta financeira)
ALTER TABLE conta_pagar ADD COLUMN IF NOT EXISTS valor_pago DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE conta_pagar ADD COLUMN IF NOT EXISTS saldo DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE conta_pagar ADD COLUMN IF NOT EXISTS numero_parcela INT NOT NULL DEFAULT 1;
ALTER TABLE conta_pagar ADD COLUMN IF NOT EXISTS total_parcelas INT NOT NULL DEFAULT 1;
ALTER TABLE conta_pagar ADD COLUMN IF NOT EXISTS grupo_parcelamento VARCHAR(64);
ALTER TABLE conta_pagar ADD COLUMN IF NOT EXISTS recorrente BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE conta_pagar ADD COLUMN IF NOT EXISTS periodicidade VARCHAR(24);
ALTER TABLE conta_pagar ADD COLUMN IF NOT EXISTS conta_financeira_id BIGINT;

ALTER TABLE conta_pagar_aud ADD COLUMN IF NOT EXISTS valor_pago DECIMAL(12,2);
ALTER TABLE conta_pagar_aud ADD COLUMN IF NOT EXISTS saldo DECIMAL(12,2);
ALTER TABLE conta_pagar_aud ADD COLUMN IF NOT EXISTS numero_parcela INT;
ALTER TABLE conta_pagar_aud ADD COLUMN IF NOT EXISTS total_parcelas INT;
ALTER TABLE conta_pagar_aud ADD COLUMN IF NOT EXISTS grupo_parcelamento VARCHAR(64);
ALTER TABLE conta_pagar_aud ADD COLUMN IF NOT EXISTS recorrente BOOLEAN;
ALTER TABLE conta_pagar_aud ADD COLUMN IF NOT EXISTS periodicidade VARCHAR(24);
ALTER TABLE conta_pagar_aud ADD COLUMN IF NOT EXISTS conta_financeira_id BIGINT;

UPDATE conta_pagar
SET valor_pago = CASE WHEN status = 'PAGO' THEN valor ELSE 0 END,
    saldo = CASE WHEN status IN ('PAGO', 'CANCELADO', 'DEVOLVIDO') THEN 0 ELSE valor END
WHERE valor_pago = 0 AND saldo = 0;

-- 5. Evolucao de Conta_Receber (parcelas multiplas, recebiveis avulsos, taxas de cartao, antecipacao e repasse)
ALTER TABLE conta_receber DROP CONSTRAINT IF EXISTS uk_conta_receber_tenant_venda;
ALTER TABLE conta_receber ALTER COLUMN venda_id DROP NOT NULL;

ALTER TABLE conta_receber ADD COLUMN IF NOT EXISTS descricao VARCHAR(160);
ALTER TABLE conta_receber ADD COLUMN IF NOT EXISTS numero_parcela INT NOT NULL DEFAULT 1;
ALTER TABLE conta_receber ADD COLUMN IF NOT EXISTS total_parcelas INT NOT NULL DEFAULT 1;
ALTER TABLE conta_receber ADD COLUMN IF NOT EXISTS grupo_parcelamento VARCHAR(64);
ALTER TABLE conta_receber ADD COLUMN IF NOT EXISTS modalidade_recebivel VARCHAR(32) NOT NULL DEFAULT 'DIRETO';
ALTER TABLE conta_receber ADD COLUMN IF NOT EXISTS taxa_percentual DECIMAL(7,4) NOT NULL DEFAULT 0;
ALTER TABLE conta_receber ADD COLUMN IF NOT EXISTS valor_taxa DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE conta_receber ADD COLUMN IF NOT EXISTS taxa_antecipacao DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE conta_receber ADD COLUMN IF NOT EXISTS data_prevista_repasse DATE;
ALTER TABLE conta_receber ADD COLUMN IF NOT EXISTS antecipado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE conta_receber ADD COLUMN IF NOT EXISTS conta_financeira_id BIGINT;

ALTER TABLE conta_receber_aud ADD COLUMN IF NOT EXISTS descricao VARCHAR(160);
ALTER TABLE conta_receber_aud ADD COLUMN IF NOT EXISTS numero_parcela INT;
ALTER TABLE conta_receber_aud ADD COLUMN IF NOT EXISTS total_parcelas INT;
ALTER TABLE conta_receber_aud ADD COLUMN IF NOT EXISTS grupo_parcelamento VARCHAR(64);
ALTER TABLE conta_receber_aud ADD COLUMN IF NOT EXISTS modalidade_recebivel VARCHAR(32);
ALTER TABLE conta_receber_aud ADD COLUMN IF NOT EXISTS taxa_percentual DECIMAL(7,4);
ALTER TABLE conta_receber_aud ADD COLUMN IF NOT EXISTS valor_taxa DECIMAL(12,2);
ALTER TABLE conta_receber_aud ADD COLUMN IF NOT EXISTS taxa_antecipacao DECIMAL(12,2);
ALTER TABLE conta_receber_aud ADD COLUMN IF NOT EXISTS data_prevista_repasse DATE;
ALTER TABLE conta_receber_aud ADD COLUMN IF NOT EXISTS antecipado BOOLEAN;
ALTER TABLE conta_receber_aud ADD COLUMN IF NOT EXISTS conta_financeira_id BIGINT;

UPDATE conta_receber
SET data_prevista_repasse = vencimento
WHERE data_prevista_repasse IS NULL;
