-- Apex Gestor v5.0 Enterprise - DER base para MySQL Workbench
-- Use em File > Run SQL Script ou importe para criar o modelo EER.

CREATE SCHEMA IF NOT EXISTS `apex_gestor_v5` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `apex_gestor_v5`;

CREATE TABLE `tenant` (
  `id_tenant` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(60) NOT NULL,
  `legal_name` varchar(160) NOT NULL,
  `storefront_name` varchar(120) NOT NULL,
  `domain_name` varchar(160) DEFAULT NULL,
  `support_email` varchar(160) DEFAULT NULL,
  `primary_color` varchar(20) NOT NULL,
  `secondary_color` varchar(20) NOT NULL,
  `logo_url` varchar(500) DEFAULT NULL,
  `status` varchar(30) NOT NULL,
  PRIMARY KEY (`id_tenant`),
  UNIQUE KEY `unq_tenant_code` (`tenant_code`)
) ENGINE=InnoDB;

CREATE TABLE `subscription_plan` (
  `id_subscription_plan` bigint NOT NULL AUTO_INCREMENT,
  `plan_code` varchar(40) NOT NULL,
  `display_name` varchar(80) NOT NULL,
  `tier` varchar(30) NOT NULL,
  `monthly_price` decimal(10,2) NOT NULL,
  `status` varchar(30) NOT NULL,
  PRIMARY KEY (`id_subscription_plan`),
  UNIQUE KEY `unq_subscription_plan_code` (`plan_code`)
) ENGINE=InnoDB;

CREATE TABLE `feature_toggle` (
  `id_feature_toggle` bigint NOT NULL AUTO_INCREMENT,
  `feature_key` varchar(80) NOT NULL,
  `display_name` varchar(120) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `minimum_tier` varchar(30) NOT NULL,
  `status` varchar(30) NOT NULL,
  PRIMARY KEY (`id_feature_toggle`),
  UNIQUE KEY `unq_feature_key` (`feature_key`)
) ENGINE=InnoDB;

CREATE TABLE `tenant_subscription` (
  `id_tenant_subscription` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `subscription_plan_id` bigint NOT NULL,
  `starts_at` datetime NOT NULL,
  `ends_at` datetime DEFAULT NULL,
  `status` varchar(30) NOT NULL,
  PRIMARY KEY (`id_tenant_subscription`),
  KEY `idx_tenant_subscription_tenant_status` (`tenant_id`, `status`),
  CONSTRAINT `fk_ts_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id_tenant`),
  CONSTRAINT `fk_ts_plan` FOREIGN KEY (`subscription_plan_id`) REFERENCES `subscription_plan` (`id_subscription_plan`)
) ENGINE=InnoDB;

CREATE TABLE `perfil` (
  `id_perfil` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(45) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id_perfil`),
  UNIQUE KEY `unq_perfil_nome` (`nome`)
) ENGINE=InnoDB;

CREATE TABLE `usuario` (
  `id_usuario` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(80) NOT NULL,
  `login` varchar(80) NOT NULL,
  `senha` varchar(255) NOT NULL,
  `data_nascimento` date NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `perfil_id` int NOT NULL,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `unq_usuario_login` (`login`),
  CONSTRAINT `fk_usuario_perfil` FOREIGN KEY (`perfil_id`) REFERENCES `perfil` (`id_perfil`)
) ENGINE=InnoDB;

CREATE TABLE `cliente` (
  `id_cliente` int NOT NULL AUTO_INCREMENT,
  `usuario_id` int DEFAULT NULL,
  `nome_razao` varchar(100) NOT NULL,
  `telefone` varchar(15) NOT NULL,
  `tipo_documento` tinyint DEFAULT NULL,
  `cpf_cnpj` varchar(14) NOT NULL,
  `data_cadastro` datetime NOT NULL DEFAULT current_timestamp(),
  `status` tinyint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id_cliente`),
  UNIQUE KEY `unq_cliente_documento` (`tipo_documento`, `cpf_cnpj`),
  UNIQUE KEY `unq_cliente_usuario` (`usuario_id`),
  CONSTRAINT `fk_cliente_usuario_b2c` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id_usuario`) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE `categoria` (
  `id_categoria` int NOT NULL AUTO_INCREMENT,
  `descricao` varchar(80) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id_categoria`)
) ENGINE=InnoDB;

