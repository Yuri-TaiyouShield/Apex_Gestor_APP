package Service;

import DTO.RecebimentoVendaRequestDTO;
import DTO.VendaDevolucaoRequestDTO;
import Model.*;
import Repository.*;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.text.Normalizer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VendaService {

    @Autowired private VendaRepository vendaRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private FormaPagamentoRepository formaPagamentoRepository;
    @Autowired private CondicaoPagamentoRepository condicaoPagamentoRepository;
    @Autowired private VendaPagamentoRepository vendaPagamentoRepository;
    @Autowired private VendaEventoRepository vendaEventoRepository;
    @Autowired private VendaDevolucaoRepository vendaDevolucaoRepository;
    @Autowired private EstoqueMovimentacaoService estoqueMovimentacaoService;
    @Autowired private CaixaService caixaService;
    @Autowired private FinanceiroOperacionalService financeiroOperacionalService;
    @Autowired private CreditoClienteService creditoClienteService;
    @Autowired private AuditTrailService auditTrailService;
    @Autowired private PrivacyGuardService privacyGuardService;

    @Transactional(readOnly = true)
    public List<Venda> listarTodos(Long empresaId, Long filialId) {
        List<Venda> vendas = vendaRepository.findByEmpresaIdAndFilialIdOrderByDataVendaDesc(empresaId, filialId);
        vendas.forEach(this::initialize);
        return vendas;
    }

    public List<Venda> listarTodos() {
        return vendaRepository.findAll();
    }

    public Optional<Venda> buscarPorId(Long id) {
        return vendaRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Venda> buscarPorId(Long id, Long empresaId, Long filialId) {
        Optional<Venda> venda = vendaRepository.findByIdVendaAndEmpresaIdAndFilialId(id, empresaId, filialId);
        venda.ifPresent(this::initialize);
        return venda;
    }

    @Transactional(readOnly = true)
    public List<Venda> filaCaixa(Long empresaId, Long filialId) {
        List<Venda> vendas = vendaRepository.findByEmpresaIdAndFilialIdAndStatusOperacionalOrderByEnviadoCaixaEmAsc(
                empresaId, filialId, VendaStatus.AGUARDANDO_CAIXA);
        vendas.forEach(this::initialize);
        return vendas;
    }

    @Transactional
    public Venda realizarVenda(Venda venda) {
        return realizarVenda(venda, venda.getEmpresaId(), venda.getFilialId(), null);
    }

    @Transactional
    public Venda realizarVenda(Venda venda, Long empresaId, Long filialId) {
        return realizarVenda(venda, empresaId, filialId, null);
    }

    @Transactional
    public Venda realizarVenda(Venda venda, Long empresaId, Long filialId, HttpServletRequest request) {
        return realizarVenda(venda, empresaId, filialId, request, null);
    }

    @Transactional
    public Venda realizarVenda(Venda venda, Long empresaId, Long filialId, HttpServletRequest request,
            String idempotencyKey) {
        if (empresaId == null || filialId == null) {
            throw new IllegalArgumentException("Empresa e filial ativas sao obrigatorias.");
        }
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        String payloadHash = salePayloadHash(venda);
        if (normalizedIdempotencyKey != null) {
            Optional<Venda> previous = vendaRepository.findByIdempotencyKeyAndEmpresaIdAndFilialId(
                    normalizedIdempotencyKey, empresaId, filialId);
            if (previous.isPresent()) {
                if (!payloadHash.equals(previous.get().getIdempotencyPayloadHash())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Idempotency-Key ja utilizada com outro payload.");
                }
                initialize(previous.get());
                return previous.get();
            }
        }
        if (venda.getItens() == null || venda.getItens().isEmpty()) {
            throw new IllegalArgumentException("Venda sem itens.");
        }
        if (venda.getCliente() == null || venda.getCliente().getIdCliente() == null) {
            throw new IllegalArgumentException("Cliente obrigatorio para venda.");
        }
        if (venda.getUsuario() == null || venda.getUsuario().getIdUsuario() == null) {
            throw new IllegalArgumentException("Vendedor autenticado nao identificado.");
        }

        Cliente cliente = clienteRepository.findByIdClienteAndEmpresaIdAndFilialId(
                venda.getCliente().getIdCliente(), empresaId, filialId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado no escopo da empresa/filial."));
        Usuario vendedor = usuarioRepository.findByIdUsuarioAndEmpresaIdAndFilialId(
                venda.getUsuario().getIdUsuario(), empresaId, filialId)
                .orElseThrow(() -> new IllegalArgumentException("Vendedor nao encontrado no escopo da empresa/filial."));
        if (venda.getCondicaoPagamentoId() != null) {
            CondicaoPagamento condicao = condicaoPagamentoRepository
                    .findByIdCondicaoPagamentoAndEmpresaIdAndFilialId(venda.getCondicaoPagamentoId(), empresaId, filialId)
                    .orElseThrow(() -> new IllegalArgumentException("Condicao de pagamento nao encontrada."));
        }

        List<ProdutoVenda> itensRecebidos = new ArrayList<>(venda.getItens());
        venda.setItens(new ArrayList<>());
        venda.setPagamentos(new ArrayList<>());
        venda.setCliente(cliente);
        venda.setUsuario(vendedor);
        venda.setEmpresaId(empresaId);
        venda.setFilialId(filialId);
        LocalDateTime agoraVenda = LocalDateTime.now();
        venda.setDataVenda(agoraVenda);
        venda.setDataEmissao(agoraVenda.toLocalDate());
        venda.setDataCompetencia(agoraVenda.toLocalDate());
        venda.setEnviadoCaixaEm(LocalDateTime.now());
        venda.setStatus(1);
        venda.setStatusOperacional(VendaStatus.AGUARDANDO_CAIXA);
        venda.setEstoqueMovimentado(false);
        venda.setIdempotencyKey(normalizedIdempotencyKey);
        venda.setIdempotencyPayloadHash(normalizedIdempotencyKey == null ? null : payloadHash);
        venda.setDesconto(nvl(venda.getDesconto()));
        // A venda precisa existir para compor as chaves dos itens, mas valor_total e obrigatorio no schema.
        venda.setValorTotal(BigDecimal.ZERO);
        Venda salva = vendaRepository.save(venda);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (ProdutoVenda recebido : itensRecebidos) {
            if (recebido.getProduto() == null || recebido.getProduto().getIdProduto() == null
                    || recebido.getQuantidade() <= 0) {
                throw new IllegalArgumentException("Item de venda invalido.");
            }
            Produto produto = produtoRepository.findByIdProdutoAndEmpresaIdAndFilialId(
                    recebido.getProduto().getIdProduto(), empresaId, filialId)
                    .orElseThrow(() -> new IllegalArgumentException("Produto nao encontrado no escopo da empresa/filial."));
            validarProdutoParaVenda(produto, recebido.getQuantidade());
            ProdutoVenda item = new ProdutoVenda();
            item.setVenda(salva);
            item.setProduto(produto);
            item.setId(new ProdutoVendaId(produto.getIdProduto(), salva.getIdVenda()));
            item.setQuantidade(recebido.getQuantidade());
            item.setCustoUnitario(produto.getCusto());
            item.setPrecoUnitario(produto.getValorVenda());
            item.setPrecoTotal(produto.getValorVenda().multiply(BigDecimal.valueOf(recebido.getQuantidade())));
            salva.getItens().add(item);
            subtotal = subtotal.add(item.getPrecoTotal());
        }
        if (venda.getDesconto().compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("Desconto nao pode superar o subtotal da venda.");
        }
        salva.setValorTotal(subtotal.subtract(venda.getDesconto()));
        salva = vendaRepository.save(salva);
        salva.setNumeroVenda("V-" + filialId + "-" + String.format(Locale.ROOT, "%08d", salva.getIdVenda()));
        salva = vendaRepository.save(salva);
        registrarEvento(salva, "ENVIADA_CAIXA", null, VendaStatus.AGUARDANDO_CAIXA, null, request);
        auditTrailService.registrarPrivacidade("VENDA_ENVIADA_CAIXA", "Venda", salva.getIdVenda(), "Vendas",
                null, resumoVenda(salva), request, "Venda criada pelo vendedor e enviada ao caixa.");
        return salva;
    }

    @Transactional
    public Venda reenviarAoCaixa(Long id, Long empresaId, Long filialId, HttpServletRequest request) {
        Venda venda = getScoped(id, empresaId, filialId);
        if (venda.getStatusOperacional() != VendaStatus.EM_ATENDIMENTO) {
            throw new IllegalArgumentException("Somente venda devolvida para ajuste pode ser reenviada.");
        }
        VendaStatus anterior = venda.getStatusOperacional();
        venda.setStatusOperacional(VendaStatus.AGUARDANDO_CAIXA);
        venda.setEnviadoCaixaEm(LocalDateTime.now());
        venda.setMotivoRecusa(null);
        Venda salva = vendaRepository.save(venda);
        registrarEvento(salva, "REENVIADA_CAIXA", anterior, salva.getStatusOperacional(), null, request);
        return salva;
    }

    @Transactional
    public Venda receberNoCaixa(Long id, RecebimentoVendaRequestDTO body,
            Long empresaId, Long filialId, HttpServletRequest request) {
        return receberNoCaixa(id, body, empresaId, filialId, request, null);
    }

    @Transactional
    public Venda receberNoCaixa(Long id, RecebimentoVendaRequestDTO body,
            Long empresaId, Long filialId, HttpServletRequest request, String idempotencyKey) {
        Venda venda = getScoped(id, empresaId, filialId);
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        String payloadHash = paymentPayloadHash(body);
        if (normalizedIdempotencyKey != null) {
            Optional<Venda> previous = vendaRepository.findByRecebimentoIdempotencyKeyAndEmpresaIdAndFilialId(
                    normalizedIdempotencyKey, empresaId, filialId);
            if (previous.isPresent() && !previous.get().getIdVenda().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency-Key ja utilizada em outra finalizacao.");
            }
            if (normalizedIdempotencyKey.equals(venda.getRecebimentoIdempotencyKey())) {
                if (!payloadHash.equals(venda.getRecebimentoIdempotencyPayloadHash())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Idempotency-Key ja utilizada com outro payload.");
                }
                if (venda.getStatusOperacional() == VendaStatus.FINALIZADA) {
                    return venda;
                }
            }
            venda.setRecebimentoIdempotencyKey(normalizedIdempotencyKey);
            venda.setRecebimentoIdempotencyPayloadHash(payloadHash);
        }
        if (venda.getStatusOperacional() != VendaStatus.AGUARDANDO_CAIXA) {
            throw new IllegalArgumentException("Venda nao esta aguardando caixa.");
        }
        CaixaSessao caixa = caixaService.caixaAberto(request)
                .orElseThrow(() -> new IllegalStateException("Caixa fechado bloqueia recebimento."));
        transicionar(venda, VendaStatus.EM_PAGAMENTO, "RECEBIMENTO_INICIADO", null, request);
        venda.setRecebidoCaixaEm(LocalDateTime.now());
        List<RecebimentoVendaRequestDTO.PagamentoDTO> solicitados = pagamentosSolicitados(body);
        if (solicitados.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos uma forma de pagamento.");
        }
        BigDecimal recebido = solicitados.stream().map(RecebimentoVendaRequestDTO.PagamentoDTO::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal troco = recebido.subtract(venda.getValorTotal());
        if (troco.signum() < 0) {
            throw new IllegalArgumentException("Valor recebido menor que o total da venda.");
        }
        Set<Long> formasUsadas = new HashSet<>();
        List<FormaPagamento> formas = new ArrayList<>();
        BigDecimal dinheiroRecebido = BigDecimal.ZERO;
        for (RecebimentoVendaRequestDTO.PagamentoDTO solicitado : solicitados) {
            if (!formasUsadas.add(solicitado.formaPagamentoId())) {
                throw new IllegalArgumentException("Agrupe valores da mesma forma de pagamento em uma unica parcela.");
            }
            FormaPagamento forma = formaPagamentoRepository.findVisibleById(
                    solicitado.formaPagamentoId(), empresaId, filialId)
                    .orElseThrow(() -> new IllegalArgumentException("Forma de pagamento nao encontrada no tenant."));
            formas.add(forma);
            if (isDinheiro(forma)) dinheiroRecebido = dinheiroRecebido.add(solicitado.valor());
            if (isCreditoVale(forma)) creditoClienteService.usar(solicitado.creditoId(), venda,
                    solicitado.valor(), request);
        }
        if (troco.signum() > 0 && dinheiroRecebido.compareTo(troco) < 0) {
            throw new IllegalArgumentException("Troco so pode ser gerado sobre a parcela em dinheiro.");
        }
        venda.setValorRecebido(recebido);
        venda.setTroco(troco);
        venda.setDiferenca(troco);
        venda.setFormaPagamentoRealId(formas.size() == 1 ? formas.get(0).getIdFormaPagamento() : null);
        venda.setCaixaSessaoId(caixa.getIdCaixaSessao());
        if (venda.getPagamentos() == null) {
            venda.setPagamentos(new ArrayList<>());
        }
        BigDecimal trocoPendente = troco;
        for (int i = 0; i < solicitados.size(); i++) {
            RecebimentoVendaRequestDTO.PagamentoDTO solicitado = solicitados.get(i);
            FormaPagamento forma = formas.get(i);
            BigDecimal valorEfetivo = solicitado.valor();
            if (trocoPendente.signum() > 0 && isDinheiro(forma)) {
                BigDecimal abatimento = valorEfetivo.min(trocoPendente);
                valorEfetivo = valorEfetivo.subtract(abatimento);
                trocoPendente = trocoPendente.subtract(abatimento);
            }
            VendaPagamento pagamento = new VendaPagamento();
            pagamento.setVenda(venda); pagamento.setFormaPagamento(forma);
            pagamento.setId(new VendaPagamentoId(venda.getIdVenda(), forma.getIdFormaPagamento()));
            pagamento.setValorPago(valorEfetivo);
            pagamento.setNumeroParcelas(solicitado.numeroParcelas() == null ? 1 : solicitado.numeroParcelas());
            pagamento.setCreditoId(solicitado.creditoId());
            venda.getPagamentos().add(pagamento);
        }

        baixarEstoque(venda);
        caixaService.registrarVenda(venda.getValorTotal(), venda.getIdVenda(), request);
        financeiroOperacionalService.registrarVendaFinalizada(venda, request);
        transicionar(venda, VendaStatus.PAGA, "PAGAMENTO_CONFIRMADO", null, request);
        LocalDateTime finalizadoAgora = LocalDateTime.now();
        venda.setFinalizadoEm(finalizadoAgora);
        if (venda.getDataEmissao() == null) {
            venda.setDataEmissao(finalizadoAgora.toLocalDate());
        }
        venda.setDataCompetencia(finalizadoAgora.toLocalDate());
        venda.setEstoqueMovimentado(true);
        transicionar(venda, VendaStatus.FINALIZADA, "VENDA_FINALIZADA", null, request);
        Venda finalizada = vendaRepository.save(venda);
        auditTrailService.registrarPrivacidade("VENDA_FINALIZADA_CAIXA", "Venda", finalizada.getIdVenda(), "Caixa",
                null, resumoVenda(finalizada), request, "Recebimento, estoque e financeiro confirmados pelo caixa.");
        initialize(finalizada);
        return finalizada;
    }

    @Transactional
    public Venda recusarNoCaixa(Long id, String motivo, Long empresaId, Long filialId, HttpServletRequest request) {
        Venda venda = getScoped(id, empresaId, filialId);
        if (venda.getStatusOperacional() != VendaStatus.AGUARDANDO_CAIXA
                && venda.getStatusOperacional() != VendaStatus.EM_PAGAMENTO) {
            throw new IllegalArgumentException("Venda nao pode ser devolvida para ajuste neste status.");
        }
        VendaStatus anterior = venda.getStatusOperacional();
        venda.setMotivoRecusa(requireMotivo(motivo));
        venda.setStatusOperacional(VendaStatus.EM_ATENDIMENTO);
        Venda salva = vendaRepository.save(venda);
        registrarEvento(salva, "RECUSADA_CAIXA", anterior, VendaStatus.EM_ATENDIMENTO, motivo, request);
        auditTrailService.registrarPrivacidade("VENDA_RECUSADA_CAIXA", "Venda", id, "Caixa",
                anterior.name(), VendaStatus.EM_ATENDIMENTO.name(), request, motivo);
        return salva;
    }

    @Transactional
    public Venda cancelarVenda(Long id, Long empresaId, Long filialId, String motivo, HttpServletRequest request) {
        Venda venda = getScoped(id, empresaId, filialId);
        if (venda.getStatusOperacional() == VendaStatus.CANCELADA) {
            throw new IllegalArgumentException("Venda ja cancelada.");
        }
        if (venda.isEstoqueMovimentado()) {
            devolverEstoque(venda, TipoMovimentacaoEstoque.CANCELAMENTO_VENDA, "Cancelamento de venda");
            venda.setEstoqueMovimentado(false);
        }
        financeiroOperacionalService.cancelarPorVenda(venda);
        venda.setMotivoCancelamento(requireMotivo(motivo));
        venda.setStatus(0);
        transicionar(venda, VendaStatus.CANCELADA, "VENDA_CANCELADA", motivo, request);
        return vendaRepository.save(venda);
    }

    @Transactional
    public Venda cancelarVenda(Long id, Long empresaId, Long filialId) {
        return cancelarVenda(id, empresaId, filialId, "Cancelamento administrativo", null);
    }

    @Transactional
    public Venda cancelarVenda(Long id) {
        Venda venda = vendaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Venda nao encontrada"));
        return cancelarVenda(id, venda.getEmpresaId(), venda.getFilialId(), "Cancelamento administrativo", null);
    }

    @Transactional
    public VendaDevolucao registrarDevolucao(Long vendaId, VendaDevolucaoRequestDTO body,
            Long empresaId, Long filialId, HttpServletRequest request) {
        return registrarDevolucao(vendaId, body, empresaId, filialId, request, null);
    }

    @Transactional
    public VendaDevolucao registrarDevolucao(Long vendaId, VendaDevolucaoRequestDTO body,
            Long empresaId, Long filialId, HttpServletRequest request, String idempotencyKey) {
        validateDevolucaoBody(body);
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        String payloadHash = devolucaoPayloadHash(vendaId, body);
        // O lock pessimista da venda deve ser adquirido antes do lookup da chave.
        // Assim, dois replays concorrentes se serializam na mesma venda e o segundo
        // enxerga o registro persistido pelo primeiro.
        Venda venda = getScoped(vendaId, empresaId, filialId);
        if (normalizedIdempotencyKey != null) {
            Optional<VendaDevolucao> previous = vendaDevolucaoRepository
                    .findByIdempotencyKeyAndEmpresaIdAndFilialId(normalizedIdempotencyKey, empresaId, filialId);
            if (previous.isPresent()) {
                if (!java.util.Objects.equals(vendaId, previous.get().getVendaOriginalId())
                        || !payloadHash.equals(previous.get().getIdempotencyPayloadHash())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Idempotency-Key ja utilizada com outro payload.");
                }
                Hibernate.initialize(previous.get().getItens());
                return previous.get();
            }
        }
        if (!List.of(VendaStatus.FINALIZADA, VendaStatus.TROCA_PARCIAL).contains(venda.getStatusOperacional())) {
            throw new IllegalArgumentException("Somente venda finalizada pode ser trocada ou devolvida.");
        }
        TipoDevolucao tipo;
        try {
            tipo = TipoDevolucao.valueOf(body.tipo().trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Tipo deve ser TOTAL, PARCIAL ou TROCA.");
        }
        String motivo = requireMotivo(body.motivo());
        VendaDevolucao devolucao = new VendaDevolucao();
        devolucao.setVendaOriginalId(vendaId);
        devolucao.setEmpresaId(empresaId);
        devolucao.setFilialId(filialId);
        devolucao.setIdempotencyKey(normalizedIdempotencyKey);
        devolucao.setIdempotencyPayloadHash(normalizedIdempotencyKey == null ? null : payloadHash);
        devolucao.setTipo(tipo);
        devolucao.setMotivo(motivo);
        devolucao.setCriadoPor(privacyGuardService.currentUserLogin(request));
        devolucao.setCriadoEm(LocalDateTime.now());
        devolucao.setValorAjuste(BigDecimal.ZERO);

        Set<Long> produtosNaRequisicao = new HashSet<>();
        for (VendaDevolucaoRequestDTO.ItemDTO itemBody : body.itens()) {
            if (!produtosNaRequisicao.add(itemBody.produtoOriginalId())) {
                throw new IllegalArgumentException(
                        "Cada produto original deve aparecer uma unica vez na devolucao.");
            }
        }
        List<VendaDevolucao> devolucoesExistentes = vendaDevolucaoRepository
                .findByVendaOriginalIdAndEmpresaIdAndFilialIdOrderByCriadoEmDesc(vendaId, empresaId, filialId);

        int totalVendido = venda.getItens().stream().mapToInt(ProdutoVenda::getQuantidade).sum();
        int totalOperado = 0;
        for (VendaDevolucaoRequestDTO.ItemDTO itemBody : body.itens()) {
            ProdutoVenda itemVenda = venda.getItens().stream()
                    .filter(item -> item.getProduto().getIdProduto().equals(itemBody.produtoOriginalId()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Produto nao pertence a venda original."));
            int jaDevolvido = devolucoesExistentes
                    .stream().flatMap(d -> d.getItens().stream())
                    .filter(i -> i.getProdutoOriginalId().equals(itemBody.produtoOriginalId()))
                    .mapToInt(VendaDevolucaoItem::getQuantidade).sum();
            if (jaDevolvido + itemBody.quantidade() > itemVenda.getQuantidade()) {
                throw new IllegalArgumentException("Quantidade devolvida supera a quantidade vendida.");
            }
            Produto original = produtoRepository.findByIdProdutoAndEmpresaIdAndFilialId(
                    itemBody.produtoOriginalId(), empresaId, filialId).orElseThrow();
            movimentarEstoque(original, itemBody.quantidade(), true, vendaId,
                    TipoMovimentacaoEstoque.DEVOLUCAO, "Entrada por troca/devolucao");
            if (tipo == TipoDevolucao.TROCA) {
                if (itemBody.produtoNovoId() == null) {
                    throw new IllegalArgumentException("Produto novo e obrigatorio em troca.");
                }
                Produto novo = produtoRepository.findByIdProdutoAndEmpresaIdAndFilialId(
                        itemBody.produtoNovoId(), empresaId, filialId)
                        .orElseThrow(() -> new IllegalArgumentException("Produto novo nao encontrado no tenant."));
                validarProdutoParaVenda(novo, itemBody.quantidade());
                movimentarEstoque(novo, itemBody.quantidade(), false, vendaId,
                        TipoMovimentacaoEstoque.TROCA_SAIDA, "Saida por troca de produto");
            }
            VendaDevolucaoItem item = new VendaDevolucaoItem();
            item.setDevolucao(devolucao);
            item.setProdutoOriginalId(itemBody.produtoOriginalId());
            item.setProdutoNovoId(itemBody.produtoNovoId());
            item.setQuantidade(itemBody.quantidade());
            item.setValorUnitario(itemVenda.getPrecoUnitario());
            item.setCustoUnitario(itemVenda.getCustoUnitario() != null ? itemVenda.getCustoUnitario() : original.getCusto());
            devolucao.getItens().add(item);
            devolucao.setValorAjuste(devolucao.getValorAjuste().add(
                    itemVenda.getPrecoUnitario().multiply(BigDecimal.valueOf(itemBody.quantidade()))));
            totalOperado += itemBody.quantidade();
        }
        VendaDevolucao salva = vendaDevolucaoRepository.save(devolucao);
        VendaStatus novoStatus = tipo == TipoDevolucao.TROCA
                ? (totalOperado == totalVendido ? VendaStatus.TROCA_TOTAL : VendaStatus.TROCA_PARCIAL)
                : (totalOperado == totalVendido ? VendaStatus.DEVOLVIDA : VendaStatus.TROCA_PARCIAL);
        venda.setTipoDevolucao(tipo);
        venda.setMotivoDevolucao(motivo);
        transicionar(venda, novoStatus, tipo == TipoDevolucao.TROCA ? "TROCA_REGISTRADA" : "DEVOLUCAO_REGISTRADA",
                motivo, request);
        if (tipo != TipoDevolucao.TROCA) {
            financeiroOperacionalService.ajustarPorDevolucao(
                    venda, devolucao.getValorAjuste(), totalOperado == totalVendido, false);
        }
        if (tipo != TipoDevolucao.TROCA) {
            creditoClienteService.gerarPorDevolucao(venda, salva, request);
        }
        auditTrailService.registrarPrivacidade("VENDA_" + tipo.name(), "Venda", vendaId, "Vendas",
                null, "devolucao=" + salva.getIdVendaDevolucao() + ";valor=" + salva.getValorAjuste(), request, motivo);
        return salva;
    }

    public List<VendaEvento> eventos(Long vendaId, Long empresaId, Long filialId) {
        return vendaEventoRepository.findByVendaIdAndEmpresaIdAndFilialIdOrderByCriadoEmAsc(vendaId, empresaId, filialId);
    }

    private void baixarEstoque(Venda venda) {
        for (ProdutoVenda item : venda.getItens()) {
            Produto produto = produtoRepository.findByIdProdutoAndEmpresaIdAndFilialId(
                    item.getProduto().getIdProduto(), venda.getEmpresaId(), venda.getFilialId()).orElseThrow();
            validarProdutoParaVenda(produto, item.getQuantidade());
            movimentarEstoque(produto, item.getQuantidade(), false, venda.getIdVenda(),
                    TipoMovimentacaoEstoque.SAIDA, "Saida automatica na finalizacao pelo caixa");
        }
    }

    private void devolverEstoque(Venda venda, TipoMovimentacaoEstoque tipo, String motivo) {
        for (ProdutoVenda item : venda.getItens()) {
            Produto produto = produtoRepository.findByIdProdutoAndEmpresaIdAndFilialId(
                    item.getProduto().getIdProduto(), venda.getEmpresaId(), venda.getFilialId()).orElseThrow();
            movimentarEstoque(produto, item.getQuantidade(), true, venda.getIdVenda(), tipo, motivo);
        }
    }

    private void movimentarEstoque(Produto produto, int quantidade, boolean entrada, Long vendaId,
            TipoMovimentacaoEstoque tipo, String motivo) {
        int anterior = produto.getQuantidadeEstoque();
        int posterior = entrada ? anterior + quantidade : anterior - quantidade;
        if (posterior < 0) {
            throw new IllegalArgumentException("Estoque insuficiente para concluir a operacao.");
        }
        produto.setQuantidadeEstoque(posterior);
        produtoRepository.save(produto);
        estoqueMovimentacaoService.registrar(produto, tipo, quantidade, anterior, posterior,
                "Venda", vendaId, motivo);
    }

    private void validarProdutoParaVenda(Produto produto, int quantidade) {
        if (produto.getStatus() != 1) {
            throw new IllegalArgumentException("Produto inativo nao pode ser vendido.");
        }
        if (produto.getCodigoBarras() == null || produto.getCodigoBarras().isBlank()) {
            throw new IllegalArgumentException("Produto vendavel exige codigo de barras.");
        }
        if (produto.getQuantidadeEstoque() < quantidade) {
            throw new IllegalArgumentException("Estoque insuficiente para o produto " + produto.getDescricao() + ".");
        }
    }

    private Venda getScoped(Long id, Long empresaId, Long filialId) {
        Venda venda = vendaRepository.findByIdVendaAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .orElseThrow(() -> new IllegalArgumentException("Venda nao encontrada no escopo da empresa/filial."));
        initialize(venda);
        if (venda.getStatusOperacional() == null) {
            venda.setStatusOperacional(venda.getStatus() == 0 ? VendaStatus.CANCELADA : VendaStatus.FINALIZADA);
        }
        return venda;
    }

    private void transicionar(Venda venda, VendaStatus novo, String evento, String motivo, HttpServletRequest request) {
        VendaStatus anterior = venda.getStatusOperacional();
        venda.setStatusOperacional(novo);
        vendaRepository.save(venda);
        registrarEvento(venda, evento, anterior, novo, motivo, request);
    }

    private void registrarEvento(Venda venda, String evento, VendaStatus anterior, VendaStatus novo,
            String motivo, HttpServletRequest request) {
        VendaEvento registro = new VendaEvento();
        registro.setVendaId(venda.getIdVenda());
        registro.setEmpresaId(venda.getEmpresaId());
        registro.setFilialId(venda.getFilialId());
        registro.setEvento(evento);
        registro.setStatusAnterior(anterior == null ? null : anterior.name());
        registro.setStatusNovo(novo == null ? null : novo.name());
        registro.setMotivo(motivo);
        registro.setUsuarioLogin(privacyGuardService.currentUserLogin(request));
        registro.setCriadoEm(LocalDateTime.now());
        vendaEventoRepository.save(registro);
    }

    private void initialize(Venda venda) {
        Hibernate.initialize(venda.getCliente());
        Hibernate.initialize(venda.getUsuario());
        Hibernate.initialize(venda.getItens());
        venda.getItens().forEach(item -> Hibernate.initialize(item.getProduto()));
        Hibernate.initialize(venda.getPagamentos());
        venda.getPagamentos().forEach(pagamento -> Hibernate.initialize(pagamento.getFormaPagamento()));
    }

    private String requireMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Motivo e obrigatorio.");
        }
        return motivo.trim();
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private List<RecebimentoVendaRequestDTO.PagamentoDTO> pagamentosSolicitados(RecebimentoVendaRequestDTO body) {
        if (body.pagamentos() != null && !body.pagamentos().isEmpty()) return body.pagamentos();
        if (body.formaPagamentoId() == null || body.valorRecebido() == null) return List.of();
        return List.of(new RecebimentoVendaRequestDTO.PagamentoDTO(
                body.formaPagamentoId(), body.valorRecebido(), body.numeroParcelas(), null));
    }

    private boolean isDinheiro(FormaPagamento forma) {
        return normalized(forma.getNome()).contains("dinheiro");
    }

    private boolean isCreditoVale(FormaPagamento forma) {
        String nome = normalized(forma.getNome());
        return nome.contains("credito") || nome.contains("vale");
    }

    private String normalized(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private String normalizeIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key excede 128 caracteres.");
        }
        return normalized;
    }

    private void validateDevolucaoBody(VendaDevolucaoRequestDTO body) {
        if (body == null || body.itens() == null || body.itens().isEmpty()) {
            throw new IllegalArgumentException("A devolucao deve conter ao menos um item.");
        }
        for (VendaDevolucaoRequestDTO.ItemDTO item : body.itens()) {
            if (item == null || item.produtoOriginalId() == null
                    || item.quantidade() == null || item.quantidade() < 1) {
                throw new IllegalArgumentException(
                        "Cada item da devolucao exige produto original e quantidade positiva.");
            }
        }
    }

    private String salePayloadHash(Venda venda) {
        StringBuilder payload = new StringBuilder()
                .append(venda.getCliente() == null ? null : venda.getCliente().getIdCliente()).append('|')
                .append(venda.getUsuario() == null ? null : venda.getUsuario().getIdUsuario()).append('|')
                .append(venda.getDesconto()).append('|').append(venda.getObservacao()).append('|')
                .append(venda.getCaixaSessaoId()).append('|').append(venda.getCondicaoPagamentoId());
        if (venda.getItens() != null) {
            venda.getItens().forEach(item -> payload.append('|')
                    .append(item.getProduto() == null ? null : item.getProduto().getIdProduto())
                    .append(':').append(item.getQuantidade()));
        }
        if (venda.getPagamentos() != null) {
            venda.getPagamentos().forEach(item -> payload.append('|')
                    .append(item.getFormaPagamento() == null ? null : item.getFormaPagamento().getIdFormaPagamento())
                    .append(':').append(item.getValorPago()).append(':').append(item.getNumeroParcelas()));
        }
        return sha256(payload.toString());
    }

    private String paymentPayloadHash(RecebimentoVendaRequestDTO body) {
        StringBuilder payload = new StringBuilder();
        pagamentosSolicitados(body).forEach(item -> payload.append(item.formaPagamentoId()).append(':')
                .append(item.valor()).append(':').append(item.numeroParcelas()).append(':').append(item.creditoId()).append('|'));
        return sha256(payload.toString());
    }

    private String devolucaoPayloadHash(Long vendaId, VendaDevolucaoRequestDTO body) {
        StringBuilder payload = new StringBuilder()
                .append(vendaId).append('|')
                .append(body.tipo() == null ? null : body.tipo().trim().toUpperCase(Locale.ROOT)).append('|')
                .append(body.motivo() == null ? null : body.motivo().trim());
        if (body.itens() != null) {
            body.itens().forEach(item -> payload.append('|')
                    .append(item.produtoOriginalId()).append(':')
                    .append(item.produtoNovoId()).append(':')
                    .append(item.quantidade()));
        }
        return sha256(payload.toString());
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel", ex);
        }
    }

    private String resumoVenda(Venda venda) {
        return "numero=" + venda.getNumeroVenda() + ";status=" + venda.getStatusOperacional()
                + ";valorTotal=" + venda.getValorTotal() + ";empresa=" + venda.getEmpresaId()
                + ";filial=" + venda.getFilialId();
    }
}
