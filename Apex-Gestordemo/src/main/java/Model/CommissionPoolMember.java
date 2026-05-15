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

@Entity
@Audited
@Table(name = "commission_pool_member", indexes = {
        @Index(name = "idx_commission_pool_member_pool", columnList = "pool_id"),
        @Index(name = "idx_commission_pool_member_user", columnList = "user_id")
})
@Getter
@Setter
public class CommissionPoolMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_commission_pool_member")
    private Long idCommissionPoolMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @JoinColumn(name = "pool_id", nullable = false)
    private CommissionPool pool;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "member_name", nullable = false, length = 120)
    private String memberName;

    @Column(name = "share_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal sharePercent;

    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;
}
