package DTO;

import java.math.BigDecimal;
import java.util.List;

public record TaxCalculationRequestDTO(
        BigDecimal salarioBruto,
        BigDecimal inssEmpregado,
        Integer dependentes,
        BigDecimal deducaoLegalDependente,
        BigDecimal pensao,
        List<TaxBracketDTO> tabelaIrrf,
        BigDecimal folhaSalarios,
        BigDecimal proLabore,
        BigDecimal receitaBruta,
        BigDecimal receitaLucroPresumido,
        BigDecimal limiteAdicionalIrpj
) {
}
