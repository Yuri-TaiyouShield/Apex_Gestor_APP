package DTO;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

public record TenantPlanSyncRequestDTO(
        Long empresaId,
        String tenantCode,
        String tenantName,
        @NotBlank String subscriptionTier,
        List<String> features,
        LocalDateTime validoAte,
        Integer gracePeriodHoras,
        String status
) {
}
