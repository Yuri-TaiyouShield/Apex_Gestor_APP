package DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MenuRequestDTO(@NotBlank @Size(max = 45) String nome,
        @NotBlank @Size(max = 100) String link, @Size(max = 45) String icone, Integer exibir) {
}
