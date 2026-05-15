-- Apex Gestor 3.3 - modulo AdmCalc/Financeiro trabalhista, tributario e documentos.
-- Execute em bancos ja existentes antes de liberar o menu financeiro.

INSERT INTO perfil (nome, status)
SELECT 'financeiro', 1
WHERE NOT EXISTS (SELECT 1 FROM perfil WHERE nome = 'financeiro');

INSERT INTO perfil (nome, status)
SELECT 'contador', 1
WHERE NOT EXISTS (SELECT 1 FROM perfil WHERE nome = 'contador');

INSERT INTO perfil (nome, status)
SELECT 'advogado', 1
WHERE NOT EXISTS (SELECT 1 FROM perfil WHERE nome = 'advogado');

CREATE TABLE IF NOT EXISTS financial_calculation (
  id_financial_calculation BIGINT NOT NULL AUTO_INCREMENT,
  tipo_calculo VARCHAR(60) NOT NULL,
  ator_login VARCHAR(120) NOT NULL,
  input_hash VARCHAR(128) NOT NULL,
  input_snapshot LONGTEXT NOT NULL,
  resultado_snapshot LONGTEXT NOT NULL,
  criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_financial_calculation),
  KEY idx_fin_calc_tipo_data (tipo_calculo, criado_em),
  KEY idx_fin_calc_ator (ator_login)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS financial_audit_event (
  id_financial_audit_event BIGINT NOT NULL AUTO_INCREMENT,
  tipo_evento VARCHAR(80) NOT NULL,
  alvo_tipo VARCHAR(80) NOT NULL,
  alvo_id BIGINT DEFAULT NULL,
  ator_login VARCHAR(120) NOT NULL,
  valor_anterior VARCHAR(255) DEFAULT NULL,
  valor_novo VARCHAR(255) DEFAULT NULL,
  metadados LONGTEXT DEFAULT NULL,
  metadados_hash VARCHAR(128) DEFAULT NULL,
  criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_financial_audit_event),
  KEY idx_fin_audit_alvo (alvo_tipo, alvo_id),
  KEY idx_fin_audit_evento_data (tipo_evento, criado_em),
  KEY idx_fin_audit_ator (ator_login)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS financial_digital_document (
  id_financial_digital_document BIGINT NOT NULL AUTO_INCREMENT,
  tipo_documento VARCHAR(80) NOT NULL,
  funcionario_nome VARCHAR(160) NOT NULL,
  funcionario_email VARCHAR(160) NOT NULL,
  status VARCHAR(40) NOT NULL,
  cargo_assinante_obrigatorio VARCHAR(80) NOT NULL,
  gerado_por VARCHAR(120) NOT NULL,
  gerado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  assinado_por VARCHAR(160) DEFAULT NULL,
  cargo_assinante VARCHAR(80) DEFAULT NULL,
  assinado_em DATETIME DEFAULT NULL,
  enviado_em DATETIME DEFAULT NULL,
  assinatura_digital_hash VARCHAR(128) DEFAULT NULL,
  conteudo_hash VARCHAR(128) NOT NULL,
  conteudo LONGTEXT NOT NULL,
  assunto_email VARCHAR(180) DEFAULT NULL,
  mensagem_email LONGTEXT DEFAULT NULL,
  referencia VARCHAR(120) DEFAULT NULL,
  ultimo_erro VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id_financial_digital_document),
  KEY idx_fin_doc_status_data (status, gerado_em),
  KEY idx_fin_doc_funcionario (funcionario_email),
  KEY idx_fin_doc_referencia (referencia)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
