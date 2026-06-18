package DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record VendaRequestDTO(
        @NotNull Long clienteId,
        Long usuarioId,
        @DecimalMin(value = "0.00") BigDecimal desconto,
        String observacao,
        Long caixaSessaoId,
        Long condicaoPagamentoId,
        Long empresaId,
        Long filialId,
        @NotEmpty List<@Valid ItemDTO> itens,
        @NotEmpty List<@Valid PagamentoDTO> pagamentos) {

    public record ItemDTO(
            @NotNull Long produtoId,
            @Min(1) Integer quantidade) {
    }

    public record PagamentoDTO(
            @NotNull Long formaPagamentoId,
            @DecimalMin(value = "0.01") BigDecimal valorPago,
            Integer numeroParcelas) {
    }
}
