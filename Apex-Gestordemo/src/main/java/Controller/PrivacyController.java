package Controller;

import DTO.ClienteExportDTO;
import DTO.ConsentRequestDTO;
import DTO.PrivacyRequestDTO;
import Model.Cliente;
import Model.ConsentAudit;
import Model.PrivacyRequest;
import Service.PrivacyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/privacy")
@CrossOrigin(originPatterns = {"http://localhost:*", "https://localhost:*", "capacitor://localhost", "ionic://localhost", "app://localhost"})
public class PrivacyController {

    private final PrivacyService privacyService;

    public PrivacyController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    @PostMapping("/consents")
    public ConsentAudit registerConsent(@Valid @RequestBody ConsentRequestDTO request, HttpServletRequest servletRequest) {
        return privacyService.registerConsent(request, servletRequest);
    }

    @PostMapping("/requests")
    public PrivacyRequest createRequest(@Valid @RequestBody PrivacyRequestDTO request, Authentication authentication) {
        return privacyService.createPrivacyRequest(request, authentication);
    }

    @GetMapping("/export/clientes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ClienteExportDTO exportCliente(@PathVariable Long id) {
        return privacyService.exportCliente(id);
    }

    @DeleteMapping("/clientes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Cliente anonymizeCliente(@PathVariable Long id, Authentication authentication) {
        return privacyService.anonymizeCliente(id, authentication);
    }
}
