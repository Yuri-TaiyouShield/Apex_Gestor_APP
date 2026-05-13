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
        log.setAtorLogin(authentication != null && authentication.isAuthenticated() ? authentication.getName() : "anonymous");
        log.setMetodo(request.getMethod());
        log.setRecurso(dataProtectionService.maskSensitiveText(request.getRequestURI()));
        log.setStatus(status);
        log.setIpHash(dataProtectionService.hash(clientIp(request)));
        log.setUserAgentHash(dataProtectionService.hash(request.getHeader("User-Agent")));
        log.setCriadoEm(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
