package Controller;

import DTO.CentroCustoRequestDTO;
import DTO.CentroCustoResponseDTO;
import Model.TipoCentroCusto;
import Service.AuthorizationService;
import Service.CentroCustoService;
import Service.TenantFeatureGuardService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexFeature;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/financeiro/centros-custo")
public class CentroCustoController {

    private final CentroCustoService centroCustoService;
    private final AuthorizationService authorizationService;
    private final TenantScopeService tenantScopeService;

    @Autowired(required = false)
    private TenantFeatureGuardService tenantFeatureGuardService;

    public CentroCustoController(
            CentroCustoService centroCustoService,
            AuthorizationService authorizationService,
            TenantScopeService tenantScopeService) {
        this.centroCustoService = centroCustoService;
        this.authorizationService = authorizationService;
        this.tenantScopeService = tenantScopeService;
    }

    @GetMapping
    public List<CentroCustoResponseDTO> listar(
            @RequestParam(required = false) Boolean somenteAtivos,
            @RequestParam(required = false) TipoCentroCusto tipoOperacional,
            HttpServletRequest request) {
        requireCoreLicense(request, "listar centros de custo");
        authorizationService.require(request, ApexPermission.FINANCE_VIEW,
                "Financeiro", "listar centros de custo");
        return centroCustoService.listar(somenteAtivos, tipoOperacional,
                tenantScopeService.empresaId(request), tenantScopeService.filialId(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CentroCustoResponseDTO> buscarPorId(@PathVariable Long id, HttpServletRequest request) {
        requireCoreLicense(request, "consultar centro de custo");
        authorizationService.require(request, ApexPermission.FINANCE_VIEW,
                "Financeiro", "consultar centro de custo");
        return ResponseEntity.ok(centroCustoService.buscarPorId(
                id, tenantScopeService.empresaId(request), tenantScopeService.filialId(request)));
    }

    @PostMapping("/inicializar-padrao")
    public ResponseEntity<List<CentroCustoResponseDTO>> inicializarPadrao(HttpServletRequest request) {
        requireCoreLicense(request, "gerenciar centro de custo");
        authorizationService.require(request, ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                "Financeiro", "inicializar centros de custo padrao");
        return ResponseEntity.ok(centroCustoService.inicializarPadrao(
                tenantScopeService.empresaId(request), tenantScopeService.filialId(request), request));
    }

    @PostMapping
    public ResponseEntity<CentroCustoResponseDTO> criar(
            @Valid @RequestBody CentroCustoRequestDTO body,
            HttpServletRequest request) {
        requireCoreLicense(request, "gerenciar centro de custo");
        authorizationService.require(request, ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                "Financeiro", "criar centro de custo");
        return ResponseEntity.status(HttpStatus.CREATED).body(centroCustoService.criar(
                body, tenantScopeService.empresaId(request), tenantScopeService.filialId(request), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CentroCustoResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CentroCustoRequestDTO body,
            HttpServletRequest request) {
        requireCoreLicense(request, "gerenciar centro de custo");
        authorizationService.require(request, ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                "Financeiro", "atualizar centro de custo");
        return ResponseEntity.ok(centroCustoService.atualizar(
                id, body, tenantScopeService.empresaId(request), tenantScopeService.filialId(request), request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CentroCustoResponseDTO> alterarStatus(
            @PathVariable Long id,
            @RequestParam boolean ativo,
            HttpServletRequest request) {
        requireCoreLicense(request, "gerenciar centro de custo");
        authorizationService.require(request, ApexPermission.FINANCE_CLASSIFICATION_MANAGE,
                "Financeiro", "alterar status do centro de custo");
        return ResponseEntity.ok(centroCustoService.alterarStatus(
                id, ativo, tenantScopeService.empresaId(request), tenantScopeService.filialId(request), request));
    }

    private void requireCoreLicense(HttpServletRequest request, String acao) {
        if (tenantFeatureGuardService != null) {
            tenantFeatureGuardService.requireFeature(request, ApexFeature.FINANCIAL_CORE, "Financeiro", acao);
        }
    }
}
