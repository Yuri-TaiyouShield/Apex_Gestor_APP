package DTO;

import Model.GrupoDreGerencial;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DreLancamentoOrigemDTO(
        String origemTipo,
        Long registroId,
        String documentoReferencia,
        String descricao,
        LocalDate dataCompetencia,
        LocalDate dataEmissao,
        LocalDate dataLiquidacaoCaixa,
        String statusOperacional,
        BigDecimal valorCompetencia,
        String impactoDre,
        int numeroLinhaDre,
        String codigoLinhaDre,
        String tituloLinhaDre,
        Long planoContaId,
        String planoContaCodigo,
        String planoContaNome,
        GrupoDreGerencial grupoDre,
        Long centroCustoId,
        String centroCustoCodigo,
        String centroCustoNome,
        Long filialId,
        String rotaDetalheFrontend) {
}
