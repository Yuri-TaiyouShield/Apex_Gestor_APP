package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import DTO.AuthResponseDTO;
import DTO.LoginRequestDTO;
import DTO.RefreshTokenRequestDTO;
import Model.Perfil;
import Model.RefreshToken;
import Model.Usuario;
import Repository.RefreshTokenRepository;
import Repository.UsuarioRepository;
import Service.AuthService;
import Service.DataProtectionService;
import Service.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceTests {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private HttpServletRequest request;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(usuarioRepository, refreshTokenRepository, passwordEncoder,
                new JwtTokenService("unit-test-jwt-secret-with-32-chars"), new DataProtectionService());
        ReflectionTestUtils.setField(authService, "refreshTokenDays", 30L);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void rejeitaUsuarioInexistenteMesmoComSenhaConhecidaDoAntigoDemo() {
        when(usuarioRepository.findByLogin("v5_vendedor")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(login("v5_vendedor", "Apex@2026"), request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejeitaSenhaIncorreta() {
        Usuario usuario = usuarioAtivo();
        when(usuarioRepository.findByLogin("operador")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(login("OPERADOR", "senha-errada"), request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejeitaUsuarioInativo() {
        Usuario usuario = usuarioAtivo();
        usuario.setStatus(0);
        when(usuarioRepository.findByLogin("operador")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(login("operador", "senha-correta"), request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejeitaUsuarioSemTenantCompleto() {
        Usuario usuario = usuarioAtivo();
        usuario.setFilialId(null);
        when(usuarioRepository.findByLogin("operador")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(login("operador", "senha-correta"), request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginValidoEmiteAccessERefreshTokenPersistido() {
        Usuario usuario = usuarioAtivo();
        when(usuarioRepository.findByLogin("operador")).thenReturn(Optional.of(usuario));

        AuthResponseDTO response = authService.login(login(" OPERADOR ", "senha-correta"), request);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.usuarioId()).isEqualTo(7L);
        assertThat(response.roles()).containsExactly("ROLE_VENDEDOR");
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void loginValidoDeCaixaEmiteRoleCaixa() {
        Usuario caixa = usuarioComPerfil("CAIXA", "caixa_teste");
        when(usuarioRepository.findByLogin("caixa_teste")).thenReturn(Optional.of(caixa));

        AuthResponseDTO response = authService.login(login("caixa_teste", "senha-correta"), request);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.roles()).containsExactly("ROLE_CAIXA");
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void rejeitaLoginParaPerfisClienteAposValidarSenha() {
        for (String nomePerfil : java.util.List.of("CLIENTE", "CLIENTE_B2C", "ROLE_CLIENTE", "ROLE_CLIENTE_B2C",
                "Clíente B2C")) {
            Usuario cliente = usuarioComPerfil(nomePerfil, "cliente_teste");
            when(usuarioRepository.findByLogin("cliente_teste")).thenReturn(Optional.of(cliente));

            assertThatThrownBy(() -> authService.login(login("cliente_teste", "senha-incorreta"), request))
                    .isInstanceOf(BadCredentialsException.class);

            assertThatThrownBy(() -> authService.login(login("cliente_teste", "senha-correta"), request))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("desabilitado");
        }
    }

    @Test
    void rejeitaRefreshParaPerfisClienteERevogaTokensAtivos() {
        for (String nomePerfil : java.util.List.of("CLIENTE", "CLIENTE_B2C")) {
            Usuario cliente = usuarioComPerfil(nomePerfil, "cliente_refresh");
            RefreshToken current = refreshToken("refresh-cliente-" + nomePerfil, cliente, LocalDateTime.now().plusDays(1));
            RefreshToken otherActive = refreshToken("refresh-ativo-" + nomePerfil, cliente, LocalDateTime.now().plusDays(2));
            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(current));
            when(refreshTokenRepository.findByUsuarioAndRevokedAtIsNull(cliente))
                    .thenReturn(java.util.List.of(current, otherActive));

            assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequestDTO("refresh-cliente-" + nomePerfil), request))
                    .isInstanceOf(BadCredentialsException.class);

            assertThat(current.getRevokedAt()).isNotNull();
            assertThat(otherActive.getRevokedAt()).isNotNull();
            verify(refreshTokenRepository).saveAll(java.util.List.of(current, otherActive));
        }
    }

    @Test
    void refreshValidoRevogaTokenAnteriorEEmiteRotacao() {
        Usuario usuario = usuarioAtivo();
        RefreshToken current = refreshToken("refresh-a", usuario, LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(current));

        AuthResponseDTO response = authService.refresh(new RefreshTokenRequestDTO("refresh-a"), request);

        assertThat(current.getRevokedAt()).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotEqualTo("refresh-a");
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void refreshExpiradoEReutilizadoSaoRejeitados() {
        Usuario usuario = usuarioAtivo();
        RefreshToken expired = refreshToken("refresh-expirado", usuario, LocalDateTime.now().minusMinutes(1));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequestDTO("refresh-expirado"), request))
                .isInstanceOf(BadCredentialsException.class);

        RefreshToken reused = refreshToken("refresh-reutilizado", usuario, LocalDateTime.now().plusDays(1));
        reused.setRevokedAt(LocalDateTime.now().minusMinutes(1));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(reused));
        when(refreshTokenRepository.findByUsuarioAndRevokedAtIsNull(usuario)).thenReturn(java.util.List.of());
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequestDTO("refresh-reutilizado"), request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshDeUsuarioInativoERejeitado() {
        Usuario usuario = usuarioAtivo();
        usuario.setStatus(0);
        RefreshToken token = refreshToken("refresh-inativo", usuario, LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequestDTO("refresh-inativo"), request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void logoutRevogaRefreshTokenPersistido() {
        Usuario usuario = usuarioAtivo();
        RefreshToken token = refreshToken("refresh-logout", usuario, LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        authService.logout(new RefreshTokenRequestDTO("refresh-logout"));

        assertThat(token.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(token);
    }

    private LoginRequestDTO login(String login, String senha) {
        return new LoginRequestDTO(login, senha, null, "2026-01", true);
    }

    private Usuario usuarioAtivo() {
        return usuarioComPerfil("VENDEDOR", "operador");
    }

    private Usuario usuarioComPerfil(String nomePerfil, String login) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7L);
        usuario.setNome("Operador");
        usuario.setLogin(login);
        usuario.setSenha(passwordEncoder.encode("senha-correta"));
        usuario.setStatus(1);
        usuario.setEmpresaId(3L);
        usuario.setFilialId(4L);
        Perfil perfil = new Perfil();
        perfil.setNome(nomePerfil);
        usuario.setPerfil(perfil);
        return usuario;
    }

    private RefreshToken refreshToken(String rawToken, Usuario usuario, LocalDateTime expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setUsuario(usuario);
        token.setTokenHash(new DataProtectionService().hash(rawToken));
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(expiresAt);
        return token;
    }
}
