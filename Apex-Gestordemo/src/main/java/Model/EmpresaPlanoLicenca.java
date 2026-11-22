package Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "empresa_plano_licenca",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_emp_plano_licenca_empresa", columnNames = {"empresa_id"}),
                @UniqueConstraint(name = "uk_emp_plano_licenca_tenant", columnNames = {"tenant_code"})
        },
        indexes = {
                @Index(name = "idx_emp_plano_status_val", columnList = "empresa_id, status, valido_ate")
        })
@Data
public class EmpresaPlanoLicenca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empresa_plano_licenca")
    private Long idEmpresaPlanoLicenca;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "tenant_code", nullable = false, length = 64)
    private String tenantCode;

    @Column(name = "tenant_name", nullable = false, length = 120)
    private String tenantName = "Empresa Apex";

    @Column(name = "subscription_tier", nullable = false, length = 32)
    private String subscriptionTier = "ESSENTIAL";

    @Column(name = "features_habilitadas", nullable = false, length = 500)
    private String featuresHabilitadas = "BASIC_PRICING,FINANCIAL_CORE";

    @Column(name = "license_key_hash", length = 64)
    private String licenseKeyHash;

    @Column(nullable = false, length = 24)
    private String status = "ATIVO";

    @Column(name = "valido_ate")
    private LocalDateTime validoAte;

    @Column(name = "ultima_validacao_em")
    private LocalDateTime ultimaValidacaoEm;

    @Column(name = "contingencia_ate")
    private LocalDateTime contingenciaAte;

    @Column(name = "grace_period_horas", nullable = false)
    private Integer gracePeriodHoras = 72;

    @Column(name = "atualizado_por", length = 80)
    private String atualizadoPor;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = now;
        }
        if (atualizadoEm == null) {
            atualizadoEm = now;
        }
        if (gracePeriodHoras == null || gracePeriodHoras <= 0) {
            gracePeriodHoras = 72;
        }
        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }
}
