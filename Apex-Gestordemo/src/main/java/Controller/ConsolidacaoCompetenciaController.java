package Controller;

import DTO.ConsolidacaoCompetenciaResponseDTO;
import Service.AuthorizationService;
import Service.ConsolidacaoCompetenciaService;
import Service.TenantFeatureGuardService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexFeature;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financeiro/consolidacao/competencia")
public class ConsolidacaoCompetenciaController {

    private final ConsolidacaoCompetenciaService consolidacaoService;
    private final AuthorizationService authorizationService;
    private final TenantScopeService tenantScopeService;

    @Autowired(required = false)
    private TenantFeatureGuardService tenantFeatureGuardService;

    public ConsolidacaoCompetenciaController(
            ConsolidacaoCompetenciaService consolidacaoService,
            AuthorizationService authorizationService,
            TenantScopeService tenantScopeService) {
        this.consolidacaoService = consolidacaoService;
        this.authorizationService = authorizationService;
        this.tenantScopeService = tenantScopeService;
    }

    @GetMapping
    public ResponseEntity<ConsolidacaoCompetenciaResponseDTO> consultar(
            @RequestParam(required = false) String competencia,
            HttpServletRequest request) {
        if (tenantFeatureGuardService != null) {
            tenantFeatureGuardService.requireFeature(request, ApexFeature.FINANCIAL_CORE,
                    "Financeiro", "consultar consolidacao de competencia");
        }
        authorizationService.require(request, ApexPermission.FINANCE_VIEW,
                "Financeiro", "consultar consolidacao de competencia");
        return ResponseEntity.ok(consolidacaoService.consultar(
                competencia, tenantScopeService.empresaId(request), tenantScopeService.filialId(request)));
    }

    @PostMapping("/recalcular")
    public ResponseEntity<ConsolidacaoCompetenciaResponseDTO> recalcular(
            @RequestParam(required = false) String competencia,
            HttpServletRequest request) {
        if (tenantFeatureGuardService != null) {
            tenantFeatureGuardService.requireFeature(request, ApexFeature.FINANCIAL_CORE,
                    "Financeiro", "recalcular consolidacao de competencia");
        }
        if (!authorizationService.can(request, ApexPermission.FINANCE_MANAGE)
                && !authorizationService.can(request, ApexPermission.FINANCE_CLASSIFICATION_MANAGE)) {
            authorizationService.require(request, ApexPermission.FINANCE_MANAGE,
                    "Financeiro", "recalcular consolidacao de competencia");
        }
        return ResponseEntity.ok(consolidacaoService.recalcular(
                competencia, tenantScopeService.empresaId(request), tenantScopeService.filialId(request), request));
    }
}
