package DTO;

import java.math.BigDecimal;
import java.util.List;

public record DiagnosticoLiquidezDTO(
        String nivelSaudeLiquidez,
        String mancheteExecutiva,
        BigDecimal disponibilidadeImediataTotal,
        BigDecimal entradasPrevistas30Dias,
        BigDecimal obrigacoesPrevistas30Dias,
        BigDecimal obrigacoesVencidasTotais,
        BigDecimal recebiveisInadimplentesTotais,
        BigDecimal taxaInadimplenciaPercentual,
        BigDecimal comprometimentoFornecedoresTotal,
        BigDecimal repassesCartaoPendentesBruto,
        BigDecimal taxasCartaoPrevistas,
        BigDecimal repassesCartaoPendentesLiquido,
        BigDecimal dependenciaAntecipacaoPercentual,
        BigDecimal geracaoCaixaOperacionalPeriodo,
        BigDecimal resultadoEconomicoDrePeriodo,
        BigDecimal diferencaDreVsCaixaOperacional,
        String explicacaoDreVsCaixa,
        boolean riscoInsuficienciaCaixa,
        boolean capacidadeSustentarOperacao,
        boolean disponibilidadeParaInvestimento,
        BigDecimal margemSegurancaInvestimento,
        List<String> contasComprometendoCaixa,
        List<String> alertasRiscoLiquidez,
        List<String> recomendacoesGerenciais) {
}
