package DTO;

import Model.StatusConfiabilidadeIndicador;
import java.math.BigDecimal;
import java.util.List;

public record PontoEquilibrioPainelDTO(
        BigDecimal receitaLiquidaAtual,
        BigDecimal margemContribuicaoTotal,
        BigDecimal indiceMargemContribuicaoDecimal,
        BigDecimal indiceMargemContribuicaoPercentual,
        BigDecimal quantidadeTotalVendidaMix,
        BigDecimal precoMedioPonderadoMix,
        BigDecimal margemContribuicaoUnitariaMediaMix,
        BigDecimal custosDespesasFixasContabeis,
        BigDecimal despesasNaoDesembolsaveisDepreciacao,
        BigDecimal amortizacaoPrincipalDividasPeriodo,
        BigDecimal desembolsosFixosFinanceiros,
        BigDecimal capitalInvestidoBase,
        BigDecimal taxaRetornoMinimoPeriodoPercentual,
        BigDecimal remuneracaoMinimaCapitalValor,
        BigDecimal pontoEquilibrioContabilReceita,
        BigDecimal pontoEquilibrioContabilUnidadesMix,
        BigDecimal pontoEquilibrioFinanceiroReceita,
        BigDecimal pontoEquilibrioFinanceiroUnidadesMix,
        BigDecimal pontoEquilibrioEconomicoReceita,
        BigDecimal pontoEquilibrioEconomicoUnidadesMix,
        BigDecimal margemSegurancaContabilValor,
        BigDecimal margemSegurancaContabilPercentual,
        BigDecimal grauAlavancagemOperacional,
        String metodologiaExplicita,
        StatusConfiabilidadeIndicador statusConfiabilidade,
        List<PontoEquilibrioFilialCategoriaDTO> porFilial,
        List<PontoEquilibrioFilialCategoriaDTO> porCategoria) {
}
