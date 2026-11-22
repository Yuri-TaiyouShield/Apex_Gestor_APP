package DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CondicaoPagamentoRequestDTO(
        @NotBlank String nome,
        @NotBlank String tipoPagamento,
        @NotNull @Min(1) Integer quantidadeParcelas,
        @NotNull @Min(0) Integer prazoDias,
        @NotNull @DecimalMin(value = "0.0000") BigDecimal percentual,
        String restricoes,
        Integer status,
        Long empresaId,
        Long filialId) {
}
