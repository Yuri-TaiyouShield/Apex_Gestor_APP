-- Apex Gestor v1 operacional core.
-- Preparacao para Flyway/Liquibase: hoje o ambiente local ainda usa spring.jpa.hibernate.ddl-auto=update.
-- Esta migration documenta a estrutura nova sem ser aplicada automaticamente nesta etapa.

ALTER TABLE Cliente
  ADD COLUMN IF NOT EXISTS empresa_id BIGINT NULL,
  ADD COLUMN IF NOT EXISTS filial_id BIGINT NULL;

CREATE INDEX IF NOT EXISTS idx_cliente_escopo
  ON Cliente (empresa_id, filial_id, nome_razao);

CREATE TABLE IF NOT EXISTS Caixa_Sessao (
  id_caixa_sessao BIGINT PRIMARY KEY AUTO_INCREMENT,
  operador_login VARCHAR(80) NOT NULL,
  empresa_id BIGINT NULL,
  filial_id BIGINT NULL,
  status VARCHAR(16) NOT NULL,
  saldo_inicial DECIMAL(12, 2) NOT NULL DEFAULT 0,
  total_vendas DECIMAL(12, 2) NOT NULL DEFAULT 0,
  total_suprimentos DECIMAL(12, 2) NOT NULL DEFAULT 0,
  total_sangrias DECIMAL(12, 2) NOT NULL DEFAULT 0,
  saldo_final DECIMAL(12, 2) NULL,
  aberto_em DATETIME NOT NULL,
  fechado_em DATETIME NULL
);

CREATE INDEX IF NOT EXISTS idx_caixa_sessao_operador_status
  ON Caixa_Sessao (operador_login, empresa_id, filial_id, status, aberto_em);

CREATE TABLE IF NOT EXISTS Caixa_Movimentacao (
  id_caixa_movimentacao BIGINT PRIMARY KEY AUTO_INCREMENT,
  caixa_sessao_id BIGINT NOT NULL,
  tipo VARCHAR(24) NOT NULL,
  valor DECIMAL(12, 2) NOT NULL,
  observacao VARCHAR(255) NULL,
  referencia_tipo VARCHAR(40) NULL,
  referencia_id BIGINT NULL,
  criado_em DATETIME NOT NULL,
  CONSTRAINT fk_caixa_movimentacao_sessao
    FOREIGN KEY (caixa_sessao_id) REFERENCES Caixa_Sessao(id_caixa_sessao)
);

CREATE INDEX IF NOT EXISTS idx_caixa_movimentacao_sessao
  ON Caixa_Movimentacao (caixa_sessao_id, criado_em);

CREATE TABLE IF NOT EXISTS Condicoes_Pagamento (
  id_condicao_pagamento BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome VARCHAR(80) NOT NULL,
  tipo_pagamento VARCHAR(40) NOT NULL,
  quantidade_parcelas INT NOT NULL,
  prazo_dias INT NOT NULL,
  percentual DECIMAL(7, 4) NOT NULL DEFAULT 100,
  restricoes VARCHAR(255) NULL,
  status INT NOT NULL DEFAULT 1,
  empresa_id BIGINT NULL,
  filial_id BIGINT NULL
);

CREATE INDEX IF NOT EXISTS idx_condicoes_pagamento_escopo
  ON Condicoes_Pagamento (empresa_id, filial_id, nome);
