package DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ConsolidacaoCompetenciaResponseDTO(
        String competenciaAnoMes,
        LocalDate inicioPeriodo,
        LocalDate fimPeriodo,
        BigDecimal receitasCompetencia,
        BigDecimal cmvCompetencia,
        BigDecimal despesasCompetencia,
        BigDecimal resultadoCompetencia,
        BigDecimal entradasRealizadasCaixa,
        BigDecimal saidasRealizadasCaixa,
        BigDecimal saldoRealizadoCaixa,
        long lancamentosSemPlanoConta,
        BigDecimal valorSemPlanoConta,
        List<String> avisosLimitacao,
        List<ResumoCompetenciaItemDTO> linhas) {
}
