package DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProdutoRequestDTO(
        @NotBlank String descricao,
        @DecimalMin(value = "0.00") BigDecimal custo,
        String codigoBarras,
        String codigoInterno,
        String codigoOriginal,
        String marca,
        String grupo,
        String subgrupo,
        String unidadeMedida,
        String embalagem,
        LocalDateTime dataAquisicao,
        @Min(0) Integer quantidadeEstoque,
        @Min(0) Integer estoqueMinimo,
        @NotNull @DecimalMin(value = "0.01") BigDecimal valorVenda,
        @DecimalMin(value = "0.00") BigDecimal margem,
        String localizacao,
        String situacaoFiscal,
        String ncm,
        String cfop,
        @DecimalMin(value = "0.00") BigDecimal icms,
        @DecimalMin(value = "0.00") BigDecimal ipi,
        @DecimalMin(value = "0.00") BigDecimal pis,
        @DecimalMin(value = "0.00") BigDecimal cofins,
        String observacoes,
        Integer status,
        RefDTO fornecedor,
        RefDTO categoria,
        Long empresaId,
        Long filialId) {

    public record RefDTO(Long idFornecedor, Long idCategoria, Long id) {
        public Long fornecedorId() {
            return idFornecedor != null ? idFornecedor : id;
        }

        public Long categoriaId() {
            return idCategoria != null ? idCategoria : id;
        }
    }
}
