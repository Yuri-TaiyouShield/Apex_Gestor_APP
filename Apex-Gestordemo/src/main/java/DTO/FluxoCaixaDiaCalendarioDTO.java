package DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FluxoCaixaDiaCalendarioDTO(
        LocalDate data,
        String tipoDado,
        BigDecimal entradasRealizadas,
        BigDecimal saidasRealizadas,
        BigDecimal entradasPrevistas,
        BigDecimal saidasPrevistas,
        BigDecimal saldoLiquidoDia,
        BigDecimal saldoAcumuladoProjetado,
        int quantidadeTitulosVencendo,
        boolean riscoSaldoNegativo) {
}
