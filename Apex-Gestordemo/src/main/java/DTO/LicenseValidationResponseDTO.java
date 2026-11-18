package DTO;

import java.time.LocalDateTime;
import java.util.List;

public record LicenseValidationResponseDTO(
        boolean valid,
        String status,
        String message,
        LocalDateTime expiresAt,
        Long remainingActivations,
        String deviceHash,
        String appId,
        String licensePlan,
        List<String> allowedApps,
        List<String> activatedApps,
        String tenantCode,
        String tenantName,
        String subscriptionTier,
        List<String> features,
        TenantBrandingDTO branding,
        LocalDateTime contingenciaAte,
        boolean degradedMode
) {
    public LicenseValidationResponseDTO(
            boolean valid,
            String status,
            String message,
            LocalDateTime expiresAt,
            Long remainingActivations,
            String deviceHash,
            String appId,
            String licensePlan,
            List<String> allowedApps,
            List<String> activatedApps) {
        this(valid, status, message, expiresAt, remainingActivations, deviceHash, appId,
                licensePlan, allowedApps, activatedApps, null, null, null, List.of(), null, null, false);
    }
}
