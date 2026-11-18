package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;

import Service.PrivacyGuardService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class PrivacyGuardServiceTests {

    private final PrivacyGuardService privacyGuardService = new PrivacyGuardService();

    @Test
    void masksPersonalDataForOperationalUsers() {
        assertThat(privacyGuardService.maskDocument("123.456.789-09")).isEqualTo("***8909");
        assertThat(privacyGuardService.maskPhone("(11) 98888-7777")).isEqualTo("(**) ****-7777");
        assertThat(privacyGuardService.maskEmail("cliente@apex.local")).isEqualTo("c***@apex.local");
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
}
