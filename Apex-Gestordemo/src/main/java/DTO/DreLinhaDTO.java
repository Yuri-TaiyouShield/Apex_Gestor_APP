package DTO;

import Model.GrupoDreGerencial;
import java.math.BigDecimal;
import java.util.List;

public record DreLinhaDTO(
        int numeroLinha,
        String codigoLinha,
        String titulo,
        String tipoLinha,
        String sinalDemonstracao,
        List<GrupoDreGerencial> gruposAssociados,
        BigDecimal valor,
        BigDecimal analiseVerticalPercentual,
        BigDecimal valorComparacao,
        BigDecimal variacaoAbsoluta,
        BigDecimal analiseHorizontalPercentual,
        String statusVariacao,
        long quantidadeLancamentos,
        String explicacaoGerencial,
        List<DreContaDetalheDTO> contas) {
}
