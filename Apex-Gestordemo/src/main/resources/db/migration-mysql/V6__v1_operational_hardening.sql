-- V6: hardening da v1 operacional na familia MySQL.
-- Campos legados permanecem compatÃ­veis para permitir backfill e validacao.

ALTER TABLE caixa_sessao ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE produtos ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE venda ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE venda ADD COLUMN idempotency_key VARCHAR(128);
ALTER TABLE venda ADD COLUMN idempotency_payload_hash VARCHAR(64);
ALTER TABLE venda ADD COLUMN recebimento_idempotency_key VARCHAR(128);
ALTER TABLE venda ADD COLUMN recebimento_idempotency_payload_hash VARCHAR(64);

ALTER TABLE conta_receber ADD CONSTRAINT uk_conta_receber_tenant_venda
  UNIQUE (empresa_id, filial_id, venda_id);

ALTER TABLE nota_fiscal_entrada ADD COLUMN numero VARCHAR(20);
ALTER TABLE nota_fiscal_entrada ADD COLUMN serie VARCHAR(5);
ALTER TABLE nota_fiscal_entrada ADD COLUMN fornecedor_id BIGINT;
ALTER TABLE nota_fiscal_entrada ADD COLUMN data_emissao DATETIME;
ALTER TABLE nota_fiscal_entrada ADD COLUMN data_entrada DATETIME;
ALTER TABLE nota_fiscal_entrada ADD COLUMN valor_total DECIMAL(10,2);
ALTER TABLE nota_fiscal_entrada ADD COLUMN idempotency_key VARCHAR(128);
ALTER TABLE nota_fiscal_entrada ADD COLUMN idempotency_payload_hash VARCHAR(64);
ALTER TABLE nota_fiscal_entrada ADD CONSTRAINT uk_nf_entrada_tenant_idempotency
  UNIQUE (empresa_id, filial_id, idempotency_key);

CREATE TABLE IF NOT EXISTS financial_calculation (
  id_financial_calculation BIGINT PRIMARY KEY AUTO_INCREMENT,
  tipo_calculo VARCHAR(60) NOT NULL,
  ator_login VARCHAR(120) NOT NULL,
  input_hash VARCHAR(128) NOT NULL,
  input_snapshot TEXT NOT NULL,
  resultado_snapshot TEXT NOT NULL,
  criado_em DATETIME NOT NULL,
  empresa_id BIGINT,
  filial_id BIGINT,
  INDEX idx_fin_calc_tenant_data (empresa_id, filial_id, criado_em)
);

CREATE TABLE IF NOT EXISTS financial_audit_event (
  id_financial_audit_event BIGINT PRIMARY KEY AUTO_INCREMENT,
  tipo_evento VARCHAR(80) NOT NULL,
  alvo_tipo VARCHAR(80) NOT NULL,
  alvo_id BIGINT,
  ator_login VARCHAR(120) NOT NULL,
  valor_anterior VARCHAR(255),
  valor_novo VARCHAR(255),
  metadados TEXT,
  metadados_hash VARCHAR(128),
  criado_em DATETIME NOT NULL,
  empresa_id BIGINT,
  filial_id BIGINT,
  INDEX idx_fin_audit_tenant_data (empresa_id, filial_id, criado_em)
);

CREATE TABLE IF NOT EXISTS financial_digital_document (
  id_financial_digital_document BIGINT PRIMARY KEY AUTO_INCREMENT,
  tipo_documento VARCHAR(80) NOT NULL,
  funcionario_nome VARCHAR(160) NOT NULL,
  funcionario_email VARCHAR(160) NOT NULL,
  status VARCHAR(40) NOT NULL,
  cargo_assinante_obrigatorio VARCHAR(80) NOT NULL,
  gerado_por VARCHAR(120) NOT NULL,
  gerado_em DATETIME NOT NULL,
  assinado_por VARCHAR(160),
  cargo_assinante VARCHAR(80),
  assinado_em DATETIME,
  enviado_em DATETIME,
  assinatura_digital_hash VARCHAR(128),
  conteudo_hash VARCHAR(128) NOT NULL,
  conteudo TEXT NOT NULL,
  assunto_email VARCHAR(180),
  mensagem_email TEXT,
  referencia VARCHAR(120),
  ultimo_erro VARCHAR(255),
  empresa_id BIGINT,
  filial_id BIGINT,
  INDEX idx_fin_doc_tenant_status (empresa_id, filial_id, status, gerado_em)
);

CREATE TABLE IF NOT EXISTS financial_document_outbox (
  id_financial_document_outbox BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  status VARCHAR(40) NOT NULL,
  queued_by VARCHAR(120) NOT NULL,
  queued_at DATETIME NOT NULL,
  sent_at DATETIME,
  attempts INT NOT NULL DEFAULT 0,
  last_error VARCHAR(255),
  next_attempt_at DATETIME,
  dead_lettered_at DATETIME,
  empresa_id BIGINT,
  filial_id BIGINT,
  CONSTRAINT fk_fin_outbox_document FOREIGN KEY (document_id)
    REFERENCES financial_digital_document(id_financial_digital_document),
  INDEX idx_fin_outbox_due (status, next_attempt_at, queued_at),
  INDEX idx_fin_outbox_tenant (empresa_id, filial_id, status)
);

CREATE TABLE IF NOT EXISTS refresh_token (
  id_refresh_token BIGINT PRIMARY KEY AUTO_INCREMENT,
  usuario_id BIGINT NOT NULL,
  token_hash VARCHAR(128) NOT NULL UNIQUE,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL,
  revoked_at DATETIME,
  ip_hash VARCHAR(128),
  user_agent_hash VARCHAR(128),
  INDEX idx_refresh_token_usuario (usuario_id, revoked_at, expires_at)
);

CREATE TABLE IF NOT EXISTS license_activation (
  id_license_activation BIGINT PRIMARY KEY AUTO_INCREMENT,
  license_key_hash VARCHAR(64) NOT NULL,
  device_hash VARCHAR(64) NOT NULL,
  device_label VARCHAR(120),
  platform VARCHAR(40),
  app_version VARCHAR(40),
  app_id VARCHAR(40) NOT NULL,
  license_plan VARCHAR(40) NOT NULL,
  licensed_apps VARCHAR(160) NOT NULL,
  status VARCHAR(20) NOT NULL,
  activated_at DATETIME NOT NULL,
  last_validated_at DATETIME NOT NULL,
  valid_until DATETIME NOT NULL,
  CONSTRAINT unq_license_device_app UNIQUE (license_key_hash, device_hash, app_id),
  INDEX idx_license_app_status (license_key_hash, app_id, status)
);

