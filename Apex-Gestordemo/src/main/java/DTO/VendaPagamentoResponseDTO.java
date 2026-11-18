package DTO;

import java.math.BigDecimal;

public record VendaPagamentoResponseDTO(Long vendaId, Long formaPagamentoId, String formaPagamentoNome,
        BigDecimal valorPago, Integer numeroParcelas, Long creditoId) {
}
