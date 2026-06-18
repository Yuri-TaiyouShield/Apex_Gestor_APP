package Controller;

import DTO.VendaRequestDTO;
import DTO.VendaResponseDTO;
import Model.Cliente;
import Model.FormaPagamento;
import Model.Produto;
import Model.ProdutoVenda;
import Model.Usuario;
import Model.Venda;
import Model.VendaPagamento;
import Service.AuditTrailService;
import Service.AuthorizationService;
import Service.CaixaService;
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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendas")
@CrossOrigin(originPatterns = {"http://localhost:*", "capacitor://localhost", "ionic://localhost", "app://localhost"})
public class VendaController {

    @Autowired
    private VendaService vendaService;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private CaixaService caixaService;
    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private TenantScopeService tenantScopeService;

    @GetMapping
    public List<VendaResponseDTO> listarTodos(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SALE_CREATE, "Vendas", "listar vendas");
        return vendaService.listarTodos(empresaId(request), filialId(request)).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendaResponseDTO> buscarPorId(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SALE_CREATE, "Vendas", "consultar venda");
        return vendaService.buscarPorId(id, empresaId(request), filialId(request))
                .map(venda -> ResponseEntity.ok(toResponse(venda)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<VendaResponseDTO> realizarVenda(@Valid @RequestBody VendaRequestDTO requestBody,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SALE_CREATE, "Vendas", "finalizar venda");
        validarPermissaoDesconto(requestBody, request);
        Venda novaVenda = vendaService.realizarVenda(toEntity(requestBody), empresaId(request), filialId(request));
        if (requestBody.caixaSessaoId() != null || authorizationService.can(request, ApexPermission.CASHIER_VIEW)) {
            caixaService.registrarVenda(novaVenda.getValorTotal(), novaVenda.getIdVenda(), request);
        }
        auditTrailService.registrarPrivacidade("VENDA_FINALIZADA", "Venda", novaVenda.getIdVenda(), "Vendas",
                null, resumoVenda(novaVenda), request, "Venda operacional finalizada.");
        return new ResponseEntity<>(toResponse(novaVenda), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<VendaResponseDTO> cancelarVenda(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SALE_CANCEL, "Vendas", "cancelar venda");
        Venda cancelada = vendaService.cancelarVenda(id, empresaId(request), filialId(request));
        auditTrailService.registrarPrivacidade("VENDA_CANCELADA", "Venda", cancelada.getIdVenda(), "Vendas",
                null, resumoVenda(cancelada), request, "Cancelamento de venda com estorno de estoque.");
        return ResponseEntity.ok(toResponse(cancelada));
    }

    private Venda toEntity(VendaRequestDTO request) {
        Venda venda = new Venda();
        Cliente cliente = new Cliente();
        cliente.setIdCliente(request.clienteId());
        venda.setCliente(cliente);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(request.usuarioId() != null ? request.usuarioId() : 1L);
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
        for (VendaRequestDTO.PagamentoDTO pagamento : request.pagamentos()) {
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

    private VendaResponseDTO toResponse(Venda venda) {
        return new VendaResponseDTO(
                venda.getIdVenda(),
                venda.getCliente() == null ? null : venda.getCliente().getIdCliente(),
                venda.getCliente() == null ? null : venda.getCliente().getNomeRazao(),
                venda.getUsuario() == null ? null : venda.getUsuario().getIdUsuario(),
                venda.getUsuario() == null ? null : venda.getUsuario().getNome(),
                venda.getStatus(),
                venda.getValorTotal(),
                venda.getDesconto(),
                venda.getObservacao(),
                venda.getEmpresaId(),
                venda.getFilialId(),
                venda.getCaixaSessaoId(),
                venda.getCondicaoPagamentoId(),
                venda.getDataVenda(),
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
                            pagamento.getNumeroParcelas()))
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

    private Long empresaId(HttpServletRequest request) {
        return tenantScopeService.empresaId(request);
    }

    private Long filialId(HttpServletRequest request) {
        return tenantScopeService.filialId(request);
    }
}
