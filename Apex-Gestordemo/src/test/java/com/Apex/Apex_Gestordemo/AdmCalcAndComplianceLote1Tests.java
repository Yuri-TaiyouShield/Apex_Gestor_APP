package com.Apex.Apex_Gestordemo;

import DTO.AdmCalcRequestDTO;
import DTO.FinancialCalculationResponseDTO;
import DTO.FinancialDocumentDTO;
import DTO.FinancialDocumentRequestDTO;
import DTO.LaborCalculationRequestDTO;
import DTO.SignFinancialDocumentRequestDTO;
import DTO.TaxBracketDTO;
import DTO.TaxCalculationRequestDTO;
import Model.AuditLog;
import Model.AuditRevisionContext;
import Model.AuditRevisionContextHolder;
import Model.FinancialAuditEvent;
import Model.FinancialCalculation;
import Model.FinancialDigitalDocument;
import Repository.AuditLogRepository;
import Repository.FinancialAuditEventRepository;
import Repository.FinancialCalculationRepository;
import Repository.FinancialDigitalDocumentRepository;
import Service.AdmCalcEngine;
import Service.AuditTrailService;
import Service.DataProtectionService;
import Service.FinancialAuditTrailService;
import Service.FinancialCalculationService;
import Service.FinancialDocumentDeliveryService;
import Service.FinancialDocumentStateMachine;
import Service.FinancialDocumentWorkflowService;
import Service.JwtTokenService;
import Service.LaborCalculationEngine;
import Service.PrivacyGuardService;
import Service.TaxCalculationEngine;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.FilterChain;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * QA Lote 1 — Motores AdmCalc / Trabalhista / Tributario, Snapshot Envers e Workflow de Assinatura.
 *
 * Nota de auditoria sobre AdmCalcEngine:
 * O motor implementa 27 indicadores (faturamento, lucroBruto, lucroLiquido, margemLucroBruta,
 * margemLucroLiquida, margemContribuicao, margemContribuicaoPercentual, pontoEquilibrio,
 * pontoEquilibrioReceita, pontoEquilibrioUnidades, ativoTotal, passivoTotal, patrimonioLiquido,
 * capitalGiro, liquidezCorrente, liquidezSeca, depreciacaoAnual, depreciacaoMensal, custoTotal,
 * custoUnitario, precoMarkup, markupMultiplicador, roi, valorPresente, valorFuturo, vpl, tir).
 * Os indicadores liquidezGeral, grauEndividamento, giroEstoque, precoVendaSugerido e ebitda
 * nao estao implementados em AdmCalcEngine / AdmCalcRequestDTO.
 */
class AdmCalcAndComplianceLote1Tests {

    private static AdmCalcRequestDTO emptyAdmCalc() {
        return new AdmCalcRequestDTO(
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null
        );
    }

    private static AdmCalcRequestDTO admCalcBase(
            BigDecimal precoVenda,
            BigDecimal quantidade,
            BigDecimal receitaTotal,
            BigDecimal cmv,
            BigDecimal despesas,
            BigDecimal impostos,
            BigDecimal custosFixos,
            BigDecimal custosVariaveis,
            BigDecimal margemContribuicao,
            BigDecimal ativoCirculante,
            BigDecimal ativoNaoCirculante,
            BigDecimal passivoCirculante,
            BigDecimal passivoNaoCirculante,
            BigDecimal estoque,
            BigDecimal custoBem,
            BigDecimal valorResidual,
            BigDecimal vidaUtil,
            BigDecimal custoTotal,
            BigDecimal quantidadeProduzida,
            BigDecimal margemDesejada,
            BigDecimal ganhoInvestimento,
            BigDecimal custoInvestimento,
            BigDecimal valorPresente,
            BigDecimal valorFuturo,
            BigDecimal taxaDesconto,
            BigDecimal periodo,
            List<BigDecimal> fluxosDeCaixa,
            String escalaPercentual
    ) {
        return new AdmCalcRequestDTO(
                precoVenda, quantidade, receitaTotal, cmv, despesas, impostos, custosFixos, custosVariaveis,
                margemContribuicao, ativoCirculante, ativoNaoCirculante, passivoCirculante, passivoNaoCirculante,
                estoque, custoBem, valorResidual, vidaUtil, custoTotal, quantidadeProduzida, margemDesejada,
                ganhoInvestimento, custoInvestimento, valorPresente, valorFuturo, taxaDesconto, periodo,
                fluxosDeCaixa, escalaPercentual
        );
    }

    @Nested
    class AdmCalcEngineTests {

        private final AdmCalcEngine engine = new AdmCalcEngine();

