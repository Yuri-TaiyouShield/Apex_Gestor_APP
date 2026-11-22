package DTO;

import Model.ClassificacaoCusto;
import Model.GrupoDreGerencial;
import Model.NaturezaContaGerencial;
import Model.TipoContaGerencial;
import java.math.BigDecimal;

public record ResumoCompetenciaItemDTO(
        Long planoContaId,
        String planoContaCodigo,
        String planoContaNome,
        TipoContaGerencial tipoConta,
        NaturezaContaGerencial natureza,
        ClassificacaoCusto classificacaoCusto,
        GrupoDreGerencial grupoDre,
        Long centroCustoId,
        String centroCustoCodigo,
        String centroCustoNome,
        BigDecimal valorCompetencia,
        BigDecimal valorRealizadoCaixa,
        long quantidadeLancamentos) {
}
