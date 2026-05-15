package DTO;

import java.util.List;

public record TenantFeatureContextDTO(
        String tenantCode,
        String tenantName,
        String planCode,
        String subscriptionTier,
        List<String> features,
        TenantBrandingDTO branding
) {
    public boolean hasFeature(String featureKey) {
        return featureKey != null && features != null && features.contains(featureKey);
    }
}
