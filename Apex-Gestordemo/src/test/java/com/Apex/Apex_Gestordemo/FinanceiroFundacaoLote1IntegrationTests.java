package com.Apex.Apex_Gestordemo;

import DTO.CentroCustoRequestDTO;
import DTO.DespesaRequestDTO;
import DTO.PagarDespesaRequestDTO;
import DTO.PlanoContasRequestDTO;
import DTO.ProdutoRequestDTO;
import DTO.ReclassificacaoManualRequestDTO;
import DTO.RegraClassificacaoRequestDTO;
import DTO.RelatorioFinanceiroDTO;
import DTO.RelatorioOperacionalDTO;
import DTO.TipoDespesaRequestDTO;
import Model.AuditLog;
import Model.Categoria;
import Model.CentroCusto;
import Model.ClassificacaoCusto;
import Model.Cliente;
import Model.ContaPagar;
import Model.ContaReceber;
import Model.Despesa;
import Model.Empresa;
import Model.Endereco;
import Model.Filial;
import Model.FinanceiroStatus;
import Model.FinancialAuditEvent;
import Model.FormaPagamento;
import Model.Fornecedor;
import Model.GrupoDreGerencial;
import Model.NaturezaContaGerencial;
import Model.OrigemClassificacaoFinanceira;
import Model.Perfil;
import Model.PlanoContasGerencial;
import Model.Produto;
import Model.ProdutoVenda;
import Model.ProdutoVendaId;
import Model.TipoCentroCusto;
import Model.TipoContaGerencial;
import Model.TipoDespesa;
import Model.TipoDevolucao;
import Model.Usuario;
import Model.Venda;
import Model.VendaDevolucao;
import Model.VendaDevolucaoItem;
import Model.VendaStatus;
import Repository.AuditLogRepository;
import Repository.CategoriaRepository;
import Repository.CentroCustoRepository;
import Repository.ClienteRepository;
import Repository.ContaPagarRepository;
import Repository.ContaReceberRepository;
import Repository.DespesaRepository;
import Repository.EmpresaRepository;
import Repository.FilialRepository;
import Repository.FinancialAuditEventRepository;
import Repository.FormaPagamentoRepository;
import Repository.FornecedorRepository;
import Repository.PerfilRepository;
import Repository.PlanoContasGerencialRepository;
import Repository.ProdutoRepository;
import Repository.TipoDespesaRepository;
import Repository.UsuarioRepository;
import Repository.VendaDevolucaoRepository;
import Repository.VendaRepository;
import Service.FinanceiroService;
import Service.RelatorioOperacionalService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:apex_lote1_fin;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "apex.jwt.secret=lote1-financeiro-integration-secret-key-32-chars",
        "apex.jwt.issuer=apex-gestor-lote1",
        "apex.jwt.audience=apex-api",
        "apex.security.require-auth=true",
        "apex.seed.homologacao=true",
        "apex.license.enforcement.enabled=false"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class FinanceiroFundacaoLote1IntegrationTests {

    private static final AtomicInteger SEQ = new AtomicInteger(100);

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private FilialRepository filialRepository;
    @Autowired private PerfilRepository perfilRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private FormaPagamentoRepository formaPagamentoRepository;
    @Autowired private TipoDespesaRepository tipoDespesaRepository;
    @Autowired private DespesaRepository despesaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private FornecedorRepository fornecedorRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private VendaRepository vendaRepository;
    @Autowired private VendaDevolucaoRepository vendaDevolucaoRepository;
    @Autowired private ContaPagarRepository contaPagarRepository;
    @Autowired private ContaReceberRepository contaReceberRepository;
    @Autowired private PlanoContasGerencialRepository planoContasRepository;
    @Autowired private CentroCustoRepository centroCustoRepository;
    @Autowired private FinancialAuditEventRepository financialAuditEventRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private FinanceiroService financeiroService;
    @Autowired private RelatorioOperacionalService relatorioOperacionalService;

    @Autowired private ObjectMapper json;

    @Test
    void flywayMigracoesV2AteV14ESchemaEnversAplicadosComSucesso() {
        Integer migrationsCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = TRUE
                  AND version IN ('2','3','4','5','6','7','8','9','10','11','12','13','14')
                """, Integer.class);
        Integer tablesLote1 = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME IN (
                    'plano_contas_gerencial',
                    'plano_contas_gerencial_aud',
                    'centro_custo',
                    'centro_custo_aud',
                    'regra_classificacao_financeira',
                    'regra_classificacao_financeira_aud',
                    'resumo_competencia_gerencial'
                )
                """, Integer.class);

        assertThat(migrationsCount).isEqualTo(13);
        assertThat(tablesLote1).isEqualTo(7);
    }

    @Test
    void planoContasECentrosCustoBootstrapIdempotenteArvoreCiclosEGuardaDeFilhaAtiva() throws Exception {
        TenantContext tenant = createTenant("PLANO");
        String token = loginToken(tenant.dono.getLogin());

        // Centros de custo bootstrap idempotente + CRUD + toggle status
        String ccInit1Body = mvc.perform(post("/api/financeiro/centros-custo/inicializar-padrao")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode ccInit1 = json.readTree(ccInit1Body);
        assertThat(ccInit1).hasSize(4);

        String ccInit2Body = mvc.perform(post("/api/financeiro/centros-custo/inicializar-padrao")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(ccInit2Body)).hasSize(4);

        String ccCreatedBody = mvc.perform(post("/api/financeiro/centros-custo")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new CentroCustoRequestDTO(
                                "CC-EXP", "Expansao Digital", "Canal online", TipoCentroCusto.COMERCIAL,
                                tenant.dono.getIdUsuario(), true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("CC-EXP"))
                .andReturn().getResponse().getContentAsString();
        long ccId = json.readTree(ccCreatedBody).get("idCentroCusto").asLong();

        mvc.perform(get("/api/financeiro/centros-custo/" + ccId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Expansao Digital"));

        mvc.perform(put("/api/financeiro/centros-custo/" + ccId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new CentroCustoRequestDTO(
                                "CC-EXP", "Expansao Omnichannel", "Loja + Web", TipoCentroCusto.COMERCIAL,
                                tenant.dono.getIdUsuario(), true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Expansao Omnichannel"));

        mvc.perform(patch("/api/financeiro/centros-custo/" + ccId + "/status?ativo=false")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mvc.perform(patch("/api/financeiro/centros-custo/" + ccId + "/status?ativo=true")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));

        // Plano de contas bootstrap idempotente
        String pcInit1 = mvc.perform(post("/api/financeiro/plano-contas/inicializar-padrao")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode pc1 = json.readTree(pcInit1);
        assertThat(pc1.get("contasCriadas").asInt()).isGreaterThan(0);
        assertThat(pc1.get("contasCriadas").asInt()).isEqualTo(26);
        assertThat(pc1.get("contasJaExistentes").asInt()).isZero();

        String pcInit2 = mvc.perform(post("/api/financeiro/plano-contas/inicializar-padrao")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode pc2 = json.readTree(pcInit2);
        assertThat(pc2.get("contasCriadas").asInt()).isZero();
        assertThat(pc2.get("contasJaExistentes").asInt()).isEqualTo(26);

        // Arvore hierarquica
        String arvoreBody = mvc.perform(get("/api/financeiro/plano-contas/arvore")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode arvore = json.readTree(arvoreBody);
        JsonNode grupo1 = findByCode(arvore, "1");
        assertThat(grupo1).isNotNull();
        assertThat(grupo1.get("filhas").size()).isGreaterThanOrEqualTo(2);

        // Criacao customizada pai + filha, guarda de auto-pai, ciclo e inativacao com filha ativa
        String paiBody = mvc.perform(post("/api/financeiro/plano-contas")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new PlanoContasRequestDTO(
                                "5.9", "Despesas Especiais", "Grupo customizado", null,
                                TipoContaGerencial.DESPESA, NaturezaContaGerencial.DEBITO,
                                ClassificacaoCusto.FIXO, GrupoDreGerencial.DESPESA_ADMINISTRATIVA,
                                590, false, true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long paiId = json.readTree(paiBody).get("idPlanoConta").asLong();

        String filhoBody = mvc.perform(post("/api/financeiro/plano-contas")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new PlanoContasRequestDTO(
                                "5.9.01", "Consultoria Externa", "Subconta analitica", paiId,
                                TipoContaGerencial.DESPESA, NaturezaContaGerencial.DEBITO,
                                ClassificacaoCusto.FIXO, GrupoDreGerencial.DESPESA_ADMINISTRATIVA,
                                591, true, true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long filhoId = json.readTree(filhoBody).get("idPlanoConta").asLong();

        // Rejeita auto-pai
        mvc.perform(put("/api/financeiro/plano-contas/" + paiId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new PlanoContasRequestDTO(
                                "5.9", "Despesas Especiais", "Auto pai", paiId,
                                TipoContaGerencial.DESPESA, NaturezaContaGerencial.DEBITO,
                                ClassificacaoCusto.FIXO, GrupoDreGerencial.DESPESA_ADMINISTRATIVA,
                                590, false, true))))
                .andExpect(status().isBadRequest());

        // Rejeita ciclo pai -> filho -> pai
        mvc.perform(put("/api/financeiro/plano-contas/" + paiId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new PlanoContasRequestDTO(
                                "5.9", "Despesas Especiais", "Ciclo", filhoId,
                                TipoContaGerencial.DESPESA, NaturezaContaGerencial.DEBITO,
                                ClassificacaoCusto.FIXO, GrupoDreGerencial.DESPESA_ADMINISTRATIVA,
                                590, false, true))))
                .andExpect(status().isBadRequest());

        // Bloqueia inativar pai enquanto filha estiver ativa
        mvc.perform(patch("/api/financeiro/plano-contas/" + paiId + "/status?ativo=false")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());

        // Inativa filha, depois permite inativar pai
        mvc.perform(patch("/api/financeiro/plano-contas/" + filhoId + "/status?ativo=false")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mvc.perform(patch("/api/financeiro/plano-contas/" + paiId + "/status?ativo=false")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void classificacaoAutomaticaEReclassificacaoManualAcrossTodasOrigensComTrilhaAuditoria() throws Exception {
        TenantContext tenant = createTenant("CLASS");
        String token = loginToken(tenant.dono.getLogin());

        mvc.perform(post("/api/financeiro/plano-contas/inicializar-padrao")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        PlanoContasGerencial conta501 = contaPorCodigo(tenant, "5.01");
        PlanoContasGerencial conta601 = contaPorCodigo(tenant, "6.01");
        PlanoContasGerencial conta801 = contaPorCodigo(tenant, "8.01");
        PlanoContasGerencial conta101 = contaPorCodigo(tenant, "1.01");
        PlanoContasGerencial conta102 = contaPorCodigo(tenant, "1.02");
        CentroCusto ccAdm = centroPorCodigo(tenant, "CC-ADM");
        CentroCusto ccLoja = centroPorCodigo(tenant, "CC-LOJA");

        FormaPagamento formaPix = saveForma(tenant, "PIX Classif", 2);
        TipoDespesa tipoTelecom = saveTipoDespesa(tenant, "Telecom");
        Categoria cat = saveCategoria(tenant, "Equipamentos");
        Fornecedor forn = saveFornecedor(tenant, "Fornecedor Classif");
        Cliente cli = saveCliente(tenant, "Cliente Classif");
        Produto prod = saveProduto(tenant, cat, forn, "Produto Classif", bd("20.00"), bd("50.00"));

        Despesa despesa = saveDespesa(tenant, tipoTelecom, "Internet Fibra Matriz", bd("180.00"),
                LocalDate.of(2025, 4, 10), null, 1, null);
        ContaPagar contaPagar = saveContaPagar(tenant, forn.getIdFornecedor(), cat.getIdCategoria(),
                "Fatura Fornecedor Classif", bd("400.00"), LocalDate.of(2025, 4, 12), FinanceiroStatus.ABERTO, null, null);
        Venda venda = saveVenda(tenant, cli, prod, 2, bd("50.00"), bd("20.00"),
                LocalDate.of(2025, 4, 15), VendaStatus.FINALIZADA, null);
        ContaReceber contaReceber = saveContaReceber(tenant, venda.getIdVenda(), cli.getIdCliente(),
                formaPix.getIdFormaPagamento(), bd("100.00"), bd("0.00"), FinanceiroStatus.ABERTO,
                LocalDate.of(2025, 4, 15), null, null);

        // CRUD de regras
        String regraDespBody = mvc.perform(post("/api/financeiro/classificacoes/regras")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegraClassificacaoRequestDTO(
                                "Regra Internet", OrigemClassificacaoFinanceira.DESPESA,
                                tipoTelecom.getIdTipoDespesa(), null, null, null, "Internet",
                                conta801.getIdPlanoConta(), ccAdm.getIdCentroCusto(), 10, true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long regraId = json.readTree(regraDespBody).get("idRegraClassificacao").asLong();

        mvc.perform(post("/api/financeiro/classificacoes/regras")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegraClassificacaoRequestDTO(
                                "Regra Fornecedor", OrigemClassificacaoFinanceira.CONTA_PAGAR,
                                null, null, null, forn.getIdFornecedor(), null,
                                conta601.getIdPlanoConta(), ccLoja.getIdCentroCusto(), 20, true))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/financeiro/classificacoes/regras")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegraClassificacaoRequestDTO(
                                "Regra Recebimento PIX", OrigemClassificacaoFinanceira.CONTA_RECEBER,
                                null, null, formaPix.getIdFormaPagamento(), null, null,
                                conta102.getIdPlanoConta(), ccLoja.getIdCentroCusto(), 30, true))))
                .andExpect(status().isCreated());

        String dummyRegraBody = mvc.perform(post("/api/financeiro/classificacoes/regras")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegraClassificacaoRequestDTO(
                                "Regra Temporaria", OrigemClassificacaoFinanceira.VENDA,
                                null, null, null, null, "NUNCA_CASA",
                                conta101.getIdPlanoConta(), null, 99, true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long dummyRegraId = json.readTree(dummyRegraBody).get("idRegraClassificacao").asLong();

        mvc.perform(get("/api/financeiro/classificacoes/regras")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        mvc.perform(put("/api/financeiro/classificacoes/regras/" + regraId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegraClassificacaoRequestDTO(
                                "Regra Internet Atualizada", OrigemClassificacaoFinanceira.DESPESA,
                                tipoTelecom.getIdTipoDespesa(), null, null, null, "Internet",
                                conta801.getIdPlanoConta(), ccAdm.getIdCentroCusto(), 5, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Regra Internet Atualizada"));

        mvc.perform(patch("/api/financeiro/classificacoes/regras/" + dummyRegraId + "/status?ativo=false")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        // Pendentes antes do automatico
        mvc.perform(get("/api/financeiro/classificacoes/pendentes")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPendentes").value(4))
                .andExpect(jsonPath("$.despesasPendentes").value(1))
                .andExpect(jsonPath("$.contasPagarPendentes").value(1))
                .andExpect(jsonPath("$.contasReceberPendentes").value(1))
                .andExpect(jsonPath("$.vendasPendentes").value(1));

        // Aplicar automatico
        mvc.perform(post("/api/financeiro/classificacoes/aplicar-automatico")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processados").value(4))
                .andExpect(jsonPath("$.classificados").value(4))
                .andExpect(jsonPath("$.naoClassificados").value(0));

        mvc.perform(get("/api/financeiro/classificacoes/pendentes")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPendentes").value(0));

        assertThat(despesaRepository.findById(despesa.getIdDespesa()).orElseThrow().getPlanoContaId())
                .isEqualTo(conta801.getIdPlanoConta());
        assertThat(contaPagarRepository.findById(contaPagar.getIdContaPagar()).orElseThrow().getPlanoContaId())
                .isEqualTo(conta601.getIdPlanoConta());
        assertThat(contaReceberRepository.findById(contaReceber.getIdContaReceber()).orElseThrow().getPlanoContaId())
                .isEqualTo(conta102.getIdPlanoConta());
        assertThat(vendaRepository.findById(venda.getIdVenda()).orElseThrow().getPlanoContaId())
                .isEqualTo(conta101.getIdPlanoConta());

        // Reclassificacao manual nas 4 origens + trilha de auditoria
        mvc.perform(post("/api/financeiro/classificacoes/manual")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReclassificacaoManualRequestDTO(
                                OrigemClassificacaoFinanceira.DESPESA, despesa.getIdDespesa(),
                                conta501.getIdPlanoConta(), ccLoja.getIdCentroCusto(), "Ajuste centro loja"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sugestaoPlanoContaId").value(conta501.getIdPlanoConta()));

        mvc.perform(post("/api/financeiro/classificacoes/manual")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReclassificacaoManualRequestDTO(
                                OrigemClassificacaoFinanceira.CONTA_PAGAR, contaPagar.getIdContaPagar(),
                                conta501.getIdPlanoConta(), ccAdm.getIdCentroCusto(), "Reclassificacao CP"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sugestaoPlanoContaId").value(conta501.getIdPlanoConta()));

        mvc.perform(post("/api/financeiro/classificacoes/manual")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReclassificacaoManualRequestDTO(
                                OrigemClassificacaoFinanceira.CONTA_RECEBER, contaReceber.getIdContaReceber(),
                                conta101.getIdPlanoConta(), ccLoja.getIdCentroCusto(), "Reclassificacao CR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sugestaoPlanoContaId").value(conta101.getIdPlanoConta()));

        mvc.perform(post("/api/financeiro/classificacoes/manual")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReclassificacaoManualRequestDTO(
                                OrigemClassificacaoFinanceira.VENDA, venda.getIdVenda(),
                                conta102.getIdPlanoConta(), ccAdm.getIdCentroCusto(), "Reclassificacao Venda"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sugestaoPlanoContaId").value(conta102.getIdPlanoConta()));

        String auditoriaBody = mvc.perform(get("/api/financeiro/auditoria")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode auditoria = json.readTree(auditoriaBody);

        List<String> reclassAlvos = new ArrayList<>();
        for (JsonNode ev : auditoria) {
            if ("RECLASSIFICACAO_MANUAL".equals(ev.get("tipoEvento").asText())) {
                reclassAlvos.add(ev.get("alvoTipo").asText());
            }
        }
        assertThat(reclassAlvos).containsExactlyInAnyOrder("DESPESA", "CONTA_PAGAR", "CONTA_RECEBER", "VENDA");

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            var reader = AuditReaderFactory.get(em);
            assertThat(reader.getRevisions(Despesa.class, despesa.getIdDespesa())).hasSizeGreaterThanOrEqualTo(2);
            assertThat(reader.getRevisions(ContaPagar.class, contaPagar.getIdContaPagar())).hasSizeGreaterThanOrEqualTo(2);
            assertThat(reader.getRevisions(ContaReceber.class, contaReceber.getIdContaReceber())).hasSizeGreaterThanOrEqualTo(2);
            assertThat(reader.getRevisions(Venda.class, venda.getIdVenda())).hasSizeGreaterThanOrEqualTo(2);
        } finally {
            em.close();
        }
    }

    @Test
    void consolidacaoCompetenciaApropriaCompetenciaEmM1ECaixaEmM2ExcluindoAguardandoECanceladas() throws Exception {
        TenantContext tenant = createTenant("COMP");
        String token = loginToken(tenant.dono.getLogin());

        mvc.perform(post("/api/financeiro/plano-contas/inicializar-padrao")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        PlanoContasGerencial conta501 = contaPorCodigo(tenant, "5.01");
        Categoria cat = saveCategoria(tenant, "Cat Comp");
        Fornecedor forn = saveFornecedor(tenant, "Forn Comp");
        Cliente cli = saveCliente(tenant, "Cli Comp");
        Produto prod = saveProduto(tenant, cat, forn, "Prod Comp", bd("100.00"), bd("250.00"));
        FormaPagamento forma = saveForma(tenant, "Boleto Comp", 3);
        TipoDespesa tipo = saveTipoDespesa(tenant, "Despesa Comp");

        // M1 = 2025-05, M2 = 2025-06
        // 1. Venda finalizada em M1 (nao classificada para testar lancamentosSemPlanoConta + avisosLimitacao)
        Venda vendaFinalizada = saveVenda(tenant, cli, prod, 2, bd("250.00"), bd("100.00"),
                LocalDate.of(2025, 5, 10), VendaStatus.FINALIZADA, null);

        // 2. Venda AGUARDANDO_CAIXA em M1 -> deve ser ignorada
        saveVenda(tenant, cli, prod, 3, bd("333.00"), bd("111.00"),
                LocalDate.of(2025, 5, 12), VendaStatus.AGUARDANDO_CAIXA, null);

        // 3. Devolucao em M1 vinculada a venda finalizada (100 de ajuste receita, 40 de CMV devolvido)
        VendaDevolucao dev = new VendaDevolucao();
        dev.setVendaOriginalId(vendaFinalizada.getIdVenda());
        dev.setEmpresaId(tenant.empresaId);
        dev.setFilialId(tenant.filialId);
        dev.setTipo(TipoDevolucao.PARCIAL);
        dev.setMotivo("Item avariado");
        dev.setValorAjuste(bd("100.00"));
        dev.setCriadoPor(tenant.dono.getLogin());
        dev.setCriadoEm(LocalDateTime.of(2025, 5, 15, 12, 0));
        VendaDevolucaoItem devItem = new VendaDevolucaoItem();
        devItem.setDevolucao(dev);
        devItem.setProdutoOriginalId(prod.getIdProduto());
        devItem.setQuantidade(1);
        devItem.setValorUnitario(bd("100.00"));
        devItem.setCustoUnitario(bd("40.00"));
        dev.getItens().add(devItem);
        vendaDevolucaoRepository.save(dev);

        // 4. Despesa competencia M1, pagamento M2 (sem planoContaId -> reportada como pendente)
        saveDespesa(tenant, tipo, "Aluguel Maio Pago Junho", bd("300.00"),
                LocalDate.of(2025, 5, 20), LocalDate.of(2025, 6, 5), 1, null);

        // 5. ContaPagar ativa competencia M1, paga em M2
        saveContaPagar(tenant, forn.getIdFornecedor(), cat.getIdCategoria(),
                "Fornecedor Maio Pago Junho", bd("150.00"), LocalDate.of(2025, 5, 18),
                FinanceiroStatus.PAGO, LocalDateTime.of(2025, 6, 8, 9, 0), conta501.getIdPlanoConta());

        // 6. ContaPagar CANCELADO em M1 -> deve ser ignorada
        saveContaPagar(tenant, forn.getIdFornecedor(), cat.getIdCategoria(),
                "Conta Cancelada Ignorada", bd("777.00"), LocalDate.of(2025, 5, 19),
                FinanceiroStatus.CANCELADO, null, conta501.getIdPlanoConta());

        // 7. ContaReceber competencia M1, recebida em M2
        saveContaReceber(tenant, vendaFinalizada.getIdVenda(), cli.getIdCliente(),
                forma.getIdFormaPagamento(), bd("500.00"), bd("500.00"), FinanceiroStatus.PAGO,
                LocalDate.of(2025, 5, 10), LocalDateTime.of(2025, 6, 3, 14, 0), null);

        // Recalculo M1 (2025-05)
        String m1Body = mvc.perform(post("/api/financeiro/consolidacao/competencia/recalcular?competencia=2025-05")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode m1 = json.readTree(m1Body);

        // Receita M1 = 500 - 100 devolucao = 400; CMV = 200 - 40 = 160; Despesas = 300 + 150 = 450
        assertThat(new BigDecimal(m1.get("receitasCompetencia").asText())).isEqualByComparingTo("400.00");
        assertThat(new BigDecimal(m1.get("cmvCompetencia").asText())).isEqualByComparingTo("160.00");
        assertThat(new BigDecimal(m1.get("despesasCompetencia").asText())).isEqualByComparingTo("450.00");
        assertThat(new BigDecimal(m1.get("resultadoCompetencia").asText())).isEqualByComparingTo("-210.00");
        assertThat(new BigDecimal(m1.get("entradasRealizadasCaixa").asText())).isEqualByComparingTo("0.00");
        assertThat(new BigDecimal(m1.get("saidasRealizadasCaixa").asText())).isEqualByComparingTo("0.00");
        assertThat(new BigDecimal(m1.get("saldoRealizadoCaixa").asText())).isEqualByComparingTo("0.00");
        assertThat(m1.get("lancamentosSemPlanoConta").asLong()).isEqualTo(2L);
        assertThat(new BigDecimal(m1.get("valorSemPlanoConta").asText())).isEqualByComparingTo("800.00");
        assertThat(m1.get("avisosLimitacao").toString()).contains("sem classificacao no plano de contas gerencial");

        // Recalculo M2 (2025-06): receitas/despesas de competencia = 0, caixa realizado = +500 - 450 = +50
        String m2Body = mvc.perform(post("/api/financeiro/consolidacao/competencia/recalcular?competencia=2025-06")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode m2 = json.readTree(m2Body);

        assertThat(new BigDecimal(m2.get("receitasCompetencia").asText())).isEqualByComparingTo("0.00");
        assertThat(new BigDecimal(m2.get("cmvCompetencia").asText())).isEqualByComparingTo("0.00");
        assertThat(new BigDecimal(m2.get("despesasCompetencia").asText())).isEqualByComparingTo("0.00");
        assertThat(new BigDecimal(m2.get("entradasRealizadasCaixa").asText())).isEqualByComparingTo("500.00");
        assertThat(new BigDecimal(m2.get("saidasRealizadasCaixa").asText())).isEqualByComparingTo("450.00");
        assertThat(new BigDecimal(m2.get("saldoRealizadoCaixa").asText())).isEqualByComparingTo("50.00");

        // Consulta GET M1 confirma snapshot consistente
        mvc.perform(get("/api/financeiro/consolidacao/competencia?competencia=2025-05")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receitasCompetencia").value(400.00))
                .andExpect(jsonPath("$.entradasRealizadasCaixa").value(0.00));
    }

    @Test
    void despesasValidacaoDefaultTipoCrudMultiTenantTipoDespesaEBloqueioCatalogoGlobal() throws Exception {
        TenantContext tenant1 = createTenant("DESP1");
        TenantContext tenant2 = createTenant("DESP2");
        String token1 = loginToken(tenant1.dono.getLogin());
        String token2 = loginToken(tenant2.dono.getLogin());

        // @Valid rejeita valor <= 0 com 400
        mvc.perform(post("/api/despesas")
                        .header("Authorization", bearer(token1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DespesaRequestDTO(
                                "Valor Zero", bd("0.00"), LocalDate.of(2026, 5, 10),
                                null, null, null, null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados invalidos"));

        mvc.perform(post("/api/despesas")
                        .header("Authorization", bearer(token1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DespesaRequestDTO(
                                "Valor Negativo", bd("-5.00"), LocalDate.of(2026, 5, 10),
                                null, null, null, null, null, null))))
                .andExpect(status().isBadRequest());

        // Omitindo tipoDespesaId usa fallback "Despesas Gerais"
        String createdBody = mvc.perform(post("/api/despesas")
                        .header("Authorization", bearer(token1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DespesaRequestDTO(
                                "Material Escritorio", bd("120.50"), LocalDate.of(2026, 5, 15),
                                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 10),
                                null, null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoDespesaNome").value("Despesas Gerais"))
                .andReturn().getResponse().getContentAsString();
        long despesaId = json.readTree(createdBody).get("idDespesa").asLong();

        // PUT /{id}
        mvc.perform(put("/api/despesas/" + despesaId)
                        .header("Authorization", bearer(token1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DespesaRequestDTO(
                                "Material Escritorio Atualizado", bd("145.00"), LocalDate.of(2026, 5, 16),
                                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 10),
                                null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Material Escritorio Atualizado"))
                .andExpect(jsonPath("$.valor").value(145.00));

        // PATCH /{id}/pagar
        mvc.perform(patch("/api/despesas/" + despesaId + "/pagar")
                        .header("Authorization", bearer(token1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataPagamento\":\"2026-05-18\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataPagamento").value("2026-05-18"));

        // PATCH /{id}/cancelar e guarda contra pagamento apos cancelamento
        mvc.perform(patch("/api/despesas/" + despesaId + "/cancelar")
                        .header("Authorization", bearer(token1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0));

        mvc.perform(patch("/api/despesas/" + despesaId + "/pagar")
                        .header("Authorization", bearer(token1)))
                .andExpect(status().isBadRequest());

        // Dois tenants criam TipoDespesa com o mesmo nome (uk_tipo_despesa_tenant_nome)
        String sharedName = "Energia Lote1 " + SEQ.incrementAndGet();
        mvc.perform(post("/api/despesas/tipos")
                        .header("Authorization", bearer(token1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new TipoDespesaRequestDTO(
                                sharedName, 1, "FIXA", null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value(sharedName));

        mvc.perform(post("/api/despesas/tipos")
                        .header("Authorization", bearer(token2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new TipoDespesaRequestDTO(
                                sharedName, 1, "FIXA", null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value(sharedName));

        // Catalogo global (empresaId == null) bloqueado contra mutacao com 403
        TipoDespesa globalTipo = new TipoDespesa();
        globalTipo.setNome("Global Tipo " + SEQ.incrementAndGet());
        globalTipo.setStatus(1);
        globalTipo.setEmpresaId(null);
        globalTipo.setFilialId(null);
        globalTipo = tipoDespesaRepository.save(globalTipo);

        FormaPagamento globalForma = new FormaPagamento();
        globalForma.setNome("Global Forma " + SEQ.incrementAndGet());
        globalForma.setDescricao("Forma global");
        globalForma.setTipoPagamento(1);
        globalForma.setStatus(1);
        globalForma.setEmpresaId(null);
        globalForma.setFilialId(null);
        globalForma = formaPagamentoRepository.save(globalForma);

        Categoria globalCat = new Categoria();
        globalCat.setDescricao("Global Cat " + SEQ.incrementAndGet());
        globalCat.setStatus(1);
        globalCat.setEmpresaId(null);
        globalCat.setFilialId(null);
        globalCat = categoriaRepository.save(globalCat);

        mvc.perform(put("/api/despesas/tipos/" + globalTipo.getIdTipoDespesa())
                        .header("Authorization", bearer(token1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new TipoDespesaRequestDTO(
                                "Tentativa Alterar Global", 1, "FIXA", null, null))))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/formaspagamento/" + globalForma.getIdFormaPagamento())
                        .header("Authorization", bearer(token1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Alterada\",\"descricao\":\"Desc\",\"tipoPagamento\":1,\"status\":1}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/formaspagamento/" + globalForma.getIdFormaPagamento() + "/desativar")
                        .header("Authorization", bearer(token1)))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/categorias/" + globalCat.getIdCategoria())
                        .header("Authorization", bearer(token1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Alterada\",\"status\":1}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/categorias/" + globalCat.getIdCategoria() + "/desativar")
                        .header("Authorization", bearer(token1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void relatoriosFinanceiroEOperacionalDistinguemSaldoCaixaDeLucroEExcluemAguardandoCaixa() throws Exception {
        TenantContext tenant = createTenant("REL");
        String token = loginToken(tenant.dono.getLogin());

        Categoria cat = saveCategoria(tenant, "Cat Rel");
        Fornecedor forn = saveFornecedor(tenant, "Forn Rel");
        Cliente cli = saveCliente(tenant, "Cli Rel");
        Produto prod = saveProduto(tenant, cat, forn, "Prod Rel", bd("150.00"), bd("500.00"));
        FormaPagamento forma = saveForma(tenant, "Prazo Rel", 3);
        TipoDespesa tipo = saveTipoDespesa(tenant, "Tipo Rel");

        Venda vendaFinalizada = saveVenda(tenant, cli, prod, 1, bd("500.00"), bd("150.00"),
                LocalDate.of(2025, 7, 10), VendaStatus.FINALIZADA, null);
        // ContaReceber em ABERTO -> entra na receita/lucro, mas NAO entra em saldoCaixa
        saveContaReceber(tenant, vendaFinalizada.getIdVenda(), cli.getIdCliente(), forma.getIdFormaPagamento(),
                bd("500.00"), bd("0.00"), FinanceiroStatus.ABERTO, LocalDate.of(2025, 7, 10), null, null);

        // Venda AGUARDANDO_CAIXA -> excluida do relatorio financeiro e de valorTotal/ativos do operacional
        saveVenda(tenant, cli, prod, 1, bd("800.00"), bd("200.00"),
                LocalDate.of(2025, 7, 11), VendaStatus.AGUARDANDO_CAIXA, null);

        // Despesa em competencia, nao paga -> entra em totalDespesas, mas nao em saldoCaixa
        saveDespesa(tenant, tipo, "Despesa Nao Paga", bd("100.00"),
                LocalDate.of(2025, 7, 12), null, 1, null);

        mvc.perform(get("/api/relatorios/financeiro?inicio=2025-07-01&fim=2025-07-31")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVendas").value(500.00))
                .andExpect(jsonPath("$.totalCustosProdutos").value(150.00))
                .andExpect(jsonPath("$.totalDespesas").value(100.00))
                .andExpect(jsonPath("$.lucroLiquido").value(250.00))
                .andExpect(jsonPath("$.saldoCaixa").value(0.00));

        RelatorioFinanceiroDTO dto = financeiroService.gerarRelatorio(
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 31), tenant.empresaId, tenant.filialId);
        assertThat(dto.getLucroLiquido()).isEqualByComparingTo("250.00");
        assertThat(dto.getSaldoCaixa()).isEqualByComparingTo("0.00");
        assertThat(dto.getSaldoCaixa()).isNotEqualByComparingTo(dto.getLucroLiquido());

        mvc.perform(get("/api/relatorios/vendas")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRegistros").value(2))
                .andExpect(jsonPath("$.ativos").value(1))
                .andExpect(jsonPath("$.inativos").value(1))
                .andExpect(jsonPath("$.valorTotal").value(500.00));

        RelatorioOperacionalDTO op = relatorioOperacionalService.vendas(tenant.empresaId, tenant.filialId);
        assertThat(op.getAtivos()).isEqualTo(1L);
        assertThat(op.getValorTotal()).isEqualByComparingTo("500.00");
    }

    @Test
    void isolamentoMultiTenantBloqueiaAcessoCruzadoEmPlanoCentrosClassificacaoEDespesas() throws Exception {
        TenantContext tenantA = createTenant("ISOA");
        TenantContext tenantB = createTenant("ISOB");
        String tokenA = loginToken(tenantA.dono.getLogin());
        String tokenB = loginToken(tenantB.dono.getLogin());

        mvc.perform(post("/api/financeiro/plano-contas/inicializar-padrao")
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/financeiro/plano-contas/inicializar-padrao")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk());

        PlanoContasGerencial contaA = contaPorCodigo(tenantA, "5.01");
        CentroCusto ccA = centroPorCodigo(tenantA, "CC-ADM");
        PlanoContasGerencial contaB = contaPorCodigo(tenantB, "5.01");
        CentroCusto ccB = centroPorCodigo(tenantB, "CC-ADM");

        TipoDespesa tipoA = saveTipoDespesa(tenantA, "Tipo Iso A");
        Despesa despesaA = saveDespesa(tenantA, tipoA, "Despesa Tenant A", bd("220.00"),
                LocalDate.of(2026, 3, 10), null, 1, null);

        // Tenant B bloqueado em /plano-contas/{id}
        mvc.perform(get("/api/financeiro/plano-contas/" + contaA.getIdPlanoConta())
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/financeiro/plano-contas/" + contaA.getIdPlanoConta())
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new PlanoContasRequestDTO(
                                "5.01", "Hack", "Hack", null, TipoContaGerencial.DESPESA,
                                NaturezaContaGerencial.DEBITO, ClassificacaoCusto.FIXO,
                                GrupoDreGerencial.DESPESA_ADMINISTRATIVA, 1, true, true))))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/financeiro/plano-contas/" + contaA.getIdPlanoConta() + "/status?ativo=false")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());

        // Tenant B bloqueado em /centros-custo/{id}
        mvc.perform(get("/api/financeiro/centros-custo/" + ccA.getIdCentroCusto())
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/financeiro/centros-custo/" + ccA.getIdCentroCusto())
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new CentroCustoRequestDTO(
                                "CC-ADM", "Hack", "Hack", TipoCentroCusto.ADMINISTRATIVO, null, true))))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/financeiro/centros-custo/" + ccA.getIdCentroCusto() + "/status?ativo=false")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());

        // Tenant B bloqueado em /classificacoes/manual contra registro de Tenant A
        mvc.perform(post("/api/financeiro/classificacoes/manual")
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReclassificacaoManualRequestDTO(
                                OrigemClassificacaoFinanceira.DESPESA, despesaA.getIdDespesa(),
                                contaB.getIdPlanoConta(), ccB.getIdCentroCusto(), "Tentativa cross-tenant"))))
                .andExpect(status().isNotFound());

        // Tenant B bloqueado em /despesas/{id}
        mvc.perform(get("/api/despesas/" + despesaA.getIdDespesa())
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/despesas/" + despesaA.getIdDespesa())
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DespesaRequestDTO(
                                "Hack", bd("10.00"), LocalDate.of(2026, 3, 10),
                                null, null, null, null, null, null))))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/despesas/" + despesaA.getIdDespesa() + "/pagar")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/despesas/" + despesaA.getIdDespesa() + "/cancelar")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());

        Despesa untouched = despesaRepository.findById(despesaA.getIdDespesa()).orElseThrow();
        assertThat(untouched.getPlanoContaId()).isNull();
        assertThat(untouched.getDescricao()).isEqualTo("Despesa Tenant A");
        assertThat(untouched.getDataPagamento()).isNull();
        assertThat(untouched.getStatus()).isEqualTo(1);
    }

    @Test
    void matrizRbac8PerfisFinanceiroDespesasECaixa() throws Exception {
        Usuario donoHml = usuarioRepository.findByLogin("v5_dono").orElseThrow();
        long empresaId = donoHml.getEmpresaId();
        long filialId = donoHml.getFilialId();

        Usuario auditorUser = ensureUser("lote1_auditor", "Auditor Lote1", "AUDITOR", empresaId, filialId);
        Usuario contadorUser = ensureUser("lote1_contador", "Contador Lote1", "CONTADOR", empresaId, filialId);
        Usuario financeiroUser = ensureUser("lote1_financeiro", "Financeiro Lote1", "FINANCEIRO", empresaId, filialId);

        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("VENDEDOR", loginToken("v5_vendedor"));
        tokens.put("CAIXA", loginToken("v5_caixa"));
        tokens.put("AUDITOR", loginToken(auditorUser.getLogin()));
        tokens.put("CONTADOR", loginToken(contadorUser.getLogin()));
        tokens.put("FINANCEIRO", loginToken(financeiroUser.getLogin()));
        tokens.put("GERENTE", loginToken("v5_gerente"));
        tokens.put("DONO_GERENTE", loginToken("v5_dono"));
        tokens.put("SYSADMIN", loginToken("v5_sysadmin"));

        String donoToken = tokens.get("DONO_GERENTE");
        mvc.perform(post("/api/financeiro/plano-contas/inicializar-padrao")
                        .header("Authorization", bearer(donoToken)))
                .andExpect(status().isOk());
        PlanoContasGerencial conta501 = planoContasRepository
                .findByCodigoAndEmpresaIdAndFilialId("5.01", empresaId, filialId).orElseThrow();

        String despBody = mvc.perform(post("/api/despesas")
                        .header("Authorization", bearer(donoToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DespesaRequestDTO(
                                "Despesa Matriz RBAC", bd("89.90"), LocalDate.of(2026, 4, 20),
                                LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 20),
                                null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long despesaId = json.readTree(despBody).get("idDespesa").asLong();

        // 1. VENDEDOR e CAIXA -> 403 em /api/financeiro/** e /api/despesas/**
        for (String role : List.of("VENDEDOR", "CAIXA")) {
            String tok = tokens.get(role);
            for (String path : List.of(
                    "/api/financeiro/plano-contas",
                    "/api/financeiro/centros-custo",
                    "/api/financeiro/classificacoes/pendentes",
                    "/api/financeiro/consolidacao/competencia?competencia=2026-04",
                    "/api/despesas"
            )) {
                mvc.perform(get(path).header("Authorization", bearer(tok)))
                        .andExpect(status().isForbidden());
            }
            mvc.perform(post("/api/financeiro/plano-contas")
                            .header("Authorization", bearer(tok))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/despesas")
                            .header("Authorization", bearer(tok))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        // 2. AUDITOR -> 200 em GET e 403 em POST/PUT/PATCH
        String auditorToken = tokens.get("AUDITOR");
        for (String getPath : List.of(
                "/api/financeiro/plano-contas",
                "/api/financeiro/centros-custo",
                "/api/financeiro/classificacoes/regras",
                "/api/financeiro/classificacoes/pendentes",
                "/api/financeiro/consolidacao/competencia?competencia=2026-04",
                "/api/financeiro/auditoria",
                "/api/despesas",
                "/api/relatorios/financeiro?inicio=2026-04-01&fim=2026-04-30"
        )) {
            mvc.perform(get(getPath).header("Authorization", bearer(auditorToken)))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/api/financeiro/plano-contas")
                        .header("Authorization", bearer(auditorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new PlanoContasRequestDTO(
                                "5.88", "Audit Fail", null, null, TipoContaGerencial.DESPESA,
                                NaturezaContaGerencial.DEBITO, ClassificacaoCusto.FIXO,
                                GrupoDreGerencial.DESPESA_ADMINISTRATIVA, 1, true, true))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/financeiro/classificacoes/manual")
                        .header("Authorization", bearer(auditorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReclassificacaoManualRequestDTO(
                                OrigemClassificacaoFinanceira.DESPESA, despesaId,
                                conta501.getIdPlanoConta(), null, "Audit fail"))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/financeiro/consolidacao/competencia/recalcular?competencia=2026-04")
                        .header("Authorization", bearer(auditorToken)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/despesas")
                        .header("Authorization", bearer(auditorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DespesaRequestDTO(
                                "Audit Fail", bd("10.00"), LocalDate.of(2026, 4, 20),
                                null, null, null, null, null, null))))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/despesas/" + despesaId)
                        .header("Authorization", bearer(auditorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DespesaRequestDTO(
                                "Audit Fail", bd("10.00"), LocalDate.of(2026, 4, 20),
                                null, null, null, null, null, null))))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/despesas/" + despesaId + "/pagar")
                        .header("Authorization", bearer(auditorToken)))
                .andExpect(status().isForbidden());

        // 3. CONTADOR -> 200/201 em GET, classificacao, tipos de despesa e recalculo de competencia;
        //    403 em pagar despesa e abrir caixa
        String contadorToken = tokens.get("CONTADOR");
        for (String getPath : List.of(
                "/api/financeiro/plano-contas",
                "/api/financeiro/centros-custo",
                "/api/financeiro/classificacoes/pendentes",
                "/api/financeiro/consolidacao/competencia?competencia=2026-04",
                "/api/despesas",
                "/api/relatorios/financeiro?inicio=2026-04-01&fim=2026-04-30"
        )) {
            mvc.perform(get(getPath).header("Authorization", bearer(contadorToken)))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/api/financeiro/classificacoes/manual")
                        .header("Authorization", bearer(contadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReclassificacaoManualRequestDTO(
                                OrigemClassificacaoFinanceira.DESPESA, despesaId,
                                conta501.getIdPlanoConta(), null, "Classificado pelo contador"))))
                .andExpect(status().isOk());
        String tipoContadorBody = mvc.perform(post("/api/despesas/tipos")
                        .header("Authorization", bearer(contadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new TipoDespesaRequestDTO(
                                "Tipo Contador " + SEQ.incrementAndGet(), 1, "FIXA",
                                conta501.getIdPlanoConta(), null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long tipoContadorId = json.readTree(tipoContadorBody).get("idTipoDespesa").asLong();
        mvc.perform(put("/api/despesas/tipos/" + tipoContadorId)
                        .header("Authorization", bearer(contadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new TipoDespesaRequestDTO(
                                "Tipo Contador Edit " + SEQ.incrementAndGet(), 1, "FIXA",
                                conta501.getIdPlanoConta(), null))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/financeiro/consolidacao/competencia/recalcular?competencia=2026-04")
                        .header("Authorization", bearer(contadorToken)))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/despesas/" + despesaId + "/pagar")
                        .header("Authorization", bearer(contadorToken)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/caixa/abrir")
                        .header("Authorization", bearer(contadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":100.00,\"observacao\":\"Contador\"}"))
                .andExpect(status().isForbidden());

        // 4. FINANCEIRO -> acesso a GET /api/caixa/atual (200/204) + /api/caixa/historico (200),
        //    200 em /api/financeiro/** e /api/despesas/**, 403 em POST /api/caixa/abrir
        String finToken = tokens.get("FINANCEIRO");
        MvcResult finCaixaAtual = mvc.perform(get("/api/caixa/atual")
                        .header("Authorization", bearer(finToken)))
                .andReturn();
        assertThat(finCaixaAtual.getResponse().getStatus()).isIn(200, 204);
        mvc.perform(get("/api/caixa/historico")
                        .header("Authorization", bearer(finToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/financeiro/plano-contas")
                        .header("Authorization", bearer(finToken)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/financeiro/consolidacao/competencia/recalcular?competencia=2026-04")
                        .header("Authorization", bearer(finToken)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/caixa/abrir")
                        .header("Authorization", bearer(finToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":100.00,\"observacao\":\"Financeiro\"}"))
                .andExpect(status().isForbidden());

        // 5. GERENTE, DONO_GERENTE, SYSADMIN -> permitidos em financeiro, despesas e caixa
        for (String role : List.of("GERENTE", "DONO_GERENTE", "SYSADMIN")) {
            String tok = tokens.get(role);
            mvc.perform(get("/api/financeiro/plano-contas").header("Authorization", bearer(tok)))
                    .andExpect(status().isOk());
            mvc.perform(post("/api/financeiro/consolidacao/competencia/recalcular?competencia=2026-04")
                            .header("Authorization", bearer(tok)))
                    .andExpect(status().isOk());
            String newDesp = mvc.perform(post("/api/despesas")
                            .header("Authorization", bearer(tok))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(new DespesaRequestDTO(
                                    "Despesa " + role, bd("50.00"), LocalDate.of(2026, 4, 22),
                                    LocalDate.of(2026, 4, 22), LocalDate.of(2026, 4, 22),
                                    null, null, null, null))))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            long newDespId = json.readTree(newDesp).get("idDespesa").asLong();
            mvc.perform(patch("/api/despesas/" + newDespId + "/pagar")
                            .header("Authorization", bearer(tok)))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/relatorios/financeiro?inicio=2026-04-01&fim=2026-04-30")
                            .header("Authorization", bearer(tok)))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/caixa/historico").header("Authorization", bearer(tok)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void roleSupervisorRecebe403EmFinanceiroDespesasECrossTenantCategoriaIdorEmProdutos() throws Exception {
        Usuario donoHml = usuarioRepository.findByLogin("v5_dono").orElseThrow();
        Usuario supervisor = ensureUser("lote1_supervisor", "Supervisor Lote1", "SUPERVISOR",
                donoHml.getEmpresaId(), donoHml.getFilialId());
        String supervisorToken = loginToken(supervisor.getLogin());

        for (String path : List.of(
                "/api/financeiro/plano-contas",
                "/api/financeiro/auditoria",
                "/api/financeiro/documentos",
                "/api/despesas"
        )) {
            mvc.perform(get(path).header("Authorization", bearer(supervisorToken)))
                    .andExpect(status().isForbidden());
        }

        TenantContext tenantA = createTenant("CATA");
        TenantContext tenantB = createTenant("CATB");
        Categoria catPrivadaA = saveCategoria(tenantA, "Categoria Privada A");
        Fornecedor fornB = saveFornecedor(tenantB, "Fornecedor B");
        String tokenB = loginToken(tenantB.dono.getLogin());
        String barcodeCrossTenant = "7899998880" + SEQ.incrementAndGet();

        ProdutoRequestDTO crossTenantProd = new ProdutoRequestDTO(
                "Produto IDOR Categoria", bd("10.00"), barcodeCrossTenant, "INT-IDOR", null,
                "Marca", "Grupo", null, "UN", "Unidade", null, 5, 1, bd("25.00"),
                null, null, null, null, null, null, null, null, null, null, 1,
                new ProdutoRequestDTO.RefDTO(fornB.getIdFornecedor(), null, null),
                new ProdutoRequestDTO.RefDTO(null, catPrivadaA.getIdCategoria(), null),
                null, null);

        mvc.perform(post("/api/produtos")
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(crossTenantProd)))
                .andExpect(status().isBadRequest());

        assertThat(produtoRepository.findByCodigoBarrasAndEmpresaIdAndFilialId(
                barcodeCrossTenant, tenantB.empresaId, tenantB.filialId)).isEmpty();
    }

    @Test
    void despesaBloqueiaMassAssignmentEImutabilidadeAposPagamentoOuCancelamento() throws Exception {
        TenantContext tenant = createTenant("MASS");
        String token = loginToken(tenant.dono.getLogin());

        // Mass assignment probe: payload tenta forcar status=0 e dataPagamento preenchida na criacao
        String massPayload = """
                {
                  "descricao": "Despesa Mass Assignment",
                  "valor": 75.50,
                  "dataVencimento": "2026-05-10",
                  "dataCompetencia": "2026-05-10",
                  "dataEmissao": "2026-05-10",
                  "status": 0,
                  "dataPagamento": "2026-05-01"
                }
                """;
        String createdBody = mvc.perform(post("/api/despesas")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(massPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.dataPagamento").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        long id1 = json.readTree(createdBody).get("idDespesa").asLong();

        Despesa persisted1 = despesaRepository.findById(id1).orElseThrow();
        assertThat(persisted1.getStatus()).isEqualTo(1);
        assertThat(persisted1.getDataPagamento()).isNull();

        // PATCH /pagar com PagarDespesaRequestDTO JSON
        mvc.perform(patch("/api/despesas/" + id1 + "/pagar")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new PagarDespesaRequestDTO(LocalDate.of(2026, 5, 18)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataPagamento").value("2026-05-18"));

        // PUT em despesa ja paga rejeitado com 400
        mvc.perform(put("/api/despesas/" + id1)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DespesaRequestDTO(
                                "Tentativa Alterar Paga", bd("99.00"), LocalDate.of(2026, 5, 10),
                                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 10),
                                null, null, null, null))))
                .andExpect(status().isBadRequest());

        // PATCH /pagar com body vazio usa data atual
        String created2 = mvc.perform(post("/api/despesas")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DespesaRequestDTO(
                                "Despesa Pagar Vazio", bd("30.00"), LocalDate.of(2026, 5, 12),
                                LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 12),
                                null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id2 = json.readTree(created2).get("idDespesa").asLong();

        mvc.perform(patch("/api/despesas/" + id2 + "/pagar")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataPagamento").value(LocalDate.now().toString()));

        // MAJOR-2: Repetir PATCH /pagar em despesa ja quitada rejeitado com 400
        mvc.perform(patch("/api/despesas/" + id2 + "/pagar")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataPagamento\":\"2026-06-01\"}"))
                .andExpect(status().isBadRequest());

        // PUT em despesa cancelada rejeitado com 400
        String created3 = mvc.perform(post("/api/despesas")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DespesaRequestDTO(
                                "Despesa Para Cancelar", bd("45.00"), LocalDate.of(2026, 5, 14),
                                LocalDate.of(2026, 5, 14), LocalDate.of(2026, 5, 14),
                                null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id3 = json.readTree(created3).get("idDespesa").asLong();

        mvc.perform(patch("/api/despesas/" + id3 + "/cancelar")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0));

        mvc.perform(put("/api/despesas/" + id3)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DespesaRequestDTO(
                                "Tentativa Alterar Cancelada", bd("90.00"), LocalDate.of(2026, 5, 14),
                                LocalDate.of(2026, 5, 14), LocalDate.of(2026, 5, 14),
                                null, null, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void classificacaoManualERegrasValidamPolaridadeCreditoVsDebito() throws Exception {
        TenantContext tenant = createTenant("POL");
        String token = loginToken(tenant.dono.getLogin());

        mvc.perform(post("/api/financeiro/plano-contas/inicializar-padrao")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        PlanoContasGerencial contaReceita101 = contaPorCodigo(tenant, "1.01");
        PlanoContasGerencial contaDespesa501 = contaPorCodigo(tenant, "5.01");

        Categoria cat = saveCategoria(tenant, "Cat Pol");
        Fornecedor forn = saveFornecedor(tenant, "Forn Pol");
        Cliente cli = saveCliente(tenant, "Cli Pol");
        Produto prod = saveProduto(tenant, cat, forn, "Prod Pol", bd("20.00"), bd("50.00"));
        FormaPagamento forma = saveForma(tenant, "Forma Pol", 1);
        TipoDespesa tipo = saveTipoDespesa(tenant, "Tipo Pol");

        Despesa despesa = saveDespesa(tenant, tipo, "Despesa Polaridade", bd("80.00"),
                LocalDate.of(2026, 2, 10), null, 1, null);
        ContaPagar contaPagar = saveContaPagar(tenant, forn.getIdFornecedor(), cat.getIdCategoria(),
                "CP Polaridade", bd("120.00"), LocalDate.of(2026, 2, 11), FinanceiroStatus.ABERTO, null, null);
        Venda venda = saveVenda(tenant, cli, prod, 1, bd("50.00"), bd("20.00"),
                LocalDate.of(2026, 2, 12), VendaStatus.FINALIZADA, null);
        ContaReceber contaReceber = saveContaReceber(tenant, venda.getIdVenda(), cli.getIdCliente(),
                forma.getIdFormaPagamento(), bd("50.00"), bd("0.00"), FinanceiroStatus.ABERTO,
                LocalDate.of(2026, 2, 12), null, null);

        // Classificacao manual: DESPESA/CONTA_PAGAR -> CREDITO (1.01) deve falhar 400
        mvc.perform(post("/api/financeiro/classificacoes/manual")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReclassificacaoManualRequestDTO(
                                OrigemClassificacaoFinanceira.DESPESA, despesa.getIdDespesa(),
                                contaReceita101.getIdPlanoConta(), null, "Polaridade errada"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/financeiro/classificacoes/manual")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReclassificacaoManualRequestDTO(
                                OrigemClassificacaoFinanceira.CONTA_PAGAR, contaPagar.getIdContaPagar(),
                                contaReceita101.getIdPlanoConta(), null, "Polaridade errada"))))
                .andExpect(status().isBadRequest());

        // Classificacao manual: VENDA/CONTA_RECEBER -> DEBITO (5.01) deve falhar 400
        mvc.perform(post("/api/financeiro/classificacoes/manual")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReclassificacaoManualRequestDTO(
                                OrigemClassificacaoFinanceira.VENDA, venda.getIdVenda(),
                                contaDespesa501.getIdPlanoConta(), null, "Polaridade errada"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/financeiro/classificacoes/manual")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReclassificacaoManualRequestDTO(
                                OrigemClassificacaoFinanceira.CONTA_RECEBER, contaReceber.getIdContaReceber(),
                                contaDespesa501.getIdPlanoConta(), null, "Polaridade errada"))))
                .andExpect(status().isBadRequest());

        // Regras de classificacao: DESPESA -> 1.01 e VENDA -> 5.01 devem falhar 400
        mvc.perform(post("/api/financeiro/classificacoes/regras")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegraClassificacaoRequestDTO(
                                "Regra Polaridade Errada Despesa", OrigemClassificacaoFinanceira.DESPESA,
                                null, null, null, null, "Despesa",
                                contaReceita101.getIdPlanoConta(), null, 10, true))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/financeiro/classificacoes/regras")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegraClassificacaoRequestDTO(
                                "Regra Polaridade Errada Venda", OrigemClassificacaoFinanceira.VENDA,
                                null, null, null, null, "Venda",
                                contaDespesa501.getIdPlanoConta(), null, 10, true))))
                .andExpect(status().isBadRequest());

        // MAJOR-1: DespesaController POST/PUT e POST /tipos rejeitam conta CREDITO (1.01) com 400
        mvc.perform(post("/api/despesas")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DespesaRequestDTO(
                                "Despesa Com Conta Receita", bd("50.00"), LocalDate.of(2026, 2, 15),
                                LocalDate.of(2026, 2, 15), LocalDate.of(2026, 2, 15),
                                tipo.getIdTipoDespesa(), null, contaReceita101.getIdPlanoConta(), null))))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/despesas/" + despesa.getIdDespesa())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DespesaRequestDTO(
                                "Despesa Polaridade", bd("80.00"), LocalDate.of(2026, 2, 10),
                                LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 10),
                                tipo.getIdTipoDespesa(), null, contaReceita101.getIdPlanoConta(), null))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/despesas/tipos")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new TipoDespesaRequestDTO(
                                "Tipo Com Conta Receita", 1, "FIXO", contaReceita101.getIdPlanoConta(), null))))
                .andExpect(status().isBadRequest());

        // MINOR-6: PUT /api/despesas/{id} omitindo planoContaId preserva classificacao manual existente
        mvc.perform(put("/api/despesas/" + despesa.getIdDespesa())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DespesaRequestDTO(
                                "Despesa Com 501", bd("80.00"), LocalDate.of(2026, 2, 10),
                                LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 10),
                                tipo.getIdTipoDespesa(), null, contaDespesa501.getIdPlanoConta(), null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planoContaId").value(contaDespesa501.getIdPlanoConta()));
        mvc.perform(put("/api/despesas/" + despesa.getIdDespesa())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DespesaRequestDTO(
                                "Despesa Editada Sem Informar Conta", bd("85.00"), LocalDate.of(2026, 2, 10),
                                LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 10),
                                tipo.getIdTipoDespesa(), null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planoContaId").value(contaDespesa501.getIdPlanoConta()));

        // MAJOR-4: GET /pendentes?origemTipo=CONTA_PAGAR filtra apenas CONTA_PAGAR
        mvc.perform(get("/api/financeiro/classificacoes/pendentes?origemTipo=CONTA_PAGAR")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].origemTipo").value("CONTA_PAGAR"));
    }

    @Test
    void competenciaRegexDriftSnapshotAoVivoMascaramentoPiiEAntiSpoofXForwardedFor() throws Exception {
        TenantContext tenant = createTenant("DRIFT");
        String token = loginToken(tenant.dono.getLogin());

        mvc.perform(post("/api/financeiro/plano-contas/inicializar-padrao")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        PlanoContasGerencial conta501 = contaPorCodigo(tenant, "5.01");
        TipoDespesa tipo = saveTipoDespesa(tenant, "Tipo Drift");

        // Regex competencia rejeita "+10000-01" em GET e POST
        mvc.perform(get("/api/financeiro/consolidacao/competencia")
                        .queryParam("competencia", "+10000-01")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/financeiro/consolidacao/competencia/recalcular")
                        .queryParam("competencia", "+10000-01")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());

        // 1. Grava snapshot limpo de M1 (2025-05) com 1 despesa classificada de 200.00
        saveDespesa(tenant, tipo, "Despesa Inicial Classificada", bd("200.00"),
                LocalDate.of(2025, 5, 10), null, 1, conta501.getIdPlanoConta());

        mvc.perform(post("/api/financeiro/consolidacao/competencia/recalcular?competencia=2025-05")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.despesasCompetencia").value(200.00))
                .andExpect(jsonPath("$.lancamentosSemPlanoConta").value(0));

        // 2a. Insere nova despesa classificada apos o snapshot -> drift real detectado em snapshotDesatualizado
        saveDespesa(tenant, tipo, "Despesa Extra Classificada", bd("50.00"),
                LocalDate.of(2025, 5, 21), null, 1, conta501.getIdPlanoConta());
        String staleBody = mvc.perform(get("/api/financeiro/consolidacao/competencia?competencia=2025-05")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(staleBody).get("avisosLimitacao").toString())
                .contains("Snapshot anterior desatualizado para os lancamentos atuais");

        // 2b. Insere despesa nao classificada e recalcula -> aviso honesto sobre pendentes sem alegar snapshot classificado desatualizado
        Despesa novaDespesa = saveDespesa(tenant, tipo, "Despesa Nova Sem Classificacao", bd("125.00"),
                LocalDate.of(2025, 5, 22), null, 1, null);
        mvc.perform(post("/api/financeiro/consolidacao/competencia/recalcular?competencia=2025-05")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        String driftBody = mvc.perform(get("/api/financeiro/consolidacao/competencia?competencia=2025-05")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode drift = json.readTree(driftBody);

        assertThat(new BigDecimal(drift.get("despesasCompetencia").asText())).isEqualByComparingTo("375.00");
        assertThat(new BigDecimal(drift.get("resultadoCompetencia").asText())).isEqualByComparingTo("-375.00");
        assertThat(drift.get("lancamentosSemPlanoConta").asLong()).isEqualTo(1L);
        assertThat(new BigDecimal(drift.get("valorSemPlanoConta").asText())).isEqualByComparingTo("125.00");
        assertThat(drift.get("linhas")).hasSize(1);
        assertThat(new BigDecimal(drift.get("linhas").get(0).get("valorCompetencia").asText()))
                .isEqualByComparingTo("250.00");
        assertThat(drift.get("avisosLimitacao").toString())
                .contains("Consolidacao ao vivo inclui lancamentos pendentes de classificacao")
                .doesNotContain("Snapshot anterior desatualizado");

        // 3. Reclassificacao manual com motivo contendo CPF/email e header X-Forwarded-For spoofado
        mvc.perform(post("/api/financeiro/classificacoes/manual")
                        .header("Authorization", bearer(token))
                        .header("X-Forwarded-For", "203.0.113.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReclassificacaoManualRequestDTO(
                                OrigemClassificacaoFinanceira.DESPESA, novaDespesa.getIdDespesa(),
                                conta501.getIdPlanoConta(), null,
                                "Revisao CPF 123.456.789-00 email joao.silva@empresa.com"))))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findByEmpresaIdAndFilialIdOrderByCriadoEmDesc(
                tenant.empresaId, tenant.filialId);
        AuditLog reclassLog = logs.stream()
                .filter(l -> "RECLASSIFICACAO_MANUAL".equals(l.getAcao())
                        && novaDespesa.getIdDespesa().equals(l.getEntidadeId()))
                .findFirst()
                .orElseThrow();

        assertThat(reclassLog.getIp()).isEqualTo("127.0.0.1");
        assertThat(reclassLog.getIp()).isNotEqualTo("203.0.113.99");
        assertThat(reclassLog.getJustificativa()).doesNotContain("123.456.789-00");
        assertThat(reclassLog.getJustificativa()).doesNotContain("joao.silva@empresa.com");
        assertThat(reclassLog.getJustificativa()).contains("***DOCUMENTO***");
        assertThat(reclassLog.getJustificativa()).contains("j***@empresa.com");

        String auditoriaJson = mvc.perform(get("/api/auditoria")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(auditoriaJson).doesNotContain("123.456.789-00");
        assertThat(auditoriaJson).doesNotContain("joao.silva@empresa.com");
        assertThat(auditoriaJson).doesNotContain("203.0.113.99");

        String enversIp = jdbcTemplate.queryForObject("""
                SELECT r.source_ip
                FROM audit_revision r
                JOIN despesas_aud da ON da.REV = r.revision_id
                WHERE da.id_despesa = ?
                ORDER BY r.revision_id DESC
                LIMIT 1
                """, String.class, novaDespesa.getIdDespesa());
        assertThat(enversIp).isEqualTo("127.0.0.1");
        assertThat(enversIp).isNotEqualTo("203.0.113.99");
    }

    private TenantContext createTenant(String tag) {
        int id = SEQ.incrementAndGet();
        Empresa emp = new Empresa();
        emp.setRazaoSocial("Empresa Lote1 " + tag + " " + id);
        emp.setNomeFantasia("Apex " + tag + " " + id);
        emp.setCnpj(String.format("99%012d", id));
        emp.setStatus(1);
        emp = empresaRepository.save(emp);

        Filial fil = new Filial();
        fil.setEmpresa(emp);
        fil.setNome("Filial " + tag + " " + id);
        fil.setCodigo("F-" + tag + "-" + id);
        fil.setStatus(1);
        fil = filialRepository.save(fil);

        String login = ("dono_" + tag + "_" + id).toLowerCase();
        Usuario dono = ensureUser(login, "Dono " + tag, "DONO_GERENTE", emp.getIdEmpresa(), fil.getIdFilial());
        return new TenantContext(emp.getIdEmpresa(), fil.getIdFilial(), dono);
    }

    private Usuario ensureUser(String login, String nome, String perfilNome, Long empresaId, Long filialId) {
        return usuarioRepository.findByLogin(login).orElseGet(() -> {
            Perfil perfil = perfilRepository.findByNome(perfilNome).orElseGet(() -> {
                Perfil p = new Perfil();
                p.setNome(perfilNome);
                p.setStatus(1);
                return perfilRepository.save(p);
            });
            Usuario u = new Usuario();
            u.setLogin(login);
            u.setNome(nome.length() > 45 ? nome.substring(0, 45) : nome);
            u.setSenha(passwordEncoder.encode("Apex@2026"));
            u.setDataNascimento(LocalDate.of(1990, 1, 1));
            u.setStatus(1);
            u.setEmpresaId(empresaId);
            u.setFilialId(filialId);
            u.setPerfil(perfil);
            return usuarioRepository.save(u);
        });
    }

    private String loginToken(String login) throws Exception {
        String payload = json.writeValueAsString(Map.of(
                "login", login,
                "senha", "Apex@2026",
                "consentVersion", "2026-01",
                "acceptedPrivacyTerms", true));
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("accessToken").asText();
    }

    private PlanoContasGerencial contaPorCodigo(TenantContext t, String codigo) {
        return planoContasRepository.findByCodigoAndEmpresaIdAndFilialId(codigo, t.empresaId, t.filialId)
                .orElseThrow();
    }

    private CentroCusto centroPorCodigo(TenantContext t, String codigo) {
        return centroCustoRepository.findByCodigoAndEmpresaIdAndFilialId(codigo, t.empresaId, t.filialId)
                .orElseThrow();
    }

    private Categoria saveCategoria(TenantContext t, String desc) {
        Categoria c = new Categoria();
        c.setDescricao(desc);
        c.setStatus(1);
        c.setEmpresaId(t.empresaId);
        c.setFilialId(t.filialId);
        return categoriaRepository.save(c);
    }

    private FormaPagamento saveForma(TenantContext t, String nome, int tipo) {
        FormaPagamento f = new FormaPagamento();
        f.setNome(nome);
        f.setDescricao(nome);
        f.setTipoPagamento(tipo);
        f.setStatus(1);
        f.setEmpresaId(t.empresaId);
        f.setFilialId(t.filialId);
        return formaPagamentoRepository.save(f);
    }

    private TipoDespesa saveTipoDespesa(TenantContext t, String nome) {
        TipoDespesa td = new TipoDespesa();
        td.setNome(nome);
        td.setStatus(1);
        td.setEmpresaId(t.empresaId);
        td.setFilialId(t.filialId);
        return tipoDespesaRepository.save(td);
    }

    private Fornecedor saveFornecedor(TenantContext t, String razao) {
        int id = SEQ.incrementAndGet();
        Endereco end = new Endereco();
        end.setEstado("SP");
        end.setCidade("Sao Paulo");
        end.setCep("01000-000");
        end.setBairro("Centro");
        end.setLogradouro("Rua Teste");
        end.setUf("SP");
        end.setNumero("100");
        end.setEmpresaId(t.empresaId);
        end.setFilialId(t.filialId);

        Fornecedor f = new Fornecedor();
        f.setStatus(1);
        f.setRazaoSocial(razao);
        f.setNomeFantasia(razao.length() > 45 ? razao.substring(0, 45) : razao);
        f.setCnpj(String.format("88%012d", id));
        f.setTelefone("11988887777");
        f.setDataCadastro(LocalDateTime.now());
        f.setEmpresaId(t.empresaId);
        f.setFilialId(t.filialId);
        f.setEndereco(end);
        return fornecedorRepository.save(f);
    }

    private Cliente saveCliente(TenantContext t, String nome) {
        int id = SEQ.incrementAndGet();
        Cliente c = new Cliente();
        c.setNomeRazao(nome);
        c.setTelefone("11977776666");
        c.setTipoDocumento(1);
        c.setCpfCnpj(String.format("%011d", id));
        c.setDataCadastro(LocalDateTime.now());
        c.setStatus(1);
        c.setEmpresaId(t.empresaId);
        c.setFilialId(t.filialId);
        return clienteRepository.save(c);
    }

    private Produto saveProduto(TenantContext t, Categoria cat, Fornecedor forn, String desc,
            BigDecimal custo, BigDecimal valorVenda) {
        int id = SEQ.incrementAndGet();
        Produto p = new Produto();
        p.setDescricao(desc);
        p.setCusto(custo);
        p.setValorVenda(valorVenda);
        p.setQuantidadeEstoque(100);
        p.setEstoqueMinimo(5);
        p.setCodigoBarras("789" + String.format("%010d", id));
        p.setCodigoInterno("INT-" + id);
        p.setStatus(1);
        p.setEmpresaId(t.empresaId);
        p.setFilialId(t.filialId);
        p.setCategoria(cat);
        p.setFornecedor(forn);
        return produtoRepository.save(p);
    }

    private Despesa saveDespesa(TenantContext t, TipoDespesa tipo, String desc, BigDecimal valor,
            LocalDate competencia, LocalDate pagamento, int status, Long planoContaId) {
        Despesa d = new Despesa();
        d.setDescricao(desc);
        d.setValor(valor);
        d.setDataCompetencia(competencia);
        d.setDataEmissao(competencia);
        d.setDataVencimento(competencia);
        d.setDataPagamento(pagamento);
        d.setStatus(status);
        d.setEmpresaId(t.empresaId);
        d.setFilialId(t.filialId);
        d.setTipoDespesa(tipo);
        d.setPlanoContaId(planoContaId);
        return despesaRepository.save(d);
    }

    private ContaPagar saveContaPagar(TenantContext t, Long fornecedorId, Long categoriaId, String desc,
            BigDecimal valor, LocalDate competencia, FinanceiroStatus status, LocalDateTime pagoEm, Long planoContaId) {
        ContaPagar cp = new ContaPagar();
        cp.setFornecedorId(fornecedorId);
        cp.setCategoriaId(categoriaId);
        cp.setEmpresaId(t.empresaId);
        cp.setFilialId(t.filialId);
        cp.setDescricao(desc);
        cp.setValor(valor);
        cp.setVencimento(competencia);
        cp.setDataCompetencia(competencia);
        cp.setDataEmissao(competencia);
        cp.setStatus(status);
        cp.setCriadoEm(competencia.atTime(9, 0));
        cp.setPagoEm(pagoEm);
        cp.setPlanoContaId(planoContaId);
        return contaPagarRepository.save(cp);
    }

    private Venda saveVenda(TenantContext t, Cliente cli, Produto prod, int qtd, BigDecimal precoUnit,
            BigDecimal custoUnit, LocalDate competencia, VendaStatus statusOp, Long planoContaId) {
        BigDecimal total = precoUnit.multiply(BigDecimal.valueOf(qtd));
        Venda v = new Venda();
        v.setUsuario(t.dono);
        v.setCliente(cli);
        v.setStatus(1);
        v.setStatusOperacional(statusOp);
        v.setNumeroVenda("V-" + t.filialId + "-" + SEQ.incrementAndGet());
        v.setValorTotal(total);
        v.setDesconto(BigDecimal.ZERO);
        v.setEmpresaId(t.empresaId);
        v.setFilialId(t.filialId);
        v.setDataVenda(competencia.atTime(10, 0));
        v.setDataCompetencia(competencia);
        v.setDataEmissao(competencia);
        if (statusOp == VendaStatus.FINALIZADA || statusOp == VendaStatus.PAGA) {
            v.setFinalizadoEm(competencia.atTime(10, 5));
        }
        v.setEstoqueMovimentado(statusOp == VendaStatus.FINALIZADA);
        v.setPlanoContaId(planoContaId);
        v.setItens(new ArrayList<>());
        v.setPagamentos(new ArrayList<>());
        v = vendaRepository.save(v);

        ProdutoVenda pv = new ProdutoVenda();
        pv.setId(new ProdutoVendaId(prod.getIdProduto(), v.getIdVenda()));
        pv.setVenda(v);
        pv.setProduto(prod);
        pv.setQuantidade(qtd);
        pv.setPrecoUnitario(precoUnit);
        pv.setCustoUnitario(custoUnit);
        pv.setPrecoTotal(total);
        v.getItens().add(pv);
        return vendaRepository.save(v);
    }

    private ContaReceber saveContaReceber(TenantContext t, Long vendaId, Long clienteId, Long formaId,
            BigDecimal valor, BigDecimal valorPago, FinanceiroStatus status, LocalDate competencia,
            LocalDateTime pagoEm, Long planoContaId) {
        ContaReceber cr = new ContaReceber();
        cr.setVendaId(vendaId);
        cr.setClienteId(clienteId);
        cr.setFormaPagamentoId(formaId);
        cr.setEmpresaId(t.empresaId);
        cr.setFilialId(t.filialId);
        cr.setValorBruto(valor);
        cr.setDesconto(BigDecimal.ZERO);
        cr.setValorLiquido(valor);
        cr.setValorPago(valorPago);
        cr.setSaldo(valor.subtract(valorPago));
        cr.setVencimento(competencia);
        cr.setDataCompetencia(competencia);
        cr.setDataEmissao(competencia);
        cr.setStatus(status);
        cr.setCriadoEm(competencia.atTime(10, 5));
        cr.setPagoEm(pagoEm);
        cr.setPlanoContaId(planoContaId);
        return contaReceberRepository.save(cr);
    }

    private JsonNode findByCode(JsonNode nodes, String code) {
        for (JsonNode n : nodes) {
            if (code.equals(n.get("codigo").asText())) {
                return n;
            }
        }
        return null;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private record TenantContext(Long empresaId, Long filialId, Usuario dono) {
    }
}
