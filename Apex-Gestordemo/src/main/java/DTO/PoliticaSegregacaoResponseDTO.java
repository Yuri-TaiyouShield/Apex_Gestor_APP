package DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PoliticaSegregacaoResponseDTO(
        Long idPoliticaSod,
        Long empresaId,
        Long filialId,
        String dominioOperacao,
        boolean exigirAprovadorDistinto,
        boolean permitirExcecaoUnipessoal,
        boolean exigirJustificativa,
        BigDecimal alcadaGerenteMaximo,
        String atualizadoPor,
        LocalDateTime atualizadoEm,
        Long version
) {
}
