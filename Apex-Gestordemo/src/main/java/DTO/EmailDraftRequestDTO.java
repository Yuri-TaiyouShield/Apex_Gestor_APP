package DTO;

import jakarta.validation.constraints.NotBlank;

public record EmailDraftRequestDTO(
        @NotBlank String assunto,
        @NotBlank String mensagem
) {
}
