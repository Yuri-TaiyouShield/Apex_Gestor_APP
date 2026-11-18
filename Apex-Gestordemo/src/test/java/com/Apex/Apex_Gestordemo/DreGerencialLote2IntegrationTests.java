package com.Apex.Apex_Gestordemo;

import DTO.PlanoContasRequestDTO;
import DTO.ReclassificacaoManualRequestDTO;
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
import Model.NotaFiscalEntrada;
import Model.OrigemClassificacaoFinanceira;
import Model.Perfil;
import Model.PlanoContasGerencial;
import Model.Produto;
import Model.ProdutoVenda;
import Model.ProdutoVendaId;
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
import Repository.NotaFiscalEntradaRepository;
import Repository.PerfilRepository;
import Repository.PlanoContasGerencialRepository;
import Repository.ProdutoRepository;
import Repository.TipoDespesaRepository;
import Repository.UsuarioRepository;
import Repository.VendaDevolucaoRepository;
import Repository.VendaRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class DreGerencialLote2IntegrationTests {

    private static final AtomicInteger SEQ = new AtomicInteger(5000);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private FilialRepository filialRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private PlanoContasGerencialRepository planoContasRepository;

    @Autowired
    private CentroCustoRepository centroCustoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private FormaPagamentoRepository formaPagamentoRepository;

    @Autowired
    private TipoDespesaRepository tipoDespesaRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private DespesaRepository despesaRepository;

    @Autowired
    private ContaPagarRepository contaPagarRepository;

    @Autowired
    private ContaReceberRepository contaReceberRepository;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private VendaDevolucaoRepository vendaDevolucaoRepository;

    @Autowired
    private NotaFiscalEntradaRepository notaFiscalEntradaRepository;

    @Autowired
    private FinancialAuditEventRepository financialAuditEventRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void dre17LinhasConfiguravelHierarquicaComContasAnaliticasSinteticasEDeducoesDescontosDevolucoes() throws Exception {
        TenantContext t = createTenant("DRE17");
        String token = loginToken(t.dono.getLogin());

        bootstrapFoundation(token);

        PlanoContasGerencial contaVendas = contaPorCodigo(t, "1.01");
        PlanoContasGerencial contaTributosVendas = contaPorCodigo(t, "2.03");
        PlanoContasGerencial contaAluguel = contaPorCodigo(t, "5.01");
        PlanoContasGerencial contaSalarios = contaPorCodigo(t, "7.01");
        PlanoContasGerencial contaTaxasCartao = contaPorCodigo(t, "9.01");
        PlanoContasGerencial contaRecFinanceira = contaPorCodigo(t, "9.02");
        PlanoContasGerencial contaOutrasDesp = contaPorCodigo(t, "10.02");
        PlanoContasGerencial contaIrpj = contaPorCodigo(t, "11.01");
        CentroCusto ccVendas = centroPorCodigo(t, "CC-LOJA");
        CentroCusto ccAdm = centroPorCodigo(t, "CC-ADM");

        // Cria conta sintetica 4.10 e conta analitica filha 4.10.01 (Marketing Digital) em DESPESA_COMERCIAL
        PlanoContasRequestDTO reqSintetica = new PlanoContasRequestDTO(
                "4.10",
                "Marketing e Trafego Pago (Sintetica)",
                "Grupo sintetico de marketing",
                null,
                TipoContaGerencial.DESPESA,
                NaturezaContaGerencial.DEBITO,
                ClassificacaoCusto.VARIAVEL,
                GrupoDreGerencial.DESPESA_COMERCIAL,
                45,
                false,
                true);
        String bodySintetica = mvc.perform(post("/api/financeiro/plano-contas")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reqSintetica)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long idSintetica = json.readTree(bodySintetica).get("idPlanoConta").asLong();

        PlanoContasRequestDTO reqAnalitica = new PlanoContasRequestDTO(
                "4.10.01",
                "Campanhas Patrocinadas Loja",
                "Subconta analitica de trafego",
                idSintetica,
                TipoContaGerencial.DESPESA,
                NaturezaContaGerencial.DEBITO,
                ClassificacaoCusto.VARIAVEL,
                GrupoDreGerencial.DESPESA_COMERCIAL,
                46,
                true,
                true);
        String bodyAnalitica = mvc.perform(post("/api/financeiro/plano-contas")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reqAnalitica)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long idMarketingAnalitica = json.readTree(bodyAnalitica).get("idPlanoConta").asLong();

        Categoria cat = saveCategoria(t, "Vestuario");
        Fornecedor forn = saveFornecedor(t, "Textil Sul");
        Cliente cli = saveCliente(t, "Cliente DRE");
        Produto prod = saveProduto(t, cat, forn, "Jaqueta Jeans", bd("40.00"), bd("100.00"));
        TipoDespesa tdGeral = saveTipoDespesa(t, "Despesa Geral");

        LocalDate comp = LocalDate.of(2026, 6, 15);

        // Venda finalizada: 20 unidades x R$ 100 = R$ 2.000 bruto, desconto comercial R$ 100 -> valorTotal liquido = R$ 1.900, CMV = 20 x 40 = R$ 800
        Venda venda = saveVendaComDesconto(t, cli, prod, 20, bd("100.00"), bd("40.00"), bd("100.00"),
                comp, VendaStatus.FINALIZADA, contaVendas.getIdPlanoConta(), ccVendas.getIdCentroCusto());

        // Devolucao parcial no mesmo mes: 2 unidades devolvidas = R$ 200 devolucao (Linha 2) e reversao de CMV = 2 x 40 = R$ 80 (reduz Linha 6 para 720)
        saveDevolucao(t, venda, prod, 2, bd("100.00"), bd("40.00"), bd("200.00"), comp);

        // Tributos sobre vendas (Linha 4 - conta 2.03): R$ 100
        saveDespesaComCentro(t, tdGeral, "DAS Simples Nacional", bd("100.00"), comp, comp, 1,
                contaTributosVendas.getIdPlanoConta(), ccVendas.getIdCentroCusto());

        // Despesa comercial na conta analitica filha 4.10.01 (Linha 8): R$ 150
        saveDespesaComCentro(t, tdGeral, "Anuncios Instagram", bd("150.00"), comp, comp, 1,
                idMarketingAnalitica, ccVendas.getIdCentroCusto());

        // Despesa administrativa fixa (Linha 9 - conta 5.01): R$ 250
        saveDespesaComCentro(t, tdGeral, "Aluguel Loja", bd("250.00"), comp, comp, 1,
                contaAluguel.getIdPlanoConta(), ccAdm.getIdCentroCusto());

        // Despesa de pessoal fixa (Linha 11 - conta 7.01): R$ 200
        saveDespesaComCentro(t, tdGeral, "Folha Mensal", bd("200.00"), comp, comp, 1,
                contaSalarios.getIdPlanoConta(), ccAdm.getIdCentroCusto());

        // Despesa financeira variavel (Linha 12 - conta 8.01): R$ 30
        saveDespesaComCentro(t, tdGeral, "Taxa Maquininha", bd("30.00"), comp, comp, 1,
                contaTaxasCartao.getIdPlanoConta(), ccVendas.getIdCentroCusto());

        // Receita financeira (Linha 13 +9.02): +R$ 50
        saveDespesaComCentro(t, tdGeral, "Rendimento Aplicacao CDB", bd("50.00"), comp, comp, 1,
                contaRecFinanceira.getIdPlanoConta(), ccAdm.getIdCentroCusto());

        // Outras despesas nao operacionais (Linha 13 -10.02): -R$ 20 -> Liquido Linha 13 = +30
        saveDespesaComCentro(t, tdGeral, "Multa Contratual", bd("20.00"), comp, comp, 1,
                contaOutrasDesp.getIdPlanoConta(), ccAdm.getIdCentroCusto());

        // Tributos sobre o lucro (Linha 16 - conta 11.01): R$ 40
        saveDespesaComCentro(t, tdGeral, "Provisao IRPJ/CSLL", bd("40.00"), comp, comp, 1,
                contaIrpj.getIdPlanoConta(), ccAdm.getIdCentroCusto());

        String dreJson = mvc.perform(get("/api/financeiro/dre")
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", "2026-06")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = json.readTree(dreJson);
        assertThat(root.get("linhas").size()).isEqualTo(17);

        // Algebra exata das 17 linhas (CPC 26 / Lei 6.404 Art. 187):
        // L1 Receita Bruta = 2000.00 (venda)
        // L2 (-) Devolucoes = 200.00
        // L3 (-) Descontos = 100.00
        // L4 (-) Tributos Vendas = 100.00
        // L5 (=) Receita Liquida = 1600.00 (AV = 100.00%)
        // L6 (-) CMV = 800.00 - 80.00 = 720.00 (AV = 45.00%)
        // L7 (=) Lucro Bruto = 880.00 (AV = 55.00%)
        // L8 (-) Desp. Comerciais = 150.00
        // L9 (-) Desp. Administrativas = 250.00
        // L10 (-) Desp. Operacionais Gerais = 0.00
        // L11 (-) Desp. Pessoal = 200.00
        // L12 (+/-) Outras Receitas/Despesas Operacionais = -20.00 (Multa 10.02)
        // L13 (=) Resultado Operacional (Antes do Resultado Financeiro) = 880 - (150+250+0+200) - 20 = 260.00 (AV = 16.25%)
        // L14 (+/-) Resultado Financeiro Liquido = +50.00 (9.02) - 30.00 (8.01) = +20.00
        // L15 (=) Resultado Antes Tributos (LAIR) = 260.00 + 20.00 = 280.00 (diferente do Resultado Operacional!)
        // L16 (-) Tributos sobre Lucro = 40.00
        // L17 (=) Resultado Liquido Gerencial = 240.00 (AV = 15.00%)
        assertThat(bd(root.get("indicadores").get("receitaBruta").asText())).isEqualByComparingTo("2000.00");
        assertThat(bd(root.get("indicadores").get("deducoesTotais").asText())).isEqualByComparingTo("400.00");
        assertThat(bd(root.get("indicadores").get("receitaLiquida").asText())).isEqualByComparingTo("1600.00");
        assertThat(bd(root.get("indicadores").get("cmv").asText())).isEqualByComparingTo("720.00");
        assertThat(bd(root.get("indicadores").get("lucroBruto").asText())).isEqualByComparingTo("880.00");
        assertThat(bd(root.get("indicadores").get("margemBrutaPercentual").asText())).isEqualByComparingTo("55.00");
        assertThat(bd(root.get("indicadores").get("despesasOperacionaisTotais").asText())).isEqualByComparingTo("620.00");
        assertThat(bd(root.get("indicadores").get("resultadoOperacional").asText())).isEqualByComparingTo("260.00");
        assertThat(bd(root.get("indicadores").get("margemOperacionalPercentual").asText())).isEqualByComparingTo("16.25");
        assertThat(bd(root.get("indicadores").get("resultadoFinanceiroLiquido").asText())).isEqualByComparingTo("20.00");
        assertThat(bd(root.get("indicadores").get("resultadoAntesTributos").asText())).isEqualByComparingTo("280.00");
        assertThat(bd(root.get("indicadores").get("tributosSobreLucro").asText())).isEqualByComparingTo("40.00");
        assertThat(bd(root.get("indicadores").get("resultadoLiquidoGerencial").asText())).isEqualByComparingTo("240.00");
        assertThat(bd(root.get("indicadores").get("margemLiquidaPercentual").asText())).isEqualByComparingTo("15.00");
        assertThat(bd(root.get("indicadores").get("despesasFixas").asText())).isEqualByComparingTo("450.00");
        assertThat(bd(root.get("indicadores").get("despesasVariaveis").asText())).isEqualByComparingTo("180.00");
        assertThat(bd(root.get("indicadores").get("custosVariaveisTotais").asText())).isEqualByComparingTo("900.00");
        assertThat(bd(root.get("indicadores").get("margemContribuicaoValor").asText())).isEqualByComparingTo("700.00");
        assertThat(bd(root.get("indicadores").get("margemContribuicaoPercentual").asText())).isEqualByComparingTo("43.75");
        assertThat(bd(root.get("indicadores").get("pontoEquilibrioContabilReceita").asText())).isEqualByComparingTo("1028.57");
        assertThat(bd(linhaPorNumero(root.get("linhas"), 12).get("valor").asText())).isEqualByComparingTo("-20.00");
        assertThat(bd(linhaPorNumero(root.get("linhas"), 13).get("valor").asText())).isEqualByComparingTo("260.00");
        assertThat(bd(linhaPorNumero(root.get("linhas"), 14).get("valor").asText())).isEqualByComparingTo("20.00");
        assertThat(bd(linhaPorNumero(root.get("linhas"), 15).get("valor").asText())).isEqualByComparingTo("280.00");

        // Verifica hierarquia Sintetica (4.10) -> Analitica (4.10.01) na Linha 8
        JsonNode linha8 = linhaPorNumero(root.get("linhas"), 8);
        assertThat(bd(linha8.get("valor").asText())).isEqualByComparingTo("150.00");
        JsonNode contaSinteticaNode = findContaByCode(linha8.get("contas"), "4.10");
        assertThat(contaSinteticaNode).isNotNull();
        assertThat(contaSinteticaNode.get("sintetica").asBoolean()).isTrue();
        assertThat(bd(contaSinteticaNode.get("valor").asText())).isEqualByComparingTo("150.00");
        assertThat(contaSinteticaNode.get("filhas").size()).isEqualTo(1);
        assertThat(contaSinteticaNode.get("filhas").get(0).get("codigo").asText()).isEqualTo("4.10.01");

        // Verifica filtro por Centro de Custo (CC-ADM deve trazer Aluguel 250, Folha 200, Multa -20 na L12, Rendimento +50 na L14, IRPJ 40)
        String dreAdmJson = mvc.perform(get("/api/financeiro/dre")
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", "2026-06")
                        .param("centroCustoId", String.valueOf(ccAdm.getIdCentroCusto()))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode rootAdm = json.readTree(dreAdmJson);
        assertThat(bd(rootAdm.get("indicadores").get("receitaBruta").asText())).isEqualByComparingTo("0.00");
        assertThat(bd(linhaPorNumero(rootAdm.get("linhas"), 9).get("valor").asText())).isEqualByComparingTo("250.00");
        assertThat(bd(linhaPorNumero(rootAdm.get("linhas"), 8).get("valor").asText())).isEqualByComparingTo("0.00");
        assertThat(bd(linhaPorNumero(rootAdm.get("linhas"), 12).get("valor").asText())).isEqualByComparingTo("-20.00");
        assertThat(bd(linhaPorNumero(rootAdm.get("linhas"), 14).get("valor").asText())).isEqualByComparingTo("50.00");

        // Verifica drill-down na conta sintetica 4.10 (retorna os lancamentos da filha analitica 4.10.01)
        String drillJson = mvc.perform(get("/api/financeiro/dre/lancamentos")
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", "2026-06")
                        .param("numeroLinha", "8")
                        .param("planoContaId", String.valueOf(idSintetica))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode drillRoot = json.readTree(drillJson);
        assertThat(drillRoot.get("totalLancamentos").asInt()).isEqualTo(1);
        assertThat(bd(drillRoot.get("somaValorCompetencia").asText())).isEqualByComparingTo("150.00");
        assertThat(drillRoot.get("lancamentos").get(0).get("planoContaCodigo").asText()).isEqualTo("4.10.01");

        // Verifica reconciliacao exata do drill-down na Linha 14 (+/- Resultado Financeiro Liquido = +50.00 - 30.00 = +20.00)
        String drillFinJson = mvc.perform(get("/api/financeiro/dre/lancamentos")
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", "2026-06")
                        .param("numeroLinha", "14")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode drillFinRoot = json.readTree(drillFinJson);
        assertThat(drillFinRoot.get("totalLancamentos").asInt()).isEqualTo(2);
        assertThat(bd(drillFinRoot.get("somaValorCompetencia").asText())).isEqualByComparingTo("20.00");
    }

    @Test
    void vendaParceladaEObrigacaoPagaEmMesPosteriorRespeitamRegimeDeCompetenciaSemDuplicarNaDre() throws Exception {
        TenantContext t = createTenant("COMPCAIXA");
        String token = loginToken(t.dono.getLogin());
        bootstrapFoundation(token);

        PlanoContasGerencial contaVendas = contaPorCodigo(t, "1.01");
        PlanoContasGerencial contaAluguel = contaPorCodigo(t, "5.01");
        PlanoContasGerencial contaEnergia = contaPorCodigo(t, "6.01");

        Categoria cat = saveCategoria(t, "Eletro");
        Fornecedor forn = saveFornecedor(t, "Distribuidora Nacional");
        Cliente cli = saveCliente(t, "Cliente Parcelado");
        FormaPagamento boleto = saveForma(t, "Boleto Parcelado", 2);
        Produto prod = saveProduto(t, cat, forn, "Refrigerador", bd("400.00"), bd("1000.00"));
        TipoDespesa td = saveTipoDespesa(t, "Ocupacao");

        LocalDate jul = LocalDate.of(2026, 7, 10);
        LocalDate ago = LocalDate.of(2026, 8, 10);
        LocalDate set = LocalDate.of(2026, 9, 10);

        // Vendas realizadas em Julho/2026 no total de R$ 3.000,00 (3 vendas de R$ 1.000, CMV = 1.200)
        // Venda 1 paga em Julho, Venda 2 paga em Agosto, Venda 3 paga em Setembro via ContaReceber
        Venda vendaJul1 = saveVenda(t, cli, prod, 1, bd("1000.00"), bd("400.00"),
                jul, VendaStatus.FINALIZADA, contaVendas.getIdPlanoConta());
        Venda vendaJul2 = saveVenda(t, cli, prod, 1, bd("1000.00"), bd("400.00"),
                jul, VendaStatus.FINALIZADA, contaVendas.getIdPlanoConta());
        Venda vendaJul3 = saveVenda(t, cli, prod, 1, bd("1000.00"), bd("400.00"),
                jul, VendaStatus.FINALIZADA, contaVendas.getIdPlanoConta());

        saveContaReceber(t, vendaJul1.getIdVenda(), cli.getIdCliente(), boleto.getIdFormaPagamento(),
                bd("1000.00"), bd("1000.00"), FinanceiroStatus.PAGO, jul, jul.atTime(15, 0), contaVendas.getIdPlanoConta());
        saveContaReceber(t, vendaJul2.getIdVenda(), cli.getIdCliente(), boleto.getIdFormaPagamento(),
                bd("1000.00"), bd("1000.00"), FinanceiroStatus.PAGO, jul, ago.atTime(15, 0), contaVendas.getIdPlanoConta());
        saveContaReceber(t, vendaJul3.getIdVenda(), cli.getIdCliente(), boleto.getIdFormaPagamento(),
                bd("1000.00"), bd("1000.00"), FinanceiroStatus.PAGO, jul, set.atTime(15, 0), contaVendas.getIdPlanoConta());

        // Venda cancelada e venda aguardando caixa em Julho (NAO podem entrar na DRE)
        saveVenda(t, cli, prod, 1, bd("1000.00"), bd("400.00"), jul, VendaStatus.CANCELADA, contaVendas.getIdPlanoConta());
        saveVenda(t, cli, prod, 1, bd("1000.00"), bd("400.00"), jul, VendaStatus.AGUARDANDO_CAIXA, contaVendas.getIdPlanoConta());

        // Despesa de competencia Julho/2026 (R$ 500) mas paga somente em Agosto/2026
        saveDespesa(t, td, "Aluguel Competencia Julho", bd("500.00"), jul, ago, 1, contaAluguel.getIdPlanoConta());
        // Despesa cancelada em Julho (status = 0 -> ignorada)
        saveDespesa(t, td, "Despesa Cancelada", bd("999.00"), jul, jul, 0, contaAluguel.getIdPlanoConta());

        // ContaPagar de competencia Julho/2026 (R$ 400) paga somente em Agosto/2026
        saveContaPagar(t, forn.getIdFornecedor(), cat.getIdCategoria(), "Energia Competencia Julho",
                bd("400.00"), jul, FinanceiroStatus.PAGO, ago.atTime(11, 0), contaEnergia.getIdPlanoConta());

        // Nota Fiscal de Entrada de Estoque em Julho (R$ 5.000) -> NAO pode entrar como despesa na DRE
        NotaFiscalEntrada nf = new NotaFiscalEntrada();
        nf.setEmpresaId(t.empresaId);
        nf.setFilialId(t.filialId);
        nf.setFornecedor(forn);
        nf.setNumero("NF-7001");
        nf.setSerie("1");
        nf.setDataEmissao(jul.atTime(9, 0));
        nf.setDataCompetencia(jul);
        nf.setValorTotal(bd("5000.00"));
        notaFiscalEntradaRepository.save(nf);

        // 1. Apura Julho/2026:
        // Receita Liquida = 3000.00 | CMV = 1200.00 | Lucro Bruto = 1800.00 | Despesas = 900.00 | Resultado Liquido = 900.00
        // Caixa Realizado Julho: Entradas = 1000.00 (1a parcela), Saidas = 0.00 -> Saldo Caixa = 1000.00
        String julJson = mvc.perform(get("/api/financeiro/dre")
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", "2026-07")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode julRoot = json.readTree(julJson);
        assertThat(bd(julRoot.get("indicadores").get("receitaLiquida").asText())).isEqualByComparingTo("3000.00");
        assertThat(bd(julRoot.get("indicadores").get("cmv").asText())).isEqualByComparingTo("1200.00");
        assertThat(bd(julRoot.get("indicadores").get("despesasOperacionaisTotais").asText())).isEqualByComparingTo("900.00");
        assertThat(bd(julRoot.get("indicadores").get("resultadoLiquidoGerencial").asText())).isEqualByComparingTo("900.00");
        assertThat(bd(julRoot.get("indicadores").get("entradasRealizadasCaixa").asText())).isEqualByComparingTo("1000.00");
        assertThat(bd(julRoot.get("indicadores").get("saidasRealizadasCaixa").asText())).isEqualByComparingTo("0.00");

        // 2. Apura Agosto/2026:
        // Receita DRE = 0.00 (parcela 2 recebida nao gera nova receita!)
        // Despesa DRE = 0.00 (pagamento do aluguel e conta de julho nao gera segunda despesa!)
        // Caixa Realizado Agosto: Entradas = 1000.00 (2a parcela), Saidas = 900.00 (500 + 400 pagos em ago) -> Saldo Caixa = +100.00
        String agoJson = mvc.perform(get("/api/financeiro/dre")
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", "2026-08")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode agoRoot = json.readTree(agoJson);
        assertThat(bd(agoRoot.get("indicadores").get("receitaBruta").asText())).isEqualByComparingTo("0.00");
        assertThat(bd(agoRoot.get("indicadores").get("receitaLiquida").asText())).isEqualByComparingTo("0.00");
        assertThat(bd(agoRoot.get("indicadores").get("despesasOperacionaisTotais").asText())).isEqualByComparingTo("0.00");
        assertThat(bd(agoRoot.get("indicadores").get("resultadoLiquidoGerencial").asText())).isEqualByComparingTo("0.00");
        assertThat(bd(agoRoot.get("indicadores").get("entradasRealizadasCaixa").asText())).isEqualByComparingTo("1000.00");
        assertThat(bd(agoRoot.get("indicadores").get("saidasRealizadasCaixa").asText())).isEqualByComparingTo("900.00");
        assertThat(bd(agoRoot.get("indicadores").get("saldoRealizadoCaixa").asText())).isEqualByComparingTo("100.00");
    }

    @Test
    void reclassificacaoAutorizadaLancamentosSemClassificacaoEReprocessamentoCompetenciaAtualizamDreEDrillDown() throws Exception {
        TenantContext t = createTenant("RECLASS");
        String token = loginToken(t.dono.getLogin());
        bootstrapFoundation(token);

        PlanoContasGerencial contaVendas = contaPorCodigo(t, "1.01");
        PlanoContasGerencial contaComercial = contaPorCodigo(t, "4.01");
        PlanoContasGerencial contaAdm = contaPorCodigo(t, "5.01");
        CentroCusto ccVendas = centroPorCodigo(t, "CC-LOJA");

        Categoria cat = saveCategoria(t, "Acessorios");
        Fornecedor forn = saveFornecedor(t, "Fornecedor Acessorios");
        Cliente cli = saveCliente(t, "Cliente Reclass");
        Produto prod = saveProduto(t, cat, forn, "Bolsa Couro", bd("80.00"), bd("200.00"));
        TipoDespesa td = saveTipoDespesa(t, "Servicos");

        LocalDate out = LocalDate.of(2026, 10, 12);
        saveVenda(t, cli, prod, 5, bd("200.00"), bd("80.00"), out, VendaStatus.FINALIZADA, contaVendas.getIdPlanoConta());

        // Despesa 1: classificada inicialmente em 5.01 (Administrativa - Linha 9) = R$ 180.00
        Despesa despReclassificavel = saveDespesa(t, td, "Consultoria de Vitrine", bd("180.00"),
                out, out, 1, contaAdm.getIdPlanoConta());

        // Despesa 2: SEM classificacao (planoContaId = null) = R$ 70.00 -> deve aparecer na Linha 10 e gerar alerta
        Despesa despSemConta = saveDespesa(t, td, "Despesa Pendente Classificacao", bd("70.00"),
                out, out, 1, null);

        String antesJson = mvc.perform(get("/api/financeiro/dre")
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", "2026-10")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode antesRoot = json.readTree(antesJson);
        assertThat(bd(linhaPorNumero(antesRoot.get("linhas"), 8).get("valor").asText())).isEqualByComparingTo("0.00");
        assertThat(bd(linhaPorNumero(antesRoot.get("linhas"), 9).get("valor").asText())).isEqualByComparingTo("180.00");
        assertThat(bd(linhaPorNumero(antesRoot.get("linhas"), 10).get("valor").asText())).isEqualByComparingTo("70.00");
        assertThat(antesRoot.get("lancamentosSemClassificacao").asLong()).isEqualTo(1L);
        assertThat(bd(antesRoot.get("valorSemClassificacao").asText())).isEqualByComparingTo("70.00");

        // Reclassifica manualmente a Despesa 1 de 5.01 (Linha 9) para 4.01 (Linha 8) e classifica a Despesa 2 em 5.01
        ReclassificacaoManualRequestDTO reclass1 = new ReclassificacaoManualRequestDTO(
                OrigemClassificacaoFinanceira.DESPESA,
                despReclassificavel.getIdDespesa(),
                contaComercial.getIdPlanoConta(),
                ccVendas.getIdCentroCusto(),
                "Reclassificacao gerencial de vitrine para comercial");
        mvc.perform(post("/api/financeiro/classificacoes/manual")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reclass1)))
                .andExpect(status().isOk());

        ReclassificacaoManualRequestDTO reclass2 = new ReclassificacaoManualRequestDTO(
                OrigemClassificacaoFinanceira.DESPESA,
                despSemConta.getIdDespesa(),
                contaAdm.getIdPlanoConta(),
                ccVendas.getIdCentroCusto(),
                "Classificacao de despesa pendente");
        mvc.perform(post("/api/financeiro/classificacoes/manual")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reclass2)))
                .andExpect(status().isOk());

        // Recalcula consolidacao de competencia e verifica que a DRE reflete a nova classificacao
        mvc.perform(post("/api/financeiro/consolidacao/competencia/recalcular")
                        .param("competencia", "2026-10")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        String depoisJson = mvc.perform(get("/api/financeiro/dre")
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", "2026-10")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode depoisRoot = json.readTree(depoisJson);
        assertThat(bd(linhaPorNumero(depoisRoot.get("linhas"), 8).get("valor").asText())).isEqualByComparingTo("180.00");
        assertThat(bd(linhaPorNumero(depoisRoot.get("linhas"), 9).get("valor").asText())).isEqualByComparingTo("70.00");
        assertThat(bd(linhaPorNumero(depoisRoot.get("linhas"), 10).get("valor").asText())).isEqualByComparingTo("0.00");
        assertThat(depoisRoot.get("lancamentosSemClassificacao").asLong()).isZero();
    }

    @Test
    void analiseHorizontalPeriodosTrimestralSemestralAnualPersonalizadoEvolucaoMensalEComparativoFiliais() throws Exception {
        TenantContext t = createTenant("MULTIFILIAL");
        String token = loginToken(t.dono.getLogin());
        bootstrapFoundation(token);

        // Cria 2a filial ativa na mesma empresa
        Empresa emp = empresaRepository.findById(t.empresaId).orElseThrow();
        Filial filial2 = new Filial();
        filial2.setEmpresa(emp);
        filial2.setNome("Filial Shopping " + t.empresaId);
        filial2.setCodigo("F-SHOP-" + t.empresaId);
        filial2.setStatus(1);
        filial2 = filialRepository.save(filial2);

        Usuario donoFilial2 = ensureUser("dono_f2_" + t.empresaId, "Dono Filial 2", "DONO_GERENTE", t.empresaId, filial2.getIdFilial());
        TenantContext t2 = new TenantContext(t.empresaId, filial2.getIdFilial(), donoFilial2);
        String tokenF2 = loginToken(donoFilial2.getLogin());
        bootstrapFoundation(tokenF2);

        PlanoContasGerencial contaVendasF1 = contaPorCodigo(t, "1.01");
        PlanoContasGerencial contaDespAdmF1 = contaPorCodigo(t, "5.01");
        PlanoContasGerencial contaVendasF2 = contaPorCodigo(t2, "1.01");
        PlanoContasGerencial contaDespAdmF2 = contaPorCodigo(t2, "5.01");

        Categoria cat1 = saveCategoria(t, "Calcados");
        Fornecedor forn1 = saveFornecedor(t, "Calcados Brasil");
        Cliente cli1 = saveCliente(t, "Cliente Matriz");
        Produto prod1 = saveProduto(t, cat1, forn1, "Tenis Corrida", bd("100.00"), bd("250.00"));
        TipoDespesa td1 = saveTipoDespesa(t, "Administrativo");

        Categoria cat2 = saveCategoria(t2, "Calcados F2");
        Fornecedor forn2 = saveFornecedor(t2, "Calcados Brasil F2");
        Cliente cli2 = saveCliente(t2, "Cliente Filial 2");
        Produto prod2 = saveProduto(t2, cat2, forn2, "Tenis Corrida F2", bd("100.00"), bd("250.00"));
        TipoDespesa td2 = saveTipoDespesa(t2, "Administrativo F2");

        // Mes 1 (Abril/2026 - Matriz): Venda R$ 1.000 (4 un), CMV R$ 400, Despesa R$ 200 -> Lucro Liquido = R$ 400
        LocalDate abr = LocalDate.of(2026, 4, 15);
        saveVenda(t, cli1, prod1, 4, bd("250.00"), bd("100.00"), abr, VendaStatus.FINALIZADA, contaVendasF1.getIdPlanoConta());
        saveDespesa(t, td1, "Aluguel Abril", bd("200.00"), abr, abr, 1, contaDespAdmF1.getIdPlanoConta());

        // Mes 2 (Maio/2026 - Matriz): Venda R$ 1.500 (6 un, +50% AH!), CMV R$ 600, Despesa R$ 200 -> Lucro Liquido = R$ 700 (+75% AH!)
        LocalDate mai = LocalDate.of(2026, 5, 15);
        saveVenda(t, cli1, prod1, 6, bd("250.00"), bd("100.00"), mai, VendaStatus.FINALIZADA, contaVendasF1.getIdPlanoConta());
        saveDespesa(t, td1, "Aluguel Maio", bd("200.00"), mai, mai, 1, contaDespAdmF1.getIdPlanoConta());

        // Mes 2 (Maio/2026 - Filial 2): Venda R$ 500 (2 un), CMV R$ 200, Despesa R$ 100 -> Lucro Liquido = R$ 200
        saveVenda(t2, cli2, prod2, 2, bd("250.00"), bd("100.00"), mai, VendaStatus.FINALIZADA, contaVendasF2.getIdPlanoConta());
        saveDespesa(t2, td2, "Condominio Shopping Maio", bd("100.00"), mai, mai, 1, contaDespAdmF2.getIdPlanoConta());

        // 1. Verifica Analise Horizontal em Maio/2026 comparado com Abril/2026 na Matriz:
        String maioJson = mvc.perform(get("/api/financeiro/dre")
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", "2026-05")
                        .param("compararPeriodoAnterior", "true")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode maioRoot = json.readTree(maioJson);
        JsonNode l1 = linhaPorNumero(maioRoot.get("linhas"), 1);
        assertThat(bd(l1.get("valor").asText())).isEqualByComparingTo("1500.00");
        assertThat(bd(l1.get("valorComparacao").asText())).isEqualByComparingTo("1000.00");
        assertThat(bd(l1.get("variacaoAbsoluta").asText())).isEqualByComparingTo("500.00");
        assertThat(bd(l1.get("analiseHorizontalPercentual").asText())).isEqualByComparingTo("50.00");
        assertThat(l1.get("statusVariacao").asText()).isEqualTo("FAVORAVEL");

        // 2. Verifica apuracao TRIMESTRAL (2o Trimestre 2026 na Matriz = Abril + Maio = 1000 + 1500 = 2500)
        String trimJson = mvc.perform(get("/api/financeiro/dre")
                        .param("tipoPeriodo", "TRIMESTRAL")
                        .param("ano", "2026")
                        .param("trimestre", "2")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(bd(json.readTree(trimJson).get("indicadores").get("receitaLiquida").asText()))
                .isEqualByComparingTo("2500.00");

        // 3. Verifica apuracao SEMESTRAL, ANUAL e PERSONALIZADO
        String semJson = mvc.perform(get("/api/financeiro/dre")
                        .param("tipoPeriodo", "SEMESTRAL")
                        .param("ano", "2026")
                        .param("semestre", "1")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(bd(json.readTree(semJson).get("indicadores").get("receitaLiquida").asText()))
                .isEqualByComparingTo("2500.00");

        String anualJson = mvc.perform(get("/api/financeiro/dre")
                        .param("tipoPeriodo", "ANUAL")
                        .param("ano", "2026")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(bd(json.readTree(anualJson).get("indicadores").get("receitaLiquida").asText()))
                .isEqualByComparingTo("2500.00");

        String persJson = mvc.perform(get("/api/financeiro/dre")
                        .param("tipoPeriodo", "PERSONALIZADO")
                        .param("dataInicio", "2026-04-01")
                        .param("dataFim", "2026-05-31")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(bd(json.readTree(persJson).get("indicadores").get("receitaLiquida").asText()))
                .isEqualByComparingTo("2500.00");

        // 4. Verifica Consolidado Empresa (consolidarFiliais=true em Maio/2026 = Matriz 1500 + Filial 2 500 = 2000)
        String consJson = mvc.perform(get("/api/financeiro/dre")
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", "2026-05")
                        .param("consolidarFiliais", "true")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode consRoot = json.readTree(consJson);
        assertThat(consRoot.get("consolidadoEmpresa").asBoolean()).isTrue();
        assertThat(bd(consRoot.get("indicadores").get("receitaLiquida").asText())).isEqualByComparingTo("2000.00");
        assertThat(bd(consRoot.get("indicadores").get("resultadoLiquidoGerencial").asText())).isEqualByComparingTo("900.00");

        // 4b. Verifica que o drill-down por conta (1.01) em modo consolidarFiliais=true soma ambas as filiais (1500 + 500 = 2000)
        String drillConsJson = mvc.perform(get("/api/financeiro/dre/lancamentos")
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", "2026-05")
                        .param("consolidarFiliais", "true")
                        .param("numeroLinha", "1")
                        .param("planoContaId", String.valueOf(contaVendasF1.getIdPlanoConta()))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode drillConsRoot = json.readTree(drillConsJson);
        assertThat(drillConsRoot.get("totalLancamentos").asInt()).isEqualTo(2);
        assertThat(bd(drillConsRoot.get("somaValorCompetencia").asText())).isEqualByComparingTo("2000.00");

        // 5. Verifica endpoint /comparativo-filiais em Maio/2026 (participacao Matriz = 75%, Filial 2 = 25%)
        String compFiliaisJson = mvc.perform(get("/api/financeiro/dre/comparativo-filiais")
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", "2026-05")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode cfRoot = json.readTree(compFiliaisJson);
        assertThat(cfRoot.get("filiais").size()).isEqualTo(2);
        assertThat(bd(cfRoot.get("consolidadoEmpresa").get("receitaLiquida").asText())).isEqualByComparingTo("2000.00");
        assertThat(bd(cfRoot.get("filiais").get(0).get("participacaoReceitaLiquidaPercentual").asText())).isEqualByComparingTo("75.00");
        assertThat(bd(cfRoot.get("filiais").get(1).get("participacaoReceitaLiquidaPercentual").asText())).isEqualByComparingTo("25.00");

        // 6. Verifica endpoint /evolucao (Abril a Maio/2026)
        String evolJson = mvc.perform(get("/api/financeiro/dre/evolucao")
                        .param("competenciaInicio", "2026-04")
                        .param("competenciaFim", "2026-05")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode evolRoot = json.readTree(evolJson);
        assertThat(evolRoot.get("quantidadeMeses").asInt()).isEqualTo(2);
        assertThat(bd(evolRoot.get("receitaLiquidaAcumulada").asText())).isEqualByComparingTo("2500.00");
        assertThat(evolRoot.get("tendenciaReceita").asText()).isEqualTo("CRESCENTE");
        assertThat(evolRoot.get("tendenciaResultado").asText()).isEqualTo("CRESCENTE");
    }

    @Test
    void isolamentoMultiempresaRbacExportacaoAuditadaCsvEJson() throws Exception {
        TenantContext tenantA = createTenant("TENANTA");
        TenantContext tenantB = createTenant("TENANTB");

        String tokenDonoA = loginToken(tenantA.dono.getLogin());
        String tokenDonoB = loginToken(tenantB.dono.getLogin());

        bootstrapFoundation(tokenDonoA);
        bootstrapFoundation(tokenDonoB);

        // Tenant B tenta consultar a filial do Tenant A -> deve receber 404 (EntityNotFoundException)
        mvc.perform(get("/api/financeiro/dre")
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", "2026-06")
                        .param("filialFiltroId", String.valueOf(tenantA.filialId))
                        .header("Authorization", bearer(tokenDonoB)))
                .andExpect(status().isNotFound());

        // Perfis operacionais sem permissao financeira (VENDEDOR, CAIXA, SUPERVISOR) -> 403 Forbidden
        for (String roleNegada : List.of("VENDEDOR", "CAIXA", "SUPERVISOR")) {
            Usuario u = ensureUser("usr_" + roleNegada.toLowerCase() + "_" + tenantA.empresaId,
                    "Usuario " + roleNegada, roleNegada, tenantA.empresaId, tenantA.filialId);
            String tok = loginToken(u.getLogin());
            mvc.perform(get("/api/financeiro/dre")
                            .header("Authorization", bearer(tok)))
                    .andExpect(status().isForbidden());
        }

        // Perfil AUDITOR: pode visualizar a DRE (200), mas NAO pode exportar (403)
        Usuario auditor = ensureUser("usr_auditor_" + tenantA.empresaId,
                "Auditor DRE", "AUDITOR", tenantA.empresaId, tenantA.filialId);
        String tokenAuditor = loginToken(auditor.getLogin());
        mvc.perform(get("/api/financeiro/dre")
                        .param("competencia", "2026-06")
                        .header("Authorization", bearer(tokenAuditor)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/financeiro/dre/exportar")
                        .param("formato", "CSV")
                        .param("competencia", "2026-06")
                        .header("Authorization", bearer(tokenAuditor)))
                .andExpect(status().isForbidden());

        // Perfil CONTADOR: pode visualizar (200) e exportar CSV e JSON (200) com trilha de auditoria gravada
        Usuario contador = ensureUser("usr_contador_" + tenantA.empresaId,
                "Contador DRE", "CONTADOR", tenantA.empresaId, tenantA.filialId);
        String tokenContador = loginToken(contador.getLogin());

        MvcResult csvRes = mvc.perform(get("/api/financeiro/dre/exportar")
                        .param("formato", "CSV")
                        .param("competencia", "2026-06")
                        .header("Authorization", bearer(tokenContador)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(csvRes.getResponse().getContentAsString()).contains("1. Receita Bruta de Vendas");
        assertThat(csvRes.getResponse().getContentAsString()).contains("17. (=) Resultado Liquido Gerencial");

        MvcResult jsonRes = mvc.perform(get("/api/financeiro/dre/exportar")
                        .param("formato", "JSON")
                        .param("competencia", "2026-06")
                        .header("Authorization", bearer(tokenContador)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(json.readTree(jsonRes.getResponse().getContentAsString()).get("linhas").size()).isEqualTo(17);

        String auditoriaBody = mvc.perform(get("/api/financeiro/auditoria")
                        .header("Authorization", bearer(tokenDonoA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode finEvents = json.readTree(auditoriaBody);
        boolean foundDreExportFin = false;
        for (JsonNode ev : finEvents) {
            if ("DRE_EXPORTADA".equals(ev.get("tipoEvento").asText())) {
                foundDreExportFin = true;
                break;
            }
        }
        assertThat(foundDreExportFin).isTrue();

        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).anyMatch(l -> "DRE_EXPORTADA".equals(l.getAcao())
                && tenantA.empresaId.equals(l.getEmpresaId()));
    }

    @Test
    void devolucaoEmMesPosteriorReverteCmvComCustoOriginalEFallbackDeCompetenciaPorVencimentoGeraAlertaERastreabilidade() throws Exception {
        TenantContext t = createTenant("DEVPOST");
        String token = loginToken(t.dono.getLogin());
        bootstrapFoundation(token);

        PlanoContasGerencial contaVendas = contaPorCodigo(t, "1.01");
        PlanoContasGerencial contaAluguel = contaPorCodigo(t, "5.01");

        Categoria cat = saveCategoria(t, "Moda");
        Fornecedor forn = saveFornecedor(t, "Confecoes Norte");
        Cliente cli = saveCliente(t, "Cliente Devolucao Posterior");
        Produto prod = saveProduto(t, cat, forn, "Casaco Inverno", bd("40.00"), bd("100.00"));
        TipoDespesa td = saveTipoDespesa(t, "Ocupacao");

        LocalDate jan = LocalDate.of(2026, 1, 20);
        LocalDate fev = LocalDate.of(2026, 2, 10);

        // Venda em Janeiro/2026: 5 unidades x R$ 100 = R$ 500, custo original na venda = R$ 40/un (CMV Jan = R$ 200)
        Venda vendaJan = saveVenda(t, cli, prod, 5, bd("100.00"), bd("40.00"),
                jan, VendaStatus.FINALIZADA, contaVendas.getIdPlanoConta());

        // Em Fevereiro/2026 o custo de reposicao do produto sobe para R$ 65,00 no cadastro
        prod.setCusto(bd("65.00"));
        produtoRepository.save(prod);

        // Em Fevereiro/2026 ocorre devolucao de 2 unidades da venda de Janeiro, com custoUnitario = null no item da devolucao
        // O motor deve buscar a vendaOriginal de Janeiro e reverter 2 x R$ 40,00 = -R$ 80,00 (e NAO 2 x 65 = 130 ou zerar o CMV!)
        saveDevolucao(t, vendaJan, prod, 2, bd("100.00"), null, bd("200.00"), fev);

        // Despesa em Fevereiro/2026 SEM dataCompetencia e SEM dataEmissao (caindo em fallback por dataVencimento)
        Despesa despVenc = new Despesa();
        despVenc.setDescricao("Taxa Condominial Sem Competencia");
        despVenc.setValor(bd("120.00"));
        despVenc.setDataCompetencia(null);
        despVenc.setDataEmissao(null);
        despVenc.setDataVencimento(fev);
        despVenc.setDataPagamento(null);
        despVenc.setStatus(1);
        despVenc.setEmpresaId(t.empresaId);
        despVenc.setFilialId(t.filialId);
        despVenc.setTipoDespesa(td);
        despVenc.setPlanoContaId(contaAluguel.getIdPlanoConta());
        despesaRepository.save(despVenc);

        String fevJson = mvc.perform(get("/api/financeiro/dre")
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", "2026-02")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode fevRoot = json.readTree(fevJson);

        // Em Fevereiro/2026:
        // Receita Bruta = 0.00 | Devolucoes = 200.00 | Receita Liquida = -200.00
        // CMV = -80.00 (reversao pelo custo original de R$ 40/un da venda de Janeiro!)
        // Lucro Bruto = -200.00 - (-80.00) = -120.00
        assertThat(bd(fevRoot.get("indicadores").get("receitaLiquida").asText())).isEqualByComparingTo("-200.00");
        assertThat(bd(fevRoot.get("indicadores").get("cmv").asText())).isEqualByComparingTo("-80.00");
        assertThat(bd(fevRoot.get("indicadores").get("lucroBruto").asText())).isEqualByComparingTo("-120.00");
        assertThat(bd(linhaPorNumero(fevRoot.get("linhas"), 6).get("valor").asText())).isEqualByComparingTo("-80.00");

        // Verifica que os avisosLimitacao alertam sobre o fallback de competencia pela data de vencimento
        assertThat(fevRoot.get("diagnostico").get("avisosLimitacao").toString())
                .contains("estimado(s) pela data de vencimento");

        // Verifica que o drill-down da Linha 9 mostra a rastreabilidade de competencia estimada por vencimento
        String drillFevL9 = mvc.perform(get("/api/financeiro/dre/lancamentos")
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", "2026-02")
                        .param("numeroLinha", "9")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode l9Root = json.readTree(drillFevL9);
        assertThat(l9Root.get("lancamentos").get(0).get("descricao").asText())
                .contains("[Competencia estimada por vencimento]");
        assertThat(l9Root.get("lancamentos").get(0).get("statusOperacional").asText())
                .contains("COMPETENCIA_ESTIMADA_VENCIMENTO");

        // Verifica que ConsolidacaoCompetenciaService em Fevereiro/2026 tambem preserva cmvCompetencia = -80.00
        String consFev = mvc.perform(get("/api/financeiro/consolidacao/competencia")
                        .param("competencia", "2026-02")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(bd(json.readTree(consFev).get("cmvCompetencia").asText())).isEqualByComparingTo("-80.00");
    }

    // =========================================================================
    // Helpers de Teste
    // =========================================================================

    private void bootstrapFoundation(String token) throws Exception {
        mvc.perform(post("/api/financeiro/plano-contas/inicializar-padrao")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/financeiro/centros-custo/inicializar-padrao")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    private JsonNode linhaPorNumero(JsonNode linhas, int numero) {
        for (JsonNode l : linhas) {
            if (l.get("numeroLinha").asInt() == numero) {
                return l;
            }
        }
        throw new AssertionError("Linha da DRE nao encontrada: " + numero);
    }

    private JsonNode findContaByCode(JsonNode contas, String codigo) {
        for (JsonNode c : contas) {
            if (codigo.equals(c.get("codigo").asText())) {
                return c;
            }
        }
        return null;
    }

    private TenantContext createTenant(String tag) {
        int id = SEQ.incrementAndGet();
        Empresa emp = new Empresa();
        emp.setRazaoSocial("Empresa Lote2 " + tag + " " + id);
        emp.setNomeFantasia("Apex L2 " + tag + " " + id);
        emp.setCnpj(String.format("98%012d", id));
        emp.setStatus(1);
        emp = empresaRepository.save(emp);

        Filial fil = new Filial();
        fil.setEmpresa(emp);
        fil.setNome("Filial " + tag + " " + id);
        fil.setCodigo("F2-" + tag + "-" + id);
        fil.setStatus(1);
        fil = filialRepository.save(fil);

        String login = ("dono_l2_" + tag + "_" + id).toLowerCase();
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
        end.setLogradouro("Rua DRE");
        end.setUf("SP");
        end.setNumero("200");
        end.setEmpresaId(t.empresaId);
        end.setFilialId(t.filialId);

        Fornecedor f = new Fornecedor();
        f.setStatus(1);
        f.setRazaoSocial(razao);
        f.setNomeFantasia(razao.length() > 45 ? razao.substring(0, 45) : razao);
        f.setCnpj(String.format("87%012d", id));
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
        p.setCodigoBarras("788" + String.format("%010d", id));
        p.setCodigoInterno("DRE-INT-" + id);
        p.setStatus(1);
        p.setEmpresaId(t.empresaId);
        p.setFilialId(t.filialId);
        p.setCategoria(cat);
        p.setFornecedor(forn);
        return produtoRepository.save(p);
    }

    private Despesa saveDespesa(TenantContext t, TipoDespesa tipo, String desc, BigDecimal valor,
            LocalDate competencia, LocalDate pagamento, int status, Long planoContaId) {
        return saveDespesaComCentro(t, tipo, desc, valor, competencia, pagamento, status, planoContaId, null);
    }

    private Despesa saveDespesaComCentro(TenantContext t, TipoDespesa tipo, String desc, BigDecimal valor,
            LocalDate competencia, LocalDate pagamento, int status, Long planoContaId, Long centroCustoId) {
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
        d.setCentroCustoId(centroCustoId);
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
        return saveVendaComDesconto(t, cli, prod, qtd, precoUnit, custoUnit, BigDecimal.ZERO,
                competencia, statusOp, planoContaId, null);
    }

    private Venda saveVendaComDesconto(TenantContext t, Cliente cli, Produto prod, int qtd, BigDecimal precoUnit,
            BigDecimal custoUnit, BigDecimal desconto, LocalDate competencia, VendaStatus statusOp,
            Long planoContaId, Long centroCustoId) {
        BigDecimal bruto = precoUnit.multiply(BigDecimal.valueOf(qtd));
        BigDecimal liquido = bruto.subtract(desconto);
        Venda v = new Venda();
        v.setUsuario(t.dono);
        v.setCliente(cli);
        v.setStatus(1);
        v.setStatusOperacional(statusOp);
        v.setNumeroVenda("VDRE-" + t.filialId + "-" + SEQ.incrementAndGet());
        v.setValorTotal(liquido);
        v.setDesconto(desconto);
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
        v.setCentroCustoId(centroCustoId);
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
        pv.setPrecoTotal(bruto);
        v.getItens().add(pv);
        return vendaRepository.save(v);
    }

    private VendaDevolucao saveDevolucao(TenantContext t, Venda venda, Produto prod, int qtd,
            BigDecimal valorUnit, BigDecimal custoUnit, BigDecimal valorTotal, LocalDate dataDev) {
        VendaDevolucao dev = new VendaDevolucao();
        dev.setVendaOriginalId(venda.getIdVenda());
        dev.setEmpresaId(t.empresaId);
        dev.setFilialId(t.filialId);
        dev.setTipo(TipoDevolucao.PARCIAL);
        dev.setMotivo("Devolucao teste DRE");
        dev.setValorAjuste(valorTotal);
        dev.setCriadoPor(t.dono.getLogin());
        dev.setCriadoEm(dataDev.atTime(16, 0));

        VendaDevolucaoItem item = new VendaDevolucaoItem();
        item.setDevolucao(dev);
        item.setProdutoOriginalId(prod.getIdProduto());
        item.setQuantidade(qtd);
        item.setValorUnitario(valorUnit);
        item.setCustoUnitario(custoUnit);
        dev.getItens().add(item);
        return vendaDevolucaoRepository.save(dev);
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

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private record TenantContext(Long empresaId, Long filialId, Usuario dono) {
    }
}
