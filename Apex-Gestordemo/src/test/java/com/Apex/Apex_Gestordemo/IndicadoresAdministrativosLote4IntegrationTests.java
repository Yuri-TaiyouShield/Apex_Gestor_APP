package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import Model.Categoria;
import Model.ClassificacaoCusto;
import Model.Cliente;
import Model.ContaFinanceira;
import Model.ContaPagar;
import Model.ContaReceber;
import Model.Despesa;
import Model.Empresa;
import Model.Endereco;
import Model.EstoqueMovimentacao;
import Model.Filial;
import Model.FinanceiroStatus;
import Model.Fornecedor;
import Model.GrupoDreGerencial;
import Model.ModalidadeRecebivel;
import Model.NaturezaContaGerencial;
import Model.NotaFiscalEntrada;
import Model.Perfil;
import Model.PlanoContasGerencial;
import Model.Produto;
import Model.ProdutoVenda;
import Model.ProdutoVendaId;
import Model.TipoContaFinanceira;
import Model.TipoContaGerencial;
import Model.TipoDespesa;
import Model.TipoDevolucao;
import Model.TipoMovimentacaoEstoque;
import Model.Usuario;
import Model.Venda;
import Model.VendaDevolucao;
import Model.VendaDevolucaoItem;
import Model.VendaStatus;
import Repository.BalancoPatrimonialGerencialRepository;
import Repository.CategoriaRepository;
import Repository.ClienteRepository;
import Repository.ContaFinanceiraRepository;
import Repository.ContaPagarRepository;
import Repository.ContaReceberRepository;
import Repository.DespesaRepository;
import Repository.EmpresaRepository;
import Repository.EstoqueMovimentacaoRepository;
import Repository.FilialRepository;
import Repository.FornecedorRepository;
import Repository.NotaFiscalEntradaRepository;
import Repository.ParametroInteligenciaAdministrativaRepository;
import Repository.PerfilRepository;
import Repository.PlanoContasGerencialRepository;
import Repository.ProdutoRepository;
import Repository.TipoDespesaRepository;
import Repository.UsuarioRepository;
import Repository.VendaDevolucaoRepository;
import Repository.VendaRepository;
import Service.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class IndicadoresAdministrativosLote4IntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private PlanoContasGerencialRepository planoContasRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private FornecedorRepository fornecedorRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private EstoqueMovimentacaoRepository estoqueMovimentacaoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private VendaRepository vendaRepository;
    @Autowired private ContaFinanceiraRepository contaFinanceiraRepository;
    @Autowired private ContaReceberRepository contaReceberRepository;
    @Autowired private ContaPagarRepository contaPagarRepository;
    @Autowired private DespesaRepository despesaRepository;
    @Autowired private TipoDespesaRepository tipoDespesaRepository;
    @Autowired private NotaFiscalEntradaRepository notaFiscalEntradaRepository;
    @Autowired private BalancoPatrimonialGerencialRepository balancoRepository;
    @Autowired private ParametroInteligenciaAdministrativaRepository parametroRepository;
    @Autowired private PerfilRepository perfilRepository;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private FilialRepository filialRepository;
    @Autowired private VendaDevolucaoRepository vendaDevolucaoRepository;

    private final ObjectMapper json = new ObjectMapper();

    private static final Long EMPRESA_A = 41L;
    private static final Long FILIAL_A1 = 4101L;
    private static final Long EMPRESA_B = 42L;
    private static final Long FILIAL_B1 = 4201L;

    private String tokenGerenteA;
    private String tokenFinanceiroA;
    private String tokenVendedorA;
    private String tokenGerenteB;

    @BeforeEach
    void setUp() {
        tokenGerenteA = bearer(jwtTokenService.issueAccessToken(
                401L, "Gerente Lote 4", "gerente_lote4", List.of("ROLE_DONO_GERENTE"), EMPRESA_A, FILIAL_A1, Duration.ofMinutes(30)));
        tokenFinanceiroA = bearer(jwtTokenService.issueAccessToken(
                402L, "Financeiro Lote 4", "fin_lote4", List.of("ROLE_FINANCEIRO"), EMPRESA_A, FILIAL_A1, Duration.ofMinutes(30)));
        tokenVendedorA = bearer(jwtTokenService.issueAccessToken(
                403L, "Vendedor Lote 4", "vend_lote4", List.of("ROLE_VENDEDOR"), EMPRESA_A, FILIAL_A1, Duration.ofMinutes(30)));
        tokenGerenteB = bearer(jwtTokenService.issueAccessToken(
                404L, "Gerente Tenant B", "gerente_b_lote4", List.of("ROLE_DONO_GERENTE"), EMPRESA_B, FILIAL_B1, Duration.ofMinutes(30)));
    }

    @Test
    void calculaRentabilidadeSemDuplaDeducaoEPontosDeEquilibrioContabilFinanceiroEEconomico() throws Exception {
        String comp = "2026-10";
        LocalDate dataRef = LocalDate.of(2026, 10, 15);
        mockMvc.perform(post("/api/financeiro/plano-contas/inicializar-padrao")
                        .header("Authorization", tokenGerenteA))
                .andExpect(status().isOk());

        Categoria catEstrela = criarCategoria(EMPRESA_A, FILIAL_A1, "Eletronicos Premium");
        Categoria catVolume = criarCategoria(EMPRESA_A, FILIAL_A1, "Acessorios Giro Rapido");

        // Produto 1 (Estrela): Preco 200, Custo 80 (10 un vendidas = 2000 bruto, CMV 800)
        Produto prodEstrela = criarProduto(EMPRESA_A, FILIAL_A1, catEstrela, "Fone Pro ANC", new BigDecimal("80.00"), new BigDecimal("200.00"), 30);
        // Produto 2 (Alto Volume / Baixa Margem): Preco 100, Custo 82 (10 un vendidas = 1000 bruto, CMV 820)
        Produto prodBaixaMargem = criarProduto(EMPRESA_A, FILIAL_A1, catVolume, "Cabo USB Basico", new BigDecimal("82.00"), new BigDecimal("100.00"), 50);

        Cliente cliente = criarCliente(EMPRESA_A, FILIAL_A1, "Cliente Indicadores");
        Usuario vendedor = criarUsuario(EMPRESA_A, FILIAL_A1, "vend_op_lote4");

        // Venda finalizada de R$ 3.000,00 (sem desconto), CMV total = 800 + 820 = R$ 1.620,00
        Venda venda = criarVendaFinalizada(
                EMPRESA_A, FILIAL_A1, cliente, vendedor, dataRef,
                List.of(
                        new ItemSeed(prodEstrela, 10, new BigDecimal("200.00"), new BigDecimal("80.00")),
                        new ItemSeed(prodBaixaMargem, 10, new BigDecimal("100.00"), new BigDecimal("82.00"))));

        // Conta a receber da venda em cartao com R$ 90,00 de taxa MDR ja liquidada (gera despesa 9.01 automatica de R$ 90,00)
        PlanoContasGerencial contaReceita = buscarContaPorCodigo(EMPRESA_A, FILIAL_A1, "1.01");
        PlanoContasGerencial contaTributoVenda = buscarContaPorCodigo(EMPRESA_A, FILIAL_A1, "2.03");
        PlanoContasGerencial contaComissaoVar = buscarContaPorCodigo(EMPRESA_A, FILIAL_A1, "4.01");
        PlanoContasGerencial contaAluguelFixo = buscarContaPorCodigo(EMPRESA_A, FILIAL_A1, "6.01");
        PlanoContasGerencial contaTaxaCartao = buscarContaPorCodigo(EMPRESA_A, FILIAL_A1, "9.01");

        ContaReceber cr = new ContaReceber();
        cr.setEmpresaId(EMPRESA_A);
        cr.setFilialId(FILIAL_A1);
        cr.setVendaId(venda.getIdVenda());
        cr.setClienteId(cliente.getIdCliente());
        cr.setDescricao("Venda Cartao #" + venda.getIdVenda());
        cr.setValorBruto(new BigDecimal("3000.00"));
        cr.setDesconto(BigDecimal.ZERO);
        cr.setTaxaPercentual(new BigDecimal("3.0000"));
        cr.setValorTaxa(new BigDecimal("90.00"));
        cr.setValorLiquido(new BigDecimal("2910.00"));
        cr.setValorPago(new BigDecimal("3000.00"));
        cr.setSaldo(BigDecimal.ZERO);
        cr.setVencimento(dataRef.plusDays(15));
        cr.setDataPrevistaRepasse(dataRef.plusDays(15));
        cr.setDataCompetencia(dataRef);
        cr.setDataEmissao(dataRef);
        cr.setPlanoContaId(contaReceita.getIdPlanoConta());
        cr.setModalidadeRecebivel(ModalidadeRecebivel.CARTAO_CREDITO);
        cr.setStatus(FinanceiroStatus.PAGO);
        cr.setPagoEm(dataRef.plusDays(15).atTime(14, 0));
        cr.setCriadoEm(dataRef.atTime(10, 0));
        cr.setVersion(0L);
        cr = contaReceberRepository.save(cr);

        // Despesa de Tributo sobre Venda (Linha 4 DRE): R$ 300,00 -> Receita Liquida = 3000 - 300 = R$ 2.700,00
        criarDespesa(EMPRESA_A, FILIAL_A1, contaTributoVenda.getIdPlanoConta(), "Simples Nacional Outubro", new BigDecimal("300.00"), dataRef);
        // Despesa Variavel de Comissao (4.01 VARIAVEL): R$ 90,00
        criarDespesa(EMPRESA_A, FILIAL_A1, contaComissaoVar.getIdPlanoConta(), "Comissoes Outubro", new BigDecimal("90.00"), dataRef);
        // Despesa Automatica de Taxa de Cartao (9.01 VARIAVEL) vinculada ao CR: R$ 90,00 (nao deve ser contada 2x!)
        criarDespesa(EMPRESA_A, FILIAL_A1, contaTaxaCartao.getIdPlanoConta(),
                "[TAXA-DEDUZIDA-ORIGEM:CR-" + cr.getIdContaReceber() + "] Taxa de liquidacao de recebivel #" + cr.getIdContaReceber(),
                new BigDecimal("90.00"), dataRef);
        // Despesa Fixa de Aluguel (6.01 FIXO): R$ 450,00
        criarDespesa(EMPRESA_A, FILIAL_A1, contaAluguelFixo.getIdPlanoConta(), "Aluguel Loja Outubro", new BigDecimal("450.00"), dataRef);

        // Configura Parametros: Capital Investido = R$ 20.000,00, Retorno Minimo Mensal = 0.90% (R$ 180,00 no mes)
        String paramBody = json.writeValueAsString(Map.of(
                "segmentoNegocio", "VAREJO_ELETRONICOS",
                "capitalInvestidoReferencia", new BigDecimal("20000.00"),
                "retornoMinimoMensalPercentual", new BigDecimal("0.9000"),
                "margemContribuicaoMinimaPercentual", new BigDecimal("25.0000")));
        mockMvc.perform(put("/api/financeiro/indicadores/parametros")
                        .header("Authorization", tokenGerenteA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paramBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segmentoNegocio").value("VAREJO_ELETRONICOS"));

        // Registra Balanco Patrimonial com Depreciacao = R$ 90,00 e Amortizacao de Principal = R$ 180,00
        String balancoBody = json.writeValueAsString(Map.of(
                "competenciaAnoMes", comp,
                "depreciacaoAmortizacaoPeriodo", new BigDecimal("90.00"),
                "amortizacaoPrincipalDividaPeriodo", new BigDecimal("180.00"),
                "confirmadoGestor", false));
        mockMvc.perform(put("/api/financeiro/indicadores/balanco-patrimonial")
                        .header("Authorization", tokenGerenteA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(balancoBody))
                .andExpect(status().isOk());

        // Verificacao matematica independente:
        // Receita Bruta = 3000.00 | Tributos s/ Venda = 300.00 -> Receita Liquida = 2700.00
        // CMV = 1620.00 | Despesas Variaveis DRE (Comissao 90 + Taxa Cartao 90, sem duplicar CR!) = 180.00
        // Custos e Despesas Variaveis Totais = 1620 + 180 = 1800.00
        // Margem de Contribuicao Total = 2700 - 1800 = 900.00 (IMC = 900 / 2700 = 33.33%)
        // Custos Fixos Contabeis = 450.00
        // PEC = 450.00 / (1/3) = R$ 1.350,00
        // Desembolsos Fixos Financeiros = 450.00 - 90.00 (depreciacao) + 180.00 (amortizacao principal) = 540.00
        // PEF = 540.00 / (1/3) = R$ 1.620,00
        // Remuneracao Minima Capital (31 dias / 30 * 0.90% * 20000 = 0.93% * 20000 = 186.00)
        // PEE = (450.00 + 186.00) / (1/3) = 636.00 * 3 = R$ 1.908,00
        String respStr = mockMvc.perform(get("/api/financeiro/indicadores")
                        .param("competencia", comp)
                        .header("Authorization", tokenFinanceiroA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rentabilidade.receitaBrutaTotal").value(3000.00))
                .andExpect(jsonPath("$.rentabilidade.receitaLiquidaTotal").value(2700.00))
                .andExpect(jsonPath("$.rentabilidade.cmvTotal").value(1620.00))
                .andExpect(jsonPath("$.rentabilidade.despesasVariaveisDre").value(180.00))
                .andExpect(jsonPath("$.rentabilidade.custosDespesasVariaveisTotais").value(1800.00))
                .andExpect(jsonPath("$.rentabilidade.margemContribuicaoTotalValor").value(900.00))
                .andExpect(jsonPath("$.rentabilidade.margemContribuicaoTotalPercentual").value(33.33))
                .andExpect(jsonPath("$.pontoEquilibrio.statusConfiabilidade").value("CALCULADO"))
                .andExpect(jsonPath("$.pontoEquilibrio.pontoEquilibrioContabilReceita").value(1350.00))
                .andExpect(jsonPath("$.pontoEquilibrio.pontoEquilibrioFinanceiroReceita").value(1620.00))
                .andExpect(jsonPath("$.pontoEquilibrio.pontoEquilibrioEconomicoReceita").value(1908.00))
                .andExpect(jsonPath("$.pontoEquilibrio.margemSegurancaContabilValor").value(1350.00))
                .andExpect(jsonPath("$.pontoEquilibrio.margemSegurancaContabilPercentual").value(50.00))
                .andExpect(jsonPath("$.pontoEquilibrio.grauAlavancagemOperacional").value(2.00))
                .andReturn().getResponse().getContentAsString();

        JsonNode root = json.readTree(respStr);
        assertThat(root.path("rentabilidade").path("porProduto").size()).isEqualTo(2);
        assertThat(root.path("rentabilidade").path("porCategoria").size()).isEqualTo(2);
        assertThat(root.path("rentabilidade").path("alertasAltoVolumeBaixaMargem").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void simulaMarkupPrecificacaoReadOnlyEAplicaPrecoSomentePorOperacaoAutorizada() throws Exception {
        Categoria cat = criarCategoria(EMPRESA_A, FILIAL_A1, "Calcados");
        Produto prod = criarProduto(EMPRESA_A, FILIAL_A1, cat, "Tenis Corrida", new BigDecimal("90.00"), new BigDecimal("150.00"), 15);

        // Custo Efetivo = 90 + 5 (frete) + 5 (imposto nao rec) = 100.00
        // DV% = 10% (tributos) + 3% (cartao) + 2% (comissao) = 15%
        // Margem Desejada = 25% -> Soma = 40% -> Markup Divisor = 0.60 -> Markup Multiplicador = 1.6667
        // Preco Recomendado = 100 / 0.60 = R$ 166,67 | Preco Minimo = 100 / 0.85 = R$ 117,65
        // Preco Atual = R$ 150,00 -> Receita apos DV (85%) = 127.50 -> Margem Real = 27.50 (18.33%) -> ABAIXO_MARGEM_DESEJADA
        String simBody = json.writeValueAsString(Map.ofEntries(
                Map.entry("produtoId", prod.getIdProduto()),
                Map.entry("custoAquisicaoBase", new BigDecimal("90.00")),
                Map.entry("freteCompraValor", new BigDecimal("5.00")),
                Map.entry("impostosNaoRecuperaveisValor", new BigDecimal("5.00")),
                Map.entry("aliquotaTributosVendaPercentual", new BigDecimal("10.00")),
                Map.entry("taxaCartaoPercentual", new BigDecimal("3.00")),
                Map.entry("comissaoVendaPercentual", new BigDecimal("2.00")),
                Map.entry("margemLucroDesejadaPercentual", new BigDecimal("25.00")),
                Map.entry("precoPraticadoAtual", new BigDecimal("150.00")),
                Map.entry("descontoPromocionalSimuladoPercentual", new BigDecimal("10.00")),
                Map.entry("variacaoCustoSimuladaPercentual", new BigDecimal("10.00"))));

        mockMvc.perform(post("/api/financeiro/indicadores/precificacao/simular")
                        .header("Authorization", tokenFinanceiroA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.custoAquisicaoEfetivoTotal").value(100.00))
                .andExpect(jsonPath("$.percentualDespesasVariaveisTotal").value(15.00))
                .andExpect(jsonPath("$.markupDivisor").value(0.60))
                .andExpect(jsonPath("$.markupMultiplicador").value(1.6667))
                .andExpect(jsonPath("$.precoMinimoEquilibrio").value(117.65))
                .andExpect(jsonPath("$.precoRecomendado").value(166.67))
                .andExpect(jsonPath("$.margemRealPrecoAtualValor").value(27.50))
                .andExpect(jsonPath("$.margemRealPrecoAtualPercentual").value(18.33))
                .andExpect(jsonPath("$.situacaoPrecoAtual").value("ABAIXO_MARGEM_DESEJADA"))
                .andExpect(jsonPath("$.simulacaoReadOnly").value(true));

        // Confirma que a simulacao NAO alterou o preco no banco de dados
        Produto aposSimulacao = produtoRepository.findById(prod.getIdProduto()).orElseThrow();
        assertThat(aposSimulacao.getValorVenda()).isEqualByComparingTo("150.00");

        // Bloqueia soma de taxas + margem >= 100%
        String invalidoBody = json.writeValueAsString(Map.of(
                "custoAquisicaoBase", new BigDecimal("100.00"),
                "aliquotaTributosVendaPercentual", new BigDecimal("60.00"),
                "margemLucroDesejadaPercentual", new BigDecimal("45.00")));
        mockMvc.perform(post("/api/financeiro/indicadores/precificacao/simular")
                        .header("Authorization", tokenFinanceiroA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidoBody))
                .andExpect(status().isBadRequest());

        // Aplicacao autorizada altera o preco do produto
        String aplicarBody = json.writeValueAsString(Map.of(
                "produtoId", prod.getIdProduto(),
                "novoValorVenda", new BigDecimal("166.67"),
                "novaMargemPercentual", new BigDecimal("25.00"),
                "justificativa", "Ajuste para atingir margem meta de 25%"));
        mockMvc.perform(post("/api/financeiro/indicadores/precificacao/aplicar-preco")
                        .header("Authorization", tokenGerenteA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aplicarBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorVenda").value(166.67));

        Produto aposAplicacao = produtoRepository.findById(prod.getIdProduto()).orElseThrow();
        assertThat(aposAplicacao.getValorVenda()).isEqualByComparingTo("166.67");
    }

    @Test
    void calculaCapitalGiroPrazosCiclosEBloqueiaIndicadoresPatrimoniaisSemBalancoConfirmado() throws Exception {
        String comp = "2026-11";
        LocalDate inicioMes = LocalDate.of(2026, 11, 1);
        LocalDate dataRef = LocalDate.of(2026, 11, 15);
        mockMvc.perform(post("/api/financeiro/plano-contas/inicializar-padrao")
                        .header("Authorization", tokenGerenteA))
                .andExpect(status().isOk());

        // 1. Conta Financeira com saldo disponivel (ACF) = R$ 15.000,00
        ContaFinanceira banco = new ContaFinanceira();
        banco.setEmpresaId(EMPRESA_A);
        banco.setFilialId(FILIAL_A1);
        banco.setCodigo("CX-PRINCIPAL");
        banco.setNome("Banco Operacional");
        banco.setTipoConta(TipoContaFinanceira.CONTA_CORRENTE);
        banco.setSaldoInicial(new BigDecimal("15000.00"));
        banco.setSaldoAtual(new BigDecimal("15000.00"));
        banco.setDataSaldoInicial(inicioMes);
        banco.setPadrao(true);
        banco.setAtivo(true);
        banco.setCriadoEm(LocalDateTime.now());
        banco.setVersion(0L);
        contaFinanceiraRepository.save(banco);

        // 2. Produto com Estoque Atual = 100 un * R$ 200,00 = R$ 20.000,00
        Categoria cat = criarCategoria(EMPRESA_A, FILIAL_A1, "Linha Branca");
        Fornecedor forn = criarFornecedor(EMPRESA_A, FILIAL_A1, "Fornecedor Industrial LTDA", "12345678000199");
        Produto prod = criarProduto(EMPRESA_A, FILIAL_A1, cat, "Microondas Inox", new BigDecimal("200.00"), new BigDecimal("400.00"), 100);

        // 3. Venda a prazo no periodo: 50 un * R$ 400 = R$ 20.000,00 (CMV = 50 * 200 = R$ 10.000,00)
        Cliente cliente = criarCliente(EMPRESA_A, FILIAL_A1, "Cliente Prazo");
        Usuario vendedor = criarUsuario(EMPRESA_A, FILIAL_A1, "vend_ciclo_lote4");
        Venda venda = criarVendaFinalizada(
                EMPRESA_A, FILIAL_A1, cliente, vendedor, dataRef,
                List.of(new ItemSeed(prod, 50, new BigDecimal("400.00"), new BigDecimal("200.00"))));

        // Conta a Receber em aberto (ACO) = R$ 20.000,00
        ContaReceber cr = new ContaReceber();
        cr.setEmpresaId(EMPRESA_A);
        cr.setFilialId(FILIAL_A1);
        cr.setVendaId(venda.getIdVenda());
        cr.setClienteId(cliente.getIdCliente());
        cr.setDescricao("Parcela Prazo 30d");
        cr.setValorBruto(new BigDecimal("20000.00"));
        cr.setDesconto(BigDecimal.ZERO);
        cr.setValorLiquido(new BigDecimal("20000.00"));
        cr.setValorPago(BigDecimal.ZERO);
        cr.setSaldo(new BigDecimal("20000.00"));
        cr.setTotalParcelas(2);
        cr.setNumeroParcela(1);
        cr.setVencimento(dataRef.plusDays(30));
        cr.setDataCompetencia(dataRef);
        cr.setDataEmissao(dataRef);
        cr.setStatus(FinanceiroStatus.ABERTO);
        cr.setCriadoEm(dataRef.atTime(10, 0));
        cr.setVersion(0L);
        contaReceberRepository.save(cr);

        // 4. Nota Fiscal de Entrada (Compra a prazo) = R$ 15.000,00 e Conta a Pagar Fornecedor em aberto (PCO) = R$ 15.000,00
        NotaFiscalEntrada nf = new NotaFiscalEntrada();
        nf.setEmpresaId(EMPRESA_A);
        nf.setFilialId(FILIAL_A1);
        nf.setFornecedor(forn);
        nf.setNumero("NF-9001");
        nf.setSerie("1");
        nf.setDataEmissao(dataRef.atTime(8, 0));
        nf.setDataEntrada(dataRef.atTime(9, 0));
        nf.setDataCompetencia(dataRef);
        nf.setValorTotal(new BigDecimal("15000.00"));
        nf.setVersion(0L);
        notaFiscalEntradaRepository.save(nf);

        // Conta patrimonial/investimento para compra de estoque (nao entra como despesa direta da DRE; entra via CMV ao vender)
        PlanoContasGerencial contaCompraEstoque = new PlanoContasGerencial();
        contaCompraEstoque.setEmpresaId(EMPRESA_A);
        contaCompraEstoque.setFilialId(FILIAL_A1);
        contaCompraEstoque.setCodigo("12.01");
        contaCompraEstoque.setNome("Compra de Mercadorias para Estoque");
        contaCompraEstoque.setTipoConta(TipoContaGerencial.INVESTIMENTO);
        contaCompraEstoque.setNatureza(NaturezaContaGerencial.DEBITO);
        contaCompraEstoque.setClassificacaoCusto(ClassificacaoCusto.VARIAVEL);
        contaCompraEstoque.setGrupoDre(GrupoDreGerencial.NAO_APLICAVEL_DRE);
        contaCompraEstoque.setAceitaLancamento(true);
        contaCompraEstoque.setAtivo(true);
        contaCompraEstoque.setVersion(0L);
        contaCompraEstoque = planoContasRepository.save(contaCompraEstoque);

        ContaPagar cp = new ContaPagar();
        cp.setEmpresaId(EMPRESA_A);
        cp.setFilialId(FILIAL_A1);
        cp.setFornecedorId(forn.getIdFornecedor());
        cp.setPlanoContaId(contaCompraEstoque.getIdPlanoConta());
        cp.setDescricao("Fornecedor NF-9001");
        cp.setValor(new BigDecimal("15000.00"));
        cp.setValorPago(BigDecimal.ZERO);
        cp.setSaldo(new BigDecimal("15000.00"));
        cp.setVencimento(dataRef.plusDays(30));
        cp.setDataCompetencia(dataRef);
        cp.setDataEmissao(dataRef);
        cp.setStatus(FinanceiroStatus.ABERTO);
        cp.setCriadoEm(dataRef.atTime(9, 0));
        cp.setVersion(0L);
        contaPagarRepository.save(cp);

        // Sem Balanco Patrimonial confirmado:
        // ACO = Estoque (20.000) + CR (20.000) = 40.000 | PCO = Fornecedores (15.000) -> NCG = 40.000 - 15.000 = R$ 25.000,00
        // ACF = 15.000 | PCF = 0 -> AC Total = 55.000 | PC Total = 15.000 -> CGL = 55.000 - 15.000 = R$ 40.000,00
        // Saldo de Tesouraria (ST) = CGL (40.000) - NCG (25.000) = R$ 15.000,00
        // Liquidez Imediata = 15.000 / 15.000 = 1.00
        // Liquidez Corrente, Seca, Geral, Endividamento e ROE = null (DADOS_INSUFICIENTES)
        mockMvc.perform(get("/api/financeiro/indicadores")
                        .param("competencia", comp)
                        .header("Authorization", tokenGerenteA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capitalGiro.ativoCirculanteOperacional").value(40000.00))
                .andExpect(jsonPath("$.capitalGiro.passivoCirculanteOperacional").value(15000.00))
                .andExpect(jsonPath("$.capitalGiro.necessidadeCapitalGiroNcg").value(25000.00))
                .andExpect(jsonPath("$.capitalGiro.capitalGiroLiquidoCgl").value(40000.00))
                .andExpect(jsonPath("$.capitalGiro.saldoTesourariaSt").value(15000.00))
                .andExpect(jsonPath("$.prazosCiclos.pmePrazoMedioEstocagemDias").value(60.00))
                .andExpect(jsonPath("$.prazosCiclos.pmrPrazoMedioRecebimentoDias").value(15.00))
                .andExpect(jsonPath("$.prazosCiclos.pmpPrazoMedioPagamentoDias").value(15.00))
                .andExpect(jsonPath("$.prazosCiclos.cicloOperacionalDias").value(75.00))
                .andExpect(jsonPath("$.prazosCiclos.cicloFinanceiroDias").value(60.00))
                .andExpect(jsonPath("$.prazosCiclos.pagaFornecedorAntesDeReceber").value(true))
                .andExpect(jsonPath("$.liquidezEndividamento.balancoPatrimonialConfirmado").value(false))
                .andExpect(jsonPath("$.liquidezEndividamento.liquidezImediata").value(1.00))
                .andExpect(jsonPath("$.liquidezEndividamento.liquidezCorrente").doesNotExist())
                .andExpect(jsonPath("$.liquidezEndividamento.endividamentoGeralPercentual").doesNotExist())
                .andExpect(jsonPath("$.liquidezEndividamento.statusConfiabilidadePatrimonial").value("DADOS_INSUFICIENTES"))
                .andExpect(jsonPath("$.investimentosRetorno.roeRetornoSobrePatrimonioPercentual").doesNotExist())
                .andExpect(jsonPath("$.investimentosRetorno.statusConfiabilidadeRoaRoe").value("DADOS_INSUFICIENTES"));

        // Agora confirma o Balanco Patrimonial Gerencial completo:
        // AC = 55.000 + 5.000 (despesas antecipadas) = 60.000
        // ANC = 10.000 (RLP) + 30.000 (Imobilizado Liquido = 40.000 - 10.000) = 40.000 -> Ativo Total = 100.000
        // PC = 15.000 (fornecedores) + 5.000 (emprestimos CP) = 20.000
        // PNC = 20.000 (emprestimos LP) -> Passivo Exigivel Total = 40.000
        // PL = 50.000 (capital social) + 10.000 (lucros acumulados) = 60.000
        String balancoCompleto = json.writeValueAsString(Map.ofEntries(
                Map.entry("competenciaAnoMes", comp),
                Map.entry("usarSaldosOperacionaisErp", true),
                Map.entry("despesasAntecipadas", new BigDecimal("5000.00")),
                Map.entry("realizavelLongoPrazo", new BigDecimal("10000.00")),
                Map.entry("imobilizadoBruto", new BigDecimal("40000.00")),
                Map.entry("depreciacaoAcumulada", new BigDecimal("10000.00")),
                Map.entry("emprestimosFinanciamentosCp", new BigDecimal("5000.00")),
                Map.entry("emprestimosFinanciamentosLp", new BigDecimal("20000.00")),
                Map.entry("capitalSocial", new BigDecimal("50000.00")),
                Map.entry("lucrosPrejuizosAcumulados", new BigDecimal("10000.00")),
                Map.entry("confirmadoGestor", true)));

        mockMvc.perform(put("/api/financeiro/indicadores/balanco-patrimonial")
                        .header("Authorization", tokenGerenteA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(balancoCompleto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consistenteContabilmente").value(true))
                .andExpect(jsonPath("$.ativoTotal").value(100000.00))
                .andExpect(jsonPath("$.passivoExigivelTotal").value(40000.00))
                .andExpect(jsonPath("$.patrimonioLiquidoTotal").value(60000.00));

        // Com Balanco confirmado:
        // Liquidez Corrente = 60.000 / 20.000 = 3.00
        // Liquidez Seca = (60.000 - 20.000 - 5.000) / 20.000 = 35.000 / 20.000 = 1.75
        // Liquidez Imediata = 15.000 / 20.000 = 0.75
        // Liquidez Geral = (60.000 + 10.000) / (20.000 + 20.000) = 70.000 / 40.000 = 1.75
        // Endividamento Geral = 40.000 / 100.000 = 40.00%
        // Participacao Capital Terceiros (PCT) = 40.000 / 60.000 = 66.67%
        // Composicao Endividamento = 20.000 / 40.000 = 50.00%
        // ROA = 10.000 (lucro liquido) / 100.000 = 10.00%
        // ROE = 10.000 / 60.000 = 16.67%
        mockMvc.perform(get("/api/financeiro/indicadores")
                        .param("competencia", comp)
                        .header("Authorization", tokenGerenteA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liquidezEndividamento.balancoPatrimonialConfirmado").value(true))
                .andExpect(jsonPath("$.liquidezEndividamento.statusConfiabilidadePatrimonial").value("CALCULADO"))
                .andExpect(jsonPath("$.liquidezEndividamento.liquidezCorrente").value(3.00))
                .andExpect(jsonPath("$.liquidezEndividamento.liquidezSeca").value(1.75))
                .andExpect(jsonPath("$.liquidezEndividamento.liquidezImediata").value(0.75))
                .andExpect(jsonPath("$.liquidezEndividamento.liquidezGeral").value(1.75))
                .andExpect(jsonPath("$.liquidezEndividamento.endividamentoGeralPercentual").value(40.00))
                .andExpect(jsonPath("$.liquidezEndividamento.participacaoCapitalTerceirosPercentual").value(66.67))
                .andExpect(jsonPath("$.liquidezEndividamento.composicaoEndividamentoCurtoPrazoPercentual").value(50.00))
                .andExpect(jsonPath("$.investimentosRetorno.statusConfiabilidadeRoaRoe").value("CALCULADO"))
                .andExpect(jsonPath("$.investimentosRetorno.roaRetornoSobreAtivoPercentual").value(10.00))
                .andExpect(jsonPath("$.investimentosRetorno.roeRetornoSobrePatrimonioPercentual").value(16.67));
    }

    @Test
    void simulaViabilidadeInvestimentoVplTirPaybackEGaranteIsolamentoMultiTenantERbac() throws Exception {
        // 1. Simulacao de Investimento (VPL, TIR, ROI, Payback Simples e Descontado)
        String invBody = json.writeValueAsString(Map.of(
                "nomeProjeto", "Abertura Quiosque Shopping",
                "investimentoInicial", new BigDecimal("10000.00"),
                "ganhoEsperadoTotal", new BigDecimal("15000.00"),
                "taxaDescontoPeriodicaPercentual", new BigDecimal("2.00"),
                "fluxosCaixaProjetados", List.of(
                        new BigDecimal("-10000.00"),
                        new BigDecimal("4000.00"),
                        new BigDecimal("4000.00"),
                        new BigDecimal("4000.00"),
                        new BigDecimal("3000.00"))));

        mockMvc.perform(post("/api/financeiro/indicadores/investimentos/simular")
                        .header("Authorization", tokenGerenteA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roiSimuladoPercentual").value(50.00))
                .andExpect(jsonPath("$.paybackSimplesPeriodos").value(2.50))
                .andExpect(jsonPath("$.vplValorPresenteLiquido").value(4307.07))
                .andExpect(jsonPath("$.investimentoViavelEconomicamente").value(true))
                .andExpect(jsonPath("$.statusConfiabilidadeSimulacao").value("SIMULADO_OU_PROJETADO"));

        // 2. RBAC: Vendedor nao pode acessar nem alterar indicadores financeiros
        mockMvc.perform(get("/api/financeiro/indicadores")
                        .header("Authorization", tokenVendedorA))
                .andExpect(status().isForbidden());

        // 3. Isolamento Multi-tenant: Tenant B nao enxerga nem altera produto do Tenant A
        Categoria catA = criarCategoria(EMPRESA_A, FILIAL_A1, "Categoria Tenant A");
        Produto prodA = criarProduto(EMPRESA_A, FILIAL_A1, catA, "Produto Exclusivo A", new BigDecimal("50.00"), new BigDecimal("100.00"), 10);

        String crossTenantBody = json.writeValueAsString(Map.of(
                "produtoId", prodA.getIdProduto(),
                "novoValorVenda", new BigDecimal("120.00"),
                "justificativa", "Tentativa cross-tenant"));
        mockMvc.perform(post("/api/financeiro/indicadores/precificacao/aplicar-preco")
                        .header("Authorization", tokenGerenteB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crossTenantBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void regressaoEstoqueRetroativoTituloNoInicioTrocaBalancoConfirmacaoPLeSimulacaoFluxos() throws Exception {
        Long empC = 43L;
        Long filC = 4301L;
        String tokenC = bearer(jwtTokenService.issueAccessToken(
                405L, "Gerente Regressao", "gerente_reg_lote4", List.of("ROLE_DONO_GERENTE"), empC, filC, Duration.ofMinutes(30)));
        String comp = "2026-11";
        LocalDate inicioMes = LocalDate.of(2026, 11, 1);
        LocalDate dataRef = LocalDate.of(2026, 11, 15);

        mockMvc.perform(post("/api/financeiro/plano-contas/inicializar-padrao")
                        .header("Authorization", tokenC))
                .andExpect(status().isOk());

        Categoria cat = criarCategoria(empC, filC, "Categoria Regressao");
        Fornecedor forn = criarFornecedor(empC, filC, "Fornecedor Regressao LTDA", "43123456000188");
        Produto prodA = criarProduto(empC, filC, cat, "Produto Estrela A", new BigDecimal("100.00"), new BigDecimal("180.00"), 100);
        Produto prodB = criarProduto(empC, filC, cat, "Produto Baixo Giro B", new BigDecimal("85.00"), new BigDecimal("100.00"), 0);

        // Movimentacao 1 dentro do periodo e Movimentacao 2 apos o fim do periodo:
        // Hoje = 100 un * R$ 100 = R$ 10.000 -> 30/11 = 11.000 -> 31/10 = 13.000 (nao pode subtrair Mov 2 duas vezes!)
        EstoqueMovimentacao movDentro = new EstoqueMovimentacao();
        movDentro.setEmpresaId(empC);
        movDentro.setFilialId(filC);
        movDentro.setProduto(prodA);
        movDentro.setTipo(TipoMovimentacaoEstoque.SAIDA);
        movDentro.setQuantidade(20);
        movDentro.setSaldoAnterior(130);
        movDentro.setSaldoPosterior(110);
        movDentro.setCustoUnitario(new BigDecimal("100.00"));
        movDentro.setValorTotalCusto(new BigDecimal("2000.00"));
        movDentro.setCriadoEm(LocalDateTime.of(2026, 11, 15, 12, 0));
        estoqueMovimentacaoRepository.save(movDentro);

        EstoqueMovimentacao movAposFim = new EstoqueMovimentacao();
        movAposFim.setEmpresaId(empC);
        movAposFim.setFilialId(filC);
        movAposFim.setProduto(prodA);
        movAposFim.setTipo(TipoMovimentacaoEstoque.SAIDA);
        movAposFim.setQuantidade(10);
        movAposFim.setSaldoAnterior(110);
        movAposFim.setSaldoPosterior(100);
        movAposFim.setCustoUnitario(new BigDecimal("100.00"));
        movAposFim.setValorTotalCusto(new BigDecimal("1000.00"));
        movAposFim.setCriadoEm(LocalDateTime.of(2026, 12, 5, 12, 0));
        estoqueMovimentacaoRepository.save(movAposFim);

        Cliente cliente = criarCliente(empC, filC, "Cliente Regressao");
        Usuario vendedor = criarUsuario(empC, filC, "vend_reg_lote4");
        Venda venda = criarVendaFinalizada(
                empC, filC, cliente, vendedor, dataRef,
                List.of(
                        new ItemSeed(prodA, 5, new BigDecimal("180.00"), new BigDecimal("100.00")),
                        new ItemSeed(prodB, 1, new BigDecimal("100.00"), new BigDecimal("85.00"))));

        // Troca pura (TipoDevolucao.TROCA) nao deve abater receita nem quantidade do Produto A
        VendaDevolucao troca = new VendaDevolucao();
        troca.setEmpresaId(empC);
        troca.setFilialId(filC);
        troca.setVendaOriginalId(venda.getIdVenda());
        troca.setTipo(TipoDevolucao.TROCA);
        troca.setMotivo("Troca de tamanho");
        troca.setValorAjuste(BigDecimal.ZERO);
        troca.setCriadoEm(LocalDateTime.of(2026, 11, 20, 14, 0));
        VendaDevolucaoItem itemTroca = new VendaDevolucaoItem();
        itemTroca.setDevolucao(troca);
        itemTroca.setProdutoOriginalId(prodA.getIdProduto());
        itemTroca.setQuantidade(2);
        itemTroca.setValorUnitario(new BigDecimal("180.00"));
        troca.setItens(new ArrayList<>(List.of(itemTroca)));
        vendaDevolucaoRepository.save(troca);

        // Titulo anterior aberto em 01/11 (pago 10/11) + Titulo emitido EXATAMENTE no dia 01/11 (inicio)
        ContaReceber crAntigo = new ContaReceber();
        crAntigo.setEmpresaId(empC);
        crAntigo.setFilialId(filC);
        crAntigo.setClienteId(cliente.getIdCliente());
        crAntigo.setDescricao("CR Outubro pago em 10/11");
        crAntigo.setValorBruto(new BigDecimal("4000.00"));
        crAntigo.setDesconto(BigDecimal.ZERO);
        crAntigo.setValorLiquido(new BigDecimal("4000.00"));
        crAntigo.setValorPago(new BigDecimal("4000.00"));
        crAntigo.setSaldo(BigDecimal.ZERO);
        crAntigo.setTotalParcelas(2);
        crAntigo.setDataCompetencia(LocalDate.of(2026, 10, 20));
        crAntigo.setDataEmissao(LocalDate.of(2026, 10, 20));
        crAntigo.setVencimento(LocalDate.of(2026, 11, 20));
        crAntigo.setStatus(FinanceiroStatus.PAGO);
        crAntigo.setPagoEm(LocalDateTime.of(2026, 11, 10, 10, 0));
        crAntigo.setCriadoEm(LocalDateTime.of(2026, 10, 20, 9, 0));
        crAntigo.setVersion(0L);
        contaReceberRepository.save(crAntigo);

        ContaReceber crInicio = new ContaReceber();
        crInicio.setEmpresaId(empC);
        crInicio.setFilialId(filC);
        crInicio.setClienteId(cliente.getIdCliente());
        crInicio.setDescricao("CR emitido no dia inicio 01/11");
        crInicio.setValorBruto(new BigDecimal("6000.00"));
        crInicio.setDesconto(BigDecimal.ZERO);
        crInicio.setValorLiquido(new BigDecimal("6000.00"));
        crInicio.setValorPago(BigDecimal.ZERO);
        crInicio.setSaldo(new BigDecimal("6000.00"));
        crInicio.setTotalParcelas(2);
        crInicio.setDataCompetencia(inicioMes);
        crInicio.setDataEmissao(inicioMes);
        crInicio.setVencimento(LocalDate.of(2026, 12, 1));
        crInicio.setStatus(FinanceiroStatus.ABERTO);
        crInicio.setCriadoEm(inicioMes.atTime(9, 0));
        crInicio.setVersion(0L);
        contaReceberRepository.save(crInicio);

        PlanoContasGerencial contaCompra = buscarContaPorCodigo(empC, filC, "3.01");
        ContaPagar cpAntigo = new ContaPagar();
        cpAntigo.setEmpresaId(empC);
        cpAntigo.setFilialId(filC);
        cpAntigo.setFornecedorId(forn.getIdFornecedor());
        cpAntigo.setPlanoContaId(contaCompra.getIdPlanoConta());
        cpAntigo.setDescricao("Fornecedor anterior aberto");
        cpAntigo.setValor(new BigDecimal("2000.00"));
        cpAntigo.setValorPago(BigDecimal.ZERO);
        cpAntigo.setSaldo(new BigDecimal("2000.00"));
        cpAntigo.setDataCompetencia(LocalDate.of(2026, 10, 20));
        cpAntigo.setDataEmissao(LocalDate.of(2026, 10, 20));
        cpAntigo.setVencimento(LocalDate.of(2026, 12, 20));
        cpAntigo.setStatus(FinanceiroStatus.ABERTO);
        cpAntigo.setCriadoEm(LocalDateTime.of(2026, 10, 20, 9, 0));
        cpAntigo.setVersion(0L);
        contaPagarRepository.save(cpAntigo);

        ContaPagar cpInicio = new ContaPagar();
        cpInicio.setEmpresaId(empC);
        cpInicio.setFilialId(filC);
        cpInicio.setFornecedorId(forn.getIdFornecedor());
        cpInicio.setPlanoContaId(contaCompra.getIdPlanoConta());
        cpInicio.setDescricao("Fornecedor emitido no dia inicio 01/11");
        cpInicio.setValor(new BigDecimal("3000.00"));
        cpInicio.setValorPago(BigDecimal.ZERO);
        cpInicio.setSaldo(new BigDecimal("3000.00"));
        cpInicio.setDataCompetencia(inicioMes);
        cpInicio.setDataEmissao(inicioMes);
        cpInicio.setVencimento(LocalDate.of(2026, 12, 15));
        cpInicio.setStatus(FinanceiroStatus.ABERTO);
        cpInicio.setCriadoEm(inicioMes.atTime(9, 0));
        cpInicio.setVersion(0L);
        contaPagarRepository.save(cpInicio);

        // 1. Omitindo confirmadoGestor nao pode auto-confirmar o Balanco Patrimonial
        String balancoSemFlag = json.writeValueAsString(Map.of(
                "competenciaAnoMes", comp,
                "capitalSocial", new BigDecimal("10000.00")));
        mockMvc.perform(put("/api/financeiro/indicadores/balanco-patrimonial")
                        .header("Authorization", tokenC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(balancoSemFlag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmadoGestor").value(false));

        String respStr = mockMvc.perform(get("/api/financeiro/indicadores")
                        .param("competencia", comp)
                        .header("Authorization", tokenC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prazosCiclos.estoqueFinalPeriodo").value(11000.00))
                .andExpect(jsonPath("$.prazosCiclos.estoqueInicialPeriodo").value(13000.00))
                .andExpect(jsonPath("$.prazosCiclos.estoqueMedioPeriodo").value(12000.00))
                .andExpect(jsonPath("$.prazosCiclos.contasReceberInicialPeriodo").value(4000.00))
                .andExpect(jsonPath("$.prazosCiclos.contasReceberFinalPeriodo").value(6000.00))
                .andExpect(jsonPath("$.prazosCiclos.vendasPrazoPeriodo").value(6000.00))
                .andExpect(jsonPath("$.prazosCiclos.fornecedoresInicialPeriodo").value(2000.00))
                .andExpect(jsonPath("$.prazosCiclos.fornecedoresFinalPeriodo").value(5000.00))
                .andExpect(jsonPath("$.prazosCiclos.comprasPrazoPeriodo").value(3000.00))
                .andExpect(jsonPath("$.liquidezEndividamento.statusConfiabilidadePatrimonial").value("DADOS_INSUFICIENTES"))
                .andReturn().getResponse().getContentAsString();

        JsonNode root = json.readTree(respStr);
        boolean prodAMantidoSemTroca = false;
        boolean prodBClassificadoCorretamente = false;
        for (JsonNode item : root.path("rentabilidade").path("porProduto")) {
            if (item.path("referenciaId").asLong() == prodA.getIdProduto()) {
                assertThat(item.path("quantidadeVendidaLiquida").decimalValue()).isEqualByComparingTo("5.00");
                assertThat(item.path("receitaBruta").decimalValue()).isEqualByComparingTo("900.00");
                prodAMantidoSemTroca = true;
            }
            if (item.path("referenciaId").asLong() == prodB.getIdProduto()) {
                assertThat(item.path("classificacaoRentabilidade").asText()).isEqualTo("BAIXO_VOLUME_E_BAIXA_MARGEM");
                prodBClassificadoCorretamente = true;
            }
        }
        assertThat(prodAMantidoSemTroca).isTrue();
        assertThat(prodBClassificadoCorretamente).isTrue();

        // 2. Balanco confirmado desbalanceado + PL <= 0 mantem liquidez calculada, bloqueia PCT/ROE e alerta desbalanceamento
        String balancoDesbPlNeg = json.writeValueAsString(Map.ofEntries(
                Map.entry("competenciaAnoMes", comp),
                Map.entry("usarSaldosOperacionaisErp", false),
                Map.entry("disponibilidadesCaixaBancos", new BigDecimal("10000.00")),
                Map.entry("contasReceberOperacionais", new BigDecimal("20000.00")),
                Map.entry("estoquesOperacionais", new BigDecimal("20000.00")),
                Map.entry("fornecedoresOperacionaisCp", new BigDecimal("45000.00")),
                Map.entry("capitalSocial", new BigDecimal("10000.00")),
                Map.entry("lucrosPrejuizosAcumulados", new BigDecimal("-15000.00")),
                Map.entry("confirmadoGestor", true)));
        mockMvc.perform(put("/api/financeiro/indicadores/balanco-patrimonial")
                        .header("Authorization", tokenC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(balancoDesbPlNeg))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consistenteContabilmente").value(false));

        String respPlNegStr = mockMvc.perform(get("/api/financeiro/indicadores")
                        .param("competencia", comp)
                        .header("Authorization", tokenC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liquidezEndividamento.balancoPatrimonialConfirmado").value(true))
                .andExpect(jsonPath("$.liquidezEndividamento.statusConfiabilidadePatrimonial").value("CALCULADO"))
                .andExpect(jsonPath("$.liquidezEndividamento.liquidezCorrente").value(1.11))
                .andExpect(jsonPath("$.liquidezEndividamento.participacaoCapitalTerceirosPercentual").doesNotExist())
                .andExpect(jsonPath("$.liquidezEndividamento.relacaoDividaPatrimonioLiquidoPercentual").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        JsonNode rootPlNeg = json.readTree(respPlNegStr);
        boolean roeDivisaoPorZero = false;
        for (JsonNode ind : rootPlNeg.path("catalogoIndicadores")) {
            if ("ROE_RETORNO_PATRIMONIO_LIQUIDO".equals(ind.path("codigo").asText())) {
                assertThat(ind.path("statusConfiabilidade").asText()).isEqualTo("DIVISAO_POR_ZERO");
                roeDivisaoPorZero = true;
            }
        }
        assertThat(roeDivisaoPorZero).isTrue();
        boolean temDiagnosticoDesbalanceado = false;
        for (JsonNode diag : rootPlNeg.path("diagnosticosExplicaveis")) {
            if ("BALANCO_PATRIMONIAL_DESBALANCEADO".equals(diag.path("codigoDiagnostico").asText())) {
                temDiagnosticoDesbalanceado = true;
            }
        }
        assertThat(temDiagnosticoDesbalanceado).isTrue();

        // 3. Simulacao com fluxo negativo no Periodo 1 (-500 != -10000 Capex) e validacao contra elemento nulo
        String invFluxoNeg1 = json.writeValueAsString(Map.of(
                "nomeProjeto", "Projeto com Carência Periodo 1",
                "investimentoInicial", new BigDecimal("10000.00"),
                "taxaDescontoPeriodicaPercentual", new BigDecimal("2.00"),
                "fluxosCaixaProjetados", List.of(
                        new BigDecimal("-500.00"),
                        new BigDecimal("6000.00"),
                        new BigDecimal("8000.00"))));
        mockMvc.perform(post("/api/financeiro/indicadores/investimentos/simular")
                        .header("Authorization", tokenC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invFluxoNeg1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roiSimuladoPercentual").value(35.00));

        String invComNulo = json.writeValueAsString(Map.of(
                "nomeProjeto", "Projeto Invalido",
                "investimentoInicial", new BigDecimal("10000.00"),
                "fluxosCaixaProjetados", Arrays.asList(new BigDecimal("1000.00"), null)));
        mockMvc.perform(post("/api/financeiro/indicadores/investimentos/simular")
                        .header("Authorization", tokenC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invComNulo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consolidaMultiFilialComBalancoParcialEPontosEquilibrioReaisPorFilial() throws Exception {
        Empresa empresa = new Empresa();
        empresa.setRazaoSocial("Rede Apex Multi Filial LTDA");
        empresa.setNomeFantasia("Rede Apex");
        empresa.setCnpj("44123456000177");
        empresa.setStatus(1);
        empresa = empresaRepository.save(empresa);

        Filial f1 = new Filial();
        f1.setEmpresa(empresa);
        f1.setNome("Filial Matriz Real");
        f1.setCodigo("F1");
        f1.setStatus(1);
        f1 = filialRepository.save(f1);

        Filial f2 = new Filial();
        f2.setEmpresa(empresa);
        f2.setNome("Filial Shopping Sem Balanco");
        f2.setCodigo("F2");
        f2.setStatus(1);
        f2 = filialRepository.save(f2);

        Long empId = empresa.getIdEmpresa();
        Long f1Id = f1.getIdFilial();
        Long f2Id = f2.getIdFilial();

        String tokenF1 = bearer(jwtTokenService.issueAccessToken(
                406L, "Gestor Matriz", "gestor_mf1", List.of("ROLE_DONO_GERENTE"), empId, f1Id, Duration.ofMinutes(30)));
        String tokenF2 = bearer(jwtTokenService.issueAccessToken(
                407L, "Gestor Shopping", "gestor_mf2", List.of("ROLE_DONO_GERENTE"), empId, f2Id, Duration.ofMinutes(30)));

        mockMvc.perform(post("/api/financeiro/plano-contas/inicializar-padrao").header("Authorization", tokenF1))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/financeiro/plano-contas/inicializar-padrao").header("Authorization", tokenF2))
                .andExpect(status().isOk());

        String comp = "2026-11";
        LocalDate dataRef = LocalDate.of(2026, 11, 12);

        // Filial 1: Receita = 2000, CMV = 1000 (MC = 50%), Aluguel Fixo = 200 -> PEC real = 200 / 0.50 = R$ 400,00
        // Possui Balanco Patrimonial salvo com disponibilidades = R$ 5.000,00
        Categoria cat1 = criarCategoria(empId, f1Id, "Geral F1");
        Produto prod1 = criarProduto(empId, f1Id, cat1, "Item F1", new BigDecimal("100.00"), new BigDecimal("200.00"), 0);
        Cliente cli1 = criarCliente(empId, f1Id, "Cliente F1");
        Usuario vend1 = criarUsuario(empId, f1Id, "vend_mf1");
        criarVendaFinalizada(empId, f1Id, cli1, vend1, dataRef,
                List.of(new ItemSeed(prod1, 10, new BigDecimal("200.00"), new BigDecimal("100.00"))));
        PlanoContasGerencial fixoF1 = buscarContaPorCodigo(empId, f1Id, "6.01");
        criarDespesa(empId, f1Id, fixoF1.getIdPlanoConta(), "Aluguel F1", new BigDecimal("200.00"), dataRef);

        String balancoF1 = json.writeValueAsString(Map.ofEntries(
                Map.entry("competenciaAnoMes", comp),
                Map.entry("usarSaldosOperacionaisErp", false),
                Map.entry("disponibilidadesCaixaBancos", new BigDecimal("5000.00")),
                Map.entry("capitalSocial", new BigDecimal("5000.00")),
                Map.entry("confirmadoGestor", true)));
        mockMvc.perform(put("/api/financeiro/indicadores/balanco-patrimonial")
                        .header("Authorization", tokenF1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(balancoF1))
                .andExpect(status().isOk());

        // Filial 2: Receita = 2000, CMV = 1000 (MC = 50%), Aluguel Fixo = 600 -> PEC real = 600 / 0.50 = R$ 1.200,00
        // NAO possui Balanco salvo, mas possui R$ 3.000,00 em ContaFinanceira
        Categoria cat2 = criarCategoria(empId, f2Id, "Geral F2");
        Produto prod2 = criarProduto(empId, f2Id, cat2, "Item F2", new BigDecimal("100.00"), new BigDecimal("200.00"), 0);
        Cliente cli2 = criarCliente(empId, f2Id, "Cliente F2");
        Usuario vend2 = criarUsuario(empId, f2Id, "vend_mf2");
        criarVendaFinalizada(empId, f2Id, cli2, vend2, dataRef,
                List.of(new ItemSeed(prod2, 10, new BigDecimal("200.00"), new BigDecimal("100.00"))));
        PlanoContasGerencial fixoF2 = buscarContaPorCodigo(empId, f2Id, "6.01");
        criarDespesa(empId, f2Id, fixoF2.getIdPlanoConta(), "Aluguel F2", new BigDecimal("600.00"), dataRef);

        ContaFinanceira bancoF2 = new ContaFinanceira();
        bancoF2.setEmpresaId(empId);
        bancoF2.setFilialId(f2Id);
        bancoF2.setCodigo("CX-F2");
        bancoF2.setNome("Banco Filial 2");
        bancoF2.setTipoConta(TipoContaFinanceira.CONTA_CORRENTE);
        bancoF2.setSaldoInicial(new BigDecimal("3000.00"));
        bancoF2.setSaldoAtual(new BigDecimal("3000.00"));
        bancoF2.setDataSaldoInicial(LocalDate.of(2026, 11, 1));
        bancoF2.setPadrao(true);
        bancoF2.setAtivo(true);
        bancoF2.setCriadoEm(LocalDateTime.now());
        bancoF2.setVersion(0L);
        contaFinanceiraRepository.save(bancoF2);

        String respStr = mockMvc.perform(get("/api/financeiro/indicadores")
                        .param("competencia", comp)
                        .param("consolidarFiliais", "true")
                        .header("Authorization", tokenF1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consolidadoEmpresa").value(true))
                .andExpect(jsonPath("$.balancoPatrimonial.disponibilidadesCaixaBancos").value(8000.00))
                .andExpect(jsonPath("$.balancoPatrimonial.confirmadoGestor").value(false))
                .andExpect(jsonPath("$.liquidezEndividamento.statusConfiabilidadePatrimonial").value("DADOS_INSUFICIENTES"))
                .andReturn().getResponse().getContentAsString();

        JsonNode root = json.readTree(respStr);
        JsonNode peFiliais = root.path("pontoEquilibrio").path("porFilial");
        assertThat(peFiliais.size()).isEqualTo(2);
        boolean achouF1 = false;
        boolean achouF2 = false;
        for (JsonNode pf : peFiliais) {
            if (pf.path("escopoId").asLong() == f1Id) {
                assertThat(pf.path("custosDespesasFixasAlocadas").decimalValue()).isEqualByComparingTo("200.00");
                assertThat(pf.path("pontoEquilibrioContabilReceita").decimalValue()).isEqualByComparingTo("400.00");
                achouF1 = true;
            }
            if (pf.path("escopoId").asLong() == f2Id) {
                assertThat(pf.path("custosDespesasFixasAlocadas").decimalValue()).isEqualByComparingTo("600.00");
                assertThat(pf.path("pontoEquilibrioContabilReceita").decimalValue()).isEqualByComparingTo("1200.00");
                achouF2 = true;
            }
        }
        assertThat(achouF1).isTrue();
        assertThat(achouF2).isTrue();
    }

    // =========================================================================
    // Helpers de Seed para Testes
    // =========================================================================

    private record ItemSeed(Produto produto, int quantidade, BigDecimal precoUnitario, BigDecimal custoUnitario) {
    }

    private Categoria criarCategoria(Long empresaId, Long filialId, String nome) {
        Categoria c = new Categoria();
        c.setEmpresaId(empresaId);
        c.setFilialId(filialId);
        c.setDescricao(nome);
        c.setStatus(1);
        return categoriaRepository.save(c);
    }

    private Fornecedor criarFornecedor(Long empresaId, Long filialId, String razao, String cnpj) {
        Endereco end = new Endereco();
        end.setEstado("SP");
        end.setCidade("Sao Paulo");
        end.setCep("01000-000");
        end.setBairro("Centro");
        end.setLogradouro("Rua Lote 4");
        end.setUf("SP");
        end.setNumero("400");
        end.setEmpresaId(empresaId);
        end.setFilialId(filialId);

        Fornecedor f = new Fornecedor();
        f.setEmpresaId(empresaId);
        f.setFilialId(filialId);
        f.setRazaoSocial(razao);
        f.setNomeFantasia(razao.length() > 45 ? razao.substring(0, 45) : razao);
        f.setCnpj(cnpj);
        f.setTelefone("11988887777");
        f.setDataCadastro(LocalDateTime.now());
        f.setEndereco(end);
        f.setStatus(1);
        return fornecedorRepository.save(f);
    }

    private Produto criarProduto(Long empresaId, Long filialId, Categoria cat, String desc, BigDecimal custo, BigDecimal venda, int estoque) {
        Produto p = new Produto();
        p.setEmpresaId(empresaId);
        p.setFilialId(filialId);
        p.setCategoria(cat);
        p.setDescricao(desc);
        p.setCusto(custo);
        p.setValorVenda(venda);
        p.setQuantidadeEstoque(estoque);
        p.setEstoqueMinimo(2);
        p.setStatus(1);
        p.setVersion(0L);
        return produtoRepository.save(p);
    }

    private Cliente criarCliente(Long empresaId, Long filialId, String nome) {
        Cliente c = new Cliente();
        c.setEmpresaId(empresaId);
        c.setFilialId(filialId);
        c.setNomeRazao(nome);
        c.setTelefone("11977776666");
        c.setTipoDocumento(1);
        c.setCpfCnpj(String.format("%011d", System.nanoTime() % 100000000000L));
        c.setDataCadastro(LocalDateTime.now());
        c.setStatus(1);
        return clienteRepository.save(c);
    }

    private Usuario criarUsuario(Long empresaId, Long filialId, String login) {
        Perfil perfil = perfilRepository.findByNome("VENDEDOR").orElseGet(() -> {
            Perfil p = new Perfil();
            p.setNome("VENDEDOR");
            p.setStatus(1);
            return perfilRepository.save(p);
        });
        Usuario u = new Usuario();
        u.setEmpresaId(empresaId);
        u.setFilialId(filialId);
        u.setNome("Operador " + login);
        u.setLogin(login);
        u.setSenha("hash");
        u.setDataNascimento(LocalDate.of(1990, 1, 1));
        u.setPerfil(perfil);
        u.setStatus(1);
        return usuarioRepository.save(u);
    }

    private Venda criarVendaFinalizada(
            Long empresaId, Long filialId, Cliente cliente, Usuario usuario, LocalDate data, List<ItemSeed> seeds) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemSeed s : seeds) {
            total = total.add(s.precoUnitario().multiply(BigDecimal.valueOf(s.quantidade())));
        }
        Venda v = new Venda();
        v.setEmpresaId(empresaId);
        v.setFilialId(filialId);
        v.setCliente(cliente);
        v.setUsuario(usuario);
        v.setStatus(1);
        v.setStatusOperacional(VendaStatus.FINALIZADA);
        v.setValorTotal(total);
        v.setDesconto(BigDecimal.ZERO);
        v.setDataVenda(data.atTime(11, 0));
        v.setDataCompetencia(data);
        v.setDataEmissao(data);
        v.setEstoqueMovimentado(true);
        v.setVersion(0L);
        v.setItens(new ArrayList<>());
        Venda salva = vendaRepository.save(v);

        for (ItemSeed s : seeds) {
            ProdutoVenda pv = new ProdutoVenda();
            pv.setId(new ProdutoVendaId(s.produto().getIdProduto(), salva.getIdVenda()));
            pv.setProduto(s.produto());
            pv.setVenda(salva);
            pv.setQuantidade(s.quantidade());
            pv.setPrecoUnitario(s.precoUnitario());
            pv.setCustoUnitario(s.custoUnitario());
            pv.setPrecoTotal(s.precoUnitario().multiply(BigDecimal.valueOf(s.quantidade())));
            salva.getItens().add(pv);
        }
        return vendaRepository.save(salva);
    }

    private Despesa criarDespesa(Long empresaId, Long filialId, Long planoContaId, String desc, BigDecimal valor, LocalDate data) {
        TipoDespesa td = tipoDespesaRepository.findByEmpresaIdAndFilialIdOrderByNomeAsc(empresaId, filialId)
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    TipoDespesa novo = new TipoDespesa();
                    novo.setEmpresaId(empresaId);
                    novo.setFilialId(filialId);
                    novo.setNome("Operacional Geral");
                    novo.setStatus(1);
                    return tipoDespesaRepository.save(novo);
                });
        Despesa d = new Despesa();
        d.setEmpresaId(empresaId);
        d.setFilialId(filialId);
        d.setTipoDespesa(td);
        d.setPlanoContaId(planoContaId);
        d.setDescricao(desc);
        d.setValor(valor);
        d.setDataVencimento(data);
        d.setDataCompetencia(data);
        d.setDataEmissao(data);
        d.setStatus(1);
        d.setVersion(0L);
        return despesaRepository.save(d);
    }

    private PlanoContasGerencial buscarContaPorCodigo(Long empresaId, Long filialId, String codigo) {
        return planoContasRepository.findByCodigoAndEmpresaIdAndFilialId(codigo, empresaId, filialId).orElseThrow();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