        @Test
        void todasAs27FormulasImplementadasSaoCalculadasComExatidaoMatematica() {
            AdmCalcRequestDTO request = admCalcBase(
                    bd("125.50"),            // precoVenda
                    bd("40"),                // quantidade -> faturamento = 5020.00
                    bd("10000"),             // receitaTotal
                    bd("4000"),              // cmv -> lucroBruto = 6000.00, margemLucroBruta = 60.00
                    bd("1500"),              // despesas
                    bd("500"),               // impostos -> lucroLiquido = 4000.00, margemLucroLiquida = 40.00
                    bd("2200"),              // custosFixos
                    bd("4500"),              // custosVariaveis -> margemContribuicao = 5500.00, % = 55.00
                    bd("50"),                // margemContribuicao unit -> PE = 44.00, PE unid = 44.00, PE receita = 4000.00
                    bd("80000"),             // ativoCirculante
                    bd("120000"),            // ativoNaoCirculante -> ativoTotal = 200000.00
                    bd("40000"),             // passivoCirculante -> capitalGiro = 40000.00, liquidezCorrente = 2.00
                    bd("60000"),             // passivoNaoCirculante -> passivoTotal = 100000.00, PL = 100000.00
                    bd("20000"),             // estoque -> liquidezSeca = 1.50
                    bd("25000"),             // custoBem
                    bd("5000"),              // valorResidual
                    bd("5"),                 // vidaUtil -> deprecAnual = 4000.00, deprecMensal = 333.33
                    null,                    // custoTotal omitido -> deriva custosFixos+custosVariaveis = 6700.00
                    bd("100"),               // quantidadeProduzida -> custoUnitario = 67.00
                    bd("20"),                // margemDesejada = 20% -> precoMarkup = 8375.00, mult = 1.25
                    bd("15000"),             // ganhoInvestimento
                    bd("10000"),             // custoInvestimento -> roi = 50.00
                    bd("10000"),             // valorPresente -> valorFuturo = 12100.00
                    bd("12100"),             // valorFuturo -> valorPresente = 10000.00
                    bd("10"),                // taxaDesconto = 10%
                    bd("2"),                 // periodo = 2
                    List.of(bd("-1000"), bd("550"), bd("605")), // vpl = -1000 + 500 + 500 = 0.00; tir = 10.00
                    "PERCENTUAL"
            );

            Map<String, BigDecimal> res = engine.calculate(request);

            assertThat(res).containsOnlyKeys(
                    "faturamento", "lucroBruto", "lucroLiquido", "margemLucroBruta", "margemLucroLiquida",
                    "margemContribuicao", "margemContribuicaoPercentual", "pontoEquilibrio",
                    "pontoEquilibrioReceita", "pontoEquilibrioUnidades", "ativoTotal", "passivoTotal",
                    "patrimonioLiquido", "capitalGiro", "liquidezCorrente", "liquidezSeca",
                    "depreciacaoAnual", "depreciacaoMensal", "custoTotal", "custoUnitario",
                    "precoMarkup", "markupMultiplicador", "roi", "valorPresente", "valorFuturo", "vpl", "tir"
            );

            assertThat(res.get("faturamento")).isEqualByComparingTo("5020.00");
            assertThat(res.get("lucroBruto")).isEqualByComparingTo("6000.00");
            assertThat(res.get("lucroLiquido")).isEqualByComparingTo("4000.00");
            assertThat(res.get("margemLucroBruta")).isEqualByComparingTo("60.00");
            assertThat(res.get("margemLucroLiquida")).isEqualByComparingTo("40.00");
            assertThat(res.get("margemContribuicao")).isEqualByComparingTo("5500.00");
            assertThat(res.get("margemContribuicaoPercentual")).isEqualByComparingTo("55.00");
            assertThat(res.get("pontoEquilibrio")).isEqualByComparingTo("44.00");
            assertThat(res.get("pontoEquilibrioReceita")).isEqualByComparingTo("4000.00");
            assertThat(res.get("pontoEquilibrioUnidades")).isEqualByComparingTo("44.00");
            assertThat(res.get("ativoTotal")).isEqualByComparingTo("200000.00");
            assertThat(res.get("passivoTotal")).isEqualByComparingTo("100000.00");
            assertThat(res.get("patrimonioLiquido")).isEqualByComparingTo("100000.00");
            assertThat(res.get("capitalGiro")).isEqualByComparingTo("40000.00");
            assertThat(res.get("liquidezCorrente")).isEqualByComparingTo("2.00");
            assertThat(res.get("liquidezSeca")).isEqualByComparingTo("1.50");
            assertThat(res.get("depreciacaoAnual")).isEqualByComparingTo("4000.00");
            assertThat(res.get("depreciacaoMensal")).isEqualByComparingTo("333.33");
            assertThat(res.get("custoTotal")).isEqualByComparingTo("6700.00");
            assertThat(res.get("custoUnitario")).isEqualByComparingTo("67.00");
            assertThat(res.get("precoMarkup")).isEqualByComparingTo("8375.00");
            assertThat(res.get("markupMultiplicador")).isEqualByComparingTo("1.25");
            assertThat(res.get("roi")).isEqualByComparingTo("50.00");
            assertThat(res.get("valorPresente")).isEqualByComparingTo("10000.00");
            assertThat(res.get("valorFuturo")).isEqualByComparingTo("12100.00");
            assertThat(res.get("vpl")).isEqualByComparingTo("0.00");
            assertThat(res.get("tir")).isEqualByComparingTo("10.00");
        }

        @Test
        void vplPositivoComFluxoConvencional() {
            AdmCalcRequestDTO request = admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, bd("10"), null,
                    List.of(bd("-1000"), bd("600"), bd("600")), "PERCENTUAL"
            );

            Map<String, BigDecimal> res = engine.calculate(request);

            // -1000 + 600/1.1 + 600/1.21 = 41.3223... -> 41.32
            assertThat(res.get("vpl")).isEqualByComparingTo("41.32");
            assertThat(res.get("tir")).isEqualByComparingTo("13.07");
        }

