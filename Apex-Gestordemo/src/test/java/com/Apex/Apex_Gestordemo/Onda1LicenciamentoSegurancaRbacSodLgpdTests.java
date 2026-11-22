package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Model.AuditLog;
import Model.Cliente;
import Model.ContaReceber;
import Model.CreditoCliente;
import Model.CreditoStatus;
import Model.EmpresaPlanoLicenca;
import Model.FinanceiroStatus;
import Model.Perfil;
import Model.SolicitacaoTitularDados;
import Model.Usuario;
import Model.Venda;
import Model.VendaStatus;
import Repository.AuditLogRepository;
import Repository.ClienteRepository;
import Repository.ContaReceberRepository;
import Repository.CreditoClienteRepository;
import Repository.EmpresaPlanoLicencaRepository;
import Repository.PerfilRepository;
import Repository.PoliticaSegregacaoAprovacaoRepository;
import Repository.SolicitacaoTitularDadosRepository;
import Repository.UsuarioRepository;
import Repository.VendaRepository;
import Service.FinancialAuditTrailService;
import Service.JwtTokenService;
import Service.LicenseValidationPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:apex_onda1_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "apex.license.enforcement.enabled=false",
        "apex.license.grace-period-hours=72",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class Onda1LicenciamentoSegurancaRbacSodLgpdTests {

    private static final AtomicLong SEQ = new AtomicLong(91000L);

    @TestConfiguration
    static class LicenseValidationPortStubConfig {
        static final AtomicReference<LicenseValidationPort.RemoteLicenseCheckResult> RESULT =
                new AtomicReference<>(LicenseValidationPort.RemoteLicenseCheckResult.ok());

        @Bean
        LicenseValidationPort licenseValidationPort() {
            return (plano, feature) -> RESULT.get();
        }
    }

    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private EmpresaPlanoLicencaRepository planoRepository;
    @Autowired private PoliticaSegregacaoAprovacaoRepository politicaRepository;
    @Autowired private PerfilRepository perfilRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private VendaRepository vendaRepository;
    @Autowired private ContaReceberRepository contaReceberRepository;
    @Autowired private CreditoClienteRepository creditoClienteRepository;
    @Autowired private SolicitacaoTitularDadosRepository solicitacaoRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private FinancialAuditTrailService financialAuditTrailService;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void resetStub() {
        LicenseValidationPortStubConfig.RESULT.set(LicenseValidationPort.RemoteLicenseCheckResult.ok());
    }

    @Test
    void controlaAcessoSimultaneoPorPlanoExpiracaoSuspensaoGracePeriodEIsolamentoMultiTenant() throws Exception {
        long empresaEssential = nextId();
        long empresaPremium = nextId();
        long empresaSuspensa = nextId();

        criarPlano(empresaEssential, "t-" + empresaEssential, "ESSENTIAL", "BASIC_PRICING,FINANCIAL_CORE",
                "ATIVO", LocalDateTime.now().plusDays(30), 72, LocalDateTime.now().plusDays(33));
        criarPlano(empresaPremium, "t-" + empresaPremium, "PREMIUM", "BASIC_PRICING,FINANCIAL_CORE,ADVANCED_FINANCE",
                "ATIVO", LocalDateTime.now().plusDays(30), 72, LocalDateTime.now().plusDays(33));
        criarPlano(empresaSuspensa, "t-" + empresaSuspensa, "PREMIUM", "BASIC_PRICING,FINANCIAL_CORE,ADVANCED_FINANCE",
                "SUSPENSO", LocalDateTime.now().plusDays(30), 72, LocalDateTime.now().plusDays(33));

        String donoEssential = token(nextId(), "dono_e_" + empresaEssential, "ROLE_DONO", empresaEssential, 1L);
        String donoPremium = token(nextId(), "dono_p_" + empresaPremium, "ROLE_DONO_GERENTE", empresaPremium, 1L);
        String donoSuspenso = token(nextId(), "dono_s_" + empresaSuspensa, "ROLE_DONO", empresaSuspensa, 1L);

        // Tenant ESSENTIAL preserva Lote 1-4 (FINANCIAL_CORE) e recebe 403 FEATURE_NOT_LICENSED em ADVANCED_FINANCE
        mvc.perform(get("/api/financeiro/dre").header("Authorization", donoEssential))
                .andExpect(status().isOk());
        mvc.perform(get("/api/financeiro/fluxo-caixa").header("Authorization", donoEssential))
                .andExpect(status().isOk());
        mvc.perform(get("/api/financeiro/indicadores").header("Authorization", donoEssential))
                .andExpect(status().isOk());
        mvc.perform(get("/api/financeiro/governanca/politicas-sod").header("Authorization", donoEssential))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FEATURE_NOT_LICENSED"))
                .andExpect(jsonPath("$.feature").value("ADVANCED_FINANCE"));

        // Tenant PREMIUM com ADVANCED_FINANCE ativo acessa governanca normalmente e consulta entitlement proprio
        mvc.perform(get("/api/financeiro/governanca/politicas-sod").header("Authorization", donoPremium))
                .andExpect(status().isOk());
        mvc.perform(get("/api/licenses/entitlement").header("Authorization", donoPremium))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresaId").value(empresaPremium))
                .andExpect(jsonPath("$.subscriptionTier").value("PREMIUM"));

        // Tenant SUSPENSO e bloqueado imediatamente sem contingencia
        mvc.perform(get("/api/financeiro/governanca/politicas-sod").header("Authorization", donoSuspenso))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FEATURE_NOT_LICENSED"))
                .andExpect(jsonPath("$.licenseStatus").value("SUSPENSO"));

        // Falha transiente dentro da janela de grace period (72h) libera com trilha auditada
        LicenseValidationPortStubConfig.RESULT.set(
                LicenseValidationPort.RemoteLicenseCheckResult.transientError("Timeout no servidor central"));
        mvc.perform(get("/api/financeiro/governanca/politicas-sod").header("Authorization", donoPremium))
                .andExpect(status().isOk());
        List<AuditLog> logsContingencia = auditLogRepository.findByEmpresaIdAndFilialIdOrderByCriadoEmDesc(empresaPremium, 1L);
        assertThat(logsContingencia.stream().anyMatch(l -> "CONTINGENCIA_LICENCA_ATIVA".equals(l.getAcao()))).isTrue();

        // Apos esgotar janela de grace period, bloqueia estritamente
        EmpresaPlanoLicenca planoPremium = planoRepository.findByEmpresaId(empresaPremium).orElseThrow();
        planoPremium.setContingenciaAte(LocalDateTime.now().minusMinutes(5));
        planoPremium = planoRepository.save(planoPremium);
        mvc.perform(get("/api/financeiro/governanca/politicas-sod").header("Authorization", donoPremium))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FEATURE_NOT_LICENSED"))
                .andExpect(jsonPath("$.licenseStatus").value("GRACE_PERIOD_EXPIRED"));

        // Validacao expirada sem falha transiente bloqueia mesmo antes de expirar contingencia
        LicenseValidationPortStubConfig.RESULT.set(LicenseValidationPort.RemoteLicenseCheckResult.ok());
        planoPremium.setValidoAte(LocalDateTime.now().minusDays(1));
        planoPremium.setContingenciaAte(LocalDateTime.now().plusHours(48));
        planoRepository.save(planoPremium);
        mvc.perform(get("/api/financeiro/governanca/politicas-sod").header("Authorization", donoPremium))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FEATURE_NOT_LICENSED"))
                .andExpect(jsonPath("$.licenseStatus").value("EXPIRADO"));
    }

    @Test
    void contemAdvogadoAdminMasterAdminEGerenteProtegendoPiiEAuditoria() throws Exception {
        long empresaId = nextId();
        long filialId = 1L;
        criarPlano(empresaId, "t-" + empresaId, "PREMIUM", "BASIC_PRICING,FINANCIAL_CORE,ADVANCED_FINANCE",
                "ATIVO", LocalDateTime.now().plusDays(30), 72, LocalDateTime.now().plusDays(33));

        Cliente cliente = criarCliente(empresaId, filialId, "Maria da Silva", docUnico(), "maria" + empresaId + "@apex.local");

        AuditLog logSensivel = new AuditLog();
        logSensivel.setEmpresaId(empresaId);
        logSensivel.setFilialId(filialId);
        logSensivel.setUsuarioId(900L);
        logSensivel.setUsuarioLogin("operador.sigiloso");
        logSensivel.setModulo("Clientes");
        logSensivel.setAcao("CLIENTE_CONSULTA");
        logSensivel.setEntidade("Cliente");
        logSensivel.setEntidadeId(cliente.getIdCliente());
        logSensivel.setIp("192.168.77.88");
        logSensivel.setDispositivo("Mozilla/5.0");
        logSensivel.setJustificativa("Auditoria de operacao regular");
        logSensivel.setCriadoEm(LocalDateTime.now());
        auditLogRepository.save(logSensivel);

        String advogado = token(nextId(), "dr_adv_" + empresaId, "ROLE_ADVOGADO", empresaId, filialId);
        String adminMaster = token(nextId(), "master_" + empresaId, "ROLE_ADMIN_MASTER", empresaId, filialId);
        String admin = token(nextId(), "admin_" + empresaId, "ROLE_ADMIN", empresaId, filialId);
        String gerente = token(nextId(), "gerente_" + empresaId, "ROLE_GERENTE", empresaId, filialId);
        String dono = token(nextId(), "dono_" + empresaId, "ROLE_DONO", empresaId, filialId);
        String sysadmin = token(nextId(), "sys_" + empresaId, "ROLE_SYSADMIN", empresaId, filialId);

        // ADVOGADO limitado exclusivamente ao escopo de compliance (/documentos/**)
        mvc.perform(get("/api/financeiro/documentos").header("Authorization", advogado))
                .andExpect(status().isOk());
        for (String blockedRoute : List.of(
                "/api/financeiro/dre",
                "/api/financeiro/fluxo-caixa",
                "/api/financeiro/indicadores",
                "/api/financeiro/plano-contas",
                "/api/financeiro/centros-custo",
                "/api/financeiro/operacional/contas-receber",
                "/api/financeiro/calculos",
                "/api/auditoria")) {
            mvc.perform(get(blockedRoute).header("Authorization", advogado))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/privacy/admin/clientes/{id}/export", cliente.getIdCliente())
                        .param("justificativa", "Tentativa indevida de advogado")
                        .header("Authorization", advogado))
                .andExpect(status().isForbidden());

        // ADMIN_MASTER nao consulta PII cadastral nem exporta/anonimiza; ve auditoria mascarada
        mvc.perform(get("/api/clientes").header("Authorization", adminMaster))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/privacy/admin/clientes/{id}/export", cliente.getIdCliente())
                        .param("justificativa", "Tentativa indevida de admin master")
                        .header("Authorization", adminMaster))
                .andExpect(status().isForbidden());

        for (String maskedViewer : List.of(adminMaster, admin, gerente)) {
            mvc.perform(get("/api/auditoria").header("Authorization", maskedViewer))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].usuarioLogin").value("op***o"))
                    .andExpect(jsonPath("$[0].ip").value("192.168.*.*"))
                    .andExpect(jsonPath("$[0].dispositivo").value("Protegido (PII)"));

            mvc.perform(get("/api/privacy/admin/clientes/{id}/export", cliente.getIdCliente())
                            .param("justificativa", "Tentativa bloqueada de exportacao")
                            .header("Authorization", maskedViewer))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/api/privacy/clientes/{id}", cliente.getIdCliente())
                            .param("confirmadoExplicito", "true")
                            .param("justificativa", "Tentativa bloqueada")
                            .header("Authorization", maskedViewer))
                    .andExpect(status().isForbidden());
        }

        // DONO visualiza auditoria completa sem mascaramento, mas apenas SYSADMIN altera plano via sync-plan
        mvc.perform(get("/api/auditoria").header("Authorization", dono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioLogin").value("operador.sigiloso"))
                .andExpect(jsonPath("$[0].ip").value("192.168.77.88"));

        mvc.perform(post("/api/licenses/entitlement/sync-plan")
                        .header("Authorization", dono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empresaId\":" + empresaId + ",\"subscriptionTier\":\"ESSENTIAL\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/licenses/entitlement/sync-plan")
                        .header("Authorization", sysadmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empresaId\":" + empresaId + ",\"subscriptionTier\":\"GROWTH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionTier").value("GROWTH"));
    }

    @Test
    void aplicaSegregacaoDeFuncoesEAlcadaComControleDeExcecaoUnipessoal() throws Exception {
        long empresaId = nextId();
        long filialId = 1L;
        criarPlano(empresaId, "t-" + empresaId, "PREMIUM", "BASIC_PRICING,FINANCIAL_CORE,ADVANCED_FINANCE",
                "ATIVO", LocalDateTime.now().plusDays(30), 72, LocalDateTime.now().plusDays(33));

        Usuario donoA = criarUsuario("dono_a_" + empresaId, "DONO", empresaId, filialId);
        Usuario donoB = criarUsuario("dono_b_" + empresaId, "DONO_GERENTE", empresaId, filialId);
        Usuario gerente = criarUsuario("gerente_" + empresaId, "GERENTE", empresaId, filialId);

        String tokDonoA = token(donoA.getIdUsuario(), donoA.getLogin(), "ROLE_DONO", empresaId, filialId);
        String tokDonoB = token(donoB.getIdUsuario(), donoB.getLogin(), "ROLE_DONO_GERENTE", empresaId, filialId);
        String tokGerente = token(gerente.getIdUsuario(), gerente.getLogin(), "ROLE_GERENTE", empresaId, filialId);

        // Gerente nao pode enfraquecer politicas SoD
        mvc.perform(put("/api/financeiro/governanca/politicas-sod")
                        .header("Authorization", tokGerente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "dominioOperacao", "ORCAMENTO_APROVACAO",
                                "exigirAprovadorDistinto", false,
                                "permitirExcecaoUnipessoal", true))))
                .andExpect(status().isForbidden());

        // DONO configura politica padrao com alcada gerencial de 5.000,00
        mvc.perform(put("/api/financeiro/governanca/politicas-sod")
                        .header("Authorization", tokDonoA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "dominioOperacao", "ORCAMENTO_APROVACAO",
                                "exigirAprovadorDistinto", true,
                                "permitirExcecaoUnipessoal", true,
                                "exigirJustificativa", true,
                                "alcadaGerenteMaximo", new BigDecimal("5000.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alcadaGerenteMaximo").value(5000.0));

        // Autoaprovacao bloqueada mesmo com unipessoal=true pois empresa tem >= 2 aprovadores ativos
        mvc.perform(post("/api/financeiro/governanca/validacao-aprovacao")
                        .header("Authorization", tokDonoA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "dominioOperacao", "ORCAMENTO_APROVACAO",
                                "submetidoPorUsuarioId", donoA.getIdUsuario(),
                                "valorOperacao", new BigDecimal("2500.00"),
                                "justificativa", "Tentativa de autoaprovacao com multiplos aprovadores"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SOD_VIOLATION"));

        // Gerente acima da alcada (6.000 > 5.000) e bloqueado; abaixo da alcada (3.000) aprova
        mvc.perform(post("/api/financeiro/governanca/validacao-aprovacao")
                        .header("Authorization", tokGerente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "dominioOperacao", "ORCAMENTO_APROVACAO",
                                "submetidoPorUsuarioId", donoA.getIdUsuario(),
                                "valorOperacao", new BigDecimal("6000.00"),
                                "justificativa", "Orcamento acima da alcada gerencial configurada"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SOD_VIOLATION"));

        mvc.perform(post("/api/financeiro/governanca/validacao-aprovacao")
                        .header("Authorization", tokGerente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "dominioOperacao", "ORCAMENTO_APROVACAO",
                                "submetidoPorUsuarioId", donoA.getIdUsuario(),
                                "valorOperacao", new BigDecimal("3000.00"),
                                "justificativa", "Orcamento operacional dentro da alcada gerencial"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autorizado").value(true));

        // Segundo aprovador (DONO_GERENTE) aprova acima de 5.000 e respeita SoD
        mvc.perform(post("/api/financeiro/governanca/validacao-aprovacao")
                        .header("Authorization", tokDonoB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "dominioOperacao", "ORCAMENTO_APROVACAO",
                                "submetidoPorUsuarioId", donoA.getIdUsuario(),
                                "valorOperacao", new BigDecimal("8500.00"),
                                "justificativa", "Aprovacao estrategica por segundo socio elegivel"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autorizado").value(true))
                .andExpect(jsonPath("$.autoaprovacaoUnipessoal").value(false));

        // Cenario unipessoal real (apenas 1 aprovador ativo): exige justificativa formal e permite excecao explicita
        donoB.setStatus(0);
        gerente.setStatus(0);
        usuarioRepository.saveAll(List.of(donoB, gerente));

        mvc.perform(post("/api/financeiro/governanca/validacao-aprovacao")
                        .header("Authorization", tokDonoA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "dominioOperacao", "ORCAMENTO_APROVACAO",
                                "submetidoPorUsuarioId", donoA.getIdUsuario(),
                                "valorOperacao", new BigDecimal("9000.00"),
                                "justificativa", "Curta"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SOD_VIOLATION"));

        mvc.perform(post("/api/financeiro/governanca/validacao-aprovacao")
                        .header("Authorization", tokDonoA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "dominioOperacao", "ORCAMENTO_APROVACAO",
                                "submetidoPorUsuarioId", donoA.getIdUsuario(),
                                "valorOperacao", new BigDecimal("9000.00"),
                                "justificativa", "Operacao unipessoal formalmente justificada pelo proprietario unico"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autorizado").value(true))
                .andExpect(jsonPath("$.autoaprovacaoUnipessoal").value(true));
    }

    @Test
    void exigeJustificativaRetencaoSimulacaoConfirmacaoEDuplaAprovacaoNaLgpd() throws Exception {
        long empresaId = nextId();
        long filialId = 1L;
        criarPlano(empresaId, "t-" + empresaId, "ESSENTIAL", "BASIC_PRICING,FINANCIAL_CORE",
                "ATIVO", LocalDateTime.now().plusDays(30), 72, LocalDateTime.now().plusDays(33));

        Usuario donoA = criarUsuario("dono_a_" + empresaId, "DONO", empresaId, filialId);
        Usuario donoB = criarUsuario("dono_b_" + empresaId, "DONO_GERENTE", empresaId, filialId);
        Cliente cliPendencia = criarCliente(empresaId, filialId, "Carlos Pendencia", docUnico(), "c" + empresaId + "@apex.local");
        String docAna = docUnico();
        Cliente cliComHistorico = criarCliente(empresaId, filialId, "Ana Quitada", docAna, "a" + empresaId + "@apex.local");

        // Vincula conta a receber em aberto e credito ativo ao cliente com pendencia
        ContaReceber cr = new ContaReceber();
        cr.setClienteId(cliPendencia.getIdCliente());
        cr.setEmpresaId(empresaId);
        cr.setFilialId(filialId);
        cr.setValorBruto(new BigDecimal("200.00"));
        cr.setDesconto(BigDecimal.ZERO);
        cr.setValorLiquido(new BigDecimal("200.00"));
        cr.setValorPago(BigDecimal.ZERO);
        cr.setSaldo(new BigDecimal("200.00"));
        cr.setVencimento(LocalDate.now().plusDays(5));
        cr.setStatus(FinanceiroStatus.ABERTO);
        cr.setCriadoEm(LocalDateTime.now());
        contaReceberRepository.save(cr);

        CreditoCliente credito = new CreditoCliente();
        credito.setClienteId(cliPendencia.getIdCliente());
        credito.setVendaOrigemId(1L);
        credito.setDevolucaoId(1L);
        credito.setEmpresaId(empresaId);
        credito.setFilialId(filialId);
        credito.setValorOriginal(new BigDecimal("50.00"));
        credito.setSaldo(new BigDecimal("50.00"));
        credito.setStatus(CreditoStatus.DISPONIVEL);
        credito.setCriadoEm(LocalDateTime.now());
        credito.setVersion(0L);
        creditoClienteRepository.save(credito);

        // Vincula historico fiscal de venda ao segundo cliente (sem pendencias em aberto)
        Venda venda = new Venda();
        venda.setUsuario(donoA);
        venda.setCliente(cliComHistorico);
        venda.setStatus(1);
        venda.setStatusOperacional(VendaStatus.FINALIZADA);
        venda.setValorTotal(new BigDecimal("350.00"));
        venda.setDesconto(BigDecimal.ZERO);
        venda.setEmpresaId(empresaId);
        venda.setFilialId(filialId);
        venda.setEstoqueMovimentado(false);
        venda.setVersion(0L);
        venda.setDataVenda(LocalDateTime.now().minusDays(10));
        vendaRepository.save(venda);

        String tokDonoA = token(donoA.getIdUsuario(), donoA.getLogin(), "ROLE_DONO", empresaId, filialId);
        String tokDonoB = token(donoB.getIdUsuario(), donoB.getLogin(), "ROLE_DONO_GERENTE", empresaId, filialId);

        // Exportacao sem justificativa falha (400); com justificativa sucede e grava solicitacao + audit
        mvc.perform(get("/api/privacy/admin/clientes/{id}/export", cliComHistorico.getIdCliente())
                        .header("Authorization", tokDonoA))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/privacy/admin/clientes/{id}/export", cliComHistorico.getIdCliente())
                        .param("justificativa", "Atendimento a requerimento formal do titular art. 18 LGPD")
                        .header("Authorization", tokDonoA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpfCnpj").value(docAna))
                .andExpect(jsonPath("$.solicitacaoId").isNumber());

        // Simulacao do cliente com pendencia retorna bloqueio operacional e tentativa de anonimizacao retorna 409 Conflict
        String simPendJson = mvc.perform(get("/api/privacy/admin/clientes/{id}/anonymize/simulate", cliPendencia.getIdCliente())
                        .header("Authorization", tokDonoA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elegivelAnonimizacaoPessoal").value(false))
                .andExpect(jsonPath("$.bloqueioOperacionalAtivo").value(true))
                .andExpect(jsonPath("$.contasReceberEmAberto").value(1))
                .andExpect(jsonPath("$.creditosAtivos").value(1))
                .andReturn().getResponse().getContentAsString();
        String hashPendencia = json.readTree(simPendJson).get("simulacaoHash").asText();

        mvc.perform(post("/api/privacy/admin/clientes/{id}/anonymize", cliPendencia.getIdCliente())
                        .header("Authorization", tokDonoB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "confirmadoExplicito", true,
                                "justificativa", "Tentativa bloqueada por titulo em aberto",
                                "simulacaoHash", hashPendencia))))
                .andExpect(status().isConflict());

        // DELETE legado sem confirmacao/hash e bloqueado
        mvc.perform(delete("/api/privacy/clientes/{id}", cliComHistorico.getIdCliente())
                        .header("Authorization", tokDonoB))
                .andExpect(status().isBadRequest());

        // Simulacao do cliente com historico fiscal quitado
        String simOkJson = mvc.perform(get("/api/privacy/admin/clientes/{id}/anonymize/simulate", cliComHistorico.getIdCliente())
                        .header("Authorization", tokDonoA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elegivelAnonimizacaoPessoal").value(true))
                .andExpect(jsonPath("$.bloqueioOperacionalAtivo").value(false))
                .andExpect(jsonPath("$.bloqueioExclusaoFisicaPorRetencaoLegal").value(true))
                .andExpect(jsonPath("$.vendasVinculadas").value(1))
                .andExpect(jsonPath("$.exigeAprovacaoSegundoResponsavel").value(true))
                .andReturn().getResponse().getContentAsString();
        JsonNode simOk = json.readTree(simOkJson);
        String hashOk = simOk.get("simulacaoHash").asText();

        // Solicitacao aberta pelo DONO A; tentar confirmar pelo proprio DONO A viola SoD (403)
        String reqBody = mvc.perform(post("/api/privacy/admin/requests")
                        .header("Authorization", tokDonoA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "titularId", String.valueOf(cliComHistorico.getIdCliente()),
                                "tipoTitular", "CLIENTE",
                                "tipo", "ANONIMIZACAO",
                                "justificativa", "Requerimento formal de anonimizacao apos quitacao total"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long solicitacaoId = json.readTree(reqBody).get("requestId").asLong();

        mvc.perform(post("/api/privacy/admin/clientes/{id}/anonymize", cliComHistorico.getIdCliente())
                        .header("Authorization", tokDonoA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "confirmadoExplicito", true,
                                "justificativa", "Autoaprovacao do solicitante deve ser recusada",
                                "simulacaoHash", hashOk,
                                "solicitacaoId", solicitacaoId))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SOD_VIOLATION"));

        // Confirmacao por segundo proprietario (DONO_GERENTE) conclui anonimizacao mantendo retencao fiscal
        mvc.perform(post("/api/privacy/admin/clientes/{id}/anonymize", cliComHistorico.getIdCliente())
                        .header("Authorization", tokDonoB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "confirmadoExplicito", true,
                                "justificativa", "Anonimizacao aprovada em dupla conferencia conforme LGPD",
                                "simulacaoHash", hashOk,
                                "solicitacaoId", solicitacaoId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTADA"));

        Cliente anonimizado = clienteRepository.findById(cliComHistorico.getIdCliente()).orElseThrow();
        assertThat(anonimizado.getNomeRazao()).startsWith("Titular anonimizado");
        assertThat(anonimizado.getEmail()).isNull();
        assertThat(anonimizado.getCelular()).isNull();
        assertThat(anonimizado.getCpfCnpj()).startsWith("ANON");
        assertThat(anonimizado.getBaseLegal()).contains("LGPD");
        assertThat(anonimizado.getAnonimizadoEm()).isNotNull();

        SolicitacaoTitularDados solExec = solicitacaoRepository.findById(solicitacaoId).orElseThrow();
        assertThat(solExec.isConfirmadoExplicito()).isTrue();
        assertThat(solExec.isRetencaoLegalValidada()).isTrue();
        assertThat(solExec.getAprovadoPorUsuarioId()).isEqualTo(donoB.getIdUsuario());
        assertThat(solExec.getSolicitadoPorUsuarioId()).isEqualTo(donoA.getIdUsuario());
    }

    @Test
    void bloqueiaEscalacaoAdminMasterLimitaGracePeriodEResolveUnipessoalEssentialEMascaraAuditoriaFinanceira() throws Exception {
        long empresaId = nextId();
        long filialId = 1L;
        criarPlano(empresaId, "t-" + empresaId, "ESSENTIAL", "BASIC_PRICING,FINANCIAL_CORE",
                "ATIVO", LocalDateTime.now().plusDays(365), 72, LocalDateTime.now().plusDays(368));

        // Apenas 1 DONO ativo no tenant ESSENTIAL: simulação e anonimizacao unipessoal devem funcionar sem deadlock
        Usuario donoUnico = criarUsuario("dono_unico_" + empresaId, "DONO", empresaId, filialId);
        Cliente cliUnipessoal = criarCliente(empresaId, filialId, "Titular Unipessoal", docUnico(), "u" + empresaId + "@apex.local");
        String tokDonoUnico = token(donoUnico.getIdUsuario(), donoUnico.getLogin(), "ROLE_DONO", empresaId, filialId);

        String simRes = mvc.perform(get("/api/privacy/admin/clientes/{id}/anonymize/simulate", cliUnipessoal.getIdCliente())
                        .header("Authorization", tokDonoUnico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exigeAprovacaoSegundoResponsavel").value(false))
                .andExpect(jsonPath("$.elegivelAnonimizacaoPessoal").value(true))
                .andReturn().getResponse().getContentAsString();
        String hashSim = json.readTree(simRes).get("simulacaoHash").asText();

        // Nao permite reutilizar solicitacaoId de EXPORTACAO na confirmacao de anonimizacao (MAJOR #8)
        String exportJson = mvc.perform(get("/api/privacy/admin/clientes/{id}/export", cliUnipessoal.getIdCliente())
                        .param("justificativa", "Exportacao antes de verificar reutilizacao indevida")
                        .header("Authorization", tokDonoUnico))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long exportSolId = json.readTree(exportJson).get("solicitacaoId").asLong();

        mvc.perform(post("/api/privacy/admin/clientes/{id}/anonymize", cliUnipessoal.getIdCliente())
                        .header("Authorization", tokDonoUnico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "confirmadoExplicito", true,
                                "justificativa", "Tentativa de reusar solicitacao de exportacao na anonimizacao",
                                "simulacaoHash", hashSim,
                                "solicitacaoId", exportSolId))))
                .andExpect(status().isBadRequest());

        // Execucao unipessoal valida no ESSENTIAL sem solicitacaoId de exportacao
        mvc.perform(post("/api/privacy/admin/clientes/{id}/anonymize", cliUnipessoal.getIdCliente())
                        .header("Authorization", tokDonoUnico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "confirmadoExplicito", true,
                                "justificativa", "Anonimizacao unipessoal legitima no tenant Essential",
                                "simulacaoHash", hashSim))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTADA"));

        // ADMIN_MASTER nao pode criar ou promover usuario para DONO/SYSADMIN nem alterar perfil DONO (BLOCKER #2)
        // E auditoria ESCALACAO_PRIVILEGIO_BLOQUEADA persiste mesmo com rollback HTTP 403 (MAJOR #6)
        Perfil perfilDono = garantirPerfil("DONO");
        Perfil perfilGerente = garantirPerfil("GERENTE");
        Usuario masterUser = criarUsuario("master_" + empresaId, "ADMIN_MASTER", empresaId, filialId);
        String tokMaster = token(masterUser.getIdUsuario(), masterUser.getLogin(), "ROLE_ADMIN_MASTER", empresaId, filialId);

        mvc.perform(post("/api/usuarios")
                        .header("Authorization", tokMaster)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "nome", "Escalador",
                                "login", "escalador_" + empresaId,
                                "senha", "Apex@2026Seguro",
                                "perfil", Map.of("idPerfil", perfilDono.getIdPerfil())))))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/usuarios/{id}", masterUser.getIdUsuario())
                        .header("Authorization", tokMaster)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "nome", "Master Promovido",
                                "login", masterUser.getLogin(),
                                "senha", "Apex@2026Seguro",
                                "perfil", Map.of("idPerfil", perfilDono.getIdPerfil())))))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/usuarios/{id}/desativar", donoUnico.getIdUsuario())
                        .header("Authorization", tokMaster))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/perfis/{id}", perfilDono.getIdPerfil())
                        .header("Authorization", tokMaster)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"DONO\",\"status\":1}"))
                .andExpect(status().isForbidden());

        List<AuditLog> escalacaoLogs = auditLogRepository.findByEmpresaIdAndFilialIdOrderByCriadoEmDesc(empresaId, filialId);
        assertThat(escalacaoLogs.stream().anyMatch(l -> "ESCALACAO_PRIVILEGIO_BLOQUEADA".equals(l.getAcao()))).isTrue();

        // Garante que ADMIN_MASTER consegue gerenciar usuario operacional comum (ex: GERENTE)
        mvc.perform(post("/api/usuarios")
                        .header("Authorization", tokMaster)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "nome", "Gerente Comum",
                                "login", "gerente_ok_" + empresaId,
                                "senha", "Apex@2026Seguro",
                                "perfil", Map.of("idPerfil", perfilGerente.getIdPerfil())))))
                .andExpect(status().isCreated());

        // Vetor A (Rodada 2): Nomes variantes com acento, hifen ou espaco nao contornam validatePerfilAssignment
        for (String variantName : List.of("DONO-GERENTE", "DONO GERENTE", "SYSÀDMIN", "SYS_ADMIN", "ADMIN MASTER")) {
            mvc.perform(post("/api/perfis")
                            .header("Authorization", tokMaster)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("nome", variantName, "status", 1))))
                    .andExpect(status().isForbidden());
        }

        // Vetor B (Rodada 2): POST /api/usuarios com perfil omitido nunca cai em SYSADMIN (id=1), mesmo com SYSADMIN no banco
        garantirPerfil("SYSADMIN");
        String userSemPerfilBody = mvc.perform(post("/api/usuarios")
                        .header("Authorization", tokMaster)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "nome", "Usuario Default",
                                "login", "user_default_" + empresaId,
                                "senha", "Apex@2026Seguro"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(userSemPerfilBody).get("perfil").get("nome").asText()).isEqualTo("VENDEDOR");

        // Grace period limitado a 72h desde ultimaValidacaoEm mesmo em plano com validoAte de 365 dias (MAJOR #4)
        long empGrace = nextId();
        criarPlano(empGrace, "t-" + empGrace, "PREMIUM", "BASIC_PRICING,FINANCIAL_CORE,ADVANCED_FINANCE",
                "ATIVO", LocalDateTime.now().plusDays(365), 72, LocalDateTime.now().plusDays(368));
        EmpresaPlanoLicenca planoAnual = planoRepository.findByEmpresaId(empGrace).orElseThrow();
        planoAnual.setUltimaValidacaoEm(LocalDateTime.now().minusHours(80));
        planoRepository.save(planoAnual);
        LicenseValidationPortStubConfig.RESULT.set(
                LicenseValidationPort.RemoteLicenseCheckResult.transientError("Queda de rede simulada"));
        String tokGrace = token(nextId(), "dono_g_" + empGrace, "ROLE_DONO", empGrace, filialId);
        mvc.perform(get("/api/financeiro/governanca/politicas-sod").header("Authorization", tokGrace))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FEATURE_NOT_LICENSED"))
                .andExpect(jsonPath("$.licenseStatus").value("GRACE_PERIOD_EXPIRED"));

        // GET /api/financeiro/auditoria mascara atorLogin para GERENTE e mantem completo para DONO (MAJOR #7)
        financialAuditTrailService.record("DOC_TEST", "FinancialDocument", 1L, "analista.sigiloso",
                null, "status=SIGNED", Map.of("ok", true), empresaId, filialId);
        String tokGerente = token(nextId(), "gerente_aud_" + empresaId, "ROLE_GERENTE", empresaId, filialId);
        mvc.perform(get("/api/financeiro/auditoria").header("Authorization", tokGerente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].atorLogin").value("an***o"));
        mvc.perform(get("/api/financeiro/auditoria").header("Authorization", tokDonoUnico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].atorLogin").value("analista.sigiloso"));
    }

    private long nextId() {
        return SEQ.incrementAndGet();
    }

    private String docUnico() {
        return String.format("%011d", nextId());
    }

    private void criarPlano(
            long empresaId,
            String tenantCode,
            String tier,
            String featuresCsv,
            String status,
            LocalDateTime validoAte,
            int graceHours,
            LocalDateTime contingenciaAte) {
        EmpresaPlanoLicenca plano = new EmpresaPlanoLicenca();
        plano.setEmpresaId(empresaId);
        plano.setTenantCode(tenantCode);
        plano.setTenantName("Tenant " + empresaId);
        plano.setSubscriptionTier(tier);
        plano.setFeaturesHabilitadas(featuresCsv);
        plano.setStatus(status);
        plano.setValidoAte(validoAte);
        plano.setUltimaValidacaoEm(LocalDateTime.now());
        plano.setGracePeriodHoras(graceHours);
        plano.setContingenciaAte(contingenciaAte);
        plano.setAtualizadoPor("TESTE");
        planoRepository.save(plano);
    }

    private Perfil garantirPerfil(String perfilNome) {
        return perfilRepository.findByNome(perfilNome).orElseGet(() -> {
            Perfil p = new Perfil();
            p.setNome(perfilNome);
            p.setStatus(1);
            return perfilRepository.save(p);
        });
    }

    private Usuario criarUsuario(String login, String perfilNome, long empresaId, long filialId) {
        Perfil perfil = garantirPerfil(perfilNome);
        Usuario u = new Usuario();
        u.setNome(login);
        u.setLogin(login);
        u.setSenha("$2a$10$hashplaceholderparatestesunitarios000000000000000");
        u.setDataNascimento(LocalDate.of(1990, 1, 1));
        u.setStatus(1);
        u.setEmpresaId(empresaId);
        u.setFilialId(filialId);
        u.setPerfil(perfil);
        return usuarioRepository.save(u);
    }

    private Cliente criarCliente(long empresaId, long filialId, String nome, String doc, String email) {
        Cliente c = new Cliente();
        c.setNomeRazao(nome);
        c.setNomeFantasia(nome + " Fantasia");
        c.setTelefone("1133334444");
        c.setCelular("11999998888");
        c.setEmail(email);
        c.setTipoDocumento(1);
        c.setCpfCnpj(doc);
        c.setStatus(1);
        c.setEmpresaId(empresaId);
        c.setFilialId(filialId);
        c.setDataCadastro(LocalDateTime.now().minusMonths(2));
        return clienteRepository.save(c);
    }

    private String token(Long uid, String login, String role, Long empresaId, Long filialId) {
        return "Bearer " + jwtTokenService.issueAccessToken(
                uid, login, login, List.of(role), empresaId, filialId, Duration.ofMinutes(15));
    }
}
