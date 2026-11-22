package DTO;

import Model.StatusConciliacaoFinanceira;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ConciliacaoItemResponseDTO(
        Long idConciliacaoItem,
        Long contaFinanceiraId,
        String contaFinanceiraNome,
        String tipoConciliacao,
        String formatoOrigem,
        String identificadorExtrato,
        LocalDate dataExtrato,
        String descricaoExtrato,
        BigDecimal valorExtrato,
        String naturezaExtrato,
        StatusConciliacaoFinanceira statusConciliacao,
        Long movimentacaoVinculadaId,
        String documentoOrigemTipo,
        Long documentoOrigemId,
        BigDecimal diferencaValor,
        String observacao,
        String conciliadoPor,
        LocalDateTime conciliadoEm) {
}
