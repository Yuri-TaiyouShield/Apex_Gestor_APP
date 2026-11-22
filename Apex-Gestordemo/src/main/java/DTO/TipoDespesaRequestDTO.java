package DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoDespesaRequestDTO(
        @NotBlank @Size(max = 50) String nome,
        Integer status,
        @Size(max = 24) String naturezaDespesa,
        Long planoContaId,
        Long centroCustoId) {
}
