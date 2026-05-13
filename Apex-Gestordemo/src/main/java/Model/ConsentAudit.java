package Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "consent_audit")
@Data
public class ConsentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consent_audit")
    private Long idConsentAudit;

    @Column(name = "titular_id", length = 80)
    private String titularId;

    @Column(name = "tipo_titular", nullable = false, length = 30)
    private String tipoTitular;

    @Column(name = "documento_hash", length = 128)
    private String documentoHash;

    @Column(nullable = false, length = 40)
    private String versao;

    @Column(name = "aceito_em", nullable = false)
    private LocalDateTime aceitoEm;

    @Column(nullable = false, length = 40)
    private String canal;

    @Column(name = "ip_hash", length = 128)
    private String ipHash;

    @Column(name = "user_agent_hash", length = 128)
    private String userAgentHash;
}
