package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;

import Service.JwtTokenService;
import Service.PrivacyGuardService;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

class PrivacyGuardServiceTests {

    private final PrivacyGuardService privacyGuardService = new PrivacyGuardService();

    @Test
    void masksPersonalDataForOperationalUsers() {
        assertThat(privacyGuardService.maskDocument("123.456.789-09")).isEqualTo("***8909");
        assertThat(privacyGuardService.maskPhone("(11) 98888-7777")).isEqualTo("(**) ****-7777");
        assertThat(privacyGuardService.maskEmail("cliente@apex.local")).isEqualTo("c***@apex.local");
        assertThat(privacyGuardService.maskIdentifier("gerente.silva")).isEqualTo("ge***a");
        assertThat(privacyGuardService.maskIdentifier("admin@apex.local")).isEqualTo("a***@apex.local");
        assertThat(privacyGuardService.maskIp("192.168.15.42")).isEqualTo("192.168.*.*");
        assertThat(privacyGuardService.maskIp("2001:db8:85a3::8a2e:370:7334")).isEqualTo("2001:db8:***");
    }

    @Test
    void ignoresClientSuppliedIdentityHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Apex-Roles", "ROLE_SYSADMIN");
        request.addHeader("X-Apex-User-Id", "1");
        request.addHeader("X-Apex-Empresa-Id", "1");
        request.addHeader("X-Apex-Filial-Id", "1");

        assertThat(privacyGuardService.canViewSensitiveData(request)).isFalse();
        assertThat(privacyGuardService.canAdminPrivacy(request)).isFalse();
        assertThat(privacyGuardService.currentUserId(request)).isNull();
        assertThat(privacyGuardService.currentEmpresaId(request)).isNull();
        assertThat(privacyGuardService.currentFilialId(request)).isNull();
    }

    @Test
    void separatesPrivacyOwnersFromManagersAdminMasterAndAdvogado() {
        JwtTokenService jwt = new JwtTokenService(
                "privacy-guard-test-secret-with-32-chars");
        PrivacyGuardService guard = new PrivacyGuardService();
        ReflectionTestUtils.setField(guard, "jwtTokenService", jwt);

        MockHttpServletRequest donoReq = jwtRequest(jwt, 1L, "dono", "ROLE_DONO");
        MockHttpServletRequest donoGerenteReq = jwtRequest(jwt, 2L, "dono_gerente", "ROLE_DONO_GERENTE");
        MockHttpServletRequest adminReq = jwtRequest(jwt, 3L, "admin", "ROLE_ADMIN");
        MockHttpServletRequest gerenteReq = jwtRequest(jwt, 4L, "gerente", "ROLE_GERENTE");
        MockHttpServletRequest adminMasterReq = jwtRequest(jwt, 5L, "admin_master", "ROLE_ADMIN_MASTER");
        MockHttpServletRequest advogadoReq = jwtRequest(jwt, 6L, "advogado", "ROLE_ADVOGADO");

        assertThat(guard.isPrivacyOwner(donoReq)).isTrue();
        assertThat(guard.canViewUnmaskedAudit(donoReq)).isTrue();
        assertThat(guard.canViewSensitiveData(donoReq)).isTrue();

        assertThat(guard.isPrivacyOwner(donoGerenteReq)).isTrue();
        assertThat(guard.canViewUnmaskedAudit(donoGerenteReq)).isTrue();

        assertThat(guard.isPrivacyOwner(adminReq)).isFalse();
        assertThat(guard.canAdminPrivacy(adminReq)).isFalse();
        assertThat(guard.canViewAudit(adminReq)).isTrue();
        assertThat(guard.canViewUnmaskedAudit(adminReq)).isFalse();

        assertThat(guard.isPrivacyOwner(gerenteReq)).isFalse();
        assertThat(guard.canViewAudit(gerenteReq)).isTrue();
        assertThat(guard.canViewUnmaskedAudit(gerenteReq)).isFalse();

        assertThat(guard.isPrivacyOwner(adminMasterReq)).isFalse();
        assertThat(guard.canViewSensitiveData(adminMasterReq)).isFalse();
        assertThat(guard.canViewAudit(adminMasterReq)).isTrue();
        assertThat(guard.canViewUnmaskedAudit(adminMasterReq)).isFalse();

        assertThat(guard.isPrivacyOwner(advogadoReq)).isFalse();
        assertThat(guard.canViewSensitiveData(advogadoReq)).isFalse();
        assertThat(guard.canViewAudit(advogadoReq)).isFalse();
    }

    private MockHttpServletRequest jwtRequest(JwtTokenService jwt, Long uid, String login, String role) {
        String token = jwt.issueAccessToken(uid, login, login, List.of(role), 1L, 1L, Duration.ofMinutes(10));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
