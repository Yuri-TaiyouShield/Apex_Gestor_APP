package DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record FinancialCalculationResponseDTO(
        Long calculationId,
        String tipo,
        LocalDateTime calculadoEm,
        Map<String, BigDecimal> resultados,
        List<String> alertas
) {
}
