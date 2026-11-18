package DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FluxoCaixaCenarioProjecaoDTO(
        String codigoCenario,
        String nomeCenario,
        String premissaExplicacao,
        BigDecimal percentualRealizacaoRecebiveis,
        BigDecimal percentualVariacaoSaidas,
        BigDecimal saldoAtualPartida,
        BigDecimal saldoProjetado7Dias,
        BigDecimal saldoProjetado15Dias,
        BigDecimal saldoProjetado30Dias,
        BigDecimal saldoProjetado60Dias,
        BigDecimal saldoProjetado90Dias,
        BigDecimal menorSaldoProjetado,
        LocalDate dataMenorSaldo,
        boolean riscoInsuficienciaCaixa,
        BigDecimal necessidadeCapitalGiroAdicional,
        List<FluxoCaixaDiaCalendarioDTO> serieHorizonte) {
}
