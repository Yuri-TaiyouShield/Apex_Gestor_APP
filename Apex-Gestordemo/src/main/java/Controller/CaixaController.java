package Controller;

import DTO.CaixaOperacaoRequestDTO;
import DTO.CaixaSessaoResponseDTO;
import DTO.MotivoOperacaoRequestDTO;
import DTO.RecebimentoVendaRequestDTO;
import DTO.VendaResponseDTO;
import DTO.CreditoClienteResponseDTO;
import Service.AuthorizationService;
import Service.CaixaService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/caixa")

public class CaixaController {

    @Autowired
    private CaixaService caixaService;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired private VendaController vendaController;
    @Autowired private Service.VendaService vendaService;
    @Autowired private Service.TenantScopeService tenantScopeService;
    @Autowired private Service.CreditoClienteService creditoClienteService;

    @GetMapping("/atual")
    public ResponseEntity<CaixaSessaoResponseDTO> atual(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.CASHIER_VIEW, "Caixa", "consultar caixa atual");
        CaixaSessaoResponseDTO atual = caixaService.statusAtual(request);
        return atual == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(atual);
    }

    @GetMapping("/historico")
    public java.util.List<CaixaSessaoResponseDTO> historico(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.CASHIER_VIEW, "Caixa", "consultar historico de caixa");
        return caixaService.historico(request);
    }

    @PostMapping("/abrir")
    public ResponseEntity<CaixaSessaoResponseDTO> abrir(@Valid @RequestBody CaixaOperacaoRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.CASHIER_OPEN, "Caixa", "abrir caixa");
        return ResponseEntity.ok(caixaService.abrir(body.valor(), request));
    }

    @PostMapping("/{id}/suprimento")
    public ResponseEntity<CaixaSessaoResponseDTO> suprimento(@PathVariable Long id,
            @Valid @RequestBody CaixaOperacaoRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.CASHIER_SUPPLY, "Caixa", "registrar suprimento");
        return ResponseEntity.ok(caixaService.suprimento(id, body.valor(), body.observacao(), request));
    }

    @PostMapping("/{id}/sangria")
    public ResponseEntity<CaixaSessaoResponseDTO> sangria(@PathVariable Long id,
            @Valid @RequestBody CaixaOperacaoRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.CASHIER_WITHDRAW, "Caixa", "registrar sangria");
        return ResponseEntity.ok(caixaService.sangria(id, body.valor(), body.observacao(), request));
    }

    @PostMapping("/{id}/fechar")
    public ResponseEntity<CaixaSessaoResponseDTO> fechar(@PathVariable Long id,
            @RequestBody(required = false) CaixaOperacaoRequestDTO body, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.CASHIER_CLOSE, "Caixa", "fechar caixa");
        return ResponseEntity.ok(caixaService.fechar(id, body == null ? null : body.valor(), request));
    }

    @GetMapping("/vendas-pendentes")
    public java.util.List<VendaResponseDTO> pendentes(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SALE_RECEIVE, "Caixa", "listar fila de recebimento");
        return vendaService.filaCaixa(tenantScopeService.empresaId(request), tenantScopeService.filialId(request))
                .stream().map(vendaController::toPublicResponse).toList();
    }

    @PostMapping("/vendas/{id}/receber")
    public VendaResponseDTO receber(@PathVariable Long id, @Valid @RequestBody RecebimentoVendaRequestDTO body,
            HttpServletRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        authorizationService.require(request, ApexPermission.SALE_RECEIVE, "Caixa", "receber venda");
        return vendaController.toPublicResponse(vendaService.receberNoCaixa(id, body,
                tenantScopeService.empresaId(request), tenantScopeService.filialId(request), request, idempotencyKey));
    }

    @PostMapping("/vendas/{id}/recusar")
    public VendaResponseDTO recusar(@PathVariable Long id, @Valid @RequestBody MotivoOperacaoRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SALE_RECEIVE, "Caixa", "devolver venda para ajuste");
        return vendaController.toPublicResponse(vendaService.recusarNoCaixa(id, body.motivo(),
                tenantScopeService.empresaId(request), tenantScopeService.filialId(request), request));
    }

    @GetMapping("/clientes/{clienteId}/creditos")
    public java.util.List<CreditoClienteResponseDTO> creditos(@PathVariable Long clienteId, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SALE_RECEIVE, "Caixa", "consultar credito do cliente");
        return creditoClienteService.listarDisponiveis(clienteId, tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request)).stream().map(c -> new CreditoClienteResponseDTO(
                        c.getIdCredito(), c.getClienteId(), c.getVendaOrigemId(), c.getDevolucaoId(),
                        c.getValorOriginal(), c.getSaldo(), c.getStatus().name(), c.getEmpresaId(),
                        c.getFilialId(), c.getCriadoEm())).toList();
    }
}
