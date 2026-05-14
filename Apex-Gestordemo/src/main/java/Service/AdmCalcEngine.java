package Service;

import DTO.AdmCalcRequestDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdmCalcEngine {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    public Map<String, BigDecimal> calculate(AdmCalcRequestDTO request) {
        Map<String, BigDecimal> results = new LinkedHashMap<>();
        BigDecimal faturamento = multiplyIfPresent(request.precoVenda(), request.quantidade());
        put(results, "faturamento", faturamento);
        BigDecimal lucroBruto = subtractIfPresent(request.receitaTotal(), request.cmv());
        put(results, "lucroBruto", lucroBruto);
        if (lucroBruto != null) {
            put(results, "lucroLiquido", lucroBruto.subtract(valueOr(request.despesas(), ZERO)).subtract(valueOr(request.impostos(), ZERO)));
            put(results, "margemLucroBruta", dividePercent(lucroBruto, request.receitaTotal()));
        }
        if (results.get("lucroLiquido") != null) {
            put(results, "margemLucroLiquida", dividePercent(results.get("lucroLiquido"), request.receitaTotal()));
        }
        put(results, "margemContribuicao", subtractIfPresent(request.receitaTotal(), request.custosVariaveis()));
        put(results, "pontoEquilibrio", divideIfPresent(request.custosFixos(), request.margemContribuicao()));
        put(results, "ativoTotal", addIfPresent(request.ativoCirculante(), request.ativoNaoCirculante()));
        put(results, "passivoTotal", addIfPresent(request.passivoCirculante(), request.passivoNaoCirculante()));
        if (results.get("ativoTotal") != null && results.get("passivoTotal") != null) {
            put(results, "patrimonioLiquido", results.get("ativoTotal").subtract(results.get("passivoTotal")));
        }
        put(results, "capitalGiro", subtractIfPresent(request.ativoCirculante(), request.passivoCirculante()));
        put(results, "liquidezCorrente", divideIfPresent(request.ativoCirculante(), request.passivoCirculante()));
        if (request.ativoCirculante() != null && request.estoque() != null) {
            put(results, "liquidezSeca", divideIfPresent(request.ativoCirculante().subtract(request.estoque()), request.passivoCirculante()));
        }
        if (request.custoBem() != null && request.valorResidual() != null) {
            put(results, "depreciacaoAnual", divideIfPresent(request.custoBem().subtract(request.valorResidual()), request.vidaUtil()));
        }
        put(results, "custoUnitario", divideIfPresent(request.custoTotal(), request.quantidadeProduzida()));
        if (request.custoTotal() != null && request.margemDesejada() != null) {
            BigDecimal margem = percent(request.margemDesejada());
            if (margem.compareTo(ONE) >= 0) {
                throw new IllegalArgumentException("margemDesejada deve ser menor que 100%");
            }
            put(results, "precoMarkup", request.custoTotal().divide(ONE.subtract(margem), MC));
        }
        put(results, "custoTotal", addIfPresent(request.custosFixos(), request.custosVariaveis()));
        if (request.ganhoInvestimento() != null && request.custoInvestimento() != null) {
            put(results, "roi", request.ganhoInvestimento().subtract(request.custoInvestimento()).divide(nonZero(request.custoInvestimento(), "custoInvestimento"), MC).multiply(BigDecimal.valueOf(100), MC));
        }
        if (request.valorFuturo() != null && request.taxaDesconto() != null && request.periodo() != null) {
            BigDecimal divisor = BigDecimal.valueOf(Math.pow(ONE.add(percent(request.taxaDesconto())).doubleValue(), request.periodo().doubleValue()));
            put(results, "valorPresente", request.valorFuturo().divide(divisor, MC));
        }
        if (request.valorPresente() != null && request.taxaDesconto() != null && request.periodo() != null) {
            BigDecimal multiplier = BigDecimal.valueOf(Math.pow(ONE.add(percent(request.taxaDesconto())).doubleValue(), request.periodo().doubleValue()));
            put(results, "valorFuturo", request.valorPresente().multiply(multiplier, MC));
        }
        if (request.fluxosDeCaixa() != null && !request.fluxosDeCaixa().isEmpty()) {
            put(results, "tir", calculateTir(request.fluxosDeCaixa()).multiply(BigDecimal.valueOf(100), MC));
        }
        return results;
    }

    private BigDecimal calculateTir(List<BigDecimal> cashFlows) {
        double lower = -0.9999;
        double upper = 10.0;
        double lowerNpv = npv(cashFlows, lower);
        double upperNpv = npv(cashFlows, upper);
        if (Math.signum(lowerNpv) == Math.signum(upperNpv)) {
            throw new IllegalArgumentException("Fluxo de caixa nao possui mudanca de sinal suficiente para calcular TIR");
        }

        for (int iteration = 0; iteration < 200; iteration++) {
            double middle = (lower + upper) / 2.0;
            double middleNpv = npv(cashFlows, middle);
            if (Math.abs(middleNpv) < 0.0000001) {
                return BigDecimal.valueOf(middle);
            }
            if (Math.signum(lowerNpv) == Math.signum(middleNpv)) {
                lower = middle;
                lowerNpv = middleNpv;
            } else {
                upper = middle;
                upperNpv = middleNpv;
            }
        }
        return BigDecimal.valueOf((lower + upper) / 2.0);
    }

    private double npv(List<BigDecimal> cashFlows, double rate) {
        double total = 0.0;
        for (int i = 0; i < cashFlows.size(); i++) {
            total += cashFlows.get(i).doubleValue() / Math.pow(1 + rate, i);
        }
        return total;
    }

    private BigDecimal multiplyIfPresent(BigDecimal left, BigDecimal right) {
        return left == null || right == null ? null : left.multiply(right, MC);
    }

    private BigDecimal addIfPresent(BigDecimal left, BigDecimal right) {
        return left == null || right == null ? null : left.add(right);
    }

    private BigDecimal subtractIfPresent(BigDecimal left, BigDecimal right) {
        return left == null || right == null ? null : left.subtract(right);
    }

    private BigDecimal divideIfPresent(BigDecimal left, BigDecimal right) {
        return left == null || right == null ? null : left.divide(nonZero(right, "divisor"), MC);
    }

    private BigDecimal dividePercent(BigDecimal left, BigDecimal right) {
        return divideIfPresent(left, right) == null ? null : divideIfPresent(left, right).multiply(BigDecimal.valueOf(100), MC);
    }

    private void put(Map<String, BigDecimal> results, String key, BigDecimal value) {
        if (value != null) {
            results.put(key, value.setScale(2, RoundingMode.HALF_UP));
        }
    }

    private BigDecimal valueOr(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private BigDecimal nonZero(BigDecimal value, String field) {
        if (value == null || value.compareTo(ZERO) == 0) {
            throw new IllegalArgumentException(field + " nao pode ser zero");
        }
        return value;
    }

    private BigDecimal percent(BigDecimal value) {
        BigDecimal safe = valueOr(value, ZERO);
        if (safe.abs().compareTo(ONE) > 0) {
            return safe.divide(BigDecimal.valueOf(100), MC);
        }
        return safe;
    }
}
