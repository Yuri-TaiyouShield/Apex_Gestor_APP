package Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Privacy_Request")
@Data
public class PrivacyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_privacy_request")
    private Long idPrivacyRequest;

    @Column(nullable = false, unique = true, length = 40)
    private String protocolo;

    @Column(name = "titular_id", nullable = false, length = 80)
    private String titularId;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "solicitado_em", nullable = false)
    private LocalDateTime solicitadoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    @Column(name = "solicitante_login", length = 80)
    private String solicitanteLogin;
}
