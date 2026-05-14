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
        List<String> activatedApps
) {
}
