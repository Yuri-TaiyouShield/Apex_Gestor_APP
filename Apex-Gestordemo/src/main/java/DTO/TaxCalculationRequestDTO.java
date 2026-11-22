package DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record TaxCalculationRequestDTO(
        BigDecimal salarioBruto,
        BigDecimal inssEmpregado,
        Integer dependentes,
        BigDecimal deducaoLegalDependente,
        BigDecimal pensao,
        @Size(max = 120) List<@Valid TaxBracketDTO> tabelaIrrf,
        BigDecimal folhaSalarios,
        BigDecimal proLabore,
        BigDecimal receitaBruta,
        BigDecimal receitaLucroPresumido,
        BigDecimal limiteAdicionalIrpj
) {
}
