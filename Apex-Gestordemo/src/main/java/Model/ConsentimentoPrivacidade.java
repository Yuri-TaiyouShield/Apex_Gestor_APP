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
@Table(name = "Consentimento_Privacidade")
@Data
public class ConsentimentoPrivacidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consentimento")
    private Long idConsentimento;

    @Column(name = "titular_id", nullable = false, length = 80)
    private String titularId;

    @Column(name = "tipo_titular", nullable = false, length = 40)
    private String tipoTitular;

    @Column(name = "documento_hash", length = 128)
    private String documentoHash;

    @Column(nullable = false, length = 120)
    private String finalidade;

    @Column(name = "versao_termo", nullable = false, length = 20)
    private String versaoTermo;

    @Column(nullable = false, length = 40)
    private String origem;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "aceito_em", nullable = false)
    private LocalDateTime aceitoEm;

    @Column(name = "revogado_em")
    private LocalDateTime revogadoEm;

    private Long empresaId;
    private Long filialId;

    @PrePersist
    public void prePersist() {
        if (aceitoEm == null) {
            aceitoEm = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "ATIVO";
        }
    }
}
