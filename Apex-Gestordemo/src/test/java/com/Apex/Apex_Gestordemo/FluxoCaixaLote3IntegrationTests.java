package com.Apex.Apex_Gestordemo;

import DTO.BaixaTituloFinanceiroRequestDTO;
import DTO.ConciliacaoManualRequestDTO;
import DTO.ContaFinanceiraRequestDTO;
import DTO.EstornoMovimentacaoRequestDTO;
import DTO.FluxoCaixaSimulacaoRequestDTO;
import DTO.GerarParcelasRequestDTO;
import DTO.ImportacaoExtratoRequestDTO;
import DTO.TransferenciaInternaRequestDTO;
import Model.CaixaMovimentacao;
import Model.CaixaSessao;
import Model.CaixaStatus;
import Model.Categoria;
import Model.Cliente;
import Model.ContaPagar;
import Model.ContaReceber;
import Model.Despesa;
import Model.Empresa;
import Model.Endereco;
import Model.Filial;
import Model.FinanceiroStatus;
import Model.FormaPagamento;
import Model.Fornecedor;
import Model.ModalidadeRecebivel;
import Model.Perfil;
import Model.PlanoContasGerencial;
import Model.Produto;
import Model.ProdutoVenda;
import Model.ProdutoVendaId;
import Model.StatusConciliacaoFinanceira;
import Model.TipoContaFinanceira;
import Model.TipoDespesa;
import Model.Usuario;
import Model.Venda;
import Model.VendaStatus;
import Repository.CaixaMovimentacaoRepository;
import Repository.CaixaSessaoRepository;
import Repository.CategoriaRepository;
import Repository.ClienteRepository;
import Repository.ContaPagarRepository;
import Repository.ContaReceberRepository;
import Repository.DespesaRepository;
import Repository.EmpresaRepository;
import Repository.FilialRepository;
import Repository.FormaPagamentoRepository;
import Repository.FornecedorRepository;
import Repository.PerfilRepository;
import Repository.PlanoContasGerencialRepository;
import Repository.ProdutoRepository;
import Repository.TipoDespesaRepository;
import Repository.UsuarioRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class FluxoCaixaLote3IntegrationTests {

    private static final AtomicInteger SEQ = new AtomicInteger(8000);

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
    private CaixaSessaoRepository caixaSessaoRepository;

    @Autowired
    private CaixaMovimentacaoRepository caixaMovimentacaoRepository;

    @Test
    void deveCumprirCaixaRealizadoTransferenciasEstornosEPagamentosParciaisSemInflarTotais() throws Exception {
        TenantContext t = createTenant("realizado");
        String token = loginToken(t.dono.getLogin());
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());

        // 1. Bootstrap contas financeiras padrao + cria conta bancaria com saldo inicial R$ 5.000,00
        String bodyBoot = mvc.perform(post("/api/financeiro/fluxo-caixa/contas/bootstrap")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode contasBoot = json.readTree(bodyBoot);
        assertThat(contasBoot.size()).isGreaterThanOrEqualTo(3);

        Long caixaLojaId = findContaFinanceiraIdByCodigo(contasBoot, "CX-LOJA");

        ContaFinanceiraRequestDTO novaContaReq = new ContaFinanceiraRequestDTO(
                "BANCO-ITAU",
                "Banco Itau Conta Movimento",
                TipoContaFinanceira.CONTA_CORRENTE,
                "341",
                "Itau",
                "0001",
                "99887-6",
                bd("5000.00"),
                inicioMes,
                false,
                true);
        String bodyNovaConta = mvc.perform(post("/api/financeiro/fluxo-caixa/contas")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(novaContaReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long bancoItauId = json.readTree(bodyNovaConta).get("idContaFinanceira").asLong();

        // 2. Recebimento parcial de Conta a Receber (R$ 1.000,00 -> baixa parcial de R$ 400,00)
        Cliente cli = saveCliente(t, "Cliente Realizado Lote3");
        FormaPagamento pix = saveForma(t, "PIX", 1);
        ContaReceber cr = saveContaReceber(t, null, cli.getIdCliente(), pix.getIdFormaPagamento(),
                bd("1000.00"), BigDecimal.ZERO, FinanceiroStatus.ABERTO, hoje, null, null);

        BaixaTituloFinanceiroRequestDTO baixaParcialReceber = new BaixaTituloFinanceiroRequestDTO(
                bd("400.00"),
                bancoItauId,
                hoje,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false,
                "Recebimento parcial PIX 1/2",
                null);
        String bodyMovRec = mvc.perform(post("/api/financeiro/fluxo-caixa/contas-receber/" + cr.getIdContaReceber() + "/baixar")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "idem-rec-parcial-" + cr.getIdContaReceber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(baixaParcialReceber)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long movRecId = json.readTree(bodyMovRec).get("idMovimentacaoFinanceira").asLong();

        // Verifica idempotencia/duplicidade: repetir mesma Idempotency-Key retorna o mesmo registro sem duplicar saldo!
        String bodyMovRecIdem = mvc.perform(post("/api/financeiro/fluxo-caixa/contas-receber/" + cr.getIdContaReceber() + "/baixar")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "idem-rec-parcial-" + cr.getIdContaReceber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(baixaParcialReceber)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(bodyMovRecIdem).get("idMovimentacaoFinanceira").asLong()).isEqualTo(movRecId);

        ContaReceber crAposParcial = contaReceberRepository.findById(cr.getIdContaReceber()).orElseThrow();
        assertThat(crAposParcial.getStatus()).isEqualTo(FinanceiroStatus.PARCIAL);
        assertThat(crAposParcial.getValorPago()).isEqualByComparingTo("400.00");
        assertThat(crAposParcial.getSaldo()).isEqualByComparingTo("600.00");

        // 3. Pagamento parcial de Conta a Pagar (R$ 800,00 -> baixa parcial de R$ 300,00)
        Fornecedor forn = saveFornecedor(t, "Fornecedor Realizado Lote3");
        Categoria cat = saveCategoria(t, "Insumos Lote3");
        ContaPagar cp = saveContaPagar(t, forn.getIdFornecedor(), cat.getIdCategoria(),
                "Fornecedor Mercadorias", bd("800.00"), hoje, FinanceiroStatus.ABERTO, null, null);

        BaixaTituloFinanceiroRequestDTO baixaParcialPagar = new BaixaTituloFinanceiroRequestDTO(
                bd("300.00"),
                bancoItauId,
                hoje,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false,
                "Pagamento parcial boleto fornecedor",
                null);
        String bodyMovPag = mvc.perform(post("/api/financeiro/fluxo-caixa/contas-pagar/" + cp.getIdContaPagar() + "/baixar")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "idem-pag-parcial-" + cp.getIdContaPagar())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(baixaParcialPagar)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long movPagId = json.readTree(bodyMovPag).get("idMovimentacaoFinanceira").asLong();

        ContaPagar cpAposParcial = contaPagarRepository.findById(cp.getIdContaPagar()).orElseThrow();
        assertThat(cpAposParcial.getStatus()).isEqualTo(FinanceiroStatus.PARCIAL);
        assertThat(cpAposParcial.getValorPago()).isEqualByComparingTo("300.00");
        assertThat(cpAposParcial.getSaldo()).isEqualByComparingTo("500.00");

        // 4. Transferencia entre contas proprias (R$ 500,00 do Banco Itau para Caixa Loja)
        // Deve alterar saldos individuais, mas ser NEUTRA nas entradas/saidas consolidadas da empresa!
        TransferenciaInternaRequestDTO transfReq = new TransferenciaInternaRequestDTO(
                bancoItauId,
                caixaLojaId,
                bd("500.00"),
                hoje,
                "Suprimento de troco para o caixa fisico",
                "TRANSF-001",
                null);
        mvc.perform(post("/api/financeiro/fluxo-caixa/transferencias")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "idem-transf-001-" + t.filialId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(transfReq)))
                .andExpect(status().isCreated());

        // Verifica painel consolidado: Entradas = 400.00, Saidas = 300.00, Transferencias = 500.00 (nao infla entradas/saidas)
        JsonNode painel = consultarPainel(token, inicioMes, fimMes, null, false);
        assertThat(bd(painel.get("entradasRealizadasPeriodo").asText())).isEqualByComparingTo("400.00");
        assertThat(bd(painel.get("saidasRealizadasPeriodo").asText())).isEqualByComparingTo("300.00");
        assertThat(bd(painel.get("geracaoLiquidaCaixaRealizado").asText())).isEqualByComparingTo("100.00");
        assertThat(bd(painel.get("transferenciasInternasVolume").asText())).isEqualByComparingTo("500.00");
        assertThat(bd(painel.get("saldoFinalRealizado").asText())).isEqualByComparingTo("5100.00");

        // 5. Estorno da baixa parcial de Conta a Pagar (R$ 300,00) -> restaura saldo da ContaPagar para R$ 800,00 e recompoe banco
        EstornoMovimentacaoRequestDTO estornoReq = new EstornoMovimentacaoRequestDTO(
                "Pagamento lancado em duplicidade, estornando para correcao",
                hoje);
        mvc.perform(post("/api/financeiro/fluxo-caixa/movimentacoes/" + movPagId + "/estornar")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(estornoReq)))
                .andExpect(status().isOk());

        ContaPagar cpAposEstorno = contaPagarRepository.findById(cp.getIdContaPagar()).orElseThrow();
        assertThat(cpAposEstorno.getStatus()).isEqualTo(FinanceiroStatus.ABERTO);
        assertThat(cpAposEstorno.getValorPago()).isEqualByComparingTo("0.00");
        assertThat(cpAposEstorno.getSaldo()).isEqualByComparingTo("800.00");

        JsonNode painelPosEstorno = consultarPainel(token, inicioMes, fimMes, null, false);
        assertThat(bd(painelPosEstorno.get("entradasRealizadasPeriodo").asText())).isEqualByComparingTo("400.00");
        assertThat(bd(painelPosEstorno.get("saidasRealizadasPeriodo").asText())).isEqualByComparingTo("0.00");
        assertThat(bd(painelPosEstorno.get("totalEstornosPeriodo").asText())).isEqualByComparingTo("300.00");
        assertThat(bd(painelPosEstorno.get("saldoFinalRealizado").asText())).isEqualByComparingTo("5400.00");
    }

    @Test
    void deveLiquidarCartaoEAnteciparRecebiveisDescontandoTaxasNoCaixaERegistrandoDespesaFinanceiraNaDre() throws Exception {
        TenantContext t = createTenant("cartao");
        String token = loginToken(t.dono.getLogin());
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());

        // Inicializa plano de contas e DRE (Lote 1 & Lote 2)
        mvc.perform(post("/api/financeiro/plano-contas/inicializar-padrao")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/financeiro/fluxo-caixa/contas/bootstrap")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        PlanoContasGerencial contaReceitaVendas = contaPorCodigo(t, "1.01");
        Categoria cat = saveCategoria(t, "Eletronicos Cartao");
        Fornecedor forn = saveFornecedor(t, "Fornecedor Eletronicos");
        Cliente cli = saveCliente(t, "Cliente Cartao Lote3");
        Produto prod = saveProduto(t, cat, forn, "Notebook Pro", bd("1200.00"), bd("2000.00"));

        // Venda de R$ 2.000,00 na competencia de hoje
        Venda venda = saveVenda(t, cli, prod, 1, bd("2000.00"), bd("1200.00"), hoje,
                VendaStatus.FINALIZADA, contaReceitaVendas.getIdPlanoConta());

        // Gera 2 parcelas de Cartao de Credito (R$ 1.000,00 cada) com taxa operadora de 3% (R$ 30,00 cada)
        GerarParcelasRequestDTO parcReq = new GerarParcelasRequestDTO(
                "RECEBER",
                "Venda Cartao 2x " + venda.getNumeroVenda(),
                bd("2000.00"),
                2,
                30,
                hoje.plusDays(15),
                hoje,
                venda.getIdVenda(),
                cli.getIdCliente(),
                null,
                null,
                null,
                contaReceitaVendas.getIdPlanoConta(),
                null,
                null,
                ModalidadeRecebivel.CARTAO_CREDITO,
                bd("3.00"),
                15,
                false,
                null);

        String bodyParcelas = mvc.perform(post("/api/financeiro/fluxo-caixa/parcelas/gerar")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(parcReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode parcelas = json.readTree(bodyParcelas);
        assertThat(parcelas.size()).isEqualTo(2);

        Long idParcela1 = parcelas.get(0).get("registroId").asLong();
        Long idParcela2 = parcelas.get(1).get("registroId").asLong();
        assertThat(bd(parcelas.get(0).get("valorBruto").asText())).isEqualByComparingTo("1000.00");
        assertThat(bd(parcelas.get(0).get("valorTaxaPrevista").asText())).isEqualByComparingTo("30.00");
        assertThat(bd(parcelas.get(0).get("saldoAbertoLiquido").asText())).isEqualByComparingTo("970.00");

        // Verifica no diagnostico de liquidez que os repasses previstos de cartao mostram Bruto = 2000, Taxas = 60, Liquido = 1940
        JsonNode painelAntes = consultarPainel(token, inicioMes, fimMes.plusDays(60), null, false);
        JsonNode diagAntes = painelAntes.get("diagnosticoLiquidez");
        assertThat(bd(diagAntes.get("repassesCartaoPendentesBruto").asText())).isEqualByComparingTo("2000.00");
        assertThat(bd(diagAntes.get("taxasCartaoPrevistas").asText())).isEqualByComparingTo("60.00");
        assertThat(bd(diagAntes.get("repassesCartaoPendentesLiquido").asText())).isEqualByComparingTo("1940.00");

        // Liquida Parcela 1 com antecipacao: Taxa operadora R$ 30,00 + Taxa antecipacao R$ 20,00 => Liquido no caixa = R$ 950,00
        BaixaTituloFinanceiroRequestDTO baixaAntecipada = new BaixaTituloFinanceiroRequestDTO(
                bd("1000.00"),
                null,
                hoje,
                bd("30.00"),
                bd("20.00"),
                BigDecimal.ZERO,
                true,
                "Antecipacao recebivel cartao parcela 1/2",
                null);
        String bodyBaixa1 = mvc.perform(post("/api/financeiro/fluxo-caixa/contas-receber/" + idParcela1 + "/baixar")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "idem-antecipacao-" + idParcela1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(baixaAntecipada)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode movBaixa1 = json.readTree(bodyBaixa1);
        Long movBaixa1Id = movBaixa1.get("idMovimentacaoFinanceira").asLong();
        assertThat(bd(movBaixa1.get("valorBruto").asText())).isEqualByComparingTo("1000.00");
        assertThat(bd(movBaixa1.get("valorTaxa").asText())).isEqualByComparingTo("50.00");
        assertThat(bd(movBaixa1.get("valorLiquido").asText())).isEqualByComparingTo("950.00");
        assertThat(movBaixa1.get("origemTipo").asText()).isEqualTo("ANTECIPACAO_RECEBIVEL");

        // Reuso da mesma Idempotency-Key em outro titulo/operacao deve retornar 409 Conflict
        mvc.perform(post("/api/financeiro/fluxo-caixa/contas-receber/" + idParcela2 + "/baixar")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "idem-antecipacao-" + idParcela1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(baixaAntecipada)))
                .andExpect(status().isConflict());

        // Verifica que o Caixa Realizado NAO deduz a taxa duas vezes (entradas = 950.00, saidas = 0.00, saldoFinal = 950.00)
        JsonNode painelPosBaixa = consultarPainel(token, inicioMes, fimMes.plusDays(60), null, false);
        assertThat(bd(painelPosBaixa.get("entradasRealizadasPeriodo").asText())).isEqualByComparingTo("950.00");
        assertThat(bd(painelPosBaixa.get("saidasRealizadasPeriodo").asText())).isEqualByComparingTo("0.00");
        assertThat(bd(painelPosBaixa.get("geracaoLiquidaCaixaRealizado").asText())).isEqualByComparingTo("950.00");
        assertThat(bd(painelPosBaixa.get("saldoFinalRealizado").asText())).isEqualByComparingTo("950.00");

        ContaReceber cr1 = contaReceberRepository.findById(idParcela1).orElseThrow();
        assertThat(cr1.getStatus()).isEqualTo(FinanceiroStatus.PAGO);
        assertThat(cr1.isAntecipado()).isTrue();
        assertThat(cr1.getTaxaAntecipacao()).isEqualByComparingTo("20.00");
        assertThat(idParcela2).isPositive();

        // Verifica consistencia com a DRE (Lote 2):
        // - Receita Bruta da DRE (Linha 1) permanece R$ 2.000,00 (nao reduzida pela taxa do cartao!)
        // - Resultado Financeiro Liquido da DRE (Linha 14) reflete -R$ 50,00 de despesa financeira (conta 9.01)!
        String competenciaMes = hoje.toString().substring(0, 7);
        String bodyDre = mvc.perform(get("/api/financeiro/dre")
                        .header("Authorization", bearer(token))
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", competenciaMes))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode dre = json.readTree(bodyDre);
        assertThat(bd(dre.get("indicadores").get("receitaBruta").asText())).isEqualByComparingTo("2000.00");
        assertThat(bd(dre.get("indicadores").get("resultadoFinanceiroLiquido").asText())).isEqualByComparingTo("-50.00");

        // Estorna a liquidacao com antecipacao e garante que:
        // 1) A despesa financeira 9.01 automatica e cancelada (resultadoFinanceiroLiquido volta a 0.00 na DRE)
        // 2) O saldo aberto de cr1 volta exatamente a 970.00 (e nao 1000.00), com antecipado=false e taxaAntecipacao=0.00
        mvc.perform(post("/api/financeiro/fluxo-caixa/movimentacoes/" + movBaixa1Id + "/estornar")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new EstornoMovimentacaoRequestDTO("Estorno teste antecipacao", hoje))))
                .andExpect(status().isOk());

        ContaReceber cr1Estornado = contaReceberRepository.findById(idParcela1).orElseThrow();
        assertThat(cr1Estornado.getStatus()).isEqualTo(FinanceiroStatus.ABERTO);
        assertThat(cr1Estornado.isAntecipado()).isFalse();
        assertThat(cr1Estornado.getTaxaAntecipacao()).isEqualByComparingTo("0.00");
        assertThat(cr1Estornado.getSaldo()).isEqualByComparingTo("970.00");

        String bodyDrePosEstorno = mvc.perform(get("/api/financeiro/dre")
                        .header("Authorization", bearer(token))
                        .param("tipoPeriodo", "MENSAL")
                        .param("competencia", competenciaMes))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode drePosEstorno = json.readTree(bodyDrePosEstorno);
        assertThat(bd(drePosEstorno.get("indicadores").get("resultadoFinanceiroLiquido").asText())).isEqualByComparingTo("0.00");
    }

    @Test
    void deveSepararVencidosDeAVencerCalcularProjecoes7A90DiasESimularSemAlterarCaixaRealizado() throws Exception {
        TenantContext t = createTenant("projecoes");
        String token = loginToken(t.dono.getLogin());
        LocalDate hoje = LocalDate.now();

        mvc.perform(post("/api/financeiro/fluxo-caixa/contas/bootstrap")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        Cliente cli = saveCliente(t, "Cliente Inadimplente");
        Fornecedor forn = saveFornecedor(t, "Fornecedor Compromissos");
        Categoria cat = saveCategoria(t, "Operacional");
        FormaPagamento boleto = saveForma(t, "Boleto", 2);
        TipoDespesa td = saveTipoDespesa(t, "Energia");

        // 1. Conta a receber vencida (inadimplente ha 10 dias: R$ 600,00) e Conta a receber a vencer (+10 dias: R$ 1.400,00)
        saveContaReceber(t, null, cli.getIdCliente(), boleto.getIdFormaPagamento(),
                bd("600.00"), BigDecimal.ZERO, FinanceiroStatus.ABERTO, hoje.minusDays(10), null, null);
        saveContaReceber(t, null, cli.getIdCliente(), boleto.getIdFormaPagamento(),
                bd("1400.00"), BigDecimal.ZERO, FinanceiroStatus.ABERTO, hoje.plusDays(10), null, null);

        // 2. Conta a pagar vencida (-5 dias: R$ 500,00), Conta a pagar a vencer (+7 dias: R$ 1.200,00) e Despesa vencida (-3 dias: R$ 200,00)
        saveContaPagar(t, forn.getIdFornecedor(), cat.getIdCategoria(),
                "Boleto Fornecedor Atrasado", bd("500.00"), hoje.minusDays(5), FinanceiroStatus.ABERTO, null, null);
        saveContaPagar(t, forn.getIdFornecedor(), cat.getIdCategoria(),
                "Boleto Fornecedor Proxima Semana", bd("1200.00"), hoje.plusDays(7), FinanceiroStatus.ABERTO, null, null);
        saveDespesa(t, td, "Conta de Luz Vencida", bd("200.00"), hoje.minusDays(3), null, 1, null);

        JsonNode painel = consultarPainel(token, hoje.minusDays(15), hoje.plusDays(30), null, false);
        assertThat(bd(painel.get("entradasPrevistasVencidas").asText())).isEqualByComparingTo("600.00");
        assertThat(bd(painel.get("entradasPrevistasPendentes").asText())).isEqualByComparingTo("1400.00");
        assertThat(bd(painel.get("saidasPrevistasVencidas").asText())).isEqualByComparingTo("700.00"); // 500 + 200
        assertThat(bd(painel.get("saidasPrevistasPendentes").asText())).isEqualByComparingTo("1200.00");

        // 3. Verifica que os 3 cenarios (OTIMISTA, REALISTA, PESSIMISTA) possuem horizontes de 7, 15, 30, 60 e 90 dias
        JsonNode cenarios = painel.get("cenariosProjecao");
        assertThat(cenarios.size()).isEqualTo(3);
        JsonNode realista = cenarios.get(1);
        assertThat(realista.get("codigoCenario").asText()).isEqualTo("REALISTA");
        assertThat(realista.hasNonNull("saldoProjetado7Dias")).isTrue();
        assertThat(realista.hasNonNull("saldoProjetado15Dias")).isTrue();
        assertThat(realista.hasNonNull("saldoProjetado30Dias")).isTrue();
        assertThat(realista.hasNonNull("saldoProjetado60Dias")).isTrue();
        assertThat(realista.hasNonNull("saldoProjetado90Dias")).isTrue();

        // 4. Diagnostico de liquidez identifica inadimplencia (600 / 2000 = 30%) e obrigacoes vencidas
        JsonNode diag = painel.get("diagnosticoLiquidez");
        assertThat(bd(diag.get("taxaInadimplenciaPercentual").asText())).isEqualByComparingTo("30.00");
        assertThat(bd(diag.get("recebiveisInadimplentesTotais").asText())).isEqualByComparingTo("600.00");
        assertThat(bd(diag.get("obrigacoesVencidasTotais").asText())).isEqualByComparingTo("700.00");
        assertThat(diag.get("alertasRiscoLiquidez").size()).isGreaterThan(0);

        // 5. Simulador de liquidez (POST /simular) aplica novo investimento de R$ 10.000,00 sem alterar o caixa real
        FluxoCaixaSimulacaoRequestDTO simReq = new FluxoCaixaSimulacaoRequestDTO(
                90,
                BigDecimal.ZERO,
                bd("1500.00"),
                bd("10000.00"),
                hoje.plusDays(5),
                false,
                BigDecimal.ZERO,
                List.of());

        String bodySim = mvc.perform(post("/api/financeiro/fluxo-caixa/simular")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(simReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode cenariosSim = json.readTree(bodySim);
        assertThat(cenariosSim.size()).isEqualTo(3);
        assertThat(cenariosSim.get(1).get("riscoInsuficienciaCaixa").asBoolean()).isTrue();
        assertThat(bd(cenariosSim.get(1).get("necessidadeCapitalGiroAdicional").asText())).isGreaterThan(BigDecimal.ZERO);

        // Reconsulta painel real e garante que NADA foi alterado pela simulacao
        JsonNode painelRealDepois = consultarPainel(token, hoje.minusDays(15), hoje.plusDays(30), null, false);
        assertThat(bd(painelRealDepois.get("saidasPrevistasPendentes").asText())).isEqualByComparingTo("1200.00");
        assertThat(bd(painelRealDepois.get("saldoFinalRealizado").asText())).isEqualByComparingTo("0.00");
    }

    @Test
    void deveConciliarExtratoCsvEOfxDetectarDivergenciaEDuplicidadeESessaoCaixaComDiferenca() throws Exception {
        TenantContext t = createTenant("conciliacao");
        String token = loginToken(t.dono.getLogin());
        LocalDate hoje = LocalDate.now();

        String bodyBoot = mvc.perform(post("/api/financeiro/fluxo-caixa/contas/bootstrap")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long contaBancoId = findContaFinanceiraIdByCodigo(json.readTree(bodyBoot), "BC-PRINCIPAL");

        // Registra 2 recebimentos no sistema hoje:
        // 1) R$ 350,00 para pareamento exato (CONCILIADO_EXATO)
        // 2) R$ 500,00 no sistema, mas no extrato entrara R$ 485,00 -> DIVERGENTE
        Cliente cli = saveCliente(t, "Cliente Conciliacao");
        FormaPagamento pix = saveForma(t, "PIX", 1);
        ContaReceber crExato = saveContaReceber(t, null, cli.getIdCliente(), pix.getIdFormaPagamento(),
                bd("350.00"), BigDecimal.ZERO, FinanceiroStatus.ABERTO, hoje, null, null);
        ContaReceber crDivergente = saveContaReceber(t, null, cli.getIdCliente(), pix.getIdFormaPagamento(),
                bd("500.00"), BigDecimal.ZERO, FinanceiroStatus.ABERTO, hoje, null, null);

        mvc.perform(post("/api/financeiro/fluxo-caixa/contas-receber/" + crExato.getIdContaReceber() + "/baixar")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new BaixaTituloFinanceiroRequestDTO(
                                bd("350.00"), contaBancoId, hoje, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, "PIX 350", null))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/financeiro/fluxo-caixa/contas-receber/" + crDivergente.getIdContaReceber() + "/baixar")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new BaixaTituloFinanceiroRequestDTO(
                                bd("500.00"), contaBancoId, hoje, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, "CARTAO 500", null))))
                .andExpect(status().isOk());

        // Cria sessao de caixa PDV fechada com diferenca de fechamento (-R$ 25,00)
        CaixaSessao sessao = new CaixaSessao();
        sessao.setOperadorLogin(t.dono.getLogin());
        sessao.setStatus(CaixaStatus.FECHADO);
        sessao.setAbertoEm(hoje.atTime(8, 0));
        sessao.setFechadoEm(hoje.atTime(18, 0));
        sessao.setSaldoInicial(bd("200.00"));
        sessao.setTotalVendas(bd("350.00"));
        sessao.setTotalSuprimentos(BigDecimal.ZERO);
        sessao.setTotalSangrias(bd("50.00"));
        sessao.setSaldoFinal(bd("500.00"));
        sessao.setSaldoInformado(bd("475.00"));
        sessao.setDiferencaFechamento(bd("-25.00"));
        sessao.setEmpresaId(t.empresaId);
        sessao.setFilialId(t.filialId);
        sessao = caixaSessaoRepository.save(sessao);

        CaixaMovimentacao sangria = new CaixaMovimentacao();
        sangria.setCaixaSessao(sessao);
        sangria.setTipo("SANGRIA");
        sangria.setValor(bd("50.00"));
        sangria.setObservacao("Sangria cofre");
        sangria.setCriadoEm(hoje.atTime(14, 0));
        caixaMovimentacaoRepository.save(sangria);

        // Importa extrato CSV (formato: data;descricao;valor;natureza;id) com:
        // 1) Linha de R$ 350,00 CREDITO -> CONCILIADO_EXATO
        // 2) Linha de R$ 485,00 CREDITO -> DIVERGENTE (contra a movimentacao de R$ 500,00 da mesma data)
        // 3) Linha repetida de R$ 350,00 CREDITO na mesma data -> DUPLICIDADE_SUSPEITA
        // 4) Linha sem par de R$ 123,45 DEBITO -> PENDENTE
        String csv = String.join("\n",
                "data;descricao;valor;natureza;id",
                hoje + ";PIX RECEBIDO 350;350.00;CREDITO;FIT-001",
                hoje + ";REPASSE CARTAO 500;485.00;CREDITO;FIT-002",
                hoje + ";PIX RECEBIDO 350 DUPLICADO;350.00;CREDITO;FIT-001-DUP",
                hoje + ";TARIFA BANCARIA AVULSA;123.45;DEBITO;FIT-003");

        ImportacaoExtratoRequestDTO impCsv = new ImportacaoExtratoRequestDTO(
                contaBancoId, "BANCARIA", "CSV", csv, null);
        String bodyConc = mvc.perform(post("/api/financeiro/fluxo-caixa/conciliacao/importar")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(impCsv)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode conc = json.readTree(bodyConc);

        assertThat(conc.get("conciliadosExatos").asInt()).isEqualTo(1);
        assertThat(conc.get("divergentes").asInt()).isEqualTo(1);
        assertThat(conc.get("duplicidadesSuspeitas").asInt()).isEqualTo(1);
        assertThat(conc.get("pendentes").asInt()).isEqualTo(1);
        assertThat(conc.get("sessoesCaixaComDiferencaFechamento").asInt()).isEqualTo(1);
        assertThat(bd(conc.get("diferencaTotalFechamentoCaixa").asText())).isEqualByComparingTo("-25.00");

        // Concilia manualmente o item PENDENTE
        Long idItemPendente = null;
        for (JsonNode item : conc.get("itens")) {
            if ("PENDENTE".equals(item.get("statusConciliacao").asText())) {
                idItemPendente = item.get("idConciliacaoItem").asLong();
                break;
            }
        }
        assertThat(idItemPendente).isNotNull();

        ConciliacaoManualRequestDTO manReq = new ConciliacaoManualRequestDTO(
                idItemPendente,
                null,
                "DESPESA_TARIFA",
                null,
                StatusConciliacaoFinanceira.CONCILIADO_MANUAL,
                "Tarifa conferida manualmente");
        String bodyMan = mvc.perform(post("/api/financeiro/fluxo-caixa/conciliacao/conciliar")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(manReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(bodyMan).get("statusConciliacao").asText()).isEqualTo("CONCILIADO_MANUAL");
    }

    @Test
    void deveGarantirIsolamentoMultiempresaConsolidacaoMultifilialERbacEstritoNoFluxoDeCaixa() throws Exception {
        TenantContext tA = createTenant("empA");
        TenantContext tB = createTenant("empB");
        String tokenA = loginToken(tA.dono.getLogin());
        String tokenB = loginToken(tB.dono.getLogin());
        LocalDate hoje = LocalDate.now();

        // Cria Filial 2 na Empresa A para testar consolidacao multi-filial
        Filial filial2A = new Filial();
        filial2A.setEmpresa(empresaRepository.findById(tA.empresaId).orElseThrow());
        filial2A.setNome("Filial 2 EmpA");
        filial2A.setCodigo("F2-EMPA-" + SEQ.incrementAndGet());
        filial2A.setStatus(1);
        filial2A = filialRepository.save(filial2A);

        Cliente cliA1 = saveCliente(tA, "Cliente Filial 1");
        FormaPagamento pixA = saveForma(tA, "PIX", 1);
        ContaReceber crFilial1 = saveContaReceber(tA, null, cliA1.getIdCliente(), pixA.getIdFormaPagamento(),
                bd("1000.00"), BigDecimal.ZERO, FinanceiroStatus.ABERTO, hoje.plusDays(5), null, null);

        TenantContext tAFilial2 = new TenantContext(tA.empresaId, filial2A.getIdFilial(), tA.dono);
        Cliente cliA2 = saveCliente(tAFilial2, "Cliente Filial 2");
        FormaPagamento pixA2 = saveForma(tAFilial2, "PIX F2", 1);
        saveContaReceber(tAFilial2, null, cliA2.getIdCliente(), pixA2.getIdFormaPagamento(),
                bd("2500.00"), BigDecimal.ZERO, FinanceiroStatus.ABERTO, hoje.plusDays(5), null, null);

        // 1. Filial 1 isolada ve R$ 1.000,00; Consolidado Empresa A ve R$ 3.500,00
        JsonNode painelFilial1 = consultarPainel(tokenA, hoje, hoje.plusDays(15), null, false);
        assertThat(bd(painelFilial1.get("entradasPrevistasPendentes").asText())).isEqualByComparingTo("1000.00");

        JsonNode painelConsolidadoA = consultarPainel(tokenA, hoje, hoje.plusDays(15), null, true);
        assertThat(bd(painelConsolidadoA.get("entradasPrevistasPendentes").asText())).isEqualByComparingTo("3500.00");

        // 2. Empresa B NAO consegue ver nem baixar titulo da Empresa A (404 Not Found)
        JsonNode painelB = consultarPainel(tokenB, hoje, hoje.plusDays(15), null, true);
        assertThat(bd(painelB.get("entradasPrevistasPendentes").asText())).isEqualByComparingTo("0.00");

        mvc.perform(post("/api/financeiro/fluxo-caixa/contas-receber/" + crFilial1.getIdContaReceber() + "/baixar")
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new BaixaTituloFinanceiroRequestDTO(
                                bd("100.00"), null, hoje, null, null, null, false, "Tentativa cross-tenant", null))))
                .andExpect(status().isNotFound());

        // 3. RBAC: VENDEDOR recebe 403 ao consultar fluxo de caixa; AUDITOR consegue ler (200), mas recebe 403 ao tentar baixar titulo!
        Usuario vendedor = ensureUser("vend_l3_" + SEQ.incrementAndGet(), "Vendedor L3", "VENDEDOR", tA.empresaId, tA.filialId);
        Usuario auditor = ensureUser("aud_l3_" + SEQ.incrementAndGet(), "Auditor L3", "AUDITOR", tA.empresaId, tA.filialId);
        String tokenVendedor = loginToken(vendedor.getLogin());
        String tokenAuditor = loginToken(auditor.getLogin());

        mvc.perform(get("/api/financeiro/fluxo-caixa")
                        .header("Authorization", bearer(tokenVendedor)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/financeiro/fluxo-caixa")
                        .header("Authorization", bearer(tokenAuditor)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/financeiro/fluxo-caixa/contas-receber/" + crFilial1.getIdContaReceber() + "/baixar")
                        .header("Authorization", bearer(tokenAuditor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new BaixaTituloFinanceiroRequestDTO(
                                bd("100.00"), null, hoje, null, null, null, false, "Tentativa mutacao por auditor", null))))
                .andExpect(status().isForbidden());

        // 4. Isolamento multi-tenant em gerarParcelas: Empresa B tentando referenciar clienteId da Empresa A recebe 404
        GerarParcelasRequestDTO parcCrossTenant = new GerarParcelasRequestDTO(
                "RECEBER",
                "Tentativa Cross Tenant",
                bd("500.00"),
                2,
                30,
                hoje.plusDays(10),
                hoje,
                null,
                cliA1.getIdCliente(),
                null,
                null,
                null,
                null,
                null,
                null,
                ModalidadeRecebivel.BOLETO,
                BigDecimal.ZERO,
                0,
                false,
                null);
        mvc.perform(post("/api/financeiro/fluxo-caixa/parcelas/gerar")
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(parcCrossTenant)))
                .andExpect(status().isNotFound());

        // 5. CORS preflight OPTIONS permitindo explicitamente Idempotency-Key para chamadas cross-origin (Web/Electron/Capacitor)
        mvc.perform(options("/api/financeiro/fluxo-caixa/transferencias")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type,Idempotency-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("Idempotency-Key")));
    }

    private JsonNode consultarPainel(String token, LocalDate inicio, LocalDate fim, Long contaId, boolean consolidar)
            throws Exception {
        var req = get("/api/financeiro/fluxo-caixa")
                .header("Authorization", bearer(token))
                .param("dataInicio", inicio.toString())
                .param("dataFim", fim.toString())
                .param("consolidarFiliais", String.valueOf(consolidar));
        if (contaId != null) {
            req = req.param("contaFinanceiraId", String.valueOf(contaId));
        }
        String body = mvc.perform(req)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private Long findContaFinanceiraIdByCodigo(JsonNode contas, String codigo) {
        for (JsonNode c : contas) {
            if (codigo.equals(c.get("codigo").asText())) {
                return c.get("idContaFinanceira").asLong();
            }
        }
        throw new AssertionError("Conta financeira nao encontrada: " + codigo);
    }

    private TenantContext createTenant(String tag) {
        int id = SEQ.incrementAndGet();
        Empresa emp = new Empresa();
        emp.setRazaoSocial("Empresa Lote3 " + tag + " " + id);
        emp.setNomeFantasia("Apex L3 " + tag + " " + id);
        emp.setCnpj(String.format("97%012d", id));
        emp.setStatus(1);
        emp = empresaRepository.save(emp);

        Filial fil = new Filial();
        fil.setEmpresa(emp);
        fil.setNome("Filial " + tag + " " + id);
        fil.setCodigo("F3-" + tag + "-" + id);
        fil.setStatus(1);
        fil = filialRepository.save(fil);

        String login = ("dono_l3_" + tag + "_" + id).toLowerCase();
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
        end.setLogradouro("Rua Caixa");
        end.setUf("SP");
        end.setNumero("300");
        end.setEmpresaId(t.empresaId);
        end.setFilialId(t.filialId);

        Fornecedor f = new Fornecedor();
        f.setStatus(1);
        f.setRazaoSocial(razao);
        f.setNomeFantasia(razao.length() > 45 ? razao.substring(0, 45) : razao);
        f.setCnpj(String.format("86%012d", id));
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
        p.setCodigoBarras("787" + String.format("%010d", id));
        p.setCodigoInterno("CF-INT-" + id);
        p.setStatus(1);
        p.setEmpresaId(t.empresaId);
        p.setFilialId(t.filialId);
        p.setCategoria(cat);
        p.setFornecedor(forn);
        return produtoRepository.save(p);
    }

    private Despesa saveDespesa(TenantContext t, TipoDespesa tipo, String desc, BigDecimal valor,
            LocalDate vencimento, LocalDate pagamento, int status, Long planoContaId) {
        Despesa d = new Despesa();
        d.setDescricao(desc);
        d.setValor(valor);
        d.setDataCompetencia(vencimento);
        d.setDataEmissao(vencimento);
        d.setDataVencimento(vencimento);
        d.setDataPagamento(pagamento);
        d.setStatus(status);
        d.setEmpresaId(t.empresaId);
        d.setFilialId(t.filialId);
        d.setTipoDespesa(tipo);
        d.setPlanoContaId(planoContaId);
        return despesaRepository.save(d);
    }

    private ContaPagar saveContaPagar(TenantContext t, Long fornecedorId, Long categoriaId, String desc,
            BigDecimal valor, LocalDate vencimento, FinanceiroStatus status, LocalDateTime pagoEm, Long planoContaId) {
        ContaPagar cp = new ContaPagar();
        cp.setFornecedorId(fornecedorId);
        cp.setCategoriaId(categoriaId);
        cp.setEmpresaId(t.empresaId);
        cp.setFilialId(t.filialId);
        cp.setDescricao(desc);
        cp.setValor(valor);
        cp.setValorPago(BigDecimal.ZERO);
        cp.setSaldo(valor);
        cp.setVencimento(vencimento);
        cp.setDataCompetencia(vencimento);
        cp.setDataEmissao(vencimento);
        cp.setStatus(status);
        cp.setCriadoEm(vencimento.atTime(9, 0));
        cp.setPagoEm(pagoEm);
        cp.setPlanoContaId(planoContaId);
        return contaPagarRepository.save(cp);
    }

    private ContaReceber saveContaReceber(TenantContext t, Long vendaId, Long clienteId, Long formaId,
            BigDecimal valor, BigDecimal valorPago, FinanceiroStatus status, LocalDate vencimento,
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
        cr.setVencimento(vencimento);
        cr.setDataCompetencia(vencimento);
        cr.setDataEmissao(vencimento);
        cr.setStatus(status);
        cr.setCriadoEm(vencimento.atTime(10, 5));
        cr.setPagoEm(pagoEm);
        cr.setPlanoContaId(planoContaId);
        return contaReceberRepository.save(cr);
    }

    private Venda saveVenda(TenantContext t, Cliente cli, Produto prod, int qtd, BigDecimal precoUnit,
            BigDecimal custoUnit, LocalDate competencia, VendaStatus statusOp, Long planoContaId) {
        BigDecimal bruto = precoUnit.multiply(BigDecimal.valueOf(qtd));
        Venda v = new Venda();
        v.setUsuario(t.dono);
        v.setCliente(cli);
        v.setStatus(1);
        v.setStatusOperacional(statusOp);
        v.setNumeroVenda("VCF-" + t.filialId + "-" + SEQ.incrementAndGet());
        v.setValorTotal(bruto);
        v.setDesconto(BigDecimal.ZERO);
        v.setEmpresaId(t.empresaId);
        v.setFilialId(t.filialId);
        v.setDataVenda(competencia.atTime(10, 0));
        v.setDataCompetencia(competencia);
        v.setDataEmissao(competencia);
        v.setFinalizadoEm(competencia.atTime(10, 5));
        v.setEstoqueMovimentado(true);
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
        pv.setPrecoTotal(bruto);
        v.getItens().add(pv);
        return vendaRepository.save(v);
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
