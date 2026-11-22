package DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DespesaRequestDTO(
        @NotBlank @Size(max = 255) String descricao,
        @NotNull @DecimalMin("0.01") BigDecimal valor,
        LocalDate dataVencimento,
        LocalDate dataCompetencia,
        LocalDate dataEmissao,
        Long tipoDespesaId,
        TipoDespesaRefDTO tipoDespesa,
        Long planoContaId,
        Long centroCustoId) {

    public record TipoDespesaRefDTO(Long idTipoDespesa) {
    }
}
