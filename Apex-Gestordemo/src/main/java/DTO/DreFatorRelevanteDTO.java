package DTO;

import java.math.BigDecimal;

public record DreFatorRelevanteDTO(
        String titulo,
        String categoria,
        BigDecimal valor,
        BigDecimal percentualReceitaLiquida,
        BigDecimal variacaoPercentual,
        String impacto,
        String explicacao) {
}
