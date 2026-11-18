-- V9: tabelas do contrato JPA atual que nao existem na baseline legada.
-- Nao substitui nem renomeia tabelas legadas; cria os nomes usados pelo modelo atual.

CREATE TABLE IF NOT EXISTS Auditoria_Log (
  id_auditoria BIGINT PRIMARY KEY AUTO_INCREMENT,
  usuario_id BIGINT,
  usuario_login VARCHAR(80),
  empresa_id BIGINT,
  filial_id BIGINT,
  modulo VARCHAR(80),
  acao VARCHAR(80) NOT NULL,
  entidade VARCHAR(80) NOT NULL,
  entidade_id BIGINT,
  valor_anterior TEXT,
  valor_novo TEXT,
  ip VARCHAR(45),
  dispositivo VARCHAR(160),
  trace_id VARCHAR(80),
  justificativa VARCHAR(255),
  criado_em DATETIME NOT NULL,
  INDEX idx_auditoria_log_tenant_data (empresa_id, filial_id, criado_em),
  INDEX idx_auditoria_log_entidade (entidade, entidade_id)
);

CREATE TABLE IF NOT EXISTS Consentimento_Privacidade (
  id_consentimento BIGINT PRIMARY KEY AUTO_INCREMENT,
  titular_id VARCHAR(80) NOT NULL,
  tipo_titular VARCHAR(40) NOT NULL,
  documento_hash VARCHAR(128),
  finalidade VARCHAR(120) NOT NULL,
  versao_termo VARCHAR(20) NOT NULL,
  origem VARCHAR(40) NOT NULL,
  status VARCHAR(20) NOT NULL,
  aceito_em DATETIME NOT NULL,
  revogado_em DATETIME,
  empresa_id BIGINT,
  filial_id BIGINT,
  INDEX idx_consentimento_privacidade_tenant (empresa_id, filial_id, titular_id),
  INDEX idx_consentimento_privacidade_status (status, aceito_em)
);

CREATE TABLE IF NOT EXISTS Estoque_Movimentacao (
  id_movimentacao BIGINT PRIMARY KEY AUTO_INCREMENT,
  produto_id BIGINT NOT NULL,
  tipo VARCHAR(30) NOT NULL,
  quantidade INT NOT NULL,
  saldo_anterior INT NOT NULL,
  saldo_posterior INT NOT NULL,
  referencia_tipo VARCHAR(60),
  referencia_id BIGINT,
  motivo VARCHAR(255),
  usuario_id BIGINT,
  empresa_id BIGINT,
  filial_id BIGINT,
  criado_em DATETIME NOT NULL,
  INDEX idx_estoque_movimentacao_tenant_data (empresa_id, filial_id, criado_em),
  INDEX idx_estoque_movimentacao_produto_data (produto_id, criado_em)
);

CREATE TABLE IF NOT EXISTS Notificacao (
  id_notificacao BIGINT PRIMARY KEY AUTO_INCREMENT,
  tipo VARCHAR(60) NOT NULL,
  titulo VARCHAR(120) NOT NULL,
  mensagem VARCHAR(500) NOT NULL,
  status VARCHAR(30) NOT NULL,
  entidade_tipo VARCHAR(60),
  entidade_id BIGINT,
  empresa_id BIGINT,
  filial_id BIGINT,
  criado_em DATETIME NOT NULL,
  lido_em DATETIME,
  INDEX idx_notificacao_tenant_status_data (empresa_id, filial_id, status, criado_em)
);

CREATE TABLE IF NOT EXISTS Solicitacao_Titular_Dados (
  id_solicitacao BIGINT PRIMARY KEY AUTO_INCREMENT,
  tipo_titular VARCHAR(40) NOT NULL,
  titular_id VARCHAR(80) NOT NULL,
  tipo VARCHAR(40) NOT NULL,
  status VARCHAR(30) NOT NULL,
  justificativa VARCHAR(500),
  solicitado_por VARCHAR(80),
  executado_por VARCHAR(80),
  solicitado_em DATETIME NOT NULL,
  executado_em DATETIME,
  empresa_id BIGINT,
  filial_id BIGINT,
  INDEX idx_solicitacao_titular_tenant (empresa_id, filial_id, titular_id),
  INDEX idx_solicitacao_titular_status (status, solicitado_em)
);

