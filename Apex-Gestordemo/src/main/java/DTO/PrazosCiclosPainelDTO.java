package DTO;

import Model.StatusConfiabilidadeIndicador;
import java.math.BigDecimal;
import java.util.List;

public record PrazosCiclosPainelDTO(
        int diasPeriodoAnalisado,
        BigDecimal estoqueInicialPeriodo,
        BigDecimal estoqueFinalPeriodo,
        BigDecimal estoqueMedioPeriodo,
        BigDecimal cmvPeriodo,
        BigDecimal pmePrazoMedioEstocagemDias,
        BigDecimal giroEstoqueVezesPeriodo,
        BigDecimal contasReceberInicialPeriodo,
        BigDecimal contasReceberFinalPeriodo,
        BigDecimal contasReceberMediaPeriodo,
        BigDecimal vendasPrazoPeriodo,
        BigDecimal pmrPrazoMedioRecebimentoDias,
        BigDecimal fornecedoresInicialPeriodo,
        BigDecimal fornecedoresFinalPeriodo,
        BigDecimal fornecedoresMediaPeriodo,
        BigDecimal comprasPrazoPeriodo,
        BigDecimal pmpPrazoMedioPagamentoDias,
        BigDecimal cicloOperacionalDias,
        BigDecimal cicloFinanceiroDias,
        boolean pagaFornecedorAntesDeReceber,
        BigDecimal descompassoCaixaDias,
        StatusConfiabilidadeIndicador statusConfiabilidadePme,
        StatusConfiabilidadeIndicador statusConfiabilidadePmr,
        StatusConfiabilidadeIndicador statusConfiabilidadePmp,
        StatusConfiabilidadeIndicador statusConfiabilidadeCicloFinanceiro,
        String tendenciaCicloFinanceiro,
        String interpretacaoPrazosCiclos,
        List<PrazosCiclosEvolucaoPontoDTO> evolucaoMensal) {
}
