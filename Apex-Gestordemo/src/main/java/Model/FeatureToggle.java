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

@Entity
@Table(name = "feature_toggle",
        uniqueConstraints = @UniqueConstraint(name = "unq_feature_key", columnNames = "feature_key"),
        indexes = @Index(name = "idx_feature_status", columnList = "status"))
@Getter
@Setter
public class FeatureToggle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_feature_toggle")
    private Long idFeatureToggle;

    @Column(name = "feature_key", nullable = false, length = 80)
    private String featureKey;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(length = 255)
    private String description;

    @Column(name = "minimum_plan_code", nullable = false, length = 40)
    private String minimumPlanCode;

    @Column(nullable = false, length = 20)
    private String status;
}
