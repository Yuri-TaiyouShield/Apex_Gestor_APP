package DTO;

import java.time.LocalDateTime;
import java.util.List;

public record TenantEntitlementResponseDTO(
        Long empresaId,
        String tenantCode,
        String tenantName,
        String subscriptionTier,
        List<String> features,
        String status,
        LocalDateTime validoAte,
        LocalDateTime ultimaValidacaoEm,
        LocalDateTime contingenciaAte,
        Integer gracePeriodHoras,
        boolean emGracePeriod
) {
}
