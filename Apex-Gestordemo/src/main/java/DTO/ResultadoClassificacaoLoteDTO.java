package DTO;

import java.util.List;

public record ResultadoClassificacaoLoteDTO(
        int processados,
        int classificados,
        int naoClassificados,
        List<String> observacoes) {
}
