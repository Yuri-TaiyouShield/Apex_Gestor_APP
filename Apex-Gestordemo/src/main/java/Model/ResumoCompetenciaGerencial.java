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

@Entity
@Table(name = "resumo_competencia_gerencial", uniqueConstraints = {
        @UniqueConstraint(name = "uk_resumo_comp_chave", columnNames = {
                "empresa_id", "filial_id", "competencia_ano_mes", "plano_conta_id", "centro_custo_id"
        })
}, indexes = {
        @Index(name = "idx_resumo_comp_tenant_mes", columnList = "empresa_id, filial_id, competencia_ano_mes")
})
@Data
public class ResumoCompetenciaGerencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resumo_competencia")
    private Long idResumoCompetencia;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "filial_id", nullable = false)
    private Long filialId;

    @Column(name = "competencia_ano_mes", nullable = false, length = 7)
    private String competenciaAnoMes;

    @Column(name = "plano_conta_id", nullable = false)
    private Long planoContaId;

    @Column(name = "centro_custo_id", nullable = false)
    private Long centroCustoId = 0L;

    @Column(name = "valor_competencia", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorCompetencia = BigDecimal.ZERO;

    @Column(name = "valor_realizado_caixa", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorRealizadoCaixa = BigDecimal.ZERO;

    @Column(name = "quantidade_lancamentos", nullable = false)
    private Long quantidadeLancamentos = 0L;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void prePersist() {
        if (centroCustoId == null) {
            centroCustoId = 0L;
        }
        if (valorCompetencia == null) {
            valorCompetencia = BigDecimal.ZERO;
        }
        if (valorRealizadoCaixa == null) {
            valorRealizadoCaixa = BigDecimal.ZERO;
        }
        if (quantidadeLancamentos == null) {
            quantidadeLancamentos = 0L;
        }
        if (atualizadoEm == null) {
            atualizadoEm = LocalDateTime.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        if (centroCustoId == null) {
            centroCustoId = 0L;
        }
        if (valorCompetencia == null) {
            valorCompetencia = BigDecimal.ZERO;
        }
        if (valorRealizadoCaixa == null) {
            valorRealizadoCaixa = BigDecimal.ZERO;
        }
        if (quantidadeLancamentos == null) {
            quantidadeLancamentos = 0L;
        }
        atualizadoEm = LocalDateTime.now();
    }
}
