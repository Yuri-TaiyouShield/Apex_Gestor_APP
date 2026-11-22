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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "politica_segregacao_aprovacao",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_politica_sod_tenant_dom", columnNames = {"empresa_id", "filial_id", "dominio_operacao"})
        },
        indexes = {
                @Index(name = "idx_politica_sod_tenant", columnList = "empresa_id, filial_id")
        })
@Data
public class PoliticaSegregacaoAprovacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_politica_sod")
    private Long idPoliticaSod;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "filial_id", nullable = false)
    private Long filialId;

    @Column(name = "dominio_operacao", nullable = false, length = 48)
    private String dominioOperacao;

    @Column(name = "exigir_aprovador_distinto", nullable = false)
    private boolean exigirAprovadorDistinto = true;

    @Column(name = "permitir_excecao_unipessoal", nullable = false)
    private boolean permitirExcecaoUnipessoal = false;

    @Column(name = "exigir_justificativa", nullable = false)
    private boolean exigirJustificativa = true;

    @Column(name = "alcada_gerente_maximo", precision = 15, scale = 2)
    private BigDecimal alcadaGerenteMaximo;

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
        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }
}
