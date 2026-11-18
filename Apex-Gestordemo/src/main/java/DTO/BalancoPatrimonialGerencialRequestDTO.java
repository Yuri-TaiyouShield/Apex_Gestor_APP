package DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BalancoPatrimonialGerencialRequestDTO(
        @NotBlank String competenciaAnoMes,
        LocalDate dataReferencia,
        @DecimalMin("0.00") BigDecimal disponibilidadesCaixaBancos,
        @DecimalMin("0.00") BigDecimal contasReceberOperacionais,
        @DecimalMin("0.00") BigDecimal estoquesOperacionais,
        @DecimalMin("0.00") BigDecimal adiantamentosOperacionais,
        @DecimalMin("0.00") BigDecimal despesasAntecipadas,
        @DecimalMin("0.00") BigDecimal outrosAtivosCirculantes,
        @DecimalMin("0.00") BigDecimal realizavelLongoPrazo,
        @DecimalMin("0.00") BigDecimal investimentosLongoPrazo,
        @DecimalMin("0.00") BigDecimal imobilizadoBruto,
        @DecimalMin("0.00") BigDecimal depreciacaoAcumulada,
        @DecimalMin("0.00") BigDecimal intangivel,
        @DecimalMin("0.00") BigDecimal fornecedoresOperacionaisCp,
        @DecimalMin("0.00") BigDecimal obrigacoesTributariasTrabalhistasCp,
        @DecimalMin("0.00") BigDecimal outrasObrigacoesOperacionaisCp,
        @DecimalMin("0.00") BigDecimal emprestimosFinanciamentosCp,
        @DecimalMin("0.00") BigDecimal emprestimosFinanciamentosLp,
        @DecimalMin("0.00") BigDecimal outrasObrigacoesLp,
        @DecimalMin("0.00") BigDecimal capitalSocial,
        BigDecimal reservasCapitalLucros,
        BigDecimal lucrosPrejuizosAcumulados,
        @DecimalMin("0.00") BigDecimal depreciacaoAmortizacaoPeriodo,
        @DecimalMin("0.00") BigDecimal amortizacaoPrincipalDividaPeriodo,
        Boolean usarSaldosOperacionaisErp,
        Boolean confirmadoGestor,
        String observacoes) {
}
