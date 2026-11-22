-- Pagamento misto e credito/vale com origem rastreavel.
-- Estrutura de producao; nenhuma massa de homologacao e inserida por Flyway.

CREATE TABLE IF NOT EXISTS Venda_FormasPagamento (
  venda_id BIGINT NOT NULL,
  forma_pagamento_id BIGINT NOT NULL,
  PRIMARY KEY (venda_id, forma_pagamento_id)
);
ALTER TABLE Venda_FormasPagamento ADD COLUMN IF NOT EXISTS credito_id BIGINT;
ALTER TABLE Conta_Receber ADD COLUMN IF NOT EXISTS valor_pago DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE Conta_Receber ADD COLUMN IF NOT EXISTS saldo DECIMAL(12,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS Credito_Cliente (
  id_credito BIGINT PRIMARY KEY AUTO_INCREMENT,
  cliente_id BIGINT NOT NULL,
  venda_origem_id BIGINT NOT NULL,
  devolucao_id BIGINT NOT NULL,
  empresa_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  valor_original DECIMAL(12,2) NOT NULL,
  saldo DECIMAL(12,2) NOT NULL,
  status VARCHAR(16) NOT NULL,
  criado_em DATETIME NOT NULL,
  INDEX idx_credito_cliente_tenant (empresa_id, filial_id, cliente_id, status),
  INDEX idx_credito_origem (empresa_id, filial_id, venda_origem_id, devolucao_id)
);

CREATE TABLE IF NOT EXISTS Credito_Movimentacao (
  id_credito_movimentacao BIGINT PRIMARY KEY AUTO_INCREMENT,
  credito_id BIGINT NOT NULL,
  venda_id BIGINT,
  empresa_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  tipo VARCHAR(16) NOT NULL,
  valor DECIMAL(12,2) NOT NULL,
  usuario_login VARCHAR(80),
  criado_em DATETIME NOT NULL,
  CONSTRAINT fk_credito_movimentacao_credito FOREIGN KEY (credito_id)
    REFERENCES Credito_Cliente(id_credito),
  INDEX idx_credito_movimento (empresa_id, filial_id, credito_id, criado_em)
);
