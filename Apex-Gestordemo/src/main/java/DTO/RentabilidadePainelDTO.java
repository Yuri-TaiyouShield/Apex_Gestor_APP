package DTO;

import java.math.BigDecimal;
import java.util.List;

public record RentabilidadePainelDTO(
        BigDecimal receitaBrutaTotal,
        BigDecimal deducoesTributosVendasTotal,
        BigDecimal receitaLiquidaTotal,
        BigDecimal cmvTotal,
        BigDecimal despesasVariaveisDre,
        BigDecimal taxasCartaoAntecipacaoPeriodo,
        BigDecimal custosDespesasVariaveisTotais,
        BigDecimal margemContribuicaoTotalValor,
        BigDecimal margemContribuicaoTotalPercentual,
        BigDecimal custosDespesasFixasTotais,
        BigDecimal resultadoOperacionalDre,
        BigDecimal resultadoLiquidoDre,
        BigDecimal margemLiquidaPercentual,
        List<MargemContribuicaoItemDTO> porProduto,
        List<MargemContribuicaoItemDTO> porCategoria,
        List<MargemContribuicaoItemDTO> porFilial,
        List<MargemContribuicaoItemDTO> alertasAltoVolumeBaixaMargem) {
}
