package Service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class TenantScopeService {

    @Autowired
    private PrivacyGuardService privacyGuardService;

    public Long empresaId(HttpServletRequest request) {
        Long value = privacyGuardService.currentEmpresaId(request);
        if (value == null) {
            throw new AccessDeniedException("Empresa ativa ausente ou token invalido.");
        }
        return value;
    }

    public Long filialId(HttpServletRequest request) {
        Long value = privacyGuardService.currentFilialId(request);
        if (value == null) {
            throw new AccessDeniedException("Filial ativa ausente ou token invalido.");
        }
        return value;
    }
}
