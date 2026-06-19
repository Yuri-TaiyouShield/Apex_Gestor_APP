package Service;

import Model.AuditLog;
import Repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final DataProtectionService dataProtectionService;

    public AuditLogService(AuditLogRepository auditLogRepository, DataProtectionService dataProtectionService) {
        this.auditLogRepository = auditLogRepository;
        this.dataProtectionService = dataProtectionService;
    }

    public void record(HttpServletRequest request, int status, Authentication authentication) {
        AuditLog log = new AuditLog();
        log.setUsuarioLogin(authentication != null && authentication.isAuthenticated() ? authentication.getName() : "anonymous");
        log.setModulo("HTTP");
        log.setAcao(request.getMethod());
        log.setEntidade("HTTP_REQUEST");
        log.setValorNovo(dataProtectionService.maskSensitiveText(request.getRequestURI()));
        log.setJustificativa("status=" + status + "; ipHash=" + dataProtectionService.hash(clientIp(request))
                + "; userAgentHash=" + dataProtectionService.hash(request.getHeader("User-Agent")));
        log.setCriadoEm(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    public void recordLogout(HttpServletRequest request, Authentication authentication, String reason) {
        AuditLog log = new AuditLog();
        log.setUsuarioLogin(authentication != null && authentication.isAuthenticated()
                ? authentication.getName() : "anonymous");
        log.setModulo("AUTENTICACAO");
        log.setAcao("LOGOUT");
        log.setEntidade("SESSAO");
        log.setValorNovo("reason=" + normalizeLogoutReason(reason));
        log.setJustificativa("ipHash=" + dataProtectionService.hash(clientIp(request))
                + "; userAgentHash=" + dataProtectionService.hash(request.getHeader("User-Agent")));
        log.setCriadoEm(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    private String normalizeLogoutReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "MANUAL";
        }
        return switch (reason.trim().toLowerCase()) {
            case "idle", "inactivity" -> "INATIVIDADE";
            case "expired" -> "TOKEN_EXPIRADO";
            default -> "MANUAL";
        };
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
