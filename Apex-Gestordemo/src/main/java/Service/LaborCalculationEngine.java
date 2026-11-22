package Service;

import DTO.LaborCalculationRequestDTO;
import DTO.TaxBracketDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LaborCalculationEngine {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal THIRTY = BigDecimal.valueOf(30);
    private static final BigDecimal NIGHT_FACTOR = BigDecimal.valueOf(8).divide(BigDecimal.valueOf(7), MC);
    private static final BigDecimal NIGHT_ADDITIONAL = BigDecimal.valueOf(0.20);
    private static final BigDecimal PERICULOSIDADE = BigDecimal.valueOf(0.30);
    private static final BigDecimal VT_LIMIT = BigDecimal.valueOf(0.06);
    private static final BigDecimal FERIAS_TERCO = BigDecimal.valueOf(4).divide(BigDecimal.valueOf(3), MC);
    private static final BigDecimal FGTS_MONTHLY = BigDecimal.valueOf(0.08);

    public Map<String, BigDecimal> calculate(LaborCalculationRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Requisicao trabalhista nao pode ser nula");
        }
        Map<String, BigDecimal> results = new LinkedHashMap<>();
        BigDecimal salarioMensal = positive(request.salarioMensal(), "salarioMensal");
        BigDecimal salarioBase = valueOr(nonNegative(request.salarioBase(), "salarioBase"), salarioMensal);
        BigDecimal jornadaMensal = positive(request.jornadaMensal(), "jornadaMensal");
        BigDecimal percentualHoraExtra = percent(valueOr(nonNegative(request.percentualHoraExtra(), "percentualHoraExtra"), BigDecimal.valueOf(0.50)));
        BigDecimal salarioHora = money(salarioMensal.divide(jornadaMensal, MC));
        BigDecimal salarioDiario = money(salarioMensal.divide(THIRTY, MC));

        results.put("salarioHora", salarioHora);
        results.put("salarioDiario", salarioDiario);
        results.put("horaExtraComum", money(salarioHora.multiply(ONE.add(percentualHoraExtra), MC)));
        BigDecimal horasNoturnas = valueOr(nonNegative(request.horasRelogioNoturnas(), "horasRelogioNoturnas"), ZERO);
        results.put("fatorReducaoNoturna", quantity(horasNoturnas.multiply(NIGHT_FACTOR, MC)));
        results.put("horaExtraNoturna", money(salarioHora.multiply(ONE.add(percentualHoraExtra), MC).multiply(ONE.add(NIGHT_ADDITIONAL), MC)));

        BigDecimal totalVerbasVariaveis = valueOr(nonNegative(request.totalVerbasVariaveis(), "totalVerbasVariaveis"), ZERO);
        int diasUteis = nonNegativeInt(request.diasUteisMes(), 0, "diasUteisMes");
        int domingosFeriados = nonNegativeInt(request.domingosFeriadosMes(), 0, "domingosFeriadosMes");
        BigDecimal dsrVariaveis = calculateDsr(totalVerbasVariaveis, diasUteis, domingosFeriados);
        results.put("dsrVariaveis", dsrVariaveis);
        results.put("baseFeriasDecimoComDsr", money(totalVerbasVariaveis.add(dsrVariaveis)));
        results.put("horaSobreaviso", money(salarioHora.divide(BigDecimal.valueOf(3), MC)));
        results.put("horaProntidao", money(salarioHora.multiply(BigDecimal.valueOf(2), MC).divide(BigDecimal.valueOf(3), MC)));

        BigDecimal adicionalInsalubridade = money(valueOr(nonNegative(request.salarioMinimo(), "salarioMinimo"), ZERO)
                .multiply(percent(valueOr(nonNegative(request.percentualInsalubridade(), "percentualInsalubridade"), ZERO)), MC));
        BigDecimal adicionalPericulosidade = money(salarioBase.multiply(PERICULOSIDADE, MC));
        results.put("adicionalInsalubridade", adicionalInsalubridade);
        results.put("adicionalPericulosidade", adicionalPericulosidade);
        results.put("salarioHoraIntegrado", money(salarioBase.add(adicionalPericulosidade).add(adicionalInsalubridade).divide(jornadaMensal, MC)));
        results.put("descontoValeTransporte", money(valueOr(nonNegative(request.custoRealPassagens(), "custoRealPassagens"), ZERO)
                .min(salarioBase.multiply(VT_LIMIT, MC))));
        int filhosElegiveis = nonNegativeInt(request.quantidadeFilhosElegiveis(), 0, "quantidadeFilhosElegiveis");
        results.put("salarioFamilia", money(BigDecimal.valueOf(filhosElegiveis)
                .multiply(valueOr(nonNegative(request.cotaSalarioFamilia(), "cotaSalarioFamilia"), ZERO), MC)));

        BigDecimal mediaAdicionais = valueOr(nonNegative(request.mediaAdicionais(), "mediaAdicionais"), ZERO);
        BigDecimal mediaDsr = valueOr(nonNegative(request.mediaDsr(), "mediaDsr"), dsrVariaveis);
        BigDecimal baseAnual = salarioBase.add(mediaAdicionais).add(mediaDsr);
        int meses13 = nonNegativeInt(request.mesesTrabalhados13(), 0, "mesesTrabalhados13");
        results.put("decimoTerceiroProporcional", money(baseAnual.divide(BigDecimal.valueOf(12), MC).multiply(BigDecimal.valueOf(meses13), MC)));
        results.put("feriasComTerco", money(baseAnual.multiply(FERIAS_TERCO, MC)));
        int diasFalta = nonNegativeInt(request.diasFalta(), 0, "diasFalta");
        int dsrSemanaFalta = nonNegativeInt(request.dsrSemanaFalta(), 0, "dsrSemanaFalta");
        results.put("descontoFaltasDsr", money(salarioDiario.multiply(BigDecimal.valueOf(diasFalta + dsrSemanaFalta), MC)));
        validateBrackets(request.tabelaInss());
        BigDecimal salarioContribuicao = valueOr(nonNegative(request.salarioContribuicao(), "salarioContribuicao"), salarioMensal);
        results.put("inssEmpregado", money(calculateProgressive(salarioContribuicao, request.tabelaInss())));

        int anos = nonNegativeInt(request.anosCompletosTrabalhados(), 0, "anosCompletosTrabalhados");
        results.put("avisoPrevioIndenizado", money(salarioDiario.multiply(BigDecimal.valueOf(30L + (3L * anos)), MC)));
        BigDecimal saldoFgts = valueOr(nonNegative(request.saldoFgts(), "saldoFgts"), ZERO);
        BigDecimal percentualMultaFgts = percent(valueOr(nonNegative(request.percentualMultaFgts(), "percentualMultaFgts"), BigDecimal.valueOf(0.40)));
        results.put("multaFgts", money(saldoFgts.multiply(percentualMultaFgts, MC)));
        results.put("multa477", money(valueOr(nonNegative(request.maiorRemuneracao(), "maiorRemuneracao"), salarioBase)));
        results.put("multa467", money(valueOr(nonNegative(request.verbasIncontroversas(), "verbasIncontroversas"), ZERO).multiply(BigDecimal.valueOf(0.50), MC)));
        results.put("indenizacao479", money(valueOr(nonNegative(request.salariosAteTerminoContrato(), "salariosAteTerminoContrato"), ZERO).divide(BigDecimal.valueOf(2), MC)));
        int mesesEstabilidade = nonNegativeInt(request.mesesRestantesEstabilidade(), 0, "mesesRestantesEstabilidade");
        results.put("indenizacaoEstabilidade", money(calculateStabilityIndemnity(mesesEstabilidade, salarioBase, mediaAdicionais, mediaDsr)));

        BigDecimal valorNominal = valueOr(nonNegative(request.valorNominal(), "valorNominal"), ZERO);
        BigDecimal fatorIpca = valueOr(nonNegative(request.fatorIpcaEAcumulado(), "fatorIpcaEAcumulado"), ONE);
        BigDecimal preJudicial = money(valorNominal.multiply(fatorIpca, MC));
        results.put("atualizacaoPreJudicial", preJudicial);
        BigDecimal selic = percent(valueOr(nonNegative(request.taxaSelicAcumulada(), "taxaSelicAcumulada"), ZERO));
        results.put("atualizacaoJudicial", money(preJudicial.multiply(ONE.add(selic), MC)));
        return results;
    }

    private BigDecimal calculateDsr(BigDecimal totalVariavel, int usefulDays, int restDays) {
        if (usefulDays <= 0 || restDays <= 0) {
            return ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return money(totalVariavel.divide(BigDecimal.valueOf(usefulDays), MC).multiply(BigDecimal.valueOf(restDays), MC));
    }

    private BigDecimal calculateStabilityIndemnity(int months, BigDecimal salarioBase, BigDecimal mediaAdicionais, BigDecimal mediaDsr) {
        BigDecimal monthlyThirteenth = salarioBase.add(mediaAdicionais).add(mediaDsr).divide(BigDecimal.valueOf(12), MC);
        BigDecimal monthlyVacation = salarioBase.multiply(FERIAS_TERCO, MC).divide(BigDecimal.valueOf(12), MC);
        BigDecimal monthlyFgts = salarioBase.multiply(FGTS_MONTHLY, MC);
        return BigDecimal.valueOf(months).multiply(salarioBase.add(mediaAdicionais).add(mediaDsr).add(monthlyThirteenth).add(monthlyVacation).add(monthlyFgts), MC);
    }

    private void validateBrackets(List<TaxBracketDTO> brackets) {
        if (brackets == null) {
            return;
        }
        for (int i = 0; i < brackets.size(); i++) {
            TaxBracketDTO bracket = brackets.get(i);
            if (bracket == null) {
                throw new IllegalArgumentException("tabelaInss nao pode conter faixas nulas");
            }
            nonNegative(bracket.limite(), "tabelaInss.limite");
            nonNegative(bracket.aliquota(), "tabelaInss.aliquota");
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

    BigDecimal calculateProgressive(BigDecimal base, List<TaxBracketDTO> brackets) {
        if (base == null || base.signum() <= 0 || brackets == null || brackets.isEmpty()) {
            return ZERO;
        }
        List<TaxBracketDTO> ordered = new ArrayList<>(brackets);
        ordered.sort(Comparator.comparing(TaxBracketDTO::limite, Comparator.nullsLast(Comparator.naturalOrder())));
        BigDecimal due = ZERO;
        BigDecimal previousLimit = ZERO;
        for (TaxBracketDTO bracket : ordered) {
            BigDecimal limit = bracket.limite();
            BigDecimal taxableUpper = limit == null ? base : base.min(limit);
            BigDecimal taxableSlice = taxableUpper.subtract(previousLimit);
            if (taxableSlice.signum() > 0) {
                due = due.add(taxableSlice.multiply(percent(valueOr(bracket.aliquota(), ZERO)), MC));
            }
            if (limit == null || base.compareTo(limit) <= 0) {
                break;
            }
            previousLimit = limit;
        }
        return due.max(ZERO);
    }

    private BigDecimal valueOr(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private int valueOr(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private BigDecimal positive(BigDecimal value, String field) {
        if (value == null || value.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException(field + " deve ser maior que zero");
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

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal quantity(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
