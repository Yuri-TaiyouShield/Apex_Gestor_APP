package DTO;

import jakarta.validation.constraints.NotBlank;

public record LicenseValidationRequestDTO(
        @NotBlank String licenseKey,
        @NotBlank String deviceFingerprint,
        String deviceLabel,
        String platform,
        String appVersion
) {
}
