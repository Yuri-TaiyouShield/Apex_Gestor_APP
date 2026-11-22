package Service;

import DTO.BaixaTituloFinanceiroRequestDTO;
import DTO.ConciliacaoItemResponseDTO;
import DTO.ConciliacaoManualRequestDTO;
import DTO.ConciliacaoResumoResponseDTO;
import DTO.ContaFinanceiraRequestDTO;
import DTO.ContaFinanceiraResponseDTO;
import DTO.DiagnosticoLiquidezDTO;
import DTO.DreGerencialResponseDTO;
import DTO.EstornoMovimentacaoRequestDTO;
import DTO.FluxoCaixaCenarioProjecaoDTO;
import DTO.FluxoCaixaDiaCalendarioDTO;
import DTO.FluxoCaixaPainelResponseDTO;
import DTO.FluxoCaixaPrevistoItemDTO;
import DTO.FluxoCaixaSimulacaoRequestDTO;
import DTO.GerarParcelasRequestDTO;
import DTO.ImportacaoExtratoRequestDTO;
import DTO.MovimentacaoFinanceiraResponseDTO;
import DTO.TransferenciaInternaRequestDTO;
import Model.CaixaMovimentacao;
import Model.CaixaSessao;
import Model.CentroCusto;
import Model.ClassificacaoCusto;
import Model.Cliente;
import Model.ConciliacaoFinanceiraItem;
import Model.ContaFinanceira;
import Model.ContaPagar;
import Model.ContaReceber;
import Model.Despesa;
import Model.Filial;
import Model.FinanceiroStatus;
import Model.Fornecedor;
import Model.GrupoDreGerencial;
import Model.ModalidadeRecebivel;
import Model.MovimentacaoFinanceira;
import Model.NaturezaContaGerencial;
import Model.OrigemMovimentacaoFinanceira;
import Model.PlanoContasGerencial;
import Model.StatusConciliacaoFinanceira;
import Model.TipoContaFinanceira;
import Model.TipoContaGerencial;
import Model.TipoDespesa;
import Model.TipoMovimentacaoFinanceira;
import Repository.CaixaMovimentacaoRepository;
import Repository.CaixaSessaoRepository;
import Repository.CategoriaRepository;
import Repository.CentroCustoRepository;
import Repository.ClienteRepository;
import Repository.ConciliacaoFinanceiraItemRepository;
import Repository.ContaFinanceiraRepository;
import Repository.ContaPagarRepository;
import Repository.ContaReceberRepository;
import Repository.DespesaRepository;
import Repository.FilialRepository;
import Repository.FormaPagamentoRepository;
import Repository.FornecedorRepository;
import Repository.MovimentacaoFinanceiraRepository;
import Repository.PlanoContasGerencialRepository;
import Repository.TipoDespesaRepository;
import Repository.VendaRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FluxoCaixaService {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final String TAG_TAXA_DEDUZIDA_ORIGEM = "[TAXA-DEDUZIDA-ORIGEM:CR-";
    private static final Pattern OFX_TRN_PATTERN = Pattern.compile("<STMTTRN>(.*?)</STMTTRN>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private final ContaFinanceiraRepository contaFinanceiraRepository;
    private final MovimentacaoFinanceiraRepository movimentacaoRepository;
    private final ConciliacaoFinanceiraItemRepository conciliacaoRepository;
    private final ContaPagarRepository contaPagarRepository;
    private final ContaReceberRepository contaReceberRepository;
    private final DespesaRepository despesaRepository;
    private final TipoDespesaRepository tipoDespesaRepository;
    private final CaixaSessaoRepository caixaSessaoRepository;
    private final CaixaMovimentacaoRepository caixaMovimentacaoRepository;
    private final PlanoContasGerencialRepository planoContasRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final ClienteRepository clienteRepository;
    private final FornecedorRepository fornecedorRepository;
    private final CategoriaRepository categoriaRepository;
    private final FormaPagamentoRepository formaPagamentoRepository;
    private final VendaRepository vendaRepository;
    private final FilialRepository filialRepository;
    private final DreGerencialService dreGerencialService;
    private final FinancialAuditTrailService financialAuditTrailService;
    private final AuditTrailService auditTrailService;
    private final PrivacyGuardService privacyGuardService;

    public FluxoCaixaService(
            ContaFinanceiraRepository contaFinanceiraRepository,
            MovimentacaoFinanceiraRepository movimentacaoRepository,
            ConciliacaoFinanceiraItemRepository conciliacaoRepository,
            ContaPagarRepository contaPagarRepository,
            ContaReceberRepository contaReceberRepository,
            DespesaRepository despesaRepository,
            TipoDespesaRepository tipoDespesaRepository,
            CaixaSessaoRepository caixaSessaoRepository,
            CaixaMovimentacaoRepository caixaMovimentacaoRepository,
            PlanoContasGerencialRepository planoContasRepository,
            CentroCustoRepository centroCustoRepository,
            ClienteRepository clienteRepository,
            FornecedorRepository fornecedorRepository,
            CategoriaRepository categoriaRepository,
            FormaPagamentoRepository formaPagamentoRepository,
            VendaRepository vendaRepository,
            FilialRepository filialRepository,
            DreGerencialService dreGerencialService,
            FinancialAuditTrailService financialAuditTrailService,
            AuditTrailService auditTrailService,
            PrivacyGuardService privacyGuardService) {
        this.contaFinanceiraRepository = contaFinanceiraRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.conciliacaoRepository = conciliacaoRepository;
        this.contaPagarRepository = contaPagarRepository;
        this.contaReceberRepository = contaReceberRepository;
        this.despesaRepository = despesaRepository;
        this.tipoDespesaRepository = tipoDespesaRepository;
        this.caixaSessaoRepository = caixaSessaoRepository;
        this.caixaMovimentacaoRepository = caixaMovimentacaoRepository;
        this.planoContasRepository = planoContasRepository;
        this.centroCustoRepository = centroCustoRepository;
        this.clienteRepository = clienteRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.categoriaRepository = categoriaRepository;
        this.formaPagamentoRepository = formaPagamentoRepository;
        this.vendaRepository = vendaRepository;
        this.filialRepository = filialRepository;
        this.dreGerencialService = dreGerencialService;
        this.financialAuditTrailService = financialAuditTrailService;
        this.auditTrailService = auditTrailService;
        this.privacyGuardService = privacyGuardService;
    }

    // =========================================================================
    // 1. Contas Financeiras (Caixa Fisico, Bancos, Carteiras, Operadoras Cartao)
    // =========================================================================

    @Transactional
    public List<ContaFinanceiraResponseDTO> inicializarContasPadrao(
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        garantirContasePadraoInterno(empresaId, filialId);
        List<ContaFinanceiraResponseDTO> lista = listarContas(true, false, empresaId, filialId);
        registrarAuditoria("CONTAS_FINANCEIRAS_BOOTSTRAP", "ContaFinanceira", null,
                "contas=" + lista.size(), "Inicializacao idempotente de contas financeiras padrao.",
                empresaId, filialId, request);
        return lista;
    }

    @Transactional(readOnly = true)
    public List<ContaFinanceiraResponseDTO> listarContas(
            Boolean somenteAtivas,
            Boolean consolidarFiliais,
            Long empresaId,
            Long filialId) {
        requireTenant(empresaId, filialId);
        Map<Long, String> nomesFiliais = mapaNomesFiliais(empresaId, filialId);
        List<ContaFinanceira> contas;
        if (Boolean.TRUE.equals(consolidarFiliais)) {
            contas = contaFinanceiraRepository.findByEmpresaIdAndAtivoTrueOrderByFilialIdAscPadraoDescNomeAsc(empresaId);
        } else if (Boolean.TRUE.equals(somenteAtivas)) {
            contas = contaFinanceiraRepository.findByEmpresaIdAndFilialIdAndAtivoTrueOrderByPadraoDescNomeAsc(empresaId, filialId);
        } else {
            contas = contaFinanceiraRepository.findByEmpresaIdAndFilialIdOrderByPadraoDescNomeAsc(empresaId, filialId);
        }
        return contas.stream()
                .map(c -> toContaResponse(c, nomesFiliais.getOrDefault(c.getFilialId(), "Filial #" + c.getFilialId())))
                .toList();
    }

    @Transactional
    public ContaFinanceiraResponseDTO criarConta(
            ContaFinanceiraRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        String codigo = normalizeRequired(body.codigo(), "Codigo da conta financeira e obrigatorio.").toUpperCase(Locale.ROOT);
        String nome = normalizeRequired(body.nome(), "Nome da conta financeira e obrigatorio.");
        if (body.tipoConta() == null) {
            throw new IllegalArgumentException("Tipo da conta financeira e obrigatorio.");
        }
        contaFinanceiraRepository.findByCodigoAndEmpresaIdAndFilialId(codigo, empresaId, filialId)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ja existe conta financeira com o codigo " + codigo + " nesta filial.");
                });

        BigDecimal saldoIni = scale2(body.saldoInicial());
        ContaFinanceira conta = new ContaFinanceira();
        conta.setEmpresaId(empresaId);
        conta.setFilialId(filialId);
        conta.setCodigo(codigo);
        conta.setNome(nome);
        conta.setTipoConta(body.tipoConta());
        conta.setBancoCodigo(trimToNull(body.bancoCodigo()));
        conta.setBancoNome(trimToNull(body.bancoNome()));
        conta.setAgencia(trimToNull(body.agencia()));
        conta.setNumeroConta(trimToNull(body.numeroConta()));
        conta.setSaldoInicial(saldoIni);
        conta.setSaldoAtual(saldoIni);
        conta.setDataSaldoInicial(body.dataSaldoInicial() != null ? body.dataSaldoInicial() : LocalDate.now());
        conta.setPadrao(Boolean.TRUE.equals(body.padrao()));
        conta.setAtivo(body.ativo() == null || body.ativo());
        conta.setCriadoEm(LocalDateTime.now());

        if (conta.isPadrao()) {
            desmarcarOutrasPadrao(empresaId, filialId, null);
        }

        ContaFinanceira salva = contaFinanceiraRepository.save(conta);
        registrarAuditoria("CONTA_FINANCEIRA_CRIADA", "ContaFinanceira", salva.getIdContaFinanceira(),
                "codigo=" + salva.getCodigo() + ";saldoInicial=" + salva.getSaldoInicial(),
                "Criacao de conta financeira/bancaria.", empresaId, filialId, request);
        Map<Long, String> nomesFiliais = mapaNomesFiliais(empresaId, filialId);
        return toContaResponse(salva, nomesFiliais.getOrDefault(filialId, "Filial #" + filialId));
    }

    @Transactional
    public ContaFinanceiraResponseDTO atualizarConta(
            Long id,
            ContaFinanceiraRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        ContaFinanceira existente = contaFinanceiraRepository
                .findByIdContaFinanceiraAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Conta financeira nao encontrada no tenant."));

        String codigo = normalizeRequired(body.codigo(), "Codigo da conta financeira e obrigatorio.").toUpperCase(Locale.ROOT);
        String nome = normalizeRequired(body.nome(), "Nome da conta financeira e obrigatorio.");
        contaFinanceiraRepository.findByCodigoAndEmpresaIdAndFilialId(codigo, empresaId, filialId)
                .ifPresent(other -> {
                    if (!Objects.equals(other.getIdContaFinanceira(), id)) {
                        throw new IllegalArgumentException("Ja existe outra conta financeira com o codigo " + codigo + ".");
                    }
                });

        BigDecimal diffSaldoIni = body.saldoInicial() != null
                ? scale2(body.saldoInicial()).subtract(nvl(existente.getSaldoInicial()))
                : BigDecimal.ZERO;

        existente.setCodigo(codigo);
        existente.setNome(nome);
        if (body.tipoConta() != null) {
            existente.setTipoConta(body.tipoConta());
        }
        existente.setBancoCodigo(trimToNull(body.bancoCodigo()));
        existente.setBancoNome(trimToNull(body.bancoNome()));
        existente.setAgencia(trimToNull(body.agencia()));
        existente.setNumeroConta(trimToNull(body.numeroConta()));
        if (body.saldoInicial() != null) {
            existente.setSaldoInicial(scale2(body.saldoInicial()));
            existente.setSaldoAtual(scale2(nvl(existente.getSaldoAtual()).add(diffSaldoIni)));
        }
        if (body.dataSaldoInicial() != null) {
            existente.setDataSaldoInicial(body.dataSaldoInicial());
        }
        if (body.padrao() != null) {
            existente.setPadrao(body.padrao());
            if (body.padrao()) {
                desmarcarOutrasPadrao(empresaId, filialId, id);
            }
        }
        if (body.ativo() != null) {
            existente.setAtivo(body.ativo());
        }
        existente.setAtualizadoEm(LocalDateTime.now());

        ContaFinanceira salva = contaFinanceiraRepository.save(existente);
        registrarAuditoria("CONTA_FINANCEIRA_ATUALIZADA", "ContaFinanceira", salva.getIdContaFinanceira(),
                "codigo=" + salva.getCodigo() + ";saldoAtual=" + salva.getSaldoAtual(),
                "Atualizacao de conta financeira/bancaria.", empresaId, filialId, request);
        Map<Long, String> nomesFiliais = mapaNomesFiliais(empresaId, filialId);
        return toContaResponse(salva, nomesFiliais.getOrDefault(filialId, "Filial #" + filialId));
    }

    // =========================================================================
    // 2. Geracao de Titulos Parcelados, Recebiveis de Cartao e Recorrencias
    // =========================================================================

    @Transactional
    public List<FluxoCaixaPrevistoItemDTO> gerarParcelas(
            GerarParcelasRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        String natureza = normalizeRequired(body.natureza(), "Natureza (RECEBER ou PAGAR) e obrigatoria.")
                .toUpperCase(Locale.ROOT);
        String descricaoBase = normalizeRequired(body.descricao(), "Descricao do parcelamento e obrigatoria.");
        BigDecimal valorTotal = positive(body.valorTotal(), "Valor total deve ser maior que zero.");
        int qtd = Math.max(1, body.quantidadeParcelas());
        int intervalo = body.intervaloDias() != null ? Math.max(1, body.intervaloDias()) : 30;
        LocalDate primeiroVenc = Objects.requireNonNull(body.primeiroVencimento(), "Primeiro vencimento e obrigatorio.");
        LocalDate dataComp = body.dataCompetencia() != null ? body.dataCompetencia() : LocalDate.now();
        String grupoId = "PARC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);

        validarReferenciasParcelamento(body, empresaId, filialId);

        BigDecimal valorParcelaPadrao = valorTotal.divide(BigDecimal.valueOf(qtd), 2, RoundingMode.DOWN);
        BigDecimal acumulado = BigDecimal.ZERO;
        List<FluxoCaixaPrevistoItemDTO> gerados = new ArrayList<>(qtd);

        if ("RECEBER".equals(natureza)) {
            ModalidadeRecebivel mod = body.modalidadeRecebivel() != null
                    ? body.modalidadeRecebivel()
                    : ModalidadeRecebivel.DIRETO;
            BigDecimal taxaPct = nvl(body.taxaPercentualCartao()).max(BigDecimal.ZERO);
            int diasRepasse = body.prazoRepasseDias() != null && body.prazoRepasseDias() >= 0
                    ? body.prazoRepasseDias()
                    : 0;

            for (int i = 1; i <= qtd; i++) {
                BigDecimal valorBrutoParc = (i == qtd)
                        ? valorTotal.subtract(acumulado)
                        : valorParcelaPadrao;
                acumulado = acumulado.add(valorBrutoParc);

                BigDecimal valorTaxaParc = taxaPct.compareTo(BigDecimal.ZERO) > 0
                        ? valorBrutoParc.multiply(taxaPct).divide(CEM, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                BigDecimal valorLiquidoParc = valorBrutoParc.subtract(valorTaxaParc).max(BigDecimal.ZERO);

                LocalDate venc = primeiroVenc.plusDays((long) (i - 1) * intervalo);
                LocalDate repasse = diasRepasse > 0
                        ? dataComp.plusDays((long) i * diasRepasse)
                        : venc;

                ContaReceber cr = new ContaReceber();
                cr.setVendaId(body.vendaId());
                cr.setClienteId(body.clienteId());
                cr.setFormaPagamentoId(body.formaPagamentoId());
                cr.setEmpresaId(empresaId);
                cr.setFilialId(filialId);
                cr.setDescricao(descricaoBase + " (" + i + "/" + qtd + ")");
                cr.setValorBruto(scale2(valorBrutoParc));
                cr.setDesconto(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                cr.setValorTaxa(scale2(valorTaxaParc));
                cr.setTaxaPercentual(taxaPct);
                cr.setTaxaAntecipacao(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                cr.setValorLiquido(scale2(valorLiquidoParc));
                cr.setValorPago(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                cr.setSaldo(scale2(valorLiquidoParc));
                cr.setVencimento(venc);
                cr.setDataPrevistaRepasse(repasse);
                cr.setDataCompetencia(dataComp);
                cr.setDataEmissao(dataComp);
                cr.setStatus(FinanceiroStatus.ABERTO);
                cr.setCriadoEm(LocalDateTime.now());
                cr.setPlanoContaId(body.planoContaId());
                cr.setCentroCustoId(body.centroCustoId());
                cr.setContaFinanceiraId(body.contaFinanceiraId());
                cr.setNumeroParcela(i);
                cr.setTotalParcelas(qtd);
                cr.setGrupoParcelamento(grupoId);
                cr.setModalidadeRecebivel(mod);
                cr.setAntecipado(false);

                ContaReceber salva = contaReceberRepository.save(cr);
                gerados.add(toPrevistoReceber(salva, LocalDate.now(), Map.of(), Map.of(), Map.of()));
            }
        } else if ("PAGAR".equals(natureza)) {
            for (int i = 1; i <= qtd; i++) {
                BigDecimal valorParc = (i == qtd)
                        ? valorTotal.subtract(acumulado)
                        : valorParcelaPadrao;
                acumulado = acumulado.add(valorParc);
                LocalDate venc = primeiroVenc.plusDays((long) (i - 1) * intervalo);

                ContaPagar cp = new ContaPagar();
                cp.setFornecedorId(body.fornecedorId());
                cp.setCategoriaId(body.categoriaId());
                cp.setFormaPagamentoPrevistaId(body.formaPagamentoId());
                cp.setEmpresaId(empresaId);
                cp.setFilialId(filialId);
                cp.setDescricao(descricaoBase + " (" + i + "/" + qtd + ")");
                cp.setValor(scale2(valorParc));
                cp.setValorPago(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                cp.setSaldo(scale2(valorParc));
                cp.setVencimento(venc);
                cp.setDataCompetencia(dataComp);
                cp.setDataEmissao(dataComp);
                cp.setStatus(FinanceiroStatus.ABERTO);
                cp.setCriadoEm(LocalDateTime.now());
                cp.setPlanoContaId(body.planoContaId());
                cp.setCentroCustoId(body.centroCustoId());
                cp.setContaFinanceiraId(body.contaFinanceiraId());
                cp.setNumeroParcela(i);
                cp.setTotalParcelas(qtd);
                cp.setGrupoParcelamento(grupoId);
                cp.setRecorrente(Boolean.TRUE.equals(body.recorrente()));
                cp.setPeriodicidade(trimToNull(body.periodicidade()));

                ContaPagar salva = contaPagarRepository.save(cp);
                gerados.add(toPrevistoPagar(salva, LocalDate.now(), Map.of(), Map.of(), Map.of()));
            }
        } else {
            throw new IllegalArgumentException("Natureza invalida: utilize RECEBER ou PAGAR.");
        }

        registrarAuditoria("PARCELAMENTO_GERADO", "FluxoCaixa", null,
                "natureza=" + natureza + ";grupo=" + grupoId + ";parcelas=" + qtd + ";valorTotal=" + valorTotal,
                "Geracao de parcelamento no fluxo de caixa previsto.", empresaId, filialId, request);
        return gerados;
    }

    private void validarReferenciasParcelamento(GerarParcelasRequestDTO body, Long empresaId, Long filialId) {
        if (body.vendaId() != null) {
            vendaRepository.findByIdVendaAndEmpresaIdAndFilialId(body.vendaId(), empresaId, filialId)
                    .orElseThrow(() -> new EntityNotFoundException("Venda informada nao encontrada no tenant."));
        }
        if (body.clienteId() != null) {
            clienteRepository.findByIdClienteAndEmpresaIdAndFilialId(body.clienteId(), empresaId, filialId)
                    .orElseThrow(() -> new EntityNotFoundException("Cliente informado nao encontrado no tenant."));
        }
        if (body.fornecedorId() != null) {
            fornecedorRepository.findByIdFornecedorAndEmpresaIdAndFilialId(body.fornecedorId(), empresaId, filialId)
                    .orElseThrow(() -> new EntityNotFoundException("Fornecedor informado nao encontrado no tenant."));
        }
        if (body.categoriaId() != null) {
            categoriaRepository.findVisibleById(body.categoriaId(), empresaId, filialId)
                    .orElseThrow(() -> new EntityNotFoundException("Categoria informada nao encontrada no tenant."));
        }
        if (body.formaPagamentoId() != null) {
            formaPagamentoRepository.findVisibleById(body.formaPagamentoId(), empresaId, filialId)
                    .orElseThrow(() -> new EntityNotFoundException("Forma de pagamento informada nao encontrada no tenant."));
        }
        if (body.planoContaId() != null) {
            PlanoContasGerencial pc = planoContasRepository
                    .findByIdPlanoContaAndEmpresaIdAndFilialId(body.planoContaId(), empresaId, filialId)
                    .orElseThrow(() -> new EntityNotFoundException("Conta do plano de contas nao encontrada no tenant."));
            if (!pc.isAtivo() || !pc.isAceitaLancamento()) {
                throw new IllegalArgumentException("A conta do plano de contas deve estar ativa e ser analitica (aceitaLancamento=true).");
            }
        }
        if (body.centroCustoId() != null) {
            CentroCusto cc = centroCustoRepository
                    .findByIdCentroCustoAndEmpresaIdAndFilialId(body.centroCustoId(), empresaId, filialId)
                    .orElseThrow(() -> new EntityNotFoundException("Centro de custo nao encontrado no tenant."));
            if (!cc.isAtivo()) {
                throw new IllegalArgumentException("O centro de custo selecionado esta inativo.");
            }
        }
        if (body.contaFinanceiraId() != null) {
            ContaFinanceira cf = contaFinanceiraRepository
                    .findByIdContaFinanceiraAndEmpresaIdAndFilialId(body.contaFinanceiraId(), empresaId, filialId)
                    .orElseThrow(() -> new EntityNotFoundException("Conta financeira nao encontrada no tenant."));
            if (!cf.isAtivo()) {
                throw new IllegalArgumentException("A conta financeira selecionada esta inativa.");
            }
        }
    }

    // =========================================================================
    // 3. Baixas Parciais/Integrais, Liquidacao de Cartao e Antecipacao
    // =========================================================================

    @Transactional
    public MovimentacaoFinanceiraResponseDTO baixarContaReceber(
            Long idContaReceber,
            BaixaTituloFinanceiroRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        String idemKey = trimToNull(body.idempotencyKey());
        if (idemKey != null) {
            Optional<MovimentacaoFinanceira> existente = movimentacaoRepository
                    .findByIdempotencyKeyAndEmpresaIdAndFilialId(idemKey, empresaId, filialId);
            if (existente.isPresent()) {
                MovimentacaoFinanceira ex = existente.get();
                boolean mesmaOrigem = Objects.equals(ex.getOrigemId(), idContaReceber)
                        && (ex.getOrigemTipo() == OrigemMovimentacaoFinanceira.CONTA_RECEBER
                                || ex.getOrigemTipo() == OrigemMovimentacaoFinanceira.LIQUIDACAO_CARTAO
                                || ex.getOrigemTipo() == OrigemMovimentacaoFinanceira.ANTECIPACAO_RECEBIVEL)
                        && scale2(ex.getValorBruto()).compareTo(scale2(body.valorLiquidado())) == 0;
                if (!mesmaOrigem) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Idempotency-Key ja utilizada em outra operacao financeira.");
                }
                return toMovimentacaoResponse(ex, Map.of(), Map.of(), Map.of());
            }
        }

        ContaReceber cr = contaReceberRepository.findByIdContaReceberAndEmpresaIdAndFilialId(
                        idContaReceber, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Conta a receber nao encontrada no tenant."));

        if (cr.getStatus() == FinanceiroStatus.CANCELADO || cr.getStatus() == FinanceiroStatus.DEVOLVIDO) {
            throw new IllegalArgumentException("Nao e permitido baixar uma conta a receber cancelada ou devolvida.");
        }
        if (cr.getStatus() == FinanceiroStatus.PAGO && nvl(cr.getSaldo()).compareTo(BigDecimal.ZERO) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta conta a receber ja se encontra integralmente liquidada.");
        }

        BigDecimal saldoAberto = calcularSaldoAbertoReceber(cr);
        BigDecimal saldoBrutoAberto = nvl(cr.getValorBruto())
                .subtract(nvl(cr.getDesconto()))
                .subtract(nvl(cr.getValorPago()))
                .max(saldoAberto);
        BigDecimal valorInformado = positive(body.valorLiquidado(), "Valor liquidado deve ser maior que zero.");
        if (valorInformado.compareTo(saldoBrutoAberto) > 0) {
            throw new IllegalArgumentException("Valor liquidado (R$ " + scale2(valorInformado)
                    + ") excede o saldo em aberto do recebivel (R$ " + scale2(saldoBrutoAberto) + ").");
        }

        boolean primeiraBaixa = nvl(cr.getValorPago()).compareTo(BigDecimal.ZERO) == 0;
        BigDecimal taxaProvisionada = nvl(cr.getValorTaxa()).max(BigDecimal.ZERO);

        // Se o usuario baixou pelo saldo liquido ja descontado da taxa provisionada (ex: R$ 970,00 em titulo de R$ 1.000,00)
        // sem informar nova taxa, recompoe o bruto para evitar dupla deducao da taxa provisionada.
        BigDecimal valorBrutoBaixa = valorInformado;
        BigDecimal taxaOperadora;
        if (body.valorTaxa() != null) {
            taxaOperadora = body.valorTaxa().max(BigDecimal.ZERO);
        } else if (primeiraBaixa && taxaProvisionada.compareTo(BigDecimal.ZERO) > 0) {
            taxaOperadora = taxaProvisionada;
            if (valorInformado.compareTo(saldoAberto) == 0 && saldoBrutoAberto.compareTo(saldoAberto) > 0) {
                valorBrutoBaixa = saldoBrutoAberto;
            }
        } else {
            taxaOperadora = BigDecimal.ZERO;
        }

        BigDecimal taxaAntecip = nvl(body.taxaAntecipacao()).max(BigDecimal.ZERO);
        BigDecimal jurosDesc = nvl(body.valorJurosDesconto());
        boolean antecipacao = Boolean.TRUE.equals(body.antecipacaoRecebivel()) || taxaAntecip.compareTo(BigDecimal.ZERO) > 0;

        BigDecimal totalTaxasBaixa = taxaOperadora.add(taxaAntecip);
        BigDecimal valorLiquidoCaixa = valorBrutoBaixa.subtract(totalTaxasBaixa).add(jurosDesc);
        if (valorLiquidoCaixa.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("As taxas informadas nao podem superar o valor liquidado.");
        }

        ContaFinanceira contaDestino = resolverContaFinanceiraOperacao(
                body.contaFinanceiraId() != null ? body.contaFinanceiraId() : cr.getContaFinanceiraId(),
                empresaId,
                filialId);

        LocalDate dataLiq = body.dataLiquidacao() != null ? body.dataLiquidacao() : LocalDate.now();
        BigDecimal novoValorPago = nvl(cr.getValorPago()).add(valorBrutoBaixa);
        BigDecimal novoSaldoBruto = saldoBrutoAberto.subtract(valorBrutoBaixa).max(BigDecimal.ZERO);
        BigDecimal novoTaxaTotal = primeiraBaixa
                ? taxaOperadora
                : nvl(cr.getValorTaxa()).add(taxaOperadora);
        BigDecimal novoSaldo = novoSaldoBruto.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : novoSaldoBruto.subtract(taxaProvisionada.subtract(novoTaxaTotal).max(BigDecimal.ZERO)).max(BigDecimal.ZERO);

        cr.setValorPago(scale2(novoValorPago));
        cr.setSaldo(scale2(novoSaldo));
        cr.setValorTaxa(scale2(novoTaxaTotal));
        cr.setTaxaAntecipacao(scale2(nvl(cr.getTaxaAntecipacao()).add(taxaAntecip)));
        if (antecipacao) {
            cr.setAntecipado(true);
        }
        cr.setContaFinanceiraId(contaDestino.getIdContaFinanceira());
        cr.setPagoEm(dataLiq.atTime(LocalTime.now()));
        cr.setStatus(novoSaldo.compareTo(BigDecimal.ZERO) == 0 ? FinanceiroStatus.PAGO : FinanceiroStatus.PARCIAL);
        contaReceberRepository.save(cr);

        OrigemMovimentacaoFinanceira origemMov = antecipacao
                ? OrigemMovimentacaoFinanceira.ANTECIPACAO_RECEBIVEL
                : (cr.getModalidadeRecebivel() == ModalidadeRecebivel.CARTAO_CREDITO
                        || cr.getModalidadeRecebivel() == ModalidadeRecebivel.CARTAO_DEBITO
                        ? OrigemMovimentacaoFinanceira.LIQUIDACAO_CARTAO
                        : OrigemMovimentacaoFinanceira.CONTA_RECEBER);

        MovimentacaoFinanceira mov = new MovimentacaoFinanceira();
        mov.setEmpresaId(empresaId);
        mov.setFilialId(filialId);
        mov.setContaFinanceiraId(contaDestino.getIdContaFinanceira());
        mov.setTipoMovimentacao(TipoMovimentacaoFinanceira.ENTRADA);
        mov.setOrigemTipo(origemMov);
        mov.setOrigemId(cr.getIdContaReceber());
        mov.setDescricao((cr.getDescricao() != null ? cr.getDescricao() : "Recebivel #" + cr.getIdContaReceber())
                + (novoSaldo.compareTo(BigDecimal.ZERO) > 0 ? " [Baixa Parcial]" : " [Baixa Integral]")
                + (antecipacao ? " [Antecipado]" : ""));
        mov.setDocumentoReferencia("CR-" + cr.getIdContaReceber());
        mov.setDataMovimentacao(dataLiq);
        mov.setDataHoraRegistro(LocalDateTime.now());
        mov.setValorBruto(scale2(valorBrutoBaixa));
        mov.setValorTaxa(scale2(totalTaxasBaixa));
        mov.setValorJurosDesconto(scale2(jurosDesc));
        mov.setValorLiquido(scale2(valorLiquidoCaixa));
        mov.setPlanoContaId(cr.getPlanoContaId());
        mov.setCentroCustoId(cr.getCentroCustoId());
        mov.setIdempotencyKey(idemKey);
        mov.setCriadoPor(privacyGuardService.currentUserLogin(request));
        MovimentacaoFinanceira movSalva = movimentacaoRepository.save(mov);

        // Se houve taxa de operadora ou taxa de antecipacao na baixa, registra como Despesa Financeira (9.01) para a DRE,
        // marcada com TAG_TAXA_DEDUZIDA_ORIGEM (incluindo ID da movimentacao) para nao ser descontada em duplicidade no Caixa Realizado e poder ser estornada individualmente.
        if (totalTaxasBaixa.compareTo(BigDecimal.ZERO) > 0) {
            String tagOrigem = TAG_TAXA_DEDUZIDA_ORIGEM + cr.getIdContaReceber() + ":MOV-" + movSalva.getIdMovimentacaoFinanceira() + "] ";
            registrarDespesaFinanceiraAutomatica(
                    empresaId,
                    filialId,
                    dataLiq,
                    totalTaxasBaixa,
                    tagOrigem + (antecipacao ? "Taxa de antecipacao de recebivel #" : "Taxa de liquidacao de recebivel #") + cr.getIdContaReceber(),
                    cr.getCentroCustoId());
        }

        contaDestino.setSaldoAtual(scale2(nvl(contaDestino.getSaldoAtual()).add(valorLiquidoCaixa)));
        contaDestino.setAtualizadoEm(LocalDateTime.now());
        contaFinanceiraRepository.save(contaDestino);

        registrarAuditoria(
                antecipacao ? "RECEBIVEL_ANTECIPADO" : "CONTA_RECEBER_BAIXADA",
                "ContaReceber",
                cr.getIdContaReceber(),
                "valorBruto=" + valorBrutoBaixa + ";taxas=" + totalTaxasBaixa + ";liquido=" + valorLiquidoCaixa + ";saldoRestante=" + novoSaldo,
                "Liquidacao de conta a receber no fluxo de caixa.",
                empresaId,
                filialId,
                request);

        return toMovimentacaoResponse(movSalva, Map.of(contaDestino.getIdContaFinanceira(), contaDestino), Map.of(), Map.of());
    }

    @Transactional
    public MovimentacaoFinanceiraResponseDTO baixarContaPagar(
            Long idContaPagar,
            BaixaTituloFinanceiroRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        String idemKey = trimToNull(body.idempotencyKey());
        if (idemKey != null) {
            Optional<MovimentacaoFinanceira> existente = movimentacaoRepository
                    .findByIdempotencyKeyAndEmpresaIdAndFilialId(idemKey, empresaId, filialId);
            if (existente.isPresent()) {
                MovimentacaoFinanceira ex = existente.get();
                boolean mesmaOrigem = Objects.equals(ex.getOrigemId(), idContaPagar)
                        && ex.getOrigemTipo() == OrigemMovimentacaoFinanceira.CONTA_PAGAR
                        && scale2(ex.getValorBruto()).compareTo(scale2(body.valorLiquidado())) == 0;
                if (!mesmaOrigem) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Idempotency-Key ja utilizada em outra operacao financeira.");
                }
                return toMovimentacaoResponse(ex, Map.of(), Map.of(), Map.of());
            }
        }

        ContaPagar cp = contaPagarRepository.findByIdContaPagarAndEmpresaIdAndFilialId(
                        idContaPagar, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Conta a pagar nao encontrada no tenant."));

        if (cp.getStatus() == FinanceiroStatus.CANCELADO || cp.getStatus() == FinanceiroStatus.DEVOLVIDO) {
            throw new IllegalArgumentException("Nao e permitido baixar uma obrigacao cancelada ou devolvida.");
        }
        BigDecimal saldoAberto = calcularSaldoAbertoPagar(cp);
        if (cp.getStatus() == FinanceiroStatus.PAGO && saldoAberto.compareTo(BigDecimal.ZERO) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta obrigacao ja se encontra integralmente paga.");
        }

        BigDecimal valorPrincipalPago = positive(body.valorLiquidado(), "Valor pago deve ser maior que zero.");
        if (valorPrincipalPago.compareTo(saldoAberto) > 0) {
            throw new IllegalArgumentException("Valor pago (R$ " + scale2(valorPrincipalPago)
                    + ") excede o saldo em aberto da obrigacao (R$ " + scale2(saldoAberto) + ").");
        }

        BigDecimal taxaBancaria = nvl(body.valorTaxa()).max(BigDecimal.ZERO);
        BigDecimal jurosMultaOuDesconto = nvl(body.valorJurosDesconto());
        BigDecimal valorSaidaCaixa = valorPrincipalPago.add(taxaBancaria).add(jurosMultaOuDesconto);
        if (valorSaidaCaixa.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor efetivo de desembolso deve ser maior que zero.");
        }

        ContaFinanceira contaOrigem = resolverContaFinanceiraOperacao(
                body.contaFinanceiraId() != null ? body.contaFinanceiraId() : cp.getContaFinanceiraId(),
                empresaId,
                filialId);

        LocalDate dataLiq = body.dataLiquidacao() != null ? body.dataLiquidacao() : LocalDate.now();
        BigDecimal novoPago = nvl(cp.getValorPago()).add(valorPrincipalPago);
        BigDecimal novoSaldo = saldoAberto.subtract(valorPrincipalPago).max(BigDecimal.ZERO);

        cp.setValorPago(scale2(novoPago));
        cp.setSaldo(scale2(novoSaldo));
        cp.setContaFinanceiraId(contaOrigem.getIdContaFinanceira());
        cp.setPagoEm(dataLiq.atTime(LocalTime.now()));
        cp.setStatus(novoSaldo.compareTo(BigDecimal.ZERO) == 0 ? FinanceiroStatus.PAGO : FinanceiroStatus.PARCIAL);
        contaPagarRepository.save(cp);

        MovimentacaoFinanceira mov = new MovimentacaoFinanceira();
        mov.setEmpresaId(empresaId);
        mov.setFilialId(filialId);
        mov.setContaFinanceiraId(contaOrigem.getIdContaFinanceira());
        mov.setTipoMovimentacao(TipoMovimentacaoFinanceira.SAIDA);
        mov.setOrigemTipo(OrigemMovimentacaoFinanceira.CONTA_PAGAR);
        mov.setOrigemId(cp.getIdContaPagar());
        mov.setDescricao(cp.getDescricao() + (novoSaldo.compareTo(BigDecimal.ZERO) > 0 ? " [Pagamento Parcial]" : " [Pagamento Integral]"));
        mov.setDocumentoReferencia("CP-" + cp.getIdContaPagar());
        mov.setDataMovimentacao(dataLiq);
        mov.setDataHoraRegistro(LocalDateTime.now());
        mov.setValorBruto(scale2(valorPrincipalPago));
        mov.setValorTaxa(scale2(taxaBancaria));
        mov.setValorJurosDesconto(scale2(jurosMultaOuDesconto));
        mov.setValorLiquido(scale2(valorSaidaCaixa));
        mov.setPlanoContaId(cp.getPlanoContaId());
        mov.setCentroCustoId(cp.getCentroCustoId());
        mov.setIdempotencyKey(idemKey);
        mov.setCriadoPor(privacyGuardService.currentUserLogin(request));
        MovimentacaoFinanceira movSalva = movimentacaoRepository.save(mov);

        contaOrigem.setSaldoAtual(scale2(nvl(contaOrigem.getSaldoAtual()).subtract(valorSaidaCaixa)));
        contaOrigem.setAtualizadoEm(LocalDateTime.now());
        contaFinanceiraRepository.save(contaOrigem);

        registrarAuditoria(
                "CONTA_PAGAR_BAIXADA",
                "ContaPagar",
                cp.getIdContaPagar(),
                "valorPago=" + valorPrincipalPago + ";saidaCaixa=" + valorSaidaCaixa + ";saldoRestante=" + novoSaldo,
                "Pagamento de obrigacao no fluxo de caixa.",
                empresaId,
                filialId,
                request);

        return toMovimentacaoResponse(movSalva, Map.of(contaOrigem.getIdContaFinanceira(), contaOrigem), Map.of(), Map.of());
    }

    // =========================================================================
    // 4. Transferencias entre Contas Proprias (Sem inflar entradas/saidas consolidadas)
    // =========================================================================

    @Transactional
    public List<MovimentacaoFinanceiraResponseDTO> transferirEntreContas(
            TransferenciaInternaRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        if (Objects.equals(body.contaOrigemId(), body.contaDestinoId())) {
            throw new IllegalArgumentException("A conta de destino deve ser diferente da conta de origem.");
        }
        String idemKey = trimToNull(body.idempotencyKey());
        if (idemKey != null) {
            Optional<MovimentacaoFinanceira> prev = movimentacaoRepository
                    .findByIdempotencyKeyAndEmpresaIdAndFilialId(idemKey, empresaId, filialId);
            if (prev.isPresent()) {
                MovimentacaoFinanceira m1 = prev.get();
                boolean mesmaTransf = m1.getOrigemTipo() == OrigemMovimentacaoFinanceira.TRANSFERENCIA_INTERNA
                        && Objects.equals(m1.getContaFinanceiraId(), body.contaOrigemId())
                        && scale2(m1.getValorBruto()).compareTo(scale2(body.valor())) == 0;
                if (!mesmaTransf) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Idempotency-Key ja utilizada em outra operacao financeira.");
                }
                MovimentacaoFinanceira m2 = m1.getTransferenciaParId() == null
                        ? null
                        : movimentacaoRepository.findById(m1.getTransferenciaParId()).orElse(null);
                List<MovimentacaoFinanceiraResponseDTO> res = new ArrayList<>();
                res.add(toMovimentacaoResponse(m1, Map.of(), Map.of(), Map.of()));
                if (m2 != null) {
                    res.add(toMovimentacaoResponse(m2, Map.of(), Map.of(), Map.of()));
                }
                return res;
            }
        }

        ContaFinanceira origem = contaFinanceiraRepository
                .findByIdContaFinanceiraAndEmpresaIdAndFilialId(body.contaOrigemId(), empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Conta financeira de origem nao encontrada."));
        ContaFinanceira destino = contaFinanceiraRepository
                .findByIdContaFinanceiraAndEmpresaIdAndFilialId(body.contaDestinoId(), empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Conta financeira de destino nao encontrada."));

        if (!origem.isAtivo() || !destino.isAtivo()) {
            throw new IllegalArgumentException("Ambas as contas da transferencia devem estar ativas.");
        }

        BigDecimal valor = scale2(positive(body.valor(), "Valor da transferencia deve ser maior que zero."));
        LocalDate dataTransf = body.dataTransferencia() != null ? body.dataTransferencia() : LocalDate.now();
        String descBase = body.descricao() != null && !body.descricao().isBlank()
                ? body.descricao().trim()
                : "Transferencia entre contas proprias (" + origem.getCodigo() + " -> " + destino.getCodigo() + ")";
        String docRef = trimToNull(body.documentoReferencia());
        String usuario = privacyGuardService.currentUserLogin(request);

        MovimentacaoFinanceira saida = new MovimentacaoFinanceira();
        saida.setEmpresaId(empresaId);
        saida.setFilialId(filialId);
        saida.setContaFinanceiraId(origem.getIdContaFinanceira());
        saida.setTipoMovimentacao(TipoMovimentacaoFinanceira.TRANSFERENCIA_SAIDA);
        saida.setOrigemTipo(OrigemMovimentacaoFinanceira.TRANSFERENCIA_INTERNA);
        saida.setDescricao(descBase);
        saida.setDocumentoReferencia(docRef);
        saida.setDataMovimentacao(dataTransf);
        saida.setDataHoraRegistro(LocalDateTime.now());
        saida.setValorBruto(valor);
        saida.setValorTaxa(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        saida.setValorJurosDesconto(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        saida.setValorLiquido(valor);
        saida.setIdempotencyKey(idemKey);
        saida.setCriadoPor(usuario);
        saida = movimentacaoRepository.save(saida);

        MovimentacaoFinanceira entrada = new MovimentacaoFinanceira();
        entrada.setEmpresaId(empresaId);
        entrada.setFilialId(filialId);
        entrada.setContaFinanceiraId(destino.getIdContaFinanceira());
        entrada.setTipoMovimentacao(TipoMovimentacaoFinanceira.TRANSFERENCIA_ENTRADA);
        entrada.setOrigemTipo(OrigemMovimentacaoFinanceira.TRANSFERENCIA_INTERNA);
        entrada.setDescricao(descBase);
        entrada.setDocumentoReferencia(docRef);
        entrada.setDataMovimentacao(dataTransf);
        entrada.setDataHoraRegistro(LocalDateTime.now());
        entrada.setValorBruto(valor);
        entrada.setValorTaxa(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        entrada.setValorJurosDesconto(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        entrada.setValorLiquido(valor);
        entrada.setTransferenciaParId(saida.getIdMovimentacaoFinanceira());
        entrada.setCriadoPor(usuario);
        entrada = movimentacaoRepository.save(entrada);

        saida.setTransferenciaParId(entrada.getIdMovimentacaoFinanceira());
        saida = movimentacaoRepository.save(saida);

        origem.setSaldoAtual(scale2(nvl(origem.getSaldoAtual()).subtract(valor)));
        origem.setAtualizadoEm(LocalDateTime.now());
        destino.setSaldoAtual(scale2(nvl(destino.getSaldoAtual()).add(valor)));
        destino.setAtualizadoEm(LocalDateTime.now());
        contaFinanceiraRepository.save(origem);
        contaFinanceiraRepository.save(destino);

        registrarAuditoria(
                "TRANSFERENCIA_INTERNA_REALIZADA",
                "MovimentacaoFinanceira",
                saida.getIdMovimentacaoFinanceira(),
                "origem=" + origem.getCodigo() + ";destino=" + destino.getCodigo() + ";valor=" + valor,
                "Transferencia interna entre contas proprias (neutra no fluxo consolidado).",
                empresaId,
                filialId,
                request);

        Map<Long, ContaFinanceira> mapaContas = Map.of(
                origem.getIdContaFinanceira(), origem,
                destino.getIdContaFinanceira(), destino);
        return List.of(
                toMovimentacaoResponse(saida, mapaContas, Map.of(), Map.of()),
                toMovimentacaoResponse(entrada, mapaContas, Map.of(), Map.of()));
    }

    // =========================================================================
    // 5. Estorno Auditavel de Movimentacao Financeira
    // =========================================================================

    @Transactional
    public MovimentacaoFinanceiraResponseDTO estornarMovimentacao(
            Long idMovimentacao,
            EstornoMovimentacaoRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        String motivo = normalizeRequired(body.motivo(), "Motivo do estorno e obrigatorio.");
        MovimentacaoFinanceira original = movimentacaoRepository
                .findByIdMovimentacaoFinanceiraAndEmpresaIdAndFilialId(idMovimentacao, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Movimentacao financeira nao encontrada no tenant."));

        if (original.isEstornado() || original.getTipoMovimentacao() == TipoMovimentacaoFinanceira.ESTORNO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta movimentacao ja foi estornada ou e um registro de estorno.");
        }

        LocalDate dataEstorno = body.dataEstorno() != null ? body.dataEstorno() : LocalDate.now();
        String usuario = privacyGuardService.currentUserLogin(request);
        ContaFinanceira conta = contaFinanceiraRepository
                .findByIdContaFinanceiraAndEmpresaIdAndFilialId(original.getContaFinanceiraId(), empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Conta financeira da movimentacao nao encontrada."));

        BigDecimal valorLiq = nvl(original.getValorLiquido());
        boolean eraEntrada = original.getTipoMovimentacao() == TipoMovimentacaoFinanceira.ENTRADA
                || original.getTipoMovimentacao() == TipoMovimentacaoFinanceira.TRANSFERENCIA_ENTRADA;

        // Reverte o saldo na conta financeira
        if (eraEntrada) {
            conta.setSaldoAtual(scale2(nvl(conta.getSaldoAtual()).subtract(valorLiq)));
        } else {
            conta.setSaldoAtual(scale2(nvl(conta.getSaldoAtual()).add(valorLiq)));
        }
        conta.setAtualizadoEm(LocalDateTime.now());
        contaFinanceiraRepository.save(conta);

        // Se estava vinculada a ContaReceber ou ContaPagar, restaura o saldo em aberto do titulo e cancela despesa automatica de taxa
        if (original.getOrigemId() != null) {
            if (original.getOrigemTipo() == OrigemMovimentacaoFinanceira.CONTA_RECEBER
                    || original.getOrigemTipo() == OrigemMovimentacaoFinanceira.LIQUIDACAO_CARTAO
                    || original.getOrigemTipo() == OrigemMovimentacaoFinanceira.ANTECIPACAO_RECEBIVEL) {
                String tagMovimento = TAG_TAXA_DEDUZIDA_ORIGEM + original.getOrigemId() + ":MOV-" + original.getIdMovimentacaoFinanceira() + "]";
                String tagLegada = TAG_TAXA_DEDUZIDA_ORIGEM + original.getOrigemId() + "]";
                for (Despesa d : despesaRepository.findByEmpresaIdAndFilialIdOrderByDataVencimentoDesc(empresaId, filialId)) {
                    if (d.getStatus() != 0 && d.getDescricao() != null
                            && (d.getDescricao().startsWith(tagMovimento) || d.getDescricao().startsWith(tagLegada))) {
                        d.setStatus(0);
                        despesaRepository.save(d);
                    }
                }
                contaReceberRepository.findByIdContaReceberAndEmpresaIdAndFilialId(original.getOrigemId(), empresaId, filialId)
                        .ifPresent(cr -> {
                            BigDecimal estornadoBruto = nvl(original.getValorBruto());
                            BigDecimal novoPago = nvl(cr.getValorPago()).subtract(estornadoBruto).max(BigDecimal.ZERO);
                            if (original.getOrigemTipo() == OrigemMovimentacaoFinanceira.ANTECIPACAO_RECEBIVEL) {
                                BigDecimal taxaEstornada = nvl(original.getValorTaxa());
                                BigDecimal taxaOpOriginal = cr.getTaxaPercentual() != null && cr.getTaxaPercentual().compareTo(BigDecimal.ZERO) > 0
                                        ? nvl(cr.getValorBruto()).multiply(cr.getTaxaPercentual()).divide(CEM, 2, RoundingMode.HALF_UP)
                                        : BigDecimal.ZERO;
                                BigDecimal antRevertida = taxaEstornada.subtract(taxaOpOriginal).max(BigDecimal.ZERO);
                                cr.setTaxaAntecipacao(scale2(nvl(cr.getTaxaAntecipacao()).subtract(antRevertida).max(BigDecimal.ZERO)));
                                cr.setValorTaxa(scale2(taxaOpOriginal));
                                if (novoPago.compareTo(BigDecimal.ZERO) == 0) {
                                    cr.setAntecipado(false);
                                }
                            }
                            BigDecimal liquidoBase = nvl(cr.getValorLiquido()).compareTo(BigDecimal.ZERO) > 0
                                    ? cr.getValorLiquido()
                                    : nvl(cr.getValorBruto()).subtract(nvl(cr.getDesconto())).subtract(nvl(cr.getValorTaxa()));
                            BigDecimal novoSaldo = liquidoBase.subtract(novoPago).max(BigDecimal.ZERO);
                            cr.setValorPago(scale2(novoPago));
                            cr.setSaldo(scale2(novoSaldo));
                            cr.setStatus(novoPago.compareTo(BigDecimal.ZERO) > 0 ? FinanceiroStatus.PARCIAL : FinanceiroStatus.ABERTO);
                            if (novoPago.compareTo(BigDecimal.ZERO) == 0) {
                                cr.setPagoEm(null);
                            }
                            contaReceberRepository.save(cr);
                        });
            } else if (original.getOrigemTipo() == OrigemMovimentacaoFinanceira.CONTA_PAGAR) {
                contaPagarRepository.findByIdContaPagarAndEmpresaIdAndFilialId(original.getOrigemId(), empresaId, filialId)
                        .ifPresent(cp -> {
                            BigDecimal estornadoPrincipal = nvl(original.getValorBruto());
                            BigDecimal novoPago = nvl(cp.getValorPago()).subtract(estornadoPrincipal).max(BigDecimal.ZERO);
                            BigDecimal novoSaldo = nvl(cp.getValor()).subtract(novoPago).max(BigDecimal.ZERO);
                            cp.setValorPago(scale2(novoPago));
                            cp.setSaldo(scale2(novoSaldo));
                            cp.setStatus(novoPago.compareTo(BigDecimal.ZERO) > 0 ? FinanceiroStatus.PARCIAL : FinanceiroStatus.ABERTO);
                            if (novoPago.compareTo(BigDecimal.ZERO) == 0) {
                                cp.setPagoEm(null);
                            }
                            contaPagarRepository.save(cp);
                        });
            }
        }

        // Se era uma perna de transferencia interna, estorna tambem a outra perna e gera o registro de ESTORNO correspondente
        if (original.getTransferenciaParId() != null) {
            movimentacaoRepository.findByIdMovimentacaoFinanceiraAndEmpresaIdAndFilialId(
                            original.getTransferenciaParId(), empresaId, filialId)
                    .ifPresent(par -> {
                        if (!par.isEstornado()) {
                            par.setEstornado(true);
                            movimentacaoRepository.save(par);
                            contaFinanceiraRepository.findByIdContaFinanceiraAndEmpresaIdAndFilialId(
                                            par.getContaFinanceiraId(), empresaId, filialId)
                                    .ifPresent(contaPar -> {
                                        if (par.getTipoMovimentacao() == TipoMovimentacaoFinanceira.TRANSFERENCIA_ENTRADA) {
                                            contaPar.setSaldoAtual(scale2(nvl(contaPar.getSaldoAtual()).subtract(nvl(par.getValorLiquido()))));
                                        } else {
                                            contaPar.setSaldoAtual(scale2(nvl(contaPar.getSaldoAtual()).add(nvl(par.getValorLiquido()))));
                                        }
                                        contaPar.setAtualizadoEm(LocalDateTime.now());
                                        contaFinanceiraRepository.save(contaPar);

                                        MovimentacaoFinanceira estornoPar = new MovimentacaoFinanceira();
                                        estornoPar.setEmpresaId(empresaId);
                                        estornoPar.setFilialId(filialId);
                                        estornoPar.setContaFinanceiraId(contaPar.getIdContaFinanceira());
                                        estornoPar.setTipoMovimentacao(TipoMovimentacaoFinanceira.ESTORNO);
                                        estornoPar.setOrigemTipo(OrigemMovimentacaoFinanceira.ESTORNO_MOVIMENTACAO);
                                        estornoPar.setOrigemId(par.getIdMovimentacaoFinanceira());
                                        estornoPar.setDescricao("ESTORNO PAR (#" + par.getIdMovimentacaoFinanceira() + "): " + motivo);
                                        estornoPar.setDocumentoReferencia(par.getDocumentoReferencia());
                                        estornoPar.setDataMovimentacao(dataEstorno);
                                        estornoPar.setDataHoraRegistro(LocalDateTime.now());
                                        estornoPar.setValorBruto(par.getValorBruto());
                                        estornoPar.setValorTaxa(par.getValorTaxa());
                                        estornoPar.setValorJurosDesconto(par.getValorJurosDesconto());
                                        estornoPar.setValorLiquido(nvl(par.getValorLiquido()).negate());
                                        estornoPar.setMovimentacaoEstornadaId(par.getIdMovimentacaoFinanceira());
                                        estornoPar.setCriadoPor(usuario);
                                        movimentacaoRepository.save(estornoPar);
                                    });
                        }
                    });
        }

        original.setEstornado(true);
        movimentacaoRepository.save(original);

        MovimentacaoFinanceira estorno = new MovimentacaoFinanceira();
        estorno.setEmpresaId(empresaId);
        estorno.setFilialId(filialId);
        estorno.setContaFinanceiraId(conta.getIdContaFinanceira());
        estorno.setTipoMovimentacao(TipoMovimentacaoFinanceira.ESTORNO);
        estorno.setOrigemTipo(OrigemMovimentacaoFinanceira.ESTORNO_MOVIMENTACAO);
        estorno.setOrigemId(original.getIdMovimentacaoFinanceira());
        estorno.setDescricao("ESTORNO (#" + original.getIdMovimentacaoFinanceira() + "): " + motivo);
        estorno.setDocumentoReferencia(original.getDocumentoReferencia());
        estorno.setDataMovimentacao(dataEstorno);
        estorno.setDataHoraRegistro(LocalDateTime.now());
        estorno.setValorBruto(original.getValorBruto());
        estorno.setValorTaxa(original.getValorTaxa());
        estorno.setValorJurosDesconto(original.getValorJurosDesconto());
        estorno.setValorLiquido(valorLiq.negate());
        estorno.setPlanoContaId(original.getPlanoContaId());
        estorno.setCentroCustoId(original.getCentroCustoId());
        estorno.setMovimentacaoEstornadaId(original.getIdMovimentacaoFinanceira());
        estorno.setCriadoPor(usuario);
        MovimentacaoFinanceira salvo = movimentacaoRepository.save(estorno);

        registrarAuditoria(
                "MOVIMENTACAO_FINANCEIRA_ESTORNADA",
                "MovimentacaoFinanceira",
                original.getIdMovimentacaoFinanceira(),
                "estornoId=" + salvo.getIdMovimentacaoFinanceira() + ";valor=" + valorLiq + ";motivo=" + motivo,
                "Estorno de movimentacao financeira com preservacao da trilha de auditoria.",
                empresaId,
                filialId,
                request);

        return toMovimentacaoResponse(salvo, Map.of(conta.getIdContaFinanceira(), conta), Map.of(), Map.of());
    }

    // =========================================================================
    // 6. Painel Completo de Fluxo de Caixa (Realizado, Previsto, Projetado e Liquidez)
    // =========================================================================

    @Transactional(readOnly = true)
    public FluxoCaixaPainelResponseDTO consultarPainelFluxoCaixa(
            LocalDate dataInicioParam,
            LocalDate dataFimParam,
            Long contaFinanceiraFiltroId,
            Long centroCustoFiltroId,
            Long filialFiltroId,
            Boolean consolidarFiliais,
            Long empresaId,
            Long currentFilialId) {
        requireTenant(empresaId, currentFilialId);
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = dataInicioParam != null ? dataInicioParam : hoje.withDayOfMonth(1);
        LocalDate fim = dataFimParam != null ? dataFimParam : hoje.plusDays(30);
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data final nao pode ser anterior a data inicial.");
        }

        List<Long> filiaisEscopo = resolverFiliaisEscopo(filialFiltroId, consolidarFiliais, empresaId, currentFilialId);
        boolean isConsolidado = Boolean.TRUE.equals(consolidarFiliais);
        Map<Long, String> nomesFiliais = mapaNomesFiliais(empresaId, currentFilialId);
        String rotuloFilial = isConsolidado
                ? "Consolidado Empresa (" + filiaisEscopo.size() + " filial/is)"
                : nomesFiliais.getOrDefault(filiaisEscopo.get(0), "Filial #" + filiaisEscopo.get(0));

        Map<Long, ContaFinanceira> contasPorId = new LinkedHashMap<>();
        List<ContaFinanceiraResponseDTO> contasDtos = new ArrayList<>();
        BigDecimal saldoInicialTotal = BigDecimal.ZERO;
        BigDecimal saldoAtualContas = BigDecimal.ZERO;

        for (Long fId : filiaisEscopo) {
            for (ContaFinanceira c : contaFinanceiraRepository.findByEmpresaIdAndFilialIdOrderByPadraoDescNomeAsc(empresaId, fId)) {
                contasPorId.put(c.getIdContaFinanceira(), c);
                if (contaFinanceiraFiltroId != null && !Objects.equals(c.getIdContaFinanceira(), contaFinanceiraFiltroId)) {
                    continue;
                }
                if (c.isAtivo()) {
                    saldoInicialTotal = saldoInicialTotal.add(nvl(c.getSaldoInicial()));
                    saldoAtualContas = saldoAtualContas.add(nvl(c.getSaldoAtual()));
                }
                contasDtos.add(toContaResponse(c, nomesFiliais.getOrDefault(fId, "Filial #" + fId)));
            }
        }

        Map<Long, PlanoContasGerencial> planosPorId = carregarPlanosPorId(empresaId, filiaisEscopo);
        Map<Long, CentroCusto> centrosPorId = carregarCentrosPorId(empresaId, filiaisEscopo);
        Map<Long, String> clientesPorId = carregarClientesPorId(empresaId, filiaisEscopo);
        Map<Long, String> fornecedoresPorId = carregarFornecedoresPorId(empresaId, filiaisEscopo);

        // Carrega listas operacionais uma unica vez por filial para eliminar varreduras duplicadas e N+1
        Map<Long, List<ContaReceber>> contasReceberPorFilial = new LinkedHashMap<>();
        Map<Long, List<ContaPagar>> contasPagarPorFilial = new LinkedHashMap<>();
        Map<Long, List<Despesa>> despesasPorFilial = new LinkedHashMap<>();
        for (Long fId : filiaisEscopo) {
            contasReceberPorFilial.put(fId, contaReceberRepository.findByEmpresaIdAndFilialIdOrderByVencimentoDesc(empresaId, fId));
            contasPagarPorFilial.put(fId, contaPagarRepository.findByEmpresaIdAndFilialIdOrderByVencimentoDesc(empresaId, fId));
            despesasPorFilial.put(fId, despesaRepository.findByEmpresaIdAndFilialIdOrderByDataVencimentoDesc(empresaId, fId));
        }

        // --- A. CAIXA REALIZADO (sem duplicar e sem inflar com transferencias internas) ---
        List<MovimentacaoFinanceiraResponseDTO> movsRealizadas = new ArrayList<>();
        BigDecimal entradasRealizadas = BigDecimal.ZERO;
        BigDecimal saidasRealizadas = BigDecimal.ZERO;
        BigDecimal transferenciasVolume = BigDecimal.ZERO;
        BigDecimal taxasCartaoRealizadas = BigDecimal.ZERO;
        BigDecimal estornosVolume = BigDecimal.ZERO;

        Set<String> origensJaContabilizadasNoLedger = new HashSet<>();

        for (Long fId : filiaisEscopo) {
            List<MovimentacaoFinanceira> movs = contaFinanceiraFiltroId != null
                    ? movimentacaoRepository.findByEmpresaIdAndFilialIdAndContaFinanceiraIdAndDataMovimentacaoBetweenOrderByDataMovimentacaoDescIdMovimentacaoFinanceiraDesc(
                            empresaId, fId, contaFinanceiraFiltroId, inicio, fim)
                    : movimentacaoRepository.findByEmpresaIdAndFilialIdAndDataMovimentacaoBetweenOrderByDataMovimentacaoDescIdMovimentacaoFinanceiraDesc(
                            empresaId, fId, inicio, fim);

            for (MovimentacaoFinanceira m : movs) {
                if (centroCustoFiltroId != null && !Objects.equals(m.getCentroCustoId(), centroCustoFiltroId)) {
                    continue;
                }
                movsRealizadas.add(toMovimentacaoResponse(m, contasPorId, planosPorId, centrosPorId));

                if (m.getOrigemId() != null && !m.isEstornado()) {
                    if (m.getOrigemTipo() == OrigemMovimentacaoFinanceira.CONTA_RECEBER
                            || m.getOrigemTipo() == OrigemMovimentacaoFinanceira.LIQUIDACAO_CARTAO
                            || m.getOrigemTipo() == OrigemMovimentacaoFinanceira.ANTECIPACAO_RECEBIVEL) {
                        origensJaContabilizadasNoLedger.add("CR:" + fId + ":" + m.getOrigemId());
                    } else if (m.getOrigemTipo() == OrigemMovimentacaoFinanceira.CONTA_PAGAR) {
                        origensJaContabilizadasNoLedger.add("CP:" + fId + ":" + m.getOrigemId());
                    } else if (m.getOrigemTipo() == OrigemMovimentacaoFinanceira.DESPESA) {
                        origensJaContabilizadasNoLedger.add("DESP:" + fId + ":" + m.getOrigemId());
                    }
                }

                if (m.getTipoMovimentacao() == TipoMovimentacaoFinanceira.ESTORNO) {
                    estornosVolume = estornosVolume.add(nvl(m.getValorLiquido()).abs());
                    continue;
                }
                if (m.isEstornado()) {
                    continue;
                }

                if (m.getTipoMovimentacao() == TipoMovimentacaoFinanceira.TRANSFERENCIA_SAIDA) {
                    transferenciasVolume = transferenciasVolume.add(nvl(m.getValorLiquido()));
                    // Se o usuario filtrou UMA conta especifica, a transferencia altera o saldo daquela conta;
                    // mas na visao consolidada da filial/empresa, transferencias internas NUNCA inflam entradas/saidas!
                    if (contaFinanceiraFiltroId != null) {
                        saidasRealizadas = saidasRealizadas.add(nvl(m.getValorLiquido()));
                    }
                } else if (m.getTipoMovimentacao() == TipoMovimentacaoFinanceira.TRANSFERENCIA_ENTRADA) {
                    if (contaFinanceiraFiltroId != null) {
                        entradasRealizadas = entradasRealizadas.add(nvl(m.getValorLiquido()));
                    }
                } else if (m.getTipoMovimentacao() == TipoMovimentacaoFinanceira.ENTRADA) {
                    entradasRealizadas = entradasRealizadas.add(nvl(m.getValorLiquido()));
                    taxasCartaoRealizadas = taxasCartaoRealizadas.add(nvl(m.getValorTaxa()));
                } else if (m.getTipoMovimentacao() == TipoMovimentacaoFinanceira.SAIDA) {
                    saidasRealizadas = saidasRealizadas.add(nvl(m.getValorLiquido()));
                }
            }

            // Integra baixas operacionais diretas (ContaReceber, ContaPagar, Despesa e Caixa PDV) que nao passaram por MovimentacaoFinanceira
            LocalDateTime inicioDt = inicio.atStartOfDay();
            LocalDateTime fimDt = fim.atTime(LocalTime.MAX);
            Set<Long> vendasCaixaContabilizadas = new HashSet<>();
            Set<Long> vendasComContaReceberAtiva = new HashSet<>();

            for (ContaReceber cr : contasReceberPorFilial.getOrDefault(fId, List.of())) {
                if (cr.getStatus() == FinanceiroStatus.CANCELADO || cr.getStatus() == FinanceiroStatus.DEVOLVIDO) {
                    continue;
                }
                if (cr.getVendaId() != null) {
                    vendasComContaReceberAtiva.add(cr.getVendaId());
                }
                if (contaFinanceiraFiltroId != null && !Objects.equals(cr.getContaFinanceiraId(), contaFinanceiraFiltroId)) {
                    continue;
                }
                if (centroCustoFiltroId != null && !Objects.equals(cr.getCentroCustoId(), centroCustoFiltroId)) {
                    continue;
                }
                if (cr.getVendaId() != null && nvl(cr.getValorPago()).compareTo(BigDecimal.ZERO) > 0) {
                    vendasCaixaContabilizadas.add(cr.getVendaId());
                }
                if (origensJaContabilizadasNoLedger.contains("CR:" + fId + ":" + cr.getIdContaReceber())) {
                    continue;
                }
                if (cr.getPagoEm() != null
                        && within(cr.getPagoEm().toLocalDate(), inicio, fim)
                        && nvl(cr.getValorPago()).compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal taxaCr = nvl(cr.getValorTaxa()).add(nvl(cr.getTaxaAntecipacao()));
                    BigDecimal liqCr = nvl(cr.getValorPago());
                    entradasRealizadas = entradasRealizadas.add(liqCr);
                    taxasCartaoRealizadas = taxasCartaoRealizadas.add(taxaCr);

                    movsRealizadas.add(new MovimentacaoFinanceiraResponseDTO(
                            -cr.getIdContaReceber(),
                            empresaId,
                            fId,
                            cr.getContaFinanceiraId(),
                            cr.getContaFinanceiraId() != null && contasPorId.containsKey(cr.getContaFinanceiraId())
                                    ? contasPorId.get(cr.getContaFinanceiraId()).getNome()
                                    : "Caixa Operacional / PDV",
                            TipoMovimentacaoFinanceira.ENTRADA,
                            OrigemMovimentacaoFinanceira.CONTA_RECEBER,
                            cr.getIdContaReceber(),
                            cr.getDescricao() != null ? cr.getDescricao() : "Recebimento #" + cr.getIdContaReceber(),
                            "CR-" + cr.getIdContaReceber(),
                            cr.getPagoEm().toLocalDate(),
                            cr.getPagoEm(),
                            scale2(nvl(cr.getValorBruto())),
                            scale2(taxaCr),
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            scale2(liqCr),
                            cr.getPlanoContaId(),
                            cr.getPlanoContaId() != null && planosPorId.containsKey(cr.getPlanoContaId())
                                    ? planosPorId.get(cr.getPlanoContaId()).getNome()
                                    : null,
                            cr.getCentroCustoId(),
                            cr.getCentroCustoId() != null && centrosPorId.containsKey(cr.getCentroCustoId())
                                    ? centrosPorId.get(cr.getCentroCustoId()).getNome()
                                    : null,
                            null,
                            null,
                            false,
                            true,
                            null,
                            "sistema"));
                }
            }

            if (contaFinanceiraFiltroId == null) {
                for (CaixaMovimentacao cm : caixaMovimentacaoRepository
                        .findByCaixaSessaoEmpresaIdAndCaixaSessaoFilialIdAndCriadoEmBetweenOrderByCriadoEmDesc(
                                empresaId, fId, inicioDt, fimDt)) {
                    if (!"VENDA".equalsIgnoreCase(cm.getTipo()) || cm.getCriadoEm() == null) {
                        continue;
                    }
                    if (cm.getReferenciaId() != null
                            && (vendasCaixaContabilizadas.contains(cm.getReferenciaId())
                                    || vendasComContaReceberAtiva.contains(cm.getReferenciaId()))) {
                        continue;
                    }
                    BigDecimal val = scale2(cm.getValor());
                    entradasRealizadas = entradasRealizadas.add(val);
                    movsRealizadas.add(new MovimentacaoFinanceiraResponseDTO(
                            -100000L - cm.getIdCaixaMovimentacao(),
                            empresaId,
                            fId,
                            null,
                            "Caixa PDV",
                            TipoMovimentacaoFinanceira.ENTRADA,
                            OrigemMovimentacaoFinanceira.CAIXA_PDV,
                            cm.getReferenciaId(),
                            cm.getObservacao() != null ? cm.getObservacao() : "Venda PDV #" + cm.getReferenciaId(),
                            "PDV-" + cm.getIdCaixaMovimentacao(),
                            cm.getCriadoEm().toLocalDate(),
                            cm.getCriadoEm(),
                            val,
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            val,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false,
                            true,
                            null,
                            "pdv"));
                }
            }

            for (ContaPagar cp : contasPagarPorFilial.getOrDefault(fId, List.of())) {
                if (cp.getStatus() == FinanceiroStatus.CANCELADO || cp.getStatus() == FinanceiroStatus.DEVOLVIDO) {
                    continue;
                }
                if (contaFinanceiraFiltroId != null && !Objects.equals(cp.getContaFinanceiraId(), contaFinanceiraFiltroId)) {
                    continue;
                }
                if (centroCustoFiltroId != null && !Objects.equals(cp.getCentroCustoId(), centroCustoFiltroId)) {
                    continue;
                }
                if (origensJaContabilizadasNoLedger.contains("CP:" + fId + ":" + cp.getIdContaPagar())) {
                    continue;
                }
                BigDecimal pago = nvl(cp.getValorPago()).compareTo(BigDecimal.ZERO) > 0
                        ? cp.getValorPago()
                        : (cp.getStatus() == FinanceiroStatus.PAGO ? nvl(cp.getValor()) : BigDecimal.ZERO);
                if (cp.getPagoEm() != null
                        && within(cp.getPagoEm().toLocalDate(), inicio, fim)
                        && pago.compareTo(BigDecimal.ZERO) > 0) {
                    saidasRealizadas = saidasRealizadas.add(pago);
                    movsRealizadas.add(new MovimentacaoFinanceiraResponseDTO(
                            -200000L - cp.getIdContaPagar(),
                            empresaId,
                            fId,
                            cp.getContaFinanceiraId(),
                            cp.getContaFinanceiraId() != null && contasPorId.containsKey(cp.getContaFinanceiraId())
                                    ? contasPorId.get(cp.getContaFinanceiraId()).getNome()
                                    : "Caixa / Tesouraria",
                            TipoMovimentacaoFinanceira.SAIDA,
                            OrigemMovimentacaoFinanceira.CONTA_PAGAR,
                            cp.getIdContaPagar(),
                            cp.getDescricao(),
                            "CP-" + cp.getIdContaPagar(),
                            cp.getPagoEm().toLocalDate(),
                            cp.getPagoEm(),
                            scale2(pago),
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            scale2(pago),
                            cp.getPlanoContaId(),
                            cp.getPlanoContaId() != null && planosPorId.containsKey(cp.getPlanoContaId())
                                    ? planosPorId.get(cp.getPlanoContaId()).getNome()
                                    : null,
                            cp.getCentroCustoId(),
                            cp.getCentroCustoId() != null && centrosPorId.containsKey(cp.getCentroCustoId())
                                    ? centrosPorId.get(cp.getCentroCustoId()).getNome()
                                    : null,
                            null,
                            null,
                            false,
                            true,
                            null,
                            "sistema"));
                }
            }

            if (contaFinanceiraFiltroId == null) {
                for (Despesa d : despesasPorFilial.getOrDefault(fId, List.of())) {
                    if (d.getStatus() == 0 || d.getDataPagamento() == null || !within(d.getDataPagamento(), inicio, fim)) {
                        continue;
                    }
                    // Despesas financeiras geradas automaticamente por taxas de liquidacao/antecipacao ja tiveram seu valor
                    // descontado no valorLiquido da entrada na ContaFinanceira; ignora aqui para evitar dupla deducao no caixa!
                    if (d.getDescricao() != null && d.getDescricao().startsWith(TAG_TAXA_DEDUZIDA_ORIGEM)) {
                        continue;
                    }
                    if (centroCustoFiltroId != null && !Objects.equals(d.getCentroCustoId(), centroCustoFiltroId)) {
                        continue;
                    }
                    if (origensJaContabilizadasNoLedger.contains("DESP:" + fId + ":" + d.getIdDespesa())) {
                        continue;
                    }
                    BigDecimal val = scale2(d.getValor());
                    saidasRealizadas = saidasRealizadas.add(val);
                    movsRealizadas.add(new MovimentacaoFinanceiraResponseDTO(
                            -300000L - d.getIdDespesa(),
                            empresaId,
                            fId,
                            null,
                            "Caixa / Tesouraria",
                            TipoMovimentacaoFinanceira.SAIDA,
                            OrigemMovimentacaoFinanceira.DESPESA,
                            d.getIdDespesa(),
                            d.getDescricao(),
                            "DESP-" + d.getIdDespesa(),
                            d.getDataPagamento(),
                            d.getDataPagamento().atTime(12, 0),
                            val,
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            val,
                            d.getPlanoContaId(),
                            d.getPlanoContaId() != null && planosPorId.containsKey(d.getPlanoContaId())
                                    ? planosPorId.get(d.getPlanoContaId()).getNome()
                                    : null,
                            d.getCentroCustoId(),
                            d.getCentroCustoId() != null && centrosPorId.containsKey(d.getCentroCustoId())
                                    ? centrosPorId.get(d.getCentroCustoId()).getNome()
                                    : null,
                            null,
                            null,
                            false,
                            true,
                            null,
                            "sistema"));
                }
            }
        }

        movsRealizadas.sort(Comparator.comparing(MovimentacaoFinanceiraResponseDTO::dataMovimentacao).reversed());

        BigDecimal geracaoLiquidaRealizada = entradasRealizadas.subtract(saidasRealizadas);
        BigDecimal saldoFinalRealizado = saldoAtualContas.compareTo(BigDecimal.ZERO) != 0 || !contasDtos.isEmpty()
                ? saldoAtualContas.add(geracaoLiquidaSemContaFinanceira(movsRealizadas))
                : saldoInicialTotal.add(geracaoLiquidaRealizada);

        // --- B. CAIXA PREVISTO (Separando Pendentes vs Vencidos, Cartoes, Parcelas e Recorrencias) ---
        List<FluxoCaixaPrevistoItemDTO> itensPrevistos = new ArrayList<>();
        BigDecimal entradasPendentes = BigDecimal.ZERO;
        BigDecimal entradasPendentes30d = BigDecimal.ZERO;
        BigDecimal entradasVencidas = BigDecimal.ZERO;
        BigDecimal saidasPendentes = BigDecimal.ZERO;
        BigDecimal saidasPendentes30d = BigDecimal.ZERO;
        BigDecimal saidasVencidas = BigDecimal.ZERO;

        BigDecimal repassesCartaoBruto = BigDecimal.ZERO;
        BigDecimal taxasCartaoPrevistas = BigDecimal.ZERO;
        BigDecimal repassesCartaoLiquido = BigDecimal.ZERO;
        BigDecimal recebiveisAntecipadosTotal = BigDecimal.ZERO;
        BigDecimal recebiveisTotaisCartao = BigDecimal.ZERO;
        BigDecimal comprometimentoFornecedores = BigDecimal.ZERO;
        Map<String, BigDecimal> pressaoCaixaPorGrupo = new LinkedHashMap<>();

        LocalDate limiteProjecao30 = hoje.plusDays(30);
        LocalDate limiteProjecao90 = hoje.plusDays(90);

        for (Long fId : filiaisEscopo) {
            for (ContaReceber cr : contasReceberPorFilial.getOrDefault(fId, List.of())) {
                if (cr.getStatus() == FinanceiroStatus.CANCELADO || cr.getStatus() == FinanceiroStatus.DEVOLVIDO) {
                    continue;
                }
                if (contaFinanceiraFiltroId != null && !Objects.equals(cr.getContaFinanceiraId(), contaFinanceiraFiltroId)) {
                    continue;
                }
                if (centroCustoFiltroId != null && !Objects.equals(cr.getCentroCustoId(), centroCustoFiltroId)) {
                    continue;
                }
                boolean isCartao = cr.getModalidadeRecebivel() == ModalidadeRecebivel.CARTAO_CREDITO
                        || cr.getModalidadeRecebivel() == ModalidadeRecebivel.CARTAO_DEBITO;
                if (isCartao) {
                    recebiveisTotaisCartao = recebiveisTotaisCartao.add(nvl(cr.getValorBruto()));
                    if (cr.isAntecipado()) {
                        recebiveisAntecipadosTotal = recebiveisAntecipadosTotal.add(nvl(cr.getValorBruto()));
                    }
                }

                BigDecimal saldoAberto = calcularSaldoAbertoReceber(cr);
                if (cr.getStatus() == FinanceiroStatus.PAGO || saldoAberto.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                FluxoCaixaPrevistoItemDTO dto = toPrevistoReceber(cr, hoje, clientesPorId, planosPorId, centrosPorId);
                itensPrevistos.add(dto);

                if ("VENCIDO".equals(dto.situacaoVencimento())) {
                    entradasVencidas = entradasVencidas.add(dto.saldoAbertoLiquido());
                } else {
                    entradasPendentes = entradasPendentes.add(dto.saldoAbertoLiquido());
                    if (!dto.dataPrevistaLiquidacao().isAfter(limiteProjecao30)) {
                        entradasPendentes30d = entradasPendentes30d.add(dto.saldoAbertoLiquido());
                    }
                }

                if (isCartao) {
                    repassesCartaoBruto = repassesCartaoBruto.add(dto.valorBruto().subtract(dto.valorPagoAcumulado()).max(BigDecimal.ZERO));
                    taxasCartaoPrevistas = taxasCartaoPrevistas.add(dto.valorTaxaPrevista());
                    repassesCartaoLiquido = repassesCartaoLiquido.add(dto.saldoAbertoLiquido());
                }
            }

            for (ContaPagar cp : contasPagarPorFilial.getOrDefault(fId, List.of())) {
                if (cp.getStatus() == FinanceiroStatus.CANCELADO || cp.getStatus() == FinanceiroStatus.DEVOLVIDO) {
                    continue;
                }
                if (contaFinanceiraFiltroId != null && !Objects.equals(cp.getContaFinanceiraId(), contaFinanceiraFiltroId)) {
                    continue;
                }
                if (centroCustoFiltroId != null && !Objects.equals(cp.getCentroCustoId(), centroCustoFiltroId)) {
                    continue;
                }
                BigDecimal saldoAberto = calcularSaldoAbertoPagar(cp);
                if (cp.getStatus() != FinanceiroStatus.PAGO && saldoAberto.compareTo(BigDecimal.ZERO) > 0) {
                    FluxoCaixaPrevistoItemDTO dto = toPrevistoPagar(cp, hoje, fornecedoresPorId, planosPorId, centrosPorId);
                    itensPrevistos.add(dto);

                    if ("VENCIDO".equals(dto.situacaoVencimento())) {
                        saidasVencidas = saidasVencidas.add(dto.saldoAbertoLiquido());
                    } else {
                        saidasPendentes = saidasPendentes.add(dto.saldoAbertoLiquido());
                        if (!dto.dataPrevistaLiquidacao().isAfter(limiteProjecao30)) {
                            saidasPendentes30d = saidasPendentes30d.add(dto.saldoAbertoLiquido());
                        }
                    }
                    if (cp.getFornecedorId() != null) {
                        comprometimentoFornecedores = comprometimentoFornecedores.add(dto.saldoAbertoLiquido());
                    }
                    String chaveGrupo = dto.planoContaNome() != null ? dto.planoContaNome() : dto.descricao();
                    pressaoCaixaPorGrupo.merge(chaveGrupo, dto.saldoAbertoLiquido(), BigDecimal::add);
                }

                // Se a obrigacao e recorrente (ex: MENSAL), projeta os proximos vencimentos dentro da janela de 90 dias
                if (cp.isRecorrente() && cp.getVencimento() != null) {
                    LocalDate proxVenc = avancarPeriodicidade(cp.getVencimento(), cp.getPeriodicidade());
                    int guardaRec = 0;
                    while (proxVenc != null && !proxVenc.isAfter(limiteProjecao90) && guardaRec++ < 6) {
                        if (proxVenc.isAfter(hoje)) {
                            BigDecimal valRec = scale2(cp.getValor());
                            saidasPendentes = saidasPendentes.add(valRec);
                            if (!proxVenc.isAfter(limiteProjecao30)) {
                                saidasPendentes30d = saidasPendentes30d.add(valRec);
                            }
                            itensPrevistos.add(new FluxoCaixaPrevistoItemDTO(
                                    "OBRIGACAO_RECORRENTE",
                                    cp.getIdContaPagar(),
                                    "SAIDA",
                                    cp.getDescricao() + " [Provisao Recorrente]",
                                    cp.getFornecedorId() != null ? fornecedoresPorId.get(cp.getFornecedorId()) : null,
                                    proxVenc,
                                    proxVenc,
                                    proxVenc,
                                    cp.getNumeroParcela() != null ? cp.getNumeroParcela() : 1,
                                    cp.getTotalParcelas() != null ? cp.getTotalParcelas() : 1,
                                    "RECORRENTE_" + (cp.getPeriodicidade() == null ? "MENSAL" : cp.getPeriodicidade()),
                                    "A_VENCER",
                                    ChronoUnit.DAYS.between(hoje, proxVenc),
                                    valRec,
                                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                                    valRec,
                                    true,
                                    false,
                                    cp.getPlanoContaId(),
                                    cp.getPlanoContaId() != null && planosPorId.containsKey(cp.getPlanoContaId())
                                            ? planosPorId.get(cp.getPlanoContaId()).getNome()
                                            : null,
                                    cp.getCentroCustoId(),
                                    cp.getCentroCustoId() != null && centrosPorId.containsKey(cp.getCentroCustoId())
                                            ? centrosPorId.get(cp.getCentroCustoId()).getNome()
                                            : null,
                                    fId));
                        }
                        proxVenc = avancarPeriodicidade(proxVenc, cp.getPeriodicidade());
                    }
                }
            }

            if (contaFinanceiraFiltroId == null) {
                for (Despesa d : despesasPorFilial.getOrDefault(fId, List.of())) {
                    if (d.getStatus() == 0 || d.getDataPagamento() != null) {
                        continue;
                    }
                    if (centroCustoFiltroId != null && !Objects.equals(d.getCentroCustoId(), centroCustoFiltroId)) {
                        continue;
                    }
                    LocalDate venc = d.getDataVencimento() != null
                            ? d.getDataVencimento()
                            : (d.getDataCompetencia() != null ? d.getDataCompetencia() : hoje);
                    BigDecimal val = scale2(d.getValor());
                    String sit = venc.isBefore(hoje) ? "VENCIDO" : (venc.isEqual(hoje) ? "VENCE_HOJE" : "A_VENCER");
                    long dias = ChronoUnit.DAYS.between(hoje, venc);

                    if ("VENCIDO".equals(sit)) {
                        saidasVencidas = saidasVencidas.add(val);
                    } else {
                        saidasPendentes = saidasPendentes.add(val);
                        if (!venc.isAfter(limiteProjecao30)) {
                            saidasPendentes30d = saidasPendentes30d.add(val);
                        }
                    }
                    String nomePlano = d.getPlanoContaId() != null && planosPorId.containsKey(d.getPlanoContaId())
                            ? planosPorId.get(d.getPlanoContaId()).getNome()
                            : (d.getTipoDespesa() != null ? d.getTipoDespesa().getNome() : d.getDescricao());
                    pressaoCaixaPorGrupo.merge(nomePlano, val, BigDecimal::add);

                    itensPrevistos.add(new FluxoCaixaPrevistoItemDTO(
                            "DESPESA",
                            d.getIdDespesa(),
                            "SAIDA",
                            d.getDescricao(),
                            d.getTipoDespesa() != null ? d.getTipoDespesa().getNome() : "Despesa Operacional",
                            d.getDataCompetencia() != null ? d.getDataCompetencia() : venc,
                            venc,
                            venc,
                            1,
                            1,
                            "DESPESA_DIRETA",
                            sit,
                            dias,
                            val,
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                            val,
                            false,
                            false,
                            d.getPlanoContaId(),
                            nomePlano,
                            d.getCentroCustoId(),
                            d.getCentroCustoId() != null && centrosPorId.containsKey(d.getCentroCustoId())
                                    ? centrosPorId.get(d.getCentroCustoId()).getNome()
                                    : null,
                            fId));
                }
            }
        }

        itensPrevistos.sort(Comparator.comparing(FluxoCaixaPrevistoItemDTO::dataPrevistaLiquidacao));

        // --- C. CALENDARIO FINANCEIRO E PROJECOES (7, 15, 30, 60 e 90 dias - Otimista, Realista, Pessimista) ---
        List<FluxoCaixaDiaCalendarioDTO> calendario = construirCalendarioFinanceiro(
                inicio, fim, hoje, saldoInicialTotal, saldoFinalRealizado, movsRealizadas, itensPrevistos);

        List<FluxoCaixaCenarioProjecaoDTO> cenarios = construirCenariosProjecao(
                hoje, saldoFinalRealizado, itensPrevistos, null);

        FluxoCaixaCenarioProjecaoDTO cenarioRealista = cenarios.stream()
                .filter(c -> "REALISTA".equals(c.codigoCenario()))
                .findFirst()
                .orElse(cenarios.get(0));

        // --- D. DIAGNOSTICO DE LIQUIDEZ E CONEXAO COM A DRE ---
        DiagnosticoLiquidezDTO diagnostico = construirDiagnosticoLiquidez(
                hoje,
                empresaId,
                currentFilialId,
                filialFiltroId,
                isConsolidado,
                centroCustoFiltroId,
                saldoFinalRealizado,
                geracaoLiquidaRealizada,
                entradasPendentes,
                entradasPendentes30d,
                entradasVencidas,
                saidasPendentes,
                saidasPendentes30d,
                saidasVencidas,
                comprometimentoFornecedores,
                repassesCartaoBruto,
                taxasCartaoPrevistas,
                repassesCartaoLiquido,
                recebiveisAntecipadosTotal,
                recebiveisTotaisCartao,
                cenarioRealista,
                pressaoCaixaPorGrupo);

        ConciliacaoResumoResponseDTO resumoConciliacao = consultarConciliacaoEscopo(
                contaFinanceiraFiltroId, null, inicio, fim, empresaId, filiaisEscopo);

        return new FluxoCaixaPainelResponseDTO(
                inicio,
                fim,
                empresaId,
                isConsolidado ? null : filiaisEscopo.get(0),
                rotuloFilial,
                isConsolidado,
                contaFinanceiraFiltroId,
                centroCustoFiltroId,
                scale2(saldoInicialTotal),
                scale2(entradasRealizadas),
                scale2(saidasRealizadas),
                scale2(geracaoLiquidaRealizada),
                scale2(saldoFinalRealizado),
                scale2(transferenciasVolume),
                scale2(taxasCartaoRealizadas),
                scale2(estornosVolume),
                scale2(entradasPendentes),
                scale2(entradasVencidas),
                scale2(saidasPendentes),
                scale2(saidasVencidas),
                cenarioRealista.saldoProjetado30Dias(),
                contasDtos,
                movsRealizadas,
                itensPrevistos,
                calendario,
                cenarios,
                diagnostico,
                resumoConciliacao);
    }

    @Transactional(readOnly = true)
    public List<FluxoCaixaCenarioProjecaoDTO> simularProjecao(
            FluxoCaixaSimulacaoRequestDTO simulacao,
            Long contaFinanceiraFiltroId,
            Long centroCustoFiltroId,
            Long filialFiltroId,
            Boolean consolidarFiliais,
            Long empresaId,
            Long currentFilialId) {
        requireTenant(empresaId, currentFilialId);
        LocalDate hoje = LocalDate.now();
        List<Long> filiaisEscopo = resolverFiliaisEscopo(filialFiltroId, consolidarFiliais, empresaId, currentFilialId);
        BigDecimal saldoAtualContas = BigDecimal.ZERO;
        for (Long fId : filiaisEscopo) {
            for (ContaFinanceira c : contaFinanceiraRepository.findByEmpresaIdAndFilialIdAndAtivoTrueOrderByPadraoDescNomeAsc(empresaId, fId)) {
                if (contaFinanceiraFiltroId != null && !Objects.equals(c.getIdContaFinanceira(), contaFinanceiraFiltroId)) {
                    continue;
                }
                saldoAtualContas = saldoAtualContas.add(nvl(c.getSaldoAtual()));
            }
        }
        List<FluxoCaixaPrevistoItemDTO> itensPrevistos = new ArrayList<>();
        LocalDate limiteProjecao90 = hoje.plusDays(90);
        for (Long fId : filiaisEscopo) {
            for (ContaReceber cr : contaReceberRepository.findByEmpresaIdAndFilialIdOrderByVencimentoDesc(empresaId, fId)) {
                if (cr.getStatus() == FinanceiroStatus.CANCELADO || cr.getStatus() == FinanceiroStatus.DEVOLVIDO || cr.getStatus() == FinanceiroStatus.PAGO) {
                    continue;
                }
                if (contaFinanceiraFiltroId != null && !Objects.equals(cr.getContaFinanceiraId(), contaFinanceiraFiltroId)) {
                    continue;
                }
                if (centroCustoFiltroId != null && !Objects.equals(cr.getCentroCustoId(), centroCustoFiltroId)) {
                    continue;
                }
                if (calcularSaldoAbertoReceber(cr).compareTo(BigDecimal.ZERO) > 0) {
                    itensPrevistos.add(toPrevistoReceber(cr, hoje, Map.of(), Map.of(), Map.of()));
                }
            }
            for (ContaPagar cp : contaPagarRepository.findByEmpresaIdAndFilialIdOrderByVencimentoDesc(empresaId, fId)) {
                if (cp.getStatus() == FinanceiroStatus.CANCELADO || cp.getStatus() == FinanceiroStatus.DEVOLVIDO) {
                    continue;
                }
                if (contaFinanceiraFiltroId != null && !Objects.equals(cp.getContaFinanceiraId(), contaFinanceiraFiltroId)) {
                    continue;
                }
                if (centroCustoFiltroId != null && !Objects.equals(cp.getCentroCustoId(), centroCustoFiltroId)) {
                    continue;
                }
                if (cp.getStatus() != FinanceiroStatus.PAGO && calcularSaldoAbertoPagar(cp).compareTo(BigDecimal.ZERO) > 0) {
                    itensPrevistos.add(toPrevistoPagar(cp, hoje, Map.of(), Map.of(), Map.of()));
                }
                if (cp.isRecorrente() && cp.getVencimento() != null) {
                    LocalDate proxVenc = avancarPeriodicidade(cp.getVencimento(), cp.getPeriodicidade());
                    int guardaRec = 0;
                    while (proxVenc != null && !proxVenc.isAfter(limiteProjecao90) && guardaRec++ < 6) {
                        if (proxVenc.isAfter(hoje)) {
                            BigDecimal valRec = scale2(cp.getValor());
                            itensPrevistos.add(new FluxoCaixaPrevistoItemDTO(
                                    "OBRIGACAO_RECORRENTE", cp.getIdContaPagar(), "SAIDA",
                                    cp.getDescricao() + " [Provisao Recorrente]", null,
                                    proxVenc, proxVenc, proxVenc, 1, 1, "RECORRENTE", "A_VENCER",
                                    ChronoUnit.DAYS.between(hoje, proxVenc), valRec, BigDecimal.ZERO, BigDecimal.ZERO,
                                    valRec, true, false, cp.getPlanoContaId(), null, cp.getCentroCustoId(), null, fId));
                        }
                        proxVenc = avancarPeriodicidade(proxVenc, cp.getPeriodicidade());
                    }
                }
            }
            if (contaFinanceiraFiltroId == null) {
                for (Despesa d : despesaRepository.findByEmpresaIdAndFilialIdOrderByDataVencimentoDesc(empresaId, fId)) {
                    if (d.getStatus() == 0 || d.getDataPagamento() != null) {
                        continue;
                    }
                    if (centroCustoFiltroId != null && !Objects.equals(d.getCentroCustoId(), centroCustoFiltroId)) {
                        continue;
                    }
                    LocalDate venc = d.getDataVencimento() != null ? d.getDataVencimento() : (d.getDataCompetencia() != null ? d.getDataCompetencia() : hoje);
                    BigDecimal val = scale2(d.getValor());
                    String sit = venc.isBefore(hoje) ? "VENCIDO" : (venc.isEqual(hoje) ? "VENCE_HOJE" : "A_VENCER");
                    itensPrevistos.add(new FluxoCaixaPrevistoItemDTO(
                            "DESPESA", d.getIdDespesa(), "SAIDA", d.getDescricao(), "Despesa Operacional",
                            venc, venc, venc, 1, 1, "DESPESA_DIRETA", sit,
                            ChronoUnit.DAYS.between(hoje, venc), val, BigDecimal.ZERO, BigDecimal.ZERO,
                            val, false, false, d.getPlanoContaId(), null, d.getCentroCustoId(), null, fId));
                }
            }
        }
        return construirCenariosProjecao(hoje, scale2(saldoAtualContas), itensPrevistos, simulacao);
    }

    // =========================================================================
    // 7. Conciliacao Financeira (Bancaria OFX/CSV, Caixa e Operadoras de Cartao)
    // =========================================================================

    @Transactional
    public ConciliacaoResumoResponseDTO importarExtratoConciliacao(
            ImportacaoExtratoRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        ContaFinanceira conta = contaFinanceiraRepository
                .findByIdContaFinanceiraAndEmpresaIdAndFilialId(body.contaFinanceiraId(), empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Conta financeira nao encontrada no tenant."));

        String tipoConc = normalizeRequired(body.tipoConciliacao(), "Tipo de conciliacao (BANCARIA, CAIXA, CARTAO) e obrigatorio.")
                .toUpperCase(Locale.ROOT);
        String formato = normalizeRequired(body.formatoOrigem(), "Formato de origem (OFX, CSV, MANUAL) e obrigatorio.")
                .toUpperCase(Locale.ROOT);

        List<ImportacaoExtratoRequestDTO.LinhaExtratoManualDTO> linhasExtraidas = new ArrayList<>();
        if (body.linhas() != null && !body.linhas().isEmpty()) {
            linhasExtraidas.addAll(body.linhas());
        }
        if (body.conteudoArquivo() != null && !body.conteudoArquivo().isBlank()) {
            if ("OFX".equals(formato)) {
                linhasExtraidas.addAll(parseOfx(body.conteudoArquivo()));
            } else if ("CSV".equals(formato)) {
                linhasExtraidas.addAll(parseCsvExtrato(body.conteudoArquivo()));
            }
        }
        if (linhasExtraidas.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma linha de extrato valida foi encontrada para importacao.");
        }

        List<MovimentacaoFinanceira> movsCandidatas = movimentacaoRepository
                .findByEmpresaIdAndFilialIdAndContaFinanceiraIdAndConciliadoFalseAndEstornadoFalseOrderByDataMovimentacaoAsc(
                        empresaId, filialId, conta.getIdContaFinanceira());
        Set<Long> movsJaUsadasNesteLote = new HashSet<>();
        String usuario = privacyGuardService.currentUserLogin(request);

        for (ImportacaoExtratoRequestDTO.LinhaExtratoManualDTO linha : linhasExtraidas) {
            LocalDate dataExt = linha.dataExtrato() != null ? linha.dataExtrato() : LocalDate.now();
            BigDecimal valExt = scale2(nvl(linha.valorExtrato()).abs());
            if (valExt.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            String natExt = linha.naturezaExtrato() != null && !linha.naturezaExtrato().isBlank()
                    ? linha.naturezaExtrato().trim().toUpperCase(Locale.ROOT)
                    : (nvl(linha.valorExtrato()).signum() < 0 ? "DEBITO" : "CREDITO");
            String descExt = linha.descricaoExtrato() != null && !linha.descricaoExtrato().isBlank()
                    ? linha.descricaoExtrato().trim()
                    : "Lancamento extrato " + formato;

            // Detecta suspeita de duplicidade no extrato ja importado
            List<ConciliacaoFinanceiraItem> similares = conciliacaoRepository
                    .findByEmpresaIdAndFilialIdAndContaFinanceiraIdAndDataExtratoAndValorExtratoAndNaturezaExtrato(
                            empresaId, filialId, conta.getIdContaFinanceira(), dataExt, valExt, natExt);

            ConciliacaoFinanceiraItem item = new ConciliacaoFinanceiraItem();
            item.setEmpresaId(empresaId);
            item.setFilialId(filialId);
            item.setContaFinanceiraId(conta.getIdContaFinanceira());
            item.setTipoConciliacao(tipoConc);
            item.setFormatoOrigem(formato);
            item.setIdentificadorExtrato(trimToNull(linha.identificadorExtrato()));
            item.setDataExtrato(dataExt);
            item.setDescricaoExtrato(descExt.length() > 200 ? descExt.substring(0, 200) : descExt);
            item.setValorExtrato(valExt);
            item.setNaturezaExtrato(natExt);
            item.setCriadoEm(LocalDateTime.now());

            if (!similares.isEmpty()) {
                item.setStatusConciliacao(StatusConciliacaoFinanceira.DUPLICIDADE_SUSPEITA);
                item.setDiferencaValor(valExt);
                item.setObservacao("Possivel duplicidade detectada: ja existe item identico (mesma data, natureza e valor) nesta conta.");
                conciliacaoRepository.save(item);
                continue;
            }

            // Tenta pareamento exato ou divergente com MovimentacaoFinanceira na janela de +-3 dias
            MovimentacaoFinanceira matchExato = null;
            MovimentacaoFinanceira matchDataProximo = null;
            for (MovimentacaoFinanceira cand : movsCandidatas) {
                if (movsJaUsadasNesteLote.contains(cand.getIdMovimentacaoFinanceira())) {
                    continue;
                }
                boolean candCredito = cand.getTipoMovimentacao() == TipoMovimentacaoFinanceira.ENTRADA
                        || cand.getTipoMovimentacao() == TipoMovimentacaoFinanceira.TRANSFERENCIA_ENTRADA;
                boolean mesmaNatureza = ("CREDITO".equals(natExt) && candCredito)
                        || ("DEBITO".equals(natExt) && !candCredito);
                if (!mesmaNatureza) {
                    continue;
                }
                long diffDias = Math.abs(ChronoUnit.DAYS.between(dataExt, cand.getDataMovimentacao()));
                if (diffDias <= 3) {
                    if (scale2(cand.getValorLiquido()).compareTo(valExt) == 0) {
                        matchExato = cand;
                        break;
                    } else if (diffDias == 0 && matchDataProximo == null) {
                        matchDataProximo = cand;
                    }
                }
            }

            if (matchExato != null) {
                movsJaUsadasNesteLote.add(matchExato.getIdMovimentacaoFinanceira());
                item.setStatusConciliacao(StatusConciliacaoFinanceira.CONCILIADO_EXATO);
                item.setMovimentacaoVinculadaId(matchExato.getIdMovimentacaoFinanceira());
                item.setDocumentoOrigemTipo(matchExato.getOrigemTipo().name());
                item.setDocumentoOrigemId(matchExato.getOrigemId());
                item.setDiferencaValor(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                item.setConciliadoPor(usuario);
                item.setConciliadoEm(LocalDateTime.now());
                item.setObservacao("Conciliado automaticamente por correspondencia exata de valor e data.");
                ConciliacaoFinanceiraItem salvo = conciliacaoRepository.save(item);

                matchExato.setConciliado(true);
                matchExato.setConciliacaoItemId(salvo.getIdConciliacaoItem());
                movimentacaoRepository.save(matchExato);
            } else if (matchDataProximo != null) {
                item.setStatusConciliacao(StatusConciliacaoFinanceira.DIVERGENTE);
                item.setMovimentacaoVinculadaId(matchDataProximo.getIdMovimentacaoFinanceira());
                item.setDocumentoOrigemTipo(matchDataProximo.getOrigemTipo().name());
                item.setDocumentoOrigemId(matchDataProximo.getOrigemId());
                item.setDiferencaValor(scale2(valExt.subtract(nvl(matchDataProximo.getValorLiquido()))));
                item.setObservacao("Divergencia de valor entre extrato (R$ " + valExt
                        + ") e sistema (R$ " + scale2(matchDataProximo.getValorLiquido()) + ").");
                conciliacaoRepository.save(item);
            } else {
                item.setStatusConciliacao(StatusConciliacaoFinanceira.PENDENTE);
                item.setDiferencaValor(valExt);
                item.setObservacao("Sem correspondencia automatica no sistema; aguarda conciliacao manual.");
                conciliacaoRepository.save(item);
            }
        }

        registrarAuditoria(
                "EXTRATO_CONCILIACAO_IMPORTADO",
                "ConciliacaoFinanceira",
                conta.getIdContaFinanceira(),
                "formato=" + formato + ";linhas=" + linhasExtraidas.size(),
                "Importacao e processamento automatico de extrato para conciliacao financeira.",
                empresaId,
                filialId,
                request);

        return consultarConciliacao(conta.getIdContaFinanceira(), null, null, null, empresaId, filialId);
    }

    @Transactional
    public ConciliacaoItemResponseDTO conciliarManualmente(
            ConciliacaoManualRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        ConciliacaoFinanceiraItem item = conciliacaoRepository
                .findByIdConciliacaoItemAndEmpresaIdAndFilialId(body.conciliacaoItemId(), empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Item de conciliacao nao encontrado no tenant."));

        ContaFinanceira conta = contaFinanceiraRepository
                .findByIdContaFinanceiraAndEmpresaIdAndFilialId(item.getContaFinanceiraId(), empresaId, filialId)
                .orElse(null);

        if (body.movimentacaoVinculadaId() != null) {
            MovimentacaoFinanceira mov = movimentacaoRepository
                    .findByIdMovimentacaoFinanceiraAndEmpresaIdAndFilialId(body.movimentacaoVinculadaId(), empresaId, filialId)
                    .orElseThrow(() -> new EntityNotFoundException("Movimentacao financeira vinculada nao encontrada."));
            if (!Objects.equals(mov.getContaFinanceiraId(), item.getContaFinanceiraId())) {
                throw new IllegalArgumentException("A movimentacao vinculada pertence a outra conta financeira.");
            }
            if (mov.isEstornado()) {
                throw new IllegalArgumentException("Nao e permitido conciliar uma movimentacao estornada.");
            }
            if (mov.isConciliado() && !Objects.equals(mov.getConciliacaoItemId(), item.getIdConciliacaoItem())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A movimentacao selecionada ja esta vinculada a outro item de conciliacao.");
            }
            item.setMovimentacaoVinculadaId(mov.getIdMovimentacaoFinanceira());
            item.setDocumentoOrigemTipo(mov.getOrigemTipo().name());
            item.setDocumentoOrigemId(mov.getOrigemId());
            item.setDiferencaValor(scale2(item.getValorExtrato().subtract(nvl(mov.getValorLiquido()))));
            mov.setConciliado(true);
            mov.setConciliacaoItemId(item.getIdConciliacaoItem());
            movimentacaoRepository.save(mov);
        } else {
            if (body.documentoOrigemTipo() != null) {
                item.setDocumentoOrigemTipo(body.documentoOrigemTipo());
            }
            if (body.documentoOrigemId() != null) {
                item.setDocumentoOrigemId(body.documentoOrigemId());
            }
            item.setDiferencaValor(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        item.setStatusConciliacao(body.novoStatus() != null
                ? body.novoStatus()
                : StatusConciliacaoFinanceira.CONCILIADO_MANUAL);
        if (body.observacao() != null && !body.observacao().isBlank()) {
            item.setObservacao(body.observacao().trim());
        }
        item.setConciliadoPor(privacyGuardService.currentUserLogin(request));
        item.setConciliadoEm(LocalDateTime.now());
        ConciliacaoFinanceiraItem salvo = conciliacaoRepository.save(item);

        registrarAuditoria(
                "CONCILIACAO_MANUAL_REGISTRADA",
                "ConciliacaoFinanceiraItem",
                salvo.getIdConciliacaoItem(),
                "status=" + salvo.getStatusConciliacao() + ";movId=" + salvo.getMovimentacaoVinculadaId(),
                "Conciliacao financeira manual registrada.",
                empresaId,
                filialId,
                request);

        return toConciliacaoItemResponse(salvo, conta != null ? Map.of(conta.getIdContaFinanceira(), conta) : Map.of());
    }

    @Transactional(readOnly = true)
    public ConciliacaoResumoResponseDTO consultarConciliacao(
            Long contaFinanceiraId,
            StatusConciliacaoFinanceira statusFiltro,
            LocalDate dataInicio,
            LocalDate dataFim,
            Long empresaId,
            Long filialId) {
        requireTenant(empresaId, filialId);
        return consultarConciliacaoEscopo(contaFinanceiraId, statusFiltro, dataInicio, dataFim, empresaId, List.of(filialId));
    }

    private ConciliacaoResumoResponseDTO consultarConciliacaoEscopo(
            Long contaFinanceiraId,
            StatusConciliacaoFinanceira statusFiltro,
            LocalDate dataInicio,
            LocalDate dataFim,
            Long empresaId,
            List<Long> filiaisEscopo) {
        Map<Long, ContaFinanceira> contasPorId = new HashMap<>();
        List<ConciliacaoFinanceiraItem> brutos = new ArrayList<>();
        int sessoesComDiff = 0;
        BigDecimal diffTotalCaixa = BigDecimal.ZERO;

        for (Long fId : filiaisEscopo) {
            for (ContaFinanceira c : contaFinanceiraRepository.findByEmpresaIdAndFilialIdOrderByPadraoDescNomeAsc(empresaId, fId)) {
                contasPorId.put(c.getIdContaFinanceira(), c);
            }
            List<ConciliacaoFinanceiraItem> itensFilial = contaFinanceiraId != null
                    ? conciliacaoRepository.findByEmpresaIdAndFilialIdAndContaFinanceiraIdOrderByDataExtratoDescIdConciliacaoItemDesc(
                            empresaId, fId, contaFinanceiraId)
                    : conciliacaoRepository.findByEmpresaIdAndFilialIdOrderByDataExtratoDescIdConciliacaoItemDesc(
                            empresaId, fId);
            brutos.addAll(itensFilial);

            for (CaixaSessao cs : caixaSessaoRepository.findTop20ByEmpresaIdAndFilialIdOrderByAbertoEmDesc(empresaId, fId)) {
                if (cs.getDiferencaFechamento() != null && cs.getDiferencaFechamento().compareTo(BigDecimal.ZERO) != 0) {
                    sessoesComDiff++;
                    diffTotalCaixa = diffTotalCaixa.add(cs.getDiferencaFechamento());
                }
            }
        }

        List<ConciliacaoItemResponseDTO> filtrados = new ArrayList<>();
        long exatos = 0L;
        long manuais = 0L;
        long pendentes = 0L;
        long divergentes = 0L;
        long duplicidades = 0L;
        BigDecimal valorDivergente = BigDecimal.ZERO;

        for (ConciliacaoFinanceiraItem it : brutos) {
            if (statusFiltro != null && it.getStatusConciliacao() != statusFiltro) {
                continue;
            }
            if (dataInicio != null && it.getDataExtrato().isBefore(dataInicio)) {
                continue;
            }
            if (dataFim != null && it.getDataExtrato().isAfter(dataFim)) {
                continue;
            }
            switch (it.getStatusConciliacao()) {
                case CONCILIADO_EXATO -> exatos++;
                case CONCILIADO_MANUAL -> manuais++;
                case PENDENTE -> {
                    pendentes++;
                    valorDivergente = valorDivergente.add(nvl(it.getDiferencaValor()).abs());
                }
                case DIVERGENTE -> {
                    divergentes++;
                    valorDivergente = valorDivergente.add(nvl(it.getDiferencaValor()).abs());
                }
                case DUPLICIDADE_SUSPEITA -> {
                    duplicidades++;
                    valorDivergente = valorDivergente.add(nvl(it.getValorExtrato()).abs());
                }
                default -> {
                }
            }
            filtrados.add(toConciliacaoItemResponse(it, contasPorId));
        }

        return new ConciliacaoResumoResponseDTO(
                filtrados.size(),
                exatos,
                manuais,
                pendentes,
                divergentes,
                duplicidades,
                scale2(valorDivergente),
                sessoesComDiff,
                scale2(diffTotalCaixa),
                filtrados);
    }

    // =========================================================================
    // 8. Motor de Projecao de Cenarios, Calendario e Diagnostico de Liquidez
    // =========================================================================

    private List<FluxoCaixaDiaCalendarioDTO> construirCalendarioFinanceiro(
            LocalDate inicio,
            LocalDate fim,
            LocalDate hoje,
            BigDecimal saldoInicialPeriodo,
            BigDecimal saldoPartidaHoje,
            List<MovimentacaoFinanceiraResponseDTO> movsRealizadas,
            List<FluxoCaixaPrevistoItemDTO> itensPrevistos) {
        Map<LocalDate, BigDecimal> entRealPorDia = new HashMap<>();
        Map<LocalDate, BigDecimal> saiRealPorDia = new HashMap<>();
        for (MovimentacaoFinanceiraResponseDTO m : movsRealizadas) {
            if (m.estornado() || m.tipoMovimentacao() == TipoMovimentacaoFinanceira.ESTORNO
                    || m.tipoMovimentacao() == TipoMovimentacaoFinanceira.TRANSFERENCIA_ENTRADA
                    || m.tipoMovimentacao() == TipoMovimentacaoFinanceira.TRANSFERENCIA_SAIDA) {
                continue;
            }
            if (m.tipoMovimentacao() == TipoMovimentacaoFinanceira.ENTRADA) {
                entRealPorDia.merge(m.dataMovimentacao(), nvl(m.valorLiquido()), BigDecimal::add);
            } else if (m.tipoMovimentacao() == TipoMovimentacaoFinanceira.SAIDA) {
                saiRealPorDia.merge(m.dataMovimentacao(), nvl(m.valorLiquido()), BigDecimal::add);
            }
        }

        Map<LocalDate, BigDecimal> entPrevPorDia = new HashMap<>();
        Map<LocalDate, BigDecimal> saiPrevPorDia = new HashMap<>();
        Map<LocalDate, Integer> qtdTitulosPorDia = new HashMap<>();
        for (FluxoCaixaPrevistoItemDTO p : itensPrevistos) {
            LocalDate dataAlvo = p.dataPrevistaLiquidacao() != null ? p.dataPrevistaLiquidacao() : p.dataVencimento();
            if (dataAlvo.isBefore(hoje)) {
                dataAlvo = hoje;
            }
            qtdTitulosPorDia.merge(dataAlvo, 1, Integer::sum);
            if ("ENTRADA".equals(p.natureza())) {
                entPrevPorDia.merge(dataAlvo, nvl(p.saldoAbertoLiquido()), BigDecimal::add);
            } else {
                saiPrevPorDia.merge(dataAlvo, nvl(p.saldoAbertoLiquido()), BigDecimal::add);
            }
        }

        List<FluxoCaixaDiaCalendarioDTO> dias = new ArrayList<>();
        BigDecimal saldoAcumHist = saldoInicialPeriodo;
        BigDecimal saldoAcumFuturo = saldoPartidaHoje;
        LocalDate cursor = inicio;
        int maxDias = 366;
        int count = 0;
        while (!cursor.isAfter(fim) && count++ < maxDias) {
            BigDecimal er = scale2(entRealPorDia.getOrDefault(cursor, BigDecimal.ZERO));
            BigDecimal sr = scale2(saiRealPorDia.getOrDefault(cursor, BigDecimal.ZERO));
            BigDecimal ep = scale2(entPrevPorDia.getOrDefault(cursor, BigDecimal.ZERO));
            BigDecimal sp = scale2(saiPrevPorDia.getOrDefault(cursor, BigDecimal.ZERO));
            int qtd = qtdTitulosPorDia.getOrDefault(cursor, 0);

            BigDecimal liqDia;
            BigDecimal saldoExibido;
            if (cursor.isBefore(hoje)) {
                liqDia = er.subtract(sr);
                saldoAcumHist = saldoAcumHist.add(liqDia);
                saldoExibido = saldoAcumHist;
            } else {
                liqDia = er.subtract(sr).add(ep).subtract(sp);
                saldoAcumFuturo = saldoAcumFuturo.add(ep).subtract(sp);
                saldoExibido = saldoAcumFuturo;
            }
            String tipoDado = cursor.isBefore(hoje) ? "REALIZADO" : (cursor.isEqual(hoje) ? "HOJE_MISTO" : "PREVISTO_PROJETADO");

            dias.add(new FluxoCaixaDiaCalendarioDTO(
                    cursor,
                    tipoDado,
                    er,
                    sr,
                    ep,
                    sp,
                    scale2(liqDia),
                    scale2(saldoExibido),
                    qtd,
                    saldoExibido.compareTo(BigDecimal.ZERO) < 0));
            cursor = cursor.plusDays(1);
        }
        return dias;
    }

    private List<FluxoCaixaCenarioProjecaoDTO> construirCenariosProjecao(
            LocalDate hoje,
            BigDecimal saldoAtualPartida,
            List<FluxoCaixaPrevistoItemDTO> itensPrevistos,
            FluxoCaixaSimulacaoRequestDTO simulacao) {
        return List.of(
                calcularCenario(
                        "OTIMISTA",
                        "Cenario Otimista",
                        "100% dos recebiveis a vencer na data prevista + recuperacao de 50% dos inadimplentes em ate 15 dias; 100% das obrigacoes previstas.",
                        new BigDecimal("100.00"),
                        new BigDecimal("0.50"),
                        new BigDecimal("1.00"),
                        hoje,
                        saldoAtualPartida,
                        itensPrevistos,
                        simulacao),
                calcularCenario(
                        "REALISTA",
                        "Cenario Realista (Base)",
                        "100% dos repasses de cartao + 92% dos recebiveis diretos/boletos no vencimento + 20% de recuperacao de vencidos; 100% das obrigacoes.",
                        new BigDecimal("92.00"),
                        new BigDecimal("0.20"),
                        new BigDecimal("1.00"),
                        hoje,
                        saldoAtualPartida,
                        itensPrevistos,
                        simulacao),
                calcularCenario(
                        "PESSIMISTA",
                        "Cenario Pessimista (Estresse de Liquidez)",
                        "100% dos repasses de cartao + 75% dos demais recebiveis, 0% de recuperacao de vencidos e margem de contingencia de +10% nas saidas.",
                        new BigDecimal("75.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("1.10"),
                        hoje,
                        saldoAtualPartida,
                        itensPrevistos,
                        simulacao));
    }

    private FluxoCaixaCenarioProjecaoDTO calcularCenario(
            String codigo,
            String nome,
            String explicacao,
            BigDecimal pctRecebiveisDiretos,
            BigDecimal fatorRecuperacaoVencidos,
            BigDecimal multiplicadorSaidas,
            LocalDate hoje,
            BigDecimal saldoPartida,
            List<FluxoCaixaPrevistoItemDTO> itensPrevistos,
            FluxoCaixaSimulacaoRequestDTO simulacao) {
        Map<LocalDate, BigDecimal> entPorDia = new HashMap<>();
        Map<LocalDate, BigDecimal> saiPorDia = new HashMap<>();
        Map<LocalDate, Integer> qtdPorDia = new HashMap<>();

        BigDecimal fatorDireto = pctRecebiveisDiretos.divide(CEM, 4, RoundingMode.HALF_UP);

        for (FluxoCaixaPrevistoItemDTO item : itensPrevistos) {
            LocalDate dataBase = item.dataPrevistaLiquidacao() != null ? item.dataPrevistaLiquidacao() : item.dataVencimento();
            if ("ENTRADA".equals(item.natureza())) {
                boolean isCartao = "CARTAO_CREDITO".equals(item.modalidade()) || "CARTAO_DEBITO".equals(item.modalidade());
                if ("VENCIDO".equals(item.situacaoVencimento())) {
                    BigDecimal rec = item.saldoAbertoLiquido().multiply(fatorRecuperacaoVencidos);
                    if (rec.compareTo(BigDecimal.ZERO) > 0) {
                        LocalDate dataRec = hoje.plusDays(15);
                        entPorDia.merge(dataRec, rec, BigDecimal::add);
                        qtdPorDia.merge(dataRec, 1, Integer::sum);
                    }
                } else {
                    LocalDate alvo = dataBase.isBefore(hoje) ? hoje : dataBase;
                    if (simulacao != null && Boolean.TRUE.equals(simulacao.simularAntecipacaoCartao()) && isCartao) {
                        BigDecimal taxaSimPct = nvl(simulacao.taxaAntecipacaoSimuladaPercentual()).max(new BigDecimal("2.00"));
                        BigDecimal descAnt = item.saldoAbertoLiquido().multiply(taxaSimPct).divide(CEM, 2, RoundingMode.HALF_UP);
                        entPorDia.merge(hoje.plusDays(1), item.saldoAbertoLiquido().subtract(descAnt).max(BigDecimal.ZERO), BigDecimal::add);
                        qtdPorDia.merge(hoje.plusDays(1), 1, Integer::sum);
                    } else {
                        BigDecimal val = isCartao ? item.saldoAbertoLiquido() : item.saldoAbertoLiquido().multiply(fatorDireto);
                        entPorDia.merge(alvo, val, BigDecimal::add);
                        qtdPorDia.merge(alvo, 1, Integer::sum);
                    }
                }
            } else {
                LocalDate alvo = dataBase.isBefore(hoje) ? hoje.plusDays(1) : dataBase;
                BigDecimal val = item.saldoAbertoLiquido().multiply(multiplicadorSaidas);
                saiPorDia.merge(alvo, val, BigDecimal::add);
                qtdPorDia.merge(alvo, 1, Integer::sum);
            }
        }

        // Aplica ajustes de simulacao se informados (sem alterar o caixa realizado)
        if (simulacao != null) {
            LocalDate dataSim = simulacao.dataSimulacao() != null && !simulacao.dataSimulacao().isBefore(hoje)
                    ? simulacao.dataSimulacao()
                    : hoje.plusDays(7);
            if (nvl(simulacao.entradaSimuladaAdicional()).compareTo(BigDecimal.ZERO) > 0) {
                entPorDia.merge(dataSim, simulacao.entradaSimuladaAdicional(), BigDecimal::add);
            }
            if (nvl(simulacao.saidaSimuladaAdicional()).compareTo(BigDecimal.ZERO) > 0) {
                saiPorDia.merge(dataSim, simulacao.saidaSimuladaAdicional(), BigDecimal::add);
            }
            if (nvl(simulacao.investimentoPlanejado()).compareTo(BigDecimal.ZERO) > 0) {
                saiPorDia.merge(dataSim, simulacao.investimentoPlanejado(), BigDecimal::add);
            }
            if (simulacao.itensSimulados() != null) {
                for (FluxoCaixaSimulacaoRequestDTO.ItemSimulacaoManualDTO itemSim : simulacao.itensSimulados()) {
                    if (itemSim.valor() == null || itemSim.valor().compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    LocalDate dt = itemSim.dataPrevista() != null && !itemSim.dataPrevista().isBefore(hoje)
                            ? itemSim.dataPrevista()
                            : dataSim;
                    if ("ENTRADA".equalsIgnoreCase(itemSim.natureza())) {
                        entPorDia.merge(dt, itemSim.valor(), BigDecimal::add);
                    } else {
                        saiPorDia.merge(dt, itemSim.valor(), BigDecimal::add);
                    }
                }
            }
        }

        BigDecimal saldoAcum = saldoPartida;
        BigDecimal menorSaldo = saldoPartida;
        LocalDate dataMenor = hoje;

        BigDecimal s7 = saldoPartida;
        BigDecimal s15 = saldoPartida;
        BigDecimal s30 = saldoPartida;
        BigDecimal s60 = saldoPartida;
        BigDecimal s90 = saldoPartida;

        List<FluxoCaixaDiaCalendarioDTO> serie = new ArrayList<>(91);
        for (int d = 0; d <= 90; d++) {
            LocalDate data = hoje.plusDays(d);
            BigDecimal ep = scale2(entPorDia.getOrDefault(data, BigDecimal.ZERO));
            BigDecimal sp = scale2(saiPorDia.getOrDefault(data, BigDecimal.ZERO));
            BigDecimal liq = ep.subtract(sp);
            saldoAcum = saldoAcum.add(liq);

            if (saldoAcum.compareTo(menorSaldo) < 0) {
                menorSaldo = saldoAcum;
                dataMenor = data;
            }
            if (d == 7) s7 = saldoAcum;
            if (d == 15) s15 = saldoAcum;
            if (d == 30) s30 = saldoAcum;
            if (d == 60) s60 = saldoAcum;
            if (d == 90) s90 = saldoAcum;

            serie.add(new FluxoCaixaDiaCalendarioDTO(
                    data,
                    simulacao != null ? "SIMULADO" : "PROJETADO",
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    ep,
                    sp,
                    scale2(liq),
                    scale2(saldoAcum),
                    qtdPorDia.getOrDefault(data, 0),
                    saldoAcum.compareTo(BigDecimal.ZERO) < 0));
        }

        boolean risco = menorSaldo.compareTo(BigDecimal.ZERO) < 0;
        BigDecimal capitalGiroAdicional = risco ? scale2(menorSaldo.abs()) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new FluxoCaixaCenarioProjecaoDTO(
                codigo,
                nome,
                explicacao,
                scale2(pctRecebiveisDiretos),
                scale2(multiplicadorSaidas.subtract(BigDecimal.ONE).multiply(CEM)),
                scale2(saldoPartida),
                scale2(s7),
                scale2(s15),
                scale2(s30),
                scale2(s60),
                scale2(s90),
                scale2(menorSaldo),
                dataMenor,
                risco,
                capitalGiroAdicional,
                serie);
    }

    private DiagnosticoLiquidezDTO construirDiagnosticoLiquidez(
            LocalDate hoje,
            Long empresaId,
            Long currentFilialId,
            Long filialFiltroId,
            boolean consolidarFiliais,
            Long centroCustoFiltroId,
            BigDecimal disponibilidadeAtual,
            BigDecimal geracaoCaixaPeriodo,
            BigDecimal entradasPendentes,
            BigDecimal entradasPendentes30d,
            BigDecimal entradasVencidas,
            BigDecimal saidasPendentes,
            BigDecimal saidasPendentes30d,
            BigDecimal saidasVencidas,
            BigDecimal comprometimentoFornecedores,
            BigDecimal repassesCartaoBruto,
            BigDecimal taxasCartaoPrevistas,
            BigDecimal repassesCartaoLiquido,
            BigDecimal recebiveisAntecipados,
            BigDecimal recebiveisTotaisCartao,
            FluxoCaixaCenarioProjecaoDTO cenarioRealista,
            Map<String, BigDecimal> pressaoPorGrupo) {

        YearMonth ymAtual = YearMonth.from(hoje);
        DreGerencialResponseDTO dreAtual = dreGerencialService.apurarDre(
                new DreGerencialService.FiltroDreRequest(
                        "MENSAL",
                        ymAtual.toString(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null,
                        centroCustoFiltroId,
                        filialFiltroId,
                        consolidarFiliais),
                empresaId,
                currentFilialId);

        BigDecimal resultadoDre = nvl(dreAtual.indicadores().resultadoLiquidoGerencial());
        BigDecimal diffDreCaixa = scale2(resultadoDre.subtract(geracaoCaixaPeriodo));

        BigDecimal totalCarteiraReceber = entradasPendentes.add(entradasVencidas);
        BigDecimal taxaInadimplencia = totalCarteiraReceber.compareTo(BigDecimal.ZERO) > 0
                ? entradasVencidas.multiply(CEM).divide(totalCarteiraReceber, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal depAntecipacao = recebiveisTotaisCartao.compareTo(BigDecimal.ZERO) > 0
                ? recebiveisAntecipados.multiply(CEM).divide(recebiveisTotaisCartao, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        boolean riscoInsuf = cenarioRealista.riscoInsuficienciaCaixa()
                || disponibilidadeAtual.compareTo(BigDecimal.ZERO) < 0;
        boolean sustentaOperacao = !riscoInsuf && cenarioRealista.saldoProjetado30Dias().compareTo(BigDecimal.ZERO) >= 0;

        // Margem de seguranca para investimento: menor saldo projetado positivo nos proximos 60 dias acima do colchao minimo de obrigacoes
        BigDecimal menorSaldo60 = cenarioRealista.menorSaldoProjetado();
        BigDecimal reservaMinima = saidasPendentes30d.multiply(new BigDecimal("0.25")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal margemInvestimento = menorSaldo60.subtract(reservaMinima).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        boolean podeInvestir = !riscoInsuf && margemInvestimento.compareTo(new BigDecimal("100.00")) >= 0;

        String nivelSaude;
        String manchete;
        if (riscoInsuf) {
            nivelSaude = "CRITICO";
            manchete = "Alerta de liquidez: o fluxo projetado aponta risco de saldo negativo (menor saldo R$ "
                    + cenarioRealista.menorSaldoProjetado() + " em " + cenarioRealista.dataMenorSaldo()
                    + "), exigindo reforco ou renegociacao de R$ " + cenarioRealista.necessidadeCapitalGiroAdicional() + ".";
        } else if (saidasVencidas.compareTo(BigDecimal.ZERO) > 0 || taxaInadimplencia.compareTo(new BigDecimal("15.00")) > 0) {
            nivelSaude = "ATENCAO";
            manchete = "Disponibilidade positiva de R$ " + scale2(disponibilidadeAtual)
                    + ", porem ha obrigacoes vencidas (R$ " + scale2(saidasVencidas)
                    + ") ou inadimplencia de clientes (" + taxaInadimplencia + "%) que requerem acao imediata.";
        } else {
            nivelSaude = "SAUDAVEL";
            manchete = "Disponibilidade imediata de R$ " + scale2(disponibilidadeAtual)
                    + " e saldo projetado em 30 dias de R$ " + cenarioRealista.saldoProjetado30Dias()
                    + ", com capacidade de honrar compromissos no horizonte de 90 dias.";
        }

        String expDreCaixa;
        if (resultadoDre.compareTo(geracaoCaixaPeriodo) > 0) {
            expDreCaixa = "A DRE apurou resultado economico de R$ " + scale2(resultadoDre)
                    + ", enquanto a geracao liquida de caixa foi de R$ " + scale2(geracaoCaixaPeriodo)
                    + " (diferenca de R$ " + diffDreCaixa + " retida em vendas a prazo/cartoes a receber ou pagamento de passivos anteriores).";
        } else if (resultadoDre.compareTo(geracaoCaixaPeriodo) < 0) {
            expDreCaixa = "A geracao de caixa (R$ " + scale2(geracaoCaixaPeriodo)
                    + ") superou o resultado por competencia da DRE (R$ " + scale2(resultadoDre)
                    + "), impulsionada pelo recebimento de parcelas anteriores ou prazos concedidos por fornecedores.";
        } else {
            expDreCaixa = "O resultado economico da DRE (R$ " + scale2(resultadoDre)
                    + ") esta alinhado com a geracao de caixa realizada no periodo.";
        }

        List<String> contasComprometendo = pressaoPorGrupo.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(e -> e.getKey() + ": R$ " + scale2(e.getValue()))
                .toList();

        List<String> alertas = new ArrayList<>();
        List<String> recomendacoes = new ArrayList<>();

        if (riscoInsuf) {
            alertas.add("Risco de insuficiencia de caixa detectado em " + cenarioRealista.dataMenorSaldo()
                    + " (necessidade estimada de capital de giro: R$ " + cenarioRealista.necessidadeCapitalGiroAdicional() + "). [Fonte: Projecao Realista 90d]");
            recomendacoes.add("Escalone vencimentos junto a fornecedores ou acione cobranca de recebiveis antes de "
                    + cenarioRealista.dataMenorSaldo() + " para evitar descoberto bancario.");
        }
        if (saidasVencidas.compareTo(BigDecimal.ZERO) > 0) {
            alertas.add("Existem R$ " + scale2(saidasVencidas)
                    + " em contas a pagar/despesas vencidas pendentes de baixa. [Fonte: Contas a Pagar / Despesas]");
            recomendacoes.add("Regularize ou renegocie as obrigacoes vencidas para evitar encargos moratorios na Linha 14 da DRE.");
        }
        if (entradasVencidas.compareTo(BigDecimal.ZERO) > 0) {
            alertas.add("Existem R$ " + scale2(entradasVencidas) + " em contas a receber vencidas ("
                    + taxaInadimplencia + "% da carteira aberta). [Fonte: Contas a Receber]");
            recomendacoes.add("Acione a regua de cobranca para recuperar os R$ " + scale2(entradasVencidas) + " de clientes em atraso.");
        }
        if (depAntecipacao.compareTo(new BigDecimal("35.00")) > 0) {
            alertas.add("Dependencia elevada de antecipacao de recebiveis (" + depAntecipacao
                    + "% do volume de cartoes). [Fonte: Recebiveis de Cartao]");
            recomendacoes.add("Reduza gradualmente a antecipacao de cartoes para preservar a margem liquida contra taxas financeiras.");
        }
        if (alertas.isEmpty()) {
            alertas.add("Nenhum alerta critico de liquidez identificado para os proximos 90 dias. [Fonte: Painel de Fluxo de Caixa]");
        }
        if (podeInvestir) {
            recomendacoes.add("A empresa possui margem de seguranca estimada de R$ " + margemInvestimento
                    + " para novos investimentos sem comprometer a reserva operacional. [Fonte: Projecao de Liquidez]");
        } else if (!riscoInsuf) {
            recomendacoes.add("Preserve o caixa atual como capital de giro operacional antes de assumir imobilizacoes ou expansoes. [Fonte: Projecao de Liquidez]");
        }

        return new DiagnosticoLiquidezDTO(
                nivelSaude,
                manchete,
                scale2(disponibilidadeAtual),
                scale2(entradasPendentes30d),
                scale2(saidasPendentes30d),
                scale2(saidasVencidas),
                scale2(entradasVencidas),
                taxaInadimplencia,
                scale2(comprometimentoFornecedores),
                scale2(repassesCartaoBruto),
                scale2(taxasCartaoPrevistas),
                scale2(repassesCartaoLiquido),
                depAntecipacao,
                scale2(geracaoCaixaPeriodo),
                scale2(resultadoDre),
                diffDreCaixa,
                expDreCaixa,
                riscoInsuf,
                sustentaOperacao,
                podeInvestir,
                margemInvestimento,
                contasComprometendo,
                alertas,
                recomendacoes);
    }

    // =========================================================================
    // Parsers de Extrato (OFX e CSV) e Helpers Internos
    // =========================================================================

    private List<ImportacaoExtratoRequestDTO.LinhaExtratoManualDTO> parseOfx(String ofx) {
        List<ImportacaoExtratoRequestDTO.LinhaExtratoManualDTO> list = new ArrayList<>();
        Matcher matcher = OFX_TRN_PATTERN.matcher(ofx);
        while (matcher.find()) {
            String block = matcher.group(1);
            String trnAmt = extractOfxTag(block, "TRNAMT");
            String dtPosted = extractOfxTag(block, "DTPOSTED");
            String fitId = extractOfxTag(block, "FITID");
            String memo = extractOfxTag(block, "MEMO");
            if (memo == null || memo.isBlank()) {
                memo = extractOfxTag(block, "NAME");
            }
            if (trnAmt == null || trnAmt.isBlank()) {
                continue;
            }
            BigDecimal rawAmt = new BigDecimal(trnAmt.trim().replace(',', '.'));
            LocalDate date = LocalDate.now();
            if (dtPosted != null && dtPosted.trim().length() >= 8) {
                String ymd = dtPosted.trim().substring(0, 8);
                date = LocalDate.parse(ymd, DateTimeFormatter.BASIC_ISO_DATE);
            }
            String natureza = rawAmt.signum() < 0 ? "DEBITO" : "CREDITO";
            list.add(new ImportacaoExtratoRequestDTO.LinhaExtratoManualDTO(
                    fitId,
                    date,
                    memo != null ? memo : "Lancamento OFX",
                    rawAmt.abs(),
                    natureza));
        }
        return list;
    }

    private String extractOfxTag(String block, String tag) {
        Pattern p = Pattern.compile("<" + tag + ">([^<\\r\\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(block);
        return m.find() ? m.group(1).trim() : null;
    }

    private List<ImportacaoExtratoRequestDTO.LinhaExtratoManualDTO> parseCsvExtrato(String csv) {
        List<ImportacaoExtratoRequestDTO.LinhaExtratoManualDTO> list = new ArrayList<>();
        String[] lines = csv.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.toLowerCase(Locale.ROOT).startsWith("data;")) {
                continue;
            }
            String[] parts = trimmed.split(";");
            if (parts.length < 3) {
                continue;
            }
            LocalDate data = LocalDate.parse(parts[0].trim());
            String desc = parts[1].trim();
            BigDecimal valor = new BigDecimal(parts[2].trim().replace(',', '.'));
            String natureza = parts.length >= 4 && !parts[3].isBlank()
                    ? parts[3].trim().toUpperCase(Locale.ROOT)
                    : (valor.signum() < 0 ? "DEBITO" : "CREDITO");
            String idExt = parts.length >= 5 ? parts[4].trim() : null;
            list.add(new ImportacaoExtratoRequestDTO.LinhaExtratoManualDTO(
                    idExt,
                    data,
                    desc,
                    valor.abs(),
                    natureza));
        }
        return list;
    }

    private void garantirContasePadraoInterno(Long empresaId, Long filialId) {
        List<ContaFinanceira> atuais = contaFinanceiraRepository.findByEmpresaIdAndFilialIdOrderByPadraoDescNomeAsc(empresaId, filialId);
        if (!atuais.isEmpty()) {
            return;
        }
        LocalDate hoje = LocalDate.now();
        salvarContaSeed(empresaId, filialId, "CX-LOJA", "Caixa Fisico Loja", TipoContaFinanceira.CAIXA_FISICO, null, null, hoje, true);
        salvarContaSeed(empresaId, filialId, "BC-PRINCIPAL", "Conta Bancaria Principal", TipoContaFinanceira.CONTA_CORRENTE, "001", "Banco Principal", hoje, false);
        salvarContaSeed(empresaId, filialId, "OP-CARTAO", "Adquirente / Operadora de Cartoes", TipoContaFinanceira.OPERADORA_CARTAO, null, "Operadora Cartoes", hoje, false);
    }

    private ContaFinanceira salvarContaSeed(
            Long empresaId,
            Long filialId,
            String codigo,
            String nome,
            TipoContaFinanceira tipo,
            String bancoCod,
            String bancoNome,
            LocalDate dataIni,
            boolean padrao) {
        ContaFinanceira c = new ContaFinanceira();
        c.setEmpresaId(empresaId);
        c.setFilialId(filialId);
        c.setCodigo(codigo);
        c.setNome(nome);
        c.setTipoConta(tipo);
        c.setBancoCodigo(bancoCod);
        c.setBancoNome(bancoNome);
        c.setSaldoInicial(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        c.setSaldoAtual(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        c.setDataSaldoInicial(dataIni);
        c.setPadrao(padrao);
        c.setAtivo(true);
        c.setCriadoEm(LocalDateTime.now());
        return contaFinanceiraRepository.save(c);
    }

    private ContaFinanceira resolverContaFinanceiraOperacao(Long contaId, Long empresaId, Long filialId) {
        if (contaId != null) {
            return contaFinanceiraRepository.findByIdContaFinanceiraAndEmpresaIdAndFilialId(contaId, empresaId, filialId)
                    .orElseThrow(() -> new EntityNotFoundException("Conta financeira nao encontrada no tenant."));
        }
        garantirContasePadraoInterno(empresaId, filialId);
        return contaFinanceiraRepository
                .findFirstByEmpresaIdAndFilialIdAndPadraoTrueAndAtivoTrueOrderByIdContaFinanceiraAsc(empresaId, filialId)
                .or(() -> contaFinanceiraRepository
                        .findByEmpresaIdAndFilialIdAndAtivoTrueOrderByPadraoDescNomeAsc(empresaId, filialId)
                        .stream()
                        .findFirst())
                .orElseThrow(() -> new IllegalStateException("Nenhuma conta financeira ativa disponivel na filial."));
    }

    private void registrarDespesaFinanceiraAutomatica(
            Long empresaId,
            Long filialId,
            LocalDate dataCompetencia,
            BigDecimal valorTaxa,
            String descricao,
            Long centroCustoId) {
        PlanoContasGerencial conta901 = planoContasRepository
                .findByCodigoAndEmpresaIdAndFilialId("9.01", empresaId, filialId)
                .orElseGet(() -> {
                    PlanoContasGerencial pc = new PlanoContasGerencial();
                    pc.setEmpresaId(empresaId);
                    pc.setFilialId(filialId);
                    pc.setCodigo("9.01");
                    pc.setNome("Despesas Financeiras e Tarifas");
                    pc.setDescricao("Juros, tarifas bancarias, taxas de cartao e descontos financeiros");
                    pc.setTipoConta(TipoContaGerencial.DESPESA);
                    pc.setNatureza(NaturezaContaGerencial.DEBITO);
                    pc.setClassificacaoCusto(ClassificacaoCusto.VARIAVEL);
                    pc.setGrupoDre(GrupoDreGerencial.DESPESA_FINANCEIRA);
                    pc.setOrdemExibicao(210);
                    pc.setAceitaLancamento(true);
                    pc.setAtivo(true);
                    pc.setCriadoEm(LocalDateTime.now());
                    return planoContasRepository.save(pc);
                });
        TipoDespesa tipoFinanceiro = tipoDespesaRepository
                .findByNomeIgnoreCaseAndEmpresaIdAndFilialId("Taxas e Tarifas Financeiras", empresaId, filialId)
                .orElseGet(() -> {
                    TipoDespesa td = new TipoDespesa();
                    td.setNome("Taxas e Tarifas Financeiras");
                    td.setStatus(1);
                    td.setEmpresaId(empresaId);
                    td.setFilialId(filialId);
                    return tipoDespesaRepository.save(td);
                });
        Despesa d = new Despesa();
        d.setEmpresaId(empresaId);
        d.setFilialId(filialId);
        d.setDescricao(descricao);
        d.setValor(scale2(valorTaxa));
        d.setDataCompetencia(dataCompetencia);
        d.setDataEmissao(dataCompetencia);
        d.setDataVencimento(dataCompetencia);
        d.setDataPagamento(dataCompetencia);
        d.setStatus(1);
        d.setTipoDespesa(tipoFinanceiro);
        d.setPlanoContaId(conta901.getIdPlanoConta());
        d.setCentroCustoId(centroCustoId);
        despesaRepository.save(d);
    }

    private void desmarcarOutrasPadrao(Long empresaId, Long filialId, Long excetoId) {
        for (ContaFinanceira c : contaFinanceiraRepository.findByEmpresaIdAndFilialIdOrderByPadraoDescNomeAsc(empresaId, filialId)) {
            if (c.isPadrao() && !Objects.equals(c.getIdContaFinanceira(), excetoId)) {
                c.setPadrao(false);
                contaFinanceiraRepository.save(c);
            }
        }
    }

    private BigDecimal calcularSaldoAbertoReceber(ContaReceber cr) {
        if (cr.getSaldo() != null && cr.getSaldo().compareTo(BigDecimal.ZERO) > 0) {
            return cr.getSaldo();
        }
        return nvl(cr.getValorLiquido()).subtract(nvl(cr.getValorPago())).max(BigDecimal.ZERO);
    }

    private BigDecimal calcularSaldoAbertoPagar(ContaPagar cp) {
        if (cp.getSaldo() != null && cp.getSaldo().compareTo(BigDecimal.ZERO) > 0) {
            return cp.getSaldo();
        }
        if (nvl(cp.getValorPago()).compareTo(BigDecimal.ZERO) > 0) {
            return nvl(cp.getValor()).subtract(nvl(cp.getValorPago())).max(BigDecimal.ZERO);
        }
        return cp.getStatus() == FinanceiroStatus.PAGO ? BigDecimal.ZERO : nvl(cp.getValor());
    }

    private BigDecimal geracaoLiquidaSemContaFinanceira(List<MovimentacaoFinanceiraResponseDTO> movs) {
        BigDecimal soma = BigDecimal.ZERO;
        for (MovimentacaoFinanceiraResponseDTO m : movs) {
            if (m.idMovimentacaoFinanceira() != null && m.idMovimentacaoFinanceira() < 0 && m.contaFinanceiraId() == null) {
                if (m.tipoMovimentacao() == TipoMovimentacaoFinanceira.ENTRADA) {
                    soma = soma.add(nvl(m.valorLiquido()));
                } else if (m.tipoMovimentacao() == TipoMovimentacaoFinanceira.SAIDA) {
                    soma = soma.subtract(nvl(m.valorLiquido()));
                }
            }
        }
        return soma;
    }

    private LocalDate avancarPeriodicidade(LocalDate base, String periodicidade) {
        if (base == null) {
            return null;
        }
        String p = periodicidade == null ? "MENSAL" : periodicidade.trim().toUpperCase(Locale.ROOT);
        return switch (p) {
            case "SEMANAL" -> base.plusWeeks(1);
            case "QUINZENAL" -> base.plusDays(15);
            case "BIMESTRAL" -> base.plusMonths(2);
            case "TRIMESTRAL" -> base.plusMonths(3);
            case "SEMESTRAL" -> base.plusMonths(6);
            case "ANUAL" -> base.plusYears(1);
            default -> base.plusMonths(1);
        };
    }

    private FluxoCaixaPrevistoItemDTO toPrevistoReceber(
            ContaReceber cr,
            LocalDate hoje,
            Map<Long, String> clientes,
            Map<Long, PlanoContasGerencial> planos,
            Map<Long, CentroCusto> centros) {
        LocalDate venc = cr.getVencimento() != null ? cr.getVencimento() : hoje;
        LocalDate liqPrev = cr.getDataPrevistaRepasse() != null ? cr.getDataPrevistaRepasse() : venc;
        String sit = liqPrev.isBefore(hoje) ? "VENCIDO" : (liqPrev.isEqual(hoje) ? "VENCE_HOJE" : "A_VENCER");
        long dias = ChronoUnit.DAYS.between(hoje, liqPrev);
        BigDecimal saldo = scale2(calcularSaldoAbertoReceber(cr));
        ModalidadeRecebivel mod = cr.getModalidadeRecebivel() != null ? cr.getModalidadeRecebivel() : ModalidadeRecebivel.DIRETO;

        return new FluxoCaixaPrevistoItemDTO(
                mod == ModalidadeRecebivel.CARTAO_CREDITO || mod == ModalidadeRecebivel.CARTAO_DEBITO
                        ? "REPASSE_CARTAO"
                        : "CONTA_RECEBER",
                cr.getIdContaReceber(),
                "ENTRADA",
                cr.getDescricao() != null ? cr.getDescricao() : "Conta a Receber #" + cr.getIdContaReceber(),
                cr.getClienteId() != null ? clientes.getOrDefault(cr.getClienteId(), "Cliente #" + cr.getClienteId()) : "Cliente / Operadora",
                cr.getDataCompetencia() != null ? cr.getDataCompetencia() : venc,
                venc,
                liqPrev,
                cr.getNumeroParcela() != null ? cr.getNumeroParcela() : 1,
                cr.getTotalParcelas() != null ? cr.getTotalParcelas() : 1,
                mod.name(),
                sit,
                dias,
                scale2(cr.getValorBruto()),
                scale2(cr.getValorPago()),
                scale2(cr.getValorTaxa()),
                saldo,
                false,
                cr.isAntecipado(),
                cr.getPlanoContaId(),
                cr.getPlanoContaId() != null && planos.containsKey(cr.getPlanoContaId())
                        ? planos.get(cr.getPlanoContaId()).getNome()
                        : null,
                cr.getCentroCustoId(),
                cr.getCentroCustoId() != null && centros.containsKey(cr.getCentroCustoId())
                        ? centros.get(cr.getCentroCustoId()).getNome()
                        : null,
                cr.getFilialId());
    }

    private FluxoCaixaPrevistoItemDTO toPrevistoPagar(
            ContaPagar cp,
            LocalDate hoje,
            Map<Long, String> fornecedores,
            Map<Long, PlanoContasGerencial> planos,
            Map<Long, CentroCusto> centros) {
        LocalDate venc = cp.getVencimento() != null ? cp.getVencimento() : hoje;
        String sit = venc.isBefore(hoje) ? "VENCIDO" : (venc.isEqual(hoje) ? "VENCE_HOJE" : "A_VENCER");
        long dias = ChronoUnit.DAYS.between(hoje, venc);
        BigDecimal saldo = scale2(calcularSaldoAbertoPagar(cp));

        return new FluxoCaixaPrevistoItemDTO(
                "CONTA_PAGAR",
                cp.getIdContaPagar(),
                "SAIDA",
                cp.getDescricao(),
                cp.getFornecedorId() != null ? fornecedores.getOrDefault(cp.getFornecedorId(), "Fornecedor #" + cp.getFornecedorId()) : "Obrigacao Operacional",
                cp.getDataCompetencia() != null ? cp.getDataCompetencia() : venc,
                venc,
                venc,
                cp.getNumeroParcela() != null ? cp.getNumeroParcela() : 1,
                cp.getTotalParcelas() != null ? cp.getTotalParcelas() : 1,
                cp.isRecorrente() ? "RECORRENTE" : "TITULO_PAGAR",
                sit,
                dias,
                scale2(cp.getValor()),
                scale2(cp.getValorPago()),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                saldo,
                cp.isRecorrente(),
                false,
                cp.getPlanoContaId(),
                cp.getPlanoContaId() != null && planos.containsKey(cp.getPlanoContaId())
                        ? planos.get(cp.getPlanoContaId()).getNome()
                        : null,
                cp.getCentroCustoId(),
                cp.getCentroCustoId() != null && centros.containsKey(cp.getCentroCustoId())
                        ? centros.get(cp.getCentroCustoId()).getNome()
                        : null,
                cp.getFilialId());
    }

    private ContaFinanceiraResponseDTO toContaResponse(ContaFinanceira c, String filialNome) {
        return new ContaFinanceiraResponseDTO(
                c.getIdContaFinanceira(),
                c.getEmpresaId(),
                c.getFilialId(),
                filialNome,
                c.getCodigo(),
                c.getNome(),
                c.getTipoConta(),
                c.getBancoCodigo(),
                c.getBancoNome(),
                c.getAgencia(),
                c.getNumeroConta(),
                scale2(c.getSaldoInicial()),
                scale2(c.getSaldoAtual()),
                c.getDataSaldoInicial(),
                c.isPadrao(),
                c.isAtivo(),
                c.getCriadoEm(),
                c.getAtualizadoEm());
    }

    private MovimentacaoFinanceiraResponseDTO toMovimentacaoResponse(
            MovimentacaoFinanceira m,
            Map<Long, ContaFinanceira> contas,
            Map<Long, PlanoContasGerencial> planos,
            Map<Long, CentroCusto> centros) {
        ContaFinanceira c = m.getContaFinanceiraId() == null ? null : contas.get(m.getContaFinanceiraId());
        PlanoContasGerencial pc = m.getPlanoContaId() == null ? null : planos.get(m.getPlanoContaId());
        CentroCusto cc = m.getCentroCustoId() == null ? null : centros.get(m.getCentroCustoId());
        return new MovimentacaoFinanceiraResponseDTO(
                m.getIdMovimentacaoFinanceira(),
                m.getEmpresaId(),
                m.getFilialId(),
                m.getContaFinanceiraId(),
                c != null ? c.getNome() : "Conta #" + m.getContaFinanceiraId(),
                m.getTipoMovimentacao(),
                m.getOrigemTipo(),
                m.getOrigemId(),
                m.getDescricao(),
                m.getDocumentoReferencia(),
                m.getDataMovimentacao(),
                m.getDataHoraRegistro(),
                scale2(m.getValorBruto()),
                scale2(m.getValorTaxa()),
                scale2(m.getValorJurosDesconto()),
                scale2(m.getValorLiquido()),
                m.getPlanoContaId(),
                pc != null ? pc.getNome() : null,
                m.getCentroCustoId(),
                cc != null ? cc.getNome() : null,
                m.getTransferenciaParId(),
                m.getMovimentacaoEstornadaId(),
                m.isEstornado(),
                m.isConciliado(),
                m.getConciliacaoItemId(),
                m.getCriadoPor());
    }

    private ConciliacaoItemResponseDTO toConciliacaoItemResponse(
            ConciliacaoFinanceiraItem it,
            Map<Long, ContaFinanceira> contas) {
        ContaFinanceira c = contas.get(it.getContaFinanceiraId());
        return new ConciliacaoItemResponseDTO(
                it.getIdConciliacaoItem(),
                it.getContaFinanceiraId(),
                c != null ? c.getNome() : "Conta #" + it.getContaFinanceiraId(),
                it.getTipoConciliacao(),
                it.getFormatoOrigem(),
                it.getIdentificadorExtrato(),
                it.getDataExtrato(),
                it.getDescricaoExtrato(),
                scale2(it.getValorExtrato()),
                it.getNaturezaExtrato(),
                it.getStatusConciliacao(),
                it.getMovimentacaoVinculadaId(),
                it.getDocumentoOrigemTipo(),
                it.getDocumentoOrigemId(),
                scale2(it.getDiferencaValor()),
                it.getObservacao(),
                it.getConciliadoPor(),
                it.getConciliadoEm());
    }

    private List<Long> resolverFiliaisEscopo(
            Long filialSolicitadaId,
            Boolean consolidarFiliais,
            Long empresaId,
            Long currentFilialId) {
        List<Filial> filiais = filialRepository.findByEmpresaIdEmpresaAndDeletedAtIsNullOrderByIdFilialAsc(empresaId);
        Set<Long> validIds = new LinkedHashSet<>();
        for (Filial f : filiais) {
            if (f.getStatus() != 0) {
                validIds.add(f.getIdFilial());
            }
        }
        validIds.add(currentFilialId);
        if (Boolean.TRUE.equals(consolidarFiliais)) {
            return List.copyOf(validIds);
        }
        Long alvo = filialSolicitadaId != null ? filialSolicitadaId : currentFilialId;
        if (!validIds.contains(alvo)) {
            throw new EntityNotFoundException("Filial nao encontrada no escopo da empresa ativa.");
        }
        return List.of(alvo);
    }

    private Map<Long, String> mapaNomesFiliais(Long empresaId, Long currentFilialId) {
        Map<Long, String> mapa = new LinkedHashMap<>();
        for (Filial f : filialRepository.findByEmpresaIdEmpresaAndDeletedAtIsNullOrderByIdFilialAsc(empresaId)) {
            mapa.put(f.getIdFilial(), f.getNome());
        }
        mapa.putIfAbsent(currentFilialId, "Filial #" + currentFilialId);
        return mapa;
    }

    private Map<Long, PlanoContasGerencial> carregarPlanosPorId(Long empresaId, List<Long> filiaisIds) {
        Map<Long, PlanoContasGerencial> map = new HashMap<>();
        for (Long fId : filiaisIds) {
            for (PlanoContasGerencial p : planoContasRepository.findByEmpresaIdAndFilialIdOrderByOrdemExibicaoAscCodigoAsc(empresaId, fId)) {
                map.put(p.getIdPlanoConta(), p);
            }
        }
        return map;
    }

    private Map<Long, CentroCusto> carregarCentrosPorId(Long empresaId, List<Long> filiaisIds) {
        Map<Long, CentroCusto> map = new HashMap<>();
        for (Long fId : filiaisIds) {
            for (CentroCusto cc : centroCustoRepository.findByEmpresaIdAndFilialIdOrderByNomeAsc(empresaId, fId)) {
                map.put(cc.getIdCentroCusto(), cc);
            }
        }
        return map;
    }

    private Map<Long, String> carregarClientesPorId(Long empresaId, List<Long> filiaisIds) {
        Map<Long, String> map = new HashMap<>();
        for (Long fId : filiaisIds) {
            for (Cliente c : clienteRepository.findByEmpresaIdAndFilialIdAndDeletedAtIsNullOrderByNomeRazaoAsc(empresaId, fId)) {
                map.put(c.getIdCliente(), c.getNomeRazao());
            }
        }
        return map;
    }

    private Map<Long, String> carregarFornecedoresPorId(Long empresaId, List<Long> filiaisIds) {
        Map<Long, String> map = new HashMap<>();
        for (Long fId : filiaisIds) {
            for (Fornecedor f : fornecedorRepository.findByEmpresaIdAndFilialIdOrderByRazaoSocialAsc(empresaId, fId)) {
                map.put(f.getIdFornecedor(), f.getRazaoSocial());
            }
        }
        return map;
    }

    private void registrarAuditoria(
            String acao,
            String entidade,
            Long idEntidade,
            String resumo,
            String justificativa,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        financialAuditTrailService.record(
                acao,
                entidade,
                idEntidade,
                privacyGuardService.currentUserLogin(request),
                null,
                resumo,
                Map.of("resumo", resumo),
                empresaId,
                filialId);
        auditTrailService.registrarPrivacidade(
                acao,
                entidade,
                idEntidade,
                "Financeiro",
                null,
                resumo,
                request,
                justificativa);
    }

    private boolean within(LocalDate date, LocalDate inicio, LocalDate fim) {
        return date != null && !date.isBefore(inicio) && !date.isAfter(fim);
    }

    private String normalizeRequired(String value, String msg) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(msg);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private BigDecimal positive(BigDecimal val, String msg) {
        if (val == null || val.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(msg);
        }
        return val;
    }

    private BigDecimal scale2(BigDecimal val) {
        return nvl(val).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }

    private void requireTenant(Long empresaId, Long filialId) {
        if (empresaId == null || filialId == null) {
            throw new IllegalArgumentException("Empresa e filial ativas sao obrigatorias.");
        }
    }
}
