package DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DreGerencialResponseDTO(
        String tipoPeriodo,
        String rotuloPeriodo,
        LocalDate dataInicio,
        LocalDate dataFim,
        boolean comparacaoAtiva,
        String rotuloPeriodoComparacao,
        LocalDate comparacaoInicio,
        LocalDate comparacaoFim,
        Long empresaId,
        Long filialId,
        String filialNome,
        boolean consolidadoEmpresa,
        Long centroCustoId,
        String centroCustoNome,
        String baseAnaliseVertical,
        long lancamentosSemClassificacao,
        BigDecimal valorSemClassificacao,
        DreIndicadoresResumoDTO indicadores,
        DreDiagnosticoGerencialDTO diagnostico,
        List<DreLinhaDTO> linhas) {
}
