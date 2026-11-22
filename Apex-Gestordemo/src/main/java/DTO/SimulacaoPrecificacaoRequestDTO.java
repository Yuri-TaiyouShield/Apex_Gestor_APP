package DTO;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record SimulacaoPrecificacaoRequestDTO(
        Long produtoId,
        @DecimalMin("0.00") BigDecimal custoAquisicaoBase,
        @DecimalMin("0.00") BigDecimal freteCompraValor,
        @DecimalMin("0.00") BigDecimal seguroCompraValor,
        @DecimalMin("0.00") BigDecimal impostosNaoRecuperaveisValor,
        @DecimalMin("0.00") BigDecimal despesasAcessoriasValor,
        @DecimalMin("0.00") BigDecimal aliquotaTributosVendaPercentual,
        @DecimalMin("0.00") BigDecimal taxaCartaoPercentual,
        @DecimalMin("0.00") BigDecimal comissaoVendaPercentual,
        @DecimalMin("0.00") BigDecimal freteVendaVariavelPercentual,
        @DecimalMin("0.00") BigDecimal outrasDespesasVariaveisPercentual,
        @DecimalMin("0.00") BigDecimal margemLucroDesejadaPercentual,
        @DecimalMin("0.00") BigDecimal precoPraticadoAtual,
        @DecimalMin("0.00") BigDecimal descontoPromocionalSimuladoPercentual,
        BigDecimal variacaoCustoSimuladaPercentual) {
}
