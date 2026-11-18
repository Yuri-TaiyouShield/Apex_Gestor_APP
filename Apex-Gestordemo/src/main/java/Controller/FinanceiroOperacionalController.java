package Controller;

import DTO.*;
import Model.ContaPagar;
import Model.ContaReceber;
import Model.CreditoCliente;
import Repository.*;
import Service.*;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/financeiro/operacional")
public class FinanceiroOperacionalController {
    @Autowired private FinanceiroOperacionalService service;
    @Autowired private AuthorizationService authorizationService;
    @Autowired private TenantScopeService tenantScopeService;
    @Autowired private CreditoClienteService creditoClienteService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private VendaRepository vendaRepository;
    @Autowired private FormaPagamentoRepository formaPagamentoRepository;
    @Autowired private FornecedorRepository fornecedorRepository;
    @Autowired private CategoriaRepository categoriaRepository;

    @GetMapping("/contas-receber")
    public List<ContaReceberResponseDTO> receber(@RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long formaPagamentoId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate vencimentoInicio,
            @RequestParam(required = false) LocalDate vencimentoFim,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_VIEW, "Financeiro", "listar contas a receber");
        Long empresaId = empresa(request);
        Long filialId = filial(request);
        Map<Long, String> clientesById = clienteRepository
                .findByEmpresaIdAndFilialIdAndDeletedAtIsNullOrderByNomeRazaoAsc(empresaId, filialId)
                .stream()
                .collect(Collectors.toMap(Model.Cliente::getIdCliente, Model.Cliente::getNomeRazao, (a, b) -> a));
        Map<Long, String> vendasById = vendaRepository
                .findByEmpresaIdAndFilialIdOrderByDataVendaDesc(empresaId, filialId)
                .stream()
                .filter(v -> v.getNumeroVenda() != null)
                .collect(Collectors.toMap(Model.Venda::getIdVenda, Model.Venda::getNumeroVenda, (a, b) -> a));
        Map<Long, String> formasById = formaPagamentoRepository
                .findVisibleToTenant(empresaId, filialId)
                .stream()
                .collect(Collectors.toMap(Model.FormaPagamento::getIdFormaPagamento, Model.FormaPagamento::getNome, (a, b) -> a));
        return service.listarReceber(empresaId, filialId).stream()
                .filter(c -> clienteId == null || clienteId.equals(c.getClienteId()))
                .filter(c -> formaPagamentoId == null || formaPagamentoId.equals(c.getFormaPagamentoId()))
                .filter(c -> status == null || status.isBlank() || c.getStatus().name().equalsIgnoreCase(status))
                .filter(c -> vencimentoInicio == null || !c.getVencimento().isBefore(vencimentoInicio))
                .filter(c -> vencimentoFim == null || !c.getVencimento().isAfter(vencimentoFim))
                .map(c -> toResponse(c, clientesById, vendasById, formasById)).toList();
    }

    @GetMapping("/contas-pagar")
    public List<ContaPagarResponseDTO> pagar(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_VIEW, "Financeiro", "listar contas a pagar");
        Long empresaId = empresa(request);
        Long filialId = filial(request);
        Map<Long, String> fornecedoresById = fornecedorRepository
                .findByEmpresaIdAndFilialIdOrderByRazaoSocialAsc(empresaId, filialId)
                .stream()
                .collect(Collectors.toMap(Model.Fornecedor::getIdFornecedor, Model.Fornecedor::getRazaoSocial, (a, b) -> a));
        Map<Long, String> categoriasById = categoriaRepository
                .findVisibleToTenant(empresaId, filialId)
                .stream()
                .collect(Collectors.toMap(Model.Categoria::getIdCategoria, Model.Categoria::getDescricao, (a, b) -> a));
        Map<Long, String> formasById = formaPagamentoRepository
                .findVisibleToTenant(empresaId, filialId)
                .stream()
                .collect(Collectors.toMap(Model.FormaPagamento::getIdFormaPagamento, Model.FormaPagamento::getNome, (a, b) -> a));
        return service.listarPagar(empresaId, filialId).stream()
                .map(c -> toResponse(c, fornecedoresById, categoriasById, formasById)).toList();
    }

    @PostMapping("/contas-pagar")
    @ResponseStatus(HttpStatus.CREATED)
    public ContaPagarResponseDTO criar(@Valid @RequestBody ContaPagarRequestDTO body, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_MANAGE, "Financeiro", "criar conta a pagar");
        return toResponse(service.criarContaPagar(body, empresa(request), filial(request), request));
    }

    @GetMapping("/creditos")
    public List<CreditoClienteResponseDTO> creditos(@RequestParam Long clienteId, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.FINANCE_VIEW, "Financeiro", "listar creditos do cliente");
        return creditoClienteService.listarDisponiveis(clienteId, empresa(request), filial(request)).stream()
                .map(this::toResponse).toList();
    }

    private ContaReceberResponseDTO toResponse(
            ContaReceber c,
            Map<Long, String> clientesById,
            Map<Long, String> vendasById,
            Map<Long, String> formasById) {
        String cliente = c.getClienteId() == null ? null : clientesById.get(c.getClienteId());
        String numeroVenda = c.getVendaId() == null ? null : vendasById.get(c.getVendaId());
        String forma = c.getFormaPagamentoId() == null ? "Misto" : formasById.get(c.getFormaPagamentoId());
        return new ContaReceberResponseDTO(c.getIdContaReceber(), c.getVendaId(), numeroVenda,
                c.getClienteId(), cliente, c.getFormaPagamentoId(), forma, c.getCondicaoPagamentoId(),
                c.getCaixaSessaoId(), c.getValorBruto(), c.getDesconto(), c.getValorLiquido(),
                c.getValorPago(), c.getSaldo(), c.getVencimento(), c.getStatus().name(), c.getEmpresaId(), c.getFilialId());
    }

    private ContaPagarResponseDTO toResponse(
            ContaPagar c,
            Map<Long, String> fornecedoresById,
            Map<Long, String> categoriasById,
            Map<Long, String> formasById) {
        String fornecedor = c.getFornecedorId() == null ? null : fornecedoresById.get(c.getFornecedorId());
        String categoria = c.getCategoriaId() == null ? null : categoriasById.get(c.getCategoriaId());
        String forma = c.getFormaPagamentoPrevistaId() == null ? null : formasById.get(c.getFormaPagamentoPrevistaId());
        return new ContaPagarResponseDTO(c.getIdContaPagar(), c.getFornecedorId(), fornecedor,
                c.getCategoriaId(), categoria, c.getDescricao(), c.getValor(), c.getVencimento(),
                c.getFormaPagamentoPrevistaId(), forma, c.getStatus().name(), c.getEmpresaId(), c.getFilialId());
    }

    private ContaPagarResponseDTO toResponse(ContaPagar c) {
        String fornecedor = c.getFornecedorId() == null ? null : fornecedorRepository
                .findByIdFornecedorAndEmpresaIdAndFilialId(c.getFornecedorId(), c.getEmpresaId(), c.getFilialId())
                .map(Model.Fornecedor::getRazaoSocial).orElse(null);
        String categoria = c.getCategoriaId() == null ? null : categoriaRepository
                .findVisibleById(c.getCategoriaId(), c.getEmpresaId(), c.getFilialId())
                .map(Model.Categoria::getDescricao).orElse(null);
        String forma = c.getFormaPagamentoPrevistaId() == null ? null : formaPagamentoRepository
                .findVisibleById(c.getFormaPagamentoPrevistaId(), c.getEmpresaId(), c.getFilialId())
                .map(Model.FormaPagamento::getNome).orElse(null);
        return new ContaPagarResponseDTO(c.getIdContaPagar(), c.getFornecedorId(), fornecedor,
                c.getCategoriaId(), categoria, c.getDescricao(), c.getValor(), c.getVencimento(),
                c.getFormaPagamentoPrevistaId(), forma, c.getStatus().name(), c.getEmpresaId(), c.getFilialId());
    }

    private CreditoClienteResponseDTO toResponse(CreditoCliente c) {
        return new CreditoClienteResponseDTO(c.getIdCredito(), c.getClienteId(), c.getVendaOrigemId(),
                c.getDevolucaoId(), c.getValorOriginal(), c.getSaldo(), c.getStatus().name(),
                c.getEmpresaId(), c.getFilialId(), c.getCriadoEm());
    }

    private Long empresa(HttpServletRequest r) { return tenantScopeService.empresaId(r); }
    private Long filial(HttpServletRequest r) { return tenantScopeService.filialId(r); }
}
