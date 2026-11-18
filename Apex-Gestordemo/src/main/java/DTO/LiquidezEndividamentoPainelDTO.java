package DTO;

import Model.StatusConfiabilidadeIndicador;
import java.math.BigDecimal;
import java.util.List;

public record LiquidezEndividamentoPainelDTO(
        boolean balancoPatrimonialConfirmado,
        String competenciaBalancoUtilizado,
        BigDecimal disponibilidadesImediatas,
        BigDecimal ativoCirculante,
        BigDecimal estoques,
        BigDecimal despesasAntecipadas,
        BigDecimal realizavelLongoPrazo,
        BigDecimal ativoNaoCirculanteTotal,
        BigDecimal ativoTotal,
        BigDecimal passivoCirculante,
        BigDecimal passivoNaoCirculante,
        BigDecimal passivoExigivelTotal,
        BigDecimal dividaFinanceiraTotal,
        BigDecimal patrimonioLiquido,
        BigDecimal liquidezImediata,
        BigDecimal liquidezSeca,
        BigDecimal liquidezCorrente,
        BigDecimal liquidezGeral,
        BigDecimal endividamentoGeralPercentual,
        BigDecimal participacaoCapitalTerceirosPercentual,
        BigDecimal composicaoEndividamentoCurtoPrazoPercentual,
        BigDecimal relacaoDividaPatrimonioLiquidoPercentual,
        BigDecimal coberturaObrigacoesCurtoPrazo,
        StatusConfiabilidadeIndicador statusConfiabilidadeLiquidezImediata,
        StatusConfiabilidadeIndicador statusConfiabilidadePatrimonial,
        List<String> dependenciasPatrimoniaisFaltantes,
        String interpretacaoLiquidezEndividamento) {
}
