package DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record B2cCartMergeRequestDTO(
        @NotNull Long clienteId,
        @Valid @NotEmpty List<B2cCartMergeItemDTO> itens
) {
}
