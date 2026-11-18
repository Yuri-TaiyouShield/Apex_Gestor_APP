package DTO;

import Model.OrigemClassificacaoFinanceira;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReclassificacaoManualRequestDTO(
        @NotNull OrigemClassificacaoFinanceira origemTipo,
        @NotNull Long registroId,
        @NotNull Long planoContaId,
        Long centroCustoId,
        @Size(max = 255) String motivo) {
}
