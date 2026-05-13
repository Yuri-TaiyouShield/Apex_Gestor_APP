package Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Data
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_audit_log")
    private Long idAuditLog;

    @Column(name = "ator_login", length = 80)
    private String atorLogin;

    @Column(nullable = false, length = 20)
    private String metodo;

    @Column(nullable = false, length = 255)
    private String recurso;

    @Column(nullable = false)
    private int status;

    @Column(name = "ip_hash", length = 128)
    private String ipHash;

    @Column(name = "user_agent_hash", length = 128)
    private String userAgentHash;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
