package DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BaixaTituloFinanceiroRequestDTO(
        @NotNull @Positive BigDecimal valorLiquidado,
        Long contaFinanceiraId,
        LocalDate dataLiquidacao,
        BigDecimal valorTaxa,
        BigDecimal taxaAntecipacao,
        BigDecimal valorJurosDesconto,
        Boolean antecipacaoRecebivel,
        String observacao,
        String idempotencyKey) {
}
