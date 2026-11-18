package DTO;

import java.math.BigDecimal;
import java.util.List;

public record DreFilialColunaDTO(
        Long filialId,
        String filialCodigo,
        String filialNome,
        BigDecimal receitaBruta,
        BigDecimal receitaLiquida,
        BigDecimal cmv,
        BigDecimal lucroBruto,
        BigDecimal despesasTotais,
        BigDecimal resultadoOperacional,
        BigDecimal resultadoLiquidoGerencial,
        BigDecimal margemBrutaPercentual,
        BigDecimal margemLiquidaPercentual,
        BigDecimal participacaoReceitaLiquidaPercentual,
        BigDecimal participacaoResultadoPercentual,
        List<DreLinhaDTO> linhas) {
}
