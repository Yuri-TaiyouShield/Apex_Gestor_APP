CREATE TABLE IF NOT EXISTS Caixa_Sessao (
  id_caixa_sessao BIGINT PRIMARY KEY AUTO_INCREMENT,
  operador_login VARCHAR(80) NOT NULL,
  empresa_id BIGINT,
  filial_id BIGINT,
  status VARCHAR(16) NOT NULL,
  saldo_inicial DECIMAL(12, 2) NOT NULL DEFAULT 0,
  total_vendas DECIMAL(12, 2) NOT NULL DEFAULT 0,
  total_suprimentos DECIMAL(12, 2) NOT NULL DEFAULT 0,
  total_sangrias DECIMAL(12, 2) NOT NULL DEFAULT 0,
  saldo_final DECIMAL(12, 2),
  aberto_em DATETIME NOT NULL,
  fechado_em DATETIME
);

CREATE TABLE IF NOT EXISTS Caixa_Movimentacao (
  id_caixa_movimentacao BIGINT PRIMARY KEY AUTO_INCREMENT,
  caixa_sessao_id BIGINT NOT NULL,
  tipo VARCHAR(24) NOT NULL,
  valor DECIMAL(12, 2) NOT NULL,
  observacao VARCHAR(255),
  referencia_tipo VARCHAR(40),
  referencia_id BIGINT,
  criado_em DATETIME NOT NULL,
  CONSTRAINT fk_caixa_movimentacao_sessao
    FOREIGN KEY (caixa_sessao_id) REFERENCES Caixa_Sessao(id_caixa_sessao)
);

CREATE TABLE IF NOT EXISTS Condicoes_Pagamento (
  id_condicao_pagamento BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome VARCHAR(80) NOT NULL,
  tipo_pagamento VARCHAR(40) NOT NULL,
  quantidade_parcelas INT NOT NULL,
  prazo_dias INT NOT NULL,
  percentual DECIMAL(7, 4) NOT NULL DEFAULT 100,
  restricoes VARCHAR(255),
  status INT NOT NULL DEFAULT 1,
  empresa_id BIGINT,
  filial_id BIGINT
);
