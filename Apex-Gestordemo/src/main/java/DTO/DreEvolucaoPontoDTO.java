package DTO;

import java.math.BigDecimal;

public record DreEvolucaoPontoDTO(
        String competencia,
        String rotulo,
        BigDecimal receitaBruta,
        BigDecimal receitaLiquida,
        BigDecimal cmv,
        BigDecimal lucroBruto,
        BigDecimal despesasTotais,
        BigDecimal resultadoOperacional,
        BigDecimal resultadoLiquidoGerencial,
        BigDecimal margemBrutaPercentual,
        BigDecimal margemOperacionalPercentual,
        BigDecimal margemLiquidaPercentual,
        BigDecimal saldoRealizadoCaixa) {
}
