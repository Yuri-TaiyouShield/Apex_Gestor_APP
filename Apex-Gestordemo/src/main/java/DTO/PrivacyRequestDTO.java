package DTO;

import jakarta.validation.constraints.NotBlank;

public record PrivacyRequestDTO(
        @NotBlank String titularId,
        @NotBlank String tipo
) {
}
