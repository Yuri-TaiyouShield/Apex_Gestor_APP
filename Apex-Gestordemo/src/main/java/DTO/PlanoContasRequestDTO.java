package DTO;

import Model.ClassificacaoCusto;
import Model.GrupoDreGerencial;
import Model.NaturezaContaGerencial;
import Model.TipoContaGerencial;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlanoContasRequestDTO(
        @NotBlank @Size(max = 32) String codigo,
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 255) String descricao,
        Long contaPaiId,
        @NotNull TipoContaGerencial tipoConta,
        @NotNull NaturezaContaGerencial natureza,
        @NotNull ClassificacaoCusto classificacaoCusto,
        @NotNull GrupoDreGerencial grupoDre,
        Integer ordemExibicao,
        Boolean aceitaLancamento,
        Boolean ativo) {
}
