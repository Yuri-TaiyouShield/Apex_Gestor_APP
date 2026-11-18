-- ============================================================================
-- V17: Onda 1 - Hardening de Licenciamento SaaS (ADVANCED_FINANCE),
-- Segregacao de Funcoes (SoD) e Direitos dos Titulares LGPD
-- Compativel com H2 (MODE=MySQL;DATABASE_TO_LOWER=TRUE)
-- ============================================================================

-- 1. Evolucao retrocompativel de license_activation (V6)
ALTER TABLE license_activation ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE license_activation ADD COLUMN IF NOT EXISTS tenant_code VARCHAR(64);
ALTER TABLE license_activation ADD COLUMN IF NOT EXISTS subscription_tier VARCHAR(32) NOT NULL DEFAULT 'ESSENTIAL';
ALTER TABLE license_activation ADD COLUMN IF NOT EXISTS licensed_features VARCHAR(500) NOT NULL DEFAULT 'BASIC_PRICING,FINANCIAL_CORE';
CREATE INDEX IF NOT EXISTS idx_license_activation_empresa ON license_activation (empresa_id, status, valid_until);

-- 2. Entitlement Comercial por Empresa/Tenant (Direito Comercial separado de RBAC)
CREATE TABLE IF NOT EXISTS empresa_plano_licenca (
  id_empresa_plano_licenca BIGINT PRIMARY KEY AUTO_INCREMENT,
  empresa_id BIGINT NOT NULL,
  tenant_code VARCHAR(64) NOT NULL,
  tenant_name VARCHAR(120) NOT NULL DEFAULT 'Empresa Apex',
  subscription_tier VARCHAR(32) NOT NULL DEFAULT 'ESSENTIAL',
  features_habilitadas VARCHAR(500) NOT NULL DEFAULT 'BASIC_PRICING,FINANCIAL_CORE',
  license_key_hash VARCHAR(64),
  status VARCHAR(24) NOT NULL DEFAULT 'ATIVO',
  valido_ate DATETIME,
  ultima_validacao_em DATETIME,
  contingencia_ate DATETIME,
  grace_period_horas INT NOT NULL DEFAULT 72,
  atualizado_por VARCHAR(80),
  criado_em DATETIME NOT NULL,
  atualizado_em DATETIME,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_emp_plano_licenca_empresa UNIQUE (empresa_id),
  CONSTRAINT uk_emp_plano_licenca_tenant UNIQUE (tenant_code),
  INDEX idx_emp_plano_status_val (empresa_id, status, valido_ate)
);

CREATE TABLE IF NOT EXISTS empresa_plano_licenca_aud (
  id_empresa_plano_licenca BIGINT NOT NULL,
  REV INT NOT NULL,
  REVTYPE TINYINT NOT NULL,
  empresa_id BIGINT,
  tenant_code VARCHAR(64),
  tenant_name VARCHAR(120),
  subscription_tier VARCHAR(32),
  features_habilitadas VARCHAR(500),
  license_key_hash VARCHAR(64),
  status VARCHAR(24),
  valido_ate DATETIME,
  ultima_validacao_em DATETIME,
  contingencia_ate DATETIME,
  grace_period_horas INT,
  atualizado_por VARCHAR(80),
  criado_em DATETIME,
  atualizado_em DATETIME,
  version BIGINT,
  PRIMARY KEY (id_empresa_plano_licenca, REV),
  CONSTRAINT fk_emp_plano_aud_rev FOREIGN KEY (REV) REFERENCES audit_revision(revision_id)
);

-- 3. Politica Configuravel de Segregacao de Funcoes (SoD) e Alcada por Tenant/Filial
CREATE TABLE IF NOT EXISTS politica_segregacao_aprovacao (
  id_politica_sod BIGINT PRIMARY KEY AUTO_INCREMENT,
  empresa_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  dominio_operacao VARCHAR(48) NOT NULL,
  exigir_aprovador_distinto BOOLEAN NOT NULL DEFAULT TRUE,
  permitir_excecao_unipessoal BOOLEAN NOT NULL DEFAULT FALSE,
  exigir_justificativa BOOLEAN NOT NULL DEFAULT TRUE,
  alcada_gerente_maximo DECIMAL(15,2),
  atualizado_por VARCHAR(80),
  criado_em DATETIME NOT NULL,
  atualizado_em DATETIME,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_politica_sod_tenant_dom UNIQUE (empresa_id, filial_id, dominio_operacao),
  INDEX idx_politica_sod_tenant (empresa_id, filial_id)
);

CREATE TABLE IF NOT EXISTS politica_segregacao_aprovacao_aud (
  id_politica_sod BIGINT NOT NULL,
  REV INT NOT NULL,
  REVTYPE TINYINT NOT NULL,
  empresa_id BIGINT,
  filial_id BIGINT,
  dominio_operacao VARCHAR(48),
  exigir_aprovador_distinto BOOLEAN,
  permitir_excecao_unipessoal BOOLEAN,
  exigir_justificativa BOOLEAN,
  alcada_gerente_maximo DECIMAL(15,2),
  atualizado_por VARCHAR(80),
  criado_em DATETIME,
  atualizado_em DATETIME,
  version BIGINT,
  PRIMARY KEY (id_politica_sod, REV),
  CONSTRAINT fk_politica_sod_aud_rev FOREIGN KEY (REV) REFERENCES audit_revision(revision_id)
);

-- 4. Fortalecimento LGPD na Solicitacao de Direitos dos Titulares (SoD + retencao + confirmacao)
ALTER TABLE solicitacao_titular_dados ADD COLUMN IF NOT EXISTS solicitado_por_usuario_id BIGINT;
ALTER TABLE solicitacao_titular_dados ADD COLUMN IF NOT EXISTS aprovado_por_usuario_id BIGINT;
ALTER TABLE solicitacao_titular_dados ADD COLUMN IF NOT EXISTS aprovado_por VARCHAR(80);
ALTER TABLE solicitacao_titular_dados ADD COLUMN IF NOT EXISTS aprovado_em DATETIME;
ALTER TABLE solicitacao_titular_dados ADD COLUMN IF NOT EXISTS executado_por_usuario_id BIGINT;
ALTER TABLE solicitacao_titular_dados ADD COLUMN IF NOT EXISTS retencao_legal_validada BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE solicitacao_titular_dados ADD COLUMN IF NOT EXISTS impedimento_retencao VARCHAR(500);
ALTER TABLE solicitacao_titular_dados ADD COLUMN IF NOT EXISTS simulacao_hash VARCHAR(128);
ALTER TABLE solicitacao_titular_dados ADD COLUMN IF NOT EXISTS confirmado_explicito BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE solicitacao_titular_dados ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
