package Service;

import DTO.ItemPendenteClassificacaoDTO;
import DTO.PendenciaClassificacaoResponseDTO;
import DTO.ReclassificacaoManualRequestDTO;
import DTO.RegraClassificacaoRequestDTO;
import DTO.RegraClassificacaoResponseDTO;
import DTO.ResultadoClassificacaoLoteDTO;
import Model.Categoria;
import Model.CentroCusto;
import Model.ContaPagar;
import Model.ContaReceber;
import Model.Despesa;
import Model.FinanceiroStatus;
import Model.NaturezaContaGerencial;
import Model.NotaFiscalEntrada;
import Model.OrigemClassificacaoFinanceira;
import Model.PlanoContasGerencial;
import Model.ProdutoVenda;
import Model.RegraClassificacaoFinanceira;
import Model.TipoContaGerencial;
import Model.TipoDespesa;
import Model.Venda;
import Model.VendaStatus;
import Repository.CategoriaRepository;
import Repository.CentroCustoRepository;
import Repository.ContaPagarRepository;
import Repository.ContaReceberRepository;
import Repository.DespesaRepository;
import Repository.FormaPagamentoRepository;
import Repository.FornecedorRepository;
import Repository.NotaFiscalEntradaRepository;
import Repository.PlanoContasGerencialRepository;
import Repository.RegraClassificacaoFinanceiraRepository;
import Repository.TipoDespesaRepository;
import Repository.VendaRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassificacaoFinanceiraService {

    private static final int MAX_PREVIEW_ITENS = 50;
    private static final int MAX_OBSERVACOES = 50;
    private static final Set<VendaStatus> STATUS_VENDA_FINALIZADA = EnumSet.of(
            VendaStatus.PAGA,
            VendaStatus.FINALIZADA,
            VendaStatus.DEVOLVIDA,
            VendaStatus.TROCA_PARCIAL,
            VendaStatus.TROCA_TOTAL);

    private final RegraClassificacaoFinanceiraRepository regraRepository;
    private final PlanoContasGerencialRepository planoContasRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final DespesaRepository despesaRepository;
    private final TipoDespesaRepository tipoDespesaRepository;
    private final ContaPagarRepository contaPagarRepository;
    private final ContaReceberRepository contaReceberRepository;
    private final VendaRepository vendaRepository;
    private final NotaFiscalEntradaRepository notaFiscalEntradaRepository;
    private final CategoriaRepository categoriaRepository;
    private final FormaPagamentoRepository formaPagamentoRepository;
    private final FornecedorRepository fornecedorRepository;
    private final ConsolidacaoCompetenciaService consolidacaoCompetenciaService;
    private final FinancialAuditTrailService financialAuditTrailService;
    private final AuditTrailService auditTrailService;
    private final PrivacyGuardService privacyGuardService;

    public ClassificacaoFinanceiraService(
            RegraClassificacaoFinanceiraRepository regraRepository,
            PlanoContasGerencialRepository planoContasRepository,
            CentroCustoRepository centroCustoRepository,
            DespesaRepository despesaRepository,
            TipoDespesaRepository tipoDespesaRepository,
            ContaPagarRepository contaPagarRepository,
            ContaReceberRepository contaReceberRepository,
            VendaRepository vendaRepository,
            NotaFiscalEntradaRepository notaFiscalEntradaRepository,
            CategoriaRepository categoriaRepository,
            FormaPagamentoRepository formaPagamentoRepository,
            FornecedorRepository fornecedorRepository,
            ConsolidacaoCompetenciaService consolidacaoCompetenciaService,
            FinancialAuditTrailService financialAuditTrailService,
            AuditTrailService auditTrailService,
            PrivacyGuardService privacyGuardService) {
        this.regraRepository = regraRepository;
        this.planoContasRepository = planoContasRepository;
        this.centroCustoRepository = centroCustoRepository;
        this.despesaRepository = despesaRepository;
        this.tipoDespesaRepository = tipoDespesaRepository;
        this.contaPagarRepository = contaPagarRepository;
        this.contaReceberRepository = contaReceberRepository;
        this.vendaRepository = vendaRepository;
        this.notaFiscalEntradaRepository = notaFiscalEntradaRepository;
        this.categoriaRepository = categoriaRepository;
        this.formaPagamentoRepository = formaPagamentoRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.consolidacaoCompetenciaService = consolidacaoCompetenciaService;
        this.financialAuditTrailService = financialAuditTrailService;
        this.auditTrailService = auditTrailService;
        this.privacyGuardService = privacyGuardService;
    }

    @Transactional(readOnly = true)
    public List<RegraClassificacaoResponseDTO> listarRegras(
            OrigemClassificacaoFinanceira origemTipo,
            Boolean somenteAtivas,
            Long empresaId,
            Long filialId) {
        requireTenant(empresaId, filialId);
        List<RegraClassificacaoFinanceira> regras = Boolean.TRUE.equals(somenteAtivas)
                ? regraRepository
                        .findByEmpresaIdAndFilialIdAndAtivoTrueOrderByPrioridadeAscIdRegraClassificacaoAsc(
                                empresaId, filialId)
                : regraRepository
                        .findByEmpresaIdAndFilialIdOrderByPrioridadeAscIdRegraClassificacaoAsc(
                                empresaId, filialId);

        Map<Long, PlanoContasGerencial> contas = mapContasPorId(empresaId, filialId, false);
        Map<Long, CentroCusto> centros = mapCentrosPorId(empresaId, filialId, false);

        return regras.stream()
                .filter(r -> origemTipo == null || r.getOrigemTipo() == origemTipo)
                .map(r -> toRuleResponse(r, contas, centros))
                .toList();
    }

    @Transactional
    public RegraClassificacaoResponseDTO criarRegra(
            RegraClassificacaoRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        String nome = normalizeRequired(body.nome(), "Nome da regra de classificacao e obrigatorio.");
        PlanoContasGerencial conta = validarContaAnaliticaAtiva(
                body.planoContaId(), empresaId, filialId, body.origemTipo());
        CentroCusto centro = validarCentroAtivo(body.centroCustoId(), empresaId, filialId);
        validarReferenciasRegra(body, empresaId, filialId);

        RegraClassificacaoFinanceira regra = new RegraClassificacaoFinanceira();
        regra.setEmpresaId(empresaId);
        regra.setFilialId(filialId);
        regra.setNome(nome);
        regra.setOrigemTipo(body.origemTipo());
        regra.setTipoDespesaId(body.tipoDespesaId());
        regra.setCategoriaId(body.categoriaId());
        regra.setFormaPagamentoId(body.formaPagamentoId());
        regra.setFornecedorId(body.fornecedorId());
        regra.setPalavraChave(trimToNull(body.palavraChave()));
        regra.setPlanoContaId(conta.getIdPlanoConta());
        regra.setCentroCustoId(centro == null ? null : centro.getIdCentroCusto());
        regra.setPrioridade(body.prioridade() == null ? 100 : body.prioridade());
        regra.setAtivo(body.ativo() == null || body.ativo());

        RegraClassificacaoFinanceira salva = regraRepository.save(regra);
        registrarAuditoriaRegra(
                "REGRA_CLASSIFICACAO_CRIADA",
                salva,
                null,
                resumoRegra(salva),
                "Criacao de regra de classificacao financeira.",
                empresaId,
                filialId,
                request);
        return toRuleResponse(salva, Map.of(conta.getIdPlanoConta(), conta),
                centro == null ? Map.of() : Map.of(centro.getIdCentroCusto(), centro));
    }

    @Transactional
    public RegraClassificacaoResponseDTO atualizarRegra(
            Long id,
            RegraClassificacaoRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        RegraClassificacaoFinanceira existente = regraRepository
                .findByIdRegraClassificacaoAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Regra de classificacao nao encontrada."));
        String nome = normalizeRequired(body.nome(), "Nome da regra de classificacao e obrigatorio.");
        PlanoContasGerencial conta = validarContaAnaliticaAtiva(
                body.planoContaId(), empresaId, filialId, body.origemTipo());
        CentroCusto centro = validarCentroAtivo(body.centroCustoId(), empresaId, filialId);
        validarReferenciasRegra(body, empresaId, filialId);

        String anterior = resumoRegra(existente);
        existente.setNome(nome);
        existente.setOrigemTipo(body.origemTipo());
        existente.setTipoDespesaId(body.tipoDespesaId());
        existente.setCategoriaId(body.categoriaId());
        existente.setFormaPagamentoId(body.formaPagamentoId());
        existente.setFornecedorId(body.fornecedorId());
        existente.setPalavraChave(trimToNull(body.palavraChave()));
        existente.setPlanoContaId(conta.getIdPlanoConta());
        existente.setCentroCustoId(centro == null ? null : centro.getIdCentroCusto());
        if (body.prioridade() != null) {
            existente.setPrioridade(body.prioridade());
        }
        if (body.ativo() != null) {
            existente.setAtivo(body.ativo());
        }

        RegraClassificacaoFinanceira salva = regraRepository.save(existente);
        registrarAuditoriaRegra(
                "REGRA_CLASSIFICACAO_ATUALIZADA",
                salva,
                anterior,
                resumoRegra(salva),
                "Atualizacao de regra de classificacao financeira.",
                empresaId,
                filialId,
                request);
        return toRuleResponse(salva, Map.of(conta.getIdPlanoConta(), conta),
                centro == null ? Map.of() : Map.of(centro.getIdCentroCusto(), centro));
    }

    @Transactional
    public RegraClassificacaoResponseDTO alterarStatusRegra(
            Long id,
            boolean ativo,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        RegraClassificacaoFinanceira existente = regraRepository
                .findByIdRegraClassificacaoAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Regra de classificacao nao encontrada."));
        String anterior = resumoRegra(existente);
        existente.setAtivo(ativo);
        RegraClassificacaoFinanceira salva = regraRepository.save(existente);
        registrarAuditoriaRegra(
                ativo ? "REGRA_CLASSIFICACAO_ATIVADA" : "REGRA_CLASSIFICACAO_INATIVADA",
                salva,
                anterior,
                resumoRegra(salva),
                "Alteracao de status da regra de classificacao.",
                empresaId,
                filialId,
                request);
        return toRuleResponse(salva, mapContasPorId(empresaId, filialId, false),
                mapCentrosPorId(empresaId, filialId, false));
    }

    @Transactional(readOnly = true)
    public PendenciaClassificacaoResponseDTO pendentes(Long empresaId, Long filialId) {
        return pendentes(null, empresaId, filialId);
    }

    @Transactional(readOnly = true)
    public PendenciaClassificacaoResponseDTO pendentes(
            OrigemClassificacaoFinanceira origemTipo,
            Long empresaId,
            Long filialId) {
        requireTenant(empresaId, filialId);
        List<Despesa> despesas = despesaRepository
                .findByEmpresaIdAndFilialIdAndPlanoContaIdIsNull(empresaId, filialId).stream()
                .filter(d -> d.getStatus() != 0)
                .toList();
        List<ContaPagar> contasPagar = contaPagarRepository
                .findByEmpresaIdAndFilialIdAndPlanoContaIdIsNull(empresaId, filialId).stream()
                .filter(cp -> cp.getStatus() != FinanceiroStatus.CANCELADO
                        && cp.getStatus() != FinanceiroStatus.DEVOLVIDO)
                .toList();
        List<ContaReceber> contasReceber = contaReceberRepository
                .findByEmpresaIdAndFilialIdAndPlanoContaIdIsNull(empresaId, filialId).stream()
                .filter(cr -> cr.getStatus() != FinanceiroStatus.CANCELADO
                        && cr.getStatus() != FinanceiroStatus.DEVOLVIDO)
                .toList();
        List<Venda> vendas = vendaRepository
                .findByEmpresaIdAndFilialIdAndPlanoContaIdIsNull(empresaId, filialId).stream()
                .filter(this::isVendaFinalizada)
                .toList();

        ContextoClassificacao ctx = carregarContexto(empresaId, filialId);
        Comparator<ItemPendenteClassificacaoDTO> itemOrder = Comparator
                .comparing(ItemPendenteClassificacaoDTO::dataCompetencia,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ItemPendenteClassificacaoDTO::registroId, Comparator.reverseOrder());

        List<ItemPendenteClassificacaoDTO> despesaItems = new ArrayList<>();
        if (origemTipo == null || origemTipo == OrigemClassificacaoFinanceira.DESPESA) {
            for (Despesa d : despesas) {
                SugestaoClassificacao sug = sugerirDespesa(d, ctx);
                despesaItems.add(new ItemPendenteClassificacaoDTO(
                        OrigemClassificacaoFinanceira.DESPESA,
                        d.getIdDespesa(),
                        d.getDescricao(),
                        d.getValor(),
                        effectiveDespesaCompetencia(d),
                        sug.planoContaId(),
                        sug.planoContaNome(),
                        sug.centroCustoId()));
            }
            despesaItems.sort(itemOrder);
        }

        List<ItemPendenteClassificacaoDTO> cpItems = new ArrayList<>();
        if (origemTipo == null || origemTipo == OrigemClassificacaoFinanceira.CONTA_PAGAR) {
            for (ContaPagar cp : contasPagar) {
                SugestaoClassificacao sug = sugerirContaPagar(cp, ctx);
                cpItems.add(new ItemPendenteClassificacaoDTO(
                        OrigemClassificacaoFinanceira.CONTA_PAGAR,
                        cp.getIdContaPagar(),
                        cp.getDescricao(),
                        cp.getValor(),
                        effectiveContaPagarCompetencia(cp),
                        sug.planoContaId(),
                        sug.planoContaNome(),
                        sug.centroCustoId()));
            }
            cpItems.sort(itemOrder);
        }

        List<ItemPendenteClassificacaoDTO> crItems = new ArrayList<>();
        if (origemTipo == null || origemTipo == OrigemClassificacaoFinanceira.CONTA_RECEBER) {
            for (ContaReceber cr : contasReceber) {
                SugestaoClassificacao sug = sugerirContaReceber(cr, ctx);
                crItems.add(new ItemPendenteClassificacaoDTO(
                        OrigemClassificacaoFinanceira.CONTA_RECEBER,
                        cr.getIdContaReceber(),
                        "Conta a receber da venda #" + cr.getVendaId(),
                        cr.getValorLiquido(),
                        effectiveContaReceberCompetencia(cr),
                        sug.planoContaId(),
                        sug.planoContaNome(),
                        sug.centroCustoId()));
            }
            crItems.sort(itemOrder);
        }

        List<ItemPendenteClassificacaoDTO> vendaItems = new ArrayList<>();
        if (origemTipo == null || origemTipo == OrigemClassificacaoFinanceira.VENDA) {
            for (Venda v : vendas) {
                SugestaoClassificacao sug = sugerirVenda(v, ctx);
                String desc = v.getNumeroVenda() != null && !v.getNumeroVenda().isBlank()
                        ? "Venda " + v.getNumeroVenda()
                        : "Venda #" + v.getIdVenda();
                vendaItems.add(new ItemPendenteClassificacaoDTO(
                        OrigemClassificacaoFinanceira.VENDA,
                        v.getIdVenda(),
                        desc,
                        v.getValorTotal(),
                        effectiveVendaCompetencia(v),
                        sug.planoContaId(),
                        sug.planoContaNome(),
                        sug.centroCustoId()));
            }
            vendaItems.sort(itemOrder);
        }

        List<ItemPendenteClassificacaoDTO> limitados;
        if (origemTipo == OrigemClassificacaoFinanceira.NFE_ENTRADA) {
            List<ItemPendenteClassificacaoDTO> nfeItems = new ArrayList<>();
            for (NotaFiscalEntrada nf : notaFiscalEntradaRepository
                    .findByEmpresaIdAndFilialIdOrderByDataEntradaDesc(empresaId, filialId)) {
                if (nf.getPlanoContaId() != null) {
                    continue;
                }
                SugestaoClassificacao sug = sugerirNfeEntrada(nf, ctx);
                nfeItems.add(new ItemPendenteClassificacaoDTO(
                        OrigemClassificacaoFinanceira.NFE_ENTRADA,
                        nf.getIdNfEntrada(),
                        "NF-e " + (nf.getNumero() == null ? nf.getIdNfEntrada() : nf.getNumero()),
                        nf.getValorTotal(),
                        effectiveNfeCompetencia(nf),
                        sug.planoContaId(),
                        sug.planoContaNome(),
                        sug.centroCustoId()));
            }
            nfeItems.sort(itemOrder);
            limitados = nfeItems.size() > MAX_PREVIEW_ITENS
                    ? List.copyOf(nfeItems.subList(0, MAX_PREVIEW_ITENS))
                    : List.copyOf(nfeItems);
        } else if (origemTipo != null) {
            List<ItemPendenteClassificacaoDTO> single = switch (origemTipo) {
                case DESPESA -> despesaItems;
                case CONTA_PAGAR -> cpItems;
                case CONTA_RECEBER -> crItems;
                case VENDA -> vendaItems;
                default -> List.of();
            };
            limitados = single.size() > MAX_PREVIEW_ITENS
                    ? List.copyOf(single.subList(0, MAX_PREVIEW_ITENS))
                    : List.copyOf(single);
        } else {
            List<List<ItemPendenteClassificacaoDTO>> groups = List.of(despesaItems, cpItems, crItems, vendaItems);
            List<ItemPendenteClassificacaoDTO> balanced = new ArrayList<>();
            int idx = 0;
            while (balanced.size() < MAX_PREVIEW_ITENS) {
                boolean added = false;
                for (List<ItemPendenteClassificacaoDTO> g : groups) {
                    if (idx < g.size() && balanced.size() < MAX_PREVIEW_ITENS) {
                        balanced.add(g.get(idx));
                        added = true;
                    }
                }
                if (!added) {
                    break;
                }
                idx++;
            }
            balanced.sort(itemOrder);
            limitados = List.copyOf(balanced);
        }

        long total = (long) despesas.size() + contasPagar.size() + contasReceber.size() + vendas.size();
        return new PendenciaClassificacaoResponseDTO(
                total,
                despesas.size(),
                contasPagar.size(),
                contasReceber.size(),
                vendas.size(),
                limitados);
    }

    @Transactional
    public ResultadoClassificacaoLoteDTO aplicarAutomatico(
            OrigemClassificacaoFinanceira origemTipo,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        ContextoClassificacao ctx = carregarContexto(empresaId, filialId);

        List<OrigemClassificacaoFinanceira> origens = origemTipo != null
                ? List.of(origemTipo)
                : List.of(
                        OrigemClassificacaoFinanceira.DESPESA,
                        OrigemClassificacaoFinanceira.CONTA_PAGAR,
                        OrigemClassificacaoFinanceira.CONTA_RECEBER,
                        OrigemClassificacaoFinanceira.VENDA);

        int processados = 0;
        int classificados = 0;
        int naoClassificados = 0;
        List<String> observacoes = new ArrayList<>();
        int observacoesOmitidas = 0;
        Set<YearMonth> competenciasAfetadas = new LinkedHashSet<>();

        for (OrigemClassificacaoFinanceira origem : origens) {
            switch (origem) {
                case DESPESA -> {
                    List<Despesa> pendentes = despesaRepository
                            .findByEmpresaIdAndFilialIdAndPlanoContaIdIsNull(empresaId, filialId).stream()
                            .filter(d -> d.getStatus() != 0)
                            .toList();
                    for (Despesa d : pendentes) {
                        processados++;
                        SugestaoClassificacao sug = sugerirDespesa(d, ctx);
                        if (sug.planoContaId() != null) {
                            d.setPlanoContaId(sug.planoContaId());
                            if (d.getCentroCustoId() == null && sug.centroCustoId() != null) {
                                d.setCentroCustoId(sug.centroCustoId());
                            }
                            despesaRepository.save(d);
                            classificados++;
                            addCompetencia(competenciasAfetadas, effectiveDespesaCompetencia(d));
                            addCompetencia(competenciasAfetadas, d.getDataPagamento());
                        } else {
                            naoClassificados++;
                            observacoesOmitidas += registrarObservacao(observacoes,
                                    "Despesa #" + d.getIdDespesa() + " ('" + d.getDescricao()
                                            + "') sem regra ou conta gerencial no tipo.");
                        }
                    }
                }
                case CONTA_PAGAR -> {
                    List<ContaPagar> pendentes = contaPagarRepository
                            .findByEmpresaIdAndFilialIdAndPlanoContaIdIsNull(empresaId, filialId).stream()
                            .filter(cp -> cp.getStatus() != FinanceiroStatus.CANCELADO
                                    && cp.getStatus() != FinanceiroStatus.DEVOLVIDO)
                            .toList();
                    for (ContaPagar cp : pendentes) {
                        processados++;
                        SugestaoClassificacao sug = sugerirContaPagar(cp, ctx);
                        if (sug.planoContaId() != null) {
                            cp.setPlanoContaId(sug.planoContaId());
                            if (cp.getCentroCustoId() == null && sug.centroCustoId() != null) {
                                cp.setCentroCustoId(sug.centroCustoId());
                            }
                            contaPagarRepository.save(cp);
                            classificados++;
                            addCompetencia(competenciasAfetadas, effectiveContaPagarCompetencia(cp));
                            addCompetencia(competenciasAfetadas, cp.getPagoEm() == null ? null : cp.getPagoEm().toLocalDate());
                        } else {
                            naoClassificados++;
                            observacoesOmitidas += registrarObservacao(observacoes,
                                    "ContaPagar #" + cp.getIdContaPagar() + " ('" + cp.getDescricao()
                                            + "') sem regra ou categoria classificada.");
                        }
                    }
                }
                case CONTA_RECEBER -> {
                    List<ContaReceber> pendentes = contaReceberRepository
                            .findByEmpresaIdAndFilialIdAndPlanoContaIdIsNull(empresaId, filialId).stream()
                            .filter(cr -> cr.getStatus() != FinanceiroStatus.CANCELADO
                                    && cr.getStatus() != FinanceiroStatus.DEVOLVIDO)
                            .toList();
                    for (ContaReceber cr : pendentes) {
                        processados++;
                        SugestaoClassificacao sug = sugerirContaReceber(cr, ctx);
                        if (sug.planoContaId() != null) {
                            cr.setPlanoContaId(sug.planoContaId());
                            if (cr.getCentroCustoId() == null && sug.centroCustoId() != null) {
                                cr.setCentroCustoId(sug.centroCustoId());
                            }
                            contaReceberRepository.save(cr);
                            classificados++;
                            addCompetencia(competenciasAfetadas, effectiveContaReceberCompetencia(cr));
                            addCompetencia(competenciasAfetadas, cr.getPagoEm() == null ? null : cr.getPagoEm().toLocalDate());
                        } else {
                            naoClassificados++;
                            observacoesOmitidas += registrarObservacao(observacoes,
                                    "ContaReceber #" + cr.getIdContaReceber()
                                            + " sem regra nem conta padrao 1.01 inicializada.");
                        }
                    }
                }
                case VENDA -> {
                    List<Venda> pendentes = vendaRepository
                            .findByEmpresaIdAndFilialIdAndPlanoContaIdIsNull(empresaId, filialId).stream()
                            .filter(this::isVendaFinalizada)
                            .toList();
                    for (Venda v : pendentes) {
                        processados++;
                        SugestaoClassificacao sug = sugerirVenda(v, ctx);
                        if (sug.planoContaId() != null) {
                            v.setPlanoContaId(sug.planoContaId());
                            if (v.getCentroCustoId() == null && sug.centroCustoId() != null) {
                                v.setCentroCustoId(sug.centroCustoId());
                            }
                            vendaRepository.save(v);
                            classificados++;
                            addCompetencia(competenciasAfetadas, effectiveVendaCompetencia(v));
                        } else {
                            naoClassificados++;
                            observacoesOmitidas += registrarObservacao(observacoes,
                                    "Venda #" + v.getIdVenda() + " sem regra nem conta padrao 1.01 inicializada.");
                        }
                    }
                }
                case NFE_ENTRADA -> {
                    List<NotaFiscalEntrada> pendentes = notaFiscalEntradaRepository
                            .findByEmpresaIdAndFilialIdOrderByDataEntradaDesc(empresaId, filialId).stream()
                            .filter(nf -> nf.getPlanoContaId() == null)
                            .toList();
                    for (NotaFiscalEntrada nf : pendentes) {
                        processados++;
                        SugestaoClassificacao sug = sugerirNfeEntrada(nf, ctx);
                        if (sug.planoContaId() != null) {
                            nf.setPlanoContaId(sug.planoContaId());
                            if (nf.getCentroCustoId() == null && sug.centroCustoId() != null) {
                                nf.setCentroCustoId(sug.centroCustoId());
                            }
                            notaFiscalEntradaRepository.save(nf);
                            classificados++;
                            addCompetencia(competenciasAfetadas, effectiveNfeCompetencia(nf));
                        } else {
                            naoClassificados++;
                            observacoesOmitidas += registrarObservacao(observacoes,
                                    "NFe Entrada #" + nf.getIdNfEntrada() + " (" + nf.getNumero()
                                            + ") sem regra deterministica.");
                        }
                    }
                }
                default -> {
                    observacoesOmitidas += registrarObservacao(observacoes,
                            "Origem " + origem + " nao suportada para classificacao automatica em lote.");
                }
            }
        }

        if (observacoesOmitidas > 0) {
            observacoes.add("... e mais " + observacoesOmitidas + " itens sem classificacao deterministica.");
        }

        for (YearMonth ym : competenciasAfetadas) {
            consolidacaoCompetenciaService.recalcularCompetenciaInterna(empresaId, filialId, ym, request);
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("origemTipo", origemTipo == null ? "TODAS" : origemTipo.name());
        metadata.put("processados", processados);
        metadata.put("classificados", classificados);
        metadata.put("naoClassificados", naoClassificados);
        metadata.put("competenciasRecalculadas", competenciasAfetadas.stream().map(YearMonth::toString).toList());

        String resumo = "origem=" + (origemTipo == null ? "TODAS" : origemTipo.name())
                + ";processados=" + processados
                + ";classificados=" + classificados
                + ";naoClassificados=" + naoClassificados;

        financialAuditTrailService.record(
                "CLASSIFICACAO_AUTOMATICA_LOTE",
                "ClassificacaoFinanceira",
                null,
                privacyGuardService.currentUserLogin(request),
                null,
                resumo,
                metadata,
                empresaId,
                filialId);
        auditTrailService.registrarPrivacidade(
                "CLASSIFICACAO_AUTOMATICA_LOTE",
                "ClassificacaoFinanceira",
                null,
                "Financeiro",
                null,
                resumo,
                request,
                "Aplicacao de classificacao financeira automatica por regras.");

        return new ResultadoClassificacaoLoteDTO(processados, classificados, naoClassificados, List.copyOf(observacoes));
    }

    @Transactional
    public ItemPendenteClassificacaoDTO reclassificarManual(
            ReclassificacaoManualRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        PlanoContasGerencial novaConta = validarContaAnaliticaAtiva(
                body.planoContaId(), empresaId, filialId, body.origemTipo());
        CentroCusto novoCentro = validarCentroAtivo(body.centroCustoId(), empresaId, filialId);
        Long novoCentroId = novoCentro == null ? null : novoCentro.getIdCentroCusto();

        Long anteriorContaId;
        Long anteriorCentroId;
        String descricao;
        BigDecimal valor;
        LocalDate dataCompetencia;
        Set<YearMonth> competenciasAfetadas = new LinkedHashSet<>();

        switch (body.origemTipo()) {
            case DESPESA -> {
                Despesa d = despesaRepository.findByIdDespesaAndEmpresaIdAndFilialId(
                                body.registroId(), empresaId, filialId)
                        .orElseThrow(() -> new EntityNotFoundException("Despesa nao encontrada no tenant."));
                anteriorContaId = d.getPlanoContaId();
                anteriorCentroId = d.getCentroCustoId();
                d.setPlanoContaId(novaConta.getIdPlanoConta());
                d.setCentroCustoId(novoCentroId);
                despesaRepository.save(d);
                descricao = d.getDescricao();
                valor = d.getValor();
                dataCompetencia = effectiveDespesaCompetencia(d);
                addCompetencia(competenciasAfetadas, dataCompetencia);
                addCompetencia(competenciasAfetadas, d.getDataPagamento());
            }
            case CONTA_PAGAR -> {
                ContaPagar cp = contaPagarRepository.findByIdContaPagarAndEmpresaIdAndFilialId(
                                body.registroId(), empresaId, filialId)
                        .orElseThrow(() -> new EntityNotFoundException("Conta a pagar nao encontrada no tenant."));
                anteriorContaId = cp.getPlanoContaId();
                anteriorCentroId = cp.getCentroCustoId();
                cp.setPlanoContaId(novaConta.getIdPlanoConta());
                cp.setCentroCustoId(novoCentroId);
                contaPagarRepository.save(cp);
                descricao = cp.getDescricao();
                valor = cp.getValor();
                dataCompetencia = effectiveContaPagarCompetencia(cp);
                addCompetencia(competenciasAfetadas, dataCompetencia);
                addCompetencia(competenciasAfetadas, cp.getPagoEm() == null ? null : cp.getPagoEm().toLocalDate());
            }
            case CONTA_RECEBER -> {
                ContaReceber cr = contaReceberRepository.findByIdContaReceberAndEmpresaIdAndFilialId(
                                body.registroId(), empresaId, filialId)
                        .orElseThrow(() -> new EntityNotFoundException("Conta a receber nao encontrada no tenant."));
                anteriorContaId = cr.getPlanoContaId();
                anteriorCentroId = cr.getCentroCustoId();
                cr.setPlanoContaId(novaConta.getIdPlanoConta());
                cr.setCentroCustoId(novoCentroId);
                contaReceberRepository.save(cr);
                descricao = "Conta a receber venda #" + cr.getVendaId();
                valor = cr.getValorLiquido();
                dataCompetencia = effectiveContaReceberCompetencia(cr);
                addCompetencia(competenciasAfetadas, dataCompetencia);
                addCompetencia(competenciasAfetadas, cr.getPagoEm() == null ? null : cr.getPagoEm().toLocalDate());
            }
            case VENDA -> {
                Venda v = vendaRepository.findByIdVendaAndEmpresaIdAndFilialId(
                                body.registroId(), empresaId, filialId)
                        .orElseThrow(() -> new EntityNotFoundException("Venda nao encontrada no tenant."));
                anteriorContaId = v.getPlanoContaId();
                anteriorCentroId = v.getCentroCustoId();
                v.setPlanoContaId(novaConta.getIdPlanoConta());
                v.setCentroCustoId(novoCentroId);
                vendaRepository.save(v);
                descricao = v.getNumeroVenda() != null ? "Venda " + v.getNumeroVenda() : "Venda #" + v.getIdVenda();
                valor = v.getValorTotal();
                dataCompetencia = effectiveVendaCompetencia(v);
                addCompetencia(competenciasAfetadas, dataCompetencia);
            }
            case NFE_ENTRADA -> {
                NotaFiscalEntrada nf = notaFiscalEntradaRepository.findByIdNfEntradaAndEmpresaIdAndFilialId(
                                body.registroId(), empresaId, filialId)
                        .orElseThrow(() -> new EntityNotFoundException("Nota fiscal de entrada nao encontrada no tenant."));
                anteriorContaId = nf.getPlanoContaId();
                anteriorCentroId = nf.getCentroCustoId();
                nf.setPlanoContaId(novaConta.getIdPlanoConta());
                nf.setCentroCustoId(novoCentroId);
                notaFiscalEntradaRepository.save(nf);
                descricao = "NF Entrada " + nf.getNumero();
                valor = nf.getValorTotal();
                dataCompetencia = effectiveNfeCompetencia(nf);
                addCompetencia(competenciasAfetadas, dataCompetencia);
            }
            default -> throw new IllegalArgumentException(
                    "Origem nao suportada para classificacao manual: " + body.origemTipo());
        }

        String oldSnapshot = "planoContaId=" + anteriorContaId + ";centroCustoId=" + anteriorCentroId;
        String newSnapshot = "planoContaId=" + novaConta.getIdPlanoConta() + ";centroCustoId=" + novoCentroId;
        String motivo = trimToNull(body.motivo());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("origemTipo", body.origemTipo().name());
        metadata.put("registroId", body.registroId());
        metadata.put("anteriorPlanoContaId", anteriorContaId);
        metadata.put("anteriorCentroCustoId", anteriorCentroId);
        metadata.put("novoPlanoContaId", novaConta.getIdPlanoConta());
        metadata.put("novoCentroCustoId", novoCentroId);
        metadata.put("motivo", motivo);

        financialAuditTrailService.record(
                "RECLASSIFICACAO_MANUAL",
                body.origemTipo().name(),
                body.registroId(),
                privacyGuardService.currentUserLogin(request),
                oldSnapshot,
                newSnapshot,
                metadata,
                empresaId,
                filialId);
        auditTrailService.registrarPrivacidade(
                "RECLASSIFICACAO_MANUAL",
                body.origemTipo().name(),
                body.registroId(),
                "Financeiro",
                oldSnapshot,
                newSnapshot,
                request,
                limit(motivo == null ? "Classificacao gerencial manual." : motivo, 255));

        for (YearMonth ym : competenciasAfetadas) {
            consolidacaoCompetenciaService.recalcularCompetenciaInterna(empresaId, filialId, ym, request);
        }

        return new ItemPendenteClassificacaoDTO(
                body.origemTipo(),
                body.registroId(),
                descricao,
                valor,
                dataCompetencia,
                novaConta.getIdPlanoConta(),
                novaConta.getNome(),
                novoCentroId);
    }

    @Transactional(readOnly = true)
    public void classificarAutomaticamenteDespesa(Despesa despesa) {
        if (despesa == null || despesa.getEmpresaId() == null || despesa.getFilialId() == null) {
            return;
        }
        if (despesa.getPlanoContaId() != null) {
            return;
        }
        ContextoClassificacao ctx = carregarContexto(despesa.getEmpresaId(), despesa.getFilialId());
        SugestaoClassificacao sug = sugerirDespesa(despesa, ctx);
        if (sug.planoContaId() != null) {
            despesa.setPlanoContaId(sug.planoContaId());
            if (despesa.getCentroCustoId() == null && sug.centroCustoId() != null) {
                despesa.setCentroCustoId(sug.centroCustoId());
            }
        }
    }

    private ContextoClassificacao carregarContexto(Long empresaId, Long filialId) {
        Map<Long, PlanoContasGerencial> contasAtivas = mapContasPorId(empresaId, filialId, true);
        Map<String, PlanoContasGerencial> contasPorCodigo = new HashMap<>();
        for (PlanoContasGerencial c : contasAtivas.values()) {
            contasPorCodigo.put(c.getCodigo(), c);
        }
        Map<Long, CentroCusto> centrosAtivos = mapCentrosPorId(empresaId, filialId, true);
        Map<Long, Categoria> categoriasPorId = new HashMap<>();
        for (Categoria c : categoriaRepository.findVisibleToTenant(empresaId, filialId)) {
            categoriasPorId.put(c.getIdCategoria(), c);
        }

        List<RegraClassificacaoFinanceira> regras = regraRepository
                .findByEmpresaIdAndFilialIdAndAtivoTrueOrderByPrioridadeAscIdRegraClassificacaoAsc(
                        empresaId, filialId)
                .stream()
                .sorted(Comparator.comparing((RegraClassificacaoFinanceira r) -> r.getPrioridade() == null ? 100 : r.getPrioridade())
                        .thenComparing(Comparator.comparingInt(this::especificidade).reversed())
                        .thenComparing(RegraClassificacaoFinanceira::getIdRegraClassificacao))
                .toList();

        return new ContextoClassificacao(regras, contasAtivas, contasPorCodigo, centrosAtivos, categoriasPorId);
    }

    private SugestaoClassificacao sugerirDespesa(Despesa d, ContextoClassificacao ctx) {
        Long tipoDespesaId = d.getTipoDespesa() == null ? null : d.getTipoDespesa().getIdTipoDespesa();
        String texto = ((d.getDescricao() == null ? "" : d.getDescricao()) + " "
                + (d.getTipoDespesa() == null || d.getTipoDespesa().getNome() == null ? "" : d.getTipoDespesa().getNome()))
                .toLowerCase(Locale.ROOT);

        for (RegraClassificacaoFinanceira r : ctx.regras()) {
            if (r.getOrigemTipo() != OrigemClassificacaoFinanceira.DESPESA) {
                continue;
            }
            if (!matchesCriteria(r, tipoDespesaId, null, null, null, texto)) {
                continue;
            }
            SugestaoClassificacao valida = validarSugestao(r.getPlanoContaId(), r.getCentroCustoId(), ctx);
            if (valida.planoContaId() != null) {
                return valida;
            }
        }

        if (d.getTipoDespesa() != null && d.getTipoDespesa().getPlanoContaId() != null) {
            return validarSugestao(d.getTipoDespesa().getPlanoContaId(), d.getTipoDespesa().getCentroCustoId(), ctx);
        }
        return SugestaoClassificacao.VAZIA;
    }

    private SugestaoClassificacao sugerirContaPagar(ContaPagar cp, ContextoClassificacao ctx) {
        String texto = cp.getDescricao() == null ? "" : cp.getDescricao().toLowerCase(Locale.ROOT);
        for (RegraClassificacaoFinanceira r : ctx.regras()) {
            if (r.getOrigemTipo() != OrigemClassificacaoFinanceira.CONTA_PAGAR) {
                continue;
            }
            if (!matchesCriteria(r, null, cp.getCategoriaId(), cp.getFormaPagamentoPrevistaId(),
                    cp.getFornecedorId(), texto)) {
                continue;
            }
            SugestaoClassificacao valida = validarSugestao(r.getPlanoContaId(), r.getCentroCustoId(), ctx);
            if (valida.planoContaId() != null) {
                return valida;
            }
        }
        if (cp.getCategoriaId() != null) {
            Categoria cat = ctx.categoriasPorId().get(cp.getCategoriaId());
            if (cat != null && cat.getPlanoContaId() != null) {
                return validarSugestao(cat.getPlanoContaId(), cat.getCentroCustoId(), ctx);
            }
        }
        return SugestaoClassificacao.VAZIA;
    }

    private SugestaoClassificacao sugerirContaReceber(ContaReceber cr, ContextoClassificacao ctx) {
        String texto = ("conta receber venda " + cr.getVendaId()).toLowerCase(Locale.ROOT);
        for (RegraClassificacaoFinanceira r : ctx.regras()) {
            if (r.getOrigemTipo() != OrigemClassificacaoFinanceira.CONTA_RECEBER) {
                continue;
            }
            if (!matchesCriteria(r, null, null, cr.getFormaPagamentoId(), null, texto)) {
                continue;
            }
            SugestaoClassificacao valida = validarSugestao(r.getPlanoContaId(), r.getCentroCustoId(), ctx);
            if (valida.planoContaId() != null) {
                return valida;
            }
        }
        PlanoContasGerencial contaPadrao = ctx.contasPorCodigo().get("1.01");
        if (contaPadrao != null && contaPadrao.isAceitaLancamento()) {
            return new SugestaoClassificacao(contaPadrao.getIdPlanoConta(), contaPadrao.getNome(), null);
        }
        return SugestaoClassificacao.VAZIA;
    }

    private SugestaoClassificacao sugerirVenda(Venda v, ContextoClassificacao ctx) {
        String texto = ((v.getNumeroVenda() == null ? "" : v.getNumeroVenda()) + " "
                + (v.getObservacao() == null ? "" : v.getObservacao())).toLowerCase(Locale.ROOT);
        for (RegraClassificacaoFinanceira r : ctx.regras()) {
            if (r.getOrigemTipo() != OrigemClassificacaoFinanceira.VENDA) {
                continue;
            }
            if (r.getTipoDespesaId() != null || r.getFornecedorId() != null) {
                continue;
            }
            if (r.getFormaPagamentoId() != null && !Objects.equals(r.getFormaPagamentoId(), v.getFormaPagamentoRealId())) {
                continue;
            }
            if (r.getCategoriaId() != null && !vendaUnicaCategoria(v, r.getCategoriaId())) {
                continue;
            }
            if (r.getPalavraChave() != null && !r.getPalavraChave().isBlank()
                    && !texto.contains(r.getPalavraChave().trim().toLowerCase(Locale.ROOT))) {
                continue;
            }
            SugestaoClassificacao valida = validarSugestao(r.getPlanoContaId(), r.getCentroCustoId(), ctx);
            if (valida.planoContaId() != null) {
                return valida;
            }
        }
        PlanoContasGerencial contaPadrao = ctx.contasPorCodigo().get("1.01");
        if (contaPadrao != null && contaPadrao.isAceitaLancamento()) {
            return new SugestaoClassificacao(contaPadrao.getIdPlanoConta(), contaPadrao.getNome(), null);
        }
        return SugestaoClassificacao.VAZIA;
    }

    private SugestaoClassificacao sugerirNfeEntrada(NotaFiscalEntrada nf, ContextoClassificacao ctx) {
        Long fornecedorId = nf.getFornecedor() == null ? null : nf.getFornecedor().getIdFornecedor();
        String texto = ("nf " + (nf.getNumero() == null ? "" : nf.getNumero()) + " "
                + (nf.getSerie() == null ? "" : nf.getSerie())).toLowerCase(Locale.ROOT);
        for (RegraClassificacaoFinanceira r : ctx.regras()) {
            if (r.getOrigemTipo() != OrigemClassificacaoFinanceira.NFE_ENTRADA) {
                continue;
            }
            if (!matchesCriteria(r, null, null, null, fornecedorId, texto)) {
                continue;
            }
            SugestaoClassificacao valida = validarSugestao(r.getPlanoContaId(), r.getCentroCustoId(), ctx);
            if (valida.planoContaId() != null) {
                return valida;
            }
        }
        return SugestaoClassificacao.VAZIA;
    }

    private boolean vendaUnicaCategoria(Venda v, Long categoriaId) {
        Hibernate.initialize(v.getItens());
        if (v.getItens() == null || v.getItens().isEmpty()) {
            return false;
        }
        for (ProdutoVenda item : v.getItens()) {
            if (item.getProduto() == null || item.getProduto().getCategoria() == null
                    || !Objects.equals(item.getProduto().getCategoria().getIdCategoria(), categoriaId)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesCriteria(
            RegraClassificacaoFinanceira r,
            Long tipoDespesaId,
            Long categoriaId,
            Long formaPagamentoId,
            Long fornecedorId,
            String textoLower) {
        if (r.getTipoDespesaId() != null && !Objects.equals(r.getTipoDespesaId(), tipoDespesaId)) {
            return false;
        }
        if (r.getCategoriaId() != null && !Objects.equals(r.getCategoriaId(), categoriaId)) {
            return false;
        }
        if (r.getFormaPagamentoId() != null && !Objects.equals(r.getFormaPagamentoId(), formaPagamentoId)) {
            return false;
        }
        if (r.getFornecedorId() != null && !Objects.equals(r.getFornecedorId(), fornecedorId)) {
            return false;
        }
        if (r.getPalavraChave() != null && !r.getPalavraChave().isBlank()) {
            if (textoLower == null || !textoLower.contains(r.getPalavraChave().trim().toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    private SugestaoClassificacao validarSugestao(
            Long planoContaId,
            Long centroCustoId,
            ContextoClassificacao ctx) {
        if (planoContaId == null) {
            return SugestaoClassificacao.VAZIA;
        }
        PlanoContasGerencial conta = ctx.contasAtivas().get(planoContaId);
        if (conta == null || !conta.isAtivo() || !conta.isAceitaLancamento()) {
            return SugestaoClassificacao.VAZIA;
        }
        Long validCentro = (centroCustoId != null && ctx.centrosAtivos().containsKey(centroCustoId))
                ? centroCustoId
                : null;
        return new SugestaoClassificacao(conta.getIdPlanoConta(), conta.getNome(), validCentro);
    }

    private int especificidade(RegraClassificacaoFinanceira r) {
        int score = 0;
        if (r.getTipoDespesaId() != null) score++;
        if (r.getCategoriaId() != null) score++;
        if (r.getFormaPagamentoId() != null) score++;
        if (r.getFornecedorId() != null) score++;
        if (r.getPalavraChave() != null && !r.getPalavraChave().isBlank()) score++;
        return score;
    }

    public PlanoContasGerencial validarContaAnaliticaAtiva(
            Long planoContaId,
            Long empresaId,
            Long filialId,
            OrigemClassificacaoFinanceira origemTipo) {
        PlanoContasGerencial conta = planoContasRepository
                .findByIdPlanoContaAndEmpresaIdAndFilialId(planoContaId, empresaId, filialId)
                .orElseThrow(() -> new IllegalArgumentException("Conta gerencial nao encontrada no tenant."));
        if (!conta.isAtivo()) {
            throw new IllegalArgumentException("Conta gerencial inativa nao pode receber classificacoes.");
        }
        if (!conta.isAceitaLancamento()) {
            throw new IllegalArgumentException("Conta sintetica nao aceita lancamentos diretos.");
        }
        validarPolaridadeCompativel(conta, origemTipo);
        return conta;
    }

    private void validarPolaridadeCompativel(
            PlanoContasGerencial conta,
            OrigemClassificacaoFinanceira origemTipo) {
        if (origemTipo == null) {
            return;
        }
        switch (origemTipo) {
            case DESPESA, CONTA_PAGAR, NFE_ENTRADA -> {
                if (conta.getNatureza() == NaturezaContaGerencial.CREDITO) {
                    throw new IllegalArgumentException(
                            "Conta de natureza CREDITO nao e compativel com origem " + origemTipo + ".");
                }
            }
            case VENDA, CONTA_RECEBER -> {
                if (conta.getNatureza() != NaturezaContaGerencial.CREDITO) {
                    throw new IllegalArgumentException(
                            "Origem " + origemTipo + " exige conta gerencial de natureza CREDITO.");
                }
            }
            case DEVOLUCAO -> {
                if (conta.getNatureza() != NaturezaContaGerencial.DEBITO
                        || !EnumSet.of(
                                TipoContaGerencial.DEDUCAO,
                                TipoContaGerencial.DESPESA,
                                TipoContaGerencial.OUTRAS).contains(conta.getTipoConta())) {
                    throw new IllegalArgumentException(
                            "Origem DEVOLUCAO exige conta de deducao, despesa ou outras com natureza DEBITO.");
                }
            }
            default -> {
            }
        }
    }

    public CentroCusto validarCentroAtivo(Long centroCustoId, Long empresaId, Long filialId) {
        if (centroCustoId == null) {
            return null;
        }
        CentroCusto cc = centroCustoRepository
                .findByIdCentroCustoAndEmpresaIdAndFilialId(centroCustoId, empresaId, filialId)
                .orElseThrow(() -> new IllegalArgumentException("Centro de custo nao encontrado no tenant."));
        if (!cc.isAtivo()) {
            throw new IllegalArgumentException("Centro de custo inativo nao pode receber classificacoes.");
        }
        return cc;
    }

    private void validarReferenciasRegra(RegraClassificacaoRequestDTO body, Long empresaId, Long filialId) {
        if (body.tipoDespesaId() != null) {
            tipoDespesaRepository.findVisibleById(body.tipoDespesaId(), empresaId, filialId)
                    .orElseThrow(() -> new IllegalArgumentException("Tipo de despesa nao encontrado no tenant."));
        }
        if (body.categoriaId() != null) {
            categoriaRepository.findVisibleById(body.categoriaId(), empresaId, filialId)
                    .orElseThrow(() -> new IllegalArgumentException("Categoria nao encontrada no tenant."));
        }
        if (body.formaPagamentoId() != null) {
            formaPagamentoRepository.findVisibleById(body.formaPagamentoId(), empresaId, filialId)
                    .orElseThrow(() -> new IllegalArgumentException("Forma de pagamento nao encontrada no tenant."));
        }
        if (body.fornecedorId() != null) {
            fornecedorRepository.findByIdFornecedorAndEmpresaIdAndFilialId(body.fornecedorId(), empresaId, filialId)
                    .orElseThrow(() -> new IllegalArgumentException("Fornecedor nao encontrado no tenant."));
        }
    }

    private Map<Long, PlanoContasGerencial> mapContasPorId(Long empresaId, Long filialId, boolean somenteAtivas) {
        List<PlanoContasGerencial> list = somenteAtivas
                ? planoContasRepository.findByEmpresaIdAndFilialIdAndAtivoTrueOrderByOrdemExibicaoAscCodigoAsc(
                        empresaId, filialId)
                : planoContasRepository.findByEmpresaIdAndFilialIdOrderByOrdemExibicaoAscCodigoAsc(
                        empresaId, filialId);
        Map<Long, PlanoContasGerencial> map = new HashMap<>();
        for (PlanoContasGerencial c : list) {
            map.put(c.getIdPlanoConta(), c);
        }
        return map;
    }

    private Map<Long, CentroCusto> mapCentrosPorId(Long empresaId, Long filialId, boolean somenteAtivos) {
        List<CentroCusto> list = somenteAtivos
                ? centroCustoRepository.findByEmpresaIdAndFilialIdAndAtivoTrueOrderByNomeAsc(empresaId, filialId)
                : centroCustoRepository.findByEmpresaIdAndFilialIdOrderByNomeAsc(empresaId, filialId);
        Map<Long, CentroCusto> map = new HashMap<>();
        for (CentroCusto c : list) {
            map.put(c.getIdCentroCusto(), c);
        }
        return map;
    }

    private RegraClassificacaoResponseDTO toRuleResponse(
            RegraClassificacaoFinanceira r,
            Map<Long, PlanoContasGerencial> contas,
            Map<Long, CentroCusto> centros) {
        PlanoContasGerencial conta = contas.get(r.getPlanoContaId());
        CentroCusto cc = r.getCentroCustoId() == null ? null : centros.get(r.getCentroCustoId());
        return new RegraClassificacaoResponseDTO(
                r.getIdRegraClassificacao(),
                r.getNome(),
                r.getOrigemTipo(),
                r.getTipoDespesaId(),
                r.getCategoriaId(),
                r.getFormaPagamentoId(),
                r.getFornecedorId(),
                r.getPalavraChave(),
                r.getPlanoContaId(),
                conta == null ? null : conta.getCodigo(),
                conta == null ? null : conta.getNome(),
                r.getCentroCustoId(),
                cc == null ? null : cc.getCodigo(),
                cc == null ? null : cc.getNome(),
                r.getPrioridade(),
                r.isAtivo(),
                r.getCriadoEm(),
                r.getAtualizadoEm());
    }

    private void registrarAuditoriaRegra(
            String evento,
            RegraClassificacaoFinanceira r,
            String anterior,
            String novo,
            String justificativa,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("idRegra", r.getIdRegraClassificacao());
        metadata.put("nome", r.getNome());
        metadata.put("origemTipo", r.getOrigemTipo().name());
        metadata.put("planoContaId", r.getPlanoContaId());
        metadata.put("centroCustoId", r.getCentroCustoId());
        metadata.put("prioridade", r.getPrioridade());
        metadata.put("ativo", r.isAtivo());
        financialAuditTrailService.record(
                evento,
                "RegraClassificacaoFinanceira",
                r.getIdRegraClassificacao(),
                privacyGuardService.currentUserLogin(request),
                anterior,
                novo,
                metadata,
                empresaId,
                filialId);
        auditTrailService.registrarPrivacidade(
                evento,
                "RegraClassificacaoFinanceira",
                r.getIdRegraClassificacao(),
                "Financeiro",
                anterior,
                novo,
                request,
                justificativa);
    }

    private boolean isVendaFinalizada(Venda v) {
        return v.getStatus() == 1
                && (v.getStatusOperacional() == null || STATUS_VENDA_FINALIZADA.contains(v.getStatusOperacional()));
    }

    private LocalDate effectiveDespesaCompetencia(Despesa d) {
        if (d.getDataCompetencia() != null) return d.getDataCompetencia();
        if (d.getDataEmissao() != null) return d.getDataEmissao();
        return d.getDataVencimento();
    }

    private LocalDate effectiveContaPagarCompetencia(ContaPagar cp) {
        if (cp.getDataCompetencia() != null) return cp.getDataCompetencia();
        if (cp.getDataEmissao() != null) return cp.getDataEmissao();
        if (cp.getCriadoEm() != null) return cp.getCriadoEm().toLocalDate();
        return cp.getVencimento();
    }

    private LocalDate effectiveContaReceberCompetencia(ContaReceber cr) {
        if (cr.getDataCompetencia() != null) return cr.getDataCompetencia();
        if (cr.getDataEmissao() != null) return cr.getDataEmissao();
        if (cr.getCriadoEm() != null) return cr.getCriadoEm().toLocalDate();
        return cr.getVencimento();
    }

    private LocalDate effectiveVendaCompetencia(Venda v) {
        if (v.getDataCompetencia() != null) return v.getDataCompetencia();
        if (v.getFinalizadoEm() != null) return v.getFinalizadoEm().toLocalDate();
        return v.getDataVenda() == null ? null : v.getDataVenda().toLocalDate();
    }

    private LocalDate effectiveNfeCompetencia(NotaFiscalEntrada nf) {
        if (nf.getDataCompetencia() != null) return nf.getDataCompetencia();
        if (nf.getDataEntrada() != null) return nf.getDataEntrada().toLocalDate();
        return nf.getDataEmissao() == null ? null : nf.getDataEmissao().toLocalDate();
    }

    private void addCompetencia(Set<YearMonth> set, LocalDate date) {
        if (date != null) {
            set.add(YearMonth.from(date));
        }
    }

    private int registrarObservacao(List<String> observacoes, String msg) {
        if (observacoes.size() < MAX_OBSERVACOES) {
            observacoes.add(msg);
            return 0;
        }
        return 1;
    }

    private void requireTenant(Long empresaId, Long filialId) {
        if (empresaId == null || filialId == null) {
            throw new IllegalArgumentException("Empresa e filial ativas sao obrigatorias.");
        }
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String resumoRegra(RegraClassificacaoFinanceira r) {
        return "nome=" + r.getNome()
                + ";origem=" + r.getOrigemTipo()
                + ";planoConta=" + r.getPlanoContaId()
                + ";prioridade=" + r.getPrioridade()
                + ";ativo=" + r.isAtivo();
    }

    private record ContextoClassificacao(
            List<RegraClassificacaoFinanceira> regras,
            Map<Long, PlanoContasGerencial> contasAtivas,
            Map<String, PlanoContasGerencial> contasPorCodigo,
            Map<Long, CentroCusto> centrosAtivos,
            Map<Long, Categoria> categoriasPorId) {
    }

    private record SugestaoClassificacao(Long planoContaId, String planoContaNome, Long centroCustoId) {
        private static final SugestaoClassificacao VAZIA = new SugestaoClassificacao(null, null, null);
    }
}
