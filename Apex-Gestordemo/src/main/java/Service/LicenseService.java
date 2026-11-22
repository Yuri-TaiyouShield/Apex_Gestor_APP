package Service;

import DTO.LicenseValidationRequestDTO;
import DTO.LicenseValidationResponseDTO;
import DTO.TenantBrandingDTO;
import DTO.TenantEntitlementResponseDTO;
import Model.Empresa;
import Model.EmpresaPlanoLicenca;
import Model.LicenseActivation;
import Repository.EmpresaPlanoLicencaRepository;
import Repository.EmpresaRepository;
import Repository.LicenseActivationRepository;
import com.Apex.Apex_Gestordemo.security.ApexFeature;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LicenseService {

    private static final String ACTIVE = "ACTIVE";
    private static final String INVALID = "INVALID";
    private static final String TIER_ESSENTIAL = "ESSENTIAL";
    private static final String TIER_GROWTH = "GROWTH";
    private static final String TIER_PREMIUM = "PREMIUM";
    private static final Set<String> HARD_BLOCKED_STATUSES = Set.of(
            "SUSPENSO", "CANCELADO", "BLOQUEADO", "REVOGADO", "INATIVO");
    private static final List<String> ALL_APPS = List.of("desktop", "mobile-staff", "mobile-client", "web-client");
    private static final List<String> ESSENTIAL_DEFAULT_FEATURES = List.of("BASIC_PRICING", "FINANCIAL_CORE");
    private static final List<String> GROWTH_DEFAULT_FEATURES = List.of(
            "BASIC_PRICING", "FINANCIAL_CORE", "ADVANCED_FINANCE", "COMMISSION_OMNICHANNEL");
    private static final List<String> PREMIUM_DEFAULT_FEATURES = Arrays.stream(ApexFeature.values())
            .map(Enum::name)
            .toList();

    private final LicenseActivationRepository licenseActivationRepository;
    private final DataProtectionService dataProtectionService;
    private final EmpresaPlanoLicencaRepository empresaPlanoLicencaRepository;
    private final EmpresaRepository empresaRepository;

    @Value("${apex.license.catalog:}")
    private String licenseCatalog = "";

    @Value("${apex.license.allowed-keys:APEX-DEMO-2026}")
    private String allowedKeys = "APEX-DEMO-2026";

    @Value("${apex.license.max-devices-per-key:3}")
    private long maxDevicesPerKey = 3;

    @Value("${apex.license.validity-days:30}")
    private long validityDays = 30;

    @Value("${apex.license.grace-period-hours:72}")
    private int gracePeriodHours = 72;

    public LicenseService(
            LicenseActivationRepository licenseActivationRepository,
            DataProtectionService dataProtectionService) {
        this(licenseActivationRepository, dataProtectionService, null, null);
    }

    @Autowired
    public LicenseService(
            LicenseActivationRepository licenseActivationRepository,
            DataProtectionService dataProtectionService,
            EmpresaPlanoLicencaRepository empresaPlanoLicencaRepository,
            EmpresaRepository empresaRepository) {
        this.licenseActivationRepository = licenseActivationRepository;
        this.dataProtectionService = dataProtectionService;
        this.empresaPlanoLicencaRepository = empresaPlanoLicencaRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional
    public LicenseValidationResponseDTO validate(LicenseValidationRequestDTO request) {
        String normalizedKey = normalizeRequired(request.licenseKey());
        String normalizedDevice = normalizeRequired(request.deviceFingerprint());
        String appId = normalizeAppId(request.appId(), request.platform());

        if (normalizedKey == null || normalizedDevice == null) {
            return invalid("INVALID_REQUEST", "Chave e identificador do dispositivo sao obrigatorios.", null, appId);
        }

        String licenseHash = dataProtectionService.hash(normalizedKey);
        String deviceHash = dataProtectionService.hash(normalizedDevice);
        LicensePlan plan = licensePlan(normalizedKey).orElse(null);

        if (plan == null) {
            return invalid(INVALID, "Licenca nao autorizada.", deviceHash, appId);
        }
        if (!plan.allows(appId)) {
            return invalid("APP_NOT_ALLOWED", "Esta chave nao libera o app solicitado.", deviceHash, appId, plan);
        }

        if (plan.fromCatalog() && plan.empresaId() != null) {
            if (request.empresaId() != null && !plan.empresaId().equals(request.empresaId())) {
                return invalid("TENANT_MISMATCH", "Chave de licenca vinculada a outra empresa.", deviceHash, appId, plan);
            }
            if (request.tenantCode() != null && !request.tenantCode().isBlank()
                    && plan.tenantCode() != null && !plan.tenantCode().equalsIgnoreCase(request.tenantCode().trim())) {
                return invalid("TENANT_MISMATCH", "Chave de licenca vinculada a outro tenant.", deviceHash, appId, plan);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<LicenseActivation> activeActivations = activeActivations(licenseHash, now);
        LicenseActivation activation = licenseActivationRepository
                .findByLicenseKeyHashAndDeviceHashAndAppId(licenseHash, deviceHash, appId)
                .orElse(null);

        Long boundEmpresaId = plan.fromCatalog() ? plan.empresaId() : null;
        String resolvedTenantCode = boundEmpresaId != null
                ? resolveTenantCode(null, plan, boundEmpresaId)
                : plan.tenantCode();

        if (isUsable(activation, now)) {
            activation.setLastValidatedAt(now);
            activation.setAppVersion(trim(request.appVersion(), 40));
            activation.setSubscriptionTier(plan.subscriptionTier());
            activation.setLicensedFeatures(String.join(",", plan.features()));
            if (boundEmpresaId != null) {
                activation.setEmpresaId(boundEmpresaId);
            }
            if (resolvedTenantCode != null) {
                activation.setTenantCode(resolvedTenantCode);
            }
            licenseActivationRepository.save(activation);
            if (boundEmpresaId != null) {
                syncTenantEntitlement(boundEmpresaId, resolvedTenantCode, plan, licenseHash, activation.getValidUntil(), now);
            }
            return response(true, ACTIVE, "Licenca valida para este app e dispositivo.", activation, licenseHash, plan, now);
        }

        long activeDevices = activeActivations.stream()
                .map(LicenseActivation::getDeviceHash)
                .distinct()
                .count();
        boolean deviceAlreadyActive = activeActivations.stream()
                .anyMatch(item -> deviceHash.equals(item.getDeviceHash()));

        if (activation == null && !deviceAlreadyActive && activeDevices >= plan.maxDevices()) {
            return invalid("LIMIT_REACHED", "Limite de dispositivos atingido para esta licenca.", deviceHash, appId, plan);
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
        activation.setLicensedApps(String.join(",", plan.allowedApps()));
        activation.setSubscriptionTier(plan.subscriptionTier());
        activation.setLicensedFeatures(String.join(",", plan.features()));
        activation.setEmpresaId(boundEmpresaId);
        activation.setTenantCode(resolvedTenantCode);
        activation.setStatus(ACTIVE);
        activation.setLastValidatedAt(now);
        activation.setValidUntil(now.plusDays(plan.validityDays()));
        licenseActivationRepository.save(activation);

        if (boundEmpresaId != null) {
            syncTenantEntitlement(boundEmpresaId, resolvedTenantCode, plan, licenseHash, activation.getValidUntil(), now);
        }
        return response(true, ACTIVE, "Licenca ativada com sucesso para este app.", activation, licenseHash, plan, now);
    }

    @Transactional
    public LicenseValidationResponseDTO checkActivation(
            String licenseKey,
            String deviceFingerprint,
            String appId,
            String platform) {
        String normalizedKey = normalizeRequired(licenseKey);
        String normalizedDevice = normalizeRequired(deviceFingerprint);
        String normalizedAppId = normalizeAppId(appId, platform);

        if (normalizedKey == null || normalizedDevice == null) {
            return invalid("LICENSE_REQUIRED", "Informe uma licenca valida para usar este recurso.", null, normalizedAppId);
        }

        String licenseHash = dataProtectionService.hash(normalizedKey);
        String deviceHash = dataProtectionService.hash(normalizedDevice);
        LicensePlan plan = licensePlan(normalizedKey).orElse(null);
        if (plan == null) {
            return invalid(INVALID, "Licenca nao autorizada.", deviceHash, normalizedAppId);
        }
        if (!plan.allows(normalizedAppId)) {
            return invalid("APP_NOT_ALLOWED", "Esta chave nao libera o app solicitado.", deviceHash, normalizedAppId, plan);
        }

        LocalDateTime now = LocalDateTime.now();
        LicenseActivation activation = licenseActivationRepository
                .findByLicenseKeyHashAndDeviceHashAndAppId(licenseHash, deviceHash, normalizedAppId)
                .orElse(null);

        if (!isUsable(activation, now)) {
            return invalid("LICENSE_INACTIVE", "Licenca inativa, expirada ou nao ativada para este app.", deviceHash, normalizedAppId, plan);
        }

        if (activation.getLastValidatedAt().isBefore(now.minusMinutes(5))) {
            activation.setLastValidatedAt(now);
            licenseActivationRepository.save(activation);
        }
        return response(true, ACTIVE, "Licenca ativa.", activation, licenseHash, plan, now);
    }

    @Transactional(readOnly = true)
    public TenantEntitlementResponseDTO getEntitlement(Long empresaId) {
        if (empresaId == null) {
            throw new IllegalArgumentException("Empresa ativa obrigatoria para consultar licenciamento.");
        }
        if (empresaPlanoLicencaRepository != null) {
            Optional<EmpresaPlanoLicenca> existing = empresaPlanoLicencaRepository.findByEmpresaId(empresaId);
            if (existing.isPresent()) {
                return toEntitlementDto(existing.get());
            }
        }
        String tenantName = resolveTenantName(empresaId, "Empresa " + empresaId);
        return new TenantEntitlementResponseDTO(
                empresaId,
                "tenant-" + empresaId,
                tenantName,
                TIER_ESSENTIAL,
                ESSENTIAL_DEFAULT_FEATURES,
                "COMPAT_ESSENTIAL",
                null,
                null,
                null,
                effectiveGraceHours(),
                false);
    }

    @Transactional
    public TenantEntitlementResponseDTO syncTenantPlan(
            Long empresaId,
            String tenantCode,
            String tenantName,
            String subscriptionTier,
            List<String> features,
            LocalDateTime validoAte,
            Integer graceHoursOverride,
            String status,
            String atualizadoPor) {
        if (empresaId == null || empresaId <= 0) {
            throw new IllegalArgumentException("empresaId valido e obrigatorio para sincronizar plano.");
        }
        if (empresaPlanoLicencaRepository == null) {
            throw new IllegalStateException("Repositorio de planos de licenca nao disponivel.");
        }
        LocalDateTime now = LocalDateTime.now();
        String tier = normalizeTier(subscriptionTier);
        List<String> normalizedFeatures = normalizeFeatures(features, tier);
        int effectiveGrace = graceHoursOverride != null && graceHoursOverride >= 0
                ? graceHoursOverride
                : effectiveGraceHours();
        LocalDateTime effectiveValidUntil = validoAte != null ? validoAte : now.plusDays(validityDays);
        String resolvedStatus = status == null || status.isBlank()
                ? "ATIVO"
                : status.trim().toUpperCase(Locale.ROOT);

        Optional<EmpresaPlanoLicenca> existingOpt = empresaPlanoLicencaRepository.findByEmpresaId(empresaId);
        EmpresaPlanoLicenca plano = existingOpt.orElseGet(EmpresaPlanoLicenca::new);

        String fallbackCode = existingOpt.map(EmpresaPlanoLicenca::getTenantCode).orElse("tenant-" + empresaId);
        String fallbackName = existingOpt.map(EmpresaPlanoLicenca::getTenantName)
                .orElseGet(() -> resolveTenantName(empresaId, "Empresa " + empresaId));
        String resolvedTenantCode = trim(firstNonBlank(tenantCode, fallbackCode), 64);
        String resolvedTenantName = trim(firstNonBlank(tenantName, fallbackName), 120);

        plano.setEmpresaId(empresaId);
        plano.setTenantCode(resolvedTenantCode);
        plano.setTenantName(resolvedTenantName);
        plano.setSubscriptionTier(tier);
        plano.setFeaturesHabilitadas(String.join(",", normalizedFeatures));
        plano.setStatus(resolvedStatus);
        plano.setValidoAte(effectiveValidUntil);
        plano.setUltimaValidacaoEm(now);
        plano.setGracePeriodHoras(effectiveGrace);
        plano.setContingenciaAte(now.plusHours(effectiveGrace));
        plano.setAtualizadoPor(trim(firstNonBlank(atualizadoPor, "SYSADMIN"), 80));

        EmpresaPlanoLicenca saved = empresaPlanoLicencaRepository.save(plano);
        return toEntitlementDto(saved);
    }

    public List<String> parseFeaturesCsv(String csv, String subscriptionTier) {
        if (csv == null || csv.isBlank()) {
            return defaultFeaturesForTier(normalizeTier(subscriptionTier));
        }
        Set<String> parsed = Arrays.stream(csv.split("[+,]"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(this::normalizeFeatureCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return parsed.isEmpty() ? defaultFeaturesForTier(normalizeTier(subscriptionTier)) : List.copyOf(parsed);
    }

    public TenantEntitlementResponseDTO toEntitlementDto(EmpresaPlanoLicenca plano) {
        String status = plano.getStatus() == null ? "ATIVO" : plano.getStatus().trim().toUpperCase(Locale.ROOT);
        boolean inGrace = "CONTINGENCIA".equals(status) || "GRACE_PERIOD".equals(status);
        return new TenantEntitlementResponseDTO(
                plano.getEmpresaId(),
                plano.getTenantCode(),
                plano.getTenantName(),
                plano.getSubscriptionTier(),
                parseFeaturesCsv(plano.getFeaturesHabilitadas(), plano.getSubscriptionTier()),
                status,
                plano.getValidoAte(),
                plano.getUltimaValidacaoEm(),
                plano.getContingenciaAte(),
                plano.getGracePeriodHoras(),
                inGrace);
    }

    private void syncTenantEntitlement(
            Long empresaId,
            String tenantCode,
            LicensePlan plan,
            String licenseHash,
            LocalDateTime validUntil,
            LocalDateTime now) {
        if (empresaId == null || empresaPlanoLicencaRepository == null) {
            return;
        }
        Optional<EmpresaPlanoLicenca> existingOpt = empresaPlanoLicencaRepository.findByEmpresaId(empresaId);
        if (existingOpt.isPresent()) {
            EmpresaPlanoLicenca existing = existingOpt.get();
            String currentStatus = existing.getStatus() == null
                    ? "ATIVO"
                    : existing.getStatus().trim().toUpperCase(Locale.ROOT);
            if (HARD_BLOCKED_STATUSES.contains(currentStatus)) {
                return;
            }
            if (existing.getLicenseKeyHash() != null && !existing.getLicenseKeyHash().isBlank()
                    && !existing.getLicenseKeyHash().equals(licenseHash)) {
                return;
            }
        }
        String resolvedTenantCode = trim(firstNonBlank(tenantCode, "tenant-" + empresaId), 64);
        String resolvedTenantName = resolveTenantName(empresaId, resolvedTenantCode);
        int grace = effectiveGraceHours();

        EmpresaPlanoLicenca plano = existingOpt.orElseGet(EmpresaPlanoLicenca::new);
        plano.setEmpresaId(empresaId);
        plano.setTenantCode(resolvedTenantCode);
        plano.setTenantName(resolvedTenantName);
        plano.setSubscriptionTier(plan.subscriptionTier());
        plano.setFeaturesHabilitadas(String.join(",", plan.features()));
        plano.setLicenseKeyHash(licenseHash);
        plano.setStatus("ATIVO");
        plano.setValidoAte(validUntil);
        plano.setUltimaValidacaoEm(now);
        plano.setGracePeriodHoras(grace);
        plano.setContingenciaAte(now.plusHours(grace));
        plano.setAtualizadoPor("LICENSE_VALIDATE");
        empresaPlanoLicencaRepository.save(plano);
    }

    private String resolveTenantCode(String requestTenantCode, LicensePlan plan, Long resolvedEmpresaId) {
        String candidate = firstNonBlank(requestTenantCode, plan.tenantCode());
        if (candidate != null) {
            return trim(candidate, 64);
        }
        return resolvedEmpresaId == null ? null : "tenant-" + resolvedEmpresaId;
    }

    private String resolveTenantName(Long empresaId, String fallback) {
        if (empresaId != null && empresaRepository != null) {
            Optional<Empresa> empresaOpt = empresaRepository.findById(empresaId);
            if (empresaOpt.isPresent()) {
                Empresa emp = empresaOpt.get();
                return trim(firstNonBlank(emp.getNomeFantasia(), emp.getRazaoSocial(), fallback), 120);
            }
        }
        return trim(fallback, 120);
    }

    private LicenseValidationResponseDTO response(
            boolean valid,
            String status,
            String message,
            LicenseActivation activation,
            String licenseHash,
            LicensePlan plan,
            LocalDateTime now) {
        long activeDevices = activeActivations(licenseHash, now).stream()
                .map(LicenseActivation::getDeviceHash)
                .distinct()
                .count();
        long remaining = Math.max(0, plan.maxDevices() - activeDevices);

        String tenantCode = firstNonBlank(activation.getTenantCode(), plan.tenantCode());
        String tenantName = activation.getEmpresaId() != null
                ? resolveTenantName(activation.getEmpresaId(), tenantCode)
                : tenantCode;
        String subscriptionTier = plan.subscriptionTier();
        List<String> features = plan.features();
        LocalDateTime contingenciaAte = now.plusHours(effectiveGraceHours());

        if (activation.getEmpresaId() != null && empresaPlanoLicencaRepository != null) {
            Optional<EmpresaPlanoLicenca> persistedOpt = empresaPlanoLicencaRepository.findByEmpresaId(activation.getEmpresaId());
            if (persistedOpt.isPresent()) {
                EmpresaPlanoLicenca persisted = persistedOpt.get();
                tenantCode = firstNonBlank(persisted.getTenantCode(), tenantCode);
                tenantName = firstNonBlank(persisted.getTenantName(), tenantName);
                subscriptionTier = firstNonBlank(persisted.getSubscriptionTier(), subscriptionTier);
                features = parseFeaturesCsv(persisted.getFeaturesHabilitadas(), subscriptionTier);
                if (persisted.getContingenciaAte() != null) {
                    contingenciaAte = persisted.getContingenciaAte();
                }
            }
        }

        TenantBrandingDTO branding = tenantName == null
                ? null
                : new TenantBrandingDTO("#0F172A", "#2563EB", null, tenantName);

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
                tenantCode,
                tenantName,
                subscriptionTier,
                features,
                branding,
                contingenciaAte,
                false);
    }

    private LicenseValidationResponseDTO invalid(String status, String message, String deviceHash, String appId) {
        return invalid(status, message, deviceHash, appId, null);
    }

    private LicenseValidationResponseDTO invalid(
            String status,
            String message,
            String deviceHash,
            String appId,
            LicensePlan plan) {
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
                plan == null ? null : plan.tenantCode(),
                null,
                plan == null ? null : plan.subscriptionTier(),
                plan == null ? List.of() : plan.features(),
                null,
                null,
                false);
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
            return Optional.of(new LicensePlan(
                    normalizedKey,
                    "LEGACY_ALL",
                    ALL_APPS,
                    maxDevicesPerKey,
                    validityDays,
                    TIER_ESSENTIAL,
                    ESSENTIAL_DEFAULT_FEATURES,
                    null,
                    null,
                    false));
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

        String tier = parts.length >= 5 && parts[4] != null && !parts[4].isBlank()
                ? normalizeTier(parts[4])
                : TIER_ESSENTIAL;
        List<String> features = parts.length >= 6 && parts[5] != null && !parts[5].isBlank()
                ? parseFeaturesCsv(parts[5], tier)
                : defaultFeaturesForTier(tier);
        String tenantCode = parts.length >= 7 ? normalizeRequired(parts[6]) : null;
        Long empresaId = parts.length >= 8 ? parseNullableLong(parts[7]) : null;

        return Optional.of(new LicensePlan(key, planName, apps, devices, days, tier, features, tenantCode, empresaId, true));
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

    private String normalizeTier(String rawTier) {
        if (rawTier == null || rawTier.isBlank()) {
            return TIER_ESSENTIAL;
        }
        String normalized = rawTier.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case TIER_ESSENTIAL, TIER_GROWTH, TIER_PREMIUM -> normalized;
            default -> throw new IllegalArgumentException("Tier de assinatura invalido: " + rawTier);
        };
    }

    private List<String> defaultFeaturesForTier(String tier) {
        return switch (tier) {
            case TIER_PREMIUM -> PREMIUM_DEFAULT_FEATURES;
            case TIER_GROWTH -> GROWTH_DEFAULT_FEATURES;
            default -> ESSENTIAL_DEFAULT_FEATURES;
        };
    }

    private List<String> normalizeFeatures(List<String> features, String tier) {
        if (features == null || features.isEmpty()) {
            return defaultFeaturesForTier(tier);
        }
        Set<String> normalized = features.stream()
                .filter(f -> f != null && !f.isBlank())
                .map(this::normalizeFeatureCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return normalized.isEmpty() ? defaultFeaturesForTier(tier) : List.copyOf(normalized);
    }

    private String normalizeFeatureCode(String code) {
        return ApexFeature.fromCode(code).name();
    }

    private int effectiveGraceHours() {
        return Math.max(0, gracePeriodHours);
    }

    private long parseLong(String value, long fallback) {
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private Long parseNullableLong(String value) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }
            long parsed = Long.parseLong(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (RuntimeException ignored) {
            return null;
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
        if (normalizedPlatform.contains("win")
                || normalizedPlatform.contains("darwin")
                || normalizedPlatform.contains("linux")
                || normalizedPlatform.contains("electron")) {
            return "desktop";
        }
        if (normalizedPlatform.contains("capacitor")
                || normalizedPlatform.contains("ionic")
                || normalizedPlatform.contains("android")
                || normalizedPlatform.contains("ios")) {
            return "mobile-staff";
        }
        return "web-client";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String val : values) {
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
        }
        return null;
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

    private record LicensePlan(
            String key,
            String name,
            List<String> allowedApps,
            long maxDevices,
            long validityDays,
            String subscriptionTier,
            List<String> features,
            String tenantCode,
            Long empresaId,
            boolean fromCatalog) {
        private boolean allows(String appId) {
            return allowedApps.contains(appId);
        }
    }
}
