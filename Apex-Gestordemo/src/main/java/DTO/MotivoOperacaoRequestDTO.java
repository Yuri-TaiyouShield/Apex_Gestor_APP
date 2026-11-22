package DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MotivoOperacaoRequestDTO(
        @NotBlank @Size(max = 255) String motivo) {
}
