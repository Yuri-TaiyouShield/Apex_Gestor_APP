ALTER TABLE `license_activation`
  ADD COLUMN `tenant_code` varchar(60) NOT NULL DEFAULT 'apex-demo' AFTER `license_plan`,
  ADD KEY `idx_license_tenant` (`tenant_code`);

CREATE TABLE IF NOT EXISTS `tenant` (
  `id_tenant` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(60) NOT NULL,
  `legal_name` varchar(160) NOT NULL,
  `trade_name` varchar(120) NOT NULL,
  `domain` varchar(160) DEFAULT NULL,
  `support_email` varchar(160) DEFAULT NULL,
  `brand_primary_color` varchar(12) NOT NULL DEFAULT '#0b3a42',
  `brand_secondary_color` varchar(12) NOT NULL DEFAULT '#1e3a8a',
  `brand_logo_url` varchar(255) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_tenant`),
  UNIQUE KEY `unq_tenant_code` (`tenant_code`),
  KEY `idx_tenant_status` (`status`),
  KEY `idx_tenant_domain` (`domain`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `subscription_plan` (
  `id_subscription_plan` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(40) NOT NULL,
  `tier_name` varchar(80) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `monthly_price` decimal(12,2) NOT NULL DEFAULT 0.00,
  `max_stores` int NOT NULL DEFAULT 1,
  `max_users` int NOT NULL DEFAULT 3,
  `omnichannel_enabled` bit(1) NOT NULL DEFAULT b'0',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (`id_subscription_plan`),
  UNIQUE KEY `unq_subscription_plan_code` (`code`),
  KEY `idx_subscription_plan_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `feature_toggle` (
  `id_feature_toggle` bigint NOT NULL AUTO_INCREMENT,
  `feature_key` varchar(80) NOT NULL,
  `display_name` varchar(120) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `minimum_plan_code` varchar(40) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (`id_feature_toggle`),
  UNIQUE KEY `unq_feature_key` (`feature_key`),
  KEY `idx_feature_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `plan_feature_toggle` (
  `plan_id` bigint NOT NULL,
  `feature_id` bigint NOT NULL,
  PRIMARY KEY (`plan_id`, `feature_id`),
  KEY `idx_plan_feature_feature` (`feature_id`),
  CONSTRAINT `fk_plan_feature_plan` FOREIGN KEY (`plan_id`) REFERENCES `subscription_plan` (`id_subscription_plan`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_plan_feature_feature` FOREIGN KEY (`feature_id`) REFERENCES `feature_toggle` (`id_feature_toggle`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tenant_subscription` (
  `id_tenant_subscription` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ends_at` datetime DEFAULT NULL,
  `billing_cycle` varchar(20) NOT NULL DEFAULT 'MONTHLY',
  `contracted_apps` varchar(180) NOT NULL DEFAULT 'desktop,mobile-staff,mobile-client,web-client',
  `module_overrides_json` longtext DEFAULT NULL,
  PRIMARY KEY (`id_tenant_subscription`),
  KEY `idx_tenant_subscription_tenant_status` (`tenant_id`, `status`),
  KEY `idx_tenant_subscription_period` (`started_at`, `ends_at`),
  KEY `idx_tenant_subscription_plan` (`plan_id`),
  CONSTRAINT `fk_tenant_subscription_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id_tenant`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_tenant_subscription_plan` FOREIGN KEY (`plan_id`) REFERENCES `subscription_plan` (`id_subscription_plan`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tenant_feature_override` (
  `id_tenant_feature_override` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `feature_id` bigint NOT NULL,
  `enabled` bit(1) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_tenant_feature_override`),
  UNIQUE KEY `unq_tenant_feature_override` (`tenant_id`, `feature_id`),
  KEY `idx_tenant_feature_override_tenant` (`tenant_id`),
  KEY `idx_tenant_feature_override_feature` (`feature_id`),
  CONSTRAINT `fk_tenant_feature_override_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id_tenant`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_tenant_feature_override_feature` FOREIGN KEY (`feature_id`) REFERENCES `feature_toggle` (`id_feature_toggle`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `pricing_rule` (
  `id_pricing_rule` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `rule_type` varchar(40) NOT NULL DEFAULT 'DEFAULT',
  `min_margin_percent` decimal(8,4) NOT NULL DEFAULT 12.0000,
  `default_margin_percent` decimal(8,4) NOT NULL DEFAULT 22.0000,
  `fixed_expense_percent` decimal(8,4) NOT NULL DEFAULT 8.0000,
  `tax_percent` decimal(8,4) NOT NULL DEFAULT 6.0000,
  `commission_percent` decimal(8,4) NOT NULL DEFAULT 0.0000,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `effective_from` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `effective_until` datetime DEFAULT NULL,
  PRIMARY KEY (`id_pricing_rule`),
  KEY `idx_pricing_rule_tenant_type` (`tenant_id`, `rule_type`, `status`),
  KEY `idx_pricing_rule_effective` (`effective_from`, `effective_until`),
  CONSTRAINT `fk_pricing_rule_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id_tenant`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `commission_pool` (
  `id_commission_pool` bigint NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint NOT NULL,
  `order_id` varchar(80) DEFAULT NULL,
  `branch_code` varchar(60) NOT NULL,
  `source_channel` varchar(40) NOT NULL,
  `gross_amount` decimal(12,2) NOT NULL,
  `commission_base` decimal(12,2) NOT NULL,
  `pool_percent` decimal(8,4) NOT NULL,
  `pool_amount` decimal(12,2) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'RESERVED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `settled_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id_commission_pool`),
  KEY `idx_commission_pool_tenant_status` (`tenant_id`, `status`),
  KEY `idx_commission_pool_order` (`tenant_id`, `order_id`),
  CONSTRAINT `fk_commission_pool_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id_tenant`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `commission_pool_member` (
  `id_commission_pool_member` bigint NOT NULL AUTO_INCREMENT,
  `pool_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `member_name` varchar(120) NOT NULL,
  `share_percent` decimal(8,4) NOT NULL,
  `commission_amount` decimal(12,2) NOT NULL,
  PRIMARY KEY (`id_commission_pool_member`),
  KEY `idx_commission_pool_member_pool` (`pool_id`),
  KEY `idx_commission_pool_member_user` (`user_id`),
  CONSTRAINT `fk_commission_pool_member_pool` FOREIGN KEY (`pool_id`) REFERENCES `commission_pool` (`id_commission_pool`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `commission_pool_AUD` (
  `id_commission_pool` bigint NOT NULL,
  `revision_id` int NOT NULL,
  `revision_type` tinyint DEFAULT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `order_id` varchar(80) DEFAULT NULL,
  `branch_code` varchar(60) DEFAULT NULL,
  `source_channel` varchar(40) DEFAULT NULL,
  `gross_amount` decimal(12,2) DEFAULT NULL,
  `commission_base` decimal(12,2) DEFAULT NULL,
  `pool_percent` decimal(8,4) DEFAULT NULL,
  `pool_amount` decimal(12,2) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `settled_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id_commission_pool`, `revision_id`),
  KEY `idx_commission_pool_aud_revision` (`revision_id`),
  CONSTRAINT `fk_commission_pool_aud_revision` FOREIGN KEY (`revision_id`) REFERENCES `audit_revision` (`revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `commission_pool_member_AUD` (
  `id_commission_pool_member` bigint NOT NULL,
  `revision_id` int NOT NULL,
  `revision_type` tinyint DEFAULT NULL,
  `pool_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `member_name` varchar(120) DEFAULT NULL,
  `share_percent` decimal(8,4) DEFAULT NULL,
  `commission_amount` decimal(12,2) DEFAULT NULL,
  PRIMARY KEY (`id_commission_pool_member`, `revision_id`),
  KEY `idx_commission_pool_member_aud_revision` (`revision_id`),
  CONSTRAINT `fk_commission_pool_member_aud_revision` FOREIGN KEY (`revision_id`) REFERENCES `audit_revision` (`revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `tenant` (`tenant_code`, `legal_name`, `trade_name`, `domain`, `support_email`, `brand_primary_color`, `brand_secondary_color`, `brand_logo_url`, `status`) VALUES
('apex-demo', 'Apex Demo Comercio LTDA', 'Apex Demo Store', 'demo.apexgestor.local', 'suporte@apexgestor.local', '#0b3a42', '#1e3a8a', NULL, 'ACTIVE');

INSERT IGNORE INTO `subscription_plan` (`code`, `tier_name`, `description`, `monthly_price`, `max_stores`, `max_users`, `omnichannel_enabled`, `status`) VALUES
('ESSENTIAL', 'Essential', 'ERP, PDV e financeiro essencial para micro e pequenas empresas.', 99.00, 1, 5, b'0', 'ACTIVE'),
('GROWTH', 'Growth', 'Operacao com comissionamento, financeiro avancado e canais digitais.', 249.00, 3, 20, b'1', 'ACTIVE'),
('PREMIUM', 'Premium', 'Rede multi-loja com roteamento logistico, white-label e relatorios hierarquicos.', 599.00, 999, 999, b'1', 'ACTIVE');

INSERT IGNORE INTO `feature_toggle` (`feature_key`, `display_name`, `description`, `minimum_plan_code`, `status`) VALUES
('BASIC_PRICING', 'Precificacao base', 'Markup automatico com custos, despesas, impostos e margem.', 'ESSENTIAL', 'ACTIVE'),
('FINANCIAL_CORE', 'Financeiro essencial', 'Contas, despesas, calculos essenciais e auditoria basica.', 'ESSENTIAL', 'ACTIVE'),
('ADVANCED_FINANCE', 'Financeiro avancado', 'AdmCalc completo, simulacoes e relatorios financeiros detalhados.', 'GROWTH', 'ACTIVE'),
('COMMISSION_OMNICHANNEL', 'Comissionamento omnichannel', 'Comissao por vendedor e pool para vendas online despachadas por filial.', 'GROWTH', 'ACTIVE'),
('B2C_WHITE_LABEL', 'B2C white-label', 'Site e app do cliente com marca e cores do tenant.', 'GROWTH', 'ACTIVE'),
('MULTI_STORE_ROUTING', 'Roteamento multi-loja', 'Geo-routing para despacho pela filial mais proxima.', 'PREMIUM', 'ACTIVE'),
('SOFT_STOCK_ALLOCATION', 'Reserva temporaria de estoque', 'Soft allocation de estoque para pedidos digitais.', 'PREMIUM', 'ACTIVE'),
('HIERARCHICAL_REPORTS', 'Relatorios hierarquicos', 'Visao matriz, regioes e filiais por nivel de permissao.', 'PREMIUM', 'ACTIVE');

INSERT IGNORE INTO `plan_feature_toggle` (`plan_id`, `feature_id`)
SELECT p.`id_subscription_plan`, f.`id_feature_toggle`
FROM `subscription_plan` p
JOIN `feature_toggle` f ON f.`feature_key` IN ('BASIC_PRICING', 'FINANCIAL_CORE')
WHERE p.`code` = 'ESSENTIAL';

INSERT IGNORE INTO `plan_feature_toggle` (`plan_id`, `feature_id`)
SELECT p.`id_subscription_plan`, f.`id_feature_toggle`
FROM `subscription_plan` p
JOIN `feature_toggle` f ON f.`feature_key` IN ('BASIC_PRICING', 'FINANCIAL_CORE', 'ADVANCED_FINANCE', 'COMMISSION_OMNICHANNEL', 'B2C_WHITE_LABEL')
WHERE p.`code` = 'GROWTH';

INSERT IGNORE INTO `plan_feature_toggle` (`plan_id`, `feature_id`)
SELECT p.`id_subscription_plan`, f.`id_feature_toggle`
FROM `subscription_plan` p
JOIN `feature_toggle` f ON f.`feature_key` IN ('BASIC_PRICING', 'FINANCIAL_CORE', 'ADVANCED_FINANCE', 'COMMISSION_OMNICHANNEL', 'B2C_WHITE_LABEL', 'MULTI_STORE_ROUTING', 'SOFT_STOCK_ALLOCATION', 'HIERARCHICAL_REPORTS')
WHERE p.`code` = 'PREMIUM';

INSERT IGNORE INTO `tenant_subscription` (`tenant_id`, `plan_id`, `status`, `billing_cycle`, `contracted_apps`)
SELECT t.`id_tenant`, p.`id_subscription_plan`, 'ACTIVE', 'MONTHLY', 'desktop,mobile-staff,mobile-client,web-client'
FROM `tenant` t
JOIN `subscription_plan` p ON p.`code` = 'PREMIUM'
WHERE t.`tenant_code` = 'apex-demo';

INSERT IGNORE INTO `pricing_rule` (`tenant_id`, `rule_type`, `min_margin_percent`, `default_margin_percent`, `fixed_expense_percent`, `tax_percent`, `commission_percent`, `status`)
SELECT t.`id_tenant`, 'DEFAULT', 12.0000, 22.0000, 8.0000, 6.0000, 3.0000, 'ACTIVE'
FROM `tenant` t
WHERE t.`tenant_code` = 'apex-demo';
