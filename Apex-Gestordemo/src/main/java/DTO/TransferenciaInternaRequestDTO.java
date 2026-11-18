package DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferenciaInternaRequestDTO(
        @NotNull Long contaOrigemId,
        @NotNull Long contaDestinoId,
        @NotNull @Positive BigDecimal valor,
        LocalDate dataTransferencia,
        String descricao,
        String documentoReferencia,
        String idempotencyKey) {
}
