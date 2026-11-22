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
@Table(name = "plano_contas_gerencial", uniqueConstraints = {
        @UniqueConstraint(name = "uk_plano_contas_tenant_codigo", columnNames = {"empresa_id", "filial_id", "codigo"})
}, indexes = {
        @Index(name = "idx_plano_contas_tenant_grupo", columnList = "empresa_id, filial_id, grupo_dre, ativo")
})
@Data
public class PlanoContasGerencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plano_conta")
    private Long idPlanoConta;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "filial_id", nullable = false)
    private Long filialId;

    @Column(name = "conta_pai_id")
    private Long contaPaiId;

    @Column(nullable = false, length = 32)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_conta", nullable = false, length = 32)
    private TipoContaGerencial tipoConta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NaturezaContaGerencial natureza;

    @Enumerated(EnumType.STRING)
    @Column(name = "classificacao_custo", nullable = false, length = 24)
    private ClassificacaoCusto classificacaoCusto;

    @Enumerated(EnumType.STRING)
    @Column(name = "grupo_dre", nullable = false, length = 40)
    private GrupoDreGerencial grupoDre;

    @Column(name = "ordem_exibicao", nullable = false)
    private Integer ordemExibicao = 0;

    @Column(name = "aceita_lancamento", nullable = false)
    private boolean aceitaLancamento = true;

    @Column(name = "sistema_padrao", nullable = false)
    private boolean sistemaPadrao = false;

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
        if (ordemExibicao == null) {
            ordemExibicao = 0;
        }
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
        if (ordemExibicao == null) {
            ordemExibicao = 0;
        }
    }
}
