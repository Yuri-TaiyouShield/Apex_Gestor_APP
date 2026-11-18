-- Variante MySQL para execucao limpa e validada com Testcontainers.
-- Consolida o fluxo operacional venda -> Caixa, devolucoes e financeiro inicial.
-- Colunas de tabelas legadas permanecem nullable para backfill seguro; uma migration
-- futura podera aplicar NOT NULL depois da validacao dos dados existentes.

CREATE TABLE IF NOT EXISTS venda (
  id_venda BIGINT PRIMARY KEY AUTO_INCREMENT
);

ALTER TABLE venda ADD COLUMN status_operacional VARCHAR(24);
ALTER TABLE venda ADD COLUMN numero_venda VARCHAR(40);
ALTER TABLE venda ADD COLUMN empresa_id BIGINT;
ALTER TABLE venda ADD COLUMN filial_id BIGINT;
ALTER TABLE venda ADD COLUMN usuario_id BIGINT;
ALTER TABLE venda ADD COLUMN cliente_id BIGINT;
ALTER TABLE venda ADD COLUMN caixa_sessao_id BIGINT;
ALTER TABLE venda ADD COLUMN condicao_pagamento_id BIGINT;
ALTER TABLE venda ADD COLUMN forma_pagamento_real_id BIGINT;
ALTER TABLE venda ADD COLUMN valor_recebido DECIMAL(12,2);
ALTER TABLE venda ADD COLUMN troco DECIMAL(12,2);
ALTER TABLE venda ADD COLUMN diferenca DECIMAL(12,2);
ALTER TABLE venda ADD COLUMN enviado_caixa_em DATETIME;
ALTER TABLE venda ADD COLUMN recebido_caixa_em DATETIME;
ALTER TABLE venda ADD COLUMN finalizado_em DATETIME;
ALTER TABLE venda ADD COLUMN motivo_recusa VARCHAR(255);
ALTER TABLE venda ADD COLUMN motivo_cancelamento VARCHAR(255);
ALTER TABLE venda ADD COLUMN venda_original_id BIGINT;
ALTER TABLE venda ADD COLUMN tipo_devolucao VARCHAR(16);
ALTER TABLE venda ADD COLUMN motivo_devolucao VARCHAR(255);
ALTER TABLE venda ADD COLUMN estoque_movimentado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_venda_tenant_status ON venda(empresa_id, filial_id, status_operacional);
CREATE INDEX idx_venda_tenant_vendedor ON venda(empresa_id, filial_id, usuario_id);
CREATE INDEX idx_venda_tenant_caixa ON venda(empresa_id, filial_id, caixa_sessao_id);
CREATE INDEX idx_venda_tenant_numero ON venda(empresa_id, filial_id, numero_venda);
CREATE INDEX idx_venda_tenant_data ON venda(empresa_id, filial_id, finalizado_em);

ALTER TABLE caixa_sessao ADD COLUMN saldo_informado DECIMAL(12,2);
ALTER TABLE caixa_sessao ADD COLUMN diferenca_fechamento DECIMAL(12,2);

CREATE TABLE IF NOT EXISTS venda_evento (
  id_venda_evento BIGINT PRIMARY KEY AUTO_INCREMENT,
  venda_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  evento VARCHAR(40) NOT NULL,
  status_anterior VARCHAR(24),
  status_novo VARCHAR(24),
  motivo VARCHAR(255),
  usuario_login VARCHAR(80),
  criado_em DATETIME NOT NULL,
  INDEX idx_venda_evento_venda (empresa_id, filial_id, venda_id, criado_em)
);

CREATE TABLE IF NOT EXISTS meta_vendedor (
  id_meta_vendedor BIGINT PRIMARY KEY AUTO_INCREMENT,
  usuario_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  periodo_inicio DATE NOT NULL,
  periodo_fim DATE NOT NULL,
  valor_meta DECIMAL(12,2) NOT NULL,
  INDEX idx_meta_vendedor_periodo (empresa_id, filial_id, usuario_id, periodo_inicio, periodo_fim)
);

CREATE TABLE IF NOT EXISTS conta_receber (
  id_conta_receber BIGINT PRIMARY KEY AUTO_INCREMENT,
  venda_id BIGINT NOT NULL,
  cliente_id BIGINT,
  forma_pagamento_id BIGINT,
  condicao_pagamento_id BIGINT,
  caixa_sessao_id BIGINT,
  empresa_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  valor_bruto DECIMAL(12,2) NOT NULL,
  desconto DECIMAL(12,2) NOT NULL DEFAULT 0,
  valor_liquido DECIMAL(12,2) NOT NULL,
  vencimento DATE NOT NULL,
  status VARCHAR(16) NOT NULL,
  criado_em DATETIME NOT NULL,
  pago_em DATETIME,
  INDEX idx_conta_receber_tenant_status (empresa_id, filial_id, status, vencimento),
  INDEX idx_conta_receber_venda (empresa_id, filial_id, venda_id)
);

CREATE TABLE IF NOT EXISTS conta_pagar (
  id_conta_pagar BIGINT PRIMARY KEY AUTO_INCREMENT,
  fornecedor_id BIGINT,
  categoria_id BIGINT,
  empresa_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  descricao VARCHAR(160) NOT NULL,
  valor DECIMAL(12,2) NOT NULL,
  vencimento DATE NOT NULL,
  forma_pagamento_prevista_id BIGINT,
  status VARCHAR(16) NOT NULL,
  criado_em DATETIME NOT NULL,
  pago_em DATETIME,
  INDEX idx_conta_pagar_tenant_status (empresa_id, filial_id, status, vencimento)
);

CREATE TABLE IF NOT EXISTS venda_devolucao (
  id_venda_devolucao BIGINT PRIMARY KEY AUTO_INCREMENT,
  venda_original_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  tipo VARCHAR(16) NOT NULL,
  motivo VARCHAR(255) NOT NULL,
  valor_ajuste DECIMAL(12,2) NOT NULL DEFAULT 0,
  criado_por VARCHAR(80),
  criado_em DATETIME NOT NULL,
  INDEX idx_devolucao_venda (empresa_id, filial_id, venda_original_id, criado_em)
);

CREATE TABLE IF NOT EXISTS venda_devolucao_item (
  id_venda_devolucao_item BIGINT PRIMARY KEY AUTO_INCREMENT,
  devolucao_id BIGINT NOT NULL,
  produto_original_id BIGINT NOT NULL,
  produto_novo_id BIGINT,
  quantidade INT NOT NULL,
  valor_unitario DECIMAL(12,2) NOT NULL,
  CONSTRAINT fk_devolucao_item_devolucao FOREIGN KEY (devolucao_id)
    REFERENCES venda_devolucao(id_venda_devolucao)
);

CREATE TABLE IF NOT EXISTS endereco (
  id_endereco BIGINT PRIMARY KEY AUTO_INCREMENT,
  cep VARCHAR(9)
);
ALTER TABLE endereco ADD COLUMN empresa_id BIGINT;
ALTER TABLE endereco ADD COLUMN filial_id BIGINT;

CREATE TABLE IF NOT EXISTS nota_fiscal_entrada (
  id_nf_entrada BIGINT PRIMARY KEY AUTO_INCREMENT
);
ALTER TABLE nota_fiscal_entrada ADD COLUMN empresa_id BIGINT;
ALTER TABLE nota_fiscal_entrada ADD COLUMN filial_id BIGINT;
