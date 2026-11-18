package DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
        @Size(max = 120) List<@Valid TaxBracketDTO> tabelaInss,
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
