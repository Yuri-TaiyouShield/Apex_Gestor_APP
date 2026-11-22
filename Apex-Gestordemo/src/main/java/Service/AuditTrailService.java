package Service;

import Model.AuditLog;
import Repository.AuditLogRepository;
import com.Apex.Apex_Gestordemo.web.RequestTraceFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditTrailService {

    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private PrivacyGuardService privacyGuardService;
    @Autowired(required = false)
    private DataProtectionService dataProtectionService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog registrarSistema(String acao, String entidade, Long entidadeId, String valorAnterior, String valorNovo) {
        AuditLog log = new AuditLog();
        log.setAcao(acao);
        log.setEntidade(entidade);
        log.setEntidadeId(entidadeId);
        log.setValorAnterior(maskText(valorAnterior, 0));
        log.setValorNovo(maskText(valorNovo, 0));
        log.setIp("system");
        log.setDispositivo("backend");
        return auditLogRepository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog registrarPrivacidade(
            String acao,
            String entidade,
            Long entidadeId,
            String modulo,
            String valorAnterior,
            String valorNovo,
            HttpServletRequest request,
            String justificativa) {
        AuditLog log = new AuditLog();
        log.setUsuarioId(privacyGuardService.currentUserId(request));
        log.setUsuarioLogin(privacyGuardService.currentUserLogin(request));
        log.setEmpresaId(privacyGuardService.currentEmpresaId(request));
        log.setFilialId(privacyGuardService.currentFilialId(request));
        log.setModulo(modulo);
        log.setAcao(acao);
        log.setEntidade(entidade);
        log.setEntidadeId(entidadeId);
        log.setValorAnterior(maskText(valorAnterior, 0));
        log.setValorNovo(maskText(valorNovo, 0));
        log.setIp(resolveIp(request));
        log.setDispositivo(resolveUserAgent(request));
        log.setTraceId(resolveTraceId(request));
        log.setJustificativa(maskText(justificativa, 255));
        return auditLogRepository.save(log);
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return "system";
        }
        return request.getRemoteAddr();
    }

    private String maskText(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        String masked = dataProtectionService != null ? dataProtectionService.maskSensitiveText(value) : value;
        if (maxLen > 0 && masked.length() > maxLen) {
            return masked.substring(0, maxLen);
        }
        return masked;
    }

    private String resolveUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "backend";
        }
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return request.getHeader("X-Client-Platform");
        }
        return userAgent.length() > 160 ? userAgent.substring(0, 160) : userAgent;
    }

    private String resolveTraceId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object traceId = request.getAttribute(RequestTraceFilter.TRACE_ID_ATTRIBUTE);
        return traceId == null ? null : String.valueOf(traceId);
    }
}
