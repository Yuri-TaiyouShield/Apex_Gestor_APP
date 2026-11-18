package Controller;

import DTO.AuditLogResponseDTO;
import Model.AuditLog;
import Repository.AuditLogRepository;
import Service.AuthorizationService;
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

    @GetMapping
    public List<AuditLogResponseDTO> listarTodos(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.AUDIT_LOG_VIEW, "Auditoria", "listar logs");
        privacyGuardService.requirePrivacyAdmin(request, "acessar logs de auditoria");
        return auditLogRepository.findByEmpresaIdAndFilialIdOrderByCriadoEmDesc(
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request)).stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponseDTO toResponse(AuditLog log) {
        return new AuditLogResponseDTO(
                log.getIdAuditoria(),
                log.getUsuarioLogin(),
                log.getEmpresaId(),
                log.getFilialId(),
                log.getModulo(),
                log.getAcao(),
                log.getEntidade(),
                log.getEntidadeId(),
                log.getIp(),
                log.getDispositivo(),
                log.getTraceId(),
                log.getJustificativa(),
                log.getCriadoEm());
    }
}
