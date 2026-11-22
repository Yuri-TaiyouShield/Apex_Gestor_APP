-- ============================================================================
-- V17: Onda 1 - Hardening de Licenciamento SaaS (ADVANCED_FINANCE),
-- Segregacao de Funcoes (SoD) e Direitos dos Titulares LGPD
-- Compativel com MySQL 8.4+
-- ============================================================================

-- 1. Evolucao idempotente de license_activation e solicitacao_titular_dados
DROP PROCEDURE IF EXISTS upgrade_v17_hardening;

DELIMITER $$
CREATE PROCEDURE upgrade_v17_hardening()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'license_activation' AND column_name = 'empresa_id') THEN
    ALTER TABLE license_activation ADD COLUMN empresa_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'license_activation' AND column_name = 'tenant_code') THEN
    ALTER TABLE license_activation ADD COLUMN tenant_code VARCHAR(64);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'license_activation' AND column_name = 'subscription_tier') THEN
    ALTER TABLE license_activation ADD COLUMN subscription_tier VARCHAR(32) NOT NULL DEFAULT 'ESSENTIAL';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'license_activation' AND column_name = 'licensed_features') THEN
    ALTER TABLE license_activation ADD COLUMN licensed_features VARCHAR(500) NOT NULL DEFAULT 'BASIC_PRICING,FINANCIAL_CORE';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'license_activation' AND index_name = 'idx_license_activation_empresa') THEN
    CREATE INDEX idx_license_activation_empresa ON license_activation (empresa_id, status, valid_until);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'solicitacao_titular_dados' AND column_name = 'solicitado_por_usuario_id') THEN
    ALTER TABLE solicitacao_titular_dados ADD COLUMN solicitado_por_usuario_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'solicitacao_titular_dados' AND column_name = 'aprovado_por_usuario_id') THEN
    ALTER TABLE solicitacao_titular_dados ADD COLUMN aprovado_por_usuario_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'solicitacao_titular_dados' AND column_name = 'aprovado_por') THEN
    ALTER TABLE solicitacao_titular_dados ADD COLUMN aprovado_por VARCHAR(80);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'solicitacao_titular_dados' AND column_name = 'aprovado_em') THEN
    ALTER TABLE solicitacao_titular_dados ADD COLUMN aprovado_em DATETIME;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'solicitacao_titular_dados' AND column_name = 'executado_por_usuario_id') THEN
    ALTER TABLE solicitacao_titular_dados ADD COLUMN executado_por_usuario_id BIGINT;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'solicitacao_titular_dados' AND column_name = 'retencao_legal_validada') THEN
    ALTER TABLE solicitacao_titular_dados ADD COLUMN retencao_legal_validada BOOLEAN NOT NULL DEFAULT FALSE;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'solicitacao_titular_dados' AND column_name = 'impedimento_retencao') THEN
    ALTER TABLE solicitacao_titular_dados ADD COLUMN impedimento_retencao VARCHAR(500);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'solicitacao_titular_dados' AND column_name = 'simulacao_hash') THEN
    ALTER TABLE solicitacao_titular_dados ADD COLUMN simulacao_hash VARCHAR(128);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'solicitacao_titular_dados' AND column_name = 'confirmado_explicito') THEN
    ALTER TABLE solicitacao_titular_dados ADD COLUMN confirmado_explicito BOOLEAN NOT NULL DEFAULT FALSE;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'solicitacao_titular_dados' AND column_name = 'version') THEN
    ALTER TABLE solicitacao_titular_dados ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
  END IF;
END $$
DELIMITER ;

CALL upgrade_v17_hardening();
DROP PROCEDURE IF EXISTS upgrade_v17_hardening;

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
