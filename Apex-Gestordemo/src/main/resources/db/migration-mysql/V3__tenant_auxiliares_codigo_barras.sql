-- Variante MySQL para execucao limpa e validada com Testcontainers.
-- V3 consolida escopo de empresa/filial em cadastros auxiliares e prepara
-- a regra operacional de codigo de barras por tenant sem alteracoes destrutivas.
-- Campos ficam nullable nesta fase para permitir backfill seguro em bases legadas.

CREATE TABLE IF NOT EXISTS categoria (
  id_categoria BIGINT PRIMARY KEY AUTO_INCREMENT,
  descricao VARCHAR(45),
  status INT NOT NULL DEFAULT 1,
  empresa_id BIGINT,
  filial_id BIGINT
);


CREATE INDEX idx_categoria_tenant_descricao
  ON categoria(empresa_id, filial_id, descricao);

CREATE TABLE IF NOT EXISTS grupo_produto (
  id_grupo_produto BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome VARCHAR(80) NOT NULL,
  descricao VARCHAR(255),
  status INT NOT NULL DEFAULT 1,
  empresa_id BIGINT,
  filial_id BIGINT
);


CREATE INDEX idx_grupo_produto_tenant_nome
  ON grupo_produto(empresa_id, filial_id, nome);

CREATE TABLE IF NOT EXISTS formas_pagamento (
  id_forma_pagamento BIGINT PRIMARY KEY AUTO_INCREMENT,
  descricao TEXT NOT NULL,
  nome VARCHAR(50) NOT NULL,
  status INT NOT NULL DEFAULT 1,
  tipo_pagamento INT NOT NULL,
  empresa_id BIGINT,
  filial_id BIGINT
);


CREATE INDEX idx_formas_pagamento_tenant_nome
  ON formas_pagamento(empresa_id, filial_id, nome);

CREATE TABLE IF NOT EXISTS produtos (
  id_produto BIGINT PRIMARY KEY AUTO_INCREMENT,
  codigo_barras VARCHAR(255),
  empresa_id BIGINT,
  filial_id BIGINT
);


CREATE INDEX idx_produto_tenant_codigo_barras
  ON produtos(empresa_id, filial_id, codigo_barras);
