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
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_rule", indexes = {
        @Index(name = "idx_pricing_rule_tenant_type", columnList = "tenant_id, rule_type, status"),
        @Index(name = "idx_pricing_rule_effective", columnList = "effective_from, effective_until")
})
@Getter
@Setter
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pricing_rule")
    private Long idPricingRule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "rule_type", nullable = false, length = 40)
    private String ruleType;

    @Column(name = "min_margin_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal minMarginPercent;

    @Column(name = "default_margin_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal defaultMarginPercent;

    @Column(name = "fixed_expense_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal fixedExpensePercent;

    @Column(name = "tax_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal taxPercent;

    @Column(name = "commission_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal commissionPercent;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_until")
    private LocalDateTime effectiveUntil;
}
