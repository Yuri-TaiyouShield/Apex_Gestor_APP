package DTO;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record LaborCalculationRequestDTO(
        @NotNull BigDecimal salarioMensal,
        @NotNull BigDecimal jornadaMensal,
        BigDecimal percentualHoraExtra,
        BigDecimal horasRelogioNoturnas,
        BigDecimal totalVerbasVariaveis,
        Integer diasUteisMes,
        Integer domingosFeriadosMes,
        BigDecimal salarioMinimo,
        BigDecimal percentualInsalubridade,
        BigDecimal salarioBase,
        BigDecimal custoRealPassagens,
        Integer quantidadeFilhosElegiveis,
        BigDecimal cotaSalarioFamilia,
        BigDecimal mediaAdicionais,
        BigDecimal mediaDsr,
        Integer mesesTrabalhados13,
        Integer diasFalta,
        Integer dsrSemanaFalta,
        BigDecimal salarioContribuicao,
        List<TaxBracketDTO> tabelaInss,
        Integer anosCompletosTrabalhados,
        BigDecimal saldoFgts,
        BigDecimal percentualMultaFgts,
        BigDecimal maiorRemuneracao,
        BigDecimal verbasIncontroversas,
        BigDecimal salariosAteTerminoContrato,
        Integer mesesRestantesEstabilidade,
        BigDecimal valorNominal,
        BigDecimal fatorIpcaEAcumulado,
        BigDecimal taxaSelicAcumulada
) {
}
