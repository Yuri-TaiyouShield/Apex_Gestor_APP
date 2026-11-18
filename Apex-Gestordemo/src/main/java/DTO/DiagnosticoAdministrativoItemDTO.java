package DTO;

import Model.StatusConfiabilidadeIndicador;

public record DiagnosticoAdministrativoItemDTO(
        String codigoDiagnostico,
        String severidade,
        String indicadorRelacionado,
        String resultadoFormatado,
        String periodo,
        String fonteDados,
        String interpretacao,
        String possivelConsequencia,
        String recomendacaoGerencial,
        StatusConfiabilidadeIndicador grauConfiabilidade) {
}
