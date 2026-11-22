-- V10: Adiciona campos fiscais e tributarios a tabela produtos de forma idempotente.

DROP PROCEDURE IF EXISTS upgrade_v10_produto_fiscais;

DELIMITER $$
CREATE PROCEDURE upgrade_v10_produto_fiscais()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS 
    WHERE table_schema = DATABASE() AND table_name = 'produtos' AND column_name = 'margem'
  ) THEN
    ALTER TABLE produtos
      ADD COLUMN margem DECIMAL(10,2),
      ADD COLUMN localizacao VARCHAR(80),
      ADD COLUMN situacao_fiscal VARCHAR(60),
      ADD COLUMN ncm VARCHAR(12),
      ADD COLUMN cfop VARCHAR(12),
      ADD COLUMN icms DECIMAL(7,2),
      ADD COLUMN ipi DECIMAL(7,2),
      ADD COLUMN pis DECIMAL(7,2),
      ADD COLUMN cofins DECIMAL(7,2),
      ADD COLUMN observacoes VARCHAR(500);
  END IF;
END $$
DELIMITER ;

CALL upgrade_v10_produto_fiscais();
DROP PROCEDURE IF EXISTS upgrade_v10_produto_fiscais;
