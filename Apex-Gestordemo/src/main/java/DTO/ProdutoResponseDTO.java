package DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProdutoResponseDTO(
        Long idProduto,
        String descricao,
        BigDecimal custo,
        String codigoBarras,
        String codigoInterno,
        String codigoOriginal,
        String marca,
        String grupo,
        String subgrupo,
        String unidadeMedida,
        String embalagem,
        LocalDateTime dataAquisicao,
        Integer quantidadeEstoque,
        Integer estoqueMinimo,
        BigDecimal valorVenda,
        BigDecimal margem,
        String localizacao,
        String situacaoFiscal,
        String ncm,
        String cfop,
        BigDecimal icms,
        BigDecimal ipi,
        BigDecimal pis,
        BigDecimal cofins,
        String observacoes,
        Integer status,
        Long empresaId,
        Long filialId,
        FornecedorResumoDTO fornecedor,
        CategoriaResumoDTO categoria) {
}
