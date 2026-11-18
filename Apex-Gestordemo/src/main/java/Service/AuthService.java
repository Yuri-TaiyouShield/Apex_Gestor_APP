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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private static final Set<String> CLIENT_PROFILES = Set.of(
            "CLIENTE",
            "CLIENTE_B2C",
            "ROLE_CLIENTE",
            "ROLE_CLIENTE_B2C"
    );

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final DataProtectionService dataProtectionService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${apex.security.jwt.refresh-token-days:30}")
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
        if (!Boolean.TRUE.equals(request.acceptedPrivacyTerms())) {
            throw new IllegalArgumentException("Aceite a politica de privacidade para acessar o ambiente.");
        }
        String login = request.login().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new BadCredentialsException("Credenciais invalidas"));

        if (usuario.getStatus() == 0 || usuario.getEmpresaId() == null || usuario.getFilialId() == null
                || !passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new BadCredentialsException("Credenciais invalidas");
        }
        if (isClientProfile(usuario)) {
            throw new AccessDeniedException("Acesso de login para perfil cliente foi desabilitado.");
        }

        String refreshToken = issueRefreshToken(usuario, servletRequest);
        return jwtTokenService.response(usuario, refreshToken);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResponseDTO refresh(RefreshTokenRequestDTO request, HttpServletRequest servletRequest) {
        String hash = dataProtectionService.hash(request.refreshToken());
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalido"));

        if (current.getRevokedAt() != null) {
            revokeActiveTokens(current.getUsuario());
            throw new BadCredentialsException("Refresh token expirado ou revogado");
        }
        if (current.getExpiresAt().isBefore(LocalDateTime.now())
                || current.getUsuario() == null
                || current.getUsuario().getStatus() == 0
                || current.getUsuario().getEmpresaId() == null
                || current.getUsuario().getFilialId() == null) {
            throw new BadCredentialsException("Refresh token expirado ou usuario inativo");
        }
        if (isClientProfile(current.getUsuario())) {
            revokeActiveTokens(current.getUsuario());
            throw new BadCredentialsException("Refresh token expirado ou usuario inativo");
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

    private void revokeActiveTokens(Usuario usuario) {
        if (usuario == null) {
            return;
        }
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUsuarioAndRevokedAtIsNull(usuario);
        if (activeTokens.isEmpty()) {
            return;
        }
        LocalDateTime revokedAt = LocalDateTime.now();
        activeTokens.forEach(token -> token.setRevokedAt(revokedAt));
        refreshTokenRepository.saveAll(activeTokens);
    }

    private boolean isClientProfile(Usuario usuario) {
        if (usuario == null || usuario.getPerfil() == null || usuario.getPerfil().getNome() == null) {
            return false;
        }
        String normalized = Normalizer.normalize(usuario.getPerfil().getNome().trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toUpperCase(Locale.ROOT);
        return CLIENT_PROFILES.contains(normalized);
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
        if (request == null) {

            return null;
        }
        return request.getRemoteAddr();
    }
}
