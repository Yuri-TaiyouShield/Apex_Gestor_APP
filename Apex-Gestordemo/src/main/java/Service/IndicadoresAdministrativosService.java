package Service;

import DTO.AdmCalcRequestDTO;
import DTO.AplicarPrecoProdutoRequestDTO;
import DTO.BalancoPatrimonialGerencialRequestDTO;
import DTO.BalancoPatrimonialGerencialResponseDTO;
import DTO.CapitalGiroPainelDTO;
import DTO.CategoriaResumoDTO;
import DTO.DiagnosticoAdministrativoItemDTO;
import DTO.DiagnosticoLiquidezDTO;
import DTO.DreContaDetalheDTO;
import DTO.DreFilialColunaDTO;
import DTO.DreGerencialResponseDTO;
import DTO.DreIndicadoresResumoDTO;
import DTO.DreLinhaDTO;
import DTO.FluxoCaixaPainelResponseDTO;
import DTO.FornecedorResumoDTO;
import DTO.IndicadorDetalhadoDTO;
import DTO.InvestimentosRetornoPainelDTO;
import DTO.LiquidezEndividamentoPainelDTO;
import DTO.MargemContribuicaoItemDTO;
import DTO.PainelInteligenciaAdministrativaResponseDTO;
import DTO.ParametroInteligenciaAdministrativaDTO;
import DTO.PontoEquilibrioFilialCategoriaDTO;
import DTO.PontoEquilibrioPainelDTO;
import DTO.PrazosCiclosEvolucaoPontoDTO;
import DTO.PrazosCiclosPainelDTO;
import DTO.ProdutoResponseDTO;
import DTO.RentabilidadePainelDTO;
import DTO.SimulacaoInvestimentoRequestDTO;
import DTO.SimulacaoPrecificacaoRequestDTO;
import DTO.SimulacaoPrecificacaoResponseDTO;
import Model.BalancoPatrimonialGerencial;
import Model.Categoria;
import Model.ContaFinanceira;
import Model.ContaPagar;
import Model.ContaReceber;
import Model.Despesa;
import Model.EstoqueMovimentacao;
import Model.Filial;
import Model.FinanceiroStatus;
import Model.GrupoDreGerencial;
import Model.ModalidadeRecebivel;
import Model.NotaFiscalEntrada;
import Model.ParametroInteligenciaAdministrativa;
import Model.Produto;
import Model.ProdutoVenda;
import Model.StatusConfiabilidadeIndicador;
import Model.TipoDevolucao;
import Model.TipoMovimentacaoEstoque;
import Model.Venda;
import Model.VendaDevolucao;
import Model.VendaDevolucaoItem;
import Model.VendaStatus;
import Repository.BalancoPatrimonialGerencialRepository;
import Repository.ContaFinanceiraRepository;
import Repository.ContaPagarRepository;
import Repository.ContaReceberRepository;
import Repository.DespesaRepository;
import Repository.EstoqueMovimentacaoRepository;
import Repository.FilialRepository;
import Repository.NotaFiscalEntradaRepository;
import Repository.ParametroInteligenciaAdministrativaRepository;
import Repository.ProdutoRepository;
import Repository.VendaDevolucaoRepository;
import Repository.VendaRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.hibernate.Hibernate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IndicadoresAdministrativosService {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal DOIS = new BigDecimal("2");
    private static final BigDecimal TRINTA = new BigDecimal("30");
    private static final String TAG_TAXA_DEDUZIDA_ORIGEM = "[TAXA-DEDUZIDA-ORIGEM:CR-";

    private static final Set<VendaStatus> STATUS_VENDA_FINALIZADA = EnumSet.of(
            VendaStatus.PAGA,
            VendaStatus.FINALIZADA,
            VendaStatus.DEVOLVIDA,
            VendaStatus.TROCA_PARCIAL,
            VendaStatus.TROCA_TOTAL);

    private static final Set<FinanceiroStatus> STATUS_ABERTO = EnumSet.of(
            FinanceiroStatus.ABERTO,
            FinanceiroStatus.PARCIAL);

    private final DreGerencialService dreGerencialService;
    private final FluxoCaixaService fluxoCaixaService;
    private final ConsolidacaoCompetenciaService consolidacaoService;
    private final AdmCalcEngine admCalcEngine;
    private final BalancoPatrimonialGerencialRepository balancoRepository;
    private final ParametroInteligenciaAdministrativaRepository parametroRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueMovimentacaoRepository estoqueMovimentacaoRepository;
    private final VendaRepository vendaRepository;
    private final VendaDevolucaoRepository vendaDevolucaoRepository;
    private final NotaFiscalEntradaRepository notaFiscalEntradaRepository;
    private final ContaReceberRepository contaReceberRepository;
    private final ContaPagarRepository contaPagarRepository;
    private final ContaFinanceiraRepository contaFinanceiraRepository;
    private final DespesaRepository despesaRepository;
    private final FilialRepository filialRepository;
    private final FinancialAuditTrailService financialAuditTrailService;
    private final AuditTrailService auditTrailService;
    private final PrivacyGuardService privacyGuardService;

    public IndicadoresAdministrativosService(
            DreGerencialService dreGerencialService,
            FluxoCaixaService fluxoCaixaService,
            ConsolidacaoCompetenciaService consolidacaoService,
            AdmCalcEngine admCalcEngine,
            BalancoPatrimonialGerencialRepository balancoRepository,
            ParametroInteligenciaAdministrativaRepository parametroRepository,
            ProdutoRepository produtoRepository,
            EstoqueMovimentacaoRepository estoqueMovimentacaoRepository,
            VendaRepository vendaRepository,
            VendaDevolucaoRepository vendaDevolucaoRepository,
            NotaFiscalEntradaRepository notaFiscalEntradaRepository,
            ContaReceberRepository contaReceberRepository,
            ContaPagarRepository contaPagarRepository,
            ContaFinanceiraRepository contaFinanceiraRepository,
            DespesaRepository despesaRepository,
            FilialRepository filialRepository,
            FinancialAuditTrailService financialAuditTrailService,
            AuditTrailService auditTrailService,
            PrivacyGuardService privacyGuardService) {
        this.dreGerencialService = dreGerencialService;
        this.fluxoCaixaService = fluxoCaixaService;
        this.consolidacaoService = consolidacaoService;
        this.admCalcEngine = admCalcEngine;
        this.balancoRepository = balancoRepository;
        this.parametroRepository = parametroRepository;
        this.produtoRepository = produtoRepository;
        this.estoqueMovimentacaoRepository = estoqueMovimentacaoRepository;
        this.vendaRepository = vendaRepository;
        this.vendaDevolucaoRepository = vendaDevolucaoRepository;
        this.notaFiscalEntradaRepository = notaFiscalEntradaRepository;
        this.contaReceberRepository = contaReceberRepository;
        this.contaPagarRepository = contaPagarRepository;
        this.contaFinanceiraRepository = contaFinanceiraRepository;
        this.despesaRepository = despesaRepository;
        this.filialRepository = filialRepository;
        this.financialAuditTrailService = financialAuditTrailService;
        this.auditTrailService = auditTrailService;
        this.privacyGuardService = privacyGuardService;
    }

    // =========================================================================
    // 1. Parametros e Benchmarks Configuraveis por Empresa/Filial
    // =========================================================================

    @Transactional(readOnly = true)
    public ParametroInteligenciaAdministrativaDTO consultarParametros(Long empresaId, Long filialId) {
        requireTenant(empresaId, filialId);
        ParametroInteligenciaAdministrativa param = obterOuCriarParametrosEmMemoria(empresaId, filialId);
        return toParametroDto(param);
    }

    @Transactional
    public ParametroInteligenciaAdministrativaDTO salvarParametros(
            ParametroInteligenciaAdministrativaDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload de parametros nao pode ser nulo.");
        }

        ParametroInteligenciaAdministrativa param = parametroRepository
                .findByEmpresaIdAndFilialId(empresaId, filialId)
                .orElseGet(() -> {
                    ParametroInteligenciaAdministrativa novo = new ParametroInteligenciaAdministrativa();
                    novo.setEmpresaId(empresaId);
                    novo.setFilialId(filialId);
                    return novo;
                });

        if (body.segmentoNegocio() != null && !body.segmentoNegocio().isBlank()) {
            param.setSegmentoNegocio(body.segmentoNegocio().trim().toUpperCase(Locale.ROOT));
        }
        if (body.retornoMinimoMensalPercentual() != null) {
            param.setRetornoMinimoMensalPercentual(scale4(nonNegative(body.retornoMinimoMensalPercentual(), "retornoMinimoMensalPercentual")));
        }
        if (body.capitalInvestidoReferencia() != null) {
            param.setCapitalInvestidoReferencia(scale2(nonNegative(body.capitalInvestidoReferencia(), "capitalInvestidoReferencia")));
        }
        if (body.taxaDescontoAnualPercentual() != null) {
            param.setTaxaDescontoAnualPercentual(scale4(nonNegative(body.taxaDescontoAnualPercentual(), "taxaDescontoAnualPercentual")));
        }
        if (body.margemContribuicaoMinimaPercentual() != null) {
            param.setMargemContribuicaoMinimaPercentual(scale4(nonNegative(body.margemContribuicaoMinimaPercentual(), "margemContribuicaoMinimaPercentual")));
        }
        if (body.margemLiquidaMinimaPercentual() != null) {
            param.setMargemLiquidaMinimaPercentual(scale4(nonNegative(body.margemLiquidaMinimaPercentual(), "margemLiquidaMinimaPercentual")));
        }
        if (body.markupMargemDesejadaPadrao() != null) {
            param.setMarkupMargemDesejadaPadrao(scale4(nonNegative(body.markupMargemDesejadaPadrao(), "markupMargemDesejadaPadrao")));
        }
        if (body.aliquotaTributosVendaPadrao() != null) {
            param.setAliquotaTributosVendaPadrao(scale4(nonNegative(body.aliquotaTributosVendaPadrao(), "aliquotaTributosVendaPadrao")));
        }
        if (body.taxaCartaoMediaPadrao() != null) {
            param.setTaxaCartaoMediaPadrao(scale4(nonNegative(body.taxaCartaoMediaPadrao(), "taxaCartaoMediaPadrao")));
        }
        if (body.comissaoVendaPadrao() != null) {
            param.setComissaoVendaPadrao(scale4(nonNegative(body.comissaoVendaPadrao(), "comissaoVendaPadrao")));
        }
        if (body.freteVariavelVendaPadrao() != null) {
            param.setFreteVariavelVendaPadrao(scale4(nonNegative(body.freteVariavelVendaPadrao(), "freteVariavelVendaPadrao")));
        }
        if (body.cicloFinanceiroMaximoDias() != null) {
            param.setCicloFinanceiroMaximoDias(scale2(nonNegative(body.cicloFinanceiroMaximoDias(), "cicloFinanceiroMaximoDias")));
        }
        if (body.pmeMaximoDias() != null) {
            param.setPmeMaximoDias(scale2(nonNegative(body.pmeMaximoDias(), "pmeMaximoDias")));
        }
        if (body.liquidezCorrenteMinima() != null) {
            param.setLiquidezCorrenteMinima(scale4(nonNegative(body.liquidezCorrenteMinima(), "liquidezCorrenteMinima")));
        }
        if (body.liquidezSecaMinima() != null) {
            param.setLiquidezSecaMinima(scale4(nonNegative(body.liquidezSecaMinima(), "liquidezSecaMinima")));
        }
        if (body.liquidezImediataMinima() != null) {
            param.setLiquidezImediataMinima(scale4(nonNegative(body.liquidezImediataMinima(), "liquidezImediataMinima")));
        }
        if (body.endividamentoGeralMaximoPercentual() != null) {
            param.setEndividamentoGeralMaximoPercentual(scale4(nonNegative(body.endividamentoGeralMaximoPercentual(), "endividamentoGeralMaximoPercentual")));
        }

        param.setAtualizadoPor(privacyGuardService.currentUserLogin(request));
        param.setAtualizadoEm(LocalDateTime.now());
        ParametroInteligenciaAdministrativa salvo = parametroRepository.save(param);

        registrarAuditoria(
                "PARAMETROS_INDICADORES_ATUALIZADOS",
                "ParametroInteligenciaAdministrativa",
                salvo.getIdParametroInteligencia(),
                "segmento=" + salvo.getSegmentoNegocio()
                        + ";retornoMin=" + salvo.getRetornoMinimoMensalPercentual()
                        + ";mcMin=" + salvo.getMargemContribuicaoMinimaPercentual(),
                "Atualizacao de benchmarks e parametros da inteligencia administrativa.",
                empresaId,
                filialId,
                request);

        return toParametroDto(salvo);
    }

    // =========================================================================
    // 2. Balanco Patrimonial Gerencial (Lote 4.6)
    // =========================================================================

    @Transactional(readOnly = true)
    public BalancoPatrimonialGerencialResponseDTO consultarBalancoPatrimonial(
            String competencia,
            Long empresaId,
            Long filialId) {
        requireTenant(empresaId, filialId);
        YearMonth ym = resolverCompetencia(competencia);
        SaldosOperacionaisInstantaneos operacionais = apurarSaldosOperacionaisInstantaneos(empresaId, List.of(filialId));
        Optional<BalancoPatrimonialGerencial> existente = balancoRepository
                .findByEmpresaIdAndFilialIdAndCompetenciaAnoMes(empresaId, filialId, ym.toString());
        return existente
                .map(b -> toBalancoResponse(b, operacionais))
                .orElseGet(() -> construirBalancoNaoConfirmadoPadrao(ym, empresaId, filialId, operacionais));
    }

    @Transactional
    public BalancoPatrimonialGerencialResponseDTO salvarBalancoPatrimonial(
            BalancoPatrimonialGerencialRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        if (body == null || body.competenciaAnoMes() == null || body.competenciaAnoMes().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A competencia (YYYY-MM) do Balanco Patrimonial Gerencial e obrigatoria.");
        }
        YearMonth ym = consolidacaoService.parseCompetencia(body.competenciaAnoMes());
        SaldosOperacionaisInstantaneos operacionais = apurarSaldosOperacionaisInstantaneos(empresaId, List.of(filialId));
        boolean usarOperacionais = body.usarSaldosOperacionaisErp() == null || Boolean.TRUE.equals(body.usarSaldosOperacionaisErp());

        BalancoPatrimonialGerencial balanco = balancoRepository
                .findByEmpresaIdAndFilialIdAndCompetenciaAnoMes(empresaId, filialId, ym.toString())
                .orElseGet(() -> {
                    BalancoPatrimonialGerencial novo = new BalancoPatrimonialGerencial();
                    novo.setEmpresaId(empresaId);
                    novo.setFilialId(filialId);
                    novo.setCompetenciaAnoMes(ym.toString());
                    return novo;
                });

        balanco.setDataReferencia(body.dataReferencia() != null ? body.dataReferencia() : ym.atEndOfMonth());
        balanco.setDisponibilidadesCaixaBancos(scale2(usarOperacionais && body.disponibilidadesCaixaBancos() == null
                ? operacionais.disponibilidades()
                : nvl(body.disponibilidadesCaixaBancos())));
        balanco.setContasReceberOperacionais(scale2(usarOperacionais && body.contasReceberOperacionais() == null
                ? operacionais.contasReceberAbertas()
                : nvl(body.contasReceberOperacionais())));
        balanco.setEstoquesOperacionais(scale2(usarOperacionais && body.estoquesOperacionais() == null
                ? operacionais.estoqueAtualCusto()
                : nvl(body.estoquesOperacionais())));
        balanco.setAdiantamentosOperacionais(scale2(nvl(body.adiantamentosOperacionais())));
        balanco.setDespesasAntecipadas(scale2(nvl(body.despesasAntecipadas())));
        balanco.setOutrosAtivosCirculantes(scale2(nvl(body.outrosAtivosCirculantes())));
        balanco.setRealizavelLongoPrazo(scale2(nvl(body.realizavelLongoPrazo())));
        balanco.setInvestimentosLongoPrazo(scale2(nvl(body.investimentosLongoPrazo())));
        balanco.setImobilizadoBruto(scale2(nvl(body.imobilizadoBruto())));
        balanco.setDepreciacaoAcumulada(scale2(nvl(body.depreciacaoAcumulada())));
        if (balanco.getDepreciacaoAcumulada().compareTo(balanco.getImobilizadoBruto()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A depreciacao acumulada nao pode superar o imobilizado bruto.");
        }
        balanco.setIntangivel(scale2(nvl(body.intangivel())));

        balanco.setFornecedoresOperacionaisCp(scale2(usarOperacionais && body.fornecedoresOperacionaisCp() == null
                ? operacionais.contasPagarAbertas()
                : nvl(body.fornecedoresOperacionaisCp())));
        balanco.setObrigacoesTributariasTrabalhistasCp(scale2(nvl(body.obrigacoesTributariasTrabalhistasCp())));
        balanco.setOutrasObrigacoesOperacionaisCp(scale2(nvl(body.outrasObrigacoesOperacionaisCp())));
        balanco.setEmprestimosFinanciamentosCp(scale2(nvl(body.emprestimosFinanciamentosCp())));
        balanco.setEmprestimosFinanciamentosLp(scale2(nvl(body.emprestimosFinanciamentosLp())));
        balanco.setOutrasObrigacoesLp(scale2(nvl(body.outrasObrigacoesLp())));

        balanco.setCapitalSocial(scale2(nvl(body.capitalSocial())));
        balanco.setReservasCapitalLucros(scale2(nvl(body.reservasCapitalLucros())));
        balanco.setLucrosPrejuizosAcumulados(scale2(nvl(body.lucrosPrejuizosAcumulados())));
        balanco.setDepreciacaoAmortizacaoPeriodo(scale2(nvl(body.depreciacaoAmortizacaoPeriodo())));
        balanco.setAmortizacaoPrincipalDividaPeriodo(scale2(nvl(body.amortizacaoPrincipalDividaPeriodo())));
        balanco.setSincronizadoOperacional(usarOperacionais);
        balanco.setConfirmadoGestor(Boolean.TRUE.equals(body.confirmadoGestor()));
        balanco.setObservacoes(body.observacoes());
        balanco.setAtualizadoPor(privacyGuardService.currentUserLogin(request));
        balanco.setAtualizadoEm(LocalDateTime.now());

        BalancoPatrimonialGerencial salvo = balancoRepository.save(balanco);
        BalancoPatrimonialGerencialResponseDTO response = toBalancoResponse(salvo, operacionais);

        registrarAuditoria(
                "BALANCO_PATRIMONIAL_GERENCIAL_SALVO",
                "BalancoPatrimonialGerencial",
                salvo.getIdBalancoPatrimonial(),
                "comp=" + ym + ";ativoTotal=" + response.ativoTotal()
                        + ";passivoExigivel=" + response.passivoExigivelTotal()
                        + ";pl=" + response.patrimonioLiquidoTotal()
                        + ";confirmado=" + response.confirmadoGestor(),
                "Registro/atualizacao do Balanco Patrimonial Gerencial para calculo de indicadores patrimoniais.",
                empresaId,
                filialId,
                request);

        return response;
    }

    // =========================================================================
    // 3. Painel Consolidado de Inteligencia Administrativa (Lotes 4.1 a 4.8)
    // =========================================================================

    @Transactional(readOnly = true)
    public PainelInteligenciaAdministrativaResponseDTO consultarPainelConsolidado(
            String competencia,
            LocalDate dataInicio,
            LocalDate dataFim,
            Long filialFiltroId,
            Boolean consolidarFiliais,
            Long empresaId,
            Long currentFilialId) {
        requireTenant(empresaId, currentFilialId);

        YearMonth ym = resolverCompetencia(competencia);
        LocalDate inicio = dataInicio != null ? dataInicio : ym.atDay(1);
        LocalDate fim = dataFim != null ? dataFim : ym.atEndOfMonth();
        if (fim.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data final nao pode ser anterior a data inicial.");
        }
        long dias = ChronoUnit.DAYS.between(inicio, fim) + 1;
        if (dias > 731) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O intervalo maximo de analise e de 731 dias.");
        }

        boolean consolidar = Boolean.TRUE.equals(consolidarFiliais);
        Long filialAlvo = consolidar ? null : (filialFiltroId != null ? filialFiltroId : currentFilialId);
        List<Long> filiaisEscopo = resolverFiliaisEscopo(empresaId, currentFilialId, filialAlvo, consolidar);
        String filialNome = consolidar
                ? "Consolidado da Empresa (" + filiaisEscopo.size() + " filial(is))"
                : obterNomeFilial(empresaId, filialAlvo != null ? filialAlvo : currentFilialId);

        ParametroInteligenciaAdministrativa paramEntity = obterOuCriarParametrosEmMemoria(
                empresaId, filialAlvo != null ? filialAlvo : currentFilialId);
        ParametroInteligenciaAdministrativaDTO parametros = toParametroDto(paramEntity);

        // Carrega os repositorios operacionais UMA unica vez por filial para eliminar varreduras repetidas e N+1
        DadosOperacionaisEscopo dadosEscopo = carregarDadosOperacionaisEscopo(empresaId, filiaisEscopo);

        // 1. Consulta DRE Gerencial do periodo
        DreGerencialService.FiltroDreRequest filtroDre = new DreGerencialService.FiltroDreRequest(
                dataInicio != null || dataFim != null ? "PERSONALIZADO" : "MENSAL",
                ym.toString(),
                ym.getYear(),
                null,
                null,
                inicio,
                fim,
                true,
                null,
                null,
                null,
                null,
                filialAlvo,
                consolidar);
        DreGerencialResponseDTO dre = dreGerencialService.apurarDre(filtroDre, empresaId, currentFilialId);

        // 2. Consulta Fluxo de Caixa do periodo
        FluxoCaixaPainelResponseDTO fluxo = fluxoCaixaService.consultarPainelFluxoCaixa(
                inicio, fim, null, null, filialAlvo, consolidar, empresaId, currentFilialId);

        // 3. Saldos Operacionais e Balanco Patrimonial Gerencial
        SaldosOperacionaisInstantaneos saldosOp = apurarSaldosOperacionaisInstantaneos(dadosEscopo, filiaisEscopo);
        BalancoPatrimonialGerencialResponseDTO balanco = obterBalancoConsolidadoEscopo(
                ym, empresaId, filiaisEscopo, filialAlvo != null ? filialAlvo : currentFilialId, saldosOp, dadosEscopo);

        // 4. Lote 4.1: Margem de Contribuicao e Rentabilidade
        ResultadoRentabilidadeCalculada rentCalc = calcularRentabilidade(
                empresaId, currentFilialId, filiaisEscopo, inicio, fim, dre, paramEntity, dadosEscopo);
        RentabilidadePainelDTO rentabilidade = rentCalc.painel();

        // 5. Lote 4.2: Pontos de Equilibrio Contabil, Financeiro e Economico
        PontoEquilibrioPainelDTO pontoEquilibrio = calcularPontoEquilibrio(
                inicio, fim, rentCalc, dre, balanco, paramEntity);

        // 6. Lote 4.3: Markup e Precificacao Inteligente (padrao ou primeiro produto ativo)
        SimulacaoPrecificacaoResponseDTO precificacaoPadrao = calcularPrecificacaoPadrao(
                empresaId, filiaisEscopo, paramEntity, dadosEscopo);

        // 7. Lote 4.4: Capital de Giro (CGL, NCG e Saldo de Tesouraria)
        CapitalGiroPainelDTO capitalGiro = calcularCapitalGiro(
                ym, empresaId, filiaisEscopo, saldosOp, balanco);

        // 8. Lote 4.5: Prazos Medios (PMR, PMP, PME) e Ciclos Operacional e Financeiro
        PrazosCiclosPainelDTO prazosCiclos = calcularPrazosCiclos(
                ym, inicio, fim, empresaId, currentFilialId, filiaisEscopo, filialAlvo, consolidar, dre, saldosOp, dadosEscopo);

        // 9. Lote 4.6: Liquidez e Endividamento (com guarda de confiabilidade patrimonial)
        LiquidezEndividamentoPainelDTO liquidezEndividamento = calcularLiquidezEndividamento(
                saldosOp, balanco, paramEntity);

        // 10. Lote 4.7: ROI, ROA, ROE, ROIC e Viabilidade de Investimentos
        InvestimentosRetornoPainelDTO investimentosRetorno = calcularInvestimentosRetorno(
                dre, fluxo, balanco, paramEntity, null);

        // 11. Catalogo de Indicadores com Confiabilidade e Variaveis
        String rotuloPeriodo = dre.rotuloPeriodo();
        LocalDateTime agora = LocalDateTime.now();
        List<IndicadorDetalhadoDTO> catalogo = construirCatalogoIndicadores(
                rotuloPeriodo,
                agora,
                rentabilidade,
                pontoEquilibrio,
                precificacaoPadrao,
                capitalGiro,
                prazosCiclos,
                liquidezEndividamento,
                investimentosRetorno,
                paramEntity,
                balanco);

        // 12. Lote 4.8: Diagnosticos Administrativos Deterministicos e Explicaveis
        List<DiagnosticoAdministrativoItemDTO> diagnosticos = construirDiagnosticosAdministrativos(
                rotuloPeriodo,
                dre,
                rentabilidade,
                pontoEquilibrio,
                capitalGiro,
                prazosCiclos,
                liquidezEndividamento,
                investimentosRetorno,
                paramEntity,
                balanco);

        return new PainelInteligenciaAdministrativaResponseDTO(
                ym.toString(),
                inicio,
                fim,
                empresaId,
                consolidar ? null : (filialAlvo != null ? filialAlvo : currentFilialId),
                filialNome,
                consolidar,
                agora,
                parametros,
                balanco,
                rentabilidade,
                pontoEquilibrio,
                precificacaoPadrao,
                capitalGiro,
                prazosCiclos,
                liquidezEndividamento,
                investimentosRetorno,
                catalogo,
                diagnosticos);
    }

    // =========================================================================
    // 4. Lote 4.1 — Margem de Contribuicao e Rentabilidade
    // =========================================================================

    private record ResultadoRentabilidadeCalculada(
            RentabilidadePainelDTO painel,
            Map<Long, BigDecimal> despesasFixasPorFilial,
            BigDecimal taxasCartaoPendentesNaoNaDre) {
    }

    private ResultadoRentabilidadeCalculada calcularRentabilidade(
            Long empresaId,
            Long currentFilialId,
            List<Long> filiaisEscopo,
            LocalDate inicio,
            LocalDate fim,
            DreGerencialResponseDTO dre,
            ParametroInteligenciaAdministrativa param,
            DadosOperacionaisEscopo dadosEscopo) {
        DreIndicadoresResumoDTO ind = dre.indicadores();
        BigDecimal receitaBrutaTotal = scale2(nvl(ind.receitaBruta()));
        BigDecimal deducoesTributosVendasTotal = scale2(nvl(ind.deducoesTotais()));
        BigDecimal receitaLiquidaTotal = scale2(nvl(ind.receitaLiquida()));
        BigDecimal cmvTotal = scale2(nvl(ind.cmv()));
        BigDecimal despesasVariaveisDre = scale2(nvl(ind.despesasVariaveis()));

        // Verifica taxas de cartao/antecipacao de vendas da competencia que ainda nao foram baixadas
        // (as ja baixadas geraram Despesa automatica [TAXA-DEDUZIDA-ORIGEM:CR-<id>] na conta 9.01 da DRE)
        Set<Long> crComTaxaJaNaDre = identificarCrComTaxaContabilizadaDre(filiaisEscopo, inicio, fim, dadosEscopo);
        Map<Long, BigDecimal> taxasCartaoPorVenda = new HashMap<>();
        Map<Long, BigDecimal> taxasCartaoPendentesPorFilial = new HashMap<>();
        Map<Long, BigDecimal> taxasCartaoTotaisPorFilial = new HashMap<>();
        BigDecimal taxasCartaoPendentesNaoNaDre = BigDecimal.ZERO;
        BigDecimal taxasCartaoTotaisPeriodo = BigDecimal.ZERO;

        for (Long fId : filiaisEscopo) {
            for (ContaReceber cr : dadosEscopo.contasReceberPorFilial().getOrDefault(fId, List.of())) {
                if (cr.getStatus() == FinanceiroStatus.CANCELADO || cr.getStatus() == FinanceiroStatus.DEVOLVIDO) {
                    continue;
                }
                LocalDate comp = cr.getDataCompetencia() != null ? cr.getDataCompetencia()
                        : (cr.getDataEmissao() != null ? cr.getDataEmissao() : cr.getVencimento());
                if (comp == null || comp.isBefore(inicio) || comp.isAfter(fim)) {
                    continue;
                }
                BigDecimal taxaTitulo = nvl(cr.getValorTaxa()).add(nvl(cr.getTaxaAntecipacao()));
                if (taxaTitulo.compareTo(BigDecimal.ZERO) > 0) {
                    taxasCartaoTotaisPeriodo = taxasCartaoTotaisPeriodo.add(taxaTitulo);
                    taxasCartaoTotaisPorFilial.merge(fId, taxaTitulo, BigDecimal::add);
                    if (cr.getVendaId() != null) {
                        taxasCartaoPorVenda.merge(cr.getVendaId(), taxaTitulo, BigDecimal::add);
                    }
                    if (!crComTaxaJaNaDre.contains(cr.getIdContaReceber())) {
                        taxasCartaoPendentesNaoNaDre = taxasCartaoPendentesNaoNaDre.add(taxaTitulo);
                        taxasCartaoPendentesPorFilial.merge(fId, taxaTitulo, BigDecimal::add);
                    }
                }
            }
        }

        // Custos e despesas variaveis totais = CMV + Despesas Variaveis DRE + Taxas de Cartao contratadas ainda nao baixadas na DRE
        // Nota: Devolucoes, descontos e tributos sobre vendas JA foram abatidos para chegar a Receita Liquida (Linha 5),
        // portanto NAO sao deduzidos novamente!
        BigDecimal custosDespesasVariaveisTotais = scale2(
                cmvTotal.add(despesasVariaveisDre).add(taxasCartaoPendentesNaoNaDre));
        BigDecimal margemContribuicaoTotalValor = scale2(receitaLiquidaTotal.subtract(custosDespesasVariaveisTotais));
        BigDecimal margemContribuicaoTotalPercentual = calcPercent(margemContribuicaoTotalValor, receitaLiquidaTotal);
        if (margemContribuicaoTotalPercentual == null) {
            margemContribuicaoTotalPercentual = ZERO;
        }

        // Apuracao por Produto e por Categoria a partir das Vendas Finalizadas e Devolucoes do periodo
        List<Venda> vendasPeriodo = carregarVendasFinalizadasPeriodo(empresaId, filiaisEscopo, inicio, fim);
        Map<String, DevolucaoAcumuladaItem> devolucoesPorVendaProduto = carregarDevolucoesPeriodo(
                empresaId, filiaisEscopo, inicio, fim);

        // Aliquota efetiva de tributos sobre vendas na DRE para rateio sem dupla deducao
        BigDecimal tributosVendasDre = extrairValorLinhaDre(dre, 4);
        BigDecimal baseAposDevolucaoDesconto = receitaBrutaTotal
                .subtract(extrairValorLinhaDre(dre, 2))
                .subtract(extrairValorLinhaDre(dre, 3))
                .max(BigDecimal.ZERO);
        BigDecimal aliquotaTributoEfetivaDre = baseAposDevolucaoDesconto.compareTo(BigDecimal.ZERO) > 0
                ? tributosVendasDre.divide(baseAposDevolucaoDesconto, MC)
                : BigDecimal.ZERO;

        // Despesas variaveis nao vinculadas diretamente a cartao (ex.: comissoes, fretes variaveis, embalagens)
        BigDecimal taxasJaNaDre = taxasCartaoTotaisPeriodo.subtract(taxasCartaoPendentesNaoNaDre).max(BigDecimal.ZERO);
        BigDecimal outrasVariaveisRateio = despesasVariaveisDre.subtract(taxasJaNaDre).max(BigDecimal.ZERO);
        BigDecimal taxaOutrasVariaveisSobreReceitaLiquida = receitaLiquidaTotal.compareTo(BigDecimal.ZERO) > 0
                ? outrasVariaveisRateio.divide(receitaLiquidaTotal, MC)
                : BigDecimal.ZERO;

        Map<Long, AcumuladorRentabilidade> porProdutoMap = new LinkedHashMap<>();
        Map<Long, AcumuladorRentabilidade> porCategoriaMap = new LinkedHashMap<>();

        for (Venda v : vendasPeriodo) {
            if (v.getItens() == null || v.getItens().isEmpty()) {
                continue;
            }
            BigDecimal somaBrutaItensVenda = BigDecimal.ZERO;
            for (ProdutoVenda pv : v.getItens()) {
                BigDecimal brutoItem = pv.getPrecoTotal() != null
                        ? pv.getPrecoTotal()
                        : nvl(pv.getPrecoUnitario()).multiply(BigDecimal.valueOf(pv.getQuantidade()));
                somaBrutaItensVenda = somaBrutaItensVenda.add(brutoItem);
            }
            BigDecimal descontoVenda = nvl(v.getDesconto());
            BigDecimal taxaCartaoVenda = nvl(taxasCartaoPorVenda.get(v.getIdVenda()));

            for (ProdutoVenda pv : v.getItens()) {
                Produto p = pv.getProduto();
                if (p == null) {
                    continue;
                }
                Long prodId = p.getIdProduto();
                String chaveDev = v.getIdVenda() + ":" + prodId;
                DevolucaoAcumuladaItem dev = devolucoesPorVendaProduto.get(chaveDev);
                int qtdDev = dev == null ? 0 : dev.quantidade();
                BigDecimal valorDev = dev == null ? BigDecimal.ZERO : dev.valorBruto();

                int qtdLiquida = Math.max(0, pv.getQuantidade() - qtdDev);
                BigDecimal brutoItem = pv.getPrecoTotal() != null
                        ? pv.getPrecoTotal()
                        : nvl(pv.getPrecoUnitario()).multiply(BigDecimal.valueOf(pv.getQuantidade()));
                BigDecimal pesoItemNaVenda = somaBrutaItensVenda.compareTo(BigDecimal.ZERO) > 0
                        ? brutoItem.divide(somaBrutaItensVenda, MC)
                        : BigDecimal.ZERO;

                BigDecimal descontoItem = descontoVenda.multiply(pesoItemNaVenda, MC);
                BigDecimal baseTributavelItem = brutoItem.subtract(valorDev).subtract(descontoItem).max(BigDecimal.ZERO);
                BigDecimal tributoItem = baseTributavelItem.multiply(aliquotaTributoEfetivaDre, MC);
                BigDecimal deducoesItem = valorDev.add(descontoItem).add(tributoItem);
                BigDecimal receitaLiquidaItem = brutoItem.subtract(deducoesItem).max(BigDecimal.ZERO);

                BigDecimal custoUnit = pv.getCustoUnitario() != null && pv.getCustoUnitario().compareTo(BigDecimal.ZERO) > 0
                        ? pv.getCustoUnitario()
                        : nvl(p.getCusto());
                BigDecimal cmvItem = custoUnit.multiply(BigDecimal.valueOf(qtdLiquida), MC);
                BigDecimal taxaCartaoItem = taxaCartaoVenda.multiply(pesoItemNaVenda, MC);
                BigDecimal comissaoFreteItem = receitaLiquidaItem.multiply(taxaOutrasVariaveisSobreReceitaLiquida, MC);

                Categoria cat = p.getCategoria();
                Long catId = cat != null ? cat.getIdCategoria() : -1L;
                String catNome = cat != null && cat.getDescricao() != null ? cat.getDescricao() : "Sem Categoria";

                AcumuladorRentabilidade accProd = porProdutoMap.computeIfAbsent(prodId, id -> new AcumuladorRentabilidade(
                        "PRODUTO",
                        id,
                        p.getCodigoInterno() != null ? p.getCodigoInterno() : "PROD-" + id,
                        p.getDescricao(),
                        catNome));
                accProd.acumular(
                        BigDecimal.valueOf(qtdLiquida),
                        brutoItem,
                        deducoesItem,
                        receitaLiquidaItem,
                        cmvItem,
                        taxaCartaoItem,
                        comissaoFreteItem,
                        BigDecimal.ZERO);

                AcumuladorRentabilidade accCat = porCategoriaMap.computeIfAbsent(catId, id -> new AcumuladorRentabilidade(
                        "CATEGORIA",
                        id,
                        "CAT-" + (id > 0 ? id : "GERAL"),
                        catNome,
                        catNome));
                accCat.acumular(
                        BigDecimal.valueOf(qtdLiquida),
                        brutoItem,
                        deducoesItem,
                        receitaLiquidaItem,
                        cmvItem,
                        taxaCartaoItem,
                        comissaoFreteItem,
                        BigDecimal.ZERO);
            }
        }

        BigDecimal metaMcPct = nvl(param.getMargemContribuicaoMinimaPercentual());
        List<MargemContribuicaoItemDTO> listaProdutos = porProdutoMap.values().stream()
                .map(a -> a.toDto(receitaLiquidaTotal, margemContribuicaoTotalValor, metaMcPct))
                .sorted(Comparator.comparing(MargemContribuicaoItemDTO::receitaLiquida).reversed())
                .toList();

        List<MargemContribuicaoItemDTO> listaCategorias = porCategoriaMap.values().stream()
                .map(a -> a.toDto(receitaLiquidaTotal, margemContribuicaoTotalValor, metaMcPct))
                .sorted(Comparator.comparing(MargemContribuicaoItemDTO::margemContribuicaoValor).reversed())
                .toList();

        List<MargemContribuicaoItemDTO> alertasAltoVolume = listaProdutos.stream()
                .filter(i -> "VOLUME_ALTO_BAIXA_MARGEM".equals(i.classificacaoRentabilidade())
                        || "MARGEM_CRITICA_OU_NEGATIVA".equals(i.classificacaoRentabilidade()))
                .toList();

        Map<Long, BigDecimal> despesasFixasPorFilial = new LinkedHashMap<>();
        List<MargemContribuicaoItemDTO> listaFiliais = construirRentabilidadePorFilial(
                empresaId,
                currentFilialId,
                filiaisEscopo,
                inicio,
                fim,
                dre,
                receitaLiquidaTotal,
                margemContribuicaoTotalValor,
                metaMcPct,
                taxasCartaoPendentesPorFilial,
                taxasCartaoTotaisPorFilial,
                despesasFixasPorFilial);

        RentabilidadePainelDTO painel = new RentabilidadePainelDTO(
                receitaBrutaTotal,
                deducoesTributosVendasTotal,
                receitaLiquidaTotal,
                cmvTotal,
                despesasVariaveisDre,
                scale2(taxasCartaoTotaisPeriodo),
                custosDespesasVariaveisTotais,
                margemContribuicaoTotalValor,
                margemContribuicaoTotalPercentual,
                scale2(nvl(ind.despesasFixas())),
                scale2(nvl(ind.resultadoOperacional())),
                scale2(nvl(ind.resultadoLiquidoGerencial())),
                scale2(nvl(ind.margemLiquidaPercentual())),
                listaProdutos,
                listaCategorias,
                listaFiliais,
                alertasAltoVolume);
        return new ResultadoRentabilidadeCalculada(painel, despesasFixasPorFilial, scale2(taxasCartaoPendentesNaoNaDre));
    }

    private List<MargemContribuicaoItemDTO> construirRentabilidadePorFilial(
            Long empresaId,
            Long currentFilialId,
            List<Long> filiaisEscopo,
            LocalDate inicio,
            LocalDate fim,
            DreGerencialResponseDTO dreEscopo,
            BigDecimal receitaLiquidaEmpresa,
            BigDecimal mcTotalEmpresa,
            BigDecimal metaMcPct,
            Map<Long, BigDecimal> taxasCartaoPendentesPorFilial,
            Map<Long, BigDecimal> taxasCartaoTotaisPorFilial,
            Map<Long, BigDecimal> despesasFixasPorFilial) {
        List<MargemContribuicaoItemDTO> resultado = new ArrayList<>();
        for (Long fId : filiaisEscopo) {
            DreGerencialResponseDTO dreFilial;
            if (filiaisEscopo.size() == 1
                    && dreEscopo != null
                    && !dreEscopo.consolidadoEmpresa()
                    && Objects.equals(dreEscopo.filialId(), fId)) {
                dreFilial = dreEscopo;
            } else {
                DreGerencialService.FiltroDreRequest filtroFilial = new DreGerencialService.FiltroDreRequest(
                        "PERSONALIZADO", null, null, null, null, inicio, fim, false, null, null, null, null, fId, false);
                dreFilial = dreGerencialService.apurarDre(filtroFilial, empresaId, currentFilialId);
            }
            DreIndicadoresResumoDTO ind = dreFilial.indicadores();
            despesasFixasPorFilial.put(fId, scale2(nvl(ind.despesasFixas())));

            BigDecimal recBruta = scale2(nvl(ind.receitaBruta()));
            BigDecimal ded = scale2(nvl(ind.deducoesTotais()));
            BigDecimal recLiq = scale2(nvl(ind.receitaLiquida()));
            BigDecimal cmv = scale2(nvl(ind.cmv()));
            BigDecimal varDre = scale2(nvl(ind.despesasVariaveis()));
            BigDecimal taxasPendentesFilial = scale2(taxasCartaoPendentesPorFilial.getOrDefault(fId, BigDecimal.ZERO));
            BigDecimal taxasTotaisFilial = scale2(taxasCartaoTotaisPorFilial.getOrDefault(fId, BigDecimal.ZERO));
            BigDecimal varTotal = scale2(cmv.add(varDre).add(taxasPendentesFilial));
            BigDecimal outrasVariaveisFilial = scale2(varDre.add(taxasPendentesFilial).subtract(taxasTotaisFilial).max(BigDecimal.ZERO));
            BigDecimal mcVal = scale2(recLiq.subtract(varTotal));
            BigDecimal mcPct = defaultedPercent(calcPercent(mcVal, recLiq));
            BigDecimal partFat = defaultedPercent(calcPercent(recLiq, receitaLiquidaEmpresa));
            BigDecimal partMc = defaultedPercent(calcPercent(mcVal, mcTotalEmpresa));

            String classif = classificarItemRentabilidade(recLiq, mcVal, mcPct, partFat, metaMcPct);
            String rec = gerarRecomendacaoRentabilidade(dreFilial.filialNome(), classif, mcPct, metaMcPct);

            resultado.add(new MargemContribuicaoItemDTO(
                    "FILIAL",
                    fId,
                    "FIL-" + fId,
                    dreFilial.filialNome(),
                    "Filial Operacional",
                    ZERO,
                    recBruta,
                    ded,
                    recLiq,
                    cmv,
                    taxasTotaisFilial,
                    outrasVariaveisFilial,
                    ZERO,
                    varTotal,
                    mcVal,
                    mcPct,
                    ZERO,
                    partFat,
                    partMc,
                    classif,
                    rec));
        }
        return resultado;
    }

    // =========================================================================
    // 5. Lote 4.2 — Pontos de Equilibrio Contabil, Financeiro e Economico
    // =========================================================================

    private PontoEquilibrioPainelDTO calcularPontoEquilibrio(
            LocalDate inicio,
            LocalDate fim,
            ResultadoRentabilidadeCalculada rentCalc,
            DreGerencialResponseDTO dre,
            BalancoPatrimonialGerencialResponseDTO balanco,
            ParametroInteligenciaAdministrativa param) {
        RentabilidadePainelDTO rentabilidade = rentCalc.painel();
        BigDecimal receitaLiquida = rentabilidade.receitaLiquidaTotal();
        BigDecimal mcTotal = rentabilidade.margemContribuicaoTotalValor();
        BigDecimal custosFixosContabeis = rentabilidade.custosDespesasFixasTotais();

        // Depreciacao/amortizacao nao desembolsavel (do Balanco Patrimonial Gerencial ou contas de depreciacao da DRE)
        BigDecimal depreciacaoBalanco = balanco != null ? nvl(balanco.depreciacaoAmortizacaoPeriodo()) : BigDecimal.ZERO;
        BigDecimal depreciacaoDre = extrairDepreciacaoDaDre(dre);
        BigDecimal depreciacaoNaoDesembolsavel = scale2(depreciacaoBalanco.max(depreciacaoDre));

        // Amortizacao de principal de emprestimos/dividas no periodo (impacta caixa, mas nao entra na DRE)
        BigDecimal amortizacaoPrincipal = scale2(balanco != null ? nvl(balanco.amortizacaoPrincipalDividaPeriodo()) : BigDecimal.ZERO);

        BigDecimal desembolsosFixosFinanceiros = scale2(
                custosFixosContabeis.subtract(depreciacaoNaoDesembolsavel).max(BigDecimal.ZERO).add(amortizacaoPrincipal));

        // Remuneracao minima desejada sobre o capital investido (Custo de Oportunidade / Meta de Lucro Economico)
        BigDecimal capitalInvestido = nvl(param.getCapitalInvestidoReferencia()).compareTo(BigDecimal.ZERO) > 0
                ? param.getCapitalInvestidoReferencia()
                : (balanco != null && balanco.confirmadoGestor() && nvl(balanco.patrimonioLiquidoTotal()).compareTo(BigDecimal.ZERO) > 0
                        ? balanco.patrimonioLiquidoTotal()
                        : BigDecimal.ZERO);
        capitalInvestido = scale2(capitalInvestido);

        long diasPeriodo = Math.max(1L, ChronoUnit.DAYS.between(inicio, fim) + 1);
        BigDecimal fatorMeses = BigDecimal.valueOf(diasPeriodo).divide(TRINTA, MC);
        BigDecimal taxaRetornoPeriodoPct = scale4(nvl(param.getRetornoMinimoMensalPercentual()).multiply(fatorMeses, MC));
        BigDecimal remuneracaoMinimaCapital = scale2(
                capitalInvestido.multiply(taxaRetornoPeriodoPct, MC).divide(CEM, MC));

        BigDecimal qtdTotalMix = rentabilidade.porProduto().stream()
                .map(MargemContribuicaoItemDTO::quantidadeVendidaLiquida)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal precoMedioMix = qtdTotalMix.compareTo(BigDecimal.ZERO) > 0
                ? scale2(receitaLiquida.divide(qtdTotalMix, MC))
                : null;
        BigDecimal mcUnitariaMediaMix = qtdTotalMix.compareTo(BigDecimal.ZERO) > 0
                ? scale2(mcTotal.divide(qtdTotalMix, MC))
                : null;

        String metodologia = "PEC = Custos e Despesas Fixas Contabeis / Indice de Margem de Contribuicao (IMC = MC / Receita Liquida). "
                + "PEF = (Custos Fixos Contabeis - Depreciacao/Amortizacao Nao Desembolsavel + Amortizacao do Principal de Dividas) / IMC. "
                + "PEE = (Custos Fixos Contabeis + Remuneracao Minima do Capital Investido) / IMC.";

        if (receitaLiquida.compareTo(BigDecimal.ZERO) <= 0 || mcTotal.compareTo(BigDecimal.ZERO) <= 0) {
            StatusConfiabilidadeIndicador status = receitaLiquida.compareTo(BigDecimal.ZERO) == 0 && custosFixosContabeis.compareTo(BigDecimal.ZERO) == 0
                    ? StatusConfiabilidadeIndicador.DADOS_INSUFICIENTES
                    : StatusConfiabilidadeIndicador.DIVISAO_POR_ZERO;
            return new PontoEquilibrioPainelDTO(
                    receitaLiquida,
                    mcTotal,
                    null,
                    rentabilidade.margemContribuicaoTotalPercentual(),
                    scale2(qtdTotalMix),
                    precoMedioMix,
                    mcUnitariaMediaMix,
                    custosFixosContabeis,
                    depreciacaoNaoDesembolsavel,
                    amortizacaoPrincipal,
                    desembolsosFixosFinanceiros,
                    capitalInvestido,
                    scale2(taxaRetornoPeriodoPct),
                    remuneracaoMinimaCapital,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    metodologia,
                    status,
                    List.of(),
                    List.of());
        }

        BigDecimal imcDecimal = mcTotal.divide(receitaLiquida, MC);
        BigDecimal imcPercentual = scale2(imcDecimal.multiply(CEM, MC));

        // Reutiliza AdmCalcEngine para o Ponto de Equilibrio Contabil e aplica a mesma razao exata para PEF e PEE
        Map<String, BigDecimal> calcPec = admCalcEngine.calculate(new AdmCalcRequestDTO(
                null, null,
                receitaLiquida,
                rentabilidade.cmvTotal(),
                custosFixosContabeis,
                ZERO,
                custosFixosContabeis,
                rentabilidade.custosDespesasVariaveisTotais(),
                mcUnitariaMediaMix != null && mcUnitariaMediaMix.compareTo(BigDecimal.ZERO) > 0 ? mcUnitariaMediaMix : null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                "PERCENTUAL"));

        BigDecimal pecReceita = scale2(calcPec.getOrDefault("pontoEquilibrioReceita", custosFixosContabeis.divide(imcDecimal, MC)));
        BigDecimal pecUnidades = mcUnitariaMediaMix != null && mcUnitariaMediaMix.compareTo(BigDecimal.ZERO) > 0
                ? scale2(custosFixosContabeis.divide(mcUnitariaMediaMix, MC))
                : null;

        BigDecimal pefReceita = scale2(desembolsosFixosFinanceiros.divide(imcDecimal, MC));
        BigDecimal pefUnidades = mcUnitariaMediaMix != null && mcUnitariaMediaMix.compareTo(BigDecimal.ZERO) > 0
                ? scale2(desembolsosFixosFinanceiros.divide(mcUnitariaMediaMix, MC))
                : null;

        BigDecimal numeradorPee = custosFixosContabeis.add(remuneracaoMinimaCapital);
        BigDecimal peeReceita = scale2(numeradorPee.divide(imcDecimal, MC));
        BigDecimal peeUnidades = mcUnitariaMediaMix != null && mcUnitariaMediaMix.compareTo(BigDecimal.ZERO) > 0
                ? scale2(numeradorPee.divide(mcUnitariaMediaMix, MC))
                : null;

        BigDecimal margemSegurancaValor = scale2(receitaLiquida.subtract(pecReceita));
        BigDecimal margemSegurancaPct = calcPercent(margemSegurancaValor, receitaLiquida);

        BigDecimal despFinNaDre = somarDespesasNaLinhaPorGrupo(dre, 14, GrupoDreGerencial.DESPESA_FINANCEIRA);
        BigDecimal resultadoOperacionalConsistente = scale2(
                rentabilidade.resultadoOperacionalDre()
                        .subtract(despFinNaDre)
                        .subtract(nvl(rentCalc.taxasCartaoPendentesNaoNaDre())));
        BigDecimal gao = resultadoOperacionalConsistente.compareTo(BigDecimal.ZERO) > 0
                ? scale2(mcTotal.divide(resultadoOperacionalConsistente, MC))
                : null;

        // Desdobramento por Filial (usando despesas fixas reais da DRE de cada filial quando disponivel) e por Categoria
        List<PontoEquilibrioFilialCategoriaDTO> pePorFilial = new ArrayList<>();
        for (MargemContribuicaoItemDTO f : rentabilidade.porFilial()) {
            BigDecimal fixoRealFilial = rentCalc.despesasFixasPorFilial().get(f.referenciaId());
            pePorFilial.add(calcularPeEscopo(
                    "FILIAL", f, fixoRealFilial, receitaLiquida, custosFixosContabeis,
                    desembolsosFixosFinanceiros, remuneracaoMinimaCapital));
        }

        List<PontoEquilibrioFilialCategoriaDTO> pePorCategoria = new ArrayList<>();
        for (MargemContribuicaoItemDTO c : rentabilidade.porCategoria()) {
            pePorCategoria.add(calcularPeEscopo(
                    "CATEGORIA", c, null, receitaLiquida, custosFixosContabeis,
                    desembolsosFixosFinanceiros, remuneracaoMinimaCapital));
        }

        return new PontoEquilibrioPainelDTO(
                receitaLiquida,
                mcTotal,
                imcDecimal.setScale(4, RoundingMode.HALF_UP),
                imcPercentual,
                scale2(qtdTotalMix),
                precoMedioMix,
                mcUnitariaMediaMix,
                custosFixosContabeis,
                depreciacaoNaoDesembolsavel,
                amortizacaoPrincipal,
                desembolsosFixosFinanceiros,
                capitalInvestido,
                scale2(taxaRetornoPeriodoPct),
                remuneracaoMinimaCapital,
                pecReceita,
                pecUnidades,
                pefReceita,
                pefUnidades,
                peeReceita,
                peeUnidades,
                margemSegurancaValor,
                margemSegurancaPct,
                gao,
                metodologia,
                StatusConfiabilidadeIndicador.CALCULADO,
                pePorFilial,
                pePorCategoria);
    }

    private PontoEquilibrioFilialCategoriaDTO calcularPeEscopo(
            String tipoEscopo,
            MargemContribuicaoItemDTO item,
            BigDecimal fixosReaisItem,
            BigDecimal receitaLiquidaTotal,
            BigDecimal fixosContabeisTotal,
            BigDecimal fixosFinanceirosTotal,
            BigDecimal remuneracaoMinimaCapitalTotal) {
        BigDecimal recItem = nvl(item.receitaLiquida());
        BigDecimal mcItem = nvl(item.margemContribuicaoValor());
        BigDecimal pesoReceita = receitaLiquidaTotal.compareTo(BigDecimal.ZERO) > 0
                ? recItem.divide(receitaLiquidaTotal, MC)
                : BigDecimal.ZERO;

        BigDecimal fixoAlocado;
        BigDecimal finAlocado;
        BigDecimal peeAlocado;
        if (fixosReaisItem != null) {
            fixoAlocado = scale2(fixosReaisItem);
            BigDecimal deltaFinSobreFixo = fixosFinanceirosTotal.subtract(fixosContabeisTotal).multiply(pesoReceita, MC);
            finAlocado = scale2(fixosReaisItem.add(deltaFinSobreFixo).max(BigDecimal.ZERO));
            peeAlocado = scale2(fixosReaisItem.add(remuneracaoMinimaCapitalTotal.multiply(pesoReceita, MC)));
        } else {
            fixoAlocado = scale2(fixosContabeisTotal.multiply(pesoReceita, MC));
            finAlocado = scale2(fixosFinanceirosTotal.multiply(pesoReceita, MC));
            peeAlocado = scale2(fixosContabeisTotal.add(remuneracaoMinimaCapitalTotal).multiply(pesoReceita, MC));
        }

        if (recItem.compareTo(BigDecimal.ZERO) <= 0 || mcItem.compareTo(BigDecimal.ZERO) <= 0) {
            return new PontoEquilibrioFilialCategoriaDTO(
                    tipoEscopo,
                    item.referenciaId(),
                    item.nome(),
                    recItem,
                    mcItem,
                    nvl(item.margemContribuicaoPercentual()),
                    fixoAlocado,
                    null,
                    null,
                    null,
                    null,
                    "DADOS_INSUFICIENTES");
        }

        BigDecimal imcItem = mcItem.divide(recItem, MC);
        BigDecimal pec = scale2(fixoAlocado.divide(imcItem, MC));
        BigDecimal pef = scale2(finAlocado.divide(imcItem, MC));
        BigDecimal pee = scale2(peeAlocado.divide(imcItem, MC));
        BigDecimal msPct = calcPercent(recItem.subtract(pec), recItem);

        String situacao = recItem.compareTo(pec) > 0
                ? "ACIMA_DO_EQUILIBRIO"
                : (recItem.compareTo(pec) == 0 ? "NO_EQUILIBRIO" : "ABAIXO_DO_EQUILIBRIO");

        return new PontoEquilibrioFilialCategoriaDTO(
                tipoEscopo,
                item.referenciaId(),
                item.nome(),
                recItem,
                mcItem,
                nvl(item.margemContribuicaoPercentual()),
                fixoAlocado,
                pec,
                pef,
                pee,
                msPct,
                situacao);
    }

    // =========================================================================
    // 6. Lote 4.3 — Markup e Precificacao Inteligente (Simulacao + Aplicacao Autorizada)
    // =========================================================================

    @Transactional(readOnly = true)
    public SimulacaoPrecificacaoResponseDTO simularPrecificacao(
            SimulacaoPrecificacaoRequestDTO body,
            Long empresaId,
            Long filialId) {
        requireTenant(empresaId, filialId);
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload de simulacao de precificacao nao pode ser nulo.");
        }
        ParametroInteligenciaAdministrativa param = obterOuCriarParametrosEmMemoria(empresaId, filialId);

        Produto produto = null;
        if (body.produtoId() != null) {
            produto = produtoRepository.findById(body.produtoId())
                    .filter(p -> Objects.equals(p.getEmpresaId(), empresaId) && Objects.equals(p.getFilialId(), filialId))
                    .orElseThrow(() -> new EntityNotFoundException("Produto nao encontrado no escopo da empresa/filial."));
        }
        return executarSimulacaoPrecificacao(body, param, produto);
    }

    private SimulacaoPrecificacaoResponseDTO executarSimulacaoPrecificacao(
            SimulacaoPrecificacaoRequestDTO body,
            ParametroInteligenciaAdministrativa param,
            Produto produto) {

        BigDecimal custoBase = body.custoAquisicaoBase() != null
                ? nonNegative(body.custoAquisicaoBase(), "custoAquisicaoBase")
                : (produto != null ? nvl(produto.getCusto()) : new BigDecimal("50.00"));
        BigDecimal freteCompra = nvl(nonNegative(body.freteCompraValor(), "freteCompraValor"));
        BigDecimal seguroCompra = nvl(nonNegative(body.seguroCompraValor(), "seguroCompraValor"));
        BigDecimal impNaoRec = nvl(nonNegative(body.impostosNaoRecuperaveisValor(), "impostosNaoRecuperaveisValor"));
        BigDecimal despAcess = nvl(nonNegative(body.despesasAcessoriasValor(), "despesasAcessoriasValor"));

        BigDecimal custoEfetivo = scale2(custoBase.add(freteCompra).add(seguroCompra).add(impNaoRec).add(despAcess));
        if (custoEfetivo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O custo de aquisicao efetivo deve ser maior que zero.");
        }

        BigDecimal aliqTribVenda = body.aliquotaTributosVendaPercentual() != null
                ? nonNegative(body.aliquotaTributosVendaPercentual(), "aliquotaTributosVendaPercentual")
                : (produto != null && somarImpostosProduto(produto).compareTo(BigDecimal.ZERO) > 0
                        ? somarImpostosProduto(produto)
                        : nvl(param.getAliquotaTributosVendaPadrao()));
        BigDecimal taxaCartao = body.taxaCartaoPercentual() != null
                ? nonNegative(body.taxaCartaoPercentual(), "taxaCartaoPercentual")
                : nvl(param.getTaxaCartaoMediaPadrao());
        BigDecimal comissao = body.comissaoVendaPercentual() != null
                ? nonNegative(body.comissaoVendaPercentual(), "comissaoVendaPercentual")
                : nvl(param.getComissaoVendaPadrao());
        BigDecimal freteVenda = body.freteVendaVariavelPercentual() != null
                ? nonNegative(body.freteVendaVariavelPercentual(), "freteVendaVariavelPercentual")
                : nvl(param.getFreteVariavelVendaPadrao());
        BigDecimal outrasVar = nvl(nonNegative(body.outrasDespesasVariaveisPercentual(), "outrasDespesasVariaveisPercentual"));

        BigDecimal dvTotalPct = scale2(aliqTribVenda.add(taxaCartao).add(comissao).add(freteVenda).add(outrasVar));
        BigDecimal margemDesejadaPct = body.margemLucroDesejadaPercentual() != null
                ? nonNegative(body.margemLucroDesejadaPercentual(), "margemLucroDesejadaPercentual")
                : nvl(param.getMarkupMargemDesejadaPadrao());
        margemDesejadaPct = scale2(margemDesejadaPct);

        BigDecimal somaTaxasEMargemPct = dvTotalPct.add(margemDesejadaPct);
        if (dvTotalPct.compareTo(CEM) >= 0 || somaTaxasEMargemPct.compareTo(CEM) >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A soma das despesas variaveis de venda (" + dvTotalPct + "%) e da margem desejada ("
                            + margemDesejadaPct + "%) nao pode ser igual ou superior a 100%.");
        }

        // Markup Divisor = 1 - (DV% + Margem%) / 100
        // Reutiliza AdmCalcEngine passando a carga total (DV% + Margem%) como taxa no divisor sobre o custoEfetivo
        Map<String, BigDecimal> calcMarkup = admCalcEngine.calculate(new AdmCalcRequestDTO(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                custoEfetivo,
                BigDecimal.ONE,
                somaTaxasEMargemPct,
                null, null, null, null, null, null, null,
                "PERCENTUAL"));

        BigDecimal markupDivisor = BigDecimal.ONE.subtract(somaTaxasEMargemPct.divide(CEM, MC)).setScale(4, RoundingMode.HALF_UP);
        BigDecimal markupMultiplicador = BigDecimal.ONE.divide(markupDivisor, MC).setScale(4, RoundingMode.HALF_UP);
        BigDecimal precoRecomendado = scale2(calcMarkup.getOrDefault("precoMarkup", custoEfetivo.divide(markupDivisor, MC)));

        BigDecimal divisorMinimo = BigDecimal.ONE.subtract(dvTotalPct.divide(CEM, MC));
        BigDecimal precoMinimo = scale2(custoEfetivo.divide(divisorMinimo, MC));

        BigDecimal precoAtual = body.precoPraticadoAtual() != null
                ? nonNegative(body.precoPraticadoAtual(), "precoPraticadoAtual")
                : (produto != null && produto.getValorVenda() != null ? produto.getValorVenda() : precoRecomendado);
        precoAtual = scale2(precoAtual);

        BigDecimal receitaLiquidaVarPrecoAtual = precoAtual.multiply(divisorMinimo, MC);
        BigDecimal margemRealAtualValor = scale2(receitaLiquidaVarPrecoAtual.subtract(custoEfetivo));
        BigDecimal margemRealAtualPct = precoAtual.compareTo(BigDecimal.ZERO) > 0
                ? scale2(margemRealAtualValor.divide(precoAtual, MC).multiply(CEM, MC))
                : ZERO;
        BigDecimal markupRealAtual = scale2(precoAtual.divide(custoEfetivo, MC));

        // Simulacao de desconto promocional
        BigDecimal descPromoPct = scale2(nvl(nonNegative(body.descontoPromocionalSimuladoPercentual(), "descontoPromocionalSimuladoPercentual")));
        if (descPromoPct.compareTo(CEM) >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O desconto promocional simulado deve ser menor que 100%.");
        }
        BigDecimal precoPromo = scale2(precoAtual.multiply(BigDecimal.ONE.subtract(descPromoPct.divide(CEM, MC)), MC));
        BigDecimal margemPromoValor = scale2(precoPromo.multiply(divisorMinimo, MC).subtract(custoEfetivo));
        BigDecimal margemPromoPct = precoPromo.compareTo(BigDecimal.ZERO) > 0
                ? scale2(margemPromoValor.divide(precoPromo, MC).multiply(CEM, MC))
                : ZERO;

        BigDecimal aumentoVolumeNecessarioPct = null;
        if (descPromoPct.compareTo(BigDecimal.ZERO) > 0
                && margemRealAtualValor.compareTo(BigDecimal.ZERO) > 0
                && margemPromoValor.compareTo(BigDecimal.ZERO) > 0
                && margemRealAtualValor.compareTo(margemPromoValor) > 0) {
            aumentoVolumeNecessarioPct = scale2(
                    margemRealAtualValor.divide(margemPromoValor, MC).subtract(BigDecimal.ONE).multiply(CEM, MC));
        }

        // Simulacao de alteracao de custo do fornecedor
        BigDecimal varCustoPct = scale2(nvl(body.variacaoCustoSimuladaPercentual()));
        BigDecimal custoReajustado = scale2(custoEfetivo.multiply(BigDecimal.ONE.add(varCustoPct.divide(CEM, MC)), MC).max(new BigDecimal("0.01")));
        BigDecimal precoRecAposReajuste = scale2(custoReajustado.divide(markupDivisor, MC));
        BigDecimal margemMantendoPrecoAtualValor = precoAtual.multiply(divisorMinimo, MC).subtract(custoReajustado);
        BigDecimal margemMantendoPrecoAtualPct = precoAtual.compareTo(BigDecimal.ZERO) > 0
                ? scale2(margemMantendoPrecoAtualValor.divide(precoAtual, MC).multiply(CEM, MC))
                : ZERO;

        String situacaoPreco;
        if (precoAtual.compareTo(precoMinimo) < 0) {
            situacaoPreco = "ABAIXO_PRECO_MINIMO_PREJUIZO";
        } else if (precoAtual.compareTo(precoRecomendado) < 0) {
            situacaoPreco = "ABAIXO_MARGEM_DESEJADA";
        } else {
            situacaoPreco = "ADEQUADO_ACIMA_RECOMENDADO";
        }

        String explicacaoMarkupVsMargem = "Markup (" + markupMultiplicador + "x sobre o custo de R$ " + custoEfetivo
                + ") e o fator multiplicador aplicado ao custo para formar o preco de venda (R$ " + precoRecomendado
                + "). Margem sobre a venda (" + margemDesejadaPct + "%) e a fatia do preco final que sobra livre apos pagar o custo ("
                + custoEfetivo + ") e todas as despesas variaveis de venda (" + dvTotalPct + "%).";

        List<String> recs = new ArrayList<>();
        if ("ABAIXO_PRECO_MINIMO_PREJUIZO".equals(situacaoPreco)) {
            recs.add("Alerta Critico: o preco atual (R$ " + precoAtual + ") esta abaixo do Preco Minimo de Equilibrio (R$ "
                    + precoMinimo + "). Cada unidade vendida gera prejuizo direto de R$ " + margemRealAtualValor.abs() + ".");
        } else if ("ABAIXO_MARGEM_DESEJADA".equals(situacaoPreco)) {
            recs.add("Atencao: o preco atual (R$ " + precoAtual + ") cobre o custo e impostos/taxas, mas entrega margem real de "
                    + margemRealAtualPct + "%, inferior a meta de " + margemDesejadaPct + "% (Preco Recomendado: R$ " + precoRecomendado + ").");
        } else {
            recs.add("Preco saudavel: o preco praticado (R$ " + precoAtual + ") garante margem real de "
                    + margemRealAtualPct + "% (R$ " + margemRealAtualValor + " por unidade).");
        }
        if (aumentoVolumeNecessarioPct != null) {
            recs.add("Para conceder desconto promocional de " + descPromoPct + "% (preco R$ " + precoPromo
                    + "), sera necessario vender " + aumentoVolumeNecessarioPct + "% a mais em quantidade para gerar a mesma contribuicao em R$.");
        } else if (descPromoPct.compareTo(BigDecimal.ZERO) > 0 && margemPromoValor.compareTo(BigDecimal.ZERO) <= 0) {
            recs.add("Inviavel: um desconto de " + descPromoPct + "% reduz o preco para R$ " + precoPromo
                    + ", tornando a margem unitaria negativa (R$ " + margemPromoValor + ").");
        }

        return new SimulacaoPrecificacaoResponseDTO(
                produto != null ? produto.getIdProduto() : null,
                produto != null ? produto.getDescricao() : "Simulacao Livre de Formacao de Preco",
                scale2(custoBase),
                custoEfetivo,
                dvTotalPct,
                margemDesejadaPct,
                markupDivisor,
                markupMultiplicador,
                precoMinimo,
                precoRecomendado,
                precoAtual,
                margemRealAtualValor,
                margemRealAtualPct,
                markupRealAtual,
                precoPromo,
                margemPromoValor,
                margemPromoPct,
                aumentoVolumeNecessarioPct,
                custoReajustado,
                precoRecAposReajuste,
                margemMantendoPrecoAtualPct,
                situacaoPreco,
                explicacaoMarkupVsMargem,
                recs,
                true);
    }

    @Transactional
    public ProdutoResponseDTO aplicarPrecoProdutoAutorizado(
            AplicarPrecoProdutoRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        if (body == null || body.produtoId() == null || body.novoValorVenda() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produto e novoValorVenda sao obrigatorios.");
        }
        Produto produto = produtoRepository.findByIdProdutoAndEmpresaIdAndFilialId(body.produtoId(), empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Produto nao encontrado no escopo da empresa/filial."));

        BigDecimal precoAnterior = nvl(produto.getValorVenda());
        BigDecimal novoPreco = scale2(positive(body.novoValorVenda(), "novoValorVenda"));
        produto.setValorVenda(novoPreco);
        if (body.novaMargemPercentual() != null) {
            produto.setMargem(scale2(nonNegative(body.novaMargemPercentual(), "novaMargemPercentual")));
        } else if (produto.getCusto() != null && produto.getCusto().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margemSobrePrecoVenda = novoPreco.subtract(produto.getCusto())
                    .divide(novoPreco, MC)
                    .multiply(CEM, MC);
            produto.setMargem(scale2(margemSobrePrecoVenda.max(BigDecimal.ZERO)));
        }

        Produto salvo = produtoRepository.save(produto);
        registrarAuditoria(
                "PRECO_PRODUTO_ATUALIZADO_MOTOR_PRECIFICACAO",
                "Produto",
                salvo.getIdProduto(),
                "precoAnterior=" + precoAnterior + ";novoPreco=" + novoPreco + ";justificativa=" + body.justificativa(),
                "Aplicacao autorizada de preco calculado no motor de precificacao inteligente.",
                empresaId,
                filialId,
                request);

        return new ProdutoResponseDTO(
                salvo.getIdProduto(),
                salvo.getDescricao(),
                salvo.getCusto(),
                salvo.getCodigoBarras(),
                salvo.getCodigoInterno(),
                salvo.getCodigoOriginal(),
                salvo.getMarca(),
                salvo.getGrupo(),
                salvo.getSubgrupo(),
                salvo.getUnidadeMedida(),
                salvo.getEmbalagem(),
                salvo.getDataAquisicao(),
                salvo.getQuantidadeEstoque(),
                salvo.getEstoqueMinimo(),
                salvo.getValorVenda(),
                salvo.getMargem(),
                salvo.getLocalizacao(),
                salvo.getSituacaoFiscal(),
                salvo.getNcm(),
                salvo.getCfop(),
                salvo.getIcms(),
                salvo.getIpi(),
                salvo.getPis(),
                salvo.getCofins(),
                salvo.getObservacoes(),
                salvo.getStatus(),
                salvo.getEmpresaId(),
                salvo.getFilialId(),
                salvo.getFornecedor() != null
                        ? new FornecedorResumoDTO(
                                salvo.getFornecedor().getIdFornecedor(),
                                salvo.getFornecedor().getRazaoSocial(),
                                salvo.getFornecedor().getNomeFantasia(),
                                salvo.getFornecedor().getCnpj(),
                                salvo.getFornecedor().getTelefone())
                        : null,
                salvo.getCategoria() != null
                        ? new CategoriaResumoDTO(
                                salvo.getCategoria().getIdCategoria(),
                                salvo.getCategoria().getDescricao(),
                                salvo.getCategoria().getStatus())
                        : null);
    }

    private SimulacaoPrecificacaoResponseDTO calcularPrecificacaoPadrao(
            Long empresaId,
            List<Long> filiaisEscopo,
            ParametroInteligenciaAdministrativa param,
            DadosOperacionaisEscopo dadosEscopo) {
        Produto prodRef = null;
        for (Long fId : filiaisEscopo) {
            List<Produto> prods = dadosEscopo.produtosPorFilial().getOrDefault(fId, List.of());
            prodRef = prods.stream()
                    .filter(p -> p.getStatus() == 1 && p.getCusto() != null && p.getCusto().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst()
                    .orElse(null);
            if (prodRef != null) {
                break;
            }
        }

        SimulacaoPrecificacaoRequestDTO req = new SimulacaoPrecificacaoRequestDTO(
                prodRef != null ? prodRef.getIdProduto() : null,
                prodRef != null ? prodRef.getCusto() : new BigDecimal("50.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                param.getAliquotaTributosVendaPadrao(),
                param.getTaxaCartaoMediaPadrao(),
                param.getComissaoVendaPadrao(),
                param.getFreteVariavelVendaPadrao(),
                BigDecimal.ZERO,
                param.getMarkupMargemDesejadaPadrao(),
                prodRef != null ? prodRef.getValorVenda() : null,
                new BigDecimal("10.00"),
                new BigDecimal("5.00"));
        return executarSimulacaoPrecificacao(req, param, prodRef);
    }

    // =========================================================================
    // 7. Lote 4.4 — Capital de Giro (CGL, NCG e Saldo de Tesouraria)
    // =========================================================================

    private CapitalGiroPainelDTO calcularCapitalGiro(
            YearMonth ym,
            Long empresaId,
            List<Long> filiaisEscopo,
            SaldosOperacionaisInstantaneos saldosOp,
            BalancoPatrimonialGerencialResponseDTO balanco) {
        BigDecimal acfDisponibilidades = scale2(balanco != null
                ? nvl(balanco.disponibilidadesCaixaBancos())
                : saldosOp.disponibilidades());
        BigDecimal contasReceberOp = scale2(balanco != null
                ? nvl(balanco.contasReceberOperacionais())
                : saldosOp.contasReceberAbertas());
        BigDecimal estoquesOp = scale2(balanco != null
                ? nvl(balanco.estoquesOperacionais())
                : saldosOp.estoqueAtualCusto());
        BigDecimal adiantamentosDespAnt = scale2(balanco != null
                ? nvl(balanco.adiantamentosOperacionais())
                        .add(nvl(balanco.despesasAntecipadas()))
                        .add(nvl(balanco.outrosAtivosCirculantes()))
                : BigDecimal.ZERO);

        // Ativo Circulante Operacional (ACO) = Estoques + Contas a Receber Operacionais + Adiantamentos/Despesas Antecipadas
        BigDecimal aco = scale2(estoquesOp.add(contasReceberOp).add(adiantamentosDespAnt));
        BigDecimal acTotal = scale2(acfDisponibilidades.add(aco));

        // Passivo Circulante Operacional (PCO) = Fornecedores/Contas a Pagar Operacionais + Obrigacoes Tributarias/Trabalhistas CP
        BigDecimal fornecedoresOp = scale2(balanco != null
                ? nvl(balanco.fornecedoresOperacionaisCp())
                : saldosOp.contasPagarAbertas());
        BigDecimal obrigacoesFiscaisTrab = scale2(balanco != null
                ? nvl(balanco.obrigacoesTributariasTrabalhistasCp()).add(nvl(balanco.outrasObrigacoesOperacionaisCp()))
                : BigDecimal.ZERO);
        BigDecimal pco = scale2(fornecedoresOp.add(obrigacoesFiscaisTrab));

        // Passivo Circulante Financeiro (PCF) = Emprestimos e Financiamentos de Curto Prazo
        BigDecimal pcf = scale2(balanco != null ? nvl(balanco.emprestimosFinanciamentosCp()) : BigDecimal.ZERO);
        BigDecimal pcTotal = scale2(pco.add(pcf));

        // 1. Capital de Giro Liquido (CGL) = Ativo Circulante Total - Passivo Circulante Total
        BigDecimal cgl = scale2(acTotal.subtract(pcTotal));

        // 2. Necessidade de Capital de Giro (NCG) = Ativo Circulante Operacional (ACO) - Passivo Circulante Operacional (PCO)
        BigDecimal ncg = scale2(aco.subtract(pco));

        // 3. Saldo de Tesouraria (ST) = CGL - NCG (identico a ACF - PCF)
        BigDecimal st = scale2(cgl.subtract(ncg));

        // Compara com competencia anterior para identificar variacao da NCG e risco de Efeito Tesoura
        YearMonth ymAnt = ym.minusMonths(1);
        BigDecimal ncgAnterior = estimarNcgCompetenciaAnterior(ymAnt, empresaId, filiaisEscopo);
        BigDecimal variacaoNcgValor = ncgAnterior != null ? scale2(ncg.subtract(ncgAnterior)) : null;
        BigDecimal variacaoNcgPct = ncgAnterior != null && ncgAnterior.compareTo(BigDecimal.ZERO) != 0
                ? calcHorizontalPercent(ncg, ncgAnterior)
                : null;

        BigDecimal pctEstoque = aco.compareTo(BigDecimal.ZERO) > 0 ? scale2(estoquesOp.divide(aco, MC).multiply(CEM, MC)) : ZERO;
        BigDecimal pctClientes = aco.compareTo(BigDecimal.ZERO) > 0 ? scale2(contasReceberOp.divide(aco, MC).multiply(CEM, MC)) : ZERO;
        BigDecimal pctFinanciadoForn = aco.compareTo(BigDecimal.ZERO) > 0 ? scale2(pco.divide(aco, MC).multiply(CEM, MC)) : ZERO;

        boolean efeitoTesoura = st.compareTo(BigDecimal.ZERO) < 0
                && ncg.compareTo(BigDecimal.ZERO) > 0
                && (variacaoNcgValor == null || variacaoNcgValor.compareTo(BigDecimal.ZERO) > 0);

        String perfil;
        if (efeitoTesoura) {
            perfil = "EFEITO_TESOURA_CRITICO";
        } else if (st.compareTo(BigDecimal.ZERO) < 0) {
            perfil = "TENSAO_DEPENDENCIA_CAPITAL_TERCEIROS";
        } else if (cgl.compareTo(ncg) >= 0 && st.compareTo(BigDecimal.ZERO) > 0) {
            perfil = "SOLIDA_SUPERAVIT_TESOURARIA";
        } else {
            perfil = "EQUILIBRADA_FINANCIAMENTO_OPERACIONAL";
        }

        StatusConfiabilidadeIndicador statusNcg = (aco.compareTo(BigDecimal.ZERO) == 0 && pco.compareTo(BigDecimal.ZERO) == 0)
                ? StatusConfiabilidadeIndicador.DADOS_INSUFICIENTES
                : StatusConfiabilidadeIndicador.CALCULADO;
        StatusConfiabilidadeIndicador statusCgl = (balanco != null && balanco.confirmadoGestor())
                ? StatusConfiabilidadeIndicador.CALCULADO
                : (acTotal.compareTo(BigDecimal.ZERO) > 0 || pcTotal.compareTo(BigDecimal.ZERO) > 0
                        ? StatusConfiabilidadeIndicador.CALCULADO
                        : StatusConfiabilidadeIndicador.DADOS_INSUFICIENTES);

        String interpretacao = "O Capital de Giro Liquido (CGL = AC - PC) e de R$ " + cgl
                + ", enquanto a Necessidade de Capital de Giro Operacional (NCG = ACO - PCO) e de R$ " + ncg
                + ", resultando em um Saldo de Tesouraria (ST = CGL - NCG) de R$ " + st + ". "
                + (ncg.compareTo(BigDecimal.ZERO) > 0
                        ? "A operacao exige R$ " + ncg + " de capital proprio ou financeiro para financiar estoques (R$ "
                                + estoquesOp + ") e vendas a prazo (R$ " + contasReceberOp + ") acima do prazo obtido com fornecedores (R$ " + pco + ")."
                        : "Os fornecedores e obrigacoes operacionais (R$ " + pco + ") financiam integralmente os ativos operacionais (R$ " + aco + ").");

        List<String> fatores = new ArrayList<>();
        if (pctEstoque.compareTo(new BigDecimal("50.00")) >= 0 && estoquesOp.compareTo(BigDecimal.ZERO) > 0) {
            fatores.add("Estoque concentra " + pctEstoque + "% do Ativo Circulante Operacional (R$ " + estoquesOp + ").");
        }
        if (pctClientes.compareTo(new BigDecimal("50.00")) >= 0 && contasReceberOp.compareTo(BigDecimal.ZERO) > 0) {
            fatores.add("Contas a Receber de clientes concentram " + pctClientes + "% do capital operacional (R$ " + contasReceberOp + ").");
        }
        if (efeitoTesoura) {
            fatores.add("Alerta de Efeito Tesoura: a NCG (R$ " + ncg + ") supera o CGL (R$ " + cgl + "), deixando o Saldo de Tesouraria negativo em R$ " + st.abs() + ".");
        }
        if (fatores.isEmpty()) {
            fatores.add("Estrutura de capital de giro equilibrada entre estoques, recebiveis e fornecedores.");
        }

        return new CapitalGiroPainelDTO(
                acfDisponibilidades,
                contasReceberOp,
                estoquesOp,
                adiantamentosDespAnt,
                aco,
                acTotal,
                fornecedoresOp,
                obrigacoesFiscaisTrab,
                pco,
                pcf,
                pcTotal,
                cgl,
                ncg,
                st,
                ncgAnterior,
                variacaoNcgValor,
                variacaoNcgPct,
                pctEstoque,
                pctClientes,
                pctFinanciadoForn,
                efeitoTesoura,
                perfil,
                statusCgl,
                statusNcg,
                interpretacao,
                fatores);
    }

    // =========================================================================
    // 8. Lote 4.5 — Prazos Medios (PMR, PMP, PME) e Ciclos Operacional e Financeiro
    // =========================================================================

    private PrazosCiclosPainelDTO calcularPrazosCiclos(
            YearMonth ym,
            LocalDate inicio,
            LocalDate fim,
            Long empresaId,
            Long currentFilialId,
            List<Long> filiaisEscopo,
            Long filialAlvo,
            boolean consolidar,
            DreGerencialResponseDTO dre,
            SaldosOperacionaisInstantaneos saldosOp,
            DadosOperacionaisEscopo dadosEscopo) {
        int diasPeriodo = (int) Math.max(1L, ChronoUnit.DAYS.between(inicio, fim) + 1);
        BigDecimal diasBd = BigDecimal.valueOf(diasPeriodo);

        // 1. PME = (Estoque Medio / CMV do Periodo) * Dias do Periodo
        BigDecimal estoqueFinal = apurarEstoqueEmData(filiaisEscopo, fim, saldosOp.estoqueAtualCusto(), dadosEscopo);
        BigDecimal estoqueInicial = apurarEstoqueEmData(filiaisEscopo, inicio.minusDays(1), saldosOp.estoqueAtualCusto(), dadosEscopo);
        BigDecimal estoqueMedio = scale2(estoqueInicial.add(estoqueFinal).divide(DOIS, MC));
        BigDecimal cmvPeriodo = scale2(nvl(dre.indicadores().cmv()));

        BigDecimal pmeDias = null;
        BigDecimal giroEstoque = null;
        StatusConfiabilidadeIndicador statusPme;
        if (cmvPeriodo.compareTo(BigDecimal.ZERO) > 0) {
            pmeDias = scale2(estoqueMedio.divide(cmvPeriodo, MC).multiply(diasBd, MC));
            giroEstoque = estoqueMedio.compareTo(BigDecimal.ZERO) > 0
                    ? scale2(cmvPeriodo.divide(estoqueMedio, MC))
                    : ZERO;
            statusPme = StatusConfiabilidadeIndicador.CALCULADO;
        } else {
            statusPme = estoqueMedio.compareTo(BigDecimal.ZERO) > 0
                    ? StatusConfiabilidadeIndicador.DIVISAO_POR_ZERO
                    : StatusConfiabilidadeIndicador.DADOS_INSUFICIENTES;
        }

        // 2. PMR = (Contas a Receber Medias / Vendas a Prazo do Periodo) * Dias do Periodo
        // Atencao: NAO utiliza receita a vista total como substituta de vendas a prazo!
        ApuracaoPrazoRecebiveis recPrazo = apurarRecebiveisEVendasPrazo(filiaisEscopo, inicio, fim, dadosEscopo);
        BigDecimal crInicial = recPrazo.saldoInicial();
        BigDecimal crFinal = recPrazo.saldoFinal();
        BigDecimal crMedio = scale2(crInicial.add(crFinal).divide(DOIS, MC));
        BigDecimal vendasPrazo = recPrazo.vendasPrazoPeriodo();

        BigDecimal pmrDias = null;
        StatusConfiabilidadeIndicador statusPmr;
        if (vendasPrazo.compareTo(BigDecimal.ZERO) > 0) {
            pmrDias = scale2(crMedio.divide(vendasPrazo, MC).multiply(diasBd, MC));
            statusPmr = StatusConfiabilidadeIndicador.CALCULADO;
        } else if (crMedio.compareTo(BigDecimal.ZERO) == 0 && nvl(dre.indicadores().receitaBruta()).compareTo(BigDecimal.ZERO) > 0) {
            // Empresa operou 100% a vista no periodo e sem carteira a prazo pendente
            pmrDias = ZERO;
            statusPmr = StatusConfiabilidadeIndicador.CALCULADO;
        } else {
            statusPmr = crMedio.compareTo(BigDecimal.ZERO) > 0
                    ? StatusConfiabilidadeIndicador.DIVISAO_POR_ZERO
                    : StatusConfiabilidadeIndicador.DADOS_INSUFICIENTES;
        }

        // 3. PMP = (Fornecedores Medios / Compras a Prazo do Periodo) * Dias do Periodo
        // Atencao: NAO substitui compras a prazo pela receita total!
        ApuracaoPrazoFornecedores fornPrazo = apurarFornecedoresEComprasPrazo(filiaisEscopo, inicio, fim, dadosEscopo);
        BigDecimal fornInicial = fornPrazo.saldoInicial();
        BigDecimal fornFinal = fornPrazo.saldoFinal();
        BigDecimal fornMedio = scale2(fornInicial.add(fornFinal).divide(DOIS, MC));
        BigDecimal comprasPrazo = fornPrazo.comprasPrazoPeriodo();

        BigDecimal pmpDias = null;
        StatusConfiabilidadeIndicador statusPmp;
        if (comprasPrazo.compareTo(BigDecimal.ZERO) > 0) {
            pmpDias = scale2(fornMedio.divide(comprasPrazo, MC).multiply(diasBd, MC));
            statusPmp = StatusConfiabilidadeIndicador.CALCULADO;
        } else {
            statusPmp = fornMedio.compareTo(BigDecimal.ZERO) > 0
                    ? StatusConfiabilidadeIndicador.DIVISAO_POR_ZERO
                    : StatusConfiabilidadeIndicador.DADOS_INSUFICIENTES;
        }

        // 4. Ciclo Operacional = PME + PMR e Ciclo Financeiro = Ciclo Operacional - PMP
        BigDecimal cicloOperacional = (pmeDias != null && pmrDias != null)
                ? scale2(pmeDias.add(pmrDias))
                : null;
        BigDecimal cicloFinanceiro = (cicloOperacional != null && pmpDias != null)
                ? scale2(cicloOperacional.subtract(pmpDias))
                : null;

        StatusConfiabilidadeIndicador statusCf = cicloFinanceiro != null
                ? StatusConfiabilidadeIndicador.CALCULADO
                : StatusConfiabilidadeIndicador.DADOS_INSUFICIENTES;

        boolean pagaAntesDeReceber = cicloFinanceiro != null && cicloFinanceiro.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal descompassoDias = pagaAntesDeReceber ? cicloFinanceiro : ZERO;

        // Evolucao historica dos ultimos 3 meses
        boolean dreMesCheio = inicio.equals(ym.atDay(1)) && fim.equals(ym.atEndOfMonth());
        List<PrazosCiclosEvolucaoPontoDTO> evolucao = construirEvolucaoPrazosCiclos(
                ym, empresaId, currentFilialId, filiaisEscopo, filialAlvo, consolidar,
                saldosOp.estoqueAtualCusto(), dadosEscopo, dreMesCheio ? dre : null);

        String tendencia = "ESTAVEL";
        if (evolucao.size() >= 2) {
            BigDecimal primeiro = evolucao.get(0).cicloFinanceiroDias();
            BigDecimal ultimo = evolucao.get(evolucao.size() - 1).cicloFinanceiroDias();
            if (primeiro != null && ultimo != null) {
                if (ultimo.compareTo(primeiro.add(new BigDecimal("2.00"))) > 0) {
                    tendencia = "DETERIORANDO_MAIS_LONGO";
                } else if (ultimo.compareTo(primeiro.subtract(new BigDecimal("2.00"))) < 0) {
                    tendencia = "MELHORANDO_MAIS_CURTO";
                }
            }
        }

        String interpretacao;
        if (cicloFinanceiro != null) {
            interpretacao = "PME de " + pmeDias + " dias + PMR de " + pmrDias + " dias = Ciclo Operacional de "
                    + cicloOperacional + " dias. Descontando o PMP com fornecedores de " + pmpDias
                    + " dias, o Ciclo Financeiro e de " + cicloFinanceiro + " dias. "
                    + (pagaAntesDeReceber
                            ? "Atencao: a empresa paga seus fornecedores " + descompassoDias
                                    + " dias antes de receber das vendas, exigindo capital de giro para cobrir esse hiato."
                            : "Posicao favoravel: o prazo concedido pelos fornecedores cobre integralmente o ciclo operacional.");
        } else {
            interpretacao = "Um ou mais prazos medios (PME, PMR ou PMP) nao puderam ser calculados por ausencia de CMV, vendas a prazo ou compras a prazo registradas no periodo ("
                    + inicio + " a " + fim + "). O sistema nao substitui compras ou vendas a prazo pelo faturamento total para preservar a fidelidade contábil.";
        }

        return new PrazosCiclosPainelDTO(
                diasPeriodo,
                estoqueInicial,
                estoqueFinal,
                estoqueMedio,
                cmvPeriodo,
                pmeDias,
                giroEstoque,
                crInicial,
                crFinal,
                crMedio,
                vendasPrazo,
                pmrDias,
                fornInicial,
                fornFinal,
                fornMedio,
                comprasPrazo,
                pmpDias,
                cicloOperacional,
                cicloFinanceiro,
                pagaAntesDeReceber,
                descompassoDias,
                statusPme,
                statusPmr,
                statusPmp,
                statusCf,
                tendencia,
                interpretacao,
                evolucao);
    }

    // =========================================================================
    // 9. Lote 4.6 — Liquidez e Endividamento (Com Guarda de Dependencia Patrimonial)
    // =========================================================================

    private LiquidezEndividamentoPainelDTO calcularLiquidezEndividamento(
            SaldosOperacionaisInstantaneos saldosOp,
            BalancoPatrimonialGerencialResponseDTO balanco,
            ParametroInteligenciaAdministrativa param) {
        boolean confirmado = balanco != null && balanco.confirmadoGestor();
        BigDecimal disponibilidades = scale2(balanco != null ? nvl(balanco.disponibilidadesCaixaBancos()) : saldosOp.disponibilidades());
        BigDecimal estoques = scale2(balanco != null ? nvl(balanco.estoquesOperacionais()) : saldosOp.estoqueAtualCusto());
        BigDecimal despesasAntecipadas = scale2(balanco != null ? nvl(balanco.despesasAntecipadas()) : BigDecimal.ZERO);
        BigDecimal ac = scale2(balanco != null ? nvl(balanco.ativoCirculanteTotal()) : disponibilidades.add(saldosOp.contasReceberAbertas()).add(estoques));
        BigDecimal pc = scale2(balanco != null ? nvl(balanco.passivoCirculanteTotal()) : saldosOp.contasPagarAbertas());

        // Liquidez Imediata = Disponibilidades / Passivo Circulante
        BigDecimal liquidezImediata = null;
        StatusConfiabilidadeIndicador statusLiqImediata;
        if (pc.compareTo(BigDecimal.ZERO) > 0) {
            liquidezImediata = scale2(disponibilidades.divide(pc, MC));
            statusLiqImediata = StatusConfiabilidadeIndicador.CALCULADO;
        } else {
            statusLiqImediata = disponibilidades.compareTo(BigDecimal.ZERO) > 0
                    ? StatusConfiabilidadeIndicador.DIVISAO_POR_ZERO
                    : StatusConfiabilidadeIndicador.DADOS_INSUFICIENTES;
        }

        List<String> dependenciasFaltantes = new ArrayList<>();
        if (!confirmado) {
            dependenciasFaltantes.add("Balanco Patrimonial Gerencial nao confirmado pelo gestor para a competencia " + (balanco != null ? balanco.competenciaAnoMes() : ""));
            dependenciasFaltantes.add("Patrimonio Liquido (Capital Social, Reservas e Lucros/Prejuizos Acumulados)");
            dependenciasFaltantes.add("Ativo Nao Circulante (Realizavel a Longo Prazo, Imobilizado e Intangivel)");
            dependenciasFaltantes.add("Passivo Financeiro e Nao Circulante (Emprestimos/Financiamentos de Curto e Longo Prazo)");
        } else {
            if (nvl(balanco.ativoTotal()).compareTo(BigDecimal.ZERO) <= 0) {
                dependenciasFaltantes.add("Ativo Total igual a zero no Balanco Patrimonial Gerencial");
            }
            if (nvl(balanco.patrimonioLiquidoTotal()).compareTo(BigDecimal.ZERO) <= 0) {
                dependenciasFaltantes.add("Patrimonio Liquido ausente ou nao positivo para calculo confiavel de PCT e Divida/PL");
            }
        }

        // Regra Crítica Lote 4: NAO calcular Liquidez Corrente, Seca, Geral, Endividamento e PCT sem Balanco Patrimonial Gerencial confirmado!
        if (!confirmado || nvl(balanco.ativoTotal()).compareTo(BigDecimal.ZERO) <= 0) {
            String interp = "A Liquidez Imediata operacional e de " + (liquidezImediata != null ? liquidezImediata : "N/D")
                    + " (Disponibilidades de R$ " + disponibilidades + " / Passivo Circulante de R$ " + pc + "). "
                    + "Os indicadores patrimoniais completos (Liquidez Corrente, Seca, Geral, Endividamento Geral, Participacao de Capital de Terceiros e Divida/PL) "
                    + "encontram-se com status DADOS_INSUFICIENTES porque o Balanco Patrimonial Gerencial da competencia ainda nao foi confirmado. "
                    + "O Apex Gestor nao improvisa Ativo Total ou Patrimonio Liquido apenas com caixa e contas a pagar/receber.";
            return new LiquidezEndividamentoPainelDTO(
                    false,
                    balanco != null ? balanco.competenciaAnoMes() : null,
                    disponibilidades,
                    ac,
                    estoques,
                    despesasAntecipadas,
                    null,
                    null,
                    null,
                    pc,
                    null,
                    null,
                    null,
                    null,
                    liquidezImediata,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    liquidezImediata,
                    statusLiqImediata,
                    StatusConfiabilidadeIndicador.DADOS_INSUFICIENTES,
                    dependenciasFaltantes,
                    interp);
        }

        BigDecimal rlp = scale2(nvl(balanco.realizavelLongoPrazo()));
        BigDecimal anc = scale2(nvl(balanco.ativoNaoCirculanteTotal()));
        BigDecimal ativoTotal = scale2(nvl(balanco.ativoTotal()));
        BigDecimal pnc = scale2(nvl(balanco.passivoNaoCirculanteTotal()));
        BigDecimal passivoExigivel = scale2(nvl(balanco.passivoExigivelTotal()));
        BigDecimal dividaFinanceira = scale2(nvl(balanco.emprestimosFinanciamentosCp()).add(nvl(balanco.emprestimosFinanciamentosLp())));
        BigDecimal pl = scale2(nvl(balanco.patrimonioLiquidoTotal()));

        BigDecimal liqCorrente = pc.compareTo(BigDecimal.ZERO) > 0
                ? scale2(ac.divide(pc, MC))
                : null;
        BigDecimal liqSeca = pc.compareTo(BigDecimal.ZERO) > 0
                ? scale2(ac.subtract(estoques).subtract(despesasAntecipadas).divide(pc, MC))
                : null;
        BigDecimal liqGeral = passivoExigivel.compareTo(BigDecimal.ZERO) > 0
                ? scale2(ac.add(rlp).divide(passivoExigivel, MC))
                : null;

        BigDecimal endividamentoGeralPct = ativoTotal.compareTo(BigDecimal.ZERO) > 0
                ? scale2(passivoExigivel.divide(ativoTotal, MC).multiply(CEM, MC))
                : null;
        BigDecimal pctCapitalTerceiros = pl.compareTo(BigDecimal.ZERO) > 0
                ? scale2(passivoExigivel.divide(pl, MC).multiply(CEM, MC))
                : null;
        BigDecimal composicaoEndividamentoPct = passivoExigivel.compareTo(BigDecimal.ZERO) > 0
                ? scale2(pc.divide(passivoExigivel, MC).multiply(CEM, MC))
                : ZERO;
        BigDecimal dividaSobrePlPct = pl.compareTo(BigDecimal.ZERO) > 0
                ? scale2(dividaFinanceira.divide(pl, MC).multiply(CEM, MC))
                : null;

        StatusConfiabilidadeIndicador statusPatrimonial = (confirmado && ativoTotal.compareTo(BigDecimal.ZERO) > 0)
                ? StatusConfiabilidadeIndicador.CALCULADO
                : StatusConfiabilidadeIndicador.DADOS_INSUFICIENTES;

        String interp = "Com base no Balanco Patrimonial Gerencial confirmado (" + balanco.competenciaAnoMes()
                + "), a Liquidez Corrente e " + (liqCorrente != null ? liqCorrente : "N/D")
                + ", a Liquidez Seca e " + (liqSeca != null ? liqSeca : "N/D")
                + ", a Liquidez Imediata e " + (liquidezImediata != null ? liquidezImediata : "N/D")
                + " e a Liquidez Geral e " + (liqGeral != null ? liqGeral : "N/D")
                + ". O Endividamento Geral representa " + (endividamentoGeralPct != null ? endividamentoGeralPct + "%" : "N/D")
                + " do Ativo Total (R$ " + ativoTotal + "), sendo " + composicaoEndividamentoPct + "% concentrado no curto prazo.";
        if (pl.compareTo(BigDecimal.ZERO) <= 0) {
            interp += " Atencao: Patrimonio Liquido nulo ou negativo (R$ " + pl
                    + "); PCT e Divida/PL nao sao calculados nessa condicao.";
        }
        if (!balanco.consistenteContabilmente()) {
            interp += " AVISO CONTABIL: O Balanco Patrimonial apresenta diferenca de equilibrio de R$ "
                    + balanco.diferencaEquilibrioPatrimonial() + " (Ativo Total != Passivo Exigivel + PL).";
        }

        return new LiquidezEndividamentoPainelDTO(
                true,
                balanco.competenciaAnoMes(),
                disponibilidades,
                ac,
                estoques,
                despesasAntecipadas,
                rlp,
                anc,
                ativoTotal,
                pc,
                pnc,
                passivoExigivel,
                dividaFinanceira,
                pl,
                liquidezImediata,
                liqSeca,
                liqCorrente,
                liqGeral,
                endividamentoGeralPct,
                pctCapitalTerceiros,
                composicaoEndividamentoPct,
                dividaSobrePlPct,
                liqCorrente,
                statusLiqImediata,
                statusPatrimonial,
                dependenciasFaltantes,
                interp);
    }

    // =========================================================================
    // 10. Lote 4.7 — ROI, ROA, ROE, ROIC e Viabilidade de Investimentos
    // =========================================================================

    @Transactional(readOnly = true)
    public InvestimentosRetornoPainelDTO simularInvestimento(
            SimulacaoInvestimentoRequestDTO body,
            String competencia,
            Long empresaId,
            Long filialId) {
        requireTenant(empresaId, filialId);
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload de simulacao de investimento nao pode ser nulo.");
        }
        YearMonth ym = resolverCompetencia(competencia);
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        DreGerencialService.FiltroDreRequest filtroDre = new DreGerencialService.FiltroDreRequest(
                "MENSAL", ym.toString(), ym.getYear(), null, null, inicio, fim, false, null, null, null, null, filialId, false);
        DreGerencialResponseDTO dre = dreGerencialService.apurarDre(filtroDre, empresaId, filialId);
        FluxoCaixaPainelResponseDTO fluxo = fluxoCaixaService.consultarPainelFluxoCaixa(
                inicio, fim, null, null, filialId, false, empresaId, filialId);
        DadosOperacionaisEscopo dadosEscopo = carregarDadosOperacionaisEscopo(empresaId, List.of(filialId));
        SaldosOperacionaisInstantaneos saldosOp = apurarSaldosOperacionaisInstantaneos(dadosEscopo, List.of(filialId));
        BalancoPatrimonialGerencialResponseDTO balanco = obterBalancoConsolidadoEscopo(
                ym, empresaId, List.of(filialId), filialId, saldosOp, dadosEscopo);
        ParametroInteligenciaAdministrativa param = obterOuCriarParametrosEmMemoria(empresaId, filialId);

        return calcularInvestimentosRetorno(dre, fluxo, balanco, param, body);
    }

    private InvestimentosRetornoPainelDTO calcularInvestimentosRetorno(
            DreGerencialResponseDTO dre,
            FluxoCaixaPainelResponseDTO fluxo,
            BalancoPatrimonialGerencialResponseDTO balanco,
            ParametroInteligenciaAdministrativa param,
            SimulacaoInvestimentoRequestDTO simulacaoCustom) {
        BigDecimal resultadoOperacional = scale2(nvl(dre.indicadores().resultadoOperacional()));
        BigDecimal tributosSobreLucro = scale2(nvl(dre.indicadores().tributosSobreLucro()));
        BigDecimal nopat = scale2(resultadoOperacional.subtract(tributosSobreLucro));
        BigDecimal resultadoLiquido = scale2(nvl(dre.indicadores().resultadoLiquidoGerencial()));

        boolean balancoConfirmado = balanco != null && balanco.confirmadoGestor();
        BigDecimal ativoMedio = balancoConfirmado && nvl(balanco.ativoTotal()).compareTo(BigDecimal.ZERO) > 0
                ? scale2(balanco.ativoTotal())
                : null;
        BigDecimal plMedio = balancoConfirmado && nvl(balanco.patrimonioLiquidoTotal()).compareTo(BigDecimal.ZERO) > 0
                ? scale2(balanco.patrimonioLiquidoTotal())
                : null;
        BigDecimal dividaFin = balancoConfirmado
                ? nvl(balanco.emprestimosFinanciamentosCp()).add(nvl(balanco.emprestimosFinanciamentosLp()))
                : BigDecimal.ZERO;
        BigDecimal capitalInvestidoOp = plMedio != null ? scale2(plMedio.add(dividaFin)) : null;

        BigDecimal roaPct = ativoMedio != null
                ? scale2(resultadoLiquido.divide(ativoMedio, MC).multiply(CEM, MC))
                : null;
        BigDecimal roePct = plMedio != null
                ? scale2(resultadoLiquido.divide(plMedio, MC).multiply(CEM, MC))
                : null;
        BigDecimal roicPct = (capitalInvestidoOp != null && capitalInvestidoOp.compareTo(BigDecimal.ZERO) > 0)
                ? scale2(nopat.divide(capitalInvestidoOp, MC).multiply(CEM, MC))
                : null;

        StatusConfiabilidadeIndicador statusRoaRoe = (roaPct != null && roePct != null)
                ? StatusConfiabilidadeIndicador.CALCULADO
                : StatusConfiabilidadeIndicador.DADOS_INSUFICIENTES;

        // Analise de Viabilidade de Investimento (VPL, TIR, ROI, Payback Simples e Descontado)
        // Reutiliza AdmCalcEngine
        BigDecimal invInicial;
        BigDecimal ganhoEsperado;
        BigDecimal taxaDescontoPct;
        List<BigDecimal> fluxosSimulados;
        StatusConfiabilidadeIndicador statusSimulacao;

        if (simulacaoCustom != null && simulacaoCustom.investimentoInicial() != null) {
            invInicial = scale2(positive(simulacaoCustom.investimentoInicial(), "investimentoInicial"));
            taxaDescontoPct = simulacaoCustom.taxaDescontoPeriodicaPercentual() != null
                    ? scale4(nonNegative(simulacaoCustom.taxaDescontoPeriodicaPercentual(), "taxaDescontoPeriodicaPercentual"))
                    : scale4(nvl(param.getRetornoMinimoMensalPercentual()));
            if (simulacaoCustom.fluxosCaixaProjetados() != null && !simulacaoCustom.fluxosCaixaProjetados().isEmpty()) {
                for (BigDecimal fx : simulacaoCustom.fluxosCaixaProjetados()) {
                    if (fx == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "fluxosCaixaProjetados nao pode conter valores nulos.");
                    }
                }
                BigDecimal primeiroFluxo = simulacaoCustom.fluxosCaixaProjetados().get(0);
                boolean primeiroJaEhDesembolsoInicial = primeiroFluxo.compareTo(BigDecimal.ZERO) < 0
                        && primeiroFluxo.abs().subtract(invInicial).abs().compareTo(new BigDecimal("0.01")) <= 0;
                fluxosSimulados = new ArrayList<>();
                if (!primeiroJaEhDesembolsoInicial) {
                    fluxosSimulados.add(invInicial.negate());
                }
                fluxosSimulados.addAll(simulacaoCustom.fluxosCaixaProjetados());
            } else {
                BigDecimal fcMensalBase = nvl(fluxo.geracaoLiquidaCaixaRealizado()).compareTo(BigDecimal.ZERO) > 0
                        ? fluxo.geracaoLiquidaCaixaRealizado()
                        : invInicial.multiply(new BigDecimal("0.25"), MC);
                fluxosSimulados = List.of(
                        invInicial.negate(),
                        scale2(fcMensalBase),
                        scale2(fcMensalBase),
                        scale2(fcMensalBase),
                        scale2(fcMensalBase),
                        scale2(fcMensalBase));
            }
            BigDecimal somaEntradas = fluxosSimulados.stream()
                    .skip(1)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ganhoEsperado = simulacaoCustom.ganhoEsperadoTotal() != null
                    ? scale2(simulacaoCustom.ganhoEsperadoTotal())
                    : scale2(somaEntradas);
            statusSimulacao = StatusConfiabilidadeIndicador.SIMULADO_OU_PROJETADO;
        } else {
            invInicial = nvl(param.getCapitalInvestidoReferencia()).compareTo(BigDecimal.ZERO) > 0
                    ? scale2(param.getCapitalInvestidoReferencia())
                    : (plMedio != null ? plMedio : new BigDecimal("10000.00"));
            taxaDescontoPct = scale4(nvl(param.getRetornoMinimoMensalPercentual()));
            BigDecimal geracaoPeriodo = nvl(fluxo.geracaoLiquidaCaixaRealizado()).compareTo(BigDecimal.ZERO) > 0
                    ? scale2(fluxo.geracaoLiquidaCaixaRealizado())
                    : (resultadoLiquido.compareTo(BigDecimal.ZERO) > 0 ? resultadoLiquido : new BigDecimal("2500.00"));
            fluxosSimulados = List.of(
                    invInicial.negate(),
                    geracaoPeriodo,
                    geracaoPeriodo,
                    geracaoPeriodo,
                    geracaoPeriodo,
                    geracaoPeriodo,
                    geracaoPeriodo);
            ganhoEsperado = scale2(geracaoPeriodo.multiply(BigDecimal.valueOf(6), MC));
            statusSimulacao = StatusConfiabilidadeIndicador.SIMULADO_OU_PROJETADO;
        }

        BigDecimal roiPct = null;
        BigDecimal vpl = null;
        BigDecimal tirPct = null;
        List<String> alertas = new ArrayList<>();

        try {
            AdmCalcRequestDTO reqAdm = new AdmCalcRequestDTO(
                    null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                    ganhoEsperado,
                    invInicial,
                    null, null,
                    taxaDescontoPct,
                    BigDecimal.valueOf(Math.max(1, fluxosSimulados.size() - 1)),
                    fluxosSimulados,
                    "PERCENTUAL");
            Map<String, BigDecimal> resAdm = admCalcEngine.calculate(reqAdm);
            roiPct = resAdm.get("roi");
            vpl = resAdm.get("vpl");
            tirPct = resAdm.get("tir");
            alertas.addAll(admCalcEngine.warnings(reqAdm));
        } catch (IllegalArgumentException ex) {
            alertas.add("Aviso na simulacao de VPL/TIR: " + ex.getMessage());
        }

        BigDecimal paybackSimples = calcularPaybackSimples(fluxosSimulados);
        BigDecimal paybackDescontado = calcularPaybackDescontado(fluxosSimulados, taxaDescontoPct.divide(CEM, MC));

        DiagnosticoLiquidezDTO diagLiq = fluxo.diagnosticoLiquidez();
        boolean capCaixa = diagLiq != null && diagLiq.disponibilidadeParaInvestimento();
        BigDecimal dispAposReserva = diagLiq != null ? scale2(nvl(diagLiq.margemSegurancaInvestimento())) : ZERO;
        boolean viavelEconomicamente = vpl != null && vpl.compareTo(BigDecimal.ZERO) > 0
                && (tirPct == null || tirPct.compareTo(taxaDescontoPct) >= 0);

        if (!balancoConfirmado) {
            alertas.add("ROA, ROE e ROIC exigem o Balanco Patrimonial Gerencial confirmado para evitar subestimar ou superestimar o Ativo Total e o Patrimonio Liquido.");
        } else if (plMedio == null) {
            alertas.add("Patrimonio Liquido nulo ou negativo (R$ " + nvl(balanco.patrimonioLiquidoTotal())
                    + "); ROE e ROIC nao sao calculados quando o Patrimonio Liquido e menor ou igual a zero.");
        }
        if (viavelEconomicamente && !capCaixa) {
            alertas.add("O projeto simulado possui VPL positivo (R$ " + vpl
                    + "), porem o Fluxo de Caixa atual nao apresenta folga acima da reserva minima de 30 dias para desembolso imediato.");
        }

        String explicacaoSeparacao = "ROA, ROE e ROIC sao indicadores contabeis de competencia (baseados no Lucro da DRE dividido pelo Ativo ou Patrimonio Liquido do Balanco). "
                + "Ja VPL, TIR e Payback Descontado sao indicadores financeiros baseados em Fluxo de Caixa descontado no tempo pela Taxa Minima de Atratividade ("
                + scale2(taxaDescontoPct) + "% por periodo).";

        return new InvestimentosRetornoPainelDTO(
                resultadoOperacional,
                resultadoLiquido,
                ativoMedio,
                plMedio,
                capitalInvestidoOp,
                roaPct,
                roePct,
                roicPct,
                roiPct,
                paybackSimples,
                paybackDescontado,
                vpl,
                tirPct,
                viavelEconomicamente,
                capCaixa,
                dispAposReserva,
                statusRoaRoe,
                statusSimulacao,
                explicacaoSeparacao,
                alertas);
    }

    // =========================================================================
    // 11. Catalogo Estruturado de Indicadores (Secao 11 do Prompt Mestre)
    // =========================================================================

    private List<IndicadorDetalhadoDTO> construirCatalogoIndicadores(
            String periodo,
            LocalDateTime agora,
            RentabilidadePainelDTO rent,
            PontoEquilibrioPainelDTO pe,
            SimulacaoPrecificacaoResponseDTO prec,
            CapitalGiroPainelDTO cg,
            PrazosCiclosPainelDTO pc,
            LiquidezEndividamentoPainelDTO le,
            InvestimentosRetornoPainelDTO inv,
            ParametroInteligenciaAdministrativa param,
            BalancoPatrimonialGerencialResponseDTO balanco) {
        List<IndicadorDetalhadoDTO> lista = new ArrayList<>();

        StatusConfiabilidadeIndicador statusMc = rent.receitaLiquidaTotal().compareTo(BigDecimal.ZERO) > 0
                ? StatusConfiabilidadeIndicador.CALCULADO
                : StatusConfiabilidadeIndicador.DADOS_INSUFICIENTES;
        lista.add(new IndicadorDetalhadoDTO(
                "MARGEM_CONTRIBUICAO_TOTAL",
                "Margem de Contribuicao Total",
                "RENTABILIDADE",
                "P0",
                "Quanto sobra da Receita Liquida apos pagar o CMV e todas as despesas variaveis (comissoes, taxas de cartao e fretes variaveis) para cobrir os custos fixos e gerar lucro.",
                "MC = Receita Liquida - CMV - Despesas Variaveis",
                mapOf("receitaLiquida", rent.receitaLiquidaTotal(), "cmv", rent.cmvTotal(), "despesasVariaveis", rent.custosDespesasVariaveisTotais().subtract(rent.cmvTotal())),
                statusMc == StatusConfiabilidadeIndicador.CALCULADO ? rent.margemContribuicaoTotalValor() : null,
                "R$",
                periodo,
                "DRE Gerencial (Lote 2) + Contas a Receber / Taxas de Cartao (Lote 3)",
                agora,
                statusMc == StatusConfiabilidadeIndicador.CALCULADO
                        ? "A operacao gerou R$ " + rent.margemContribuicaoTotalValor() + " (" + rent.margemContribuicaoTotalPercentual() + "% da Receita Liquida) de margem de contribuicao."
                        : "Sem faturamento liquido apurado no periodo.",
                "Depende da correta classificacao das contas de despesas entre FIXO e VARIAVEL no Plano de Contas Gerencial.",
                statusMc,
                statusMc == StatusConfiabilidadeIndicador.CALCULADO ? List.of() : List.of("Vendas finalizadas na competencia")));

        lista.add(new IndicadorDetalhadoDTO(
                "INDICE_MARGEM_CONTRIBUICAO",
                "Indice de Margem de Contribuicao (MC%)",
                "RENTABILIDADE",
                "P0",
                "Percentual de cada R$ 100,00 de Receita Liquida que permanece na empresa apos os gastos variaveis.",
                "MC% = (Margem de Contribuicao / Receita Liquida) x 100",
                mapOf("margemContribuicao", rent.margemContribuicaoTotalValor(), "receitaLiquida", rent.receitaLiquidaTotal()),
                statusMc == StatusConfiabilidadeIndicador.CALCULADO ? rent.margemContribuicaoTotalPercentual() : null,
                "%",
                periodo,
                "DRE Gerencial + Vendas PDV",
                agora,
                statusMc == StatusConfiabilidadeIndicador.CALCULADO
                        ? "Cada venda deixa " + rent.margemContribuicaoTotalPercentual() + "% de contribuicao (benchmark configurado: " + scale2(param.getMargemContribuicaoMinimaPercentual()) + "%)."
                        : "Receita liquida igual a zero no periodo.",
                "Nao considera despesas fixas operacionais.",
                statusMc,
                statusMc == StatusConfiabilidadeIndicador.CALCULADO ? List.of() : List.of("Receita Liquida > 0")));

        lista.add(new IndicadorDetalhadoDTO(
                "PONTO_EQUILIBRIO_CONTABIL",
                "Ponto de Equilibrio Contabil (PEC)",
                "PONTO_EQUILIBRIO",
                "P0",
                "Faturamento liquido minimo necessario para cobrir todos os custos e despesas fixas contabeis (resultado operacional zero).",
                "PEC = Custos e Despesas Fixas Contabeis / Indice de Margem de Contribuicao",
                mapOf("custosFixosContabeis", pe.custosDespesasFixasContabeis(), "indiceMcPct", pe.indiceMargemContribuicaoPercentual()),
                pe.pontoEquilibrioContabilReceita(),
                "R$",
                periodo,
                "DRE Gerencial + AdmCalcEngine",
                agora,
                pe.pontoEquilibrioContabilReceita() != null
                        ? "A loja precisa faturar R$ " + pe.pontoEquilibrioContabilReceita() + " de receita liquida para nao ter prejuizo contabil."
                        : "Nao foi possivel calcular o PEC (margem de contribuicao nula ou negativa).",
                "Assume mix de vendas e precos constantes ao longo do periodo.",
                pe.statusConfiabilidade(),
                pe.pontoEquilibrioContabilReceita() != null ? List.of() : List.of("Margem de Contribuicao positiva")));

        lista.add(new IndicadorDetalhadoDTO(
                "PONTO_EQUILIBRIO_FINANCEIRO",
                "Ponto de Equilibrio Financeiro (PEF)",
                "PONTO_EQUILIBRIO",
                "P0",
                "Faturamento liquido minimo para cobrir os gastos fixos desembolsaveis e amortizacoes de principal de dividas.",
                "PEF = (Custos Fixos - Depreciacao + Amortizacao Principal) / Indice de MC",
                mapOf("desembolsosFixos", pe.desembolsosFixosFinanceiros(), "depreciacao", pe.despesasNaoDesembolsaveisDepreciacao(), "amortizacaoPrincipal", pe.amortizacaoPrincipalDividasPeriodo()),
                pe.pontoEquilibrioFinanceiroReceita(),
                "R$",
                periodo,
                "DRE Gerencial + Fluxo de Caixa + Balanco Gerencial",
                agora,
                pe.pontoEquilibrioFinanceiroReceita() != null
                        ? "Exige R$ " + pe.pontoEquilibrioFinanceiroReceita() + " de faturamento liquido para honrar os compromissos fixos de caixa."
                        : "Nao calculado por ausencia de margem de contribuicao positiva.",
                "Para refletir amortizacoes de emprestimos, registre a parcela de principal no Balanco Patrimonial Gerencial.",
                pe.statusConfiabilidade(),
                pe.pontoEquilibrioFinanceiroReceita() != null ? List.of() : List.of("Margem de Contribuicao positiva")));

        lista.add(new IndicadorDetalhadoDTO(
                "PONTO_EQUILIBRIO_ECONOMICO",
                "Ponto de Equilibrio Economico (PEE)",
                "PONTO_EQUILIBRIO",
                "P0",
                "Faturamento liquido necessario para cobrir todos os custos fixos e ainda remunerar o capital investido pela taxa minima desejada.",
                "PEE = (Custos Fixos + Remuneracao Minima do Capital) / Indice de MC",
                mapOf("custosFixos", pe.custosDespesasFixasContabeis(), "remuneracaoCapital", pe.remuneracaoMinimaCapitalValor(), "capitalInvestido", pe.capitalInvestidoBase()),
                pe.pontoEquilibrioEconomicoReceita(),
                "R$",
                periodo,
                "DRE Gerencial + Parametros de Inteligencia Administrativa",
                agora,
                pe.pontoEquilibrioEconomicoReceita() != null
                        ? "Para pagar os custos fixos e remunerar o capital em R$ " + pe.remuneracaoMinimaCapitalValor() + ", o faturamento liquido deve atingir R$ " + pe.pontoEquilibrioEconomicoReceita() + "."
                        : "Nao calculado por ausencia de margem de contribuicao positiva.",
                "Sensivel ao parametro de Capital Investido e Taxa de Retorno Minimo configurados.",
                pe.statusConfiabilidade(),
                pe.pontoEquilibrioEconomicoReceita() != null ? List.of() : List.of("Margem de Contribuicao positiva")));

        lista.add(new IndicadorDetalhadoDTO(
                "MARKUP_MULTIPLICADOR_PADRAO",
                "Markup Multiplicador de Formacao de Preco",
                "PRECIFICACAO",
                "P0",
                "Fator multiplicador sobre o custo de aquisicao efetivo para cobrir tributos de venda, taxas de cartao, comissoes e margem desejada.",
                "Markup = 1 / (1 - (%Despesas Variaveis Venda + %Margem Desejada))",
                mapOf("custoEfetivo", prec.custoAquisicaoEfetivoTotal(), "despesasVariaveisPct", prec.percentualDespesasVariaveisTotal(), "margemDesejadaPct", prec.margemLucroDesejadaPercentual()),
                prec.markupMultiplicador(),
                "INDICE",
                periodo,
                "Motor de Precificacao Inteligente + AdmCalcEngine",
                agora,
                "Multiplicar o custo efetivo por " + prec.markupMultiplicador() + " garante margem liquida de venda de " + prec.margemLucroDesejadaPercentual() + "%.",
                "Nao substitui a analise de elasticidade de preco frente a concorrencia.",
                StatusConfiabilidadeIndicador.CALCULADO,
                List.of()));

        lista.add(new IndicadorDetalhadoDTO(
                "CAPITAL_GIRO_LIQUIDO_CGL",
                "Capital de Giro Liquido (CGL)",
                "CAPITAL_GIRO",
                "P1",
                "Parcela dos recursos de curto prazo (Ativo Circulante) que excede as obrigacoes de curto prazo (Passivo Circulante).",
                "CGL = Ativo Circulante Total - Passivo Circulante Total",
                mapOf("ativoCirculante", cg.ativoCirculanteTotal(), "passivoCirculante", cg.passivoCirculanteTotal()),
                cg.statusConfiabilidadeCgl() == StatusConfiabilidadeIndicador.CALCULADO ? cg.capitalGiroLiquidoCgl() : null,
                "R$",
                periodo,
                "Tesouraria + Estoque + Contas a Receber/Pagar + Balanco Gerencial",
                agora,
                "CGL apurado em R$ " + cg.capitalGiroLiquidoCgl() + ".",
                "Para incluir emprestimos bancarios de curto prazo no Passivo Circulante, mantenha o Balanco Gerencial atualizado.",
                cg.statusConfiabilidadeCgl(),
                List.of()));

        lista.add(new IndicadorDetalhadoDTO(
                "NECESSIDADE_CAPITAL_GIRO_NCG",
                "Necessidade de Capital de Giro (NCG)",
                "CAPITAL_GIRO",
                "P1",
                "Capital efetivamente imobilizado no giro operacional da loja (Estoques + Clientes - Fornecedores/Obrigacoes Operacionais).",
                "NCG = Ativo Circulante Operacional (ACO) - Passivo Circulante Operacional (PCO)",
                mapOf("ativoCirculanteOperacional", cg.ativoCirculanteOperacional(), "passivoCirculanteOperacional", cg.passivoCirculanteOperacional()),
                cg.statusConfiabilidadeNcg() == StatusConfiabilidadeIndicador.CALCULADO ? cg.necessidadeCapitalGiroNcg() : null,
                "R$",
                periodo,
                "Estoque + Contas a Receber + Contas a Pagar",
                agora,
                cg.necessidadeCapitalGiroNcg().compareTo(BigDecimal.ZERO) > 0
                        ? "A operacao consome R$ " + cg.necessidadeCapitalGiroNcg() + " de capital de giro operacional."
                        : "Os passivos operacionais financiam integralmente os ativos operacionais.",
                "Reflete a fotografia operacional dos saldos abertos e estoques valorizados ao custo medio.",
                cg.statusConfiabilidadeNcg(),
                List.of()));

        lista.add(new IndicadorDetalhadoDTO(
                "SALDO_TESOURARIA_ST",
                "Saldo de Tesouraria (ST)",
                "CAPITAL_GIRO",
                "P1",
                "Folga ou deficit financeiro liquido apos financiar a Necessidade de Capital de Giro com o Capital de Giro Liquido.",
                "ST = CGL - NCG = Ativo Circulante Financeiro - Passivo Circulante Financeiro",
                mapOf("cgl", cg.capitalGiroLiquidoCgl(), "ncg", cg.necessidadeCapitalGiroNcg()),
                cg.saldoTesourariaSt(),
                "R$",
                periodo,
                "Central de Fluxo de Caixa + Capital de Giro",
                agora,
                cg.saldoTesourariaSt().compareTo(BigDecimal.ZERO) >= 0
                        ? "Saldo de Tesouraria positivo (R$ " + cg.saldoTesourariaSt() + "): reserva financeira cobre a NCG."
                        : "Saldo de Tesouraria negativo (R$ " + cg.saldoTesourariaSt() + "): dependencia de recursos de terceiros no curto prazo.",
                "Deve ser monitorado em conjunto com a evolucao da NCG para prevenir o Efeito Tesoura.",
                 cg.statusConfiabilidadeCgl(),
                List.of()));

        lista.add(new IndicadorDetalhadoDTO(
                "PME_PRAZO_MEDIO_ESTOCAGEM",
                "Prazo Medio de Estocagem (PME)",
                "PRAZOS_CICLOS",
                "P1",
                "Tempo medio (em dias) que as mercadorias permanecem estocadas antes de serem vendidas.",
                "PME = (Estoque Medio / CMV do Periodo) x Dias do Periodo",
                mapOf("estoqueMedio", pc.estoqueMedioPeriodo(), "cmvPeriodo", pc.cmvPeriodo(), "diasPeriodo", BigDecimal.valueOf(pc.diasPeriodoAnalisado())),
                pc.pmePrazoMedioEstocagemDias(),
                "DIAS",
                periodo,
                "EstoqueMovimentacao + Produto + DRE (CMV)",
                agora,
                pc.pmePrazoMedioEstocagemDias() != null
                        ? "O estoque leva em media " + pc.pmePrazoMedioEstocagemDias() + " dias para girar."
                        : "Indisponivel por ausencia de CMV apurado no periodo.",
                "Requer registro regular de baixas de estoque nas vendas.",
                pc.statusConfiabilidadePme(),
                pc.pmePrazoMedioEstocagemDias() != null ? List.of() : List.of("CMV > 0 no periodo")));

        lista.add(new IndicadorDetalhadoDTO(
                "PMR_PRAZO_MEDIO_RECEBIMENTO",
                "Prazo Medio de Recebimento (PMR)",
                "PRAZOS_CICLOS",
                "P1",
                "Tempo medio (em dias) que a empresa leva para receber suas vendas a prazo/cartao.",
                "PMR = (Contas a Receber Medias / Vendas a Prazo do Periodo) x Dias do Periodo",
                mapOf("contasReceberMedia", pc.contasReceberMediaPeriodo(), "vendasPrazo", pc.vendasPrazoPeriodo(), "diasPeriodo", BigDecimal.valueOf(pc.diasPeriodoAnalisado())),
                pc.pmrPrazoMedioRecebimentoDias(),
                "DIAS",
                periodo,
                "ContaReceber (Titulos a Prazo / Cartao)",
                agora,
                pc.pmrPrazoMedioRecebimentoDias() != null
                        ? "Os clientes e operadoras levam em media " + pc.pmrPrazoMedioRecebimentoDias() + " dias para liquidar as vendas a prazo."
                        : "Sem vendas a prazo registradas no periodo para calculo do PMR.",
                "Calculado exclusivamente sobre vendas a prazo (nao mistura receita a vista).",
                pc.statusConfiabilidadePmr(),
                pc.pmrPrazoMedioRecebimentoDias() != null ? List.of() : List.of("Vendas a prazo emitidas no periodo")));

        lista.add(new IndicadorDetalhadoDTO(
                "PMP_PRAZO_MEDIO_PAGAMENTO",
                "Prazo Medio de Pagamento (PMP)",
                "PRAZOS_CICLOS",
                "P1",
                "Tempo medio (em dias) que a empresa utiliza para pagar seus fornecedores de mercadorias e insumos.",
                "PMP = (Fornecedores Medios / Compras a Prazo do Periodo) x Dias do Periodo",
                mapOf("fornecedoresMedia", pc.fornecedoresMediaPeriodo(), "comprasPrazo", pc.comprasPrazoPeriodo(), "diasPeriodo", BigDecimal.valueOf(pc.diasPeriodoAnalisado())),
                pc.pmpPrazoMedioPagamentoDias(),
                "DIAS",
                periodo,
                "NotaFiscalEntrada + ContaPagar (Fornecedores)",
                agora,
                pc.pmpPrazoMedioPagamentoDias() != null
                        ? "Prazo medio de pagamento a fornecedores de " + pc.pmpPrazoMedioPagamentoDias() + " dias."
                        : "Sem compras a prazo/NFs de entrada registradas no periodo.",
                "Calculado exclusivamente sobre compras e obrigacoes com fornecedores (nunca usa receita total).",
                pc.statusConfiabilidadePmp(),
                pc.pmpPrazoMedioPagamentoDias() != null ? List.of() : List.of("Notas Fiscais de Entrada ou Contas a Pagar de Fornecedores no periodo")));

        lista.add(new IndicadorDetalhadoDTO(
                "CICLO_FINANCEIRO",
                "Ciclo Financeiro (Ciclo de Conversao de Caixa)",
                "PRAZOS_CICLOS",
                "P1",
                "Intervalo em dias entre o pagamento aos fornecedores e o efetivo recebimento das vendas dos clientes.",
                "Ciclo Financeiro = (PME + PMR) - PMP",
                mapOf("pmeDias", pc.pmePrazoMedioEstocagemDias(), "pmrDias", pc.pmrPrazoMedioRecebimentoDias(), "pmpDias", pc.pmpPrazoMedioPagamentoDias()),
                pc.cicloFinanceiroDias(),
                "DIAS",
                periodo,
                "Motor de Prazos e Ciclos Operacionais",
                agora,
                pc.cicloFinanceiroDias() != null
                        ? "Ciclo Financeiro de " + pc.cicloFinanceiroDias() + " dias."
                        : "Exige que PME, PMR e PMP estejam simultaneamente disponiveis.",
                "Depende da completude dos tres prazos medios operacionais.",
                pc.statusConfiabilidadeCicloFinanceiro(),
                pc.cicloFinanceiroDias() != null ? List.of() : List.of("PME, PMR e PMP calculados")));

        lista.add(new IndicadorDetalhadoDTO(
                "LIQUIDEZ_IMEDIATA",
                "Liquidez Imediata",
                "LIQUIDEZ",
                "P2",
                "Capacidade de pagar as obrigacoes de curto prazo utilizando apenas o saldo disponivel hoje em caixa e bancos.",
                "Liquidez Imediata = Disponibilidades Imediatas / Passivo Circulante",
                mapOf("disponibilidades", le.disponibilidadesImediatas(), "passivoCirculante", le.passivoCirculante()),
                le.liquidezImediata(),
                "INDICE",
                periodo,
                "Contas Financeiras + Passivo Circulante",
                agora,
                le.liquidezImediata() != null
                        ? "Para cada R$ 1,00 de divida de curto prazo, a loja possui R$ " + le.liquidezImediata() + " em caixa imediato."
                        : "Sem passivo circulante aberto ou sem dados suficientes.",
                "Nao inclui recebiveis futuros nem estoques.",
                le.statusConfiabilidadeLiquidezImediata(),
                List.of()));

        lista.add(new IndicadorDetalhadoDTO(
                "LIQUIDEZ_CORRENTE",
                "Liquidez Corrente",
                "LIQUIDEZ",
                "P2",
                "Capacidade de pagamento no curto prazo considerando caixa, contas a receber e estoques frente ao passivo circulante.",
                "Liquidez Corrente = Ativo Circulante / Passivo Circulante",
                mapOf("ativoCirculante", le.ativoCirculante(), "passivoCirculante", le.passivoCirculante()),
                le.liquidezCorrente(),
                "INDICE",
                periodo,
                "Balanco Patrimonial Gerencial",
                agora,
                le.liquidezCorrente() != null
                        ? "Liquidez Corrente de " + le.liquidezCorrente() + " (minimo recomendado: " + scale2(param.getLiquidezCorrenteMinima()) + ")."
                        : "Bloqueado ate confirmacao do Balanco Patrimonial Gerencial da competencia.",
                "Exige Balanco Patrimonial Gerencial confirmado para nao omitir passivos financeiros ou tributarios.",
                le.statusConfiabilidadePatrimonial(),
                le.dependenciasPatrimoniaisFaltantes()));

        lista.add(new IndicadorDetalhadoDTO(
                "LIQUIDEZ_SECA",
                "Liquidez Seca",
                "LIQUIDEZ",
                "P2",
                "Capacidade de pagamento de curto prazo sem depender da venda dos estoques nem de despesas antecipadas.",
                "Liquidez Seca = (Ativo Circulante - Estoques - Despesas Antecipadas) / Passivo Circulante",
                mapOf("ativoCirculante", le.ativoCirculante(), "estoques", le.estoques(), "despesasAntecipadas", le.despesasAntecipadas(), "passivoCirculante", le.passivoCirculante()),
                le.liquidezSeca(),
                "INDICE",
                periodo,
                "Balanco Patrimonial Gerencial",
                agora,
                le.liquidezSeca() != null
                        ? "Liquidez Seca de " + le.liquidezSeca() + "."
                        : "Requer confirmacao do Balanco Patrimonial Gerencial.",
                "Exige Balanco Patrimonial Gerencial confirmado.",
                le.statusConfiabilidadePatrimonial(),
                le.dependenciasPatrimoniaisFaltantes()));

        lista.add(new IndicadorDetalhadoDTO(
                "LIQUIDEZ_GERAL",
                "Liquidez Geral",
                "LIQUIDEZ",
                "P2",
                "Capacidade de pagamento de curto e longo prazo considerando todo o ativo realizavel frente ao passivo exigivel total.",
                "Liquidez Geral = (Ativo Circulante + Realizavel a Longo Prazo) / (Passivo Circulante + Passivo Nao Circulante)",
                mapOf("ativoCirculante", le.ativoCirculante(), "realizavelLongoPrazo", le.realizavelLongoPrazo(), "passivoExigivelTotal", le.passivoExigivelTotal()),
                le.liquidezGeral(),
                "INDICE",
                periodo,
                "Balanco Patrimonial Gerencial",
                agora,
                le.liquidezGeral() != null
                        ? "Liquidez Geral de " + le.liquidezGeral() + "."
                        : "Requer confirmacao do Balanco Patrimonial Gerencial (RLP e PNC).",
                "Exige Balanco Patrimonial Gerencial confirmado.",
                le.statusConfiabilidadePatrimonial(),
                le.dependenciasPatrimoniaisFaltantes()));

        lista.add(new IndicadorDetalhadoDTO(
                "ENDIVIDAMENTO_GERAL",
                "Endividamento Geral",
                "ENDIVIDAMENTO",
                "P2",
                "Proporcao do Ativo Total que esta sendo financiada por capital de terceiros (curto e longo prazo).",
                "Endividamento Geral (%) = (Passivo Exigivel Total / Ativo Total) x 100",
                mapOf("passivoExigivelTotal", le.passivoExigivelTotal(), "ativoTotal", le.ativoTotal()),
                le.endividamentoGeralPercentual(),
                "%",
                periodo,
                "Balanco Patrimonial Gerencial",
                agora,
                le.endividamentoGeralPercentual() != null
                        ? "O capital de terceiros financia " + le.endividamentoGeralPercentual() + "% dos ativos da empresa."
                        : "Requer confirmacao do Balanco Patrimonial Gerencial.",
                "Nao pode ser calculado sem o Ativo Nao Circulante e Patrimonio Liquido.",
                le.statusConfiabilidadePatrimonial(),
                le.dependenciasPatrimoniaisFaltantes()));

        boolean roeBloqueadoPorPl = inv.roeRetornoSobrePatrimonioPercentual() == null
                && balanco != null
                && balanco.confirmadoGestor()
                && nvl(balanco.ativoTotal()).compareTo(BigDecimal.ZERO) > 0
                && nvl(balanco.patrimonioLiquidoTotal()).compareTo(BigDecimal.ZERO) <= 0;
        StatusConfiabilidadeIndicador statusRoe = inv.roeRetornoSobrePatrimonioPercentual() != null
                ? StatusConfiabilidadeIndicador.CALCULADO
                : (roeBloqueadoPorPl ? StatusConfiabilidadeIndicador.DIVISAO_POR_ZERO : inv.statusConfiabilidadeRoaRoe());
        List<String> dependenciasRoe = roeBloqueadoPorPl
                ? List.of("Patrimonio Liquido positivo (> 0) necessario para calcular ROE.")
                : le.dependenciasPatrimoniaisFaltantes();

        lista.add(new IndicadorDetalhadoDTO(
                "ROE_RETORNO_PATRIMONIO_LIQUIDO",
                "Retorno sobre o Patrimonio Liquido (ROE)",
                "INVESTIMENTO",
                "P2",
                "Rentabilidade contábil gerada para os proprietarios/socios em relacao ao Patrimonio Liquido investido.",
                "ROE (%) = (Resultado Liquido DRE / Patrimonio Liquido Medio) x 100",
                mapOf("resultadoLiquidoDre", inv.resultadoLiquidoPeriodo(), "patrimonioLiquidoMedio", inv.patrimonioLiquidoMedio()),
                inv.roeRetornoSobrePatrimonioPercentual(),
                "%",
                periodo,
                "DRE Gerencial + Balanco Patrimonial Gerencial",
                agora,
                inv.roeRetornoSobrePatrimonioPercentual() != null
                        ? "ROE de " + inv.roeRetornoSobrePatrimonioPercentual() + "% no periodo."
                        : (roeBloqueadoPorPl
                                ? "Patrimonio Liquido nulo ou negativo (R$ " + scale2(nvl(balanco.patrimonioLiquidoTotal())) + "); calculo de ROE nao aplicavel."
                                : "Requer confirmacao do Patrimonio Liquido no Balanco Patrimonial Gerencial."),
                "Indicador contabil por competencia; nao deve ser confundido com fluxo de caixa livre.",
                statusRoe,
                dependenciasRoe));

        return lista;
    }

    // =========================================================================
    // 12. Lote 4.8 — Diagnosticos Deterministicos e Explicaveis
    // =========================================================================

    private List<DiagnosticoAdministrativoItemDTO> construirDiagnosticosAdministrativos(
            String periodo,
            DreGerencialResponseDTO dre,
            RentabilidadePainelDTO rent,
            PontoEquilibrioPainelDTO pe,
            CapitalGiroPainelDTO cg,
            PrazosCiclosPainelDTO pc,
            LiquidezEndividamentoPainelDTO le,
            InvestimentosRetornoPainelDTO inv,
            ParametroInteligenciaAdministrativa param,
            BalancoPatrimonialGerencialResponseDTO balanco) {
        List<DiagnosticoAdministrativoItemDTO> lista = new ArrayList<>();

        // 1. Ponto de Equilibrio acima do faturamento atual
        if (pe.pontoEquilibrioContabilReceita() != null
                && rent.receitaLiquidaTotal().compareTo(BigDecimal.ZERO) > 0
                && pe.pontoEquilibrioContabilReceita().compareTo(rent.receitaLiquidaTotal()) > 0) {
            BigDecimal deficit = scale2(pe.pontoEquilibrioContabilReceita().subtract(rent.receitaLiquidaTotal()));
            lista.add(new DiagnosticoAdministrativoItemDTO(
                    "PONTO_EQUILIBRIO_ACIMA_FATURAMENTO",
                    "CRITICO",
                    "Ponto de Equilibrio Contabil (PEC)",
                    "PEC R$ " + pe.pontoEquilibrioContabilReceita() + " vs Receita Liquida R$ " + rent.receitaLiquidaTotal(),
                    periodo,
                    "DRE Gerencial + Motor de Ponto de Equilibrio",
                    "O faturamento liquido atual ficou R$ " + deficit + " abaixo do Ponto de Equilibrio Contabil.",
                    "A operacao nao gerou margem de contribuicao suficiente para pagar a estrutura fixa da loja, acumulando prejuizo operacional.",
                    "Reduzir despesas fixas nao essenciais, revisar descontos comerciais ou ampliar o mix de produtos com maior margem de contribuicao.",
                    pe.statusConfiabilidade()));
        }

        // 2. Margem de contribuicao insuficiente frente ao benchmark da empresa
        if (rent.receitaLiquidaTotal().compareTo(BigDecimal.ZERO) > 0
                && rent.margemContribuicaoTotalPercentual().compareTo(param.getMargemContribuicaoMinimaPercentual()) < 0) {
            lista.add(new DiagnosticoAdministrativoItemDTO(
                    "MARGEM_CONTRIBUICAO_INSUFICIENTE",
                    "ALERTA",
                    "Indice de Margem de Contribuicao (MC%)",
                    rent.margemContribuicaoTotalPercentual() + "% (Meta minima: " + scale2(param.getMargemContribuicaoMinimaPercentual()) + "%)",
                    periodo,
                    "DRE Gerencial + Vendas PDV + Contas a Receber",
                    "A margem de contribuicao percentual esta abaixo do patamar minimo parametrizado para o segmento " + param.getSegmentoNegocio() + ".",
                    "Exige um volume de vendas excessivamente alto para cobrir os custos fixos e eleva o Ponto de Equilibrio.",
                    "Renegociar custo de aquisicao (CMV) com fornecedores, revisar taxas de maquininha/antecipacao e ajustar precos dos itens subprecificados.",
                    StatusConfiabilidadeIndicador.CALCULADO));
        }

        // 3. Produtos com margem negativa ou armadilha de volume (alto faturamento e baixa margem)
        if (!rent.alertasAltoVolumeBaixaMargem().isEmpty()) {
            MargemContribuicaoItemDTO pior = rent.alertasAltoVolumeBaixaMargem().get(0);
            boolean negativa = pior.margemContribuicaoValor().compareTo(BigDecimal.ZERO) < 0;
            lista.add(new DiagnosticoAdministrativoItemDTO(
                    negativa ? "PRODUTO_MARGEM_NEGATIVA" : "PRODUTO_ALTO_VOLUME_BAIXA_MARGEM",
                    negativa ? "CRITICO" : "ALERTA",
                    "Margem de Contribuicao por Produto (" + pior.nome() + ")",
                    "MC: R$ " + pior.margemContribuicaoValor() + " (" + pior.margemContribuicaoPercentual() + "%) | Fat.: " + pior.participacaoFaturamentoPercentual() + "%",
                    periodo,
                    "Itens de Venda (ProdutoVenda) + Devolucoes + Custo Medio",
                    negativa
                            ? "O produto '" + pior.nome() + "' apresenta margem de contribuicao negativa após abater custo de reposicao, impostos e taxas."
                            : "O produto '" + pior.nome() + "' representa parcela relevante do faturamento (" + pior.participacaoFaturamentoPercentual() + "%), mas deixa baixa margem (" + pior.margemContribuicaoPercentual() + "%).",
                    negativa
                            ? "Quanto mais unidades deste item sao vendidas, maior o prejuizo acumulado pela loja."
                            : "Sensacao falsa de movimento: alto giro operacional com baixa geracao real de caixa e lucro.",
                    "Simular reajuste no Motor de Precificacao Inteligente (Lote 4.3) ou restringir descontos sobre este item.",
                    StatusConfiabilidadeIndicador.CALCULADO));
        }

        // 4. Capital de giro comprometido / Efeito Tesoura
        if (cg.riscoEfeitoTesoura() || cg.saldoTesourariaSt().compareTo(BigDecimal.ZERO) < 0) {
            lista.add(new DiagnosticoAdministrativoItemDTO(
                    "CAPITAL_GIRO_COMPROMETIDO",
                    cg.riscoEfeitoTesoura() ? "CRITICO" : "ALERTA",
                    "Saldo de Tesouraria (ST) e NCG",
                    "ST: R$ " + cg.saldoTesourariaSt() + " | NCG: R$ " + cg.necessidadeCapitalGiroNcg() + " | CGL: R$ " + cg.capitalGiroLiquidoCgl(),
                    periodo,
                    "Analise Dinamica do Capital de Giro (Estoque + CR + CP + Caixa)",
                    "A Necessidade de Capital de Giro (R$ " + cg.necessidadeCapitalGiroNcg() + ") supera o Capital de Giro Liquido (R$ " + cg.capitalGiroLiquidoCgl() + ").",
                    "O dinheiro da empresa esta retido em estoque (" + cg.percentualCapitalPresoEstoque() + "% do ACO) e clientes (" + cg.percentualCapitalPresoClientes() + "%), gerando dependencia de capital de terceiros ou antecipacao de recebiveis.",
                    "Reduzir compras de itens de baixo giro, encurtar parcelamentos concedidos e negociar prazos maiores com fornecedores.",
                    cg.statusConfiabilidadeNcg()));
        }

        // 5. Ciclo financeiro excessivamente longo / Pagamento antes do recebimento
        if (pc.cicloFinanceiroDias() != null
                && pc.cicloFinanceiroDias().compareTo(param.getCicloFinanceiroMaximoDias()) > 0) {
            lista.add(new DiagnosticoAdministrativoItemDTO(
                    "CICLO_FINANCEIRO_LONGO",
                    "ALERTA",
                    "Ciclo Financeiro (PME + PMR - PMP)",
                    pc.cicloFinanceiroDias() + " dias (Limite recomendado: " + param.getCicloFinanceiroMaximoDias() + " dias)",
                    periodo,
                    "Estoque + Contas a Receber + Notas Fiscais de Entrada / Fornecedores",
                    "A empresa desembolsa o pagamento aos fornecedores " + pc.cicloFinanceiroDias() + " dias antes de recuperar o caixa nas vendas.",
                    "Qualquer crescimento nas vendas aumentara proporcionalmente a necessidade de caixa para bancar o hiato operacional.",
                    "Acelerar o giro do estoque (PME atual: " + pc.pmePrazoMedioEstocagemDias() + "d) ou alongar o prazo medio com fornecedores (PMP atual: " + pc.pmpPrazoMedioPagamentoDias() + "d).",
                    pc.statusConfiabilidadeCicloFinanceiro()));
        }

        // 6. Estoque com baixa rotatividade
        if (pc.pmePrazoMedioEstocagemDias() != null
                && pc.pmePrazoMedioEstocagemDias().compareTo(param.getPmeMaximoDias()) > 0) {
            lista.add(new DiagnosticoAdministrativoItemDTO(
                    "ESTOQUE_BAIXA_ROTATIVIDADE",
                    "ALERTA",
                    "Prazo Medio de Estocagem (PME)",
                    pc.pmePrazoMedioEstocagemDias() + " dias (Limite configurado: " + param.getPmeMaximoDias() + " dias)",
                    periodo,
                    "Movimentacoes de Estoque + CMV da DRE",
                    "O tempo medio de permanencia das mercadorias em estoque excede o parametro operacional da loja.",
                    "Imobilizacao excessiva de capital de giro, risco de obsolescencia/perda e custo financeiro invisivel.",
                    "Promover campanhas direcionadas para itens parados respeitando o Preco Minimo de Equilibrio.",
                    pc.statusConfiabilidadePme()));
        }

        // 7. Endividamento elevado, Liquidez deteriorando ou Balanco Desbalanceado (quando Balanco confirmado)
        if (le.balancoPatrimonialConfirmado()) {
            if (balanco != null && !balanco.consistenteContabilmente()) {
                lista.add(new DiagnosticoAdministrativoItemDTO(
                        "BALANCO_PATRIMONIAL_DESBALANCEADO",
                        "ALERTA",
                        "Equilibrio Contabil do Balanco Patrimonial",
                        "Diferenca: R$ " + scale2(nvl(balanco.diferencaEquilibrioPatrimonial())) + " (Ativo != Passivo + PL)",
                        periodo,
                        "Balanco Patrimonial Gerencial",
                        "O Balanco Patrimonial Gerencial confirmado apresenta divergencia entre o Ativo Total (R$ "
                                + balanco.ativoTotal() + ") e a soma Passivo Exigivel + Patrimonio Liquido (R$ "
                                + scale2(nvl(balanco.passivoExigivelTotal()).add(nvl(balanco.patrimonioLiquidoTotal()))) + ").",
                        "Indicadores patrimoniais (Liquidez, Endividamento, ROA, ROE) perdem fidedignidade se houver conta patrimonial omitida ou invertida.",
                        "Revisar os lancamentos na aba 'Balanco e Saude Patrimonial' (Imobilizado, Emprestimos e Lucros/Prejuizos Acumulados) ate zerar a diferenca de equilibrio.",
                        le.statusConfiabilidadePatrimonial()));
            }
            if (le.endividamentoGeralPercentual() != null
                    && le.endividamentoGeralPercentual().compareTo(param.getEndividamentoGeralMaximoPercentual()) > 0) {
                lista.add(new DiagnosticoAdministrativoItemDTO(
                        "ENDIVIDAMENTO_ELEVADO",
                        "CRITICO",
                        "Endividamento Geral",
                        le.endividamentoGeralPercentual() + "% (Limite: " + scale2(param.getEndividamentoGeralMaximoPercentual()) + "%)",
                        periodo,
                        "Balanco Patrimonial Gerencial",
                        "Mais de " + le.endividamentoGeralPercentual() + "% dos ativos totais dependem de passivos exigiveis.",
                        "Aumento das despesas financeiras com juros na DRE (Linha 14) e perda de autonomia para novos investimentos.",
                        "Priorizar amortizacao das dividas mais caras e reter parte do lucro liquido para recompor o Patrimonio Liquido.",
                        le.statusConfiabilidadePatrimonial()));
            }
            if (le.liquidezCorrente() != null
                    && le.liquidezCorrente().compareTo(param.getLiquidezCorrenteMinima()) < 0) {
                lista.add(new DiagnosticoAdministrativoItemDTO(
                        "LIQUIDEZ_DETERIORANDO",
                        "ALERTA",
                        "Liquidez Corrente",
                        le.liquidezCorrente() + " (Minimo: " + scale2(param.getLiquidezCorrenteMinima()) + ")",
                        periodo,
                        "Balanco Patrimonial Gerencial",
                        "O indice de Liquidez Corrente esta abaixo da margem de seguranca parametrizada para o curto prazo.",
                        "Pressao sobre a tesouraria em caso de inadimplencia de clientes ou queda sazonal nas vendas.",
                        "Preservar caixa liquido e evitar imobilizacoes ou distribuicoes extraordinarias no curto prazo.",
                        le.statusConfiabilidadePatrimonial()));
            }
        } else {
            lista.add(new DiagnosticoAdministrativoItemDTO(
                    "DEPENDENCIA_BALANCO_PATRIMONIAL",
                    "ALERTA",
                    "Balanco Patrimonial Gerencial",
                    "DADOS_INSUFICIENTES (Pendente de confirmacao)",
                    periodo,
                    "Estrutura Patrimonial (Lote 4.6)",
                    "Os saldos operacionais de Caixa, Clientes, Estoque e Fornecedores ja foram integrados, mas faltam os dados patrimoniais estruturais (Imobilizado, Emprestimos e Patrimonio Liquido).",
                    "Os indicadores Liquidez Corrente, Seca, Geral, Endividamento e ROE permanecem bloqueados para evitar numeros falsos ou improvisados.",
                    "Preencher e confirmar a aba 'Balanco e Saude Patrimonial' com o Capital Social, Imobilizado e eventuais financiamentos da empresa.",
                    StatusConfiabilidadeIndicador.DADOS_INSUFICIENTES));
        }

        // 8. Crescimento do faturamento sem crescimento proporcional da rentabilidade
        DreLinhaDTO l5Receita = dre.linhas().stream().filter(l -> l.numeroLinha() == 5).findFirst().orElse(null);
        DreLinhaDTO l17Lucro = dre.linhas().stream().filter(l -> l.numeroLinha() == 17).findFirst().orElse(null);
        if (l5Receita != null && l17Lucro != null
                && l5Receita.analiseHorizontalPercentual() != null
                && l17Lucro.analiseHorizontalPercentual() != null
                && l5Receita.analiseHorizontalPercentual().compareTo(new BigDecimal("5.00")) > 0
                && l17Lucro.analiseHorizontalPercentual().compareTo(l5Receita.analiseHorizontalPercentual().subtract(new BigDecimal("5.00"))) < 0) {
            lista.add(new DiagnosticoAdministrativoItemDTO(
                    "CRESCIMENTO_FATURAMENTO_SEM_RENTABILIDADE",
                    "ALERTA",
                    "Evolucao Receita Liquida vs Resultado Liquido",
                    "Receita +" + l5Receita.analiseHorizontalPercentual() + "% vs Resultado " + l17Lucro.analiseHorizontalPercentual() + "%",
                    periodo,
                    "Analise Horizontal da DRE Gerencial",
                    "O faturamento cresceu em relacao ao periodo anterior, mas o lucro liquido nao acompanhou o mesmo ritmo.",
                    "Compressao de margens causada por aumento de CMV, descontos excessivos ou crescimento desproporcional das despesas.",
                    "Auditar a Analise Vertical da DRE e a matriz de rentabilidade por produto para estancar o vazamento de margem.",
                    StatusConfiabilidadeIndicador.CALCULADO));
        }

        if (lista.isEmpty() || lista.stream().allMatch(d -> "DEPENDENCIA_BALANCO_PATRIMONIAL".equals(d.codigoDiagnostico()))) {
            lista.add(new DiagnosticoAdministrativoItemDTO(
                    "OPERACAO_ECONOMICAMENTE_SAUDAVEL",
                    "SAUDAVEL",
                    "Visao Geral de Rentabilidade e Equilibrio",
                    "MC: " + rent.margemContribuicaoTotalPercentual() + "% | Lucro Liquido: R$ " + rent.resultadoLiquidoDre(),
                    periodo,
                    "DRE Gerencial + Fluxo de Caixa + Motor de Indicadores",
                    "A empresa opera acima do Ponto de Equilibrio Contabil e com margem de contribuicao compativel com sua estrutura.",
                    "Geracao sustentavel de resultado economico e manutencao da capacidade operacional.",
                    "Avaliar oportunidades de investimento ou expansao utilizando o simulador de VPL/TIR e o excedente de reserva de caixa.",
                    StatusConfiabilidadeIndicador.CALCULADO));
        }

        return lista;
    }

    // =========================================================================
    // Helpers de Apuracao Historica e Matematica Financeira
    // =========================================================================

    private record DadosOperacionaisEscopo(
            Map<Long, List<ContaFinanceira>> contasFinanceirasPorFilial,
            Map<Long, List<ContaReceber>> contasReceberPorFilial,
            Map<Long, List<Produto>> produtosPorFilial,
            Map<Long, List<ContaPagar>> contasPagarPorFilial,
            Map<Long, List<EstoqueMovimentacao>> movimentacoesEstoquePorFilial,
            Map<Long, List<NotaFiscalEntrada>> notasFiscaisEntradaPorFilial,
            Map<Long, List<Despesa>> despesasPorFilial) {
    }

    private DadosOperacionaisEscopo carregarDadosOperacionaisEscopo(Long empresaId, List<Long> filiaisEscopo) {
        Map<Long, List<ContaFinanceira>> contasFin = new LinkedHashMap<>();
        Map<Long, List<ContaReceber>> contasRec = new LinkedHashMap<>();
        Map<Long, List<Produto>> prods = new LinkedHashMap<>();
        Map<Long, List<ContaPagar>> contasPag = new LinkedHashMap<>();
        Map<Long, List<EstoqueMovimentacao>> movsEst = new LinkedHashMap<>();
        Map<Long, List<NotaFiscalEntrada>> nfs = new LinkedHashMap<>();
        Map<Long, List<Despesa>> desps = new LinkedHashMap<>();
        for (Long fId : filiaisEscopo) {
            contasFin.put(fId, contaFinanceiraRepository.findByEmpresaIdAndFilialIdAndAtivoTrueOrderByPadraoDescNomeAsc(empresaId, fId));
            contasRec.put(fId, contaReceberRepository.findByEmpresaIdAndFilialIdOrderByVencimentoDesc(empresaId, fId));
            prods.put(fId, produtoRepository.findByEmpresaIdAndFilialIdOrderByDescricaoAsc(empresaId, fId));
            contasPag.put(fId, contaPagarRepository.findByEmpresaIdAndFilialIdOrderByVencimentoDesc(empresaId, fId));
            movsEst.put(fId, estoqueMovimentacaoRepository.findByEmpresaIdAndFilialIdOrderByCriadoEmAsc(empresaId, fId));
            nfs.put(fId, notaFiscalEntradaRepository.findByEmpresaIdAndFilialIdOrderByDataEntradaDesc(empresaId, fId));
            desps.put(fId, despesaRepository.findByEmpresaIdAndFilialIdOrderByDataVencimentoDesc(empresaId, fId));
        }
        return new DadosOperacionaisEscopo(contasFin, contasRec, prods, contasPag, movsEst, nfs, desps);
    }

    private record SaldosOperacionaisInstantaneos(
            BigDecimal disponibilidades,
            BigDecimal contasReceberAbertas,
            BigDecimal estoqueAtualCusto,
            BigDecimal contasPagarAbertas) {
    }

    private SaldosOperacionaisInstantaneos apurarSaldosOperacionaisInstantaneos(Long empresaId, List<Long> filiaisEscopo) {
        return apurarSaldosOperacionaisInstantaneos(carregarDadosOperacionaisEscopo(empresaId, filiaisEscopo), filiaisEscopo);
    }

    private SaldosOperacionaisInstantaneos apurarSaldosOperacionaisInstantaneos(
            DadosOperacionaisEscopo dadosEscopo, List<Long> filiaisEscopo) {
        BigDecimal disp = BigDecimal.ZERO;
        BigDecimal crAberto = BigDecimal.ZERO;
        BigDecimal estoqueCusto = BigDecimal.ZERO;
        BigDecimal cpAberto = BigDecimal.ZERO;

        for (Long fId : filiaisEscopo) {
            SaldosOperacionaisInstantaneos parcial = apurarSaldosOperacionaisFilial(dadosEscopo, fId);
            disp = disp.add(parcial.disponibilidades());
            crAberto = crAberto.add(parcial.contasReceberAbertas());
            estoqueCusto = estoqueCusto.add(parcial.estoqueAtualCusto());
            cpAberto = cpAberto.add(parcial.contasPagarAbertas());
        }
        return new SaldosOperacionaisInstantaneos(scale2(disp), scale2(crAberto), scale2(estoqueCusto), scale2(cpAberto));
    }

    private SaldosOperacionaisInstantaneos apurarSaldosOperacionaisFilial(DadosOperacionaisEscopo dadosEscopo, Long fId) {
        BigDecimal disp = BigDecimal.ZERO;
        BigDecimal crAberto = BigDecimal.ZERO;
        BigDecimal estoqueCusto = BigDecimal.ZERO;
        BigDecimal cpAberto = BigDecimal.ZERO;

        for (ContaFinanceira cf : dadosEscopo.contasFinanceirasPorFilial().getOrDefault(fId, List.of())) {
            disp = disp.add(nvl(cf.getSaldoAtual()));
        }
        for (ContaReceber cr : dadosEscopo.contasReceberPorFilial().getOrDefault(fId, List.of())) {
            if (STATUS_ABERTO.contains(cr.getStatus())) {
                BigDecimal saldo = nvl(cr.getSaldo()).compareTo(BigDecimal.ZERO) > 0
                        ? cr.getSaldo()
                        : nvl(cr.getValorLiquido()).subtract(nvl(cr.getValorPago())).max(BigDecimal.ZERO);
                crAberto = crAberto.add(saldo);
            }
        }
        for (Produto p : dadosEscopo.produtosPorFilial().getOrDefault(fId, List.of())) {
            if (p.getStatus() == 1 && p.getQuantidadeEstoque() > 0 && p.getCusto() != null && p.getCusto().compareTo(BigDecimal.ZERO) > 0) {
                estoqueCusto = estoqueCusto.add(p.getCusto().multiply(BigDecimal.valueOf(p.getQuantidadeEstoque()), MC));
            }
        }
        for (ContaPagar cp : dadosEscopo.contasPagarPorFilial().getOrDefault(fId, List.of())) {
            if (STATUS_ABERTO.contains(cp.getStatus())) {
                BigDecimal saldo = nvl(cp.getSaldo()).compareTo(BigDecimal.ZERO) > 0
                        ? cp.getSaldo()
                        : nvl(cp.getValor()).subtract(nvl(cp.getValorPago())).max(BigDecimal.ZERO);
                cpAberto = cpAberto.add(saldo);
            }
        }
        return new SaldosOperacionaisInstantaneos(scale2(disp), scale2(crAberto), scale2(estoqueCusto), scale2(cpAberto));
    }

    private BigDecimal apurarEstoqueEmData(
            List<Long> filiaisEscopo, LocalDate dataAlvo, BigDecimal estoqueAtual, DadosOperacionaisEscopo dadosEscopo) {
        LocalDateTime limitePosterior = dataAlvo.atTime(LocalTime.MAX);
        BigDecimal variacaoPosterior = BigDecimal.ZERO;

        for (Long fId : filiaisEscopo) {
            List<EstoqueMovimentacao> movs = dadosEscopo.movimentacoesEstoquePorFilial().getOrDefault(fId, List.of());
            for (EstoqueMovimentacao m : movs) {
                if (m.getCriadoEm() == null || !m.getCriadoEm().isAfter(limitePosterior)) {
                    continue;
                }
                int deltaQtd = m.getSaldoPosterior() - m.getSaldoAnterior();
                BigDecimal custoUnit = m.getCustoUnitario() != null && m.getCustoUnitario().compareTo(BigDecimal.ZERO) > 0
                        ? m.getCustoUnitario()
                        : (m.getCustoMedioPosterior() != null && m.getCustoMedioPosterior().compareTo(BigDecimal.ZERO) > 0
                                ? m.getCustoMedioPosterior()
                                : (m.getProduto() != null ? nvl(m.getProduto().getCusto()) : BigDecimal.ZERO));
                BigDecimal valorMov = m.getValorTotalCusto() != null && m.getValorTotalCusto().compareTo(BigDecimal.ZERO) > 0
                        ? m.getValorTotalCusto()
                        : custoUnit.multiply(BigDecimal.valueOf(Math.abs(deltaQtd != 0 ? deltaQtd : m.getQuantidade())), MC);
                if (deltaQtd > 0 || m.getTipo() == TipoMovimentacaoEstoque.ENTRADA
                        || m.getTipo() == TipoMovimentacaoEstoque.DEVOLUCAO
                        || m.getTipo() == TipoMovimentacaoEstoque.CANCELAMENTO_VENDA) {
                    variacaoPosterior = variacaoPosterior.add(valorMov);
                } else if (deltaQtd < 0 || m.getTipo() == TipoMovimentacaoEstoque.SAIDA
                        || m.getTipo() == TipoMovimentacaoEstoque.TROCA_SAIDA) {
                    variacaoPosterior = variacaoPosterior.subtract(valorMov);
                }
            }
        }
        return scale2(estoqueAtual.subtract(variacaoPosterior).max(BigDecimal.ZERO));
    }

    private record ApuracaoPrazoRecebiveis(
            BigDecimal saldoInicial,
            BigDecimal saldoFinal,
            BigDecimal vendasPrazoPeriodo) {
    }

    private ApuracaoPrazoRecebiveis apurarRecebiveisEVendasPrazo(
            List<Long> filiaisEscopo, LocalDate inicio, LocalDate fim, DadosOperacionaisEscopo dadosEscopo) {
        BigDecimal saldoIni = BigDecimal.ZERO;
        BigDecimal saldoFim = BigDecimal.ZERO;
        BigDecimal vendasPrazo = BigDecimal.ZERO;

        for (Long fId : filiaisEscopo) {
            for (ContaReceber cr : dadosEscopo.contasReceberPorFilial().getOrDefault(fId, List.of())) {
                if (cr.getStatus() == FinanceiroStatus.CANCELADO || cr.getStatus() == FinanceiroStatus.DEVOLVIDO) {
                    continue;
                }
                LocalDate dataEmissao = cr.getDataCompetencia() != null ? cr.getDataCompetencia()
                        : (cr.getDataEmissao() != null ? cr.getDataEmissao()
                                : (cr.getCriadoEm() != null ? cr.getCriadoEm().toLocalDate() : cr.getVencimento()));
                LocalDate dataBaixa = cr.getPagoEm() != null ? cr.getPagoEm().toLocalDate() : null;
                BigDecimal valorTitulo = nvl(cr.getValorBruto()).subtract(nvl(cr.getDesconto())).max(nvl(cr.getValorLiquido()));

                boolean isVendaPrazo = (cr.getTotalParcelas() != null && cr.getTotalParcelas() > 1)
                        || (cr.getModalidadeRecebivel() != null && cr.getModalidadeRecebivel() != ModalidadeRecebivel.DIRETO)
                        || (dataEmissao != null && cr.getVencimento() != null && cr.getVencimento().isAfter(dataEmissao))
                        || cr.getStatus() != FinanceiroStatus.PAGO
                        || (dataBaixa != null && dataEmissao != null && dataBaixa.isAfter(dataEmissao));

                if (!isVendaPrazo) {
                    continue;
                }

                if (dataEmissao != null && !dataEmissao.isBefore(inicio) && !dataEmissao.isAfter(fim)) {
                    vendasPrazo = vendasPrazo.add(valorTitulo);
                }

                // Estava em aberto imediatamente antes do inicio do periodo (sem bitributar emissao do dia 'inicio')?
                if (dataEmissao != null && dataEmissao.isBefore(inicio)
                        && (dataBaixa == null || !dataBaixa.isBefore(inicio))) {
                    saldoIni = saldoIni.add(valorTitulo);
                }
                // Estava em aberto no fim do periodo?
                if (dataEmissao != null && !dataEmissao.isAfter(fim)
                        && (dataBaixa == null || dataBaixa.isAfter(fim))) {
                    BigDecimal abertoFim = cr.getStatus() == FinanceiroStatus.PARCIAL && nvl(cr.getSaldo()).compareTo(BigDecimal.ZERO) > 0
                            ? cr.getSaldo()
                            : valorTitulo;
                    saldoFim = saldoFim.add(abertoFim);
                }
            }
        }
        return new ApuracaoPrazoRecebiveis(scale2(saldoIni), scale2(saldoFim), scale2(vendasPrazo));
    }

    private record ApuracaoPrazoFornecedores(
            BigDecimal saldoInicial,
            BigDecimal saldoFinal,
            BigDecimal comprasPrazoPeriodo) {
    }

    private ApuracaoPrazoFornecedores apurarFornecedoresEComprasPrazo(
            List<Long> filiaisEscopo, LocalDate inicio, LocalDate fim, DadosOperacionaisEscopo dadosEscopo) {
        BigDecimal saldoIni = BigDecimal.ZERO;
        BigDecimal saldoFim = BigDecimal.ZERO;
        BigDecimal comprasNfs = BigDecimal.ZERO;
        BigDecimal comprasContasPagarFornecedor = BigDecimal.ZERO;

        for (Long fId : filiaisEscopo) {
            for (NotaFiscalEntrada nf : dadosEscopo.notasFiscaisEntradaPorFilial().getOrDefault(fId, List.of())) {
                LocalDate comp = nf.getDataCompetencia() != null ? nf.getDataCompetencia()
                        : (nf.getDataEntrada() != null ? nf.getDataEntrada().toLocalDate() : nf.getDataEmissao().toLocalDate());
                if (comp != null && !comp.isBefore(inicio) && !comp.isAfter(fim)) {
                    comprasNfs = comprasNfs.add(nvl(nf.getValorTotal()));
                }
            }
            for (ContaPagar cp : dadosEscopo.contasPagarPorFilial().getOrDefault(fId, List.of())) {
                if (cp.getStatus() == FinanceiroStatus.CANCELADO || cp.getStatus() == FinanceiroStatus.DEVOLVIDO) {
                    continue;
                }
                boolean isFornecedorOuCompra = cp.getFornecedorId() != null
                        || (cp.getDescricao() != null && (cp.getDescricao().toLowerCase(Locale.ROOT).contains("fornecedor")
                                || cp.getDescricao().toLowerCase(Locale.ROOT).contains("compra")
                                || cp.getDescricao().toLowerCase(Locale.ROOT).contains("mercadoria")
                                || cp.getDescricao().toLowerCase(Locale.ROOT).contains("nf")));
                if (!isFornecedorOuCompra) {
                    continue;
                }
                LocalDate dataEmissao = cp.getDataCompetencia() != null ? cp.getDataCompetencia()
                        : (cp.getDataEmissao() != null ? cp.getDataEmissao()
                                : (cp.getCriadoEm() != null ? cp.getCriadoEm().toLocalDate() : cp.getVencimento()));
                LocalDate dataPago = cp.getPagoEm() != null ? cp.getPagoEm().toLocalDate() : null;
                BigDecimal valor = nvl(cp.getValor());

                if (dataEmissao != null && !dataEmissao.isBefore(inicio) && !dataEmissao.isAfter(fim)) {
                    comprasContasPagarFornecedor = comprasContasPagarFornecedor.add(valor);
                }
                if (dataEmissao != null && dataEmissao.isBefore(inicio)
                        && (dataPago == null || !dataPago.isBefore(inicio))) {
                    saldoIni = saldoIni.add(valor);
                }
                if (dataEmissao != null && !dataEmissao.isAfter(fim)
                        && (dataPago == null || dataPago.isAfter(fim))) {
                    BigDecimal abertoFim = cp.getStatus() == FinanceiroStatus.PARCIAL && nvl(cp.getSaldo()).compareTo(BigDecimal.ZERO) > 0
                            ? cp.getSaldo()
                            : valor;
                    saldoFim = saldoFim.add(abertoFim);
                }
            }
        }
        // Usa o maior entre o total de NFs de entrada e os titulos de fornecedores emitidos na competencia (evitando duplicar quando a NF gera ContaPagar)
        BigDecimal comprasPrazo = comprasNfs.max(comprasContasPagarFornecedor);
        return new ApuracaoPrazoFornecedores(scale2(saldoIni), scale2(saldoFim), scale2(comprasPrazo));
    }

    private List<PrazosCiclosEvolucaoPontoDTO> construirEvolucaoPrazosCiclos(
            YearMonth fimYm,
            Long empresaId,
            Long currentFilialId,
            List<Long> filiaisEscopo,
            Long filialAlvo,
            boolean consolidar,
            BigDecimal estoqueAtual,
            DadosOperacionaisEscopo dadosEscopo,
            DreGerencialResponseDTO dreAtualMesCheio) {
        List<PrazosCiclosEvolucaoPontoDTO> serie = new ArrayList<>();
        for (int i = 2; i >= 0; i--) {
            YearMonth cursor = fimYm.minusMonths(i);
            LocalDate ini = cursor.atDay(1);
            LocalDate fim = cursor.atEndOfMonth();
            BigDecimal dias = BigDecimal.valueOf(cursor.lengthOfMonth());

            DreGerencialResponseDTO dreMes;
            if (i == 0 && dreAtualMesCheio != null) {
                dreMes = dreAtualMesCheio;
            } else {
                DreGerencialService.FiltroDreRequest fDre = new DreGerencialService.FiltroDreRequest(
                        "MENSAL", cursor.toString(), cursor.getYear(), null, null, ini, fim, false, null, null, null, null, filialAlvo, consolidar);
                dreMes = dreGerencialService.apurarDre(fDre, empresaId, currentFilialId);
            }
            BigDecimal cmvMes = nvl(dreMes.indicadores().cmv());

            BigDecimal estFim = apurarEstoqueEmData(filiaisEscopo, fim, estoqueAtual, dadosEscopo);
            BigDecimal estIni = apurarEstoqueEmData(filiaisEscopo, ini.minusDays(1), estoqueAtual, dadosEscopo);
            BigDecimal estMed = estIni.add(estFim).divide(DOIS, MC);
            BigDecimal pme = cmvMes.compareTo(BigDecimal.ZERO) > 0 ? scale2(estMed.divide(cmvMes, MC).multiply(dias, MC)) : null;

            ApuracaoPrazoRecebiveis rec = apurarRecebiveisEVendasPrazo(filiaisEscopo, ini, fim, dadosEscopo);
            BigDecimal crMed = rec.saldoInicial().add(rec.saldoFinal()).divide(DOIS, MC);
            BigDecimal pmr = rec.vendasPrazoPeriodo().compareTo(BigDecimal.ZERO) > 0
                    ? scale2(crMed.divide(rec.vendasPrazoPeriodo(), MC).multiply(dias, MC))
                    : (crMed.compareTo(BigDecimal.ZERO) == 0 && nvl(dreMes.indicadores().receitaBruta()).compareTo(BigDecimal.ZERO) > 0 ? ZERO : null);

            ApuracaoPrazoFornecedores forn = apurarFornecedoresEComprasPrazo(filiaisEscopo, ini, fim, dadosEscopo);
            BigDecimal fornMed = forn.saldoInicial().add(forn.saldoFinal()).divide(DOIS, MC);
            BigDecimal pmp = forn.comprasPrazoPeriodo().compareTo(BigDecimal.ZERO) > 0
                    ? scale2(fornMed.divide(forn.comprasPrazoPeriodo(), MC).multiply(dias, MC))
                    : null;

            BigDecimal co = (pme != null && pmr != null) ? scale2(pme.add(pmr)) : null;
            BigDecimal cf = (co != null && pmp != null) ? scale2(co.subtract(pmp)) : null;

            serie.add(new PrazosCiclosEvolucaoPontoDTO(cursor.toString(), pme, pmr, pmp, co, cf));
        }
        return serie;
    }

    private BigDecimal calcularPaybackSimples(List<BigDecimal> fluxos) {
        if (fluxos == null || fluxos.size() < 2 || fluxos.get(0).compareTo(BigDecimal.ZERO) >= 0) {
            return null;
        }
        BigDecimal acumulado = fluxos.get(0);
        for (int t = 1; t < fluxos.size(); t++) {
            BigDecimal fc = nvl(fluxos.get(t));
            BigDecimal anterior = acumulado;
            acumulado = acumulado.add(fc);
            if (acumulado.compareTo(BigDecimal.ZERO) >= 0 && fc.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal fracao = anterior.abs().divide(fc, MC);
                return scale2(BigDecimal.valueOf(t - 1L).add(fracao));
            }
        }
        return null;
    }

    private BigDecimal calcularPaybackDescontado(List<BigDecimal> fluxos, BigDecimal taxaDecimal) {
        if (fluxos == null || fluxos.size() < 2 || fluxos.get(0).compareTo(BigDecimal.ZERO) >= 0) {
            return null;
        }
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxaDecimal.max(BigDecimal.ZERO));
        BigDecimal fator = BigDecimal.ONE;
        BigDecimal acumulado = fluxos.get(0);
        for (int t = 1; t < fluxos.size(); t++) {
            fator = fator.multiply(umMaisTaxa, MC);
            BigDecimal fcDescontado = nvl(fluxos.get(t)).divide(fator, MC);
            BigDecimal anterior = acumulado;
            acumulado = acumulado.add(fcDescontado);
            if (acumulado.compareTo(BigDecimal.ZERO) >= 0 && fcDescontado.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal fracao = anterior.abs().divide(fcDescontado, MC);
                return scale2(BigDecimal.valueOf(t - 1L).add(fracao));
            }
        }
        return null;
    }

    private BigDecimal estimarNcgCompetenciaAnterior(YearMonth ymAnt, Long empresaId, List<Long> filiaisEscopo) {
        BigDecimal ncgBalanco = BigDecimal.ZERO;
        boolean achou = false;
        for (Long fId : filiaisEscopo) {
            Optional<BalancoPatrimonialGerencial> bOpt = balancoRepository
                    .findByEmpresaIdAndFilialIdAndCompetenciaAnoMes(empresaId, fId, ymAnt.toString());
            if (bOpt.isPresent()) {
                achou = true;
                BalancoPatrimonialGerencial b = bOpt.get();
                BigDecimal aco = nvl(b.getEstoquesOperacionais())
                        .add(nvl(b.getContasReceberOperacionais()))
                        .add(nvl(b.getAdiantamentosOperacionais()))
                        .add(nvl(b.getDespesasAntecipadas()))
                        .add(nvl(b.getOutrosAtivosCirculantes()));
                BigDecimal pco = nvl(b.getFornecedoresOperacionaisCp())
                        .add(nvl(b.getObrigacoesTributariasTrabalhistasCp()))
                        .add(nvl(b.getOutrasObrigacoesOperacionaisCp()));
                ncgBalanco = ncgBalanco.add(aco.subtract(pco));
            }
        }
        return achou ? scale2(ncgBalanco) : null;
    }

    private Set<Long> identificarCrComTaxaContabilizadaDre(
            List<Long> filiaisEscopo, LocalDate inicio, LocalDate fim, DadosOperacionaisEscopo dadosEscopo) {
        Set<Long> ids = new HashSet<>();
        for (Long fId : filiaisEscopo) {
            for (Despesa d : dadosEscopo.despesasPorFilial().getOrDefault(fId, List.of())) {
                if (d.getStatus() == 0 || d.getDescricao() == null || !d.getDescricao().startsWith(TAG_TAXA_DEDUZIDA_ORIGEM)) {
                    continue;
                }
                LocalDate comp = d.getDataCompetencia() != null ? d.getDataCompetencia() : d.getDataVencimento();
                if (comp != null && !comp.isBefore(inicio) && !comp.isAfter(fim)) {
                    int start = TAG_TAXA_DEDUZIDA_ORIGEM.length();
                    int endColon = d.getDescricao().indexOf(':', start);
                    int endBracket = d.getDescricao().indexOf(']', start);
                    int end = (endColon > start && endColon < endBracket) ? endColon : endBracket;
                    if (end > start) {
                        try {
                            ids.add(Long.parseLong(d.getDescricao().substring(start, end).trim()));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }
        return ids;
    }

    private List<Venda> carregarVendasFinalizadasPeriodo(
            Long empresaId, List<Long> filiaisEscopo, LocalDate inicio, LocalDate fim) {
        LocalDateTime iniTime = inicio.atStartOfDay();
        LocalDateTime fimTime = fim.atTime(LocalTime.MAX);
        List<Venda> resultado = new ArrayList<>();
        for (Long fId : filiaisEscopo) {
            Specification<Venda> spec = (root, query, cb) -> cb.and(
                    cb.equal(root.get("empresaId"), empresaId),
                    cb.equal(root.get("filialId"), fId),
                    cb.greaterThanOrEqualTo(root.get("dataVenda"), iniTime),
                    cb.lessThanOrEqualTo(root.get("dataVenda"), fimTime));
            for (Venda v : vendaRepository.findAll(spec)) {
                if (!STATUS_VENDA_FINALIZADA.contains(v.getStatusOperacional())) {
                    continue;
                }
                if (v.getItens() != null) {
                    Hibernate.initialize(v.getItens());
                    for (ProdutoVenda pv : v.getItens()) {
                        if (pv.getProduto() != null) {
                            Hibernate.initialize(pv.getProduto());
                            if (pv.getProduto().getCategoria() != null) {
                                Hibernate.initialize(pv.getProduto().getCategoria());
                            }
                        }
                    }
                }
                resultado.add(v);
            }
        }
        return resultado;
    }

    private record DevolucaoAcumuladaItem(int quantidade, BigDecimal valorBruto) {
    }

    private Map<String, DevolucaoAcumuladaItem> carregarDevolucoesPeriodo(
            Long empresaId, List<Long> filiaisEscopo, LocalDate inicio, LocalDate fim) {
        LocalDateTime iniTime = inicio.atStartOfDay();
        LocalDateTime fimTime = fim.atTime(LocalTime.MAX);
        Map<String, DevolucaoAcumuladaItem> mapa = new HashMap<>();
        for (Long fId : filiaisEscopo) {
            for (VendaDevolucao dev : vendaDevolucaoRepository.findByEmpresaIdAndFilialIdAndCriadoEmBetweenOrderByCriadoEmDesc(empresaId, fId, iniTime, fimTime)) {
                if (dev.getTipo() == TipoDevolucao.TROCA || dev.getItens() == null) {
                    continue;
                }
                for (VendaDevolucaoItem item : dev.getItens()) {
                    if (dev.getVendaOriginalId() == null || item.getProdutoOriginalId() == null) {
                        continue;
                    }
                    String chave = dev.getVendaOriginalId() + ":" + item.getProdutoOriginalId();
                    BigDecimal val = nvl(item.getValorUnitario()).multiply(BigDecimal.valueOf(item.getQuantidade()), MC);
                    mapa.merge(chave, new DevolucaoAcumuladaItem(item.getQuantidade(), val),
                            (a, b) -> new DevolucaoAcumuladaItem(a.quantidade() + b.quantidade(), a.valorBruto().add(b.valorBruto())));
                }
            }
        }
        return mapa;
    }

    private BigDecimal extrairValorLinhaDre(DreGerencialResponseDTO dre, int numeroLinha) {
        if (dre == null || dre.linhas() == null) {
            return BigDecimal.ZERO;
        }
        return dre.linhas().stream()
                .filter(l -> l.numeroLinha() == numeroLinha)
                .map(DreLinhaDTO::valor)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal somarDespesasNaLinhaPorGrupo(
            DreGerencialResponseDTO dre, int numeroLinha, GrupoDreGerencial grupo) {
        if (dre == null || dre.linhas() == null) {
            return BigDecimal.ZERO;
        }
        DreLinhaDTO linha = dre.linhas().stream()
                .filter(l -> l.numeroLinha() == numeroLinha)
                .findFirst()
                .orElse(null);
        if (linha == null || linha.contas() == null) {
            return BigDecimal.ZERO;
        }
        return somarFolhasPorGrupoRecursivo(linha.contas(), grupo);
    }

    private BigDecimal somarFolhasPorGrupoRecursivo(List<DreContaDetalheDTO> contas, GrupoDreGerencial grupo) {
        if (contas == null || contas.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (DreContaDetalheDTO c : contas) {
            if (!c.sintetica() && c.grupoDre() == grupo) {
                // Em linhas sinalizadas (+/-) da DRE (Linha 14), debitos sao retornados negativos; soma-se a magnitude
                total = total.add(nvl(c.valor()).abs());
            }
            if (c.filhas() != null && !c.filhas().isEmpty()) {
                total = total.add(somarFolhasPorGrupoRecursivo(c.filhas(), grupo));
            }
        }
        return total;
    }

    private BigDecimal extrairDepreciacaoDaDre(DreGerencialResponseDTO dre) {
        if (dre == null || dre.linhas() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (DreLinhaDTO l : dre.linhas()) {
            if (l.contas() == null) {
                continue;
            }
            for (var c : l.contas()) {
                String nome = c.nome() != null ? c.nome().toLowerCase(Locale.ROOT) : "";
                if (nome.contains("deprecia") || nome.contains("amortiza")) {
                    total = total.add(nvl(c.valor()));
                }
            }
        }
        return scale2(total);
    }

    private BigDecimal somarImpostosProduto(Produto p) {
        if (p == null) {
            return BigDecimal.ZERO;
        }
        return nvl(p.getIcms()).add(nvl(p.getPis())).add(nvl(p.getCofins())).add(nvl(p.getIpi()));
    }

    private static final class AcumuladorRentabilidade {
        private final String tipoAgrupamento;
        private final Long referenciaId;
        private final String codigo;
        private final String nome;
        private final String categoriaNome;
        private BigDecimal quantidade = BigDecimal.ZERO;
        private BigDecimal receitaBruta = BigDecimal.ZERO;
        private BigDecimal deducoes = BigDecimal.ZERO;
        private BigDecimal receitaLiquida = BigDecimal.ZERO;
        private BigDecimal cmv = BigDecimal.ZERO;
        private BigDecimal taxasCartao = BigDecimal.ZERO;
        private BigDecimal comissoesFretes = BigDecimal.ZERO;
        private BigDecimal outrasVariaveis = BigDecimal.ZERO;

        private AcumuladorRentabilidade(String tipoAgrupamento, Long referenciaId, String codigo, String nome, String categoriaNome) {
            this.tipoAgrupamento = tipoAgrupamento;
            this.referenciaId = referenciaId;
            this.codigo = codigo;
            this.nome = nome;
            this.categoriaNome = categoriaNome;
        }

        private void acumular(
                BigDecimal qtd,
                BigDecimal bruto,
                BigDecimal ded,
                BigDecimal liq,
                BigDecimal cmvItem,
                BigDecimal taxaCartao,
                BigDecimal comissaoFrete,
                BigDecimal outraVar) {
            this.quantidade = this.quantidade.add(qtd);
            this.receitaBruta = this.receitaBruta.add(bruto);
            this.deducoes = this.deducoes.add(ded);
            this.receitaLiquida = this.receitaLiquida.add(liq);
            this.cmv = this.cmv.add(cmvItem);
            this.taxasCartao = this.taxasCartao.add(taxaCartao);
            this.comissoesFretes = this.comissoesFretes.add(comissaoFrete);
            this.outrasVariaveis = this.outrasVariaveis.add(outraVar);
        }

        private MargemContribuicaoItemDTO toDto(
                BigDecimal receitaLiquidaGlobal,
                BigDecimal mcTotalGlobal,
                BigDecimal metaMcPct) {
            BigDecimal recLiq = scale2Static(receitaLiquida);
            BigDecimal varTotal = scale2Static(cmv.add(taxasCartao).add(comissoesFretes).add(outrasVariaveis));
            BigDecimal mcVal = scale2Static(recLiq.subtract(varTotal));
            BigDecimal mcPct = recLiq.compareTo(BigDecimal.ZERO) > 0
                    ? scale2Static(mcVal.divide(recLiq, MC).multiply(CEM, MC))
                    : ZERO;
            BigDecimal mcUnit = quantidade.compareTo(BigDecimal.ZERO) > 0
                    ? scale2Static(mcVal.divide(quantidade, MC))
                    : ZERO;
            BigDecimal partFat = receitaLiquidaGlobal.compareTo(BigDecimal.ZERO) > 0
                    ? scale2Static(recLiq.divide(receitaLiquidaGlobal, MC).multiply(CEM, MC))
                    : ZERO;
            BigDecimal partMc = mcTotalGlobal.compareTo(BigDecimal.ZERO) > 0
                    ? scale2Static(mcVal.divide(mcTotalGlobal, MC).multiply(CEM, MC))
                    : ZERO;

            String classif = classificarItemRentabilidade(recLiq, mcVal, mcPct, partFat, metaMcPct);
            String rec = gerarRecomendacaoRentabilidade(nome, classif, mcPct, metaMcPct);

            return new MargemContribuicaoItemDTO(
                    tipoAgrupamento,
                    referenciaId,
                    codigo,
                    nome,
                    categoriaNome,
                    scale2Static(quantidade),
                    scale2Static(receitaBruta),
                    scale2Static(deducoes),
                    recLiq,
                    scale2Static(cmv),
                    scale2Static(taxasCartao),
                    scale2Static(comissoesFretes),
                    scale2Static(outrasVariaveis),
                    varTotal,
                    mcVal,
                    mcPct,
                    mcUnit,
                    partFat,
                    partMc,
                    classif,
                    rec);
        }

        private static BigDecimal scale2Static(BigDecimal v) {
            return v == null ? ZERO : v.setScale(2, RoundingMode.HALF_UP);
        }
    }

    private static String classificarItemRentabilidade(
            BigDecimal recLiq,
            BigDecimal mcVal,
            BigDecimal mcPct,
            BigDecimal partFat,
            BigDecimal metaMcPct) {
        if (recLiq.compareTo(BigDecimal.ZERO) <= 0) {
            return "SEM_MOVIMENTACAO";
        }
        if (mcVal.compareTo(BigDecimal.ZERO) < 0 || mcPct.compareTo(new BigDecimal("5.00")) < 0) {
            return "MARGEM_CRITICA_OU_NEGATIVA";
        }
        boolean altoVolume = partFat.compareTo(new BigDecimal("20.00")) >= 0;
        boolean boaMargem = mcPct.compareTo(metaMcPct) >= 0;
        if (altoVolume && !boaMargem) {
            return "VOLUME_ALTO_BAIXA_MARGEM";
        }
        if (altoVolume && boaMargem) {
            return "ESTRELA_ALTA_CONTRIBUICAO";
        }
        return boaMargem ? "MARGEM_SAUDAVEL_BAIXO_VOLUME" : "BAIXO_VOLUME_E_BAIXA_MARGEM";
    }

    private static String gerarRecomendacaoRentabilidade(
            String nome, String classif, BigDecimal mcPct, BigDecimal metaMcPct) {
        return switch (classif) {
            case "MARGEM_CRITICA_OU_NEGATIVA" ->
                    "Urgente: revisar preco de venda e custo de reposicao de '" + nome + "' (MC atual: " + mcPct + "%).";
            case "VOLUME_ALTO_BAIXA_MARGEM" ->
                    "Atencao: '" + nome + "' possui bom giro, mas margem de contribuicao (" + mcPct + "%) abaixo da meta (" + metaMcPct.setScale(2, RoundingMode.HALF_UP) + "%). Pequeno ajuste de preco ou custo gerara forte ganho no caixa.";
            case "BAIXO_VOLUME_E_BAIXA_MARGEM" ->
                    "Monitorar: '" + nome + "' possui baixo giro e margem (" + mcPct + "%) abaixo da meta (" + metaMcPct.setScale(2, RoundingMode.HALF_UP) + "%). Avaliar reposicionamento de preco, substituicao de fornecedor ou descontinuacao do mix.";
            case "ESTRELA_ALTA_CONTRIBUICAO" ->
                    "Destaque positivo: '" + nome + "' combina volume relevante e margem saudavel (" + mcPct + "%). Garanta disponibilidade de estoque.";
            case "MARGEM_SAUDAVEL_BAIXO_VOLUME" ->
                    "Oportunidade comercial: '" + nome + "' entrega excelente margem (" + mcPct + "%). Estimule a venda no PDV.";
            default -> "Sem movimentacao relevante no periodo.";
        };
    }

    private BalancoPatrimonialGerencialResponseDTO obterBalancoConsolidadoEscopo(
            YearMonth ym,
            Long empresaId,
            List<Long> filiaisEscopo,
            Long filialPadraoId,
            SaldosOperacionaisInstantaneos saldosOp,
            DadosOperacionaisEscopo dadosEscopo) {
        if (filiaisEscopo.size() == 1) {
            Long fId = filiaisEscopo.get(0);
            return balancoRepository.findByEmpresaIdAndFilialIdAndCompetenciaAnoMes(empresaId, fId, ym.toString())
                    .map(b -> toBalancoResponse(b, saldosOp))
                    .orElseGet(() -> construirBalancoNaoConfirmadoPadrao(ym, empresaId, fId, saldosOp));
        }

        Map<Long, BalancoPatrimonialGerencial> salvosPorFilial = new LinkedHashMap<>();
        for (Long fId : filiaisEscopo) {
            balancoRepository.findByEmpresaIdAndFilialIdAndCompetenciaAnoMes(empresaId, fId, ym.toString())
                    .ifPresent(b -> salvosPorFilial.put(fId, b));
        }
        if (salvosPorFilial.isEmpty()) {
            return construirBalancoNaoConfirmadoPadrao(ym, empresaId, filialPadraoId, saldosOp);
        }

        BalancoPatrimonialGerencial agregado = new BalancoPatrimonialGerencial();
        agregado.setEmpresaId(empresaId);
        agregado.setFilialId(filialPadraoId);
        agregado.setCompetenciaAnoMes(ym.toString());
        agregado.setDataReferencia(ym.atEndOfMonth());
        boolean todosConfirmados = true;
        for (Long fId : filiaisEscopo) {
            BalancoPatrimonialGerencial b = salvosPorFilial.get(fId);
            if (b != null) {
                agregado.setDisponibilidadesCaixaBancos(agregado.getDisponibilidadesCaixaBancos().add(nvl(b.getDisponibilidadesCaixaBancos())));
                agregado.setContasReceberOperacionais(agregado.getContasReceberOperacionais().add(nvl(b.getContasReceberOperacionais())));
                agregado.setEstoquesOperacionais(agregado.getEstoquesOperacionais().add(nvl(b.getEstoquesOperacionais())));
                agregado.setAdiantamentosOperacionais(agregado.getAdiantamentosOperacionais().add(nvl(b.getAdiantamentosOperacionais())));
                agregado.setDespesasAntecipadas(agregado.getDespesasAntecipadas().add(nvl(b.getDespesasAntecipadas())));
                agregado.setOutrosAtivosCirculantes(agregado.getOutrosAtivosCirculantes().add(nvl(b.getOutrosAtivosCirculantes())));
                agregado.setRealizavelLongoPrazo(agregado.getRealizavelLongoPrazo().add(nvl(b.getRealizavelLongoPrazo())));
                agregado.setInvestimentosLongoPrazo(agregado.getInvestimentosLongoPrazo().add(nvl(b.getInvestimentosLongoPrazo())));
                agregado.setImobilizadoBruto(agregado.getImobilizadoBruto().add(nvl(b.getImobilizadoBruto())));
                agregado.setDepreciacaoAcumulada(agregado.getDepreciacaoAcumulada().add(nvl(b.getDepreciacaoAcumulada())));
                agregado.setIntangivel(agregado.getIntangivel().add(nvl(b.getIntangivel())));
                agregado.setFornecedoresOperacionaisCp(agregado.getFornecedoresOperacionaisCp().add(nvl(b.getFornecedoresOperacionaisCp())));
                agregado.setObrigacoesTributariasTrabalhistasCp(agregado.getObrigacoesTributariasTrabalhistasCp().add(nvl(b.getObrigacoesTributariasTrabalhistasCp())));
                agregado.setOutrasObrigacoesOperacionaisCp(agregado.getOutrasObrigacoesOperacionaisCp().add(nvl(b.getOutrasObrigacoesOperacionaisCp())));
                agregado.setEmprestimosFinanciamentosCp(agregado.getEmprestimosFinanciamentosCp().add(nvl(b.getEmprestimosFinanciamentosCp())));
                agregado.setEmprestimosFinanciamentosLp(agregado.getEmprestimosFinanciamentosLp().add(nvl(b.getEmprestimosFinanciamentosLp())));
                agregado.setOutrasObrigacoesLp(agregado.getOutrasObrigacoesLp().add(nvl(b.getOutrasObrigacoesLp())));
                agregado.setCapitalSocial(agregado.getCapitalSocial().add(nvl(b.getCapitalSocial())));
                agregado.setReservasCapitalLucros(agregado.getReservasCapitalLucros().add(nvl(b.getReservasCapitalLucros())));
                agregado.setLucrosPrejuizosAcumulados(agregado.getLucrosPrejuizosAcumulados().add(nvl(b.getLucrosPrejuizosAcumulados())));
                agregado.setDepreciacaoAmortizacaoPeriodo(agregado.getDepreciacaoAmortizacaoPeriodo().add(nvl(b.getDepreciacaoAmortizacaoPeriodo())));
                agregado.setAmortizacaoPrincipalDividaPeriodo(agregado.getAmortizacaoPrincipalDividaPeriodo().add(nvl(b.getAmortizacaoPrincipalDividaPeriodo())));
                todosConfirmados = todosConfirmados && b.isConfirmadoGestor();
            } else {
                SaldosOperacionaisInstantaneos opFilial = apurarSaldosOperacionaisFilial(dadosEscopo, fId);
                agregado.setDisponibilidadesCaixaBancos(agregado.getDisponibilidadesCaixaBancos().add(opFilial.disponibilidades()));
                agregado.setContasReceberOperacionais(agregado.getContasReceberOperacionais().add(opFilial.contasReceberAbertas()));
                agregado.setEstoquesOperacionais(agregado.getEstoquesOperacionais().add(opFilial.estoqueAtualCusto()));
                agregado.setFornecedoresOperacionaisCp(agregado.getFornecedoresOperacionaisCp().add(opFilial.contasPagarAbertas()));
                todosConfirmados = false;
            }
        }
        agregado.setConfirmadoGestor(todosConfirmados);
        return toBalancoResponse(agregado, saldosOp);
    }

    private BalancoPatrimonialGerencialResponseDTO construirBalancoNaoConfirmadoPadrao(
            YearMonth ym, Long empresaId, Long filialId, SaldosOperacionaisInstantaneos op) {
        BalancoPatrimonialGerencial b = new BalancoPatrimonialGerencial();
        b.setEmpresaId(empresaId);
        b.setFilialId(filialId);
        b.setCompetenciaAnoMes(ym.toString());
        b.setDataReferencia(ym.atEndOfMonth());
        b.setDisponibilidadesCaixaBancos(op.disponibilidades());
        b.setContasReceberOperacionais(op.contasReceberAbertas());
        b.setEstoquesOperacionais(op.estoqueAtualCusto());
        b.setFornecedoresOperacionaisCp(op.contasPagarAbertas());
        b.setSincronizadoOperacional(true);
        b.setConfirmadoGestor(false);
        return toBalancoResponse(b, op);
    }

    private BalancoPatrimonialGerencialResponseDTO toBalancoResponse(
            BalancoPatrimonialGerencial b, SaldosOperacionaisInstantaneos op) {
        BigDecimal disp = scale2(nvl(b.getDisponibilidadesCaixaBancos()));
        BigDecimal cr = scale2(nvl(b.getContasReceberOperacionais()));
        BigDecimal est = scale2(nvl(b.getEstoquesOperacionais()));
        BigDecimal adiant = scale2(nvl(b.getAdiantamentosOperacionais()));
        BigDecimal despAnt = scale2(nvl(b.getDespesasAntecipadas()));
        BigDecimal outrosAc = scale2(nvl(b.getOutrosAtivosCirculantes()));
        BigDecimal acTotal = scale2(disp.add(cr).add(est).add(adiant).add(despAnt).add(outrosAc));

        BigDecimal rlp = scale2(nvl(b.getRealizavelLongoPrazo()));
        BigDecimal invLp = scale2(nvl(b.getInvestimentosLongoPrazo()));
        BigDecimal imobBruto = scale2(nvl(b.getImobilizadoBruto()));
        BigDecimal depAcum = scale2(nvl(b.getDepreciacaoAcumulada()));
        BigDecimal imobLiq = scale2(imobBruto.subtract(depAcum).max(BigDecimal.ZERO));
        BigDecimal intang = scale2(nvl(b.getIntangivel()));
        BigDecimal ancTotal = scale2(rlp.add(invLp).add(imobLiq).add(intang));
        BigDecimal ativoTotal = scale2(acTotal.add(ancTotal));

        BigDecimal fornCp = scale2(nvl(b.getFornecedoresOperacionaisCp()));
        BigDecimal obrigCp = scale2(nvl(b.getObrigacoesTributariasTrabalhistasCp()));
        BigDecimal outrasCp = scale2(nvl(b.getOutrasObrigacoesOperacionaisCp()));
        BigDecimal empCp = scale2(nvl(b.getEmprestimosFinanciamentosCp()));
        BigDecimal pcTotal = scale2(fornCp.add(obrigCp).add(outrasCp).add(empCp));

        BigDecimal empLp = scale2(nvl(b.getEmprestimosFinanciamentosLp()));
        BigDecimal outrasLp = scale2(nvl(b.getOutrasObrigacoesLp()));
        BigDecimal pncTotal = scale2(empLp.add(outrasLp));
        BigDecimal passivoExigivel = scale2(pcTotal.add(pncTotal));

        BigDecimal capSocial = scale2(nvl(b.getCapitalSocial()));
        BigDecimal reservas = scale2(nvl(b.getReservasCapitalLucros()));
        BigDecimal lucrosAcum = scale2(nvl(b.getLucrosPrejuizosAcumulados()));
        BigDecimal plTotal = scale2(capSocial.add(reservas).add(lucrosAcum));

        BigDecimal passivoMaisPl = scale2(passivoExigivel.add(plTotal));
        BigDecimal difEquilibrio = scale2(ativoTotal.subtract(passivoMaisPl));
        boolean consistente = b.isConfirmadoGestor() && difEquilibrio.abs().compareTo(new BigDecimal("0.05")) <= 0;

        return new BalancoPatrimonialGerencialResponseDTO(
                b.getIdBalancoPatrimonial(),
                b.getEmpresaId(),
                b.getFilialId(),
                b.getCompetenciaAnoMes(),
                b.getDataReferencia(),
                disp,
                cr,
                est,
                adiant,
                despAnt,
                outrosAc,
                acTotal,
                rlp,
                invLp,
                imobBruto,
                depAcum,
                imobLiq,
                intang,
                ancTotal,
                ativoTotal,
                fornCp,
                obrigCp,
                outrasCp,
                empCp,
                pcTotal,
                empLp,
                outrasLp,
                pncTotal,
                passivoExigivel,
                capSocial,
                reservas,
                lucrosAcum,
                plTotal,
                passivoMaisPl,
                difEquilibrio,
                scale2(nvl(b.getDepreciacaoAmortizacaoPeriodo())),
                scale2(nvl(b.getAmortizacaoPrincipalDividaPeriodo())),
                b.isSincronizadoOperacional(),
                b.isConfirmadoGestor(),
                consistente,
                b.getObservacoes(),
                b.getAtualizadoPor(),
                b.getAtualizadoEm());
    }

    private ParametroInteligenciaAdministrativa obterOuCriarParametrosEmMemoria(Long empresaId, Long filialId) {
        return parametroRepository.findByEmpresaIdAndFilialId(empresaId, filialId)
                .orElseGet(() -> {
                    ParametroInteligenciaAdministrativa p = new ParametroInteligenciaAdministrativa();
                    p.setEmpresaId(empresaId);
                    p.setFilialId(filialId);
                    return p;
                });
    }

    private ParametroInteligenciaAdministrativaDTO toParametroDto(ParametroInteligenciaAdministrativa p) {
        return new ParametroInteligenciaAdministrativaDTO(
                p.getIdParametroInteligencia(),
                p.getEmpresaId(),
                p.getFilialId(),
                p.getSegmentoNegocio(),
                scale4(p.getRetornoMinimoMensalPercentual()),
                scale2(p.getCapitalInvestidoReferencia()),
                scale4(p.getTaxaDescontoAnualPercentual()),
                scale4(p.getMargemContribuicaoMinimaPercentual()),
                scale4(p.getMargemLiquidaMinimaPercentual()),
                scale4(p.getMarkupMargemDesejadaPadrao()),
                scale4(p.getAliquotaTributosVendaPadrao()),
                scale4(p.getTaxaCartaoMediaPadrao()),
                scale4(p.getComissaoVendaPadrao()),
                scale4(p.getFreteVariavelVendaPadrao()),
                scale2(p.getCicloFinanceiroMaximoDias()),
                scale2(p.getPmeMaximoDias()),
                scale4(p.getLiquidezCorrenteMinima()),
                scale4(p.getLiquidezSecaMinima()),
                scale4(p.getLiquidezImediataMinima()),
                scale4(p.getEndividamentoGeralMaximoPercentual()),
                p.getAtualizadoPor(),
                p.getAtualizadoEm());
    }

    private YearMonth resolverCompetencia(String competencia) {
        if (competencia == null || competencia.isBlank()) {
            return YearMonth.now();
        }
        return consolidacaoService.parseCompetencia(competencia);
    }

    private List<Long> resolverFiliaisEscopo(Long empresaId, Long currentFilialId, Long filialFiltroId, boolean consolidar) {
        if (consolidar) {
            List<Filial> filiais = filialRepository.findByEmpresaIdEmpresaAndDeletedAtIsNullOrderByIdFilialAsc(empresaId);
            if (filiais.isEmpty()) {
                return List.of(currentFilialId);
            }
            return filiais.stream().map(Filial::getIdFilial).toList();
        }
        Long alvo = filialFiltroId != null ? filialFiltroId : currentFilialId;
        if (!Objects.equals(alvo, currentFilialId)) {
            filialRepository.findByIdFilialAndEmpresaIdEmpresa(alvo, empresaId)
                    .orElseThrow(() -> new EntityNotFoundException("Filial solicitada nao pertence a empresa autenticada."));
        }
        return List.of(alvo);
    }

    private String obterNomeFilial(Long empresaId, Long filialId) {
        return filialRepository.findByIdFilialAndEmpresaIdEmpresa(filialId, empresaId)
                .map(Filial::getNome)
                .orElse("Filial #" + filialId);
    }

    private Map<String, BigDecimal> mapOf(Object... kv) {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            String k = String.valueOf(kv[i]);
            BigDecimal v = kv[i + 1] instanceof BigDecimal bd ? bd : null;
            m.put(k, v);
        }
        return m;
    }

    private void registrarAuditoria(
            String acao,
            String entidade,
            Long idEntidade,
            String resumoNovo,
            String justificativa,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        String login = privacyGuardService.currentUserLogin(request);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("entidade", entidade);
        meta.put("idEntidade", idEntidade);
        meta.put("resumo", resumoNovo);
        financialAuditTrailService.record(
                acao,
                entidade,
                idEntidade,
                login,
                null,
                resumoNovo,
                meta,
                empresaId,
                filialId);
        auditTrailService.registrarPrivacidade(
                acao,
                entidade,
                idEntidade,
                "Financeiro-Indicadores",
                null,
                resumoNovo,
                request,
                justificativa);
    }

    private void requireTenant(Long empresaId, Long filialId) {
        if (empresaId == null || empresaId <= 0 || filialId == null || filialId <= 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Escopo multiempresa/multifilial invalido.");
        }
    }

    private BigDecimal nonNegative(BigDecimal v, String field) {
        if (v != null && v.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O campo " + field + " nao pode ser negativo.");
        }
        return v;
    }

    private BigDecimal positive(BigDecimal v, String field) {
        if (v == null || v.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O campo " + field + " deve ser maior que zero.");
        }
        return v;
    }

    private BigDecimal calcPercent(BigDecimal parte, BigDecimal base) {
        if (parte == null || base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return parte.divide(base, MC).multiply(CEM, MC).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcHorizontalPercent(BigDecimal atual, BigDecimal anterior) {
        if (atual == null || anterior == null || anterior.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return atual.subtract(anterior).divide(anterior.abs(), MC).multiply(CEM, MC).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultedPercent(BigDecimal v) {
        return v == null ? ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal scale2(BigDecimal v) {
        return v == null ? ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale4(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : v.setScale(4, RoundingMode.HALF_UP);
    }
}
