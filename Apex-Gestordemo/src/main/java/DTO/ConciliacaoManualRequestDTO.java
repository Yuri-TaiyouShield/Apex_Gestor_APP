package DTO;

import Model.StatusConciliacaoFinanceira;
import jakarta.validation.constraints.NotNull;

public record ConciliacaoManualRequestDTO(
        @NotNull Long conciliacaoItemId,
        Long movimentacaoVinculadaId,
        String documentoOrigemTipo,
        Long documentoOrigemId,
        StatusConciliacaoFinanceira novoStatus,
        String observacao) {
}
