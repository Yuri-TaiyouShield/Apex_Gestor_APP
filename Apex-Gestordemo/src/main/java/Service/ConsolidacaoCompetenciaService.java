package Service;

import DTO.ConsolidacaoCompetenciaResponseDTO;
import DTO.ResumoCompetenciaItemDTO;
import Model.CaixaMovimentacao;
import Model.CentroCusto;
import Model.ContaPagar;
import Model.ContaReceber;
import Model.Despesa;
import Model.FinanceiroStatus;
import Model.GrupoDreGerencial;
import Model.NaturezaContaGerencial;
import Model.NotaFiscalEntrada;
import Model.PlanoContasGerencial;
import Model.ProdutoVenda;
import Model.ResumoCompetenciaGerencial;
import Model.TipoContaGerencial;
import Model.TipoDevolucao;
import Model.Venda;
import Model.VendaDevolucao;
import Model.VendaDevolucaoItem;
import Model.VendaStatus;
import Repository.CaixaMovimentacaoRepository;
import Repository.CentroCustoRepository;
import Repository.ContaPagarRepository;
import Repository.ContaReceberRepository;
import Repository.DespesaRepository;
import Repository.NotaFiscalEntradaRepository;
import Repository.PlanoContasGerencialRepository;
import Repository.ResumoCompetenciaGerencialRepository;
import Repository.VendaDevolucaoRepository;
import Repository.VendaRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.hibernate.Hibernate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsolidacaoCompetenciaService {

    private static final Pattern COMPETENCIA_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
    private static final Set<VendaStatus> STATUS_VENDA_FINALIZADA = EnumSet.of(
            VendaStatus.PAGA,
            VendaStatus.FINALIZADA,
            VendaStatus.DEVOLVIDA,
            VendaStatus.TROCA_PARCIAL,
            VendaStatus.TROCA_TOTAL);

    private final ResumoCompetenciaGerencialRepository resumoRepository;
    private final PlanoContasGerencialRepository planoContasRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final VendaRepository vendaRepository;
    private final VendaDevolucaoRepository vendaDevolucaoRepository;
    private final DespesaRepository despesaRepository;
    private final ContaPagarRepository contaPagarRepository;
    private final ContaReceberRepository contaReceberRepository;
    private final CaixaMovimentacaoRepository caixaMovimentacaoRepository;
    private final NotaFiscalEntradaRepository notaFiscalEntradaRepository;
    private final FinancialAuditTrailService financialAuditTrailService;
    private final AuditTrailService auditTrailService;
    private final PrivacyGuardService privacyGuardService;

    public ConsolidacaoCompetenciaService(
            ResumoCompetenciaGerencialRepository resumoRepository,
            PlanoContasGerencialRepository planoContasRepository,
            CentroCustoRepository centroCustoRepository,
            VendaRepository vendaRepository,
            VendaDevolucaoRepository vendaDevolucaoRepository,
            DespesaRepository despesaRepository,
            ContaPagarRepository contaPagarRepository,
            ContaReceberRepository contaReceberRepository,
            CaixaMovimentacaoRepository caixaMovimentacaoRepository,
            NotaFiscalEntradaRepository notaFiscalEntradaRepository,
            FinancialAuditTrailService financialAuditTrailService,
            AuditTrailService auditTrailService,
            PrivacyGuardService privacyGuardService) {
        this.resumoRepository = resumoRepository;
        this.planoContasRepository = planoContasRepository;
        this.centroCustoRepository = centroCustoRepository;
        this.vendaRepository = vendaRepository;
        this.vendaDevolucaoRepository = vendaDevolucaoRepository;
        this.despesaRepository = despesaRepository;
        this.contaPagarRepository = contaPagarRepository;
        this.contaReceberRepository = contaReceberRepository;
        this.caixaMovimentacaoRepository = caixaMovimentacaoRepository;
        this.notaFiscalEntradaRepository = notaFiscalEntradaRepository;
        this.financialAuditTrailService = financialAuditTrailService;
        this.auditTrailService = auditTrailService;
        this.privacyGuardService = privacyGuardService;
    }

    @Transactional(readOnly = true)
    public ConsolidacaoCompetenciaResponseDTO consultar(String competencia, Long empresaId, Long filialId) {
        requireTenant(empresaId, filialId);
        YearMonth ym = parseCompetencia(competencia);
        String competenciaAnoMes = ym.toString();

        List<ResumoCompetenciaGerencial> persistidos = resumoRepository
                .findByEmpresaIdAndFilialIdAndCompetenciaAnoMesOrderByPlanoContaIdAscCentroCustoIdAsc(
                        empresaId, filialId, competenciaAnoMes);

        ConsolidacaoCalculada calculada = calcularConsolidacao(ym, empresaId, filialId);
        List<String> avisos = new ArrayList<>(calculada.avisos());

        if (persistidos.isEmpty()) {
            avisos.add("Consolidacao calculada em tempo real (sem snapshot gravado para esta competencia).");
            return construirResponse(ym, calculada,
                    calculada.toItemDtos(calculada.contasPorId(), calculada.centrosPorId()),
                    List.copyOf(avisos));
        }

        boolean staleClassified = snapshotDesatualizado(persistidos, calculada);
        if (!staleClassified && calculada.naoClassificadosPorChave().isEmpty()) {
            List<ResumoCompetenciaItemDTO> linhasPersistidas = persistidos.stream()
                    .map(r -> toItemDto(r, calculada.contasPorId(), calculada.centrosPorId()))
                    .toList();
            return construirResponse(ym, calculada, linhasPersistidas, List.copyOf(avisos));
        }

        if (staleClassified) {
            avisos.add("Snapshot anterior desatualizado para os lancamentos atuais; exibindo consolidacao ao vivo (execute Recalcular para persistir).");
        } else {
            avisos.add("Consolidacao ao vivo inclui lancamentos pendentes de classificacao nao gravados no snapshot (classifique-os para persistir no resumo).");
        }
        return construirResponse(ym, calculada,
                calculada.toItemDtos(calculada.contasPorId(), calculada.centrosPorId()),
                List.copyOf(avisos));
    }

    @Transactional
    public ConsolidacaoCompetenciaResponseDTO recalcular(
            String competencia,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        YearMonth ym = parseCompetencia(competencia);
        String competenciaAnoMes = ym.toString();

        ConsolidacaoCalculada calculada = calcularConsolidacao(ym, empresaId, filialId);

        resumoRepository.deleteByEmpresaIdAndFilialIdAndCompetenciaAnoMes(empresaId, filialId, competenciaAnoMes);
        resumoRepository.flush();

        List<ResumoCompetenciaGerencial> paraSalvar = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();
        for (AggregatedBucket bucket : calculada.bucketsOrdenados()) {
            if (bucket.planoContaId == null || bucket.planoContaId <= 0) {
                continue;
            }
            ResumoCompetenciaGerencial resumo = new ResumoCompetenciaGerencial();
            resumo.setEmpresaId(empresaId);
            resumo.setFilialId(filialId);
            resumo.setCompetenciaAnoMes(competenciaAnoMes);
            resumo.setPlanoContaId(bucket.planoContaId);
            resumo.setCentroCustoId(bucket.centroCustoId == null ? 0L : bucket.centroCustoId);
            resumo.setValorCompetencia(bucket.valorCompetencia);
            resumo.setValorRealizadoCaixa(bucket.valorRealizadoCaixa);
            resumo.setQuantidadeLancamentos(bucket.quantidadeLancamentos);
            resumo.setAtualizadoEm(agora);
            paraSalvar.add(resumo);
        }
        List<ResumoCompetenciaGerencial> salvos = paraSalvar.isEmpty()
                ? List.of()
                : resumoRepository.saveAll(paraSalvar);

        List<ResumoCompetenciaItemDTO> linhas = salvos.stream()
                .map(r -> toItemDto(r, calculada.contasPorId(), calculada.centrosPorId()))
                .toList();

        ConsolidacaoCompetenciaResponseDTO response = construirResponse(
                ym, calculada, linhas, List.copyOf(calculada.avisos()));

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("competencia", competenciaAnoMes);
        metadata.put("linhasPersistidas", salvos.size());
        metadata.put("receitasCompetencia", response.receitasCompetencia());
        metadata.put("cmvCompetencia", response.cmvCompetencia());
        metadata.put("despesasCompetencia", response.despesasCompetencia());
        metadata.put("resultadoCompetencia", response.resultadoCompetencia());
        metadata.put("saldoRealizadoCaixa", response.saldoRealizadoCaixa());
        metadata.put("lancamentosSemPlanoConta", response.lancamentosSemPlanoConta());

        String novoResumo = "comp=" + competenciaAnoMes
                + ";resultado=" + response.resultadoCompetencia()
                + ";caixa=" + response.saldoRealizadoCaixa()
                + ";pendentes=" + response.lancamentosSemPlanoConta();

        financialAuditTrailService.record(
                "COMPETENCIA_RECALCULADA",
                "ResumoCompetenciaGerencial",
                null,
                privacyGuardService.currentUserLogin(request),
                null,
                novoResumo,
                metadata,
                empresaId,
                filialId);
        auditTrailService.registrarPrivacidade(
                "COMPETENCIA_RECALCULADA",
                "ResumoCompetenciaGerencial",
                null,
                "Financeiro",
                null,
                novoResumo,
                request,
                "Recalculo gerencial da competencia " + competenciaAnoMes + ".");

        return response;
    }

    @Transactional
    public void recalcularCompetenciaInterna(
            Long empresaId,
            Long filialId,
            YearMonth ym,
            HttpServletRequest request) {
        if (empresaId == null || filialId == null || ym == null) {
            return;
        }
        recalcular(ym.toString(), empresaId, filialId, request);
    }

    private ConsolidacaoCalculada calcularConsolidacao(YearMonth ym, Long empresaId, Long filialId) {
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();
        LocalDateTime inicioTime = inicio.atStartOfDay();
        LocalDateTime fimTime = fim.atTime(LocalTime.MAX);

        Map<Long, PlanoContasGerencial> contasPorId = new HashMap<>();
        Map<String, PlanoContasGerencial> contasPorCodigo = new HashMap<>();
        for (PlanoContasGerencial conta : planoContasRepository
                .findByEmpresaIdAndFilialIdOrderByOrdemExibicaoAscCodigoAsc(empresaId, filialId)) {
            contasPorId.put(conta.getIdPlanoConta(), conta);
            contasPorCodigo.put(conta.getCodigo(), conta);
        }

        Map<Long, CentroCusto> centrosPorId = new HashMap<>();
        for (CentroCusto cc : centroCustoRepository.findByEmpresaIdAndFilialIdOrderByNomeAsc(empresaId, filialId)) {
            centrosPorId.put(cc.getIdCentroCusto(), cc);
        }

        PlanoContasGerencial contaDevolucao = contasPorCodigo.get("2.01");
        PlanoContasGerencial contaDesconto = contasPorCodigo.get("2.02");
        PlanoContasGerencial contaCmv = contasPorCodigo.get("3.01");

        Map<BucketKey, AggregatedBucket> buckets = new LinkedHashMap<>();
        Map<String, BigDecimal> naoClassificadosPorChave = new LinkedHashMap<>();
        List<String> avisos = new ArrayList<>();

        // 1. Vendas finalizadas no periodo (consulta delimitada por data no banco)
        List<Venda> vendasPeriodo = vendaRepository.findAll(
                vendaPeriodoSpec(empresaId, filialId, inicio, fim, inicioTime, fimTime));
        Map<Long, Venda> vendasPorId = new HashMap<>();
        Set<Long> vendasCompetencia = new HashSet<>();
        BigDecimal cmvBruto = BigDecimal.ZERO;
        long itensSemCusto = 0L;

        for (Venda v : vendasPeriodo) {
            vendasPorId.put(v.getIdVenda(), v);
            if (!isVendaFinalizada(v)) {
                continue;
            }
            LocalDate compDate = effectiveVendaCompetencia(v);
            if (!within(compDate, inicio, fim)) {
                continue;
            }
            vendasCompetencia.add(v.getIdVenda());
            BigDecimal valorVendaLiquidoDesconto = nvl(v.getValorTotal());
            BigDecimal descontoVenda = nvl(v.getDesconto()).max(BigDecimal.ZERO);

            Hibernate.initialize(v.getItens());
            if (v.getItens() != null) {
                for (ProdutoVenda item : v.getItens()) {
                    BigDecimal custoUnit = item.getCustoUnitario();
                    if (custoUnit == null || custoUnit.compareTo(BigDecimal.ZERO) <= 0) {
                        itensSemCusto++;
                    } else {
                        cmvBruto = cmvBruto.add(custoUnit.multiply(BigDecimal.valueOf(item.getQuantidade())));
                    }
                }
            }

            if (isValidConta(v.getPlanoContaId(), contasPorId)) {
                PlanoContasGerencial contaVenda = contasPorId.get(v.getPlanoContaId());
                long ccVenda = validCentroId(v.getCentroCustoId(), centrosPorId);
                boolean isReceitaBruta = contaVenda != null && contaVenda.getGrupoDre() == GrupoDreGerencial.RECEITA_BRUTA;
                if (isReceitaBruta && descontoVenda.compareTo(BigDecimal.ZERO) > 0 && contaDesconto != null && contaDesconto.isAtivo()) {
                    addBucket(buckets, v.getPlanoContaId(), ccVenda,
                            valorVendaLiquidoDesconto.add(descontoVenda), BigDecimal.ZERO, 1L);
                    addBucket(buckets, contaDesconto.getIdPlanoConta(), ccVenda,
                            descontoVenda, BigDecimal.ZERO, 1L);
                } else {
                    addBucket(buckets, v.getPlanoContaId(), ccVenda,
                            valorVendaLiquidoDesconto, BigDecimal.ZERO, 1L);
                }
            } else {
                naoClassificadosPorChave.putIfAbsent("VENDA:" + v.getIdVenda(), valorVendaLiquidoDesconto);
            }
        }

        // 2. Devolucoes no periodo (e lookup em lote de vendas de competencias anteriores se necessario)
        List<VendaDevolucao> devolucoes = vendaDevolucaoRepository
                .findByEmpresaIdAndFilialIdAndCriadoEmBetweenOrderByCriadoEmDesc(
                        empresaId, filialId, inicioTime, fimTime);
        Set<Long> vendasFaltantes = new HashSet<>();
        for (VendaDevolucao dev : devolucoes) {
            if (dev.getVendaOriginalId() != null && !vendasPorId.containsKey(dev.getVendaOriginalId())) {
                vendasFaltantes.add(dev.getVendaOriginalId());
            }
        }

        List<ContaReceber> contasReceberCaixa = contaReceberRepository.findAll(
                contaReceberCaixaSpec(empresaId, filialId, inicioTime, fimTime));
        for (ContaReceber cr : contasReceberCaixa) {
            if (cr.getVendaId() != null && !vendasPorId.containsKey(cr.getVendaId())) {
                vendasFaltantes.add(cr.getVendaId());
            }
        }

        List<CaixaMovimentacao> movimentacoesCaixaPeriodo = caixaMovimentacaoRepository
                .findByCaixaSessaoEmpresaIdAndCaixaSessaoFilialIdAndCriadoEmBetweenOrderByCriadoEmDesc(
                        empresaId, filialId, inicioTime, fimTime);
        for (CaixaMovimentacao mov : movimentacoesCaixaPeriodo) {
            if ("VENDA".equalsIgnoreCase(mov.getTipo()) && mov.getReferenciaId() != null
                    && !vendasPorId.containsKey(mov.getReferenciaId())) {
                vendasFaltantes.add(mov.getReferenciaId());
            }
        }
        carregarVendasPorIds(vendasFaltantes, empresaId, filialId, vendasPorId);

        BigDecimal totalDevolucoes = BigDecimal.ZERO;
        BigDecimal cmvDevolvido = BigDecimal.ZERO;
        for (VendaDevolucao dev : devolucoes) {
            if (dev.getTipo() == TipoDevolucao.TROCA) {
                continue;
            }
            BigDecimal ajuste = nvl(dev.getValorAjuste()).max(BigDecimal.ZERO);
            totalDevolucoes = totalDevolucoes.add(ajuste);
            Hibernate.initialize(dev.getItens());
            if (dev.getItens() != null) {
                Venda vendaOrigem = vendasPorId.get(dev.getVendaOriginalId());
                for (VendaDevolucaoItem item : dev.getItens()) {
                    BigDecimal custoUnit = resolverCustoDevolucao(item, vendaOrigem);
                    if (custoUnit != null && custoUnit.compareTo(BigDecimal.ZERO) > 0 && item.getQuantidade() != null) {
                        cmvDevolvido = cmvDevolvido.add(custoUnit.multiply(BigDecimal.valueOf(item.getQuantidade())));
                    }
                }
            }
            if (ajuste.compareTo(BigDecimal.ZERO) > 0 && contaDevolucao != null) {
                addBucket(buckets, contaDevolucao.getIdPlanoConta(), 0L, ajuste, BigDecimal.ZERO, 1L);
            }
        }
        if (totalDevolucoes.compareTo(BigDecimal.ZERO) > 0 && contaDevolucao == null) {
            avisos.add("Existem devolucoes no periodo sem conta gerencial 2.01 configurada.");
        }

        // 3. CMV liquido das vendas finalizadas (permite saldo negativo caso haja devolucao de venda de mes anterior)
        BigDecimal cmvLiquidoVendas = cmvBruto.subtract(cmvDevolvido);
        if (cmvLiquidoVendas.compareTo(BigDecimal.ZERO) != 0) {
            if (contaCmv != null) {
                addBucket(buckets, contaCmv.getIdPlanoConta(), 0L, cmvLiquidoVendas, BigDecimal.ZERO, 1L);
            } else {
                avisos.add("Conta gerencial 3.01 (CMV) nao encontrada; CMV apurado diretamente pelos itens vendidos.");
            }
        }
        if (itensSemCusto > 0) {
            avisos.add("Existem " + itensSemCusto
                    + " itens vendidos no periodo com custo unitario zerado ou ausente no calculo de CMV.");
        }

        // 4. Despesas (competencia + caixa realizado, consulta delimitada por data)
        BigDecimal despesasSemConta = BigDecimal.ZERO;
        BigDecimal saidasCaixa = BigDecimal.ZERO;
        long competenciaEstimadaCount = 0L;
        long competenciaPorVencimentoCount = 0L;
        for (Despesa d : despesaRepository.findAll(despesaPeriodoSpec(empresaId, filialId, inicio, fim))) {
            if (d.getStatus() == 0) {
                continue;
            }
            boolean inCompetencia = within(effectiveDespesaCompetencia(d), inicio, fim);
            boolean inCaixa = within(d.getDataPagamento(), inicio, fim);
            if (!inCompetencia && !inCaixa) {
                continue;
            }
            if (inCompetencia && d.getDataCompetencia() == null) {
                competenciaEstimadaCount++;
                if (d.getDataEmissao() == null) {
                    competenciaPorVencimentoCount++;
                }
            }
            BigDecimal valor = nvl(d.getValor());
            if (inCaixa) {
                saidasCaixa = saidasCaixa.add(valor);
            }
            if (isValidConta(d.getPlanoContaId(), contasPorId)) {
                addBucket(buckets, d.getPlanoContaId(), validCentroId(d.getCentroCustoId(), centrosPorId),
                        inCompetencia ? valor : BigDecimal.ZERO,
                        inCaixa ? valor : BigDecimal.ZERO,
                        1L);
            } else {
                if (inCompetencia) {
                    despesasSemConta = despesasSemConta.add(valor);
                }
                naoClassificadosPorChave.putIfAbsent("DESPESA:" + d.getIdDespesa(), valor);
            }
        }

        // 5. Contas a Pagar (competencia + caixa realizado, consulta delimitada por data)
        for (ContaPagar cp : contaPagarRepository.findAll(
                contaPagarPeriodoSpec(empresaId, filialId, inicio, fim, inicioTime, fimTime))) {
            if (cp.getStatus() == FinanceiroStatus.CANCELADO || cp.getStatus() == FinanceiroStatus.DEVOLVIDO) {
                continue;
            }
            boolean inCompetencia = within(effectiveContaPagarCompetencia(cp), inicio, fim);
            boolean inCaixa = cp.getStatus() == FinanceiroStatus.PAGO
                    && cp.getPagoEm() != null
                    && within(cp.getPagoEm().toLocalDate(), inicio, fim);
            if (!inCompetencia && !inCaixa) {
                continue;
            }
            if (inCompetencia && cp.getDataCompetencia() == null) {
                competenciaEstimadaCount++;
                if (cp.getDataEmissao() == null && cp.getCriadoEm() == null) {
                    competenciaPorVencimentoCount++;
                }
            }
            BigDecimal valor = nvl(cp.getValor());
            if (inCaixa) {
                saidasCaixa = saidasCaixa.add(valor);
            }
            if (isValidConta(cp.getPlanoContaId(), contasPorId)) {
                addBucket(buckets, cp.getPlanoContaId(), validCentroId(cp.getCentroCustoId(), centrosPorId),
                        inCompetencia ? valor : BigDecimal.ZERO,
                        inCaixa ? valor : BigDecimal.ZERO,
                        1L);
            } else {
                if (inCompetencia) {
                    despesasSemConta = despesasSemConta.add(valor);
                }
                naoClassificadosPorChave.putIfAbsent("CONTA_PAGAR:" + cp.getIdContaPagar(), valor);
            }
        }
        if (competenciaEstimadaCount > 0) {
            avisos.add("Existem " + competenciaEstimadaCount
                    + " lancamento(s) sem data de competencia explicita ("
                    + competenciaPorVencimentoCount
                    + " estimado(s) pela data de vencimento); recomenda-se informar a data de competencia real do fato gerador.");
        }

        // 6. Regime de caixa realizado - Entradas (ContaReceber + fallback PDV sem ContaReceber)
        BigDecimal entradasCaixa = BigDecimal.ZERO;
        Set<Long> vendasRecebidasNoCaixa = new HashSet<>();
        for (ContaReceber cr : contasReceberCaixa) {
            if (cr.getStatus() == FinanceiroStatus.CANCELADO) {
                continue;
            }
            boolean settledInMonth = cr.getPagoEm() != null
                    && within(cr.getPagoEm().toLocalDate(), inicio, fim)
                    && nvl(cr.getValorPago()).compareTo(BigDecimal.ZERO) > 0;
            if (!settledInMonth) {
                continue;
            }
            BigDecimal valorPago = nvl(cr.getValorPago());
            entradasCaixa = entradasCaixa.add(valorPago);
            vendasRecebidasNoCaixa.add(cr.getVendaId());

            if (isValidConta(cr.getPlanoContaId(), contasPorId)) {
                long ccId = validCentroId(cr.getCentroCustoId(), centrosPorId);
                long inc = jaContabilizadoNoBucket(buckets, vendasCompetencia, cr.getVendaId(), cr.getPlanoContaId(), ccId)
                        ? 0L : 1L;
                addBucket(buckets, cr.getPlanoContaId(), ccId, BigDecimal.ZERO, valorPago, inc);
            } else {
                Venda vendaLink = vendasPorId.get(cr.getVendaId());
                if (vendaLink != null && isValidConta(vendaLink.getPlanoContaId(), contasPorId)) {
                    long ccId = validCentroId(vendaLink.getCentroCustoId(), centrosPorId);
                    long inc = jaContabilizadoNoBucket(
                            buckets, vendasCompetencia, cr.getVendaId(), vendaLink.getPlanoContaId(), ccId)
                            ? 0L : 1L;
                    addBucket(buckets, vendaLink.getPlanoContaId(), ccId, BigDecimal.ZERO, valorPago, inc);
                } else if (!naoClassificadosPorChave.containsKey("VENDA:" + cr.getVendaId())) {
                    naoClassificadosPorChave.putIfAbsent("CONTA_RECEBER:" + cr.getIdContaReceber(), valorPago);
                }
            }
        }

        // Verificar existencia de ContaReceber em lote apenas para vendas candidatas a fallback
        Set<Long> vendasCandidatasFallback = new HashSet<>();
        for (CaixaMovimentacao mov : movimentacoesCaixaPeriodo) {
            if ("VENDA".equalsIgnoreCase(mov.getTipo()) && mov.getReferenciaId() != null
                    && !vendasRecebidasNoCaixa.contains(mov.getReferenciaId())) {
                vendasCandidatasFallback.add(mov.getReferenciaId());
            }
        }
        for (Venda v : vendasPeriodo) {
            if (isVendaFinalizada(v) && !vendasRecebidasNoCaixa.contains(v.getIdVenda())) {
                vendasCandidatasFallback.add(v.getIdVenda());
            }
        }
        Set<Long> vendasComContaReceber = carregarVendasComContaReceber(
                vendasCandidatasFallback, empresaId, filialId);

        for (CaixaMovimentacao mov : movimentacoesCaixaPeriodo) {
            if (!"VENDA".equalsIgnoreCase(mov.getTipo()) || mov.getCriadoEm() == null) {
                continue;
            }
            if (!within(mov.getCriadoEm().toLocalDate(), inicio, fim)) {
                continue;
            }
            Long vendaId = mov.getReferenciaId();
            if (vendaId != null && (vendasRecebidasNoCaixa.contains(vendaId)
                    || vendasComContaReceber.contains(vendaId))) {
                continue;
            }
            BigDecimal valorMov = nvl(mov.getValor());
            entradasCaixa = entradasCaixa.add(valorMov);
            if (vendaId != null) {
                vendasRecebidasNoCaixa.add(vendaId);
                Venda vendaLink = vendasPorId.get(vendaId);
                if (vendaLink != null && isValidConta(vendaLink.getPlanoContaId(), contasPorId)) {
                    long ccId = validCentroId(vendaLink.getCentroCustoId(), centrosPorId);
                    long inc = jaContabilizadoNoBucket(
                            buckets, vendasCompetencia, vendaId, vendaLink.getPlanoContaId(), ccId) ? 0L : 1L;
                    addBucket(buckets, vendaLink.getPlanoContaId(), ccId, BigDecimal.ZERO, valorMov, inc);
                } else if (!naoClassificadosPorChave.containsKey("VENDA:" + vendaId)) {
                    naoClassificadosPorChave.putIfAbsent("VENDA_CAIXA:" + vendaId, valorMov);
                }
            } else {
                naoClassificadosPorChave.putIfAbsent("VENDA_CAIXA:MOV-" + mov.getIdCaixaMovimentacao(), valorMov);
            }
        }

        for (Venda v : vendasPeriodo) {
            if (!isVendaFinalizada(v) || vendasRecebidasNoCaixa.contains(v.getIdVenda())
                    || vendasComContaReceber.contains(v.getIdVenda())) {
                continue;
            }
            LocalDateTime pagoDt = v.getFinalizadoEm() != null ? v.getFinalizadoEm() : v.getRecebidoCaixaEm();
            if (pagoDt != null && within(pagoDt.toLocalDate(), inicio, fim)) {
                BigDecimal valor = nvl(v.getValorTotal());
                entradasCaixa = entradasCaixa.add(valor);
                vendasRecebidasNoCaixa.add(v.getIdVenda());
                if (isValidConta(v.getPlanoContaId(), contasPorId)) {
                    long ccId = validCentroId(v.getCentroCustoId(), centrosPorId);
                    long inc = jaContabilizadoNoBucket(
                            buckets, vendasCompetencia, v.getIdVenda(), v.getPlanoContaId(), ccId) ? 0L : 1L;
                    addBucket(buckets, v.getPlanoContaId(), ccId, BigDecimal.ZERO, valor, inc);
                } else if (!naoClassificadosPorChave.containsKey("VENDA:" + v.getIdVenda())) {
                    naoClassificadosPorChave.putIfAbsent("VENDA_CAIXA:" + v.getIdVenda(), valor);
                }
            }
        }

        // Notas fiscais de entrada: aviso informativo sobre nao duplicacao com CMV
        List<NotaFiscalEntrada> nfsPeriodo = notaFiscalEntradaRepository
                .findByEmpresaIdAndFilialIdAndDataCompetenciaBetweenOrderByDataCompetenciaDesc(
                        empresaId, filialId, inicio, fim);
        if (!nfsPeriodo.isEmpty()) {
            avisos.add("Existem " + nfsPeriodo.size()
                    + " notas fiscais de entrada no periodo; compras de estoque nao sao lancadas como despesa para nao duplicar com o CMV.");
        }

        // Classificacao dos totais de competencia a partir dos buckets validos + nao classificados
        BigDecimal receitasCompetencia = BigDecimal.ZERO;
        BigDecimal cmvTotal = BigDecimal.ZERO;
        BigDecimal despesasCompetencia = despesasSemConta;
        boolean temInvestimento = false;

        for (AggregatedBucket bucket : buckets.values()) {
            PlanoContasGerencial conta = contasPorId.get(bucket.planoContaId);
            if (conta == null) {
                continue;
            }
            BigDecimal valComp = bucket.valorCompetencia;
            if (conta.getTipoConta() == TipoContaGerencial.CMV) {
                cmvTotal = cmvTotal.add(valComp);
            } else if (conta.getTipoConta() == TipoContaGerencial.INVESTIMENTO) {
                temInvestimento = true;
            } else if (conta.getTipoConta() == TipoContaGerencial.DEDUCAO) {
                receitasCompetencia = receitasCompetencia.subtract(valComp);
            } else if (conta.getNatureza() == NaturezaContaGerencial.CREDITO) {
                receitasCompetencia = receitasCompetencia.add(valComp);
            } else {
                despesasCompetencia = despesasCompetencia.add(valComp);
            }
        }

        // Incluir vendas nao classificadas na receita (subtraindo devolucoes se conta 2.01 nao existir)
        for (Map.Entry<String, BigDecimal> entry : naoClassificadosPorChave.entrySet()) {
            if (entry.getKey().startsWith("VENDA:")) {
                receitasCompetencia = receitasCompetencia.add(entry.getValue());
            }
        }
        if (contaDevolucao == null) {
            receitasCompetencia = receitasCompetencia.subtract(totalDevolucoes);
        }
        if (contaCmv == null) {
            cmvTotal = cmvTotal.add(cmvLiquidoVendas);
        }
        if (temInvestimento) {
            avisos.add("Lancamentos em contas do tipo INVESTIMENTO nao compoem despesas operacionais do resultado.");
        }

        long lancamentosSemPlanoConta = naoClassificadosPorChave.size();
        BigDecimal valorSemPlanoConta = naoClassificadosPorChave.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lancamentosSemPlanoConta > 0) {
            avisos.add("Existem " + lancamentosSemPlanoConta + " lancamentos (R$ " + valorSemPlanoConta
                    + ") no periodo sem classificacao no plano de contas gerencial.");
        }

        return new ConsolidacaoCalculada(
                contasPorId,
                centrosPorId,
                buckets,
                naoClassificadosPorChave,
                receitasCompetencia,
                cmvTotal,
                despesasCompetencia,
                entradasCaixa,
                saidasCaixa,
                lancamentosSemPlanoConta,
                valorSemPlanoConta,
                avisos);
    }

    private void carregarVendasPorIds(
            Set<Long> vendaIds,
            Long empresaId,
            Long filialId,
            Map<Long, Venda> vendasPorId) {
        if (vendaIds == null || vendaIds.isEmpty()) {
            return;
        }
        Specification<Venda> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("empresaId"), empresaId),
                cb.equal(root.get("filialId"), filialId),
                root.get("idVenda").in(vendaIds));
        for (Venda v : vendaRepository.findAll(spec)) {
            vendasPorId.put(v.getIdVenda(), v);
        }
    }

    private Set<Long> carregarVendasComContaReceber(Set<Long> vendaIds, Long empresaId, Long filialId) {
        if (vendaIds == null || vendaIds.isEmpty()) {
            return Set.of();
        }
        Specification<ContaReceber> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("empresaId"), empresaId),
                cb.equal(root.get("filialId"), filialId),
                root.get("vendaId").in(vendaIds));
        Set<Long> resultado = new HashSet<>();
        for (ContaReceber cr : contaReceberRepository.findAll(spec)) {
            if (cr.getVendaId() != null) {
                resultado.add(cr.getVendaId());
            }
        }
        return resultado;
    }

    private boolean jaContabilizadoNoBucket(
            Map<BucketKey, AggregatedBucket> buckets,
            Set<Long> vendasCompetencia,
            Long vendaId,
            Long planoContaId,
            long centroCustoId) {
        if (vendaId == null || !vendasCompetencia.contains(vendaId)) {
            return false;
        }
        AggregatedBucket existing = buckets.get(new BucketKey(planoContaId, centroCustoId));
        return existing != null && existing.quantidadeLancamentos > 0;
    }

    private ConsolidacaoCompetenciaResponseDTO construirResponse(
            YearMonth ym,
            ConsolidacaoCalculada calculada,
            List<ResumoCompetenciaItemDTO> linhas,
            List<String> avisos) {
        BigDecimal resultado = calculada.receitasCompetencia()
                .subtract(calculada.cmvCompetencia())
                .subtract(calculada.despesasCompetencia());
        BigDecimal saldoCaixa = calculada.entradasRealizadasCaixa()
                .subtract(calculada.saidasRealizadasCaixa());
        return new ConsolidacaoCompetenciaResponseDTO(
                ym.toString(),
                ym.atDay(1),
                ym.atEndOfMonth(),
                calculada.receitasCompetencia(),
                calculada.cmvCompetencia(),
                calculada.despesasCompetencia(),
                resultado,
                calculada.entradasRealizadasCaixa(),
                calculada.saidasRealizadasCaixa(),
                saldoCaixa,
                calculada.lancamentosSemPlanoConta(),
                calculada.valorSemPlanoConta(),
                avisos,
                linhas);
    }

    private boolean snapshotDesatualizado(
            List<ResumoCompetenciaGerencial> persistidos,
            ConsolidacaoCalculada calculada) {
        List<AggregatedBucket> liveBuckets = calculada.bucketsOrdenados();
        if (persistidos.size() != liveBuckets.size()) {
            return true;
        }
        Map<BucketKey, ResumoCompetenciaGerencial> persistidoMap = new HashMap<>();
        for (ResumoCompetenciaGerencial r : persistidos) {
            persistidoMap.put(
                    new BucketKey(r.getPlanoContaId(), r.getCentroCustoId() == null ? 0L : r.getCentroCustoId()),
                    r);
        }
        for (AggregatedBucket b : liveBuckets) {
            ResumoCompetenciaGerencial r = persistidoMap.get(new BucketKey(b.planoContaId, b.centroCustoId));
            if (r == null
                    || nvl(r.getValorCompetencia()).compareTo(b.valorCompetencia) != 0
                    || nvl(r.getValorRealizadoCaixa()).compareTo(b.valorRealizadoCaixa) != 0
                    || !Objects.equals(r.getQuantidadeLancamentos(), b.quantidadeLancamentos)) {
                return true;
            }
        }
        return false;
    }

    private ResumoCompetenciaItemDTO toItemDto(
            ResumoCompetenciaGerencial r,
            Map<Long, PlanoContasGerencial> contasPorId,
            Map<Long, CentroCusto> centrosPorId) {
        PlanoContasGerencial conta = contasPorId.get(r.getPlanoContaId());
        Long ccId = r.getCentroCustoId() != null && r.getCentroCustoId() > 0 ? r.getCentroCustoId() : null;
        CentroCusto cc = ccId == null ? null : centrosPorId.get(ccId);
        return new ResumoCompetenciaItemDTO(
                r.getPlanoContaId(),
                conta == null ? null : conta.getCodigo(),
                conta == null ? "Conta #" + r.getPlanoContaId() : conta.getNome(),
                conta == null ? null : conta.getTipoConta(),
                conta == null ? null : conta.getNatureza(),
                conta == null ? null : conta.getClassificacaoCusto(),
                conta == null ? null : conta.getGrupoDre(),
                ccId,
                cc == null ? null : cc.getCodigo(),
                cc == null ? null : cc.getNome(),
                nvl(r.getValorCompetencia()),
                nvl(r.getValorRealizadoCaixa()),
                r.getQuantidadeLancamentos() == null ? 0L : r.getQuantidadeLancamentos());
    }

    public BigDecimal resolverCustoDevolucao(VendaDevolucaoItem item, Venda vendaOrigem) {
        if (item.getCustoUnitario() != null && item.getCustoUnitario().compareTo(BigDecimal.ZERO) > 0) {
            return item.getCustoUnitario();
        }
        if (vendaOrigem != null && vendaOrigem.getItens() != null && item.getProdutoOriginalId() != null) {
            Hibernate.initialize(vendaOrigem.getItens());
            for (ProdutoVenda pv : vendaOrigem.getItens()) {
                if (pv.getProduto() != null
                        && Objects.equals(pv.getProduto().getIdProduto(), item.getProdutoOriginalId())) {
                    return pv.getCustoUnitario();
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private void addBucket(
            Map<BucketKey, AggregatedBucket> buckets,
            Long planoContaId,
            Long centroCustoId,
            BigDecimal valorCompetencia,
            BigDecimal valorCaixa,
            long quantidade) {
        if (planoContaId == null || planoContaId <= 0) {
            return;
        }
        long cc = centroCustoId == null || centroCustoId < 0 ? 0L : centroCustoId;
        BucketKey key = new BucketKey(planoContaId, cc);
        AggregatedBucket bucket = buckets.computeIfAbsent(key, ignored -> new AggregatedBucket(planoContaId, cc));
        bucket.valorCompetencia = bucket.valorCompetencia.add(nvl(valorCompetencia));
        bucket.valorRealizadoCaixa = bucket.valorRealizadoCaixa.add(nvl(valorCaixa));
        bucket.quantidadeLancamentos += quantidade;
        if (bucket.quantidadeLancamentos == 0 && (valorCompetencia.signum() != 0 || valorCaixa.signum() != 0)) {
            bucket.quantidadeLancamentos = 1L;
        }
    }

    private boolean isValidConta(Long planoContaId, Map<Long, PlanoContasGerencial> contasPorId) {
        return planoContaId != null && planoContaId > 0 && contasPorId.containsKey(planoContaId);
    }

    private long validCentroId(Long centroCustoId, Map<Long, CentroCusto> centrosPorId) {
        if (centroCustoId != null && centroCustoId > 0 && centrosPorId.containsKey(centroCustoId)) {
            return centroCustoId;
        }
        return 0L;
    }

    public boolean isVendaFinalizada(Venda venda) {
        return venda.getStatus() == 1
                && (venda.getStatusOperacional() == null
                        || STATUS_VENDA_FINALIZADA.contains(venda.getStatusOperacional()));
    }

    public LocalDate effectiveVendaCompetencia(Venda v) {
        if (v.getDataCompetencia() != null) {
            return v.getDataCompetencia();
        }
        if (v.getFinalizadoEm() != null) {
            return v.getFinalizadoEm().toLocalDate();
        }
        return v.getDataVenda() == null ? null : v.getDataVenda().toLocalDate();
    }

    public LocalDate effectiveDespesaCompetencia(Despesa d) {
        if (d.getDataCompetencia() != null) {
            return d.getDataCompetencia();
        }
        if (d.getDataEmissao() != null) {
            return d.getDataEmissao();
        }
        return d.getDataVencimento();
    }

    public LocalDate effectiveContaPagarCompetencia(ContaPagar cp) {
        if (cp.getDataCompetencia() != null) {
            return cp.getDataCompetencia();
        }
        if (cp.getDataEmissao() != null) {
            return cp.getDataEmissao();
        }
        if (cp.getCriadoEm() != null) {
            return cp.getCriadoEm().toLocalDate();
        }
        return cp.getVencimento();
    }

    public LocalDate effectiveContaReceberCompetencia(ContaReceber cr) {
        if (cr.getDataCompetencia() != null) {
            return cr.getDataCompetencia();
        }
        if (cr.getDataEmissao() != null) {
            return cr.getDataEmissao();
        }
        if (cr.getCriadoEm() != null) {
            return cr.getCriadoEm().toLocalDate();
        }
        return cr.getVencimento();
    }

    public boolean within(LocalDate date, LocalDate inicio, LocalDate fim) {
        return date != null && !date.isBefore(inicio) && !date.isAfter(fim);
    }

    public YearMonth parseCompetencia(String competencia) {
        if (competencia == null || competencia.isBlank()) {
            return YearMonth.now();
        }
        String trimmed = competencia.trim();
        if (!COMPETENCIA_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Competencia deve estar no formato YYYY-MM.");
        }
        try {
            return YearMonth.parse(trimmed);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Competencia deve estar no formato YYYY-MM.");
        }
    }

    public Specification<Venda> vendaPeriodoSpec(
            Long empresaId,
            Long filialId,
            LocalDate inicio,
            LocalDate fim,
            LocalDateTime inicioTime,
            LocalDateTime fimTime) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("empresaId"), empresaId),
                cb.equal(root.get("filialId"), filialId),
                cb.equal(root.get("status"), 1),
                cb.or(
                        cb.between(root.get("dataCompetencia"), inicio, fim),
                        cb.and(cb.isNull(root.get("dataCompetencia")),
                                cb.or(
                                        cb.between(root.get("finalizadoEm"), inicioTime, fimTime),
                                        cb.and(cb.isNull(root.get("finalizadoEm")),
                                                cb.between(root.get("dataVenda"), inicioTime, fimTime)))),
                        cb.between(root.get("finalizadoEm"), inicioTime, fimTime),
                        cb.between(root.get("recebidoCaixaEm"), inicioTime, fimTime)));
    }

    public Specification<Despesa> despesaPeriodoSpec(
            Long empresaId,
            Long filialId,
            LocalDate inicio,
            LocalDate fim) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("empresaId"), empresaId),
                cb.equal(root.get("filialId"), filialId),
                cb.notEqual(root.get("status"), 0),
                cb.or(
                        cb.between(root.get("dataCompetencia"), inicio, fim),
                        cb.and(cb.isNull(root.get("dataCompetencia")),
                                cb.between(root.get("dataEmissao"), inicio, fim)),
                        cb.and(cb.isNull(root.get("dataCompetencia")),
                                cb.isNull(root.get("dataEmissao")),
                                cb.between(root.get("dataVencimento"), inicio, fim)),
                        cb.between(root.get("dataPagamento"), inicio, fim)));
    }

    public Specification<ContaPagar> contaPagarPeriodoSpec(
            Long empresaId,
            Long filialId,
            LocalDate inicio,
            LocalDate fim,
            LocalDateTime inicioTime,
            LocalDateTime fimTime) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("empresaId"), empresaId),
                cb.equal(root.get("filialId"), filialId),
                cb.not(root.get("status").in(FinanceiroStatus.CANCELADO, FinanceiroStatus.DEVOLVIDO)),
                cb.or(
                        cb.between(root.get("dataCompetencia"), inicio, fim),
                        cb.and(cb.isNull(root.get("dataCompetencia")),
                                cb.between(root.get("dataEmissao"), inicio, fim)),
                        cb.and(cb.isNull(root.get("dataCompetencia")),
                                cb.isNull(root.get("dataEmissao")),
                                cb.between(root.get("criadoEm"), inicioTime, fimTime)),
                        cb.and(cb.isNull(root.get("dataCompetencia")),
                                cb.isNull(root.get("dataEmissao")),
                                cb.isNull(root.get("criadoEm")),
                                cb.between(root.get("vencimento"), inicio, fim)),
                        cb.between(root.get("pagoEm"), inicioTime, fimTime)));
    }

    public Specification<ContaReceber> contaReceberCaixaSpec(
            Long empresaId,
            Long filialId,
            LocalDateTime inicioTime,
            LocalDateTime fimTime) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("empresaId"), empresaId),
                cb.equal(root.get("filialId"), filialId),
                cb.notEqual(root.get("status"), FinanceiroStatus.CANCELADO),
                cb.between(root.get("pagoEm"), inicioTime, fimTime));
    }

    public Specification<ContaReceber> contaReceberAvulsaPeriodoSpec(
            Long empresaId,
            Long filialId,
            LocalDate inicio,
            LocalDate fim,
            LocalDateTime inicioTime,
            LocalDateTime fimTime) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("empresaId"), empresaId),
                cb.equal(root.get("filialId"), filialId),
                cb.isNull(root.get("vendaId")),
                cb.not(root.get("status").in(FinanceiroStatus.CANCELADO, FinanceiroStatus.DEVOLVIDO)),
                cb.or(
                        cb.between(root.get("dataCompetencia"), inicio, fim),
                        cb.and(cb.isNull(root.get("dataCompetencia")),
                                cb.between(root.get("dataEmissao"), inicio, fim)),
                        cb.and(cb.isNull(root.get("dataCompetencia")),
                                cb.isNull(root.get("dataEmissao")),
                                cb.between(root.get("criadoEm"), inicioTime, fimTime)),
                        cb.and(cb.isNull(root.get("dataCompetencia")),
                                cb.isNull(root.get("dataEmissao")),
                                cb.isNull(root.get("criadoEm")),
                                cb.between(root.get("vencimento"), inicio, fim))));
    }

    private void requireTenant(Long empresaId, Long filialId) {
        if (empresaId == null || filialId == null) {
            throw new IllegalArgumentException("Empresa e filial ativas sao obrigatorias.");
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record BucketKey(Long planoContaId, Long centroCustoId) {
    }

    private static final class AggregatedBucket {
        private final Long planoContaId;
        private final Long centroCustoId;
        private BigDecimal valorCompetencia = BigDecimal.ZERO;
        private BigDecimal valorRealizadoCaixa = BigDecimal.ZERO;
        private long quantidadeLancamentos = 0L;

        private AggregatedBucket(Long planoContaId, Long centroCustoId) {
            this.planoContaId = planoContaId;
            this.centroCustoId = centroCustoId;
        }
    }

    private record ConsolidacaoCalculada(
            Map<Long, PlanoContasGerencial> contasPorId,
            Map<Long, CentroCusto> centrosPorId,
            Map<BucketKey, AggregatedBucket> buckets,
            Map<String, BigDecimal> naoClassificadosPorChave,
            BigDecimal receitasCompetencia,
            BigDecimal cmvCompetencia,
            BigDecimal despesasCompetencia,
            BigDecimal entradasRealizadasCaixa,
            BigDecimal saidasRealizadasCaixa,
            long lancamentosSemPlanoConta,
            BigDecimal valorSemPlanoConta,
            List<String> avisos) {

        private List<AggregatedBucket> bucketsOrdenados() {
            return buckets.values().stream()
                    .sorted(Comparator.comparing((AggregatedBucket b) -> b.planoContaId)
                            .thenComparing(b -> b.centroCustoId))
                    .toList();
        }

        private List<ResumoCompetenciaItemDTO> toItemDtos(
                Map<Long, PlanoContasGerencial> contas,
                Map<Long, CentroCusto> centros) {
            return bucketsOrdenados().stream()
                    .map(b -> {
                        PlanoContasGerencial conta = contas.get(b.planoContaId);
                        Long ccId = b.centroCustoId > 0 ? b.centroCustoId : null;
                        CentroCusto cc = ccId == null ? null : centros.get(ccId);
                        return new ResumoCompetenciaItemDTO(
                                b.planoContaId,
                                conta == null ? null : conta.getCodigo(),
                                conta == null ? "Conta #" + b.planoContaId : conta.getNome(),
                                conta == null ? null : conta.getTipoConta(),
                                conta == null ? null : conta.getNatureza(),
                                conta == null ? null : conta.getClassificacaoCusto(),
                                conta == null ? null : conta.getGrupoDre(),
                                ccId,
                                cc == null ? null : cc.getCodigo(),
                                cc == null ? null : cc.getNome(),
                                b.valorCompetencia,
                                b.valorRealizadoCaixa,
                                b.quantidadeLancamentos);
                    })
                    .toList();
        }
    }
}
