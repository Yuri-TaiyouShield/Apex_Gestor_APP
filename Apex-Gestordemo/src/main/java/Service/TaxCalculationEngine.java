package Service;

import DTO.TaxBracketDTO;
import DTO.TaxCalculationRequestDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaxCalculationEngine {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    public Map<String, BigDecimal> calculate(TaxCalculationRequestDTO request) {
        Map<String, BigDecimal> results = new LinkedHashMap<>();
        BigDecimal salarioBruto = valueOr(request.salarioBruto(), ZERO);
        BigDecimal inss = valueOr(request.inssEmpregado(), ZERO);
        int dependentes = valueOr(request.dependentes(), 0);
        BigDecimal deducaoDependentes = valueOr(request.deducaoLegalDependente(), ZERO).multiply(BigDecimal.valueOf(dependentes), MC);
        BigDecimal pensao = valueOr(request.pensao(), ZERO);
        BigDecimal baseIrrf = salarioBruto.subtract(inss).subtract(deducaoDependentes).subtract(pensao).max(ZERO);
        results.put("baseIrrf", money(baseIrrf));
        results.put("irrf", money(calculateDeductibleTax(baseIrrf, request.tabelaIrrf())));

        BigDecimal folha = valueOr(request.folhaSalarios(), ZERO);
        BigDecimal proLabore = valueOr(request.proLabore(), ZERO);
        BigDecimal receitaBruta = valueOr(request.receitaBruta(), ZERO);
        BigDecimal fatorR = receitaBruta.signum() == 0 ? ZERO : folha.add(proLabore).divide(receitaBruta, MC);
        results.put("fatorR", percentDisplay(fatorR));

        BigDecimal receitaPresumido = valueOr(request.receitaLucroPresumido(), receitaBruta);
        BigDecimal baseIrpj = receitaPresumido.multiply(BigDecimal.valueOf(0.32), MC);
        BigDecimal limiteAdicional = valueOr(request.limiteAdicionalIrpj(), BigDecimal.valueOf(20000));
        results.put("lucroPresumidoBaseIrpj", money(baseIrpj));
        results.put("lucroPresumidoIrpj", money(baseIrpj.multiply(BigDecimal.valueOf(0.15), MC)));
        results.put("lucroPresumidoCsll", money(baseIrpj.multiply(BigDecimal.valueOf(0.09), MC)));
        results.put("lucroPresumidoPisCofins", money(receitaPresumido.multiply(BigDecimal.valueOf(0.0365), MC)));
        results.put("lucroPresumidoAdicionalIrpj", money(baseIrpj.subtract(limiteAdicional).max(ZERO).multiply(BigDecimal.valueOf(0.10), MC)));
        return results;
    }

    private BigDecimal calculateDeductibleTax(BigDecimal base, List<TaxBracketDTO> brackets) {
        if (base == null || base.signum() <= 0 || brackets == null || brackets.isEmpty()) {
            return ZERO;
        }
        return brackets.stream()
                .sorted(Comparator.comparing(TaxBracketDTO::limite, Comparator.nullsLast(Comparator.naturalOrder())))
                .filter(bracket -> bracket.limite() == null || base.compareTo(bracket.limite()) <= 0)
                .findFirst()
                .map(bracket -> base.multiply(percent(valueOr(bracket.aliquota(), ZERO)), MC).subtract(valueOr(bracket.parcelaDeduzir(), ZERO)).max(ZERO))
                .orElse(ZERO);
    }

    private BigDecimal valueOr(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private int valueOr(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private BigDecimal percent(BigDecimal value) {
        BigDecimal safe = valueOr(value, ZERO);
        if (safe.abs().compareTo(ONE) > 0) {
            return safe.divide(BigDecimal.valueOf(100), MC);
        }
        return safe;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentDisplay(BigDecimal value) {
        return value.multiply(BigDecimal.valueOf(100), MC).setScale(2, RoundingMode.HALF_UP);
    }
}
