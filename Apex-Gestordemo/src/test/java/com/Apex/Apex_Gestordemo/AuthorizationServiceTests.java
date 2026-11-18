package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;

import Service.AuthorizationService;
import Service.PrivacyGuardService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthorizationServiceTests {

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        authorizationService = new AuthorizationService();
        ReflectionTestUtils.setField(authorizationService, "privacyGuardService", new PrivacyGuardService());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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
    void sysadminTemControleTotalEDonoEquivaleADonoGerente() {
        MockHttpServletRequest sysadmin = requestWithRole("ROLE_SYSADMIN");
        assertThat(authorizationService.can(sysadmin, ApexPermission.PERMISSION_MANAGE)).isTrue();
        assertThat(authorizationService.can(sysadmin, ApexPermission.PERSONAL_DATA_ANONYMIZE)).isTrue();
        assertThat(authorizationService.isOwnerOrSysadmin(sysadmin)).isTrue();

        for (String role : List.of("ROLE_DONO", "ROLE_DONO_GERENTE")) {
            MockHttpServletRequest req = requestWithRole(role);
            assertThat(authorizationService.isOwnerOrSysadmin(req)).isTrue();
            assertThat(authorizationService.can(req, ApexPermission.PERSONAL_DATA_EXPORT)).isTrue();
            assertThat(authorizationService.can(req, ApexPermission.PERSONAL_DATA_ANONYMIZE)).isTrue();
            assertThat(authorizationService.can(req, ApexPermission.BUDGET_APPROVE)).isTrue();
            assertThat(authorizationService.can(req, ApexPermission.PERMISSION_MANAGE)).isFalse();
        }
    }

    @Test
    void gerenteAdminMasterEAdvogadoPossuemRestricoesOnda1() {
        MockHttpServletRequest gerente = requestWithRole("ROLE_GERENTE");
        assertThat(authorizationService.can(gerente, ApexPermission.AUDIT_LOG_VIEW)).isTrue();
        assertThat(authorizationService.can(gerente, ApexPermission.BUDGET_APPROVE)).isTrue();
        assertThat(authorizationService.can(gerente, ApexPermission.PERSONAL_DATA_EXPORT)).isFalse();
        assertThat(authorizationService.can(gerente, ApexPermission.PERSONAL_DATA_ANONYMIZE)).isFalse();

        MockHttpServletRequest adminMaster = requestWithRole("ROLE_ADMIN_MASTER");
        assertThat(authorizationService.can(adminMaster, ApexPermission.AUDIT_LOG_VIEW)).isTrue();
        assertThat(authorizationService.can(adminMaster, ApexPermission.CUSTOMER_VIEW)).isFalse();
        assertThat(authorizationService.can(adminMaster, ApexPermission.PERSONAL_DATA_VIEW)).isFalse();
        assertThat(authorizationService.can(adminMaster, ApexPermission.PERSONAL_DATA_EXPORT)).isFalse();
        assertThat(authorizationService.can(adminMaster, ApexPermission.PERSONAL_DATA_ANONYMIZE)).isFalse();

        MockHttpServletRequest advogado = requestWithRole("ROLE_ADVOGADO");
        assertThat(authorizationService.can(advogado, ApexPermission.FINANCE_COMPLIANCE_VIEW)).isTrue();
        assertThat(authorizationService.can(advogado, ApexPermission.FINANCE_COMPLIANCE_SIGN)).isTrue();
        assertThat(authorizationService.can(advogado, ApexPermission.FINANCE_VIEW)).isFalse();
        assertThat(authorizationService.can(advogado, ApexPermission.FINANCE_DRE_VIEW)).isFalse();
        assertThat(authorizationService.can(advogado, ApexPermission.FINANCE_CASHFLOW_VIEW)).isFalse();
        assertThat(authorizationService.can(advogado, ApexPermission.FINANCIAL_INTELLIGENCE_VIEW)).isFalse();
        assertThat(authorizationService.can(advogado, ApexPermission.AUDIT_LOG_VIEW)).isFalse();
        assertThat(authorizationService.can(advogado, ApexPermission.PERSONAL_DATA_EXPORT)).isFalse();
    }

    private MockHttpServletRequest requestWithRole(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                role.toLowerCase(), null, List.of(new SimpleGrantedAuthority(role))));
        return request;
    }
}
