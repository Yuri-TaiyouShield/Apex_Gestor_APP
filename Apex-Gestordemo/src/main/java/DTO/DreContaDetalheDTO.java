package DTO;

import Model.ClassificacaoCusto;
import Model.GrupoDreGerencial;
import Model.NaturezaContaGerencial;
import Model.TipoContaGerencial;
import java.math.BigDecimal;
import java.util.List;

public record DreContaDetalheDTO(
        Long idPlanoConta,
        Long contaPaiId,
        String codigo,
        String nome,
        TipoContaGerencial tipoConta,
        NaturezaContaGerencial natureza,
        ClassificacaoCusto classificacaoCusto,
        GrupoDreGerencial grupoDre,
        boolean sintetica,
        BigDecimal valor,
        BigDecimal analiseVerticalPercentual,
        BigDecimal valorComparacao,
        BigDecimal variacaoAbsoluta,
        BigDecimal analiseHorizontalPercentual,
        long quantidadeLancamentos,
        List<DreContaDetalheDTO> filhas) {
}
