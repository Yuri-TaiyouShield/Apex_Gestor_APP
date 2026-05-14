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
    private static final BigDecimal NIGHT_FACTOR = BigDecimal.valueOf(1.142857);
    private static final BigDecimal NIGHT_ADDITIONAL = BigDecimal.valueOf(0.20);
    private static final BigDecimal PERICULOSIDADE = BigDecimal.valueOf(0.30);
    private static final BigDecimal VT_LIMIT = BigDecimal.valueOf(0.06);
    private static final BigDecimal FERIAS_TERCO = BigDecimal.valueOf(1.3333);
    private static final BigDecimal FGTS_MONTHLY = BigDecimal.valueOf(0.08);

    public Map<String, BigDecimal> calculate(LaborCalculationRequestDTO request) {
        Map<String, BigDecimal> results = new LinkedHashMap<>();
        BigDecimal salarioMensal = positive(request.salarioMensal(), "salarioMensal");
        BigDecimal salarioBase = valueOr(request.salarioBase(), salarioMensal);
        BigDecimal jornadaMensal = positive(request.jornadaMensal(), "jornadaMensal");
        BigDecimal percentualHoraExtra = percent(valueOr(request.percentualHoraExtra(), BigDecimal.valueOf(0.50)));
        BigDecimal salarioHora = money(salarioMensal.divide(jornadaMensal, MC));
        BigDecimal salarioDiario = money(salarioMensal.divide(THIRTY, MC));

        results.put("salarioHora", salarioHora);
        results.put("salarioDiario", salarioDiario);
        results.put("horaExtraComum", money(salarioHora.multiply(ONE.add(percentualHoraExtra), MC)));
        results.put("fatorReducaoNoturna", quantity(valueOr(request.horasRelogioNoturnas(), ZERO).multiply(NIGHT_FACTOR, MC)));
        results.put("horaExtraNoturna", money(salarioHora.multiply(ONE.add(percentualHoraExtra), MC).multiply(ONE.add(NIGHT_ADDITIONAL), MC)));

        BigDecimal totalVerbasVariaveis = valueOr(request.totalVerbasVariaveis(), ZERO);
        BigDecimal dsrVariaveis = calculateDsr(totalVerbasVariaveis, request.diasUteisMes(), request.domingosFeriadosMes());
        results.put("dsrVariaveis", dsrVariaveis);
        results.put("baseFeriasDecimoComDsr", money(totalVerbasVariaveis.add(dsrVariaveis)));
        results.put("horaSobreaviso", money(salarioHora.divide(BigDecimal.valueOf(3), MC)));
        results.put("horaProntidao", money(salarioHora.multiply(BigDecimal.valueOf(2), MC).divide(BigDecimal.valueOf(3), MC)));

        BigDecimal adicionalInsalubridade = money(valueOr(request.salarioMinimo(), ZERO).multiply(percent(valueOr(request.percentualInsalubridade(), ZERO)), MC));
        BigDecimal adicionalPericulosidade = money(salarioBase.multiply(PERICULOSIDADE, MC));
        results.put("adicionalInsalubridade", adicionalInsalubridade);
        results.put("adicionalPericulosidade", adicionalPericulosidade);
        results.put("salarioHoraIntegrado", money(salarioBase.add(adicionalPericulosidade).add(adicionalInsalubridade).divide(jornadaMensal, MC)));
        results.put("descontoValeTransporte", money(valueOr(request.custoRealPassagens(), ZERO).min(salarioBase.multiply(VT_LIMIT, MC))));
        results.put("salarioFamilia", money(BigDecimal.valueOf(valueOr(request.quantidadeFilhosElegiveis(), 0)).multiply(valueOr(request.cotaSalarioFamilia(), ZERO), MC)));

        BigDecimal mediaAdicionais = valueOr(request.mediaAdicionais(), ZERO);
        BigDecimal mediaDsr = valueOr(request.mediaDsr(), dsrVariaveis);
        BigDecimal baseAnual = salarioBase.add(mediaAdicionais).add(mediaDsr);
        results.put("decimoTerceiroProporcional", money(baseAnual.divide(BigDecimal.valueOf(12), MC).multiply(BigDecimal.valueOf(valueOr(request.mesesTrabalhados13(), 0)), MC)));
        results.put("feriasComTerco", money(baseAnual.multiply(FERIAS_TERCO, MC)));
        results.put("descontoFaltasDsr", money(salarioDiario.multiply(BigDecimal.valueOf(valueOr(request.diasFalta(), 0) + valueOr(request.dsrSemanaFalta(), 0)), MC)));
        results.put("inssEmpregado", money(calculateProgressive(valueOr(request.salarioContribuicao(), salarioMensal), request.tabelaInss())));

        int anos = valueOr(request.anosCompletosTrabalhados(), 0);
        results.put("avisoPrevioIndenizado", money(salarioDiario.multiply(BigDecimal.valueOf(30L + (3L * anos)), MC)));
        results.put("multaFgts", money(valueOr(request.saldoFgts(), ZERO).multiply(percent(valueOr(request.percentualMultaFgts(), BigDecimal.valueOf(0.40))), MC)));
        results.put("multa477", money(valueOr(request.maiorRemuneracao(), salarioBase)));
        results.put("multa467", money(valueOr(request.verbasIncontroversas(), ZERO).multiply(BigDecimal.valueOf(0.50), MC)));
        results.put("indenizacao479", money(valueOr(request.salariosAteTerminoContrato(), ZERO).divide(BigDecimal.valueOf(2), MC)));
        results.put("indenizacaoEstabilidade", money(calculateStabilityIndemnity(request, salarioBase, mediaAdicionais, mediaDsr)));

        BigDecimal preJudicial = money(valueOr(request.valorNominal(), ZERO).multiply(valueOr(request.fatorIpcaEAcumulado(), ONE), MC));
        results.put("atualizacaoPreJudicial", preJudicial);
        results.put("atualizacaoJudicial", money(preJudicial.multiply(ONE.add(percent(valueOr(request.taxaSelicAcumulada(), ZERO))), MC)));
        return results;
    }

    private BigDecimal calculateDsr(BigDecimal totalVariavel, Integer diasUteis, Integer domingosFeriados) {
        int usefulDays = valueOr(diasUteis, 0);
        int restDays = valueOr(domingosFeriados, 0);
        if (usefulDays <= 0 || restDays <= 0) {
            return ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return money(totalVariavel.divide(BigDecimal.valueOf(usefulDays), MC).multiply(BigDecimal.valueOf(restDays), MC));
    }

    private BigDecimal calculateStabilityIndemnity(LaborCalculationRequestDTO request, BigDecimal salarioBase, BigDecimal mediaAdicionais, BigDecimal mediaDsr) {
        int months = valueOr(request.mesesRestantesEstabilidade(), 0);
        BigDecimal monthlyThirteenth = salarioBase.add(mediaAdicionais).add(mediaDsr).divide(BigDecimal.valueOf(12), MC);
        BigDecimal monthlyVacation = salarioBase.multiply(FERIAS_TERCO, MC).divide(BigDecimal.valueOf(12), MC);
        BigDecimal monthlyFgts = salarioBase.multiply(FGTS_MONTHLY, MC);
        return BigDecimal.valueOf(months).multiply(salarioBase.add(mediaAdicionais).add(mediaDsr).add(monthlyThirteenth).add(monthlyVacation).add(monthlyFgts), MC);
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
