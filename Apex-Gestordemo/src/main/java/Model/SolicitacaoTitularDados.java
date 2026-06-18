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
@Table(name = "Solicitacao_Titular_Dados")
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

    @Column(name = "executado_por", length = 80)
    private String executadoPor;

    @Column(name = "solicitado_em", nullable = false)
    private LocalDateTime solicitadoEm;

    @Column(name = "executado_em")
    private LocalDateTime executadoEm;

    private Long empresaId;
    private Long filialId;

    @PrePersist
    public void prePersist() {
        if (solicitadoEm == null) {
            solicitadoEm = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "RECEBIDA";
        }
    }
}
