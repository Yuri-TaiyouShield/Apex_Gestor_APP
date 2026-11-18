package DTO;

import Model.TipoCentroCusto;
import java.time.LocalDateTime;

public record CentroCustoResponseDTO(
        Long idCentroCusto,
        String codigo,
        String nome,
        String descricao,
        TipoCentroCusto tipoOperacional,
        Long responsavelUsuarioId,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
