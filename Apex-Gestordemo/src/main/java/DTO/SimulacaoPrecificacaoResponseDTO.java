package DTO;

import java.math.BigDecimal;
import java.util.List;

public record SimulacaoPrecificacaoResponseDTO(
        Long produtoId,
        String produtoDescricao,
        BigDecimal custoAquisicaoBase,
        BigDecimal custoAquisicaoEfetivoTotal,
        BigDecimal percentualDespesasVariaveisTotal,
        BigDecimal margemLucroDesejadaPercentual,
        BigDecimal markupDivisor,
        BigDecimal markupMultiplicador,
        BigDecimal precoMinimoEquilibrio,
        BigDecimal precoRecomendado,
        BigDecimal precoPraticadoAtual,
        BigDecimal margemRealPrecoAtualValor,
        BigDecimal margemRealPrecoAtualPercentual,
        BigDecimal markupRealPrecoAtual,
        BigDecimal precoPromocionalSimulado,
        BigDecimal margemPromocionalValor,
        BigDecimal margemPromocionalPercentual,
        BigDecimal aumentoVolumeNecessarioParaMesmoLucroPercentual,
        BigDecimal custoComReajusteSimulado,
        BigDecimal precoRecomendadoAposReajusteCusto,
        BigDecimal margemSeMantiverPrecoAtualAposReajustePercentual,
        String situacaoPrecoAtual,
        String diferencaMarkupVsMargemExplicacao,
        List<String> recomendacoesPrecificacao,
        boolean simulacaoReadOnly) {
}
