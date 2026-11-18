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
@Table(name = "Notificacao")
@Data
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacao")
    private Long idNotificacao;

    @Column(nullable = false, length = 60)
    private String tipo;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String mensagem;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "entidade_tipo", length = 60)
    private String entidadeTipo;

    @Column(name = "entidade_id")
    private Long entidadeId;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "filial_id")
    private Long filialId;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "lido_em")
    private LocalDateTime lidoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "PENDENTE";
        }
    }
}
