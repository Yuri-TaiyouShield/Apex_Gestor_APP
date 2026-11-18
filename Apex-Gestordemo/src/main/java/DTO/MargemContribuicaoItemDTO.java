package DTO;

import java.math.BigDecimal;

public record MargemContribuicaoItemDTO(
        String tipoAgrupamento,
        Long referenciaId,
        String codigo,
        String nome,
        String categoriaNome,
        BigDecimal quantidadeVendidaLiquida,
        BigDecimal receitaBruta,
        BigDecimal deducoesTributosDescontos,
        BigDecimal receitaLiquida,
        BigDecimal cmvTotal,
        BigDecimal taxasCartaoAlocadas,
        BigDecimal comissoesFretesVariaveis,
        BigDecimal outrasDespesasVariaveis,
        BigDecimal custosDespesasVariaveisTotais,
        BigDecimal margemContribuicaoValor,
        BigDecimal margemContribuicaoPercentual,
        BigDecimal margemContribuicaoUnitariaMedia,
        BigDecimal participacaoFaturamentoPercentual,
        BigDecimal participacaoMargemTotalPercentual,
        String classificacaoRentabilidade,
        String recomendacaoItem) {
}