-- Contratos JPA legados que nao tinham uma migration ativa na cadeia V2-V8.
-- As tabelas sao criadas sem dados de homologacao; colunas de tabelas existentes
-- sao adicionadas de forma forward-only para permitir validacao sem ddl-auto.
CREATE TABLE IF NOT EXISTS Empresa (
  id_empresa BIGINT PRIMARY KEY AUTO_INCREMENT,
  razao_social VARCHAR(120) NOT NULL,
  nome_fantasia VARCHAR(120),
  cnpj VARCHAR(18),
  status INT NOT NULL DEFAULT 1,
  criado_em DATETIME NOT NULL,
  deleted_at DATETIME,
  deleted_by BIGINT
);

CREATE TABLE IF NOT EXISTS Filial (
  id_filial BIGINT PRIMARY KEY AUTO_INCREMENT,
  empresa_id BIGINT NOT NULL,
  nome VARCHAR(80) NOT NULL,
  codigo VARCHAR(30),
  status INT NOT NULL DEFAULT 1,
  criado_em DATETIME NOT NULL,
  deleted_at DATETIME,
  deleted_by BIGINT,
  INDEX idx_filial_empresa_status (empresa_id, status)
);

CREATE TABLE IF NOT EXISTS perfil (
  id_perfil BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome VARCHAR(45) NOT NULL,
  status INT NOT NULL DEFAULT 1,
  CONSTRAINT uk_perfil_nome UNIQUE (nome)
);

