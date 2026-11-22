package DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContaReceberResponseDTO(Long idContaReceber, Long vendaId, String numeroVenda,
        Long clienteId, String clienteNome, Long formaPagamentoId, String formaPagamentoNome,
        Long condicaoPagamentoId, Long caixaSessaoId, BigDecimal valorBruto, BigDecimal desconto,
        BigDecimal valorLiquido, BigDecimal valorPago, BigDecimal saldo, LocalDate vencimento,
        String status, Long empresaId, Long filialId) {
}
