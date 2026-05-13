package DTO;

import java.time.LocalDateTime;

public record LicenseValidationResponseDTO(
        boolean valid,
        String status,
        String message,
        LocalDateTime expiresAt,
        Long remainingActivations,
        String deviceHash
) {
}
