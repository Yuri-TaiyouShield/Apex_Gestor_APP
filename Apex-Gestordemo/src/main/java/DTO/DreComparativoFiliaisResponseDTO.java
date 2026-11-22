package DTO;

import java.time.LocalDate;
import java.util.List;

public record DreComparativoFiliaisResponseDTO(
        String tipoPeriodo,
        String rotuloPeriodo,
        LocalDate dataInicio,
        LocalDate dataFim,
        Long empresaId,
        Long centroCustoId,
        DreIndicadoresResumoDTO consolidadoEmpresa,
        List<DreFilialColunaDTO> filiais,
        List<String> destaquesComparativos) {
}
