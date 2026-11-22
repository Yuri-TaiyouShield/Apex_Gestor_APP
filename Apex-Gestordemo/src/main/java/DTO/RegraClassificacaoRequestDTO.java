package DTO;

import Model.OrigemClassificacaoFinanceira;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegraClassificacaoRequestDTO(
        @NotBlank @Size(max = 120) String nome,
        @NotNull OrigemClassificacaoFinanceira origemTipo,
        Long tipoDespesaId,
        Long categoriaId,
        Long formaPagamentoId,
        Long fornecedorId,
        @Size(max = 120) String palavraChave,
        @NotNull Long planoContaId,
        Long centroCustoId,
        Integer prioridade,
        Boolean ativo) {
}
