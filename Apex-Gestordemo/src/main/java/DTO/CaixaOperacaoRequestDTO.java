package DTO;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record CaixaOperacaoRequestDTO(
        @DecimalMin(value = "0.00") BigDecimal valor,
        String observacao,
        Long caixaSessaoId) {
}
