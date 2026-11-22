package Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "centro_custo", uniqueConstraints = {
        @UniqueConstraint(name = "uk_centro_custo_tenant_codigo", columnNames = {"empresa_id", "filial_id", "codigo"})
}, indexes = {
        @Index(name = "idx_centro_custo_tenant_ativo", columnList = "empresa_id, filial_id, ativo")
})
@Data
public class CentroCusto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_centro_custo")
    private Long idCentroCusto;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "filial_id", nullable = false)
    private Long filialId;

    @Column(nullable = false, length = 32)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacional", nullable = false, length = 32)
    private TipoCentroCusto tipoOperacional;

    @Column(name = "responsavel_usuario_id")
    private Long responsavelUsuarioId;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = now;
        }
        if (atualizadoEm == null) {
            atualizadoEm = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }
}
