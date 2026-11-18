package DTO;

import java.math.BigDecimal;

public record CaixaFormaPagamentoResumoDTO(Long formaPagamentoId, String nome, BigDecimal total,
        Long quantidadePagamentos) {
}
