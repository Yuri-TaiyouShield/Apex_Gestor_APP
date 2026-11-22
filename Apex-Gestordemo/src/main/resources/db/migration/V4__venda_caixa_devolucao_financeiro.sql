-- Consolida o fluxo operacional Venda -> Caixa, devolucoes e financeiro inicial.
-- Colunas de tabelas legadas permanecem nullable para backfill seguro; uma migration
-- futura podera aplicar NOT NULL depois da validacao dos dados existentes.

CREATE TABLE IF NOT EXISTS Venda (
  id_venda BIGINT PRIMARY KEY AUTO_INCREMENT
);

ALTER TABLE Venda ADD COLUMN IF NOT EXISTS status_operacional VARCHAR(24);
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS numero_venda VARCHAR(40);
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS filial_id BIGINT;
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS usuario_id BIGINT;
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS cliente_id BIGINT;
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS caixa_sessao_id BIGINT;
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS condicao_pagamento_id BIGINT;
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS forma_pagamento_real_id BIGINT;
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS valor_recebido DECIMAL(12,2);
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS troco DECIMAL(12,2);
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS diferenca DECIMAL(12,2);
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS enviado_caixa_em DATETIME;
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS recebido_caixa_em DATETIME;
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS finalizado_em DATETIME;
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS motivo_recusa VARCHAR(255);
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS motivo_cancelamento VARCHAR(255);
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS venda_original_id BIGINT;
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS tipo_devolucao VARCHAR(16);
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS motivo_devolucao VARCHAR(255);
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS estoque_movimentado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_venda_tenant_status ON Venda(empresa_id, filial_id, status_operacional);
CREATE INDEX idx_venda_tenant_vendedor ON Venda(empresa_id, filial_id, usuario_id);
CREATE INDEX idx_venda_tenant_caixa ON Venda(empresa_id, filial_id, caixa_sessao_id);
CREATE INDEX idx_venda_tenant_numero ON Venda(empresa_id, filial_id, numero_venda);
CREATE INDEX idx_venda_tenant_data ON Venda(empresa_id, filial_id, finalizado_em);

ALTER TABLE Caixa_Sessao ADD COLUMN IF NOT EXISTS saldo_informado DECIMAL(12,2);
ALTER TABLE Caixa_Sessao ADD COLUMN IF NOT EXISTS diferenca_fechamento DECIMAL(12,2);

CREATE TABLE IF NOT EXISTS Venda_Evento (
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

CREATE TABLE IF NOT EXISTS Meta_Vendedor (
  id_meta_vendedor BIGINT PRIMARY KEY AUTO_INCREMENT,
  usuario_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  periodo_inicio DATE NOT NULL,
  periodo_fim DATE NOT NULL,
  valor_meta DECIMAL(12,2) NOT NULL,
  INDEX idx_meta_vendedor_periodo (empresa_id, filial_id, usuario_id, periodo_inicio, periodo_fim)
);

CREATE TABLE IF NOT EXISTS Conta_Receber (
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

CREATE TABLE IF NOT EXISTS Conta_Pagar (
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

CREATE TABLE IF NOT EXISTS Venda_Devolucao (
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

CREATE TABLE IF NOT EXISTS Venda_Devolucao_Item (
  id_venda_devolucao_item BIGINT PRIMARY KEY AUTO_INCREMENT,
  devolucao_id BIGINT NOT NULL,
  produto_original_id BIGINT NOT NULL,
  produto_novo_id BIGINT,
  quantidade INT NOT NULL,
  valor_unitario DECIMAL(12,2) NOT NULL,
  CONSTRAINT fk_devolucao_item_devolucao FOREIGN KEY (devolucao_id)
    REFERENCES Venda_Devolucao(id_venda_devolucao)
);

CREATE TABLE IF NOT EXISTS Endereco (
  id_endereco BIGINT PRIMARY KEY AUTO_INCREMENT,
  cep VARCHAR(9)
);
ALTER TABLE Endereco ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE Endereco ADD COLUMN IF NOT EXISTS filial_id BIGINT;

CREATE TABLE IF NOT EXISTS Nota_Fiscal_Entrada (
  id_nf_entrada BIGINT PRIMARY KEY AUTO_INCREMENT
);
ALTER TABLE Nota_Fiscal_Entrada ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE Nota_Fiscal_Entrada ADD COLUMN IF NOT EXISTS filial_id BIGINT;
