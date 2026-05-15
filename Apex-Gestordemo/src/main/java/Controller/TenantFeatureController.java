package Controller;

import DTO.TenantFeatureContextDTO;
import Service.TenantFeatureService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@CrossOrigin(originPatterns = {"http://localhost:*", "https://localhost:*", "capacitor://localhost", "ionic://localhost", "app://localhost"})
public class TenantFeatureController {

    private final TenantFeatureService tenantFeatureService;

    public TenantFeatureController(TenantFeatureService tenantFeatureService) {
        this.tenantFeatureService = tenantFeatureService;
    }

    @GetMapping("/current/features")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TenantFeatureContextDTO> currentFeatures(
            @RequestHeader(value = "X-Apex-Tenant-Code", required = false) String tenantCode
    ) {
        return ResponseEntity.ok(tenantFeatureService.resolve(tenantCode));
    }

    @GetMapping("/feature-catalog")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','AUDITOR')")
    public List<String> featureCatalog() {
        return tenantFeatureService.catalog();
    }
}
