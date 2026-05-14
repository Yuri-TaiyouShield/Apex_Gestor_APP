package Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "license_activation",
        uniqueConstraints = @UniqueConstraint(name = "unq_license_device_app", columnNames = {"license_key_hash", "device_hash", "app_id"}),
        indexes = {
                @Index(name = "idx_license_key_hash", columnList = "license_key_hash"),
                @Index(name = "idx_license_device_hash", columnList = "device_hash"),
                @Index(name = "idx_license_app_status", columnList = "license_key_hash, app_id, status")
        })
@Data
public class LicenseActivation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_license_activation")
    private Long idLicenseActivation;

    @Column(name = "license_key_hash", nullable = false, length = 64)
    private String licenseKeyHash;

    @Column(name = "device_hash", nullable = false, length = 64)
    private String deviceHash;

    @Column(name = "device_label", length = 120)
    private String deviceLabel;

    @Column(length = 40)
    private String platform;

    @Column(name = "app_version", length = 40)
    private String appVersion;

    @Column(name = "app_id", nullable = false, length = 40)
    private String appId;

    @Column(name = "license_plan", nullable = false, length = 40)
    private String licensePlan;

    @Column(name = "licensed_apps", nullable = false, length = 160)
    private String licensedApps;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "activated_at", nullable = false)
    private LocalDateTime activatedAt;

    @Column(name = "last_validated_at", nullable = false)
    private LocalDateTime lastValidatedAt;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;
}
