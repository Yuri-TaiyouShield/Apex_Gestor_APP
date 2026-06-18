package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;

import Service.AuthorizationService;
import Service.PrivacyGuardService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

class AuthorizationServiceTests {

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
        ReflectionTestUtils.setField(authorizationService, "privacyGuardService", new PrivacyGuardService());
    }

    @Test
    void vendedorNaoAcessaCaixa() {
        MockHttpServletRequest request = requestWithRole("ROLE_VENDEDOR");

        assertThat(authorizationService.can(request, ApexPermission.SALE_CREATE)).isTrue();
        assertThat(authorizationService.can(request, ApexPermission.CASHIER_OPEN)).isFalse();
        assertThat(authorizationService.can(request, ApexPermission.CASHIER_CLOSE)).isFalse();
        assertThat(authorizationService.can(request, ApexPermission.SALE_RECEIVE)).isFalse();
        assertThat(authorizationService.can(request, ApexPermission.FINANCE_VIEW)).isFalse();
    }

    @Test
    void caixaNaoEditaProdutoNemPreco() {
        MockHttpServletRequest request = requestWithRole("ROLE_CAIXA");

        assertThat(authorizationService.can(request, ApexPermission.CASHIER_OPEN)).isTrue();
        assertThat(authorizationService.can(request, ApexPermission.PRODUCT_VIEW)).isTrue();
        assertThat(authorizationService.can(request, ApexPermission.CUSTOMER_VIEW)).isTrue();
        assertThat(authorizationService.can(request, ApexPermission.CUSTOMER_UPDATE)).isFalse();
        assertThat(authorizationService.can(request, ApexPermission.PRODUCT_UPDATE)).isFalse();
        assertThat(authorizationService.can(request, ApexPermission.PRODUCT_PRICE_UPDATE)).isFalse();
        assertThat(authorizationService.can(request, ApexPermission.SUPPLIER_VIEW)).isFalse();
        assertThat(authorizationService.can(request, ApexPermission.SALE_RECEIVE)).isTrue();
        assertThat(authorizationService.can(request, ApexPermission.SALE_RETURN)).isFalse();
    }

    @Test
    void vendedorConsultaClienteMasNaoMantemCadastro() {
        MockHttpServletRequest request = requestWithRole("ROLE_VENDEDOR");

        assertThat(authorizationService.can(request, ApexPermission.CUSTOMER_VIEW)).isTrue();
        assertThat(authorizationService.can(request, ApexPermission.CUSTOMER_CREATE)).isFalse();
        assertThat(authorizationService.can(request, ApexPermission.CUSTOMER_UPDATE)).isFalse();
        assertThat(authorizationService.can(request, ApexPermission.CUSTOMER_DISABLE)).isFalse();
        assertThat(authorizationService.can(request, ApexPermission.SUPPLIER_VIEW)).isFalse();
    }

    @Test
    void sysadminTemControleTotal() {
        MockHttpServletRequest request = requestWithRole("ROLE_SYSADMIN");

        assertThat(authorizationService.can(request, ApexPermission.PERMISSION_MANAGE)).isTrue();
        assertThat(authorizationService.can(request, ApexPermission.PERSONAL_DATA_ANONYMIZE)).isTrue();
    }

    private MockHttpServletRequest requestWithRole(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Apex-Roles", role);
        request.addHeader("X-Apex-User-Login", role.toLowerCase());
        return request;
    }
}
