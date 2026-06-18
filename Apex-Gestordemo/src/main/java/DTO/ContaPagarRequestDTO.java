package DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContaPagarRequestDTO(Long fornecedorId, Long categoriaId,
        @NotBlank String descricao, @NotNull @DecimalMin("0.01") BigDecimal valor,
        @NotNull LocalDate vencimento, Long formaPagamentoPrevistaId) {
}
