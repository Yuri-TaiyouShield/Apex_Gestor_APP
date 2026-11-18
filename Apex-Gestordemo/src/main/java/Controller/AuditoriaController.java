package Controller;

import DTO.AuditLogResponseDTO;
import Model.AuditLog;
import Repository.AuditLogRepository;
import Service.AuthorizationService;
import Service.DataProtectionService;
import Service.PrivacyGuardService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private PrivacyGuardService privacyGuardService;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private TenantScopeService tenantScopeService;
    @Autowired(required = false)
    private DataProtectionService dataProtectionService;

    @GetMapping
    public List<AuditLogResponseDTO> listarTodos(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.AUDIT_LOG_VIEW, "Auditoria", "listar logs");
        privacyGuardService.requireAuditView(request, "acessar logs de auditoria");
        boolean unmasked = privacyGuardService.canViewUnmaskedAudit(request);
        return auditLogRepository.findByEmpresaIdAndFilialIdOrderByCriadoEmDesc(
                        tenantScopeService.empresaId(request),
                        tenantScopeService.filialId(request)).stream()
                .map(log -> toResponse(log, unmasked))
                .toList();
    }

    private AuditLogResponseDTO toResponse(AuditLog log, boolean unmasked) {
        String login = unmasked ? log.getUsuarioLogin() : privacyGuardService.maskIdentifier(log.getUsuarioLogin());
        String ip = unmasked ? log.getIp() : privacyGuardService.maskIp(log.getIp());
        String dispositivo = unmasked ? log.getDispositivo() : "Protegido (PII)";
        String justificativa = unmasked
                ? log.getJustificativa()
                : (dataProtectionService != null
                        ? dataProtectionService.maskSensitiveText(log.getJustificativa())
                        : "***");
        return new AuditLogResponseDTO(
                log.getIdAuditoria(),
                login,
                log.getEmpresaId(),
                log.getFilialId(),
                log.getModulo(),
                log.getAcao(),
                log.getEntidade(),
                log.getEntidadeId(),
                ip,
                dispositivo,
                log.getTraceId(),
                justificativa,
                log.getCriadoEm());
    }
}
