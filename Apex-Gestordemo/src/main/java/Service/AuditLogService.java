package Service;

import Model.AuditLog;
import Repository.AuditLogRepository;
import com.Apex.Apex_Gestordemo.web.RequestTraceFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final DataProtectionService dataProtectionService;
    private final JwtTokenService jwtTokenService;

    @Autowired
    public AuditLogService(
            AuditLogRepository auditLogRepository,
            DataProtectionService dataProtectionService,
            ObjectProvider<JwtTokenService> jwtTokenServiceProvider
    ) {
        this.auditLogRepository = auditLogRepository;
        this.dataProtectionService = dataProtectionService;
        this.jwtTokenService = jwtTokenServiceProvider == null ? null : jwtTokenServiceProvider.getIfAvailable();
    }

    public AuditLogService(AuditLogRepository auditLogRepository, DataProtectionService dataProtectionService) {
        this(auditLogRepository, dataProtectionService, null);
    }

    public void record(HttpServletRequest request, int status, Authentication authentication) {
        AuditLog log = new AuditLog();
        applyActorAndContext(log, request, authentication);
        log.setModulo("HTTP");
        log.setAcao(request == null ? "UNKNOWN" : request.getMethod());
        log.setEntidade("HTTP_REQUEST");
        log.setValorNovo(request == null ? null : dataProtectionService.maskSensitiveText(request.getRequestURI()));
        log.setJustificativa("status=" + status + "; ipHash=" + dataProtectionService.hash(clientIp(request))
                + "; userAgentHash=" + dataProtectionService.hash(userAgent(request)));
        log.setCriadoEm(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    public void recordLogout(HttpServletRequest request, Authentication authentication, String reason) {
        AuditLog log = new AuditLog();
        applyActorAndContext(log, request, authentication);
        log.setModulo("AUTENTICACAO");
        log.setAcao("LOGOUT");
        log.setEntidade("SESSAO");
        log.setValorNovo("reason=" + normalizeLogoutReason(reason));
        log.setJustificativa("ipHash=" + dataProtectionService.hash(clientIp(request))
                + "; userAgentHash=" + dataProtectionService.hash(userAgent(request)));
        log.setCriadoEm(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    private void applyActorAndContext(AuditLog log, HttpServletRequest request, Authentication authentication) {
        JwtTokenService.JwtClaims claims = resolveClaims(request, authentication);
        String login = null;
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getName() != null && !authentication.getName().isBlank()) {
            login = authentication.getName();
        } else if (claims != null && claims.login() != null && !claims.login().isBlank()) {
            login = claims.login();
        }
        log.setUsuarioLogin(login == null ? "anonymous" : limit(login, 80));
        if (claims != null) {
            log.setUsuarioId(claims.userId());
            log.setEmpresaId(claims.empresaId());
            log.setFilialId(claims.filialId());
        }
        log.setTraceId(limit(resolveTraceId(request), 80));
    }

    private JwtTokenService.JwtClaims resolveClaims(HttpServletRequest request, Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof JwtTokenService.JwtClaims claims) {
            return claims;
        }
        if (request == null || jwtTokenService == null) {
            return null;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = authorization.substring(7).trim();
        if (token.isBlank()) {
            return null;
        }
        try {
            return jwtTokenService.parseAndValidate(token);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        if (request != null) {
            Object attr = request.getAttribute(RequestTraceFilter.TRACE_ID_ATTRIBUTE);
            if (attr != null && !String.valueOf(attr).isBlank()) {
                return String.valueOf(attr);
            }
            String header = request.getHeader("X-Trace-Id");
            if (header != null && !header.isBlank() && header.length() <= 80 && header.matches("[A-Za-z0-9._:-]+")) {
                return header;
            }
        }
        String mdcTrace = MDC.get("traceId");
        return mdcTrace == null || mdcTrace.isBlank() ? null : mdcTrace;
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
        if (request == null) {
            return null;
        }
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        return request == null ? null : request.getHeader("User-Agent");
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
