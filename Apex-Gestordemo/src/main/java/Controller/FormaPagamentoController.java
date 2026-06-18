package Controller;

import DTO.FormaPagamentoRequestDTO;
import DTO.FormaPagamentoResponseDTO;
import Model.FormaPagamento;
import Service.AuditTrailService;
import Service.AuthorizationService;
import Service.FormaPagamentoService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/formaspagamento")
@CrossOrigin(origins = "http://localhost:4200")
public class FormaPagamentoController {

    @Autowired
    private FormaPagamentoService formaPagamentoService;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private TenantScopeService tenantScopeService;
    @Autowired
    private AuditTrailService auditTrailService;

    @GetMapping
    public List<FormaPagamentoResponseDTO> listarTodos(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PAYMENT_CONDITION_VIEW, "Financeiro",
                "listar formas de pagamento");
        return formaPagamentoService.listarTodos(empresaId(request), filialId(request)).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormaPagamentoResponseDTO> buscarPorId(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PAYMENT_CONDITION_VIEW, "Financeiro",
                "consultar forma de pagamento");
        return formaPagamentoService.buscarPorId(id, empresaId(request), filialId(request))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FormaPagamentoResponseDTO> criar(@Valid @RequestBody FormaPagamentoRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PAYMENT_CONDITION_MANAGE, "Financeiro",
                "criar forma de pagamento");
        FormaPagamento salva = formaPagamentoService.salvar(toEntity(null, body), empresaId(request), filialId(request));
        auditTrailService.registrarPrivacidade("FORMA_PAGAMENTO_CRIADA", "FormaPagamento", salva.getIdFormaPagamento(),
                "Financeiro", null, resumo(salva), request, "Forma de pagamento criada.");
        return new ResponseEntity<>(toResponse(salva), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormaPagamentoResponseDTO> atualizar(@PathVariable Long id,
            @Valid @RequestBody FormaPagamentoRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PAYMENT_CONDITION_MANAGE, "Financeiro",
                "atualizar forma de pagamento");
        return formaPagamentoService.buscarPorId(id, empresaId(request), filialId(request))
                .map(existente -> {
                    String anterior = resumo(existente);
                    FormaPagamento salva = formaPagamentoService.salvar(toEntity(id, body), empresaId(request), filialId(request));
                    auditTrailService.registrarPrivacidade("FORMA_PAGAMENTO_ATUALIZADA", "FormaPagamento",
                            salva.getIdFormaPagamento(), "Financeiro", anterior, resumo(salva), request,
                            "Forma de pagamento atualizada.");
                    return ResponseEntity.ok(toResponse(salva));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<FormaPagamentoResponseDTO> desativar(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PAYMENT_CONDITION_MANAGE, "Financeiro",
                "desativar forma de pagamento");
        return formaPagamentoService.buscarPorId(id, empresaId(request), filialId(request))
                .map(existente -> {
                    String anterior = resumo(existente);
                    FormaPagamento salva = formaPagamentoService.desativar(id, empresaId(request), filialId(request));
                    auditTrailService.registrarPrivacidade("FORMA_PAGAMENTO_DESATIVADA", "FormaPagamento",
                            salva.getIdFormaPagamento(), "Financeiro", anterior, resumo(salva), request,
                            "Forma de pagamento desativada.");
                    return ResponseEntity.ok(toResponse(salva));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private FormaPagamento toEntity(Long id, FormaPagamentoRequestDTO body) {
        FormaPagamento formaPagamento = new FormaPagamento();
        formaPagamento.setIdFormaPagamento(id);
        formaPagamento.setNome(body.nome());
        formaPagamento.setDescricao(body.descricao());
        formaPagamento.setTipoPagamento(body.tipoPagamento());
        formaPagamento.setStatus(body.status() == null ? 1 : body.status());
        return formaPagamento;
    }

    private FormaPagamentoResponseDTO toResponse(FormaPagamento formaPagamento) {
        return new FormaPagamentoResponseDTO(
                formaPagamento.getIdFormaPagamento(),
                formaPagamento.getNome(),
                formaPagamento.getDescricao(),
                formaPagamento.getTipoPagamento(),
                formaPagamento.getStatus(),
                formaPagamento.getEmpresaId(),
                formaPagamento.getFilialId());
    }

    private Long empresaId(HttpServletRequest request) {
        return tenantScopeService.empresaId(request);
    }

    private Long filialId(HttpServletRequest request) {
        return tenantScopeService.filialId(request);
    }

    private String resumo(FormaPagamento formaPagamento) {
        return "nome=" + formaPagamento.getNome()
                + ";tipoPagamento=" + formaPagamento.getTipoPagamento()
                + ";status=" + formaPagamento.getStatus();
    }
}
