package DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record SimulacaoInvestimentoRequestDTO(
        String nomeProjeto,
        @DecimalMin("0.01") BigDecimal investimentoInicial,
        @DecimalMin("0.00") BigDecimal ganhoEsperadoTotal,
        BigDecimal taxaDescontoPeriodicaPercentual,
        @Size(max = 240) List<BigDecimal> fluxosCaixaProjetados) {
}
