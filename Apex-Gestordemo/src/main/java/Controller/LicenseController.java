package Controller;

import DTO.LicenseValidationRequestDTO;
import DTO.LicenseValidationResponseDTO;
import DTO.TenantEntitlementResponseDTO;
import DTO.TenantPlanSyncRequestDTO;
import Service.AuthorizationService;
import Service.LicenseService;
import Service.PrivacyGuardService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/licenses")
public class LicenseController {

    private final LicenseService licenseService;

    @Autowired(required = false)
    private TenantScopeService tenantScopeService;

    @Autowired(required = false)
    private AuthorizationService authorizationService;

    @Autowired(required = false)
    private PrivacyGuardService privacyGuardService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @PostMapping("/validate")
    public LicenseValidationResponseDTO validate(@Valid @RequestBody LicenseValidationRequestDTO request) {
        return licenseService.validate(request);
    }

    @GetMapping("/entitlement")
    public TenantEntitlementResponseDTO entitlement(HttpServletRequest request) {
        Long empresaId = tenantScopeService.empresaId(request);
        return licenseService.getEntitlement(empresaId);
    }

    @PostMapping("/entitlement/sync-plan")
    public TenantEntitlementResponseDTO syncPlan(
            @Valid @RequestBody TenantPlanSyncRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PERMISSION_MANAGE,
                "Licenciamento", "sincronizar plano comercial do tenant");
        Long scopedEmpresaId = tenantScopeService.empresaId(request);
        Long targetEmpresaId = body.empresaId() != null ? body.empresaId() : scopedEmpresaId;
        String actor = privacyGuardService != null ? privacyGuardService.currentUserLogin(request) : "SYSADMIN";
        return licenseService.syncTenantPlan(
                targetEmpresaId,
                body.tenantCode(),
                body.tenantName(),
                body.subscriptionTier(),
                body.features(),
                body.validoAte(),
                body.gracePeriodHoras(),
                body.status(),
                actor);
    }
}
