package Controller;

import DTO.DashboardVendedorDTO;
import Service.*;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/vendedor")
public class DashboardVendedorController {
    @Autowired private DashboardVendedorService dashboardVendedorService;
    @Autowired private AuthorizationService authorizationService;
    @Autowired private PrivacyGuardService privacyGuardService;
    @Autowired private TenantScopeService tenantScopeService;

    @GetMapping
    public DashboardVendedorDTO dashboard(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SALE_CREATE, "Dashboard", "consultar dashboard vendedor");
        Long usuarioId = privacyGuardService.currentUserId(request);
        if (usuarioId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Usuario autenticado nao identificado.");
        }
        return dashboardVendedorService.calcular(usuarioId, tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request), authorizationService.can(request, ApexPermission.SALE_CANCEL));
    }
}
