CREATE TABLE IF NOT EXISTS `audit_revision` (
  `revision_id` int NOT NULL AUTO_INCREMENT,
  `revision_timestamp` bigint NOT NULL,
  `actor` varchar(120) DEFAULT NULL,
  `source_ip` varchar(64) DEFAULT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  `correlation_id` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`revision_id`),
  KEY `idx_audit_revision_actor` (`actor`),
  KEY `idx_audit_revision_correlation` (`correlation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `financial_document_outbox` (
  `id_financial_document_outbox` bigint NOT NULL AUTO_INCREMENT,
  `document_id` bigint NOT NULL,
  `status` varchar(40) NOT NULL,
  `queued_by` varchar(120) NOT NULL,
  `queued_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `sent_at` datetime DEFAULT NULL,
  `attempts` int NOT NULL DEFAULT 0,
  `last_error` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_financial_document_outbox`),
  KEY `idx_fin_doc_outbox_status` (`status`, `queued_at`),
  KEY `idx_fin_doc_outbox_document` (`document_id`),
  CONSTRAINT `fk_fin_doc_outbox_document` FOREIGN KEY (`document_id`) REFERENCES `financial_digital_document` (`id_financial_digital_document`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `financial_digital_document_AUD` (
  `id_financial_digital_document` bigint NOT NULL,
  `revision_id` int NOT NULL,
  `revision_type` tinyint DEFAULT NULL,
  `tipo_documento` varchar(80) DEFAULT NULL,
  `funcionario_nome` varchar(160) DEFAULT NULL,
  `funcionario_email` varchar(160) DEFAULT NULL,
  `status` varchar(40) DEFAULT NULL,
  `cargo_assinante_obrigatorio` varchar(80) DEFAULT NULL,
  `gerado_por` varchar(120) DEFAULT NULL,
  `gerado_em` datetime DEFAULT NULL,
  `assinado_por` varchar(160) DEFAULT NULL,
  `cargo_assinante` varchar(80) DEFAULT NULL,
  `assinado_em` datetime DEFAULT NULL,
  `enviado_em` datetime DEFAULT NULL,
  `assinatura_digital_hash` varchar(128) DEFAULT NULL,
  `conteudo_hash` varchar(128) DEFAULT NULL,
  `conteudo` longtext DEFAULT NULL,
  `assunto_email` varchar(180) DEFAULT NULL,
  `mensagem_email` longtext DEFAULT NULL,
  `referencia` varchar(120) DEFAULT NULL,
  `ultimo_erro` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_financial_digital_document`, `revision_id`),
  KEY `idx_fin_doc_aud_revision` (`revision_id`),
  CONSTRAINT `fk_fin_doc_aud_revision` FOREIGN KEY (`revision_id`) REFERENCES `audit_revision` (`revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `financial_calculation_AUD` (
  `id_financial_calculation` bigint NOT NULL,
  `revision_id` int NOT NULL,
  `revision_type` tinyint DEFAULT NULL,
  `tipo_calculo` varchar(60) DEFAULT NULL,
  `ator_login` varchar(120) DEFAULT NULL,
  `input_hash` varchar(128) DEFAULT NULL,
  `input_snapshot` longtext DEFAULT NULL,
  `resultado_snapshot` longtext DEFAULT NULL,
  `criado_em` datetime DEFAULT NULL,
  PRIMARY KEY (`id_financial_calculation`, `revision_id`),
  KEY `idx_fin_calc_aud_revision` (`revision_id`),
  CONSTRAINT `fk_fin_calc_aud_revision` FOREIGN KEY (`revision_id`) REFERENCES `audit_revision` (`revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `financial_document_outbox_AUD` (
  `id_financial_document_outbox` bigint NOT NULL,
  `revision_id` int NOT NULL,
  `revision_type` tinyint DEFAULT NULL,
  `document_id` bigint DEFAULT NULL,
  `status` varchar(40) DEFAULT NULL,
  `queued_by` varchar(120) DEFAULT NULL,
  `queued_at` datetime DEFAULT NULL,
  `sent_at` datetime DEFAULT NULL,
  `attempts` int DEFAULT NULL,
  `last_error` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_financial_document_outbox`, `revision_id`),
  KEY `idx_fin_doc_outbox_aud_revision` (`revision_id`),
  CONSTRAINT `fk_fin_doc_outbox_aud_revision` FOREIGN KEY (`revision_id`) REFERENCES `audit_revision` (`revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `produtos_AUD` (
  `id_produto` bigint NOT NULL,
  `revision_id` int NOT NULL,
  `revision_type` tinyint DEFAULT NULL,
  `descricao` varchar(60) DEFAULT NULL,
  `custo` decimal(10,2) DEFAULT NULL,
  `fornecedor_id` bigint DEFAULT NULL,
  `codigo_barras` varchar(20) DEFAULT NULL,
  `marca` varchar(45) DEFAULT NULL,
  `unidade_medida` varchar(10) DEFAULT NULL,
  `data_aquisicao` datetime DEFAULT NULL,
  `quantidade_estoque` int DEFAULT NULL,
  `estoque_minimo` int DEFAULT NULL,
  `valor_venda` decimal(10,2) DEFAULT NULL,
  `status` int DEFAULT NULL,
  `categoria_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id_produto`, `revision_id`),
  KEY `idx_produtos_aud_revision` (`revision_id`),
  CONSTRAINT `fk_produtos_aud_revision` FOREIGN KEY (`revision_id`) REFERENCES `audit_revision` (`revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `venda_AUD` (
  `id_venda` bigint NOT NULL,
  `revision_id` int NOT NULL,
  `revision_type` tinyint DEFAULT NULL,
  `usuario_id` bigint DEFAULT NULL,
  `cliente_id` bigint DEFAULT NULL,
  `status` int DEFAULT NULL,
  `valor_total` decimal(10,2) DEFAULT NULL,
  `desconto` decimal(10,2) DEFAULT NULL,
  `observacao` varchar(255) DEFAULT NULL,
  `data_venda` datetime DEFAULT NULL,
  PRIMARY KEY (`id_venda`, `revision_id`),
  KEY `idx_venda_aud_revision` (`revision_id`),
  CONSTRAINT `fk_venda_aud_revision` FOREIGN KEY (`revision_id`) REFERENCES `audit_revision` (`revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `produto_venda_AUD` (
  `produto_id` bigint NOT NULL,
  `venda_id` bigint NOT NULL,
  `revision_id` int NOT NULL,
  `revision_type` tinyint DEFAULT NULL,
  `quantidade` int DEFAULT NULL,
  `preco_unitario` decimal(10,2) DEFAULT NULL,
  `custo_unitario` decimal(10,2) DEFAULT NULL,
  `preco_total` decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (`produto_id`, `venda_id`, `revision_id`),
  KEY `idx_produto_venda_aud_revision` (`revision_id`),
  CONSTRAINT `fk_produto_venda_aud_revision` FOREIGN KEY (`revision_id`) REFERENCES `audit_revision` (`revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `perfil` (`id_perfil`, `nome`, `status`) VALUES
(8, 'gestor', 1),
(9, 'auditor', 1);
