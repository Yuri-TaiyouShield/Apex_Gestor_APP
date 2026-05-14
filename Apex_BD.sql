-- Otimizado e Unificado SQL Dump para Apex_BD
-- Versão: 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Tempo de geração: 09/11/2025 às 19:53
-- Versão do servidor: 10.4.32-MariaDB
-- Versão do PHP: 8.2.12

-- Configurações iniciais para eficiência e segurança (desabilitar checks temporariamente)
SET @OLD_UNIQUE_CHECKS = @@UNIQUE_CHECKS, UNIQUE_CHECKS = 0;
SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0;
SET @OLD_SQL_MODE = @@SQL_MODE, SQL_MODE = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- Criação do schema (se não existir)
CREATE SCHEMA IF NOT EXISTS `apex_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `apex_db`;

-- Criação das tabelas na ordem correta para evitar erros de chaves estrangeiras
-- (Tabelas independentes primeiro, depois dependentes)

-- Tabela categoria (independente)
CREATE TABLE IF NOT EXISTS `categoria` (
  `id_categoria` int(11) NOT NULL AUTO_INCREMENT,
  `descricao` varchar(45) DEFAULT NULL,
  `status` tinyint(4) DEFAULT 1,
  PRIMARY KEY (`id_categoria`),
  KEY `idx_categoria_status` (`status`),
  KEY `idx_categoria_descricao` (`descricao`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela endereco (independente)
CREATE TABLE IF NOT EXISTS `endereco` (
  `id_endereco` int(11) NOT NULL AUTO_INCREMENT,
  `estado` varchar(45) DEFAULT NULL,
  `cidade` varchar(45) DEFAULT NULL,
  `cep` varchar(9) NOT NULL,
  `bairro` varchar(100) DEFAULT NULL,
  `logradouro` varchar(100) DEFAULT NULL,
  `uf` char(2) DEFAULT NULL,
  `numero` varchar(10) DEFAULT NULL,
  `complemento` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id_endereco`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela perfil (independente)
