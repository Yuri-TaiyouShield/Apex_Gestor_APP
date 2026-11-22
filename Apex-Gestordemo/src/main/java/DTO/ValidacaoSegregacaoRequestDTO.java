package DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ValidacaoSegregacaoRequestDTO(
        @NotBlank @Size(max = 48) String dominioOperacao,
        @NotNull Long submetidoPorUsuarioId,
        @DecimalMin("0.00") BigDecimal valorOperacao,
        @Size(max = 500) String justificativa
) {
}
