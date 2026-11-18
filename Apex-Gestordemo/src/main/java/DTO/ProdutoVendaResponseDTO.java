package DTO;

import java.math.BigDecimal;

public record ProdutoVendaResponseDTO(Long produtoId, Long vendaId, String produtoDescricao,
        Integer quantidade, BigDecimal precoUnitario, BigDecimal precoTotal) {
}
