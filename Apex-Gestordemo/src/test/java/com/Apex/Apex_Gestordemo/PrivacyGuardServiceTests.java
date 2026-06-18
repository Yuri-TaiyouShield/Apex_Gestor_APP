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
    void grantsSensitiveDataOnlyToAuthorizedRoles() {
        MockHttpServletRequest sysadmin = new MockHttpServletRequest();
        sysadmin.addHeader("Authorization", "Bearer demo-access-v5_sysadmin-abc");

        MockHttpServletRequest cashier = new MockHttpServletRequest();
        cashier.addHeader("Authorization", "Bearer demo-access-v5_caixa-abc");

        assertThat(privacyGuardService.canViewSensitiveData(sysadmin)).isTrue();
        assertThat(privacyGuardService.canAdminPrivacy(sysadmin)).isTrue();
        assertThat(privacyGuardService.canViewSensitiveData(cashier)).isFalse();
        assertThat(privacyGuardService.canAdminPrivacy(cashier)).isFalse();
    }
}
