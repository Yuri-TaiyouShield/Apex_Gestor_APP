package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;

import Service.JwtTokenService;
import Service.PrivacyGuardService;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenServiceTests {

    @Test
    void emiteJwtAssinadoComClaimsOperacionais() {
        JwtTokenService service = tokenService();

        String token = service.issueAccessToken(7L, "Vendedor", "v5_vendedor",
                List.of("ROLE_VENDEDOR"), 3L, 4L, Duration.ofMinutes(10));
        JwtTokenService.JwtClaims claims = service.parseAndValidate(token);

        assertThat(claims.login()).isEqualTo("v5_vendedor");
        assertThat(claims.userId()).isEqualTo(7L);
        assertThat(claims.roles()).containsExactly("ROLE_VENDEDOR");
        assertThat(claims.empresaId()).isEqualTo(3L);
        assertThat(claims.filialId()).isEqualTo(4L);
    }

    @Test
    void privacyGuardPriorizaJwtValidoSobreHeadersManipulados() {
        JwtTokenService tokenService = tokenService();
        PrivacyGuardService privacyGuard = new PrivacyGuardService();
        ReflectionTestUtils.setField(privacyGuard, "jwtTokenService", tokenService);
        String token = tokenService.issueAccessToken(7L, "Vendedor", "v5_vendedor",
                List.of("ROLE_VENDEDOR"), 3L, 4L, Duration.ofMinutes(10));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        request.addHeader("X-Apex-Roles", "ROLE_SYSADMIN");
        request.addHeader("X-Apex-Empresa-Id", "999");

        assertThat(privacyGuard.currentRoles(request)).containsExactly("ROLE_VENDEDOR");
        assertThat(privacyGuard.currentEmpresaId(request)).isEqualTo(3L);
        assertThat(privacyGuard.currentUserLogin(request)).isEqualTo("v5_vendedor");
    }

    private JwtTokenService tokenService() {
        return new JwtTokenService("unit-test-secret");
    }
}
