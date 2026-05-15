package DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record B2cCartMergeItemDTO(
        @NotNull Long produtoId,
        @Min(1) int quantidade
) {
}
