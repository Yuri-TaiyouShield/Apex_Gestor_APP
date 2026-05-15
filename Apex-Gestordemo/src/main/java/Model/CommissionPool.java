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
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Audited
@Table(name = "commission_pool", indexes = {
        @Index(name = "idx_commission_pool_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_commission_pool_order", columnList = "tenant_id, order_id")
})
@Getter
@Setter
public class CommissionPool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_commission_pool")
    private Long idCommissionPool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "order_id", length = 80)
    private String orderId;

    @Column(name = "branch_code", nullable = false, length = 60)
    private String branchCode;

    @Column(name = "source_channel", nullable = false, length = 40)
    private String sourceChannel;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "commission_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionBase;

    @Column(name = "pool_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal poolPercent;

    @Column(name = "pool_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal poolAmount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;
}
