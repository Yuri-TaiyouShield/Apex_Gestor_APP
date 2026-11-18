package DTO;

import java.math.BigDecimal;

public record PrazosCiclosEvolucaoPontoDTO(
        String competencia,
        BigDecimal pmeDias,
        BigDecimal pmrDias,
        BigDecimal pmpDias,
        BigDecimal cicloOperacionalDias,
        BigDecimal cicloFinanceiroDias) {
}
