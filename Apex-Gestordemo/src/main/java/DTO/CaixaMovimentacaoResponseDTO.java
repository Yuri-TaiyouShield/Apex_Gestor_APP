package DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CaixaMovimentacaoResponseDTO(
        Long idCaixaMovimentacao,
        String tipo,
        BigDecimal valor,
        String observacao,
        String referenciaTipo,
        Long referenciaId,
        LocalDateTime criadoEm) {
}
