package Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "solicitacao_titular_dados")
@Data
public class SolicitacaoTitularDados {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitacao")
    private Long idSolicitacao;

    @Column(name = "tipo_titular", nullable = false, length = 40)
    private String tipoTitular;

    @Column(name = "titular_id", nullable = false, length = 80)
    private String titularId;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 500)
    private String justificativa;

    @Column(name = "solicitado_por", length = 80)
    private String solicitadoPor;

    @Column(name = "solicitado_por_usuario_id")
    private Long solicitadoPorUsuarioId;

    @Column(name = "aprovado_por_usuario_id")
    private Long aprovadoPorUsuarioId;

    @Column(name = "aprovado_por", length = 80)
    private String aprovadoPor;

    @Column(name = "aprovado_em")
    private LocalDateTime aprovadoEm;

    @Column(name = "executado_por", length = 80)
    private String executadoPor;

    @Column(name = "executado_por_usuario_id")
    private Long executadoPorUsuarioId;

    @Column(name = "solicitado_em", nullable = false)
    private LocalDateTime solicitadoEm;

    @Column(name = "executado_em")
    private LocalDateTime executadoEm;

    @Column(name = "retencao_legal_validada", nullable = false)
    private boolean retencaoLegalValidada = false;

    @Column(name = "impedimento_retencao", length = 500)
    private String impedimentoRetencao;

    @Column(name = "simulacao_hash", length = 128)
    private String simulacaoHash;

    @Column(name = "confirmado_explicito", nullable = false)
    private boolean confirmadoExplicito = false;

    private Long empresaId;
    private Long filialId;

    @jakarta.persistence.Version
    @Column(nullable = false)
    private Long version = 0L;

    @PrePersist
    public void prePersist() {
        if (solicitadoEm == null) {
            solicitadoEm = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "RECEBIDA";
        }
        if (version == null) {
            version = 0L;
        }
    }
}
