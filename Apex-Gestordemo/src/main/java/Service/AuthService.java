package Service;

import DTO.AuthResponseDTO;
import DTO.LoginRequestDTO;
import DTO.RefreshTokenRequestDTO;
import Model.RefreshToken;
import Model.Usuario;
import Repository.RefreshTokenRepository;
import Repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final DataProtectionService dataProtectionService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${apex.security.jwt.refresh-token-days}")
    private long refreshTokenDays;

    public AuthService(
            UsuarioRepository usuarioRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            DataProtectionService dataProtectionService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.dataProtectionService = dataProtectionService;
    }

    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request, HttpServletRequest servletRequest) {
        Usuario usuario = usuarioRepository.findByLogin(request.login())
                .orElseThrow(() -> new BadCredentialsException("Credenciais invalidas"));

        if (usuario.getStatus() == 0 || !passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new BadCredentialsException("Credenciais invalidas");
        }

        String refreshToken = issueRefreshToken(usuario, servletRequest);
        return jwtTokenService.response(usuario, refreshToken);
    }

    @Transactional
    public AuthResponseDTO refresh(RefreshTokenRequestDTO request, HttpServletRequest servletRequest) {
        String hash = dataProtectionService.hash(request.refreshToken());
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalido"));

        if (current.getRevokedAt() != null || current.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token expirado ou revogado");
        }

        current.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(current);
        String newRefreshToken = issueRefreshToken(current.getUsuario(), servletRequest);
        return jwtTokenService.response(current.getUsuario(), newRefreshToken);
    }

    @Transactional
    public void logout(RefreshTokenRequestDTO request) {
        String hash = dataProtectionService.hash(request.refreshToken());
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(token);
        });
    }

    private String issueRefreshToken(Usuario usuario, HttpServletRequest request) {
        byte[] random = new byte[48];
        secureRandom.nextBytes(random);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);

        RefreshToken token = new RefreshToken();
        token.setUsuario(usuario);
        token.setTokenHash(dataProtectionService.hash(rawToken));
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenDays));
        token.setIpHash(dataProtectionService.hash(clientIp(request)));
        token.setUserAgentHash(dataProtectionService.hash(request.getHeader("User-Agent")));
        refreshTokenRepository.save(token);
        return rawToken;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
