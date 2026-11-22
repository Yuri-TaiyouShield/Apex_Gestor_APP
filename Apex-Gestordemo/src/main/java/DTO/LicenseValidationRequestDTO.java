package DTO;

import jakarta.validation.constraints.NotBlank;

public record LicenseValidationRequestDTO(
        @NotBlank String licenseKey,
        @NotBlank String deviceFingerprint,
        String deviceLabel,
        String platform,
        String appVersion,
        String appId,
        String tenantCode,
        Long empresaId
) {
    public LicenseValidationRequestDTO(
            String licenseKey,
            String deviceFingerprint,
            String deviceLabel,
            String platform,
            String appVersion,
            String appId) {
        this(licenseKey, deviceFingerprint, deviceLabel, platform, appVersion, appId, null, null);
    }
}
