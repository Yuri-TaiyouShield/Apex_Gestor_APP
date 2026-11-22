package DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record NotaFiscalEntradaRequestDTO(@NotNull String numero, String serie,
        @NotNull Long fornecedorId, @NotNull LocalDateTime dataEmissao,
        @NotEmpty List<@Valid ItemDTO> itens) {
    public record ItemDTO(@NotNull Long produtoId, @Min(1) int quantidade,
            @NotNull @DecimalMin("0.01") BigDecimal valorCustoUnitario) {
    }
}
