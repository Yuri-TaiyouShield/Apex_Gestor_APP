-- V11: Adiciona colunas cadastrais que faltavam na tabela produtos (H2 / generico).

ALTER TABLE produtos ADD COLUMN IF NOT EXISTS codigo_interno VARCHAR(40);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS codigo_original VARCHAR(60);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS marca VARCHAR(45);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS grupo VARCHAR(60);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS subgrupo VARCHAR(60);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS unidade_medida VARCHAR(10);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS embalagem VARCHAR(40);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS data_aquisicao DATETIME;
