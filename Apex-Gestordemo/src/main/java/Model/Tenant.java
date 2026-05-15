package Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant",
        uniqueConstraints = @UniqueConstraint(name = "unq_tenant_code", columnNames = "tenant_code"),
        indexes = {
                @Index(name = "idx_tenant_status", columnList = "status"),
                @Index(name = "idx_tenant_domain", columnList = "domain")
        })
@Getter
@Setter
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tenant")
    private Long idTenant;

    @Column(name = "tenant_code", nullable = false, length = 60)
    private String tenantCode;

    @Column(name = "legal_name", nullable = false, length = 160)
    private String legalName;

    @Column(name = "trade_name", nullable = false, length = 120)
    private String tradeName;

    @Column(length = 160)
    private String domain;

    @Column(name = "support_email", length = 160)
    private String supportEmail;

    @Column(name = "brand_primary_color", nullable = false, length = 12)
    private String brandPrimaryColor;

    @Column(name = "brand_secondary_color", nullable = false, length = 12)
    private String brandSecondaryColor;

    @Column(name = "brand_logo_url", length = 255)
    private String brandLogoUrl;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
