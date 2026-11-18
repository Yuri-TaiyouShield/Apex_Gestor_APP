package DTO;

import java.math.BigDecimal;

public record PontoEquilibrioFilialCategoriaDTO(
        String tipoEscopo,
        Long escopoId,
        String escopoNome,
        BigDecimal receitaLiquidaAtual,
        BigDecimal margemContribuicaoValor,
        BigDecimal indiceMargemContribuicaoPercentual,
        BigDecimal custosDespesasFixasAlocadas,
        BigDecimal pontoEquilibrioContabilReceita,
        BigDecimal pontoEquilibrioFinanceiroReceita,
        BigDecimal pontoEquilibrioEconomicoReceita,
        BigDecimal margemSegurancaPercentual,
        String situacaoEquilibrio) {
}
