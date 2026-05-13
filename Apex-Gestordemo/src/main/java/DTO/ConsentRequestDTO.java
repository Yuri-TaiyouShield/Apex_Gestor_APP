package DTO;

import jakarta.validation.constraints.NotBlank;

public record ConsentRequestDTO(
        String titularId,
        @NotBlank String tipoTitular,
        String documento,
        @NotBlank String versao,
        @NotBlank String canal
) {
}
