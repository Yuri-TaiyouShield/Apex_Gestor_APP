package Service;

import DTO.TenantBrandingDTO;
import DTO.TenantFeatureContextDTO;
import Model.FeatureToggle;
import Model.SubscriptionPlan;
import Model.Tenant;
import Model.TenantFeatureOverride;
import Model.TenantSubscription;
import Repository.FeatureToggleRepository;
import Repository.TenantFeatureOverrideRepository;
import Repository.TenantRepository;
import Repository.TenantSubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TenantFeatureService {

    private static final String ACTIVE = "ACTIVE";

    private final TenantRepository tenantRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final TenantFeatureOverrideRepository tenantFeatureOverrideRepository;
    private final FeatureToggleRepository featureToggleRepository;

    @Value("${apex.tenant.default-code:apex-demo}")
    private String defaultTenantCode;

    public TenantFeatureService(
            TenantRepository tenantRepository,
            TenantSubscriptionRepository tenantSubscriptionRepository,
            TenantFeatureOverrideRepository tenantFeatureOverrideRepository,
            FeatureToggleRepository featureToggleRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.tenantFeatureOverrideRepository = tenantFeatureOverrideRepository;
        this.featureToggleRepository = featureToggleRepository;
    }

    @Transactional(readOnly = true)
    public TenantFeatureContextDTO resolve(String requestedTenantCode) {
        String tenantCode = normalizeTenantCode(requestedTenantCode);
        Tenant tenant = tenantRepository.findByTenantCodeAndStatus(tenantCode, ACTIVE).orElse(null);
        if (tenant == null) {
            return fallbackContext(tenantCode);
        }

        TenantSubscription subscription = tenantSubscriptionRepository
                .findFirstByTenantTenantCodeAndStatusOrderByStartedAtDesc(tenant.getTenantCode(), ACTIVE)
                .orElse(null);
        if (subscription == null) {
            return fallbackContext(tenant.getTenantCode());
        }

        SubscriptionPlan plan = subscription.getPlan();
        Set<String> features = new LinkedHashSet<>();
        for (FeatureToggle feature : plan.getFeatures()) {
            if (ACTIVE.equals(feature.getStatus())) {
                features.add(feature.getFeatureKey());
            }
        }

        for (TenantFeatureOverride override : tenantFeatureOverrideRepository.findByTenantTenantCode(tenant.getTenantCode())) {
            String featureKey = override.getFeatureToggle().getFeatureKey();
            if (Boolean.TRUE.equals(override.getEnabled())) {
                features.add(featureKey);
            } else {
                features.remove(featureKey);
            }
        }

        return new TenantFeatureContextDTO(
                tenant.getTenantCode(),
                tenant.getTradeName(),
                plan.getCode(),
                plan.getTierName(),
                features.stream().sorted().toList(),
                new TenantBrandingDTO(
                        tenant.getBrandPrimaryColor(),
                        tenant.getBrandSecondaryColor(),
                        tenant.getBrandLogoUrl(),
                        tenant.getTradeName()
                )
        );
    }

    @Transactional(readOnly = true)
    public boolean hasFeature(String tenantCode, String featureKey) {
        return resolve(tenantCode).hasFeature(featureKey);
    }

    @Transactional(readOnly = true)
    public void requireFeature(String tenantCode, String featureKey) {
        TenantFeatureContextDTO context = resolve(tenantCode);
        if (!context.hasFeature(featureKey)) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "O plano " + context.subscriptionTier() + " nao possui a feature " + featureKey + "."
            );
        }
    }

    @Transactional(readOnly = true)
    public List<String> catalog() {
        return featureToggleRepository.findByStatus(ACTIVE).stream()
                .map(FeatureToggle::getFeatureKey)
                .sorted()
                .toList();
    }

    public String normalizeTenantCode(String tenantCode) {
        String value = tenantCode == null || tenantCode.isBlank() ? defaultTenantCode : tenantCode;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private TenantFeatureContextDTO fallbackContext(String tenantCode) {
        return new TenantFeatureContextDTO(
                normalizeTenantCode(tenantCode),
                "Apex Demo",
                "ESSENTIAL",
                "Essential",
                List.of(TenantFeatureKey.BASIC_PRICING, TenantFeatureKey.FINANCIAL_CORE),
                new TenantBrandingDTO("#0b3a42", "#1e3a8a", null, "Apex Demo")
        );
    }
}
