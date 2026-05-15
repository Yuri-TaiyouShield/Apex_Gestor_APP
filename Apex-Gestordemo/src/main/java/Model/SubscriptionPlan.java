package Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "subscription_plan",
        uniqueConstraints = @UniqueConstraint(name = "unq_subscription_plan_code", columnNames = "code"),
        indexes = @Index(name = "idx_subscription_plan_status", columnList = "status"))
@Getter
@Setter
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_subscription_plan")
    private Long idSubscriptionPlan;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(name = "tier_name", nullable = false, length = 80)
    private String tierName;

    @Column(length = 255)
    private String description;

    @Column(name = "monthly_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "max_stores", nullable = false)
    private Integer maxStores;

    @Column(name = "max_users", nullable = false)
    private Integer maxUsers;

    @Column(name = "omnichannel_enabled", nullable = false)
    private Boolean omnichannelEnabled;

    @Column(nullable = false, length = 20)
    private String status;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "plan_feature_toggle",
            joinColumns = @JoinColumn(name = "plan_id"),
            inverseJoinColumns = @JoinColumn(name = "feature_id"))
    private Set<FeatureToggle> features = new LinkedHashSet<>();
}
