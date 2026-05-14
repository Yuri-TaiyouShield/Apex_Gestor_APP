package com.Apex.Apex_Gestordemo;

import DTO.AdmCalcRequestDTO;
import DTO.LaborCalculationRequestDTO;
import DTO.TaxBracketDTO;
import DTO.TaxCalculationRequestDTO;
import Service.AdmCalcEngine;
import Service.LaborCalculationEngine;
import Service.TaxCalculationEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialCalculationEngineTests {

    private final LaborCalculationEngine labor = new LaborCalculationEngine();
    private final TaxCalculationEngine tax = new TaxCalculationEngine();
    private final AdmCalcEngine admCalc = new AdmCalcEngine();

    @Test
    void laborEngineCalculatesCoreSalaryAndRiskFormulas() {
        LaborCalculationRequestDTO request = new LaborCalculationRequestDTO(
                BigDecimal.valueOf(3000),
                BigDecimal.valueOf(220),
                BigDecimal.valueOf(0.5),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(600),
                22,
                5,
                BigDecimal.valueOf(1500),
                BigDecimal.valueOf(0.2),
                BigDecimal.valueOf(3000),
                BigDecimal.valueOf(250),
                2,
                BigDecimal.valueOf(62),
                BigDecimal.valueOf(400),
                BigDecimal.valueOf(100),
                6,
                1,
                1,
                BigDecimal.valueOf(3000),
                List.of(new TaxBracketDTO(BigDecimal.valueOf(2000), BigDecimal.valueOf(0.08), BigDecimal.ZERO), new TaxBracketDTO(null, BigDecimal.valueOf(0.09), BigDecimal.ZERO)),
                2,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(0.4),
                BigDecimal.valueOf(3500),
                BigDecimal.valueOf(2000),
                BigDecimal.valueOf(12000),
                3,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1.10),
                BigDecimal.valueOf(0.05)
        );

        Map<String, BigDecimal> results = labor.calculate(request);

        assertThat(results.get("salarioHora")).isEqualByComparingTo("13.64");
        assertThat(results.get("salarioDiario")).isEqualByComparingTo("100.00");
        assertThat(results.get("adicionalPericulosidade")).isEqualByComparingTo("900.00");
        assertThat(results.get("adicionalInsalubridade")).isEqualByComparingTo("300.00");
        assertThat(results.get("descontoValeTransporte")).isEqualByComparingTo("180.00");
        assertThat(results.get("salarioFamilia")).isEqualByComparingTo("124.00");
        assertThat(results.get("multaFgts")).isEqualByComparingTo("4000.00");
    }

    @Test
    void taxEngineCalculatesIrrfAndPresumedProfit() {
        TaxCalculationRequestDTO request = new TaxCalculationRequestDTO(
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(550),
                1,
                BigDecimal.valueOf(189.59),
                BigDecimal.valueOf(100),
                List.of(
                        new TaxBracketDTO(BigDecimal.valueOf(2500), BigDecimal.valueOf(0), BigDecimal.ZERO),
                        new TaxBracketDTO(BigDecimal.valueOf(5000), BigDecimal.valueOf(0.15), BigDecimal.valueOf(370)),
                        new TaxBracketDTO(null, BigDecimal.valueOf(0.275), BigDecimal.valueOf(900))
                ),
                BigDecimal.valueOf(28000),
                BigDecimal.valueOf(2000),
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(100000),
                BigDecimal.valueOf(20000)
        );

        Map<String, BigDecimal> results = tax.calculate(request);

        assertThat(results.get("baseIrrf")).isEqualByComparingTo("4160.41");
        assertThat(results.get("irrf")).isEqualByComparingTo("254.06");
        assertThat(results.get("fatorR")).isEqualByComparingTo("30.00");
        assertThat(results.get("lucroPresumidoBaseIrpj")).isEqualByComparingTo("32000.00");
        assertThat(results.get("lucroPresumidoAdicionalIrpj")).isEqualByComparingTo("1200.00");
    }

    @Test
    void admCalcEngineMigratesOriginalFinancialFormulas() {
        AdmCalcRequestDTO request = new AdmCalcRequestDTO(
                BigDecimal.valueOf(49.90),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(6000),
                BigDecimal.valueOf(1200),
                BigDecimal.valueOf(800),
                BigDecimal.valueOf(3000),
                BigDecimal.valueOf(4000),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(80000),
                BigDecimal.valueOf(120000),
                BigDecimal.valueOf(30000),
                BigDecimal.valueOf(40000),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(25000),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(5),
                BigDecimal.valueOf(7000),
                BigDecimal.valueOf(350),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(15000),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(16105.10),
                BigDecimal.valueOf(0.10),
                BigDecimal.valueOf(5),
                List.of(BigDecimal.valueOf(-1000), BigDecimal.valueOf(300), BigDecimal.valueOf(400), BigDecimal.valueOf(500), BigDecimal.valueOf(600))
        );

        Map<String, BigDecimal> results = admCalc.calculate(request);

        assertThat(results.get("faturamento")).isEqualByComparingTo("4990.00");
        assertThat(results.get("lucroBruto")).isEqualByComparingTo("4000.00");
        assertThat(results.get("lucroLiquido")).isEqualByComparingTo("2000.00");
        assertThat(results.get("patrimonioLiquido")).isEqualByComparingTo("130000.00");
        assertThat(results.get("precoMarkup")).isEqualByComparingTo("10000.00");
        assertThat(results.get("roi")).isEqualByComparingTo("50.00");
    }
}
