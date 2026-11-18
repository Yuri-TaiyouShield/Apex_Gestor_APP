package Controller;

import DTO.AuthResponseDTO;
import DTO.LoginRequestDTO;
import DTO.RefreshTokenRequestDTO;
import Service.AuditLogService;
import Service.AuthService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuditLogService auditLogService;

    public AuthController(AuthService authService, AuditLogService auditLogService) {
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest servletRequest) {
        if (!Boolean.TRUE.equals(request.acceptedPrivacyTerms())) {
            return problem(HttpStatus.BAD_REQUEST, "Consentimento obrigatorio",
                    "Aceite a politica de privacidade para acessar o ambiente.");
        }
        return ResponseEntity.ok(authService.login(request, servletRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO request,
            HttpServletRequest servletRequest) {
        return ResponseEntity.ok(authService.refresh(request, servletRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) LogoutRequest request,
            HttpServletRequest servletRequest, Authentication authentication) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            authService.logout(new RefreshTokenRequestDTO(request.refreshToken()));
        }
        auditLogService.recordLogout(servletRequest, authentication, request == null ? null : request.reason());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }

    public record LogoutRequest(String reason, String refreshToken) {
    }
}
