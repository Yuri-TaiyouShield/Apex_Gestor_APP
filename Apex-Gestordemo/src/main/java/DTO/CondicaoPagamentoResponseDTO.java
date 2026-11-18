package DTO;

import java.math.BigDecimal;

public record CondicaoPagamentoResponseDTO(
        Long idCondicaoPagamento,
        String nome,
        String tipoPagamento,
        Integer quantidadeParcelas,
        Integer prazoDias,
        BigDecimal percentual,
        String restricoes,
        Integer status,
        Long empresaId,
        Long filialId) {
}
