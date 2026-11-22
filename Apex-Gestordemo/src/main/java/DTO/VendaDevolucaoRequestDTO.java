package DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record VendaDevolucaoRequestDTO(
        @NotNull String tipo,
        @NotBlank @Size(max = 255) String motivo,
        @NotEmpty List<@Valid ItemDTO> itens) {
    public record ItemDTO(
            @NotNull Long produtoOriginalId,
            Long produtoNovoId,
            @NotNull @Min(1) Integer quantidade) {
    }
}
