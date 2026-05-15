package Service;

import DTO.LicenseValidationRequestDTO;
import DTO.LicenseValidationResponseDTO;
import DTO.TenantFeatureContextDTO;
import Model.LicenseActivation;
import Repository.LicenseActivationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LicenseService {

    private static final String ACTIVE = "ACTIVE";
    private static final String INVALID = "INVALID";
    private static final List<String> ALL_APPS = List.of("desktop", "mobile-staff", "mobile-client", "web-client");

    private final LicenseActivationRepository licenseActivationRepository;
    private final DataProtectionService dataProtectionService;
    private final TenantFeatureService tenantFeatureService;

    @Value("${apex.license.catalog:}")
    private String licenseCatalog;

    @Value("${apex.license.allowed-keys:APEX-DEMO-2026}")
    private String allowedKeys;

    @Value("${apex.license.max-devices-per-key:3}")
    private long maxDevicesPerKey;

    @Value("${apex.license.validity-days:30}")
    private long validityDays;

    public LicenseService(
            LicenseActivationRepository licenseActivationRepository,
            DataProtectionService dataProtectionService,
            TenantFeatureService tenantFeatureService
    ) {
        this.licenseActivationRepository = licenseActivationRepository;
        this.dataProtectionService = dataProtectionService;
        this.tenantFeatureService = tenantFeatureService;
    }

    @Transactional
    public LicenseValidationResponseDTO validate(LicenseValidationRequestDTO request) {
        String normalizedKey = normalizeRequired(request.licenseKey());
        String normalizedDevice = normalizeRequired(request.deviceFingerprint());
        String appId = normalizeAppId(request.appId(), request.platform());
        String tenantCode = tenantFeatureService.normalizeTenantCode(request.tenantCode());

        if (normalizedKey == null || normalizedDevice == null) {
            return invalid("INVALID_REQUEST", "Chave e identificador do dispositivo sao obrigatorios.", null, appId, null, tenantCode);
        }

        String licenseHash = dataProtectionService.hash(normalizedKey);
        String deviceHash = dataProtectionService.hash(normalizedDevice);
        LicensePlan plan = licensePlan(normalizedKey).orElse(null);

        if (plan == null) {
            return invalid(INVALID, "Licenca nao autorizada.", deviceHash, appId, null, tenantCode);
        }
        if (!plan.allows(appId)) {
            return invalid("APP_NOT_ALLOWED", "Esta chave nao libera o app solicitado.", deviceHash, appId, plan, tenantCode);
        }

        LocalDateTime now = LocalDateTime.now();
        List<LicenseActivation> activeActivations = activeActivations(licenseHash, now);
        LicenseActivation activation = licenseActivationRepository
                .findByLicenseKeyHashAndDeviceHashAndAppId(licenseHash, deviceHash, appId)
                .orElse(null);

        if (isUsable(activation, now)) {
            activation.setLastValidatedAt(now);
            activation.setAppVersion(trim(request.appVersion(), 40));
            activation.setTenantCode(tenantCode);
            licenseActivationRepository.save(activation);
            return response(true, ACTIVE, "Licenca valida para este app e dispositivo.", activation, licenseHash, plan, now, tenantCode);
        }

        long activeDevices = activeActivations.stream()
                .map(LicenseActivation::getDeviceHash)
                .distinct()
                .count();
        boolean deviceAlreadyActive = activeActivations.stream()
                .anyMatch(item -> deviceHash.equals(item.getDeviceHash()));

        if (activation == null && !deviceAlreadyActive && activeDevices >= plan.maxDevices()) {
            return invalid("LIMIT_REACHED", "Limite de dispositivos atingido para esta licenca.", deviceHash, appId, plan, tenantCode);
        }

        if (activation == null) {
            activation = new LicenseActivation();
            activation.setLicenseKeyHash(licenseHash);
            activation.setDeviceHash(deviceHash);
            activation.setActivatedAt(now);
        }

        activation.setDeviceLabel(trim(request.deviceLabel(), 120));
        activation.setPlatform(trim(request.platform(), 40));
        activation.setAppVersion(trim(request.appVersion(), 40));
        activation.setAppId(appId);
        activation.setLicensePlan(plan.name());
        activation.setTenantCode(tenantCode);
        activation.setLicensedApps(String.join(",", plan.allowedApps()));
        activation.setStatus(ACTIVE);
        activation.setLastValidatedAt(now);
        activation.setValidUntil(now.plusDays(plan.validityDays()));
        licenseActivationRepository.save(activation);

        return response(true, ACTIVE, "Licenca ativada com sucesso para este app.", activation, licenseHash, plan, now, tenantCode);
    }

    @Transactional
    public LicenseValidationResponseDTO checkActivation(String licenseKey, String deviceFingerprint, String appId, String platform) {
        return checkActivation(licenseKey, deviceFingerprint, appId, platform, null);
    }

    @Transactional
    public LicenseValidationResponseDTO checkActivation(String licenseKey, String deviceFingerprint, String appId, String platform, String tenantCode) {
        String normalizedKey = normalizeRequired(licenseKey);
        String normalizedDevice = normalizeRequired(deviceFingerprint);
        String normalizedAppId = normalizeAppId(appId, platform);
        String normalizedTenantCode = tenantFeatureService.normalizeTenantCode(tenantCode);

        if (normalizedKey == null || normalizedDevice == null) {
            return invalid("LICENSE_REQUIRED", "Informe uma licenca valida para usar este recurso.", null, normalizedAppId, null, normalizedTenantCode);
        }

        String licenseHash = dataProtectionService.hash(normalizedKey);
        String deviceHash = dataProtectionService.hash(normalizedDevice);
        LicensePlan plan = licensePlan(normalizedKey).orElse(null);
        if (plan == null) {
            return invalid(INVALID, "Licenca nao autorizada.", deviceHash, normalizedAppId, null, normalizedTenantCode);
        }
        if (!plan.allows(normalizedAppId)) {
            return invalid("APP_NOT_ALLOWED", "Esta chave nao libera o app solicitado.", deviceHash, normalizedAppId, plan, normalizedTenantCode);
        }

        LocalDateTime now = LocalDateTime.now();
        LicenseActivation activation = licenseActivationRepository
                .findByLicenseKeyHashAndDeviceHashAndAppId(licenseHash, deviceHash, normalizedAppId)
                .orElse(null);

        if (!isUsable(activation, now)) {
            return invalid("LICENSE_INACTIVE", "Licenca inativa, expirada ou nao ativada para este app.", deviceHash, normalizedAppId, plan, normalizedTenantCode);
        }

        if (activation.getLastValidatedAt().isBefore(now.minusMinutes(5))) {
            activation.setLastValidatedAt(now);
            activation.setTenantCode(normalizedTenantCode);
            licenseActivationRepository.save(activation);
        }
        return response(true, ACTIVE, "Licenca ativa.", activation, licenseHash, plan, now, normalizedTenantCode);
    }

    private LicenseValidationResponseDTO response(boolean valid, String status, String message, LicenseActivation activation, String licenseHash, LicensePlan plan, LocalDateTime now, String tenantCode) {
        long activeDevices = activeActivations(licenseHash, now).stream()
                .map(LicenseActivation::getDeviceHash)
                .distinct()
                .count();
        long remaining = Math.max(0, plan.maxDevices() - activeDevices);
        TenantFeatureContextDTO tenant = tenantFeatureService.resolve(tenantCode);
        return new LicenseValidationResponseDTO(
                valid,
                status,
                message,
                activation.getValidUntil(),
                remaining,
                shortHash(activation.getDeviceHash()),
                activation.getAppId(),
                plan.name(),
                plan.allowedApps(),
                activatedApps(licenseHash, now),
                tenant.tenantCode(),
                tenant.tenantName(),
                tenant.subscriptionTier(),
                tenant.features(),
                tenant.branding()
        );
    }

    private LicenseValidationResponseDTO invalid(String status, String message, String deviceHash, String appId) {
        return invalid(status, message, deviceHash, appId, null);
    }

    private LicenseValidationResponseDTO invalid(String status, String message, String deviceHash, String appId, LicensePlan plan) {
        return invalid(status, message, deviceHash, appId, plan, null);
    }

    private LicenseValidationResponseDTO invalid(String status, String message, String deviceHash, String appId, LicensePlan plan, String tenantCode) {
        TenantFeatureContextDTO tenant = tenantFeatureService.resolve(tenantCode);
        return new LicenseValidationResponseDTO(
                false,
                status,
                message,
                null,
                plan == null ? null : plan.maxDevices(),
                shortHash(deviceHash),
                appId,
                plan == null ? null : plan.name(),
                plan == null ? List.of() : plan.allowedApps(),
                List.of(),
                tenant.tenantCode(),
                tenant.tenantName(),
                tenant.subscriptionTier(),
                tenant.features(),
                tenant.branding()
        );
    }

    private List<LicenseActivation> activeActivations(String licenseHash, LocalDateTime now) {
        return licenseActivationRepository.findByLicenseKeyHashAndStatus(licenseHash, ACTIVE).stream()
                .filter(activation -> isUsable(activation, now))
                .toList();
    }

    private List<String> activatedApps(String licenseHash, LocalDateTime now) {
        return activeActivations(licenseHash, now).stream()
                .map(LicenseActivation::getAppId)
                .distinct()
                .sorted()
                .toList();
    }

    private boolean isUsable(LicenseActivation activation, LocalDateTime now) {
        return activation != null
                && ACTIVE.equals(activation.getStatus())
                && activation.getValidUntil() != null
                && activation.getValidUntil().isAfter(now);
    }

    private Optional<LicensePlan> licensePlan(String normalizedKey) {
        Optional<LicensePlan> catalogPlan = Arrays.stream(licenseCatalog.split(";"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::parsePlan)
                .flatMap(Optional::stream)
                .filter(plan -> plan.key().equals(normalizedKey))
                .findFirst();

        if (catalogPlan.isPresent()) {
            return catalogPlan;
        }
        if (allowedKeySet().contains(normalizedKey)) {
            return Optional.of(new LicensePlan(normalizedKey, "LEGACY_ALL", ALL_APPS, maxDevicesPerKey, validityDays));
        }
        return Optional.empty();
    }

    private Optional<LicensePlan> parsePlan(String value) {
        String[] parts = value.split("\\|");
        if (parts.length < 4) {
            return Optional.empty();
        }

        String key = normalizeRequired(parts[0]);
        if (key == null) {
            return Optional.empty();
        }

        List<String> apps = parseApps(parts[1]);
        if (apps.isEmpty()) {
            return Optional.empty();
        }

        long devices = parseLong(parts[2], maxDevicesPerKey);
        long days = parseLong(parts[3], validityDays);
        String planName = apps.size() == ALL_APPS.size() ? "ALL_APPS" : "APP_BUNDLE_" + apps.size();
        return Optional.of(new LicensePlan(key, planName, apps, devices, days));
    }

    private List<String> parseApps(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim())) {
            return ALL_APPS;
        }

        Set<String> apps = Arrays.stream(value.split("[+,]"))
                .map(app -> normalizeAppId(app, null))
                .filter(ALL_APPS::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return List.copyOf(apps);
    }

    private long parseLong(String value, long fallback) {
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private Set<String> allowedKeySet() {
        return Arrays.stream(allowedKeys.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private String normalizeAppId(String appId, String platform) {
        String value = normalize(appId);
        if (value != null) {
            return switch (value) {
                case "desktop", "electron", "app", "win32", "darwin", "linux" -> "desktop";
                case "staff", "mobile", "mobile-staff", "empresa" -> "mobile-staff";
                case "client", "mobile-client", "cliente" -> "mobile-client";
                case "web", "web-client", "site" -> "web-client";
                default -> value;
            };
        }

        String normalizedPlatform = normalize(platform);
        if (normalizedPlatform == null) {
            return "web-client";
        }
        if (normalizedPlatform.contains("win") || normalizedPlatform.contains("darwin") || normalizedPlatform.contains("linux") || normalizedPlatform.contains("electron")) {
            return "desktop";
        }
        if (normalizedPlatform.contains("capacitor") || normalizedPlatform.contains("ionic") || normalizedPlatform.contains("android") || normalizedPlatform.contains("ios")) {
            return "mobile-staff";
        }
        return "web-client";
    }

    private String normalizeRequired(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : value.trim();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private String shortHash(String hash) {
        return hash == null || hash.length() <= 12 ? hash : hash.substring(0, 12);
    }

    private record LicensePlan(String key, String name, List<String> allowedApps, long maxDevices, long validityDays) {
        private boolean allows(String appId) {
            return allowedApps.contains(appId);
        }
    }
}
