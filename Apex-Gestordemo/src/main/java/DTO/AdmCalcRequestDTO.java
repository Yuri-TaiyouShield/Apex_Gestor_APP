package DTO;

import jakarta.validation.constraints.Size;
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
        @Size(max = 240) List<BigDecimal> fluxosDeCaixa,
        String escalaPercentual
) {
    /**
     * Sobrecarga de compatibilidade para chamadores legados de 27 argumentos (usa escala AUTO).
     * Prefira o construtor canonico informando explicitamente "PERCENTUAL" ou "DECIMAL".
     */
    public AdmCalcRequestDTO(
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
        this(precoVenda, quantidade, receitaTotal, cmv, despesas, impostos, custosFixos, custosVariaveis,
                margemContribuicao, ativoCirculante, ativoNaoCirculante, passivoCirculante, passivoNaoCirculante,
                estoque, custoBem, valorResidual, vidaUtil, custoTotal, quantidadeProduzida, margemDesejada,
                ganhoInvestimento, custoInvestimento, valorPresente, valorFuturo, taxaDesconto, periodo,
                fluxosDeCaixa, null);
    }
}