CREATE TABLE `fornecedor` (
  `id_fornecedor` int NOT NULL AUTO_INCREMENT,
  `razao_social` varchar(120) NOT NULL,
  `nome_fantasia` varchar(80) DEFAULT NULL,
  `cnpj` varchar(18) NOT NULL,
  `telefone` varchar(20) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id_fornecedor`),
  UNIQUE KEY `unq_fornecedor_cnpj` (`cnpj`)
) ENGINE=InnoDB;

CREATE TABLE `produtos` (
  `id_produto` int NOT NULL AUTO_INCREMENT,
  `descricao` varchar(120) NOT NULL,
  `marca` varchar(80) DEFAULT NULL,
  `modelo` varchar(80) DEFAULT NULL,
  `codigo_barras` varchar(30) DEFAULT NULL,
  `imagem_url` varchar(500) DEFAULT NULL,
  `imagem_mime_type` varchar(80) DEFAULT NULL,
  `imagem_atualizada_em` datetime DEFAULT NULL,
  `enriquecimento_catalogo_status` varchar(40) DEFAULT NULL,
  `custo` decimal(10,2) NOT NULL DEFAULT 0,
  `valor_venda` decimal(10,2) NOT NULL DEFAULT 0,
  `quantidade_estoque` int NOT NULL DEFAULT 0,
  `estoque_minimo` int NOT NULL DEFAULT 0,
  `fornecedor_id` int DEFAULT NULL,
  `categoria_id` int NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id_produto`),
  KEY `idx_produtos_busca` (`status`, `descricao`, `marca`, `modelo`),
  CONSTRAINT `fk_produto_categoria` FOREIGN KEY (`categoria_id`) REFERENCES `categoria` (`id_categoria`),
  CONSTRAINT `fk_produto_fornecedor` FOREIGN KEY (`fornecedor_id`) REFERENCES `fornecedor` (`id_fornecedor`)
) ENGINE=InnoDB;

CREATE TABLE `b2c_cart` (
  `id_b2c_cart` bigint NOT NULL AUTO_INCREMENT,
  `cliente_id` int NOT NULL,
  `usuario_id` int DEFAULT NULL,
  `status` varchar(30) NOT NULL,
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  `atualizado_em` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_b2c_cart`),
  KEY `idx_b2c_cart_cliente_status` (`cliente_id`, `status`),
  CONSTRAINT `fk_b2c_cart_cliente` FOREIGN KEY (`cliente_id`) REFERENCES `cliente` (`id_cliente`),
  CONSTRAINT `fk_b2c_cart_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id_usuario`) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE `b2c_cart_item` (
  `id_b2c_cart_item` bigint NOT NULL AUTO_INCREMENT,
  `cart_id` bigint NOT NULL,
  `produto_id` int NOT NULL,
  `quantidade` int NOT NULL,
  `preco_unitario_snapshot` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id_b2c_cart_item`),
  UNIQUE KEY `unq_b2c_cart_item_produto` (`cart_id`, `produto_id`),
  CONSTRAINT `fk_b2c_cart_item_cart` FOREIGN KEY (`cart_id`) REFERENCES `b2c_cart` (`id_b2c_cart`) ON DELETE CASCADE,
  CONSTRAINT `fk_b2c_cart_item_produto` FOREIGN KEY (`produto_id`) REFERENCES `produtos` (`id_produto`)
) ENGINE=InnoDB;

CREATE TABLE `venda` (
  `id_venda` int NOT NULL AUTO_INCREMENT,
  `usuario_id` int NOT NULL,
  `cliente_id` int NOT NULL,
  `status` tinyint NOT NULL,
  `valor_total` decimal(10,2) NOT NULL,
  `desconto` decimal(10,2) DEFAULT 0,
  `observacao` varchar(255) DEFAULT NULL,
  `data_venda` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_venda`),
  KEY `idx_venda_cliente_data` (`cliente_id`, `data_venda`),
  CONSTRAINT `fk_venda_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id_usuario`),
  CONSTRAINT `fk_venda_cliente` FOREIGN KEY (`cliente_id`) REFERENCES `cliente` (`id_cliente`)
) ENGINE=InnoDB;

CREATE TABLE `produto_venda` (
  `produto_id` int NOT NULL,
  `venda_id` int NOT NULL,
  `quantidade` int NOT NULL,
  `preco_unitario` decimal(10,2) NOT NULL,
  `custo_unitario` decimal(10,2) NOT NULL,
  `preco_total` decimal(10,2) NOT NULL,
  PRIMARY KEY (`produto_id`, `venda_id`),
  CONSTRAINT `fk_pv_produto` FOREIGN KEY (`produto_id`) REFERENCES `produtos` (`id_produto`),
  CONSTRAINT `fk_pv_venda` FOREIGN KEY (`venda_id`) REFERENCES `venda` (`id_venda`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `financial_calculation` (
  `id_financial_calculation` bigint NOT NULL AUTO_INCREMENT,
  `tipo_calculo` varchar(60) NOT NULL,
  `ator_login` varchar(120) NOT NULL,
  `input_hash` varchar(128) NOT NULL,
  `input_snapshot` longtext NOT NULL,
  `resultado_snapshot` longtext NOT NULL,
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_financial_calculation`)
) ENGINE=InnoDB;

CREATE TABLE `financial_digital_document` (
  `id_financial_digital_document` bigint NOT NULL AUTO_INCREMENT,
  `tipo_documento` varchar(80) NOT NULL,
  `funcionario_nome` varchar(160) NOT NULL,
  `funcionario_email` varchar(160) NOT NULL,
  `status` varchar(40) NOT NULL,
  `assinatura_digital_hash` varchar(128) DEFAULT NULL,
  `conteudo_hash` varchar(128) NOT NULL,
  `conteudo` longtext NOT NULL,
  PRIMARY KEY (`id_financial_digital_document`)
) ENGINE=InnoDB;
