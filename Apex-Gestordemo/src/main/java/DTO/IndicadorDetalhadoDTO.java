package DTO;

import Model.StatusConfiabilidadeIndicador;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record IndicadorDetalhadoDTO(
        String codigo,
        String nome,
        String categoriaIndicador,
        String prioridade,
        String descricao,
        String formula,
        Map<String, BigDecimal> variaveis,
        BigDecimal resultado,
        String unidadeMedida,
        String periodo,
        String fonteDados,
        LocalDateTime dataAtualizacao,
        String interpretacaoGerencial,
        String limitacoes,
        StatusConfiabilidadeIndicador statusConfiabilidade,
        List<String> dependenciasFaltantes) {
}
