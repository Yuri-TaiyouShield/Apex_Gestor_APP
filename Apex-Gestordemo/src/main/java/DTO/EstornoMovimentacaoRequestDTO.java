package DTO;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record EstornoMovimentacaoRequestDTO(
        @NotBlank String motivo,
        LocalDate dataEstorno) {
}
