package DTO;

import Model.StatusConfiabilidadeIndicador;
import java.math.BigDecimal;
import java.util.List;

public record InvestimentosRetornoPainelDTO(
        BigDecimal resultadoOperacionalPeriodo,
        BigDecimal resultadoLiquidoPeriodo,
        BigDecimal ativoTotalMedio,
        BigDecimal patrimonioLiquidoMedio,
        BigDecimal capitalInvestidoOperacional,
        BigDecimal roaRetornoSobreAtivoPercentual,
        BigDecimal roeRetornoSobrePatrimonioPercentual,
        BigDecimal roicRetornoSobreCapitalInvestidoPercentual,
        BigDecimal roiSimuladoPercentual,
        BigDecimal paybackSimplesPeriodos,
        BigDecimal paybackDescontadoPeriodos,
        BigDecimal vplValorPresenteLiquido,
        BigDecimal tirTaxaInternaRetornoPercentual,
        boolean investimentoViavelEconomicamente,
        boolean capacidadeFinanceiraCaixaParaInvestir,
        BigDecimal disponibilidadeCaixaAposReserva,
        StatusConfiabilidadeIndicador statusConfiabilidadeRoaRoe,
        StatusConfiabilidadeIndicador statusConfiabilidadeSimulacao,
        String separacaoContabilVsCaixaExplicacao,
        List<String> alertasInvestimento) {
}
