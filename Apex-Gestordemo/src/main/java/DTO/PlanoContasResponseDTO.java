package DTO;

import Model.ClassificacaoCusto;
import Model.GrupoDreGerencial;
import Model.NaturezaContaGerencial;
import Model.TipoContaGerencial;
import java.time.LocalDateTime;
import java.util.List;

public record PlanoContasResponseDTO(
        Long idPlanoConta,
        Long contaPaiId,
        String codigo,
        String nome,
        String descricao,
        TipoContaGerencial tipoConta,
        NaturezaContaGerencial natureza,
        ClassificacaoCusto classificacaoCusto,
        GrupoDreGerencial grupoDre,
        Integer ordemExibicao,
        boolean aceitaLancamento,
        boolean sistemaPadrao,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        List<PlanoContasResponseDTO> filhas) {
}
