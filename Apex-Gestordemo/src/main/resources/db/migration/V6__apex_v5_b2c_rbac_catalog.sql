ALTER TABLE `produtos`
  ADD COLUMN `modelo` varchar(80) DEFAULT NULL AFTER `marca`,
  ADD COLUMN `imagem_url` varchar(500) DEFAULT NULL AFTER `modelo`,
  ADD COLUMN `imagem_mime_type` varchar(80) DEFAULT NULL AFTER `imagem_url`,
  ADD COLUMN `imagem_atualizada_em` datetime DEFAULT NULL AFTER `imagem_mime_type`,
  ADD COLUMN `enriquecimento_catalogo_status` varchar(40) DEFAULT NULL AFTER `imagem_atualizada_em`;

CREATE INDEX `idx_produtos_marca_modelo` ON `produtos` (`marca`, `modelo`);

ALTER TABLE `cliente`
  ADD COLUMN `usuario_id` int(11) DEFAULT NULL AFTER `status`;

CREATE UNIQUE INDEX `unq_cliente_usuario` ON `cliente` (`usuario_id`);

ALTER TABLE `cliente`
  ADD CONSTRAINT `fk_cliente_usuario_b2c` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id_usuario`) ON DELETE SET NULL ON UPDATE CASCADE;

CREATE TABLE IF NOT EXISTS `b2c_cart` (
  `id_b2c_cart` bigint NOT NULL AUTO_INCREMENT,
  `cliente_id` int(11) NOT NULL,
  `usuario_id` int(11) DEFAULT NULL,
  `status` varchar(30) NOT NULL,
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  `atualizado_em` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_b2c_cart`),
  KEY `idx_b2c_cart_cliente_status` (`cliente_id`, `status`),
  KEY `idx_b2c_cart_usuario` (`usuario_id`),
  CONSTRAINT `fk_b2c_cart_cliente` FOREIGN KEY (`cliente_id`) REFERENCES `cliente` (`id_cliente`) ON UPDATE CASCADE,
  CONSTRAINT `fk_b2c_cart_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuario` (`id_usuario`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `b2c_cart_item` (
  `id_b2c_cart_item` bigint NOT NULL AUTO_INCREMENT,
  `cart_id` bigint NOT NULL,
  `produto_id` int(11) NOT NULL,
  `quantidade` int NOT NULL,
  `preco_unitario_snapshot` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id_b2c_cart_item`),
  UNIQUE KEY `unq_b2c_cart_item_produto` (`cart_id`, `produto_id`),
  KEY `idx_b2c_cart_item_produto` (`produto_id`),
  CONSTRAINT `fk_b2c_cart_item_cart` FOREIGN KEY (`cart_id`) REFERENCES `b2c_cart` (`id_b2c_cart`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_b2c_cart_item_produto` FOREIGN KEY (`produto_id`) REFERENCES `produtos` (`id_produto`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `perfil` (`nome`, `status`)
SELECT 'SYSADMIN', 1 WHERE NOT EXISTS (SELECT 1 FROM `perfil` WHERE `nome` = 'SYSADMIN');
INSERT INTO `perfil` (`nome`, `status`)
SELECT 'DONO_GERENTE', 1 WHERE NOT EXISTS (SELECT 1 FROM `perfil` WHERE `nome` = 'DONO_GERENTE');
INSERT INTO `perfil` (`nome`, `status`)
SELECT 'FINANCEIRO', 1 WHERE NOT EXISTS (SELECT 1 FROM `perfil` WHERE `nome` = 'FINANCEIRO');
INSERT INTO `perfil` (`nome`, `status`)
SELECT 'VENDEDOR', 1 WHERE NOT EXISTS (SELECT 1 FROM `perfil` WHERE `nome` = 'VENDEDOR');
INSERT INTO `perfil` (`nome`, `status`)
SELECT 'CAIXA', 1 WHERE NOT EXISTS (SELECT 1 FROM `perfil` WHERE `nome` = 'CAIXA');
INSERT INTO `perfil` (`nome`, `status`)
SELECT 'DESPACHANTE', 1 WHERE NOT EXISTS (SELECT 1 FROM `perfil` WHERE `nome` = 'DESPACHANTE');
INSERT INTO `perfil` (`nome`, `status`)
SELECT 'CLIENTE_B2C', 1 WHERE NOT EXISTS (SELECT 1 FROM `perfil` WHERE `nome` = 'CLIENTE_B2C');

INSERT INTO `usuario` (`nome`, `login`, `senha`, `data_nascimento`, `status`, `perfil_id`)
SELECT 'Apex Sysadmin V5', 'v5_sysadmin', '$2a$12$9BB1hOyShcY.9ABcr2x5lOWUpFlqEPzc9cH/NAkXgkWkrANI85AAq', '1990-01-01', 1, p.`id_perfil`
FROM `perfil` p WHERE p.`nome` = 'SYSADMIN' AND NOT EXISTS (SELECT 1 FROM `usuario` WHERE `login` = 'v5_sysadmin');
INSERT INTO `usuario` (`nome`, `login`, `senha`, `data_nascimento`, `status`, `perfil_id`)
SELECT 'Apex Dono Gerente V5', 'v5_dono', '$2a$12$9BB1hOyShcY.9ABcr2x5lOWUpFlqEPzc9cH/NAkXgkWkrANI85AAq', '1990-01-01', 1, p.`id_perfil`
FROM `perfil` p WHERE p.`nome` = 'DONO_GERENTE' AND NOT EXISTS (SELECT 1 FROM `usuario` WHERE `login` = 'v5_dono');
INSERT INTO `usuario` (`nome`, `login`, `senha`, `data_nascimento`, `status`, `perfil_id`)
SELECT 'Apex Financeiro V5', 'v5_financeiro', '$2a$12$9BB1hOyShcY.9ABcr2x5lOWUpFlqEPzc9cH/NAkXgkWkrANI85AAq', '1990-01-01', 1, p.`id_perfil`
FROM `perfil` p WHERE p.`nome` = 'FINANCEIRO' AND NOT EXISTS (SELECT 1 FROM `usuario` WHERE `login` = 'v5_financeiro');
INSERT INTO `usuario` (`nome`, `login`, `senha`, `data_nascimento`, `status`, `perfil_id`)
SELECT 'Apex Vendedor V5', 'v5_vendedor', '$2a$12$9BB1hOyShcY.9ABcr2x5lOWUpFlqEPzc9cH/NAkXgkWkrANI85AAq', '1990-01-01', 1, p.`id_perfil`
FROM `perfil` p WHERE p.`nome` = 'VENDEDOR' AND NOT EXISTS (SELECT 1 FROM `usuario` WHERE `login` = 'v5_vendedor');
INSERT INTO `usuario` (`nome`, `login`, `senha`, `data_nascimento`, `status`, `perfil_id`)
SELECT 'Apex Caixa V5', 'v5_caixa', '$2a$12$9BB1hOyShcY.9ABcr2x5lOWUpFlqEPzc9cH/NAkXgkWkrANI85AAq', '1990-01-01', 1, p.`id_perfil`
FROM `perfil` p WHERE p.`nome` = 'CAIXA' AND NOT EXISTS (SELECT 1 FROM `usuario` WHERE `login` = 'v5_caixa');
INSERT INTO `usuario` (`nome`, `login`, `senha`, `data_nascimento`, `status`, `perfil_id`)
SELECT 'Apex Despachante V5', 'v5_despachante', '$2a$12$9BB1hOyShcY.9ABcr2x5lOWUpFlqEPzc9cH/NAkXgkWkrANI85AAq', '1990-01-01', 1, p.`id_perfil`
FROM `perfil` p WHERE p.`nome` = 'DESPACHANTE' AND NOT EXISTS (SELECT 1 FROM `usuario` WHERE `login` = 'v5_despachante');
INSERT INTO `usuario` (`nome`, `login`, `senha`, `data_nascimento`, `status`, `perfil_id`)
SELECT 'Cliente B2C V5', 'v5_cliente', '$2a$12$9BB1hOyShcY.9ABcr2x5lOWUpFlqEPzc9cH/NAkXgkWkrANI85AAq', '1995-05-15', 1, p.`id_perfil`
FROM `perfil` p WHERE p.`nome` = 'CLIENTE_B2C' AND NOT EXISTS (SELECT 1 FROM `usuario` WHERE `login` = 'v5_cliente');

INSERT INTO `cliente` (`nome_razao`, `telefone`, `tipo_documento`, `cpf_cnpj`, `data_cadastro`, `status`, `usuario_id`)
SELECT 'Cliente B2C Teste V5', '(11)90000-5000', 1, '50000000001', NOW(), 1, u.`id_usuario`
FROM `usuario` u
WHERE u.`login` = 'v5_cliente'
  AND NOT EXISTS (SELECT 1 FROM `cliente` WHERE `cpf_cnpj` = '50000000001');

INSERT INTO `b2c_cart` (`cliente_id`, `usuario_id`, `status`, `criado_em`, `atualizado_em`)
SELECT c.`id_cliente`, u.`id_usuario`, 'OPEN', NOW(), NOW()
FROM `cliente` c
JOIN `usuario` u ON u.`id_usuario` = c.`usuario_id`
WHERE u.`login` = 'v5_cliente'
  AND NOT EXISTS (SELECT 1 FROM `b2c_cart` bc WHERE bc.`cliente_id` = c.`id_cliente` AND bc.`status` = 'OPEN');

INSERT INTO `b2c_cart_item` (`cart_id`, `produto_id`, `quantidade`, `preco_unitario_snapshot`)
SELECT bc.`id_b2c_cart`, p.`id_produto`, 1, p.`valor_venda`
FROM `b2c_cart` bc
JOIN `cliente` c ON c.`id_cliente` = bc.`cliente_id`
JOIN `usuario` u ON u.`id_usuario` = c.`usuario_id`
JOIN `produtos` p ON p.`id_produto` = 1
WHERE u.`login` = 'v5_cliente'
  AND NOT EXISTS (SELECT 1 FROM `b2c_cart_item` i WHERE i.`cart_id` = bc.`id_b2c_cart` AND i.`produto_id` = p.`id_produto`);

INSERT INTO `b2c_cart_item` (`cart_id`, `produto_id`, `quantidade`, `preco_unitario_snapshot`)
SELECT bc.`id_b2c_cart`, p.`id_produto`, 2, p.`valor_venda`
FROM `b2c_cart` bc
JOIN `cliente` c ON c.`id_cliente` = bc.`cliente_id`
JOIN `usuario` u ON u.`id_usuario` = c.`usuario_id`
JOIN `produtos` p ON p.`id_produto` = 2
WHERE u.`login` = 'v5_cliente'
  AND NOT EXISTS (SELECT 1 FROM `b2c_cart_item` i WHERE i.`cart_id` = bc.`id_b2c_cart` AND i.`produto_id` = p.`id_produto`);
