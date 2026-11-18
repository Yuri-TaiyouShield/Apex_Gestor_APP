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
        if (request == null) {
            throw new IllegalArgumentException("Requisicao tributaria nao pode ser nula");
        }
        Map<String, BigDecimal> results = new LinkedHashMap<>();
        BigDecimal salarioBruto = valueOr(nonNegative(request.salarioBruto(), "salarioBruto"), ZERO);
        BigDecimal inss = valueOr(nonNegative(request.inssEmpregado(), "inssEmpregado"), ZERO);
        int dependentes = nonNegativeInt(request.dependentes(), 0, "dependentes");
        BigDecimal deducaoDependentes = valueOr(nonNegative(request.deducaoLegalDependente(), "deducaoLegalDependente"), ZERO)
                .multiply(BigDecimal.valueOf(dependentes), MC);
        BigDecimal pensao = valueOr(nonNegative(request.pensao(), "pensao"), ZERO);
        validateBrackets(request.tabelaIrrf());
        BigDecimal baseIrrf = salarioBruto.subtract(inss).subtract(deducaoDependentes).subtract(pensao).max(ZERO);
        results.put("baseIrrf", money(baseIrrf));
        results.put("irrf", money(calculateDeductibleTax(baseIrrf, request.tabelaIrrf())));

        BigDecimal folha = valueOr(nonNegative(request.folhaSalarios(), "folhaSalarios"), ZERO);
        BigDecimal proLabore = valueOr(nonNegative(request.proLabore(), "proLabore"), ZERO);
        BigDecimal receitaBruta = valueOr(nonNegative(request.receitaBruta(), "receitaBruta"), ZERO);
        BigDecimal fatorR = receitaBruta.signum() == 0 ? ZERO : folha.add(proLabore).divide(receitaBruta, MC);
        results.put("fatorR", percentDisplay(fatorR));

        BigDecimal receitaPresumido = valueOr(nonNegative(request.receitaLucroPresumido(), "receitaLucroPresumido"), receitaBruta);
        BigDecimal baseIrpj = receitaPresumido.multiply(BigDecimal.valueOf(0.32), MC);
        BigDecimal limiteAdicional = valueOr(nonNegative(request.limiteAdicionalIrpj(), "limiteAdicionalIrpj"), BigDecimal.valueOf(20000));
        results.put("lucroPresumidoBaseIrpj", money(baseIrpj));
        results.put("lucroPresumidoIrpj", money(baseIrpj.multiply(BigDecimal.valueOf(0.15), MC)));
        results.put("lucroPresumidoCsll", money(baseIrpj.multiply(BigDecimal.valueOf(0.09), MC)));
        results.put("lucroPresumidoPisCofins", money(receitaPresumido.multiply(BigDecimal.valueOf(0.0365), MC)));
        results.put("lucroPresumidoAdicionalIrpj", money(baseIrpj.subtract(limiteAdicional).max(ZERO).multiply(BigDecimal.valueOf(0.10), MC)));
        return results;
    }

    private void validateBrackets(List<TaxBracketDTO> brackets) {
        if (brackets == null) {
            return;
        }
        for (TaxBracketDTO bracket : brackets) {
            if (bracket == null) {
                throw new IllegalArgumentException("tabelaIrrf nao pode conter faixas nulas");
            }
            nonNegative(bracket.limite(), "tabelaIrrf.limite");
            nonNegative(bracket.aliquota(), "tabelaIrrf.aliquota");
            nonNegative(bracket.parcelaDeduzir(), "tabelaIrrf.parcelaDeduzir");
        }
    }

    private BigDecimal nonNegative(BigDecimal value, String field) {
        if (value != null && value.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(field + " nao pode ser negativo");
        }
        return value;
    }

    private int nonNegativeInt(Integer value, int fallback, String field) {
        int resolved = valueOr(value, fallback);
        if (resolved < 0) {
            throw new IllegalArgumentException(field + " nao pode ser negativo");
        }
        return resolved;
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
