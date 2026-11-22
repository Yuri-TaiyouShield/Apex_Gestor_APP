package DTO;

import Model.GrupoDreGerencial;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DreDrillDownResponseDTO(
        String rotuloPeriodo,
        LocalDate dataInicio,
        LocalDate dataFim,
        Integer numeroLinhaFiltro,
        String codigoLinhaFiltro,
        String tituloLinhaFiltro,
        Long planoContaIdFiltro,
        String planoContaNomeFiltro,
        GrupoDreGerencial grupoDreFiltro,
        Long centroCustoIdFiltro,
        Long filialIdFiltro,
        boolean consolidadoEmpresa,
        long totalLancamentos,
        BigDecimal somaValorCompetencia,
        List<DreLancamentoOrigemDTO> lancamentos) {
}
