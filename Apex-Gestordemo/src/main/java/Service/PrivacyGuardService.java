package Service;

import DTO.EnderecoResumoDTO;
import Model.Endereco;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class PrivacyGuardService {

    @Autowired(required = false)
    private JwtTokenService jwtTokenService;

    private static final Set<String> SENSITIVE_DATA_ROLES = Set.of(
            "ROLE_SYSADMIN",
            "ROLE_DONO_GERENTE",
            "ROLE_ADMIN",
            "ROLE_GERENTE",
            "ROLE_GESTOR",
            "ROLE_FINANCEIRO",
            "ROLE_AUDITOR",
            "ROLE_CONTADOR",
            "ROLE_ADVOGADO");

    private static final Set<String> PRIVACY_ADMIN_ROLES = Set.of(
            "ROLE_SYSADMIN",
            "ROLE_DONO_GERENTE",
            "ROLE_ADMIN",
            "ROLE_GERENTE");

    public boolean canViewSensitiveData(HttpServletRequest request) {
        return hasAnyRole(request, SENSITIVE_DATA_ROLES);
    }

    public boolean canAdminPrivacy(HttpServletRequest request) {
        return hasAnyRole(request, PRIVACY_ADMIN_ROLES);
    }

    public void requirePrivacyAdmin(HttpServletRequest request, String action) {
        if (!canAdminPrivacy(request)) {
            throw new AccessDeniedException("Permissao insuficiente para " + action + ".");
        }
    }

    public String currentUserLogin(HttpServletRequest request) {
        JwtTokenService.JwtClaims claims = jwtClaims(request);
        if (claims != null && claims.login() != null && !claims.login().isBlank()) {
            return claims.login();
        }
        if (hasJwtValidatorAndToken(request)) {
            return "publico";
        }
        String explicit = header(request, "X-Apex-User-Login");
        if (explicit != null && !explicit.isBlank()) {
            return explicit.trim();
        }
        String token = authorizationToken(request);
        if (token == null || token.isBlank()) {
            return "publico";
        }
        if (token.contains("v5_sysadmin")) {
            return "v5_sysadmin";
        }
        if (token.contains("v5_dono")) {
            return "v5_dono";
        }
        if (token.contains("v5_caixa")) {
            return "v5_caixa";
        }
        if (token.contains("v5_gerente")) {
            return "v5_gerente";
        }
        if (token.contains("v5_vendedor")) {
            return "v5_vendedor";
        }
        if (token.contains("v5_cliente")) {
            return "v5_cliente";
        }
        return "autenticado";
    }

    public Long currentUserId(HttpServletRequest request) {
        JwtTokenService.JwtClaims claims = jwtClaims(request);
        if (claims != null && claims.userId() != null) {
            return claims.userId();
        }
        if (hasJwtValidatorAndToken(request)) {
            return null;
        }
        String value = header(request, "X-Apex-User-Id");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public Long currentEmpresaId(HttpServletRequest request) {
        JwtTokenService.JwtClaims claims = jwtClaims(request);
        if (claims == null && hasJwtValidatorAndToken(request)) {
            return null;
        }
        return claims != null && claims.empresaId() != null ? claims.empresaId() : headerLong(request, "X-Apex-Empresa-Id");
    }

    public Long currentFilialId(HttpServletRequest request) {
        JwtTokenService.JwtClaims claims = jwtClaims(request);
        if (claims == null && hasJwtValidatorAndToken(request)) {
            return null;
        }
        return claims != null && claims.filialId() != null ? claims.filialId() : headerLong(request, "X-Apex-Filial-Id");
    }

    public Set<String> currentRoles(HttpServletRequest request) {
        JwtTokenService.JwtClaims claims = jwtClaims(request);
        if (claims != null && claims.roles() != null && !claims.roles().isEmpty()) {
            return claims.roles().stream()
                    .map(this::normalizeRole)
                    .collect(Collectors.toSet());
        }
        if (hasJwtValidatorAndToken(request)) {
            return Set.of("ROLE_PUBLICO");
        }
        String explicitRoles = header(request, "X-Apex-Roles");
        if (explicitRoles != null && !explicitRoles.isBlank()) {
            return Arrays.stream(explicitRoles.split(","))
                    .map(String::trim)
                    .filter(role -> !role.isBlank())
                    .map(this::normalizeRole)
                    .collect(Collectors.toSet());
        }
        String token = authorizationToken(request);
        if (token == null || token.isBlank()) {
            return Set.of("ROLE_PUBLICO");
        }
        if (token.contains("v5_sysadmin")) {
            return Set.of("ROLE_SYSADMIN");
        }
        if (token.contains("v5_dono")) {
            return Set.of("ROLE_DONO_GERENTE");
        }
        if (token.contains("v5_caixa")) {
            return Set.of("ROLE_CAIXA");
        }
        if (token.contains("v5_gerente")) {
            return Set.of("ROLE_GERENTE");
        }
        if (token.contains("v5_vendedor")) {
            return Set.of("ROLE_VENDEDOR");
        }
        if (token.contains("v5_cliente")) {
            return Set.of("ROLE_CLIENTE_B2C");
        }
        return Set.of("ROLE_AUTENTICADO");
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
        String value = role.trim().toUpperCase();
        return value.startsWith("ROLE_") ? value : "ROLE_" + value;
    }

    private String authorizationToken(HttpServletRequest request) {
        String authorization = header(request, "Authorization");
        if (authorization == null) {
            return null;
        }
        return authorization.replaceFirst("(?i)^Bearer\\s+", "").trim();
    }

    private boolean hasAuthorizationToken(HttpServletRequest request) {
        String token = authorizationToken(request);
        return token != null && !token.isBlank();
    }

    private boolean hasJwtValidatorAndToken(HttpServletRequest request) {
        return jwtTokenService != null && hasAuthorizationToken(request);
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

    private Long headerLong(HttpServletRequest request, String name) {
        String value = header(request, name);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String header(HttpServletRequest request, String name) {
        return request == null ? null : request.getHeader(name);
    }

    private String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
