package DTO;

import java.math.BigDecimal;
import java.util.List;

public record DreEvolucaoResponseDTO(
        String competenciaInicio,
        String competenciaFim,
        int quantidadeMeses,
        Long empresaId,
        Long filialId,
        boolean consolidadoEmpresa,
        Long centroCustoId,
        BigDecimal receitaLiquidaAcumulada,
        BigDecimal lucroBrutoAcumulado,
        BigDecimal resultadoLiquidoAcumulado,
        BigDecimal margemLiquidaMediaPercentual,
        String tendenciaReceita,
        String tendenciaResultado,
        List<DreEvolucaoPontoDTO> serie) {
}
