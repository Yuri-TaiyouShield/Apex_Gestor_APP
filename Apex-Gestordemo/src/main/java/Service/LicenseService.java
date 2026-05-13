package Service;

import DTO.LicenseValidationRequestDTO;
import DTO.LicenseValidationResponseDTO;
import Model.LicenseActivation;
import Repository.LicenseActivationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LicenseService {

    private static final String ACTIVE = "ACTIVE";

    private final LicenseActivationRepository licenseActivationRepository;
    private final DataProtectionService dataProtectionService;

    @Value("${apex.license.allowed-keys:APEX-DEMO-2026}")
    private String allowedKeys;

    @Value("${apex.license.max-devices-per-key:3}")
    private long maxDevicesPerKey;

    @Value("${apex.license.validity-days:30}")
    private long validityDays;

    public LicenseService(
            LicenseActivationRepository licenseActivationRepository,
            DataProtectionService dataProtectionService
    ) {
        this.licenseActivationRepository = licenseActivationRepository;
        this.dataProtectionService = dataProtectionService;
    }

    @Transactional
    public LicenseValidationResponseDTO validate(LicenseValidationRequestDTO request) {
        String normalizedKey = request.licenseKey().trim();
        String licenseHash = dataProtectionService.hash(normalizedKey);
        String deviceHash = dataProtectionService.hash(request.deviceFingerprint());
        LocalDateTime now = LocalDateTime.now();

        LicenseActivation activation = licenseActivationRepository
                .findByLicenseKeyHashAndDeviceHash(licenseHash, deviceHash)
                .orElse(null);

        if (activation != null && ACTIVE.equals(activation.getStatus()) && activation.getValidUntil().isAfter(now)) {
            activation.setLastValidatedAt(now);
            activation.setAppVersion(trim(request.appVersion(), 40));
            licenseActivationRepository.save(activation);
            return response(true, ACTIVE, "Licenca valida para este dispositivo.", activation, licenseHash);
        }

        if (!allowedKeySet().contains(normalizedKey)) {
            return new LicenseValidationResponseDTO(false, "INVALID", "Licenca nao autorizada.", null, null, shortHash(deviceHash));
        }

        long activeDevices = licenseActivationRepository.countByLicenseKeyHashAndStatus(licenseHash, ACTIVE);
        if (activation == null && activeDevices >= maxDevicesPerKey) {
            return new LicenseValidationResponseDTO(false, "LIMIT_REACHED", "Limite de dispositivos atingido para esta licenca.", null, 0L, shortHash(deviceHash));
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
        activation.setStatus(ACTIVE);
        activation.setLastValidatedAt(now);
        activation.setValidUntil(now.plusDays(validityDays));
        licenseActivationRepository.save(activation);

        return response(true, ACTIVE, "Licenca ativada com sucesso.", activation, licenseHash);
    }

    private LicenseValidationResponseDTO response(boolean valid, String status, String message, LicenseActivation activation, String licenseHash) {
        long activeDevices = licenseActivationRepository.countByLicenseKeyHashAndStatus(licenseHash, ACTIVE);
        long remaining = Math.max(0, maxDevicesPerKey - activeDevices);
        return new LicenseValidationResponseDTO(valid, status, message, activation.getValidUntil(), remaining, shortHash(activation.getDeviceHash()));
    }

    private Set<String> allowedKeySet() {
        return Arrays.stream(allowedKeys.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
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
}
