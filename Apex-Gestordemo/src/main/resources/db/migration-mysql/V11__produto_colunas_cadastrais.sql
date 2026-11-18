-- V11: Adiciona colunas cadastrais que faltavam na tabela produtos de forma idempotente.

DROP PROCEDURE IF EXISTS upgrade_v11_produto_cadastrais;

DELIMITER $$
CREATE PROCEDURE upgrade_v11_produto_cadastrais()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS 
    WHERE table_schema = DATABASE() AND table_name = 'produtos' AND column_name = 'codigo_interno'
  ) THEN
    ALTER TABLE produtos
      ADD COLUMN codigo_interno VARCHAR(40),
      ADD COLUMN codigo_original VARCHAR(60),
      ADD COLUMN marca VARCHAR(45),
      ADD COLUMN grupo VARCHAR(60),
      ADD COLUMN subgrupo VARCHAR(60),
      ADD COLUMN unidade_medida VARCHAR(10),
      ADD COLUMN embalagem VARCHAR(40),
      ADD COLUMN data_aquisicao DATETIME;
  END IF;
END $$
DELIMITER ;

CALL upgrade_v11_produto_cadastrais();
DROP PROCEDURE IF EXISTS upgrade_v11_produto_cadastrais;
