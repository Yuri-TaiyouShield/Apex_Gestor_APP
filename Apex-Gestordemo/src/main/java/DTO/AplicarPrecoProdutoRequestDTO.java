package DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AplicarPrecoProdutoRequestDTO(
        @NotNull Long produtoId,
        @NotNull @DecimalMin("0.01") BigDecimal novoValorVenda,
        @DecimalMin("0.00") BigDecimal novaMargemPercentual,
        @NotBlank String justificativa) {
}
