package Service;

import DTO.RelatorioFinanceiroDTO;
import Model.CaixaMovimentacao;
import Model.ContaPagar;
import Model.ContaReceber;
import Model.Despesa;
import Model.FinanceiroStatus;
import Model.ProdutoVenda;
import Model.TipoDespesa;
import Model.TipoDevolucao;
import Model.Venda;
import Model.VendaDevolucao;
import Model.VendaDevolucaoItem;
import Model.VendaStatus;
import Repository.CaixaMovimentacaoRepository;
import Repository.ContaPagarRepository;
import Repository.ContaReceberRepository;
import Repository.DespesaRepository;
import Repository.TipoDespesaRepository;
import Repository.VendaDevolucaoRepository;
import Repository.VendaRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class FinanceiroService {

    private static final Set<VendaStatus> STATUS_VENDA_FINALIZADA = EnumSet.of(
            VendaStatus.PAGA,
            VendaStatus.FINALIZADA,
            VendaStatus.TROCA_PARCIAL,
            VendaStatus.TROCA_TOTAL,
            VendaStatus.DEVOLVIDA);

    @Autowired
    private DespesaRepository despesaRepository;
    @Autowired
    private VendaRepository vendaRepository;
    @Autowired
    private VendaDevolucaoRepository vendaDevolucaoRepository;
    @Autowired
    private TipoDespesaRepository tipoDespesaRepository;
    @Autowired
    private ContaReceberRepository contaReceberRepository;
    @Autowired
    private ContaPagarRepository contaPagarRepository;
    @Autowired
    private CaixaMovimentacaoRepository caixaMovimentacaoRepository;
    @Autowired
    private TenantScopeService tenantScopeService;

    @Transactional
    public Despesa salvarDespesa(Despesa despesa) {
        HttpServletRequest req = currentRequest();
        return salvarDespesa(despesa, tenantScopeService.empresaId(req), tenantScopeService.filialId(req));
    }

    @Transactional
    public Despesa salvarDespesa(Despesa despesa, Long empresaId, Long filialId) {
        requireTenant(empresaId, filialId);
        despesa.setEmpresaId(empresaId);
        despesa.setFilialId(filialId);
        return despesaRepository.save(despesa);
    }

    @Transactional(readOnly = true)
    public List<Despesa> listarDespesas() {
        HttpServletRequest req = currentRequest();
        return listarDespesas(tenantScopeService.empresaId(req), tenantScopeService.filialId(req));
    }

    @Transactional(readOnly = true)
    public List<Despesa> listarDespesas(Long empresaId, Long filialId) {
        requireTenant(empresaId, filialId);
        return despesaRepository.findByEmpresaIdAndFilialIdOrderByDataVencimentoDesc(empresaId, filialId);
    }

    @Transactional
    public TipoDespesa salvarTipoDespesa(TipoDespesa td) {
        HttpServletRequest req = currentRequest();
        return salvarTipoDespesa(td, tenantScopeService.empresaId(req), tenantScopeService.filialId(req));
    }

    @Transactional
    public TipoDespesa salvarTipoDespesa(TipoDespesa td, Long empresaId, Long filialId) {
        requireTenant(empresaId, filialId);
        if (td.getIdTipoDespesa() != null) {
            TipoDespesa existente = tipoDespesaRepository.findVisibleById(td.getIdTipoDespesa(), empresaId, filialId)
                    .orElseThrow(() -> new IllegalArgumentException("Tipo de despesa nao encontrado no tenant."));
            if (existente.getEmpresaId() == null) {
                throw new AccessDeniedException("Tipos globais do catalogo nao podem ser alterados pelo tenant.");
            }
        }
        td.setEmpresaId(empresaId);
        td.setFilialId(filialId);
        return tipoDespesaRepository.save(td);
    }

    @Transactional(readOnly = true)
    public List<TipoDespesa> listarTiposDespesa() {
        HttpServletRequest req = currentRequest();
        return listarTiposDespesa(tenantScopeService.empresaId(req), tenantScopeService.filialId(req));
    }

    @Transactional(readOnly = true)
    public List<TipoDespesa> listarTiposDespesa(Long empresaId, Long filialId) {
        requireTenant(empresaId, filialId);
        return tipoDespesaRepository.findVisibleToTenant(empresaId, filialId);
    }

    @Transactional(readOnly = true)
    public RelatorioFinanceiroDTO gerarRelatorio(LocalDate inicio, LocalDate fim) {
        HttpServletRequest req = currentRequest();
        return gerarRelatorio(inicio, fim, tenantScopeService.empresaId(req), tenantScopeService.filialId(req));
    }

    @Transactional(readOnly = true)
    public RelatorioFinanceiroDTO gerarRelatorio(LocalDate inicio, LocalDate fim, Long empresaId, Long filialId) {
        requireTenant(empresaId, filialId);
        if (inicio == null || fim == null || inicio.isAfter(fim)) {
            throw new IllegalArgumentException("Periodo do relatorio financeiro invalido.");
        }
        LocalDateTime inicioTime = inicio.atStartOfDay();
        LocalDateTime fimTime = fim.atTime(LocalTime.MAX);

        List<Venda> vendas = vendaRepository.findByEmpresaIdAndFilialIdAndDataVendaBetweenAndStatus(
                        empresaId, filialId, inicioTime, fimTime, 1).stream()
                .filter(this::isVendaFinalizada)
                .toList();

        Map<Long, Venda> vendasPorId = new HashMap<>();
        BigDecimal receitaBruta = BigDecimal.ZERO;
        BigDecimal cmvBruto = BigDecimal.ZERO;

        for (Venda v : vendas) {
            vendasPorId.put(v.getIdVenda(), v);
            receitaBruta = receitaBruta.add(nvl(v.getValorTotal()));
            Hibernate.initialize(v.getItens());
            if (v.getItens() != null) {
                for (ProdutoVenda item : v.getItens()) {
                    cmvBruto = cmvBruto.add(nvl(item.getCustoUnitario()).multiply(BigDecimal.valueOf(item.getQuantidade())));
                }
            }
        }

        List<VendaDevolucao> devolucoes = vendaDevolucaoRepository
                .findByEmpresaIdAndFilialIdAndCriadoEmBetweenOrderByCriadoEmDesc(
                        empresaId, filialId, inicioTime, fimTime);

        Set<Long> vendasFaltantes = new HashSet<>();
        for (VendaDevolucao dev : devolucoes) {
            if (dev.getVendaOriginalId() != null && !vendasPorId.containsKey(dev.getVendaOriginalId())) {
                vendasFaltantes.add(dev.getVendaOriginalId());
            }
        }
        if (!vendasFaltantes.isEmpty()) {
            Specification<Venda> missingSpec = (root, query, cb) -> cb.and(
                    cb.equal(root.get("empresaId"), empresaId),
                    cb.equal(root.get("filialId"), filialId),
                    root.get("idVenda").in(vendasFaltantes));
            for (Venda v : vendaRepository.findAll(missingSpec)) {
                vendasPorId.put(v.getIdVenda(), v);
            }
        }

        BigDecimal totalDevolucoes = BigDecimal.ZERO;
        BigDecimal cmvDevolvido = BigDecimal.ZERO;
        for (VendaDevolucao dev : devolucoes) {
            if (dev.getTipo() == TipoDevolucao.TROCA) {
                continue;
            }
            totalDevolucoes = totalDevolucoes.add(nvl(dev.getValorAjuste()).max(BigDecimal.ZERO));
            Hibernate.initialize(dev.getItens());
            if (dev.getItens() != null) {
                Venda origem = vendasPorId.get(dev.getVendaOriginalId());
                for (VendaDevolucaoItem item : dev.getItens()) {
                    BigDecimal custoUnit = resolverCustoDevolucao(item, origem);
                    if (custoUnit != null && custoUnit.compareTo(BigDecimal.ZERO) > 0 && item.getQuantidade() != null) {
                        cmvDevolvido = cmvDevolvido.add(custoUnit.multiply(BigDecimal.valueOf(item.getQuantidade())));
                    }
                }
            }
        }

        BigDecimal receitaLiquida = receitaBruta.subtract(totalDevolucoes);
        BigDecimal cmvLiquido = cmvBruto.subtract(cmvDevolvido).max(BigDecimal.ZERO);

        List<Despesa> despesasPeriodo = despesaRepository.findAll(despesaPeriodoSpec(empresaId, filialId, inicio, fim));
        BigDecimal totalDespesas = BigDecimal.ZERO;
        BigDecimal saidasCaixaDespesas = BigDecimal.ZERO;
        for (Despesa d : despesasPeriodo) {
            if (d.getStatus() == 0) {
                continue;
            }
            LocalDate comp = d.getDataCompetencia() != null ? d.getDataCompetencia() : d.getDataVencimento();
            if (within(comp, inicio, fim)) {
                totalDespesas = totalDespesas.add(nvl(d.getValor()));
            }
            if (within(d.getDataPagamento(), inicio, fim)) {
                saidasCaixaDespesas = saidasCaixaDespesas.add(nvl(d.getValor()));
            }
        }

        List<ContaPagar> contasPagarPeriodo = contaPagarRepository.findAll(
                contaPagarPeriodoSpec(empresaId, filialId, inicio, fim, inicioTime, fimTime));
        BigDecimal saidasCaixaPagar = BigDecimal.ZERO;
        for (ContaPagar cp : contasPagarPeriodo) {
            if (cp.getStatus() == FinanceiroStatus.CANCELADO || cp.getStatus() == FinanceiroStatus.DEVOLVIDO) {
                continue;
            }
            LocalDate comp = cp.getDataCompetencia() != null
                    ? cp.getDataCompetencia()
                    : (cp.getCriadoEm() != null ? cp.getCriadoEm().toLocalDate() : cp.getVencimento());
            if (within(comp, inicio, fim)) {
                totalDespesas = totalDespesas.add(nvl(cp.getValor()));
            }
            if (cp.getStatus() == FinanceiroStatus.PAGO && cp.getPagoEm() != null
                    && within(cp.getPagoEm().toLocalDate(), inicio, fim)) {
                saidasCaixaPagar = saidasCaixaPagar.add(nvl(cp.getValor()));
            }
        }

        BigDecimal entradasCaixa = BigDecimal.ZERO;
        Set<Long> vendasRecebidas = new HashSet<>();
        for (ContaReceber cr : contaReceberRepository.findAll(
                contaReceberCaixaSpec(empresaId, filialId, inicioTime, fimTime))) {
            if (cr.getStatus() == FinanceiroStatus.CANCELADO) {
                continue;
            }
            if (cr.getPagoEm() != null
                    && within(cr.getPagoEm().toLocalDate(), inicio, fim)
                    && nvl(cr.getValorPago()).compareTo(BigDecimal.ZERO) > 0) {
                entradasCaixa = entradasCaixa.add(nvl(cr.getValorPago()));
                vendasRecebidas.add(cr.getVendaId());
            }
        }

        List<CaixaMovimentacao> movimentacoesPeriodo = caixaMovimentacaoRepository
                .findByCaixaSessaoEmpresaIdAndCaixaSessaoFilialIdAndCriadoEmBetweenOrderByCriadoEmDesc(
                        empresaId, filialId, inicioTime, fimTime);
        Set<Long> vendasCandidatasFallback = new HashSet<>();
        for (CaixaMovimentacao mov : movimentacoesPeriodo) {
            if ("VENDA".equalsIgnoreCase(mov.getTipo()) && mov.getReferenciaId() != null
                    && !vendasRecebidas.contains(mov.getReferenciaId())) {
                vendasCandidatasFallback.add(mov.getReferenciaId());
            }
        }
        Set<Long> vendasComContaReceber = carregarVendasComContaReceber(
                vendasCandidatasFallback, empresaId, filialId);

        for (CaixaMovimentacao mov : movimentacoesPeriodo) {
            if (!"VENDA".equalsIgnoreCase(mov.getTipo()) || mov.getCriadoEm() == null) {
                continue;
            }
            if (!within(mov.getCriadoEm().toLocalDate(), inicio, fim)) {
                continue;
            }
            Long vendaId = mov.getReferenciaId();
            if (vendaId != null && (vendasRecebidas.contains(vendaId) || vendasComContaReceber.contains(vendaId))) {
                continue;
            }
            entradasCaixa = entradasCaixa.add(nvl(mov.getValor()));
            if (vendaId != null) {
                vendasRecebidas.add(vendaId);
            }
        }

        BigDecimal lucro = receitaLiquida.subtract(cmvLiquido).subtract(totalDespesas);
        BigDecimal saldoCaixa = entradasCaixa.subtract(saidasCaixaDespesas).subtract(saidasCaixaPagar);
        return new RelatorioFinanceiroDTO(receitaLiquida, cmvLiquido, totalDespesas, lucro, saldoCaixa);
    }

    private Set<Long> carregarVendasComContaReceber(Set<Long> vendaIds, Long empresaId, Long filialId) {
        if (vendaIds == null || vendaIds.isEmpty()) {
            return Set.of();
        }
        Specification<ContaReceber> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("empresaId"), empresaId),
                cb.equal(root.get("filialId"), filialId),
                root.get("vendaId").in(vendaIds));
        Set<Long> result = new HashSet<>();
        for (ContaReceber cr : contaReceberRepository.findAll(spec)) {
            if (cr.getVendaId() != null) {
                result.add(cr.getVendaId());
            }
        }
        return result;
    }

    private BigDecimal resolverCustoDevolucao(VendaDevolucaoItem item, Venda vendaOrigem) {
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

    private boolean isVendaFinalizada(Venda venda) {
        return venda.getStatus() == 1
                && (venda.getStatusOperacional() == null
                        || STATUS_VENDA_FINALIZADA.contains(venda.getStatusOperacional()));
    }

    private boolean within(LocalDate date, LocalDate inicio, LocalDate fim) {
        return date != null && !date.isBefore(inicio) && !date.isAfter(fim);
    }

    private Specification<Despesa> despesaPeriodoSpec(
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
                                cb.between(root.get("dataVencimento"), inicio, fim)),
                        cb.between(root.get("dataPagamento"), inicio, fim)));
    }

    private Specification<ContaPagar> contaPagarPeriodoSpec(
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
                                cb.between(root.get("criadoEm"), inicioTime, fimTime)),
                        cb.and(cb.isNull(root.get("dataCompetencia")),
                                cb.isNull(root.get("criadoEm")),
                                cb.between(root.get("vencimento"), inicio, fim)),
                        cb.between(root.get("pagoEm"), inicioTime, fimTime)));
    }

    private Specification<ContaReceber> contaReceberCaixaSpec(
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

    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }

    private void requireTenant(Long empresaId, Long filialId) {
        if (empresaId == null || filialId == null) {
            throw new AccessDeniedException("Empresa e filial ativas sao obrigatorias.");
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
