-- V12: Adiciona colunas operacionais da entidade Venda de forma idempotente.

DROP PROCEDURE IF EXISTS upgrade_v12_venda_operacional;

DELIMITER $$
CREATE PROCEDURE upgrade_v12_venda_operacional()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS 
    WHERE table_schema = DATABASE() AND table_name = 'venda' AND column_name = 'data_venda'
  ) THEN
    ALTER TABLE venda
      ADD COLUMN status INT NOT NULL DEFAULT 1,
      ADD COLUMN valor_total DECIMAL(10,2) NOT NULL DEFAULT 0,
      ADD COLUMN desconto DECIMAL(10,2) NOT NULL DEFAULT 0,
      ADD COLUMN observacao VARCHAR(255),
      ADD COLUMN data_venda DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
  END IF;
END $$
DELIMITER ;

CALL upgrade_v12_venda_operacional();
DROP PROCEDURE IF EXISTS upgrade_v12_venda_operacional;
