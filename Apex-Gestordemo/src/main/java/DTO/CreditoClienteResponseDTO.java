package DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreditoClienteResponseDTO(Long idCredito, Long clienteId, Long vendaOrigemId,
        Long devolucaoId, BigDecimal valorOriginal, BigDecimal saldo, String status,
        Long empresaId, Long filialId, LocalDateTime criadoEm) {
}
