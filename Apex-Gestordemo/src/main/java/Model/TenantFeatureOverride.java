package Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_feature_override",
        uniqueConstraints = @UniqueConstraint(name = "unq_tenant_feature_override", columnNames = {"tenant_id", "feature_id"}),
        indexes = @Index(name = "idx_tenant_feature_override_tenant", columnList = "tenant_id"))
@Getter
@Setter
public class TenantFeatureOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tenant_feature_override")
    private Long idTenantFeatureOverride;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "feature_id", nullable = false)
    private FeatureToggle featureToggle;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(length = 255)
    private String reason;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
