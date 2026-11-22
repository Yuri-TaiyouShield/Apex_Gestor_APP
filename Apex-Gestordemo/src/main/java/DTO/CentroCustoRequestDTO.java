package DTO;

import Model.TipoCentroCusto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CentroCustoRequestDTO(
        @NotBlank @Size(max = 32) String codigo,
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 255) String descricao,
        @NotNull TipoCentroCusto tipoOperacional,
        Long responsavelUsuarioId,
        Boolean ativo) {
}
