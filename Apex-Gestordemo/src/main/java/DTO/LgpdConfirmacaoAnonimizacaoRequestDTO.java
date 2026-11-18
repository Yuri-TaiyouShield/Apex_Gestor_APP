package DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LgpdConfirmacaoAnonimizacaoRequestDTO(
        @NotNull Boolean confirmadoExplicito,
        @NotBlank @Size(min = 10, max = 500) String justificativa,
        String simulacaoHash,
        Long solicitacaoId
) {
}
