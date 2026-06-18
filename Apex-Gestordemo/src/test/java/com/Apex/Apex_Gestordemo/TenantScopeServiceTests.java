package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import Service.PrivacyGuardService;
import Service.TenantScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

class TenantScopeServiceTests {

    private TenantScopeService tenantScopeService;

    @BeforeEach
    void setUp() {
        tenantScopeService = new TenantScopeService();
        ReflectionTestUtils.setField(tenantScopeService, "privacyGuardService", new PrivacyGuardService());
    }

    @Test
    void resolvesEmpresaAndFilialFromRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Apex-Empresa-Id", "7");
        request.addHeader("X-Apex-Filial-Id", "3");

        assertThat(tenantScopeService.empresaId(request)).isEqualTo(7L);
        assertThat(tenantScopeService.filialId(request)).isEqualTo(3L);
    }

    @Test
    void rejectsMissingScopeInsteadOfDefaultingSilently() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> tenantScopeService.empresaId(request))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> tenantScopeService.filialId(request))
                .isInstanceOf(AccessDeniedException.class);
    }
}
