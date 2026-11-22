package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;

import DTO.LicenseValidationRequestDTO;
import DTO.LicenseValidationResponseDTO;
import DTO.TenantEntitlementResponseDTO;
import Repository.EmpresaPlanoLicencaRepository;
import Repository.LicenseActivationRepository;
import Service.LicenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "apex.license.catalog=TEST-ALL|all|2|30;TEST-DESKTOP|desktop|1|30;TEST-DUO|desktop+mobile-staff|1|30;TEST-ADV|all|3|30|PREMIUM|BASIC_PRICING,FINANCIAL_CORE,ADVANCED_FINANCE|tenant-adv|77"
})
@ActiveProfiles("test")
class LicenseServiceTests {

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private LicenseActivationRepository licenseActivationRepository;

    @Autowired
    private EmpresaPlanoLicencaRepository empresaPlanoLicencaRepository;

    @BeforeEach
    void cleanLicenses() {
        licenseActivationRepository.deleteAll();
        empresaPlanoLicencaRepository.deleteAll();
    }

    @Test
    void allAppsKeyAllowsAnyAppInCatalogAndDefaultsToEssential() {
        LicenseValidationResponseDTO desktop = licenseService.validate(request("TEST-ALL", "device-1", "desktop"));
        LicenseValidationResponseDTO web = licenseService.validate(request("TEST-ALL", "device-1", "web-client"));

        assertThat(desktop.valid()).isTrue();
        assertThat(web.valid()).isTrue();
        assertThat(web.allowedApps()).containsExactly("desktop", "mobile-staff", "mobile-client", "web-client");
        assertThat(web.activatedApps()).contains("desktop", "web-client");
        assertThat(web.subscriptionTier()).isEqualTo("ESSENTIAL");
        assertThat(web.features()).containsExactly("BASIC_PRICING", "FINANCIAL_CORE");
    }

    @Test
    void singleAppKeyBlocksAppsOutsideContract() {
        LicenseValidationResponseDTO desktop = licenseService.validate(request("TEST-DESKTOP", "device-1", "desktop"));
        LicenseValidationResponseDTO mobileClient = licenseService.validate(request("TEST-DESKTOP", "device-1", "mobile-client"));

        assertThat(desktop.valid()).isTrue();
        assertThat(mobileClient.valid()).isFalse();
        assertThat(mobileClient.status()).isEqualTo("APP_NOT_ALLOWED");
    }

    @Test
    void bundleKeyAllowsConfiguredAppsOnSameDeviceAndBlocksExtraDevices() {
        LicenseValidationResponseDTO desktop = licenseService.validate(request("TEST-DUO", "device-1", "desktop"));
        LicenseValidationResponseDTO staff = licenseService.validate(request("TEST-DUO", "device-1", "mobile-staff"));
        LicenseValidationResponseDTO secondDevice = licenseService.validate(request("TEST-DUO", "device-2", "desktop"));

        assertThat(desktop.valid()).isTrue();
        assertThat(staff.valid()).isTrue();
        assertThat(secondDevice.valid()).isFalse();
        assertThat(secondDevice.status()).isEqualTo("LIMIT_REACHED");
    }

    @Test
    void protectedApiCheckRequiresPriorActivationForTheSameApp() {
        licenseService.validate(request("TEST-DESKTOP", "device-1", "desktop"));

        LicenseValidationResponseDTO desktop = licenseService.checkActivation("TEST-DESKTOP", "device-1", "desktop", "electron");
        LicenseValidationResponseDTO mobile = licenseService.checkActivation("TEST-DESKTOP", "device-1", "mobile-client", "mobile");

        assertThat(desktop.valid()).isTrue();
        assertThat(mobile.valid()).isFalse();
        assertThat(mobile.status()).isEqualTo("APP_NOT_ALLOWED");
    }

    @Test
    void extendedCatalogSyncsTenantEntitlementAndAdvancedFeatures() {
        LicenseValidationResponseDTO res = licenseService.validate(request("TEST-ADV", "device-pro", "web-client"));

        assertThat(res.valid()).isTrue();
        assertThat(res.subscriptionTier()).isEqualTo("PREMIUM");
        assertThat(res.tenantCode()).isEqualTo("tenant-adv");
        assertThat(res.features()).contains("BASIC_PRICING", "FINANCIAL_CORE", "ADVANCED_FINANCE");
        assertThat(res.contingenciaAte()).isNotNull();

        TenantEntitlementResponseDTO entitlement = licenseService.getEntitlement(77L);
        assertThat(entitlement.subscriptionTier()).isEqualTo("PREMIUM");
        assertThat(entitlement.features()).contains("ADVANCED_FINANCE");
        assertThat(entitlement.status()).isEqualTo("ATIVO");

        // syncTenantPlan sem tenantCode preserva o tenantCode existente
        TenantEntitlementResponseDTO synced = licenseService.syncTenantPlan(
                77L, null, null, "GROWTH", null, null, null, null, "SYSADMIN");
        assertThat(synced.tenantCode()).isEqualTo("tenant-adv");
        assertThat(synced.subscriptionTier()).isEqualTo("GROWTH");
    }

    @Test
    void chaveSemVinculoNaoSobrescreveTenantExistenteERecusaDivergenciaCatalogo() {
        licenseService.syncTenantPlan(88L, "tenant-88", "Empresa 88", "PREMIUM", null, null, 72, "ATIVO", "SYSADMIN");

        // Tentativa de chave legada informando empresaId=88 nao pode rebaixar plano existente para ESSENTIAL
        LicenseValidationRequestDTO reqLegada = new LicenseValidationRequestDTO(
                "TEST-ALL", "dev-anon", "Anon", "web", "3.1.0", "web-client", "tenant-88", 88L);
        LicenseValidationResponseDTO respLegada = licenseService.validate(reqLegada);
        assertThat(respLegada.valid()).isTrue();
        assertThat(licenseService.getEntitlement(88L).subscriptionTier()).isEqualTo("PREMIUM");

        // Tentativa de usar chave vinculada a empresa 77 para outro empresaId e recusada
        LicenseValidationRequestDTO reqDivergente = new LicenseValidationRequestDTO(
                "TEST-ADV", "dev-anon2", "Anon2", "web", "3.1.0", "web-client", "tenant-adv", 88L);
        LicenseValidationResponseDTO respDivergente = licenseService.validate(reqDivergente);
        assertThat(respDivergente.valid()).isFalse();
        assertThat(respDivergente.status()).isEqualTo("TENANT_MISMATCH");
    }

    private LicenseValidationRequestDTO request(String key, String device, String appId) {
        return new LicenseValidationRequestDTO(key, device, "Dispositivo de teste", appId, "3.1.0", appId);
    }
}
