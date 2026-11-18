package Service;

import DTO.EnderecoResumoDTO;
import Model.Endereco;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PrivacyGuardService {

    @Autowired(required = false)
    private JwtTokenService jwtTokenService;

    /**
     * Perfis com visualizacao de PII cadastral. ADMIN_MASTER e ADVOGADO foram
     * removidos daqui para impedir exposicao irrestrita fora do escopo autorizado.
     */
    private static final Set<String> SENSITIVE_DATA_ROLES = Set.of(
            "ROLE_SYSADMIN",
            "ROLE_DONO_GERENTE",
            "ROLE_DONO",
            "ROLE_ADMIN",
            "ROLE_ADMINISTRACAO",
            "ROLE_GERENTE",
            "ROLE_GESTOR",
            "ROLE_FINANCEIRO",
            "ROLE_FINANCAS",
            "ROLE_AUDITOR",
            "ROLE_CONTADOR");

    /**
     * Operacoes criticas de privacidade (exportacao completa de titular e anonimizacao)
     * restritas exclusivamente ao proprietario (DONO / DONO_GERENTE) ou SYSADMIN.
     */
    private static final Set<String> PRIVACY_OWNER_ROLES = Set.of(
            "ROLE_SYSADMIN",
            "ROLE_DONO_GERENTE",
            "ROLE_DONO");

    /**
     * Perfis autorizados a consultar trilha de auditoria (ADMIN/GERENTE/ADMIN_MASTER/AUDITOR/FINANCEIRO
     * visualizam com PII mascarada; apenas PRIVACY_OWNER_ROLES visualizam PII sem mascara).
     */
    private static final Set<String> AUDIT_VIEW_ROLES = Set.of(
            "ROLE_SYSADMIN",
            "ROLE_DONO_GERENTE",
            "ROLE_DONO",
            "ROLE_ADMIN_MASTER",
            "ROLE_ADMIN",
            "ROLE_ADMINISTRACAO",
            "ROLE_GERENTE",
            "ROLE_GESTOR",
            "ROLE_AUDITOR",
            "ROLE_FINANCEIRO",
            "ROLE_FINANCAS");

    public boolean canViewSensitiveData(HttpServletRequest request) {
        return hasAnyRole(request, SENSITIVE_DATA_ROLES);
    }

    public boolean isPrivacyOwner(HttpServletRequest request) {
        return hasAnyRole(request, PRIVACY_OWNER_ROLES);
    }

    public void requirePrivacyOwner(HttpServletRequest request, String action) {
        if (!isPrivacyOwner(request)) {
            throw new AccessDeniedException(
                    "Permissao insuficiente para " + action + ". Exclusivo para proprietario (DONO/DONO_GERENTE) ou SYSADMIN.");
        }
    }

    public boolean canAdminPrivacy(HttpServletRequest request) {
        return isPrivacyOwner(request);
    }

    public void requirePrivacyAdmin(HttpServletRequest request, String action) {
        requirePrivacyOwner(request, action);
    }

    public boolean canViewAudit(HttpServletRequest request) {
        return hasAnyRole(request, AUDIT_VIEW_ROLES);
    }

    public void requireAuditView(HttpServletRequest request, String action) {
        if (!canViewAudit(request)) {
            throw new AccessDeniedException("Permissao insuficiente para " + action + ".");
        }
    }

    public boolean canViewUnmaskedAudit(HttpServletRequest request) {
        return isPrivacyOwner(request);
    }

    public String currentUserLogin(HttpServletRequest request) {
        JwtTokenService.JwtClaims claims = jwtClaims(request);
        if (claims != null && claims.login() != null && !claims.login().isBlank()) {
            return claims.login();
        }
        Authentication authentication = authentication();
        return authentication == null ? "publico" : authentication.getName();
    }

    public Long currentUserId(HttpServletRequest request) {
        JwtTokenService.JwtClaims claims = jwtClaims(request);
        if (claims != null && claims.userId() != null) {
            return claims.userId();
        }
        Authentication authentication = authentication();
        if (authentication != null && authentication.getDetails() instanceof JwtTokenService.JwtClaims details) {
            return details.userId();
        }
        return claims == null ? null : claims.userId();
    }

    public Long currentEmpresaId(HttpServletRequest request) {
        JwtTokenService.JwtClaims claims = jwtClaims(request);
        return claims == null ? null : claims.empresaId();
    }

    public Long currentFilialId(HttpServletRequest request) {
        JwtTokenService.JwtClaims claims = jwtClaims(request);
        return claims == null ? null : claims.filialId();
    }

    public Set<String> currentRoles(HttpServletRequest request) {
        JwtTokenService.JwtClaims claims = jwtClaims(request);
        if (claims != null && claims.roles() != null && !claims.roles().isEmpty()) {
            return claims.roles().stream()
                    .map(this::normalizeRole)
                    .collect(Collectors.toSet());
        }
        Authentication authentication = authentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .map(authority -> normalizeRole(authority.getAuthority()))
                    .collect(Collectors.toSet());
        }
        return Set.of("ROLE_PUBLICO");
    }

    public String maskIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return "***";
        }
        String trimmed = value.trim();
        if (trimmed.contains("@")) {
            return maskEmail(trimmed);
        }
        if (trimmed.length() <= 3) {
            return trimmed.substring(0, 1) + "***";
        }
        return trimmed.substring(0, 2) + "***" + trimmed.substring(trimmed.length() - 1);
    }

    public String maskIp(String value) {
        if (value == null || value.isBlank()) {
            return "***";
        }
        String trimmed = value.trim();
        if (trimmed.contains(".")) {
            String[] octets = trimmed.split("\\.");
            if (octets.length == 4) {
                return octets[0] + "." + octets[1] + ".*.*";
            }
        }
        if (trimmed.contains(":")) {
            String[] segments = trimmed.split(":");
            if (segments.length >= 2) {
                return segments[0] + ":" + segments[1] + ":***";
            }
        }
        return "***";
    }

    public String maskDocument(String value) {
        String digits = onlyDigits(value);
        if (digits.isBlank()) {
            return "***";
        }
        String visible = digits.length() > 4 ? digits.substring(digits.length() - 4) : digits;
        return "***" + visible;
    }

    public String maskPhone(String value) {
        String digits = onlyDigits(value);
        if (digits.isBlank()) {
            return "***";
        }
        String visible = digits.length() > 4 ? digits.substring(digits.length() - 4) : digits;
        return "(**) ****-" + visible;
    }

    public String maskEmail(String value) {
        if (value == null || value.isBlank() || !value.contains("@")) {
            return "***";
        }
        String[] parts = value.split("@", 2);
        String user = parts[0].isBlank() ? "*" : parts[0].substring(0, 1);
        return user + "***@" + parts[1];
    }

    public String maskCep(String value) {
        String digits = onlyDigits(value);
        if (digits.length() < 3) {
            return "***";
        }
        return digits.substring(0, 2) + "***-***";
    }

    public EnderecoResumoDTO maskAddress(Endereco endereco, boolean fullAccess) {
        if (endereco == null) {
            return null;
        }
        if (fullAccess) {
            return EnderecoResumoDTO.from(endereco);
        }
        return new EnderecoResumoDTO(
                endereco.getIdEndereco(),
                null,
                endereco.getCidade(),
                maskCep(endereco.getCep()),
                null,
                null,
                endereco.getUf(),
                null,
                null);
    }

    public String normalizeDocument(String value) {
        return onlyDigits(value);
    }

    public String sha256(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel no runtime.", ex);
        }
    }

    private boolean hasAnyRole(HttpServletRequest request, Set<String> roles) {
        Set<String> current = currentRoles(request);
        return current.stream().anyMatch(roles::contains);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_PUBLICO";
        }
        String ascii = java.text.Normalizer.normalize(role.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "_")
                .toUpperCase(Locale.ROOT);
        if (ascii.isBlank()) {
            return "ROLE_PUBLICO";
        }
        return ascii.startsWith("ROLE_") ? ascii : "ROLE_" + ascii;
    }

    private String authorizationToken(HttpServletRequest request) {
        String authorization = header(request, "Authorization");
        if (authorization == null) {
            return null;
        }
        return authorization.replaceFirst("(?i)^Bearer\\s+", "").trim();
    }

    private JwtTokenService.JwtClaims jwtClaims(HttpServletRequest request) {
        if (jwtTokenService == null) {
            return null;
        }
        String token = authorizationToken(request);
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return jwtTokenService.parseAndValidate(token);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Authentication authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !authentication.isAuthenticated() ? null : authentication;
    }

    private String header(HttpServletRequest request, String name) {
        return request == null ? null : request.getHeader(name);
    }

    private String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
