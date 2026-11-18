package DTO;

import java.util.List;

public record DreDiagnosticoGerencialDTO(
        String situacaoPeriodo,
        String mancheteExplicativa,
        String explicacaoResultado,
        String explicacaoCompetenciaVsCaixa,
        String explicacaoPontoEquilibrio,
        List<DreFatorRelevanteDTO> principaisFatores,
        List<String> recomendacoesGerenciais,
        List<String> avisosLimitacao) {
}
