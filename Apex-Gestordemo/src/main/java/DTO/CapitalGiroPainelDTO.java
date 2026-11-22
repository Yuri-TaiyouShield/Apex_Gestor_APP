package DTO;

import Model.StatusConfiabilidadeIndicador;
import java.math.BigDecimal;
import java.util.List;

public record CapitalGiroPainelDTO(
        BigDecimal disponibilidadesCaixaBancos,
        BigDecimal contasReceberOperacionais,
        BigDecimal estoquesOperacionais,
        BigDecimal adiantamentosDespesasAntecipadas,
        BigDecimal ativoCirculanteOperacional,
        BigDecimal ativoCirculanteTotal,
        BigDecimal fornecedoresContasPagarOperacionais,
        BigDecimal obrigacoesTributariasTrabalhistasOperacionais,
        BigDecimal passivoCirculanteOperacional,
        BigDecimal emprestimosFinanciamentosCurtoPrazo,
        BigDecimal passivoCirculanteTotal,
        BigDecimal capitalGiroLiquidoCgl,
        BigDecimal necessidadeCapitalGiroNcg,
        BigDecimal saldoTesourariaSt,
        BigDecimal ncgPeriodoAnterior,
        BigDecimal variacaoNcgValor,
        BigDecimal variacaoNcgPercentual,
        BigDecimal percentualCapitalPresoEstoque,
        BigDecimal percentualCapitalPresoClientes,
        BigDecimal percentualFinanciadoFornecedores,
        boolean riscoEfeitoTesoura,
        String perfilEstruturaFinanceira,
        StatusConfiabilidadeIndicador statusConfiabilidadeCgl,
        StatusConfiabilidadeIndicador statusConfiabilidadeNcg,
        String interpretacaoCapitalGiro,
        List<String> fatoresPressaoCapitalGiro) {
}
