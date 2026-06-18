package DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record RecebimentoVendaRequestDTO(
        List<@Valid PagamentoDTO> pagamentos,
        Long formaPagamentoId,
        @DecimalMin("0.01") BigDecimal valorRecebido,
        @Min(1) Integer numeroParcelas) {

    public RecebimentoVendaRequestDTO(Long formaPagamentoId, BigDecimal valorRecebido, Integer numeroParcelas) {
        this(List.of(), formaPagamentoId, valorRecebido, numeroParcelas);
    }

    public record PagamentoDTO(
            @NotNull Long formaPagamentoId,
            @NotNull @DecimalMin("0.01") BigDecimal valor,
            @Min(1) Integer numeroParcelas,
            Long creditoId) {
    }
}
