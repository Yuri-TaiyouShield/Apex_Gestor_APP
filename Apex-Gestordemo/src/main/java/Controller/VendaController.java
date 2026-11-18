package Controller;

import DTO.VendaRequestDTO;
import DTO.VendaResponseDTO;
import DTO.MotivoOperacaoRequestDTO;
import DTO.VendaDevolucaoRequestDTO;
import DTO.VendaDevolucaoResponseDTO;
import DTO.VendaEventoResponseDTO;
import Model.Cliente;
import Model.FormaPagamento;
import Model.Produto;
import Model.ProdutoVenda;
import Model.Usuario;
import Model.Venda;
import Model.VendaPagamento;
import Service.AuditTrailService;
import Service.AuthorizationService;
import Service.PrivacyGuardService;
import Service.TenantScopeService;
import Service.VendaService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendas")

public class VendaController {

    @Autowired
    private VendaService vendaService;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private TenantScopeService tenantScopeService;
    @Autowired
    private PrivacyGuardService privacyGuardService;

    @GetMapping
    public List<VendaResponseDTO> listarTodos(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SALE_CREATE, "Vendas", "listar vendas");
        return filtrarEscopoVendedor(vendaService.listarTodos(empresaId(request), filialId(request)), request).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/historico")
    public List<VendaResponseDTO> historico(@RequestParam(required = false) String q,
            @RequestParam(required = false) String status, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SALE_HISTORY_VIEW, "Vendas", "consultar historico");
        List<Venda> vendas = filtrarEscopoVendedor(
                vendaService.listarTodos(empresaId(request), filialId(request)), request);
        String termo = q == null ? "" : q.trim().toLowerCase();
        if (termo.replaceAll("\\D", "").length() >= 11) {
            auditTrailService.registrarPrivacidade("VENDA_BUSCA_DOCUMENTO", "Venda", null, "Vendas",
                    null, "busca sensivel mascarada", request, "Pesquisa no historico por identificador documental.");
        }
        return vendas.stream()
                .filter(v -> status == null || status.isBlank()
                        || status.equalsIgnoreCase(status(v)))
                .filter(v -> termo.isBlank() || corresponde(v, termo))
                .map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendaResponseDTO> buscarPorId(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SALE_CREATE, "Vendas", "consultar venda");
        return vendaService.buscarPorId(id, empresaId(request), filialId(request))
                .map(venda -> {
                    requireOwnerIfSeller(venda, request);
                    return ResponseEntity.ok(toResponse(venda));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<VendaResponseDTO> realizarVenda(@Valid @RequestBody VendaRequestDTO requestBody,
            HttpServletRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        authorizationService.require(request, ApexPermission.SALE_CREATE, "Vendas", "enviar venda ao caixa");
        validarPermissaoDesconto(requestBody, request);
        Venda novaVenda = vendaService.realizarVenda(toEntity(requestBody, request), empresaId(request), filialId(request),
                request, idempotencyKey);
        return new ResponseEntity<>(toResponse(novaVenda), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/enviar-caixa")
    public VendaResponseDTO reenviar(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SALE_CREATE, "Vendas", "reenviar venda ao caixa");
        requireOwnerIfSeller(vendaService.buscarPorId(id, empresaId(request), filialId(request))
                .orElseThrow(() -> new IllegalArgumentException("Venda nao encontrada no tenant.")), request);
        return toResponse(vendaService.reenviarAoCaixa(id, empresaId(request), filialId(request), request));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<VendaResponseDTO> cancelarVenda(@PathVariable Long id,
            @RequestBody(required = false) MotivoOperacaoRequestDTO body, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SALE_CANCEL, "Vendas", "cancelar venda");
        Venda cancelada = vendaService.cancelarVenda(id, empresaId(request), filialId(request),
                body == null ? "Cancelamento administrativo" : body.motivo(), request);
        auditTrailService.registrarPrivacidade("VENDA_CANCELADA", "Venda", cancelada.getIdVenda(), "Vendas",
                null, resumoVenda(cancelada), request, "Cancelamento de venda com estorno de estoque.");
        return ResponseEntity.ok(toResponse(cancelada));
    }

    @PostMapping("/{id}/devolucoes")
    public VendaDevolucaoResponseDTO devolver(@PathVariable Long id,
            @Valid @RequestBody VendaDevolucaoRequestDTO body, HttpServletRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        authorizationService.require(request, ApexPermission.SALE_RETURN, "Vendas", "trocar ou devolver venda");
        var devolucao = vendaService.registrarDevolucao(id, body, empresaId(request), filialId(request), request,
                idempotencyKey);
        return new VendaDevolucaoResponseDTO(devolucao.getIdVendaDevolucao(), devolucao.getVendaOriginalId(),
                devolucao.getTipo().name(), devolucao.getMotivo(), devolucao.getValorAjuste(), devolucao.getCriadoEm(),
                devolucao.getItens().stream().map(item -> new VendaDevolucaoResponseDTO.ItemDTO(
                        item.getProdutoOriginalId(), item.getProdutoNovoId(), item.getQuantidade(), item.getValorUnitario()))
                        .toList());
    }

    @GetMapping("/{id}/eventos")
    public List<VendaEventoResponseDTO> eventos(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SALE_HISTORY_VIEW, "Vendas", "consultar eventos da venda");
        Venda venda = vendaService.buscarPorId(id, empresaId(request), filialId(request))
                .orElseThrow(() -> new IllegalArgumentException("Venda nao encontrada no tenant."));
        requireOwnerIfSeller(venda, request);
        return vendaService.eventos(id, empresaId(request), filialId(request)).stream()
                .map(e -> new VendaEventoResponseDTO(e.getIdVendaEvento(), e.getEvento(), e.getStatusAnterior(),
                        e.getStatusNovo(), e.getMotivo(), e.getUsuarioLogin(), e.getCriadoEm())).toList();
    }

    private Venda toEntity(VendaRequestDTO request, HttpServletRequest httpRequest) {
        Venda venda = new Venda();
        Cliente cliente = new Cliente();
        cliente.setIdCliente(request.clienteId());
        venda.setCliente(cliente);
        Usuario usuario = new Usuario();
        Long currentUserId = privacyGuardService.currentUserId(httpRequest);
        usuario.setIdUsuario(currentUserId != null ? currentUserId : request.usuarioId());
        venda.setUsuario(usuario);
        venda.setDesconto(request.desconto() == null ? BigDecimal.ZERO : request.desconto());
        venda.setObservacao(request.observacao());
        venda.setCaixaSessaoId(request.caixaSessaoId());
        venda.setCondicaoPagamentoId(request.condicaoPagamentoId());
        List<ProdutoVenda> itens = new ArrayList<>();
        for (VendaRequestDTO.ItemDTO item : request.itens()) {
            Produto produto = new Produto();
            produto.setIdProduto(item.produtoId());
            ProdutoVenda produtoVenda = new ProdutoVenda();
            produtoVenda.setProduto(produto);
            produtoVenda.setQuantidade(item.quantidade());
            itens.add(produtoVenda);
        }
        venda.setItens(itens);
        List<VendaPagamento> pagamentos = new ArrayList<>();
        for (VendaRequestDTO.PagamentoDTO pagamento : request.pagamentos() == null ? List.<VendaRequestDTO.PagamentoDTO>of() : request.pagamentos()) {
            FormaPagamento formaPagamento = new FormaPagamento();
            formaPagamento.setIdFormaPagamento(pagamento.formaPagamentoId());
            VendaPagamento vendaPagamento = new VendaPagamento();
            vendaPagamento.setFormaPagamento(formaPagamento);
            vendaPagamento.setValorPago(pagamento.valorPago());
            vendaPagamento.setNumeroParcelas(pagamento.numeroParcelas());
            pagamentos.add(vendaPagamento);
        }
        venda.setPagamentos(pagamentos);
        return venda;
    }

    private void validarPermissaoDesconto(VendaRequestDTO request, HttpServletRequest httpRequest) {
        BigDecimal desconto = request.desconto() == null ? BigDecimal.ZERO : request.desconto();
        if (desconto.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        authorizationService.require(httpRequest, ApexPermission.SALE_DISCOUNT_APPLY, "Vendas", "aplicar desconto");
        if (desconto.compareTo(new BigDecimal("50.00")) > 0) {
            authorizationService.require(httpRequest, ApexPermission.SALE_DISCOUNT_OVERRIDE, "Vendas",
                    "autorizar desconto acima do limite operacional");
        }
        auditTrailService.registrarPrivacidade("VENDA_DESCONTO_APLICADO", "Venda", null, "Vendas",
                null, "desconto=" + desconto, httpRequest, "Desconto aplicado no PDV.");
    }

    public VendaResponseDTO toPublicResponse(Venda venda) {
        return toResponse(venda);
    }

    private VendaResponseDTO toResponse(Venda venda) {
        return new VendaResponseDTO(
                venda.getIdVenda(),
                venda.getCliente() == null ? null : venda.getCliente().getIdCliente(),
                venda.getCliente() == null ? null : venda.getCliente().getNomeRazao(),
                venda.getUsuario() == null ? null : venda.getUsuario().getIdUsuario(),
                venda.getUsuario() == null ? null : venda.getUsuario().getNome(),
                venda.getNumeroVenda(),
                status(venda),
                venda.getValorTotal(),
                venda.getDesconto(),
                venda.getObservacao(),
                venda.getEmpresaId(),
                venda.getFilialId(),
                venda.getCaixaSessaoId(),
                venda.getCondicaoPagamentoId(),
                venda.getFormaPagamentoRealId(),
                venda.getValorRecebido(),
                venda.getTroco(),
                venda.getDiferenca(),
                venda.getDataVenda(),
                venda.getEnviadoCaixaEm(),
                venda.getRecebidoCaixaEm(),
                venda.getFinalizadoEm(),
                venda.getMotivoRecusa(),
                safeItems(venda),
                safePagamentos(venda));
    }

    private List<VendaResponseDTO.ItemDTO> safeItems(Venda venda) {
        try {
            return venda.getItens() == null ? List.of() : venda.getItens().stream()
                    .map(item -> new VendaResponseDTO.ItemDTO(
                            item.getProduto() == null ? null : item.getProduto().getIdProduto(),
                            item.getProduto() == null ? null : item.getProduto().getDescricao(),
                            item.getQuantidade(),
                            item.getPrecoUnitario(),
                            item.getPrecoTotal()))
                    .toList();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private List<VendaResponseDTO.PagamentoDTO> safePagamentos(Venda venda) {
        try {
            return venda.getPagamentos() == null ? List.of() : venda.getPagamentos().stream()
                    .map(pagamento -> new VendaResponseDTO.PagamentoDTO(
                            pagamento.getFormaPagamento() == null ? null : pagamento.getFormaPagamento().getIdFormaPagamento(),
                            pagamento.getFormaPagamento() == null ? null : pagamento.getFormaPagamento().getNome(),
                            pagamento.getValorPago(),
                            pagamento.getNumeroParcelas(),
                            pagamento.getCreditoId()))
                    .toList();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private String resumoVenda(Venda venda) {
        return "id=" + venda.getIdVenda()
                + ";status=" + venda.getStatus()
                + ";valorTotal=" + venda.getValorTotal()
                + ";desconto=" + venda.getDesconto();
    }

    private List<Venda> filtrarEscopoVendedor(List<Venda> vendas, HttpServletRequest request) {
        boolean vendedorPuro = privacyGuardService.currentRoles(request).contains("ROLE_VENDEDOR")
                && !authorizationService.can(request, ApexPermission.SALE_CANCEL);
        Long usuarioId = privacyGuardService.currentUserId(request);
        return vendedorPuro && usuarioId != null
                ? vendas.stream().filter(v -> v.getUsuario() != null && usuarioId.equals(v.getUsuario().getIdUsuario())).toList()
                : vendas;
    }

    private boolean corresponde(Venda venda, String termo) {
        if (String.valueOf(venda.getIdVenda()).equals(termo)
                || contains(venda.getNumeroVenda(), termo)
                || contains(status(venda), termo)
                || (venda.getCliente() != null && (contains(venda.getCliente().getNomeRazao(), termo)
                    || contains(venda.getCliente().getCpfCnpj(), termo)))
                || (venda.getUsuario() != null && contains(venda.getUsuario().getNome(), termo))) {
            return true;
        }
        return safeItems(venda).stream().anyMatch(item -> contains(item.descricao(), termo)
                || String.valueOf(item.produtoId()).equals(termo)
                || venda.getItens().stream().anyMatch(raw -> raw.getProduto() != null
                    && raw.getProduto().getIdProduto().equals(item.produtoId())
                    && (contains(raw.getProduto().getCodigoBarras(), termo)
                        || contains(raw.getProduto().getCodigoInterno(), termo))));
    }

    private void requireOwnerIfSeller(Venda venda, HttpServletRequest request) {
        boolean vendedorPuro = privacyGuardService.currentRoles(request).contains("ROLE_VENDEDOR")
                && !authorizationService.can(request, ApexPermission.SALE_CANCEL);
        Long usuarioId = privacyGuardService.currentUserId(request);
        if (vendedorPuro && (usuarioId == null || venda.getUsuario() == null
                || !usuarioId.equals(venda.getUsuario().getIdUsuario()))) {
            throw new org.springframework.security.access.AccessDeniedException("Vendedor so pode acessar as proprias vendas.");
        }
    }

    private boolean contains(String value, String termo) {
        return value != null && value.toLowerCase().contains(termo);
    }

    private String status(Venda venda) {
        return venda.getStatusOperacional() == null
                ? (venda.getStatus() == 0 ? "CANCELADA" : "FINALIZADA")
                : venda.getStatusOperacional().name();
    }

    private Long empresaId(HttpServletRequest request) {
        return tenantScopeService.empresaId(request);
    }

    private Long filialId(HttpServletRequest request) {
        return tenantScopeService.filialId(request);
    }
}
