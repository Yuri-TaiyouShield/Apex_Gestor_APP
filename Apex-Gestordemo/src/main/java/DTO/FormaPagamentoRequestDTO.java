package DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FormaPagamentoRequestDTO(
        @NotBlank @Size(max = 50) String nome,
        @NotBlank String descricao,
        @NotNull Integer tipoPagamento,
        Integer status) {
}
