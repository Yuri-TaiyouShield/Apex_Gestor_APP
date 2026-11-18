package DTO;

import java.math.BigDecimal;

public record DreIndicadoresResumoDTO(
        BigDecimal receitaBruta,
        BigDecimal deducoesTotais,
        BigDecimal receitaLiquida,
        BigDecimal cmv,
        BigDecimal lucroBruto,
        BigDecimal margemBrutaPercentual,
        BigDecimal despesasOperacionaisTotais,
        BigDecimal despesasFixas,
        BigDecimal despesasVariaveis,
        BigDecimal custosVariaveisTotais,
        BigDecimal margemContribuicaoValor,
        BigDecimal margemContribuicaoPercentual,
        BigDecimal pontoEquilibrioContabilReceita,
        BigDecimal resultadoOperacional,
        BigDecimal margemOperacionalPercentual,
        BigDecimal resultadoFinanceiroLiquido,
        BigDecimal resultadoAntesTributos,
        BigDecimal tributosSobreLucro,
        BigDecimal resultadoLiquidoGerencial,
        BigDecimal margemLiquidaPercentual,
        BigDecimal entradasRealizadasCaixa,
        BigDecimal saidasRealizadasCaixa,
        BigDecimal saldoRealizadoCaixa,
        BigDecimal diferencaCompetenciaVsCaixa) {
}