CREATE TABLE IF NOT EXISTS `perfil` (
  `id_perfil` int(11) NOT NULL AUTO_INCREMENT,
  `nome` varchar(45) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id_perfil`),
  UNIQUE KEY `unq_perfil_nome` (`nome`),
  KEY `idx_perfil_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela menu (independente)
CREATE TABLE IF NOT EXISTS `menu` (
  `id_menu` int(11) NOT NULL AUTO_INCREMENT,
  `nome` varchar(45) NOT NULL,
  `link` varchar(100) NOT NULL,
  `icone` varchar(45) DEFAULT NULL,
  `exibir` tinyint(4) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id_menu`),
  KEY `idx_menu_exibir` (`exibir`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela formas_pagamento (independente)
CREATE TABLE IF NOT EXISTS `formas_pagamento` (
  `id_forma_pagamento` int(11) NOT NULL AUTO_INCREMENT,
  `descricao` text NOT NULL,
  `nome` varchar(50) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT 0,
  `tipo_pagamento` tinyint(4) NOT NULL,
  PRIMARY KEY (`id_forma_pagamento`),
  KEY `idx_formas_pagamento_status` (`status`),
  KEY `idx_formas_pagamento_tipo` (`tipo_pagamento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tipo_despesa` (
  `id_tipo_despesa` int(11) NOT NULL AUTO_INCREMENT,
  `nome` varchar(50) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id_tipo_despesa`),
  UNIQUE KEY `unq_tipo_despesa_nome` (`nome`),
  KEY `idx_tipo_despesa_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela fornecedor (depende de endereco)
CREATE TABLE IF NOT EXISTS `fornecedor` (
  `id_fornecedor` int(11) NOT NULL AUTO_INCREMENT,
  `status` tinyint(4) NOT NULL DEFAULT 1,
  `razao_social` varchar(100) NOT NULL,
  `nome_fantasia` varchar(45) NOT NULL,
  `cnpj` varchar(18) NOT NULL,
  `inscricao_estadual` varchar(20) DEFAULT NULL,
  `data_cadastro` timestamp NOT NULL DEFAULT current_timestamp(),
  `telefone` varchar(15) NOT NULL,
  `endereco_id` int(11) NOT NULL,
  PRIMARY KEY (`id_fornecedor`),
  UNIQUE KEY `cnpj` (`cnpj`),
  KEY `idx_fornecedor_status` (`status`),
  KEY `idx_fornecedor_nome_fantasia` (`nome_fantasia`),
  KEY `idx_fornecedor_endereco` (`endereco_id`),
  CONSTRAINT `fk_fornecedor_endereco` FOREIGN KEY (`endereco_id`) REFERENCES `endereco` (`id_endereco`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela usuario (depende de perfil)
CREATE TABLE IF NOT EXISTS `usuario` (
  `id_usuario` int(11) NOT NULL AUTO_INCREMENT,
  `nome` varchar(45) NOT NULL,
  `login` varchar(45) NOT NULL,
  `senha` varchar(255) NOT NULL,  -- Aumentado para suportar hashes seguros
  `data_nascimento` date NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT 1,
  `perfil_id` int(11) NOT NULL,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `login` (`login`),
  KEY `idx_usuario_status` (`status`),
  KEY `idx_usuario_perfil` (`perfil_id`),
  CONSTRAINT `fk_usuario_perfil` FOREIGN KEY (`perfil_id`) REFERENCES `perfil` (`id_perfil`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela cliente (independente)
CREATE TABLE IF NOT EXISTS `cliente` (
  `id_cliente` int(11) NOT NULL AUTO_INCREMENT,
  `nome_razao` varchar(100) NOT NULL,
  `telefone` varchar(15) NOT NULL,
  `tipo_documento` tinyint(4) DEFAULT NULL,
  `cpf_cnpj` char(14) NOT NULL,
  `data_cadastro` datetime DEFAULT current_timestamp(),
  `status` tinyint(4) DEFAULT 1,
  PRIMARY KEY (`id_cliente`),
  UNIQUE KEY `unq_cliente_documento` (`tipo_documento`,`cpf_cnpj`),
  KEY `idx_cliente_status` (`status`),
  KEY `idx_cliente_nome` (`nome_razao`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela venda (depende de usuario e cliente)
CREATE TABLE IF NOT EXISTS `venda` (
  `id_venda` int(11) NOT NULL AUTO_INCREMENT,
  `usuario_id` int(11) NOT NULL,
  `cliente_id` int(11) NOT NULL,
  `status` tinyint(4) NOT NULL,
  `valor_total` decimal(10,2) NOT NULL,
  `desconto` decimal(10,2) DEFAULT 0.00,
  `observacao` varchar(255) DEFAULT NULL,
  `data_venda` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_venda`),
  KEY `idx_venda_status_data` (`status`, `data_venda`),
  KEY `idx_venda_usuario` (`usuario_id`),
  KEY `idx_venda_cliente` (`cliente_id`),
  CONSTRAINT `fk_venda_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id_usuario`) ON UPDATE CASCADE,
  CONSTRAINT `fk_venda_cliente` FOREIGN KEY (`cliente_id`) REFERENCES `cliente` (`id_cliente`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela produtos (depende de fornecedor e categoria)
CREATE TABLE IF NOT EXISTS `produtos` (
  `id_produto` int(11) NOT NULL AUTO_INCREMENT,
  `descricao` varchar(60) NOT NULL,
  `custo` decimal(10,2) NOT NULL DEFAULT 0.00,
  `fornecedor_id` int(11) DEFAULT NULL,
  `codigo_barras` varchar(20) DEFAULT NULL,
  `marca` varchar(45) DEFAULT NULL,
  `unidade_medida` varchar(10) DEFAULT NULL,
  `data_aquisicao` timestamp NULL DEFAULT current_timestamp(),
  `quantidade_estoque` int(11) NOT NULL DEFAULT 0,
  `estoque_minimo` int(11) NOT NULL DEFAULT 0,
  `valor_venda` decimal(10,2) NOT NULL DEFAULT 0.00,
  `status` tinyint(4) NOT NULL DEFAULT 1,
  `categoria_id` int(11) NOT NULL,
  PRIMARY KEY (`id_produto`),
  UNIQUE KEY `unq_produtos_codigo_barras` (`codigo_barras`),
  KEY `idx_produtos_status` (`status`),
  KEY `idx_produtos_descricao` (`descricao`),
  KEY `idx_produtos_fornecedor` (`fornecedor_id`),
  KEY `idx_produtos_categoria` (`categoria_id`),
  CONSTRAINT `fk_produtos_fornecedor` FOREIGN KEY (`fornecedor_id`) REFERENCES `fornecedor` (`id_fornecedor`) ON UPDATE CASCADE,
  CONSTRAINT `fk_produtos_categoria` FOREIGN KEY (`categoria_id`) REFERENCES `categoria` (`id_categoria`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabelas de relacionamento (dependem das principais)
CREATE TABLE IF NOT EXISTS `cliente_endereco` (
  `cliente_id` int(11) NOT NULL,
  `endereco_id` int(11) NOT NULL,
  PRIMARY KEY (`cliente_id`,`endereco_id`),
  KEY `idx_cliente_endereco_endereco` (`endereco_id`),
  KEY `idx_cliente_endereco_cliente` (`cliente_id`),
  CONSTRAINT `fk_cliente_endereco_cliente` FOREIGN KEY (`cliente_id`) REFERENCES `cliente` (`id_cliente`) ON UPDATE CASCADE,
  CONSTRAINT `fk_cliente_endereco_endereco` FOREIGN KEY (`endereco_id`) REFERENCES `endereco` (`id_endereco`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `menu_perfil` (
  `menu_id` int(11) NOT NULL,
  `perfil_id` int(11) NOT NULL,
  PRIMARY KEY (`menu_id`,`perfil_id`),
  KEY `idx_menu_perfil_perfil` (`perfil_id`),
  KEY `idx_menu_perfil_menu` (`menu_id`),
  CONSTRAINT `fk_menu_perfil_menu` FOREIGN KEY (`menu_id`) REFERENCES `menu` (`id_menu`) ON UPDATE CASCADE,
  CONSTRAINT `fk_menu_perfil_perfil` FOREIGN KEY (`perfil_id`) REFERENCES `perfil` (`id_perfil`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `produto_venda` (
  `produto_id` int(11) NOT NULL,
  `venda_id` int(11) NOT NULL,
  `quantidade` int(11) NOT NULL,
  `preco_unitario` decimal(10,2) NOT NULL,
  `custo_unitario` decimal(10,2) NOT NULL DEFAULT 0.00,
  `preco_total` decimal(10,2) DEFAULT 0.00,
  PRIMARY KEY (`produto_id`,`venda_id`),
  KEY `idx_produto_venda_venda` (`venda_id`),
  CONSTRAINT `fk_produto_venda_produto` FOREIGN KEY (`produto_id`) REFERENCES `produtos` (`id_produto`) ON UPDATE CASCADE,
  CONSTRAINT `fk_produto_venda_venda` FOREIGN KEY (`venda_id`) REFERENCES `venda` (`id_venda`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `venda_formas_pagamento` (
  `venda_id` int(11) NOT NULL,
  `forma_pagamento_id` int(11) NOT NULL,
  `valor_pago` decimal(10,2) DEFAULT 0.00,
  `numero_parcelas` int(11) DEFAULT 1,
  PRIMARY KEY (`venda_id`,`forma_pagamento_id`),
  KEY `fk_venda_formapagamento_venda` (`venda_id`),
  KEY `fk_venda_formapagamento_forma` (`forma_pagamento_id`),
  CONSTRAINT `fk_venda_formapagamento_forma` FOREIGN KEY (`forma_pagamento_id`) REFERENCES `formas_pagamento` (`id_forma_pagamento`) ON UPDATE CASCADE,
  CONSTRAINT `fk_venda_formapagamento_venda` FOREIGN KEY (`venda_id`) REFERENCES `venda` (`id_venda`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `venda_produto` (
  `id_venda_produto` int(11) NOT NULL AUTO_INCREMENT,
  `venda_id` int(11) NOT NULL,
  `produto_id` int(11) NOT NULL,
  `quantidade` int(11) DEFAULT 1,
  `valor_unitario` decimal(10,2) DEFAULT 0.00,
  `subtotal` decimal(10,2) GENERATED ALWAYS AS (`quantidade` * `valor_unitario`) STORED,
  PRIMARY KEY (`id_venda_produto`),
  KEY `fk_venda_produto_venda` (`venda_id`),
  KEY `fk_venda_produto_produto` (`produto_id`),
  CONSTRAINT `fk_venda_produto_produto` FOREIGN KEY (`produto_id`) REFERENCES `produtos` (`id_produto`) ON UPDATE CASCADE,
  CONSTRAINT `fk_venda_produto_venda` FOREIGN KEY (`venda_id`) REFERENCES `venda` (`id_venda`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `despesas` (
  `id_despesa` int(11) NOT NULL AUTO_INCREMENT,
  `descricao` varchar(255) NOT NULL,
  `valor` decimal(10,2) NOT NULL,
  `data_vencimento` date NOT NULL,
  `data_pagamento` date DEFAULT NULL,
  `tipo_despesa_id` int(11) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id_despesa`),
  KEY `idx_despesas_status_vencimento` (`status`, `data_vencimento`),
  KEY `idx_despesas_tipo` (`tipo_despesa_id`),
  CONSTRAINT `fk_despesas_tipo` FOREIGN KEY (`tipo_despesa_id`) REFERENCES `tipo_despesa` (`id_tipo_despesa`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `nota_fiscal_entrada` (
  `id_nf_entrada` int(11) NOT NULL AUTO_INCREMENT,
  `numero` varchar(20) NOT NULL,
  `serie` varchar(5) DEFAULT NULL,
  `fornecedor_id` int(11) NOT NULL,
  `data_emissao` datetime NOT NULL,
  `data_entrada` datetime NOT NULL DEFAULT current_timestamp(),
  `valor_total` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id_nf_entrada`),
  UNIQUE KEY `unq_nf_entrada_fornecedor_numero_serie` (`fornecedor_id`,`numero`,`serie`),
  KEY `idx_nf_entrada_data` (`data_entrada`),
  CONSTRAINT `fk_nf_entrada_fornecedor` FOREIGN KEY (`fornecedor_id`) REFERENCES `fornecedor` (`id_fornecedor`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `item_nota_fiscal` (
  `id_item_nf` int(11) NOT NULL AUTO_INCREMENT,
  `nf_entrada_id` int(11) NOT NULL,
  `produto_id` int(11) NOT NULL,
  `quantidade` int(11) NOT NULL,
  `valor_custo_unitario` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id_item_nf`),
  KEY `idx_item_nf_entrada` (`nf_entrada_id`),
  KEY `idx_item_nf_produto` (`produto_id`),
  CONSTRAINT `fk_item_nf_entrada` FOREIGN KEY (`nf_entrada_id`) REFERENCES `nota_fiscal_entrada` (`id_nf_entrada`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_item_nf_produto` FOREIGN KEY (`produto_id`) REFERENCES `produtos` (`id_produto`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `refresh_token` (
  `id_refresh_token` bigint NOT NULL AUTO_INCREMENT,
  `usuario_id` int(11) NOT NULL,
  `token_hash` varchar(128) NOT NULL,
  `expires_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `revoked_at` datetime DEFAULT NULL,
  `ip_hash` varchar(128) DEFAULT NULL,
  `user_agent_hash` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id_refresh_token`),
  UNIQUE KEY `idx_refresh_token_hash` (`token_hash`),
  KEY `idx_refresh_token_usuario` (`usuario_id`),
  KEY `idx_refresh_token_expira` (`expires_at`, `revoked_at`),
  CONSTRAINT `fk_refresh_token_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `audit_log` (
  `id_audit_log` bigint NOT NULL AUTO_INCREMENT,
  `ator_login` varchar(80) DEFAULT NULL,
  `metodo` varchar(20) NOT NULL,
  `recurso` varchar(255) NOT NULL,
  `status` int NOT NULL,
  `ip_hash` varchar(128) DEFAULT NULL,
  `user_agent_hash` varchar(128) DEFAULT NULL,
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_audit_log`),
  KEY `idx_audit_log_criado` (`criado_em`),
  KEY `idx_audit_log_ator` (`ator_login`),
  KEY `idx_audit_log_recurso` (`recurso`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `consent_audit` (
  `id_consent_audit` bigint NOT NULL AUTO_INCREMENT,
  `titular_id` varchar(80) DEFAULT NULL,
  `tipo_titular` varchar(30) NOT NULL,
  `documento_hash` varchar(128) DEFAULT NULL,
  `versao` varchar(40) NOT NULL,
  `aceito_em` datetime NOT NULL DEFAULT current_timestamp(),
  `canal` varchar(40) NOT NULL,
  `ip_hash` varchar(128) DEFAULT NULL,
  `user_agent_hash` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id_consent_audit`),
  KEY `idx_consent_titular` (`tipo_titular`, `titular_id`),
  KEY `idx_consent_documento` (`documento_hash`),
  KEY `idx_consent_aceito` (`aceito_em`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `privacy_request` (
  `id_privacy_request` bigint NOT NULL AUTO_INCREMENT,
  `protocolo` varchar(40) NOT NULL,
  `titular_id` varchar(80) NOT NULL,
  `tipo` varchar(30) NOT NULL,
  `status` varchar(30) NOT NULL,
  `solicitado_em` datetime NOT NULL DEFAULT current_timestamp(),
  `concluido_em` datetime DEFAULT NULL,
  `solicitante_login` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`id_privacy_request`),
  UNIQUE KEY `unq_privacy_request_protocolo` (`protocolo`),
  KEY `idx_privacy_request_titular` (`titular_id`),
  KEY `idx_privacy_request_status` (`status`, `solicitado_em`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `data_erasure_log` (
  `id_data_erasure_log` bigint NOT NULL AUTO_INCREMENT,
  `titular_hash` varchar(128) NOT NULL,
  `motivo` varchar(120) NOT NULL,
  `operador_login` varchar(80) DEFAULT NULL,
  `executado_em` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_data_erasure_log`),
  KEY `idx_data_erasure_titular` (`titular_hash`),
  KEY `idx_data_erasure_executado` (`executado_em`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `license_activation` (
  `id_license_activation` bigint NOT NULL AUTO_INCREMENT,
  `license_key_hash` varchar(64) NOT NULL,
  `device_hash` varchar(64) NOT NULL,
  `device_label` varchar(120) DEFAULT NULL,
  `platform` varchar(40) DEFAULT NULL,
  `app_version` varchar(40) DEFAULT NULL,
  `app_id` varchar(40) NOT NULL DEFAULT 'web-client',
  `license_plan` varchar(40) NOT NULL DEFAULT 'LEGACY_ALL',
  `licensed_apps` varchar(160) NOT NULL DEFAULT 'desktop,mobile-staff,mobile-client,web-client',
  `status` varchar(20) NOT NULL,
  `activated_at` datetime NOT NULL,
  `last_validated_at` datetime NOT NULL,
  `valid_until` datetime NOT NULL,
  PRIMARY KEY (`id_license_activation`),
  UNIQUE KEY `unq_license_device_app` (`license_key_hash`, `device_hash`, `app_id`),
  KEY `idx_license_key_hash` (`license_key_hash`),
  KEY `idx_license_device_hash` (`device_hash`),
  KEY `idx_license_app_status` (`license_key_hash`, `app_id`, `status`),
  KEY `idx_license_status` (`license_key_hash`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `estoque_movimento` (
  `id_estoque_movimento` bigint NOT NULL AUTO_INCREMENT,
  `produto_id` int(11) NOT NULL,
  `venda_id` int(11) DEFAULT NULL,
  `nf_entrada_id` int(11) DEFAULT NULL,
  `tipo` varchar(30) NOT NULL,
  `quantidade` int(11) NOT NULL,
  `saldo_apos_movimento` int(11) DEFAULT NULL,
  `custo_unitario` decimal(10,2) DEFAULT NULL,
  `origem` varchar(40) DEFAULT NULL,
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_estoque_movimento`),
  KEY `idx_estoque_movimento_produto_data` (`produto_id`, `criado_em`),
  KEY `idx_estoque_movimento_venda` (`venda_id`),
  KEY `idx_estoque_movimento_nf` (`nf_entrada_id`),
  CONSTRAINT `fk_estoque_movimento_produto` FOREIGN KEY (`produto_id`) REFERENCES `produtos` (`id_produto`) ON UPDATE CASCADE,
  CONSTRAINT `fk_estoque_movimento_venda` FOREIGN KEY (`venda_id`) REFERENCES `venda` (`id_venda`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_estoque_movimento_nf` FOREIGN KEY (`nf_entrada_id`) REFERENCES `nota_fiscal_entrada` (`id_nf_entrada`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `entrega` (
  `id_entrega` bigint NOT NULL AUTO_INCREMENT,
  `venda_id` int(11) NOT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'PENDENTE',
  `codigo_rastreio` varchar(80) DEFAULT NULL,
  `transportadora` varchar(80) DEFAULT NULL,
  `previsao_entrega` datetime DEFAULT NULL,
  `enviado_em` datetime DEFAULT NULL,
  `entregue_em` datetime DEFAULT NULL,
  `atualizado_em` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_entrega`),
  UNIQUE KEY `unq_entrega_venda` (`venda_id`),
  KEY `idx_entrega_status` (`status`, `atualizado_em`),
  CONSTRAINT `fk_entrega_venda` FOREIGN KEY (`venda_id`) REFERENCES `venda` (`id_venda`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Inserção de dados (após criação de todas as tabelas para respeitar FK)
INSERT INTO `categoria` (`id_categoria`, `descricao`, `status`) VALUES
(1, 'Lingeries e Fantasias', 1),
(2, 'Óleos e Aromas', 1),
(3, 'Cosméticos Sensuais', 1),
(4, 'Presentes Românticos', 1),
(5, 'Jogos e Kits', 1);

INSERT INTO `endereco` (`id_endereco`, `estado`, `cidade`, `cep`, `bairro`, `logradouro`, `uf`, `numero`, `complemento`) VALUES
(1, 'Distrito Federal', 'Brasília', '70630-010', 'Asa Norte', 'SQN 211 Bloco F', 'DF', '102', 'Apto 203'),
(2, 'Goiás', 'Anápolis', '75020-110', 'Jundiaí', 'Rua das Flores', 'GO', '58', 'Próximo à praça central'),
(3, 'São Paulo', 'Campinas', '13050-200', 'Cambuí', 'Av. Moraes Sales', 'SP', '920', 'Loja 01'),
(4, 'Rio de Janeiro', 'Niterói', '24210-500', 'Icaraí', 'Rua Coronel Moreira César', 'RJ', '300', 'Sobreloja');

INSERT INTO `fornecedor` (`id_fornecedor`, `status`, `razao_social`, `nome_fantasia`, `cnpj`, `inscricao_estadual`, `data_cadastro`, `telefone`, `endereco_id`) VALUES
(1, 2, 'Essência Natural LTDA', 'Essência Natural', '12.345.678/0001-99', '123456789', '2025-11-08 03:00:00', '(61) 99988-7777', 1),
(2, 1, 'Sensual Aromas EIRELI', 'Sensual Aromas', '98.765.432/0001-22', '987654321', '2025-11-09 00:54:38', '(62) 98888-6666', 2),
(3, 1, 'Delicatta Moda Íntima', 'Delicatta Lingeries', '23.456.789/0001-55', '223344556', '2025-11-09 00:54:38', '(11) 97777-5555', 3),
(4, 1, 'Amor & Cia Cosméticos', 'Amor & Cia', '34.567.890/0001-77', '667788990', '2025-11-09 00:54:38', '(21) 96666-4444', 4);

INSERT INTO `perfil` (`id_perfil`, `nome`, `status`) VALUES
(1, 'admin', 1),
(2, 'gerente', 1),
(3, 'vendedor', 1),
(4, 'estoquista', 1);

INSERT INTO `usuario` (`id_usuario`, `nome`, `login`, `senha`, `data_nascimento`, `status`, `perfil_id`) VALUES
(1, 'Júlia Moraes', 'julia', '$2a$10$d7pRK.L04FIpbAaEZ1.Pg.uee3HgjIOZFynclkEiqmWHmDok9sxgG', '1995-06-13', 2, 2),
(2, 'Marcos Lima', 'marcos', '$2a$10$d7pRK.L04FIpbAaEZ1.Pg.uee3HgjIOZFynclkEiqmWHmDok9sxgG', '1990-09-22', 2, 3),
(3, 'Renata Oliveira', 'renata', '$2a$10$d7pRK.L04FIpbAaEZ1.Pg.uee3HgjIOZFynclkEiqmWHmDok9sxgG', '1993-02-10', 1, 4),
(4, 'Camila Duarte', 'camila', '$2a$10$d7pRK.L04FIpbAaEZ1.Pg.uee3HgjIOZFynclkEiqmWHmDok9sxgG', '1988-03-05', 1, 1);

-- Inserção de dados (continuação após criação de todas as tabelas)
INSERT INTO `menu` (`id_menu`, `nome`, `link`, `icone`, `exibir`) VALUES
(1, 'ListarUsuario', 'gerenciar_usuario.do?acao=listarTodos', '', 1),
(2, 'AlterarUsuario', 'gerenciar_usuario.do?acao=alterar', '', 2),
(3, 'DesativarUsuario', 'gerenciar_usuario.do?acao=desativar', '', 2),
(4, 'CadastrarUsuario', 'form_usuario.jsp', '', 2),
(5, 'ListarCliente', 'gerenciar_cliente.do?acao=listarTodos', '', 1),
(6, 'AlterarCliente', 'gerenciar_cliente.do?acao=alterar', '', 2),
(7, 'DesativarCliente', 'gerenciar_cliente.do?acao=desativar', '', 2),
(8, 'CadastrarCliente', 'form_cliente.jsp', '', 2),
(9, 'ListarProduto', 'gerenciar_produto.do?acao=listarTodos', '', 1),
(10, 'AlterarProduto', 'gerenciar_produto.do?acao=alterar', '', 2),
(11, 'DesativarProduto', 'gerenciar_produto.do?acao=desativar', '', 2),
(12, 'CadastrarProduto', 'form_produto.jsp', '', 2),
(13, 'ListarFornecedor', 'gerenciar_fornecedor.do?acao=listarTodos', '', 1),
(14, 'AlterarFornecedor', 'gerenciar_fornecedor.do?acao=alterar', '', 2),
(15, 'DesativarFornecedor', 'gerenciar_fornecedor.do?acao=desativar', '', 2),
(16, 'CadastrarFornecedor', 'form_fornecedor.jsp', '', 2),
(17, 'ListarVendas', 'gerenciar_vendas.do?acao=listarTodos', '', 1),
(18, 'AdicionarNovaVenda', 'gerenciar_vendas.do?acao=novaVenda', '', 2),
(19, 'AdicionarProduto', 'gerenciar_vendas.do?acao=adicionarProduto', '', 2),
(20, 'RetirarProduto', 'gerenciar_vendas.do?acao=retirarProduto', '', 2),
(21, 'GravarVenda', 'gerenciar_vendas.do?acao=gravar', '', 2),
(22, 'FormularioVenda', 'form_venda.jsp', '', 2),
(23, 'ListarMenu', 'gerenciar_menu.do?acao=listarTodos', '', 1),
(24, 'AlterarMenu', 'gerenciar_menu.do?acao=alterar', '', 2),
(25, 'ExcluirMenu', 'gerenciar_menu.do?acao=excluir', '', 2),
(26, 'CadastrarMenu', 'form_menu.jsp', '', 2),
(27, 'GerenciarMenuPerfil', 'gerenciar_menu_perfil.do?acao=gerenciar', '', 2),
(28, 'DesvincularMenu', 'gerenciar_menu_perfil.do?acao=desvincular', '', 2),
(29, 'FormularioMenuPerfil', 'form_menu_perfil.jsp', '', 2),
(30, 'ListarPerfil', 'gerenciar_perfil.do?acao=listarTodos', '', 1),
(31, 'AlterarPerfil', 'gerenciar_perfil.do?acao=alterar', '', 2),
(32, 'ExcluirPerfil', 'gerenciar_perfil.do?acao=excluir', '', 2),
(33, 'CadastrarPerfil', 'form_perfil.jsp', '', 2),
(34, 'FormularioLogin', 'form_login.jsp', '', 2),
(35, 'FormularioVisualizaCarrinho', 'form_visualizar_carrinho.jsp', '', 1),
(36, 'Home', 'index.jsp', '', 1),
(37, 'Clientes', 'listar_cliente.jsp', '', 2),
(38, 'Fornecedores', 'listar_fornecedor.jsp', '', 2),
(39, 'Menus', 'listar_menu.jsp', '', 2),
(40, 'Perfis', 'listar_perfil.jsp', '', 2),
(41, 'Produtos', 'listar_produto.jsp', '', 2),
(42, 'Usuarios', 'listar_usuario.jsp', '', 2),
(43, 'Vendas', 'listar_venda.jsp', '', 2),
(44, 'Pagina de Menu', 'menu.jsp', '', 2),
(45, 'Adicionar Produto', 'gerenciar_venda_produto.do?acao=adicionarProduto', 'glyphicon glyphicon-plus', 2);

INSERT INTO `cliente` (`id_cliente`, `nome_razao`, `telefone`, `tipo_documento`, `cpf_cnpj`, `data_cadastro`, `status`) VALUES
(1, 'Lucas Andrade', '(61)99991-2345', 1, '12345678901', '2025-11-08 21:54:38', 1),
(2, 'Patrícia Souza', '(61)98888-7777', 1, '98765432100', '2025-11-08 21:54:38', 1),
(3, 'Fernanda Dias', '(61)99988-5566', 1, '45678912300', '2025-11-08 21:54:38', 1),
(4, 'Gustavo Pereira', '(61)98877-9988', 1, '32165498700', '2025-11-08 21:54:38', 1),
(5, 'teste', '1234567899', 1, '12345678910', '2025-11-08 23:40:56', 2);

INSERT INTO `cliente_endereco` (`cliente_id`, `endereco_id`) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4);

INSERT INTO `formas_pagamento` (`id_forma_pagamento`, `descricao`, `nome`, `status`, `tipo_pagamento`) VALUES
(1, 'Pagamento à vista em dinheiro', 'Dinheiro', 1, 1),
(2, 'Cartão de crédito até 3x sem juros', 'Cartão de Crédito', 1, 2),
(3, 'Cartão de débito', 'Cartão de Débito', 1, 3),
(4, 'Pix instantâneo', 'Pix', 1, 4);

INSERT INTO `tipo_despesa` (`id_tipo_despesa`, `nome`, `status`) VALUES
(1, 'Aluguel', 1),
(2, 'Energia', 1),
(3, 'Marketing', 1),
(4, 'Logistica', 1);

INSERT INTO `despesas` (`id_despesa`, `descricao`, `valor`, `data_vencimento`, `data_pagamento`, `tipo_despesa_id`, `status`) VALUES
(1, 'Aluguel da loja', 2500.00, '2025-11-10', NULL, 1, 1),
(2, 'Campanha de divulgacao online', 850.00, '2025-11-15', NULL, 3, 1);

INSERT INTO `menu_perfil` (`menu_id`, `perfil_id`) VALUES
(1, 1),
(2, 1),
(3, 1),
(4, 1),
(5, 1),
(6, 1),
(7, 1),
(8, 1),
(9, 1),
(10, 1),
(11, 1),
(12, 1),
(13, 1),
(14, 1),
(15, 1),
(16, 1),
(17, 1),
(18, 1),
(19, 1),
(20, 1),
(21, 1),
(22, 1),
(23, 1),
(24, 1),
(25, 1),
(26, 1),
(27, 1),
(28, 1),
(29, 1),
(30, 1),
(31, 1),
(32, 1),
(33, 1),
(34, 1),
(35, 1),
(36, 1),
(37, 1),
(38, 1),
(39, 1),
(40, 1),
(41, 1),
(42, 1),
(43, 1),
(44, 1),
(45, 1);

INSERT INTO `produtos` (`id_produto`, `descricao`, `custo`, `fornecedor_id`, `codigo_barras`, `marca`, `unidade_medida`, `data_aquisicao`, `quantidade_estoque`, `estoque_minimo`, `valor_venda`, `status`, `categoria_id`) VALUES
(1, 'Óleo de Massagem com Aroma de Baunilha', 12.50, 2, '7891234560012', 'Essência Natural', 'UN', '2025-11-07 03:00:00', 30, 5, 29.90, 1, 2),
(2, 'Gel Beijável de Morango 50ml', 8.90, 2, '7891234560029', 'Sensual Aromas', 'UN', '2025-11-09 00:54:38', 50, 10, 19.90, 1, 3),
(3, 'Conjunto de Lingerie Vermelha Renda', 35.00, 3, '7891234560036', 'Delicatta', 'UN', '2025-11-09 00:54:38', 20, 4, 79.90, 1, 1),
(4, 'Jogo Romântico “Noite Perfeita”', 42.00, 4, '7891234560043', 'Amor & Cia', 'UN', '2025-11-09 00:54:38', 10, 2, 99.90, 1, 5),
(5, 'Perfume Afrodisíaco Unissex 100ml', 25.00, 2, '7891234560050', 'Sensual Aromas', 'UN', '2025-11-09 00:54:38', 15, 3, 59.90, 1, 3),
(6, 'Kit Banho Romântico (Sabonete + Óleo + Vela)', 30.00, 1, '7891234560067', 'Essência Natural', 'KIT', '2025-11-09 00:54:38', 12, 2, 74.90, 1, 4),
(7, 'Lubrificante Natural Neutro 60ml', 10.00, 2, '7891234560074', 'Sensual Aromas', 'UN', '2025-11-09 00:54:38', 25, 5, 22.90, 1, 3),
(8, 'Fantasia de Enfermeira Premium', 40.00, 3, '7891234560081', 'Delicatta', 'UN', '2025-11-09 00:54:38', 8, 2, 94.90, 1, 1);

INSERT INTO `produto_venda` (`produto_id`, `venda_id`, `quantidade`, `preco_unitario`) VALUES
(1, 1, 2, 29.90),
(2, 1, 1, 19.90),
(2, 4, 1, 19.90),
(3, 2, 1, 79.90),
(4, 2, 1, 99.90),
(5, 3, 1, 59.90);

INSERT INTO `venda` (`id_venda`, `usuario_id`, `cliente_id`, `status`, `valor_total`, `desconto`, `observacao`, `data_venda`) VALUES
(1, 2, 1, 1, 139.80, 0.00, 'Venda realizada no balcão - sem desconto', '2025-11-08 21:54:38'),
(2, 3, 2, 1, 179.70, 10.00, 'Cliente fidelidade com 10 reais de desconto', '2025-11-08 21:54:38'),
(3, 4, 3, 1, 99.90, 0.00, 'Venda online com retirada em loja', '2025-11-08 21:54:38'),
(4, 2, 4, 1, 59.90, 0.00, 'Compra via Pix', '2025-11-08 21:54:38'),
(5, 4, 3, 1, 50.00, 20.00, '', '2025-11-09 13:57:54'),
(6, 3, 1, 1, 30.00, 2.00, '', '2025-11-09 13:58:26');

INSERT INTO `venda_formas_pagamento` (`venda_id`, `forma_pagamento_id`, `valor_pago`, `numero_parcelas`) VALUES
(1, 1, 79.70, 1),
(2, 2, 169.70, 2),
(3, 4, 99.90, 1),
(4, 3, 59.90, 1);

INSERT INTO `venda_produto` (`id_venda_produto`, `venda_id`, `produto_id`, `quantidade`, `valor_unitario`) VALUES
(1, 1, 1, 2, 39.85),
(2, 2, 3, 1, 169.70),
(3, 3, 2, 1, 99.90),
(4, 4, 5, 1, 59.90);

-- Confirmação da transação
COMMIT;

-- Restauração das configurações originais
SET SQL_MODE = @OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS = @OLD_UNIQUE_CHECKS;
