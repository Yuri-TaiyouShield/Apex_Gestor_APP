package com.Apex.Apex_Gestordemo;

import DTO.LicenseValidationRequestDTO;
import DTO.LicenseValidationResponseDTO;
import Repository.LicenseActivationRepository;
import Service.LicenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "apex.license.catalog=TEST-ALL|all|2|30;TEST-DESKTOP|desktop|1|30;TEST-DUO|desktop+mobile-staff|1|30"
})
@ActiveProfiles("test")
class LicenseServiceTests {

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private LicenseActivationRepository licenseActivationRepository;

    @BeforeEach
    void cleanLicenses() {
        licenseActivationRepository.deleteAll();
    }

    @Test
    void allAppsKeyAllowsAnyAppInCatalog() {
        LicenseValidationResponseDTO desktop = licenseService.validate(request("TEST-ALL", "device-1", "desktop"));
        LicenseValidationResponseDTO web = licenseService.validate(request("TEST-ALL", "device-1", "web-client"));

        assertThat(desktop.valid()).isTrue();
        assertThat(web.valid()).isTrue();
        assertThat(web.allowedApps()).containsExactly("desktop", "mobile-staff", "mobile-client", "web-client");
        assertThat(web.activatedApps()).contains("desktop", "web-client");
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

    private LicenseValidationRequestDTO request(String key, String device, String appId) {
        return new LicenseValidationRequestDTO(key, device, "Dispositivo de teste", appId, "3.1.0", appId);
    }
}
