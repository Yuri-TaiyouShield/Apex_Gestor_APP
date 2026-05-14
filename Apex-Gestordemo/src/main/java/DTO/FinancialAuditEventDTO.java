package DTO;

import java.time.LocalDateTime;

public record FinancialAuditEventDTO(
        Long idEvento,
        String tipoEvento,
        String alvoTipo,
        Long alvoId,
        String atorLogin,
        String valorAnterior,
        String valorNovo,
        String metadados,
        LocalDateTime criadoEm
) {
}