        @Test
        void modosEscalaPercentualDecimalEAutoFuncionamConformeContrato() {
            // PERCENTUAL: 1 -> 1%, 0.5 -> 0.5%, 20 -> 20%
            Map<String, BigDecimal> p1 = engine.calculate(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, bd("100"),
                    null, bd("1"), null, null, bd("1000"), null, bd("1"), bd("1"), null, "PERCENTUAL"));
            assertThat(p1.get("valorFuturo")).isEqualByComparingTo("1010.00");
            assertThat(p1.get("precoMarkup")).isEqualByComparingTo("101.01");

            Map<String, BigDecimal> p05 = engine.calculate(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, bd("100"),
                    null, bd("0.5"), null, null, bd("1000"), null, bd("0.5"), bd("1"), null, "PERCENTUAL"));
            assertThat(p05.get("valorFuturo")).isEqualByComparingTo("1005.00");
            assertThat(p05.get("precoMarkup")).isEqualByComparingTo("100.50");

            Map<String, BigDecimal> p20 = engine.calculate(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, bd("100"),
                    null, bd("20"), null, null, bd("1000"), null, bd("20"), bd("1"), null, "PERCENTUAL"));
            assertThat(p20.get("valorFuturo")).isEqualByComparingTo("1200.00");
            assertThat(p20.get("precoMarkup")).isEqualByComparingTo("125.00");
            assertThat(p20.get("markupMultiplicador")).isEqualByComparingTo("1.25");

            // DECIMAL: 0.20 -> 20%, 0.01 -> 1%
            Map<String, BigDecimal> d20 = engine.calculate(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, bd("100"),
                    null, bd("0.20"), null, null, bd("1000"), null, bd("0.20"), bd("1"), null, "DECIMAL"));
            assertThat(d20.get("valorFuturo")).isEqualByComparingTo("1200.00");
            assertThat(d20.get("precoMarkup")).isEqualByComparingTo("125.00");

            Map<String, BigDecimal> d01 = engine.calculate(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, bd("100"),
                    null, bd("0.01"), null, null, bd("1000"), null, bd("0.01"), bd("1"), null, "DECIMAL"));
            assertThat(d01.get("valorFuturo")).isEqualByComparingTo("1010.00");
            assertThat(d01.get("precoMarkup")).isEqualByComparingTo("101.01");

            // AUTO / legado 27 args: <= 1 tratado como decimal, > 1 dividido por 100
            AdmCalcRequestDTO legacyAutoSmall = new AdmCalcRequestDTO(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, bd("100"),
                    null, bd("0.20"), null, null, bd("1000"), null, bd("0.5"), bd("1"), null);
            Map<String, BigDecimal> autoSmall = engine.calculate(legacyAutoSmall);
            assertThat(autoSmall.get("valorFuturo")).isEqualByComparingTo("1500.00");
            assertThat(autoSmall.get("precoMarkup")).isEqualByComparingTo("125.00");

            Map<String, BigDecimal> autoLarge = engine.calculate(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, bd("100"),
                    null, bd("20"), null, null, bd("1000"), null, bd("20"), bd("1"), null, "AUTO"));
            assertThat(autoLarge.get("valorFuturo")).isEqualByComparingTo("1200.00");
            assertThat(autoLarge.get("precoMarkup")).isEqualByComparingTo("125.00");

            assertThatThrownBy(() -> engine.calculate(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, "ESCALA_DESCONHECIDA")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void guardsRejeitamNegativosDivisaoPorZeroOverflowCustoDerivadoPrecisaoENulos() {
            assertThatThrownBy(() -> engine.calculate(null))
                    .isInstanceOf(IllegalArgumentException.class);

            // Entradas negativas rejeitadas com IllegalArgumentException
            assertThatThrownBy(() -> engine.calculate(admCalcBase(
                    bd("-1"), bd("1"), null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> engine.calculate(admCalcBase(
                    null, null, bd("-10"), null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> engine.calculate(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, bd("100"), bd("200"), bd("5"), null,
                    null, null, null, null, null, null, null, null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> engine.calculate(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, bd("0"), null,
                    null, null, null, null, null, null, null, null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> engine.calculate(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    null, null, bd("100"), bd("0"), null, null, null, null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> engine.calculate(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, bd("100"),
                    null, bd("100"), null, null, null, null, null, null, null, "PERCENTUAL")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> engine.calculate(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, bd("100"), null, bd("-100"), bd("1"), null, "PERCENTUAL")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> engine.calculate(admCalcBase(
                    null, null, null, null, null, null, bd("1000"), null, bd("-5"),
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("margemContribuicao");

            // Divisao por zero protegida
            assertThatThrownBy(() -> engine.calculate(admCalcBase(
                    null, null, bd("0"), bd("0"), null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> engine.calculate(admCalcBase(
                    null, null, null, null, null, null, bd("1000"), null, bd("0"),
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> engine.calculate(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    bd("1000"), null, bd("0"), null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class);

            // Math.pow overflow (Infinity / NaN) tratado como IllegalArgumentException sem NumberFormatException
            assertThatThrownBy(() -> engine.calculate(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, bd("1000"), null, bd("100"), bd("5000"), null, "PERCENTUAL")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .isNotInstanceOf(NumberFormatException.class);

            // Encadeamento custoTotal -> custoUnitario -> precoMarkup quando custoTotal nao foi informado
            Map<String, BigDecimal> chained = engine.calculate(admCalcBase(
                    null, null, null, null, null, null, bd("1200"), bd("800"), null,
                    null, null, null, null, null, null, null, null, null,
                    bd("40"), bd("25"), null, null, null, null, null, null, null, "PERCENTUAL"));
            assertThat(chained.get("custoTotal")).isEqualByComparingTo("2000.00");
            assertThat(chained.get("custoUnitario")).isEqualByComparingTo("50.00");
            assertThat(chained.get("precoMarkup")).isEqualByComparingTo("2666.67");
            assertThat(chained.get("markupMultiplicador")).isEqualByComparingTo("1.33");

            // Lucro liquido NAO arredondado entra em margemLucroLiquida (1.665 / 3 * 100 = 55.50, nao 1.67 / 3 * 100 = 55.67)
            Map<String, BigDecimal> unroundedMargin = engine.calculate(admCalcBase(
                    null, null, bd("3"), bd("1"), bd("0.335"), bd("0"), null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null));
            assertThat(unroundedMargin.get("lucroLiquido")).isEqualByComparingTo("1.67");
            assertThat(unroundedMargin.get("margemLucroLiquida")).isEqualByComparingTo("55.50");

            // Elemento nulo em fluxosDeCaixa rejeitado
            assertThatThrownBy(() -> engine.calculate(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    Arrays.asList(bd("-100"), null, bd("200")), null)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> engine.warnings(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    Arrays.asList(bd("-100"), null, bd("200")), null)))
                    .isInstanceOf(IllegalArgumentException.class);

            // Fluxo sem mudanca de sinal rejeitado na TIR
            assertThatThrownBy(() -> engine.calculate(admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    List.of(bd("100"), bd("200"), bd("300")), null)))
                    .isInstanceOf(IllegalArgumentException.class);

            // Multiplas mudancas de sinal: Descartes warning emitido e calculo retorna raiz encontrada
            AdmCalcRequestDTO multiSign = admCalcBase(
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    List.of(bd("-100"), bd("230"), bd("-132"), bd("10")), null);
            List<String> warnings = engine.warnings(multiSign);
            assertThat(warnings).hasSize(1);
            assertThat(warnings.getFirst()).contains("Descartes");
            assertThat(engine.calculate(multiSign).get("tir")).isNotNull();
            assertThat(engine.warnings(emptyAdmCalc())).isEmpty();
        }
    }

    @Nested
    class LaborAndTaxEngineTests {

        private final LaborCalculationEngine labor = new LaborCalculationEngine();
        private final TaxCalculationEngine tax = new TaxCalculationEngine();

        private LaborCalculationRequestDTO baseLaborRequest(BigDecimal horasNoturnas, BigDecimal salarioBase) {
            return new LaborCalculationRequestDTO(
                    bd("3000"), bd("220"), bd("0.50"), horasNoturnas, bd("0"),
                    22, 4, bd("1412"), bd("0.20"), salarioBase,
                    bd("200"), 1, bd("62.04"), bd("0"), bd("0"),
                    12, 0, 0, bd("3000"),
                    List.of(new TaxBracketDTO(bd("1412"), bd("0.075"), bd("0")),
                            new TaxBracketDTO(bd("3000"), bd("0.09"), bd("0"))),
                    2, bd("5000"), bd("0.40"), bd("3000"), bd("1000"),
                    bd("6000"), 2, bd("1000"), bd("1.05"), bd("0.08")
            );
        }

        @Test
        void laborEngineUsaRazaoExata8Sobre7E4Sobre3ERejeitaEntradasInvalidas() {
            Map<String, BigDecimal> res7h = labor.calculate(baseLaborRequest(bd("7"), bd("3000")));
            // 7 * 8 / 7 = 8.0000 exato
            assertThat(res7h.get("fatorReducaoNoturna")).isEqualByComparingTo("8.0000");
            // 3000 * 4 / 3 = 4000.00 exato
            assertThat(res7h.get("feriasComTerco")).isEqualByComparingTo("4000.00");

            Map<String, BigDecimal> res1h = labor.calculate(baseLaborRequest(bd("1"), bd("1000")));
            // 1 * 8 / 7 = 1.142857... -> 1.1429
            assertThat(res1h.get("fatorReducaoNoturna")).isEqualByComparingTo("1.1429");
            // 1000 * 4 / 3 = 1333.3333... -> 1333.33
            assertThat(res1h.get("feriasComTerco")).isEqualByComparingTo("1333.33");

            assertThatThrownBy(() -> labor.calculate(null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> labor.calculate(baseLaborRequest(bd("-1"), bd("3000"))))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> labor.calculate(baseLaborRequest(bd("7"), bd("-100"))))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> labor.calculate(new LaborCalculationRequestDTO(
                    bd("0"), bd("220"), null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> labor.calculate(new LaborCalculationRequestDTO(
                    bd("3000"), bd("220"), null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    List.of(new TaxBracketDTO(bd("1000"), bd("-0.1"), bd("0"))),
                    null, null, null, null, null, null, null, null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void taxEngineCalculaFaixasIrrfFatorRLucroPresumidoERejeitaNegativos() {
            List<TaxBracketDTO> irrf = List.of(
                    new TaxBracketDTO(bd("2259.20"), bd("0"), bd("0")),
                    new TaxBracketDTO(bd("3751.05"), bd("0.15"), bd("381.44")),
                    new TaxBracketDTO(null, bd("0.275"), bd("896.00"))
            );

            // Faixa isenta
            Map<String, BigDecimal> isento = tax.calculate(new TaxCalculationRequestDTO(
                    bd("2000"), bd("150"), 0, bd("189.59"), bd("0"), irrf,
                    bd("10000"), bd("0"), bd("0"), bd("0"), bd("20000")
            ));
            assertThat(isento.get("baseIrrf")).isEqualByComparingTo("1850.00");
            assertThat(isento.get("irrf")).isEqualByComparingTo("0.00");
            assertThat(isento.get("fatorR")).isEqualByComparingTo("0.00");

            // Faixa intermediaria (base = 4000 - 400 - 189.59 - 100 = 3310.41 -> 3310.41*0.15 - 381.44 = 115.12)
            Map<String, BigDecimal> medio = tax.calculate(new TaxCalculationRequestDTO(
                    bd("4000"), bd("400"), 1, bd("189.59"), bd("100"), irrf,
                    bd("28000"), bd("2000"), bd("100000"), bd("100000"), bd("20000")
            ));
            assertThat(medio.get("baseIrrf")).isEqualByComparingTo("3310.41");
            assertThat(medio.get("irrf")).isEqualByComparingTo("115.12");
            assertThat(medio.get("fatorR")).isEqualByComparingTo("30.00");
            assertThat(medio.get("lucroPresumidoBaseIrpj")).isEqualByComparingTo("32000.00");
            assertThat(medio.get("lucroPresumidoIrpj")).isEqualByComparingTo("4800.00");
            assertThat(medio.get("lucroPresumidoCsll")).isEqualByComparingTo("2880.00");
            assertThat(medio.get("lucroPresumidoPisCofins")).isEqualByComparingTo("3650.00");
            assertThat(medio.get("lucroPresumidoAdicionalIrpj")).isEqualByComparingTo("1200.00");

            // Faixa maxima (limite == null)
            Map<String, BigDecimal> maximo = tax.calculate(new TaxCalculationRequestDTO(
                    bd("10000"), bd("900"), 0, bd("189.59"), bd("0"), irrf,
                    null, null, null, null, null
            ));
            assertThat(maximo.get("baseIrrf")).isEqualByComparingTo("9100.00");
            // 9100 * 0.275 - 896 = 2502.50 - 896 = 1606.50
            assertThat(maximo.get("irrf")).isEqualByComparingTo("1606.50");

            assertThatThrownBy(() -> tax.calculate(null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> tax.calculate(new TaxCalculationRequestDTO(
                    bd("-1"), bd("0"), 0, bd("0"), bd("0"), irrf,
                    null, null, null, null, null))).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> tax.calculate(new TaxCalculationRequestDTO(
                    bd("3000"), bd("0"), -1, bd("0"), bd("0"), irrf,
                    null, null, null, null, null))).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> tax.calculate(new TaxCalculationRequestDTO(
                    bd("3000"), bd("0"), 0, bd("0"), bd("0"),
                    Arrays.asList((TaxBracketDTO) null),
                    null, null, null, null, null))).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> tax.calculate(new TaxCalculationRequestDTO(
                    bd("3000"), bd("0"), 0, bd("0"), bd("0"),
                    List.of(new TaxBracketDTO(bd("2000"), bd("0.15"), bd("-10"))),
                    null, null, null, null, null))).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class FinancialDocumentWorkflowTests {

        @Mock
        private FinancialDigitalDocumentRepository documentRepository;
        @Mock
        private FinancialAuditTrailService auditTrailService;
        @Mock
        private FinancialDocumentDeliveryService deliveryService;

        private FinancialDocumentWorkflowService workflowService;
        private final AtomicLong seq = new AtomicLong(10L);

        @BeforeEach
        void setUp() {
            workflowService = new FinancialDocumentWorkflowService(
                    documentRepository,
                    auditTrailService,
                    deliveryService,
                    new FinancialDocumentStateMachine(),
                    new DataProtectionService()
            );
        }

        @Test
        void contadorNaoPodeAssinarComoAdvogadoEAuditorNaoPodeAssinar() {
            FinancialDigitalDocument pendingDoc = new FinancialDigitalDocument();
            pendingDoc.setIdFinancialDigitalDocument(55L);
            pendingDoc.setTipoDocumento("DRE_MENSAL");
            pendingDoc.setFuncionarioNome("Maria Contabil");
            pendingDoc.setFuncionarioEmail("maria@empresa.com");
            pendingDoc.setStatus("PENDING_SIGNATURE");
            pendingDoc.setCargoAssinanteObrigatorio("CONTADOR");
            pendingDoc.setConteudoHash("hash-conteudo-sigiloso");
            pendingDoc.setConteudo("Salario confidencial R$ 15.000,00");
            pendingDoc.setEmpresaId(1L);
            pendingDoc.setFilialId(1L);

            when(documentRepository.findByIdFinancialDigitalDocumentAndEmpresaIdAndFilialId(55L, 1L, 1L))
                    .thenReturn(Optional.of(pendingDoc));

            Authentication contador = authWithClaims("contador_lote1", "ROLE_CONTADOR", 1L, 1L);
            SignFinancialDocumentRequestDTO signAsAdvogado = new SignFinancialDocumentRequestDTO(
                    "Dr. Advogado", "ADVOGADO", "adv@empresa.com", "fp-1", true, "ok");

            assertThatThrownBy(() -> workflowService.sign(55L, signAsAdvogado, contador, 1L, 1L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("nao pertence ao usuario autenticado");

            Authentication auditor = authWithClaims("auditor_lote1", "ROLE_AUDITOR", 1L, 1L);
            SignFinancialDocumentRequestDTO signAsAuditor = new SignFinancialDocumentRequestDTO(
                    "Auditor", "AUDITOR", "aud@empresa.com", "fp-2", true, "ok");
            assertThatThrownBy(() -> workflowService.sign(55L, signAsAuditor, auditor, 1L, 1L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("nao possui cargo autorizado");

            SignFinancialDocumentRequestDTO signAsContadorFromAuditor = new SignFinancialDocumentRequestDTO(
                    "Auditor", "CONTADOR", "aud@empresa.com", "fp-3", true, "ok");
            assertThatThrownBy(() -> workflowService.sign(55L, signAsContadorFromAuditor, auditor, 1L, 1L))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void assinaturaPersisteLoginVerificadoEAuditoriaNaoExpoeConteudo() {
            when(documentRepository.save(any(FinancialDigitalDocument.class))).thenAnswer(inv -> {
                FinancialDigitalDocument doc = inv.getArgument(0);
                if (doc.getIdFinancialDigitalDocument() == null) {
                    doc.setIdFinancialDigitalDocument(seq.incrementAndGet());
                }
                return doc;
            });

            Authentication financeiro = authWithClaims("financeiro_real", "ROLE_FINANCEIRO", 7L, 8L);
            String segredoConteudo = "CONTEUDO_CONFIDENCIAL_SALARIO_98765_CPF_12345678900";
            FinancialDocumentDTO created = workflowService.create(
                    new FinancialDocumentRequestDTO(
                            "HOLERITE", "Joao Silva", "joao@empresa.com",
                            "CONTADOR", segredoConteudo, "REF-2026-10"),
                    financeiro, 7L, 8L
            );

            FinancialDigitalDocument stored = new FinancialDigitalDocument();
            stored.setIdFinancialDigitalDocument(created.idDocumento());
            stored.setTipoDocumento("HOLERITE");
            stored.setFuncionarioNome("Joao Silva");
            stored.setFuncionarioEmail("joao@empresa.com");
            stored.setStatus("PENDING_SIGNATURE");
            stored.setCargoAssinanteObrigatorio("CONTADOR");
            stored.setGeradoPor("financeiro_real");
            stored.setConteudo(segredoConteudo);
            stored.setConteudoHash("hash-abc");
            stored.setReferencia("REF-2026-10");
            stored.setEmpresaId(7L);
            stored.setFilialId(8L);

            when(documentRepository.findByIdFinancialDigitalDocumentAndEmpresaIdAndFilialId(
                    created.idDocumento(), 7L, 8L)).thenReturn(Optional.of(stored));

            Authentication contador = new UsernamePasswordAuthenticationToken(
                    "principal_generico", null, List.of(new SimpleGrantedAuthority("ROLE_CONTADOR")));
            ((UsernamePasswordAuthenticationToken) contador).setDetails(
                    new JwtTokenService.JwtClaims("contador_verificado", 99L, List.of("ROLE_CONTADOR"),
                            7L, 8L, Instant.now().plusSeconds(600).getEpochSecond()));

            FinancialDocumentDTO signed = workflowService.sign(
                    created.idDocumento(),
                    new SignFinancialDocumentRequestDTO(
                            "Nome Forjado no Payload", "CONTADOR", "contador@empresa.com",
                            "cert-fp-999", true, "Aprovado"),
                    contador, 7L, 8L
            );

            assertThat(signed.assinadoPor()).isEqualTo("contador_verificado");
            assertThat(signed.status()).isEqualTo("SIGNED");

            ArgumentCaptor<Object> metadataCaptor = ArgumentCaptor.forClass(Object.class);
            verify(auditTrailService).record(
                    eq("DOCUMENTO_ASSINADO"),
                    eq("financial_digital_document"),
                    eq(created.idDocumento()),
                    eq("contador_verificado"),
                    eq("PENDING_SIGNATURE"),
                    eq("SIGNED"),
                    metadataCaptor.capture(),
                    eq(7L),
                    eq(8L)
            );
            verify(auditTrailService).record(
                    eq("DOCUMENTO_GERADO"),
                    eq("financial_digital_document"),
                    eq(created.idDocumento()),
                    eq("financeiro_real"),
                    isNull(),
                    eq("DRAFT"),
                    metadataCaptor.capture(),
                    eq(7L),
                    eq(8L)
            );

            for (Object metaObj : metadataCaptor.getAllValues()) {
                assertThat(metaObj).isInstanceOf(Map.class);
                @SuppressWarnings("unchecked")
                Map<String, Object> meta = (Map<String, Object>) metaObj;
                assertThat(meta).doesNotContainKeys("conteudo", "mensagemEmail", "assuntoEmail");
                assertThat(meta.toString()).doesNotContain(segredoConteudo);
                assertThat(meta).containsKey("conteudoHash");
            }
        }

        private Authentication authWithClaims(String login, String role, long empresaId, long filialId) {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    login, null, List.of(new SimpleGrantedAuthority(role)));
            auth.setDetails(new JwtTokenService.JwtClaims(
                    login, 1L, List.of(role), empresaId, filialId,
                    Instant.now().plusSeconds(600).getEpochSecond()));
            return auth;
        }
    }

    @Nested
    @SpringBootTest(properties = {
            "spring.datasource.url=jdbc:h2:mem:apex_lote1_calc_envers;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.flyway.enabled=true",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
            "apex.jwt.secret=lote1-calc-envers-integration-secret-32-chars",
            "apex.jwt.issuer=apex-gestor-lote1",
            "apex.jwt.audience=apex-api",
            "apex.security.require-auth=true",
            "apex.seed.homologacao=false",
            "apex.license.enforcement.enabled=false"
    })
    @ActiveProfiles("local")
    class FinancialCalculationServiceAndEnversTests {

        @Autowired
        private FinancialCalculationService calculationService;
        @Autowired
        private FinancialCalculationRepository calculationRepository;
        @Autowired
        private FinancialAuditEventRepository auditEventRepository;
        @Autowired
        private EntityManagerFactory entityManagerFactory;
        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Test
        void preservaNumeros8MaisDigitosSemMascaraTelefoneEAuditaSnapshotLongoEmEnversSemTruncamento() {
            long empresaId = 701L;
            long filialId = 702L;

            AdmCalcRequestDTO largeRequest = admCalcBase(
                    bd("12345678"),      // 8 digitos: nao pode virar ***TELEFONE***
                    bd("10"),            // faturamento = 123456780.00 (9 digitos)
                    bd("98765432"),      // 8 digitos
                    bd("11111111"),      // lucroBruto = 87654321.00
                    bd("2222222"),       // despesas
                    bd("3333333"),       // impostos -> lucroLiquido = 82098766.00
                    bd("4444444"),       // custosFixos
                    bd("5555555"),       // custosVariaveis
                    bd("50000"),         // margemContribuicao unit
                    bd("87654321"),      // ativoCirculante
                    bd("123456789"),     // ativoNaoCirculante -> ativoTotal = 211111110.00
                    bd("23456789"),      // passivoCirculante
                    bd("34567890"),      // passivoNaoCirculante -> passivoTotal = 58024679.00
                    bd("12345678"),      // estoque
                    bd("50000000"),      // custoBem
                    bd("10000000"),      // valorResidual
                    bd("5"),             // vidaUtil
                    null,                // custoTotal derivado
                    bd("100"),           // quantidadeProduzida
                    bd("25"),            // margemDesejada
                    bd("18000000"),      // ganhoInvestimento
                    bd("12000000"),      // custoInvestimento
                    bd("10000000"),      // valorPresente
                    bd("12100000"),      // valorFuturo
                    bd("10"),            // taxaDesconto
                    bd("2"),             // periodo
                    List.of(bd("-10000000"), bd("5500000"), bd("6050000")),
                    "PERCENTUAL"
            );

            FinancialCalculationResponseDTO saved = calculationService.calculateAdmCalc(
                    largeRequest, "financeiro_envers", empresaId, filialId);

            FinancialCalculation entity = calculationRepository.findById(saved.calculationId()).orElseThrow();
            assertThat(entity.getResultadoSnapshot().length()).isGreaterThan(255);
            assertThat(entity.getInputSnapshot().length()).isGreaterThan(255);
            assertThat(entity.getResultadoSnapshot()).doesNotContain("***TELEFONE***");
            assertThat(entity.getResultadoSnapshot()).contains("123456780.00");

            // listRecent desserializa todos os numeros de 8+ digitos com fidelidade exata
            List<FinancialCalculationResponseDTO> recent = calculationService.listRecent(10, empresaId, filialId);
            FinancialCalculationResponseDTO reloaded = recent.stream()
                    .filter(r -> r.calculationId().equals(saved.calculationId()))
                    .findFirst()
                    .orElseThrow();

            assertThat(reloaded.alertas()).isEmpty();
            assertThat(reloaded.resultados().get("faturamento")).isEqualByComparingTo("123456780.00");
            assertThat(reloaded.resultados().get("lucroBruto")).isEqualByComparingTo("87654321.00");
            assertThat(reloaded.resultados().get("lucroLiquido")).isEqualByComparingTo("82098766.00");
            assertThat(reloaded.resultados().get("ativoTotal")).isEqualByComparingTo("211111110.00");
            assertThat(reloaded.resultados().get("passivoTotal")).isEqualByComparingTo("58024679.00");

            // Verifica tabela Envers financial_calculation_aud sem truncamento > 255 chars
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                List<Number> revisions = AuditReaderFactory.get(em)
                        .getRevisions(FinancialCalculation.class, saved.calculationId());
                assertThat(revisions).isNotEmpty();
            } finally {
                em.close();
            }

            String auditedResultSnapshot = jdbcTemplate.queryForObject(
                    "SELECT resultado_snapshot FROM financial_calculation_aud WHERE id_financial_calculation = ? ORDER BY REV DESC LIMIT 1",
                    String.class,
                    saved.calculationId()
            );
            String auditedInputSnapshot = jdbcTemplate.queryForObject(
                    "SELECT input_snapshot FROM financial_calculation_aud WHERE id_financial_calculation = ? ORDER BY REV DESC LIMIT 1",
                    String.class,
                    saved.calculationId()
            );
            assertThat(auditedResultSnapshot).hasSizeGreaterThan(255);
            assertThat(auditedResultSnapshot).isEqualTo(entity.getResultadoSnapshot());
            assertThat(auditedInputSnapshot).hasSizeGreaterThan(255);
        }

        @Test
        void auditoriaDeCalculosTrabalhistaETributarioNuncaExpoeValoresSalariaisNosMetadados() {
            long empresaId = 703L;
            long filialId = 704L;

            LaborCalculationRequestDTO laborReq = new LaborCalculationRequestDTO(
                    bd("19876.54"), bd("220"), bd("0.50"), bd("7"), bd("4321.99"),
                    22, 4, bd("1412"), bd("0.20"), bd("19876.54"),
                    bd("321.00"), 2, bd("62.04"), bd("543.21"), bd("234.56"),
                    12, 0, 0, bd("19876.54"),
                    List.of(new TaxBracketDTO(bd("3000"), bd("0.09"), bd("0"))),
                    3, bd("15432.10"), bd("0.40"), bd("19876.54"), bd("2500"),
                    bd("12000"), 2, bd("5000"), bd("1.05"), bd("0.08")
            );
            calculationService.calculateLabor(laborReq, "contador_sigilo", empresaId, filialId);

            TaxCalculationRequestDTO taxReq = new TaxCalculationRequestDTO(
                    bd("19876.54"), bd("908.85"), 2, bd("189.59"), bd("765.43"),
                    List.of(new TaxBracketDTO(null, bd("0.275"), bd("896.00"))),
                    bd("45678.90"), bd("7654.32"), bd("150000"), bd("150000"), bd("20000")
            );
            calculationService.calculateTaxes(taxReq, "contador_sigilo", empresaId, filialId);

            List<FinancialAuditEvent> events = auditEventRepository.findAll().stream()
                    .filter(e -> empresaId == (e.getEmpresaId() == null ? -1L : e.getEmpresaId()))
                    .toList();

            assertThat(events).hasSize(2);
            for (FinancialAuditEvent event : events) {
                assertThat(event.getTipoEvento()).isEqualTo("CALCULO_EXECUTADO");
                assertThat(event.getMetadados()).contains("\"sigiloDadosPessoais\":true");
                assertThat(event.getMetadados()).contains("\"indicadores\"");
                assertThat(event.getMetadados()).contains("\"inputHash\"");
                assertThat(event.getMetadados()).doesNotContain(
                        "19876.54", "4321.99", "15432.10", "45678.90", "7654.32", "765.43", "908.85"
                );
            }
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class AuditIpSpoofingAndMaskingTests {

        @Mock
        private AuditLogRepository auditLogRepository;
        @Mock
        private PrivacyGuardService privacyGuardService;
        @Spy
        private DataProtectionService dataProtectionService = new DataProtectionService();
        @InjectMocks
        private AuditTrailService auditTrailService;

        @Test
        void auditTrailServiceEForensicFilterIgnoramXForwardedForSpoofadoEMascaramPii() throws Exception {
            when(privacyGuardService.currentUserId(any())).thenReturn(42L);
            when(privacyGuardService.currentUserLogin(any())).thenReturn("contador_audit");
            when(privacyGuardService.currentEmpresaId(any())).thenReturn(1L);
            when(privacyGuardService.currentFilialId(any())).thenReturn(1L);
            when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.20.30.40");
            request.addHeader("X-Forwarded-For", "203.0.113.99");
            request.addHeader("User-Agent", "ApexTest/1.0");

            AuditLog saved = auditTrailService.registrarPrivacidade(
                    "RECLASSIFICACAO_MANUAL", "DESPESA", 99L, "Financeiro",
                    "planoContaId=null", "planoContaId=5", request,
                    "Ajuste CPF 123.456.789-00 contato joao.silva@empresa.com");

            assertThat(saved.getIp()).isEqualTo("10.20.30.40");
            assertThat(saved.getIp()).isNotEqualTo("203.0.113.99");
            assertThat(saved.getJustificativa()).doesNotContain("123.456.789-00");
            assertThat(saved.getJustificativa()).doesNotContain("joao.silva@empresa.com");
            assertThat(saved.getJustificativa()).contains("***DOCUMENTO***");
            assertThat(saved.getJustificativa()).contains("j***@empresa.com");

            ForensicAuditContextFilter filter = new ForensicAuditContextFilter();
            MockHttpServletResponse response = new MockHttpServletResponse();
            final AuditRevisionContext[] captured = new AuditRevisionContext[1];
            FilterChain chain = (req, res) -> captured[0] = AuditRevisionContextHolder.get();

            filter.doFilter(request, response, chain);

            assertThat(captured[0]).isNotNull();
            assertThat(captured[0].sourceIp()).isEqualTo("10.20.30.40");
            assertThat(captured[0].sourceIp()).isNotEqualTo("203.0.113.99");
            assertThat(AuditRevisionContextHolder.get()).isNull();
        }
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
