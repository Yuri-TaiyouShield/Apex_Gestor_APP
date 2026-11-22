package Controller;

import DTO.DespesaRequestDTO;
import DTO.DespesaResponseDTO;
import DTO.PagarDespesaRequestDTO;
import DTO.TipoDespesaRequestDTO;
import DTO.TipoDespesaResponseDTO;
import Service.AuthorizationService;
import Service.DespesaService;
import Service.TenantFeatureGuardService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexFeature;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/despesas")
public class DespesaController {

    private final DespesaService despesaService;
    private final AuthorizationService authorizationService;
    private final TenantScopeService tenantScopeService;

    @Autowired(required = false)
    private TenantFeatureGuardService tenantFeatureGuardService;

    public DespesaController(
            DespesaService despesaService,
            AuthorizationService authorizationService,
            TenantScopeService tenantScopeService) {
        this.despesaService = despesaService;
        this.authorizationService = authorizationService;
        this.tenantScopeService = tenantScopeService;
    }

    @GetMapping
    public List<DespesaResponseDTO> listar(HttpServletRequest request) {
        requireCoreLicense(request, "listar despesas");
        authorizationService.require(request, ApexPermission.FINANCE_VIEW, "Financeiro", "listar despesas");
        return despesaService.listarTodos(empresaId(request), filialId(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DespesaResponseDTO> buscarPorId(@PathVariable Long id, HttpServletRequest request) {
        requireCoreLicense(request, "consultar despesa");
        authorizationService.require(request, ApexPermission.FINANCE_VIEW, "Financeiro", "consultar despesa");
        return despesaService.buscarPorId(id, empresaId(request), filialId(request))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DespesaResponseDTO> criar(
            @Valid @RequestBody DespesaRequestDTO body,
            HttpServletRequest request) {
        requireCoreLicense(request, "criar despesa");
        authorizationService.require(request, ApexPermission.FINANCE_MANAGE, "Financeiro", "criar despesa");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(despesaService.criar(body, empresaId(request), filialId(request), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DespesaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody DespesaRequestDTO body,
            HttpServletRequest request) {
        requireCoreLicense(request, "atualizar despesa");
        authorizationService.require(request, ApexPermission.FINANCE_MANAGE, "Financeiro", "atualizar despesa");
        return ResponseEntity.ok(despesaService.atualizar(id, body, empresaId(request), filialId(request), request));
    }

    @PatchMapping("/{id}/pagar")
    public ResponseEntity<DespesaResponseDTO> pagar(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataPagamento,
            @RequestBody(required = false) @Valid PagarDespesaRequestDTO body,
            HttpServletRequest request) {
        requireCoreLicense(request, "pagar despesa");
        authorizationService.require(request, ApexPermission.PAYABLE_SETTLE, "Financeiro", "pagar despesa");
        LocalDate dataEfetiva = dataPagamento != null
                ? dataPagamento
                : (body != null ? body.dataPagamento() : null);
        return ResponseEntity.ok(despesaService.pagar(
                id, dataEfetiva, empresaId(request), filialId(request), request));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<DespesaResponseDTO> cancelar(@PathVariable Long id, HttpServletRequest request) {
        requireCoreLicense(request, "cancelar despesa");
        authorizationService.require(request, ApexPermission.FINANCE_CHANGE, "Financeiro", "cancelar despesa");
        return ResponseEntity.ok(despesaService.cancelar(id, empresaId(request), filialId(request), request));
    }

    @GetMapping("/tipos")
    public List<TipoDespesaResponseDTO> listarTipos(
            @RequestParam(required = false) Boolean somenteAtivos,
            HttpServletRequest request) {
        requireCoreLicense(request, "listar tipos de despesa");
        authorizationService.require(request, ApexPermission.FINANCE_VIEW, "Financeiro", "listar tipos de despesa");
        return despesaService.listarTipos(somenteAtivos, empresaId(request), filialId(request));
    }

    @PostMapping("/tipos")
    public ResponseEntity<TipoDespesaResponseDTO> criarTipo(
            @Valid @RequestBody TipoDespesaRequestDTO body,
            HttpServletRequest request) {
        requireCoreLicense(request, "criar tipo de despesa");
        authorizationService.require(request, ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                "Financeiro", "criar tipo de despesa");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(despesaService.criarTipo(body, empresaId(request), filialId(request), request));
    }

    @PutMapping("/tipos/{id}")
    public ResponseEntity<TipoDespesaResponseDTO> atualizarTipo(
            @PathVariable Long id,
            @Valid @RequestBody TipoDespesaRequestDTO body,
            HttpServletRequest request) {
        requireCoreLicense(request, "atualizar tipo de despesa");
        authorizationService.require(request, ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                "Financeiro", "atualizar tipo de despesa");
        return ResponseEntity.ok(despesaService.atualizarTipo(
                id, body, empresaId(request), filialId(request), request));
    }

    private void requireCoreLicense(HttpServletRequest request, String acao) {
        if (tenantFeatureGuardService != null) {
            tenantFeatureGuardService.requireFeature(request, ApexFeature.FINANCIAL_CORE, "Financeiro", acao);
        }
    }

    private Long empresaId(HttpServletRequest request) {
        return tenantScopeService.empresaId(request);
    }

    private Long filialId(HttpServletRequest request) {
        return tenantScopeService.filialId(request);
    }
}
