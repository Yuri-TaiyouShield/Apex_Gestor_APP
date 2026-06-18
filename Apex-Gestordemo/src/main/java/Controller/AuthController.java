package Controller;

import Service.JwtTokenService;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String TEST_PASSWORD = "Apex@2026";
    private static final Map<String, DemoUser> DEMO_USERS = Map.of(
            "v5_dono", new DemoUser(1L, "Dono/Gerente Apex", "v5_dono", List.of("ROLE_DONO_GERENTE")),
            "v5_sysadmin", new DemoUser(2L, "Sysadmin Apex", "v5_sysadmin", List.of("ROLE_SYSADMIN")),
            "v5_caixa", new DemoUser(3L, "Caixa Apex", "v5_caixa", List.of("ROLE_CAIXA")),
            "v5_gerente", new DemoUser(5L, "Gerente Apex", "v5_gerente", List.of("ROLE_GERENTE")),
            "v5_vendedor", new DemoUser(6L, "Vendedor Apex", "v5_vendedor", List.of("ROLE_VENDEDOR")),
            "v5_cliente", new DemoUser(4L, "Cliente Apex", "v5_cliente", List.of("ROLE_CLIENTE_B2C"))
    );

    private final JwtTokenService jwtTokenService;

    public AuthController(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        DemoUser user = DEMO_USERS.get(normalizeLogin(request.login()));
        if (user == null || !TEST_PASSWORD.equals(request.senha())) {
            return problem(HttpStatus.UNAUTHORIZED, "Credenciais invalidas",
                    "Use um login de teste valido e a senha Apex@2026.");
        }
        if (!Boolean.TRUE.equals(request.acceptedPrivacyTerms())) {
            return problem(HttpStatus.BAD_REQUEST, "Consentimento obrigatorio",
                    "Aceite a politica de privacidade para acessar o ambiente.");
        }

        return ResponseEntity.ok(AuthResponse.from(user, jwtTokenService));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        DemoUser user = DEMO_USERS.values().stream()
                .filter(candidate -> request.refreshToken() != null
                        && request.refreshToken().contains(candidate.login()))
                .findFirst()
                .orElse(DEMO_USERS.get("v5_dono"));
        return ResponseEntity.ok(AuthResponse.from(user, jwtTokenService));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }

    private String normalizeLogin(String login) {
        return login == null ? "" : login.trim().toLowerCase();
    }

    public record LoginRequest(
            @NotBlank String login,
            @NotBlank String senha,
            String totpCode,
            String consentVersion,
            Boolean acceptedPrivacyTerms) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    private record DemoUser(Long id, String nome, String login, List<String> roles) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            OffsetDateTime expiresAt,
            String tokenType,
            Long usuarioId,
            String nome,
            String login,
            List<String> roles) {
        static AuthResponse from(DemoUser user, JwtTokenService jwtTokenService) {
            return new AuthResponse(
                    jwtTokenService.issueAccessToken(
                            user.id(),
                            user.nome(),
                            user.login(),
                            user.roles(),
                            1L,
                            1L,
                            Duration.ofHours(8)),
                    "demo-refresh-" + user.login() + "-" + UUID.randomUUID(),
                    OffsetDateTime.now().plusHours(8),
                    "Bearer",
                    user.id(),
                    user.nome(),
                    user.login(),
                    user.roles());
        }
    }
}