CREATE TABLE IF NOT EXISTS menu (
  id_menu BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome VARCHAR(45) NOT NULL,
  link VARCHAR(100) NOT NULL,
  icone VARCHAR(45),
  exibir INT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS menu_perfil (
  menu_id BIGINT NOT NULL,
  perfil_id BIGINT NOT NULL,
  PRIMARY KEY (menu_id, perfil_id)
);

CREATE TABLE IF NOT EXISTS usuario (
  id_usuario BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome VARCHAR(45) NOT NULL,
  login VARCHAR(45) NOT NULL,
  senha VARCHAR(255) NOT NULL,
  data_nascimento DATE NOT NULL,
  status INT NOT NULL DEFAULT 1,
  empresa_id BIGINT,
  filial_id BIGINT,
  perfil_id BIGINT NOT NULL,
  CONSTRAINT uk_usuario_login UNIQUE (login),
  INDEX idx_usuario_tenant_login (empresa_id, filial_id, login)
);

CREATE TABLE IF NOT EXISTS endereco (
  id_endereco BIGINT PRIMARY KEY AUTO_INCREMENT,
  estado VARCHAR(45),
  cidade VARCHAR(45),
  cep VARCHAR(9),
  bairro VARCHAR(100),
  logradouro VARCHAR(100),
  uf VARCHAR(2),
  numero VARCHAR(10),
  complemento VARCHAR(100),
  empresa_id BIGINT,
  filial_id BIGINT
);

CREATE TABLE IF NOT EXISTS Cliente (
  id_cliente BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome_razao VARCHAR(100) NOT NULL,
  nome_fantasia VARCHAR(100),
  telefone VARCHAR(15) NOT NULL,
  celular VARCHAR(15),
  email VARCHAR(120),
  tipo_documento INT NOT NULL DEFAULT 0,
  cpf_cnpj VARCHAR(14) NOT NULL,
  rg_ie VARCHAR(20),
  situacao_fiscal VARCHAR(60),
  observacoes VARCHAR(500),
  data_cadastro DATETIME,
  status INT NOT NULL DEFAULT 1,
  empresa_id BIGINT,
  filial_id BIGINT,
  deleted_at DATETIME,
  deleted_by VARCHAR(80),
  anonimizado_em DATETIME,
  anonimizado_por VARCHAR(80),
  motivo_retencao VARCHAR(255),
  base_legal VARCHAR(120),
  CONSTRAINT uk_cliente_documento UNIQUE (tipo_documento, cpf_cnpj),
  INDEX idx_cliente_tenant_status (empresa_id, filial_id, status)
);

CREATE TABLE IF NOT EXISTS Cliente_Endereco (
  cliente_id BIGINT NOT NULL,
  endereco_id BIGINT NOT NULL,
  PRIMARY KEY (cliente_id, endereco_id)
);

CREATE TABLE IF NOT EXISTS fornecedor (
  id_fornecedor BIGINT PRIMARY KEY AUTO_INCREMENT,
  status INT NOT NULL DEFAULT 1,
  razao_social VARCHAR(100) NOT NULL,
  nome_fantasia VARCHAR(45) NOT NULL,
  cnpj VARCHAR(18) NOT NULL,
  inscricao_estadual VARCHAR(20),
  email VARCHAR(120),
  celular VARCHAR(15),
  situacao_fiscal VARCHAR(60),
  condicoes_pagamento VARCHAR(255),
  dados_bancarios VARCHAR(255),
  observacoes VARCHAR(500),
  empresa_id BIGINT,
  filial_id BIGINT,
  data_cadastro DATETIME NOT NULL,
  telefone VARCHAR(15) NOT NULL,
  endereco_id BIGINT NOT NULL,
  CONSTRAINT uk_fornecedor_cnpj UNIQUE (cnpj),
  INDEX idx_fornecedor_tenant_status (empresa_id, filial_id, status)
);

CREATE TABLE IF NOT EXISTS tipo_despesa (
  id_tipo_despesa BIGINT PRIMARY KEY AUTO_INCREMENT,
  nome VARCHAR(50) NOT NULL,
  status INT NOT NULL DEFAULT 1,
  CONSTRAINT uk_tipo_despesa_nome UNIQUE (nome)
);

CREATE TABLE IF NOT EXISTS despesas (
  id_despesa BIGINT PRIMARY KEY AUTO_INCREMENT,
  descricao VARCHAR(255) NOT NULL,
  valor DECIMAL(10,2) NOT NULL,
  data_vencimento DATE NOT NULL,
  data_pagamento DATE,
  empresa_id BIGINT,
  filial_id BIGINT,
  tipo_despesa_id BIGINT NOT NULL,
  status INT NOT NULL DEFAULT 1,
  INDEX idx_despesas_tenant_status (empresa_id, filial_id, status, data_vencimento)
);

CREATE TABLE IF NOT EXISTS consent_audit (
  id_consent_audit BIGINT PRIMARY KEY AUTO_INCREMENT,
  titular_id VARCHAR(80),
  tipo_titular VARCHAR(30) NOT NULL,
  documento_hash VARCHAR(128),
  versao VARCHAR(40) NOT NULL,
  aceito_em DATETIME NOT NULL,
  canal VARCHAR(40) NOT NULL,
  ip_hash VARCHAR(128),
  user_agent_hash VARCHAR(128)
);

CREATE TABLE IF NOT EXISTS privacy_request (
  id_privacy_request BIGINT PRIMARY KEY AUTO_INCREMENT,
  protocolo VARCHAR(40) NOT NULL,
  titular_id VARCHAR(80) NOT NULL,
  tipo VARCHAR(30) NOT NULL,
  status VARCHAR(30) NOT NULL,
  solicitado_em DATETIME NOT NULL,
  concluido_em DATETIME,
  solicitante_login VARCHAR(80),
  CONSTRAINT uk_privacy_request_protocolo UNIQUE (protocolo)
);

CREATE TABLE IF NOT EXISTS data_erasure_log (
  id_data_erasure_log BIGINT PRIMARY KEY AUTO_INCREMENT,
  titular_hash VARCHAR(128) NOT NULL,
  motivo VARCHAR(120) NOT NULL,
  operador_login VARCHAR(80),
  executado_em DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS produtos (
  id_produto BIGINT PRIMARY KEY AUTO_INCREMENT,
  descricao VARCHAR(60) NOT NULL,
  custo DECIMAL(10,2) NOT NULL DEFAULT 0,
  codigo_barras VARCHAR(20),
  codigo_interno VARCHAR(40),
  codigo_original VARCHAR(60),
  marca VARCHAR(45),
  grupo VARCHAR(60),
  subgrupo VARCHAR(60),
  unidade_medida VARCHAR(10),
  embalagem VARCHAR(40),
  data_aquisicao DATETIME,
  quantidade_estoque INT NOT NULL DEFAULT 0,
  estoque_minimo INT NOT NULL DEFAULT 0,
  valor_venda DECIMAL(10,2) NOT NULL DEFAULT 0,
  margem DECIMAL(10,2),
  localizacao VARCHAR(80),
  situacao_fiscal VARCHAR(60),
  ncm VARCHAR(12),
  cfop VARCHAR(12),
  icms DECIMAL(7,2),
  ipi DECIMAL(7,2),
  pis DECIMAL(7,2),
  cofins DECIMAL(7,2),
  observacoes VARCHAR(500),
  empresa_id BIGINT,
  filial_id BIGINT,
  status INT NOT NULL DEFAULT 1,
  version BIGINT NOT NULL DEFAULT 0,
  fornecedor_id BIGINT,
  categoria_id BIGINT,
  INDEX idx_produtos_tenant_status (empresa_id, filial_id, status),
  INDEX idx_produtos_tenant_codigo (empresa_id, filial_id, codigo_barras)
);

CREATE TABLE IF NOT EXISTS produto_venda (
  produto_id BIGINT NOT NULL,
  venda_id BIGINT NOT NULL,
  quantidade INT NOT NULL,
  preco_unitario DECIMAL(10,2) NOT NULL,
  custo_unitario DECIMAL(10,2) NOT NULL,
  preco_total DECIMAL(10,2),
  PRIMARY KEY (produto_id, venda_id)
);

CREATE TABLE IF NOT EXISTS venda_formas_pagamento (
  venda_id BIGINT NOT NULL,
  forma_pagamento_id BIGINT NOT NULL,
  valor_pago DECIMAL(10,2),
  numero_parcelas INT,
  credito_id BIGINT,
  PRIMARY KEY (venda_id, forma_pagamento_id)
);

CREATE TABLE IF NOT EXISTS item_nota_fiscal (
  id_item_nf BIGINT PRIMARY KEY AUTO_INCREMENT,
  nf_entrada_id BIGINT NOT NULL,
  produto_id BIGINT NOT NULL,
  quantidade INT NOT NULL,
  valor_custo_unitario DECIMAL(10,2) NOT NULL
);

ALTER TABLE Venda ADD COLUMN IF NOT EXISTS status INT NOT NULL DEFAULT 1;
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS valor_total DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS desconto DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS observacao VARCHAR(255);
ALTER TABLE Venda ADD COLUMN IF NOT EXISTS data_venda DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE Endereco ADD COLUMN IF NOT EXISTS estado VARCHAR(45);
ALTER TABLE Endereco ADD COLUMN IF NOT EXISTS cidade VARCHAR(45);
ALTER TABLE Endereco ADD COLUMN IF NOT EXISTS bairro VARCHAR(100);
ALTER TABLE Endereco ADD COLUMN IF NOT EXISTS logradouro VARCHAR(100);
ALTER TABLE Endereco ADD COLUMN IF NOT EXISTS uf VARCHAR(2);
ALTER TABLE Endereco ADD COLUMN IF NOT EXISTS numero VARCHAR(10);
ALTER TABLE Endereco ADD COLUMN IF NOT EXISTS complemento VARCHAR(100);
ALTER TABLE Endereco ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE Endereco ADD COLUMN IF NOT EXISTS filial_id BIGINT;
ALTER TABLE Nota_Fiscal_Entrada ADD COLUMN IF NOT EXISTS numero VARCHAR(20);
ALTER TABLE Nota_Fiscal_Entrada ADD COLUMN IF NOT EXISTS serie VARCHAR(5);
ALTER TABLE Nota_Fiscal_Entrada ADD COLUMN IF NOT EXISTS fornecedor_id BIGINT;
ALTER TABLE Nota_Fiscal_Entrada ADD COLUMN IF NOT EXISTS data_emissao DATETIME;
ALTER TABLE Nota_Fiscal_Entrada ADD COLUMN IF NOT EXISTS data_entrada DATETIME;
ALTER TABLE Nota_Fiscal_Entrada ADD COLUMN IF NOT EXISTS valor_total DECIMAL(10,2);
ALTER TABLE Nota_Fiscal_Entrada ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);
ALTER TABLE Nota_Fiscal_Entrada ADD COLUMN IF NOT EXISTS idempotency_payload_hash VARCHAR(64);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS descricao VARCHAR(60);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS custo DECIMAL(10,2) DEFAULT 0;
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS quantidade_estoque INT DEFAULT 0;
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS estoque_minimo INT DEFAULT 0;
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS valor_venda DECIMAL(10,2) DEFAULT 0;
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS status INT DEFAULT 1;
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS fornecedor_id BIGINT;
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS categoria_id BIGINT;
ALTER TABLE venda_formas_pagamento ADD COLUMN IF NOT EXISTS valor_pago DECIMAL(10,2);
ALTER TABLE venda_formas_pagamento ADD COLUMN IF NOT EXISTS numero_parcelas INT;
ALTER TABLE venda_formas_pagamento ADD COLUMN IF NOT EXISTS credito_id BIGINT;
