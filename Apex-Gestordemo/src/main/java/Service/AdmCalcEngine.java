package Service;

import DTO.AdmCalcRequestDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AdmCalcEngine {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal MINUS_ONE = BigDecimal.valueOf(-1);

    private enum PercentScale {
        AUTO,
        DECIMAL,
        PERCENTUAL
    }

    public Map<String, BigDecimal> calculate(AdmCalcRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Requisicao de calculo AdmCalc nao pode ser nula");
        }
        PercentScale scale = parseScale(request.escalaPercentual());

        BigDecimal precoVenda = nonNegative(request.precoVenda(), "precoVenda");
        BigDecimal quantidade = nonNegative(request.quantidade(), "quantidade");
        BigDecimal receitaTotal = nonNegative(request.receitaTotal(), "receitaTotal");
        BigDecimal cmv = nonNegative(request.cmv(), "cmv");
        BigDecimal despesas = nonNegative(request.despesas(), "despesas");
        BigDecimal impostos = nonNegative(request.impostos(), "impostos");
        BigDecimal custosFixos = nonNegative(request.custosFixos(), "custosFixos");
        BigDecimal custosVariaveis = nonNegative(request.custosVariaveis(), "custosVariaveis");
        BigDecimal ativoCirculante = nonNegative(request.ativoCirculante(), "ativoCirculante");
        BigDecimal ativoNaoCirculante = nonNegative(request.ativoNaoCirculante(), "ativoNaoCirculante");
        BigDecimal passivoCirculante = nonNegative(request.passivoCirculante(), "passivoCirculante");
        BigDecimal passivoNaoCirculante = nonNegative(request.passivoNaoCirculante(), "passivoNaoCirculante");
        BigDecimal estoque = nonNegative(request.estoque(), "estoque");
        BigDecimal custoBem = nonNegative(request.custoBem(), "custoBem");
        BigDecimal valorResidual = nonNegative(request.valorResidual(), "valorResidual");
        if (custoBem != null && valorResidual != null && valorResidual.compareTo(custoBem) > 0) {
            throw new IllegalArgumentException("valorResidual nao pode ser maior que custoBem");
        }
        BigDecimal vidaUtil = positive(request.vidaUtil(), "vidaUtil");
        BigDecimal custoTotalInput = nonNegative(request.custoTotal(), "custoTotal");
        BigDecimal quantidadeProduzida = positive(request.quantidadeProduzida(), "quantidadeProduzida");
        BigDecimal margemDesejadaInput = nonNegative(request.margemDesejada(), "margemDesejada");
        BigDecimal ganhoInvestimento = nonNegative(request.ganhoInvestimento(), "ganhoInvestimento");
        BigDecimal custoInvestimento = positive(request.custoInvestimento(), "custoInvestimento");
        BigDecimal periodo = nonNegative(request.periodo(), "periodo");
        BigDecimal taxaDescontoDecimal = null;
        if (request.taxaDesconto() != null) {
            taxaDescontoDecimal = validateDiscountRate(percent(request.taxaDesconto(), scale));
        }
        if (request.fluxosDeCaixa() != null) {
            validateCashFlows(request.fluxosDeCaixa());
        }

        Map<String, BigDecimal> results = new LinkedHashMap<>();

        BigDecimal faturamento = multiplyIfPresent(precoVenda, quantidade);
        put(results, "faturamento", faturamento);

        BigDecimal lucroBruto = subtractIfPresent(receitaTotal, cmv);
        put(results, "lucroBruto", lucroBruto);
        if (lucroBruto != null) {
            BigDecimal lucroLiquido = lucroBruto
                    .subtract(valueOr(despesas, ZERO))
                    .subtract(valueOr(impostos, ZERO));
            put(results, "lucroLiquido", lucroLiquido);
            put(results, "margemLucroBruta", dividePercent(lucroBruto, receitaTotal));
            put(results, "margemLucroLiquida", dividePercent(lucroLiquido, receitaTotal));
        }

        BigDecimal margemContribuicaoTotal = subtractIfPresent(receitaTotal, custosVariaveis);
        put(results, "margemContribuicao", margemContribuicaoTotal);
        if (margemContribuicaoTotal != null && receitaTotal != null && receitaTotal.compareTo(ZERO) != 0) {
            put(results, "margemContribuicaoPercentual",
                    margemContribuicaoTotal.divide(receitaTotal, MC).multiply(HUNDRED, MC));
        }

        BigDecimal margemContribuicaoInput = nonNegative(request.margemContribuicao(), "margemContribuicao");
        put(results, "pontoEquilibrio", divideIfPresent(custosFixos, margemContribuicaoInput));
        if (custosFixos != null && margemContribuicaoTotal != null && receitaTotal != null
                && margemContribuicaoTotal.compareTo(ZERO) > 0 && receitaTotal.compareTo(ZERO) > 0) {
            BigDecimal mcRatio = margemContribuicaoTotal.divide(receitaTotal, MC);
            put(results, "pontoEquilibrioReceita", custosFixos.divide(mcRatio, MC));
        }
        if (custosFixos != null && margemContribuicaoInput != null
                && margemContribuicaoInput.compareTo(ZERO) > 0) {
            put(results, "pontoEquilibrioUnidades", custosFixos.divide(margemContribuicaoInput, MC));
        }

        BigDecimal ativoTotal = addIfPresent(ativoCirculante, ativoNaoCirculante);
        BigDecimal passivoTotal = addIfPresent(passivoCirculante, passivoNaoCirculante);
        put(results, "ativoTotal", ativoTotal);
        put(results, "passivoTotal", passivoTotal);
        if (ativoTotal != null && passivoTotal != null) {
            put(results, "patrimonioLiquido", ativoTotal.subtract(passivoTotal));
        }
        put(results, "capitalGiro", subtractIfPresent(ativoCirculante, passivoCirculante));
        put(results, "liquidezCorrente", divideIfPresent(ativoCirculante, passivoCirculante));
        if (ativoCirculante != null && estoque != null && passivoCirculante != null) {
            put(results, "liquidezSeca", divideIfPresent(ativoCirculante.subtract(estoque), passivoCirculante));
        }

        if (custoBem != null && valorResidual != null && vidaUtil != null) {
            BigDecimal depreciacaoAnual = custoBem.subtract(valorResidual).divide(vidaUtil, MC);
            put(results, "depreciacaoAnual", depreciacaoAnual);
            put(results, "depreciacaoMensal", depreciacaoAnual.divide(TWELVE, MC));
        }

        // custoTotal deve ser consolidado antes de custoUnitario e precoMarkup.
        BigDecimal custoTotal = custoTotalInput != null
                ? custoTotalInput
                : addIfPresent(custosFixos, custosVariaveis);
        put(results, "custoTotal", custoTotal);
        put(results, "custoUnitario", divideIfPresent(custoTotal, quantidadeProduzida));

        if (custoTotal != null && margemDesejadaInput != null) {
            BigDecimal margem = percent(margemDesejadaInput, scale);
            if (margem.compareTo(ONE) >= 0) {
                throw new IllegalArgumentException("margemDesejada deve ser menor que 100%");
            }
            // Markup divisor: (1 - margem) aplicado sobre o custo (preco = custo / (1 - margem)).
            // Markup multiplicador: 1 / (1 - margem), equivalente a precoMarkup / custoTotal.
            BigDecimal markupDivisor = ONE.subtract(margem);
            BigDecimal precoMarkup = custoTotal.divide(markupDivisor, MC);
            BigDecimal markupMultiplicador = ONE.divide(markupDivisor, MC);
            put(results, "precoMarkup", precoMarkup);
            put(results, "markupMultiplicador", markupMultiplicador);
        }

        if (ganhoInvestimento != null && custoInvestimento != null) {
            put(results, "roi", ganhoInvestimento.subtract(custoInvestimento)
                    .divide(custoInvestimento, MC)
                    .multiply(HUNDRED, MC));
        }

        if (taxaDescontoDecimal != null && periodo != null
                && (request.valorFuturo() != null || request.valorPresente() != null)) {
            BigDecimal factor = compoundFactor(taxaDescontoDecimal, periodo);
            if (request.valorFuturo() != null) {
                put(results, "valorPresente", request.valorFuturo().divide(factor, MC));
            }
            if (request.valorPresente() != null) {
                put(results, "valorFuturo", request.valorPresente().multiply(factor, MC));
            }
        }

        if (request.fluxosDeCaixa() != null && !request.fluxosDeCaixa().isEmpty()) {
            if (taxaDescontoDecimal != null) {
                put(results, "vpl", calculateVpl(request.fluxosDeCaixa(), taxaDescontoDecimal));
            }
            put(results, "tir", calculateTir(request.fluxosDeCaixa()).multiply(HUNDRED, MC));
        }
        return results;
    }

    /**
     * Emite alertas de interpretacao para fluxos nao convencionais (multiplas mudancas de sinal).
     */
    public List<String> warnings(AdmCalcRequestDTO request) {
        if (request == null || request.fluxosDeCaixa() == null || request.fluxosDeCaixa().isEmpty()) {
            return List.of();
        }
        validateCashFlows(request.fluxosDeCaixa());
        if (countSignChanges(request.fluxosDeCaixa()) > 1) {
            return List.of("Fluxo de caixa possui multiplas mudancas de sinal; podem existir multiplas TIRs pela regra dos sinais de Descartes.");
        }
        return List.of();
    }

    /**
     * Calcula a TIR por bissecao em (-0.9999, 10.0).
     * Em fluxos com multiplas mudancas de sinal, a bissecao encontra apenas uma raiz no intervalo.
     */
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

    private BigDecimal calculateVpl(List<BigDecimal> cashFlows, BigDecimal rate) {
        BigDecimal total = ZERO;
        BigDecimal onePlusRate = ONE.add(rate);
        BigDecimal discountFactor = ONE;
        for (int i = 0; i < cashFlows.size(); i++) {
            if (i > 0) {
                discountFactor = discountFactor.multiply(onePlusRate, MC);
            }
            total = total.add(cashFlows.get(i).divide(discountFactor, MC), MC);
        }
        return total;
    }

    private double npv(List<BigDecimal> cashFlows, double rate) {
        double onePlusRate = 1.0 + rate;
        if (!Double.isFinite(onePlusRate) || onePlusRate <= 0.0) {
            throw new IllegalArgumentException("Taxa de desconto invalida para calculo do VPL/TIR");
        }
        double total = 0.0;
        double discountFactor = 1.0;
        for (int i = 0; i < cashFlows.size(); i++) {
            double cf = cashFlows.get(i).doubleValue();
            if (!Double.isFinite(cf)) {
                throw new IllegalArgumentException("fluxosDeCaixa contem valor fora dos limites numericos finitos");
            }
            if (i > 0) {
                discountFactor *= onePlusRate;
            }
            if (!Double.isFinite(discountFactor) || discountFactor == 0.0) {
                throw new IllegalArgumentException("Fluxo de caixa excede os limites finitos de convergencia da TIR");
            }
            total += cf / discountFactor;
            if (!Double.isFinite(total)) {
                throw new IllegalArgumentException("Resultado de VPL/TIR fora dos limites numericos finitos");
            }
        }
        return total;
    }

    private void validateCashFlows(List<BigDecimal> cashFlows) {
        for (int i = 0; i < cashFlows.size(); i++) {
            if (cashFlows.get(i) == null) {
                throw new IllegalArgumentException("fluxosDeCaixa nao pode conter elementos nulos (indice " + i + ")");
            }
        }
    }

    private int countSignChanges(List<BigDecimal> cashFlows) {
        int changes = 0;
        int previousSign = 0;
        for (BigDecimal cf : cashFlows) {
            int sign = cf.signum();
            if (sign == 0) {
                continue;
            }
            if (previousSign != 0 && sign != previousSign) {
                changes++;
            }
            previousSign = sign;
        }
        return changes;
    }

    private BigDecimal compoundFactor(BigDecimal rateDecimal, BigDecimal periodo) {
        BigDecimal onePlusRate = ONE.add(rateDecimal);
        double base = onePlusRate.doubleValue();
        double exp = periodo.doubleValue();
        if (!Double.isFinite(base) || !Double.isFinite(exp) || base <= 0.0) {
            throw new IllegalArgumentException("taxaDesconto e periodo invalidos para capitalizacao");
        }
        double factor = Math.pow(base, exp);
        if (!Double.isFinite(factor) || factor <= 0.0) {
            throw new IllegalArgumentException("Fator de capitalizacao fora dos limites numericos finitos");
        }
        return BigDecimal.valueOf(factor);
    }

    private BigDecimal validateDiscountRate(BigDecimal rateDecimal) {
        if (rateDecimal.compareTo(MINUS_ONE) <= 0) {
            throw new IllegalArgumentException("taxaDesconto deve ser maior que -100%");
        }
        return rateDecimal;
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
        BigDecimal quotient = divideIfPresent(left, right);
        return quotient == null ? null : quotient.multiply(HUNDRED, MC);
    }

    private void put(Map<String, BigDecimal> results, String key, BigDecimal value) {
        if (value != null) {
            results.put(key, value.setScale(2, RoundingMode.HALF_UP));
        }
    }

    private BigDecimal valueOr(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private BigDecimal nonNegative(BigDecimal value, String field) {
        if (value != null && value.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(field + " nao pode ser negativo");
        }
        return value;
    }

    private BigDecimal positive(BigDecimal value, String field) {
        if (value != null && value.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException(field + " deve ser maior que zero");
        }
        return value;
    }

    private BigDecimal nonZero(BigDecimal value, String field) {
        if (value == null || value.compareTo(ZERO) == 0) {
            throw new IllegalArgumentException(field + " nao pode ser zero");
        }
        return value;
    }

    private PercentScale parseScale(String raw) {
        if (raw == null || raw.isBlank()) {
            return PercentScale.AUTO;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "AUTO" -> PercentScale.AUTO;
            case "DECIMAL" -> PercentScale.DECIMAL;
            case "PERCENTUAL" -> PercentScale.PERCENTUAL;
            default -> throw new IllegalArgumentException("escalaPercentual deve ser AUTO, DECIMAL ou PERCENTUAL");
        };
    }

    /**
     * Converte taxas/margens para fracao decimal conforme escalaPercentual.
     * Recomenda-se informar explicitamente "PERCENTUAL" (ex.: 30 -> 0.30, 0.5 -> 0.005)
     * ou "DECIMAL" (ex.: 0.30 -> 0.30, 1.0 -> 1.00); "AUTO" mantem heuristica legada (>1 divide por 100).
     */
    private BigDecimal percent(BigDecimal value, PercentScale scale) {
        BigDecimal safe = valueOr(value, ZERO);
        return switch (scale) {
            case PERCENTUAL -> safe.divide(HUNDRED, MC);
            case DECIMAL -> safe;
            case AUTO -> safe.abs().compareTo(ONE) > 0 ? safe.divide(HUNDRED, MC) : safe;
        };
    }
}
