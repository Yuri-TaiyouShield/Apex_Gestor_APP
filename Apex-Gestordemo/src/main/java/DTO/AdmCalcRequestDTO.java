package DTO;

import java.math.BigDecimal;
import java.util.List;

public record AdmCalcRequestDTO(
        BigDecimal precoVenda,
        BigDecimal quantidade,
        BigDecimal receitaTotal,
        BigDecimal cmv,
        BigDecimal despesas,
        BigDecimal impostos,
        BigDecimal custosFixos,
        BigDecimal custosVariaveis,
        BigDecimal margemContribuicao,
        BigDecimal ativoCirculante,
        BigDecimal ativoNaoCirculante,
        BigDecimal passivoCirculante,
        BigDecimal passivoNaoCirculante,
        BigDecimal estoque,
        BigDecimal custoBem,
        BigDecimal valorResidual,
        BigDecimal vidaUtil,
        BigDecimal custoTotal,
        BigDecimal quantidadeProduzida,
        BigDecimal margemDesejada,
        BigDecimal ganhoInvestimento,
        BigDecimal custoInvestimento,
        BigDecimal valorPresente,
        BigDecimal valorFuturo,
        BigDecimal taxaDesconto,
        BigDecimal periodo,
        List<BigDecimal> fluxosDeCaixa
) {
}
