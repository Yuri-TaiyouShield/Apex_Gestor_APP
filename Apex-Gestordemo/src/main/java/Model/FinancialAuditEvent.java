package Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "financial_audit_event", indexes = {
        @Index(name = "idx_fin_audit_alvo", columnList = "alvo_tipo, alvo_id"),
        @Index(name = "idx_fin_audit_evento_data", columnList = "tipo_evento, criado_em"),
        @Index(name = "idx_fin_audit_ator", columnList = "ator_login")
})
@Data
public class FinancialAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_financial_audit_event")
    private Long idFinancialAuditEvent;

    @Column(name = "tipo_evento", nullable = false, length = 80)
    private String tipoEvento;

    @Column(name = "alvo_tipo", nullable = false, length = 80)
    private String alvoTipo;

    @Column(name = "alvo_id")
    private Long alvoId;

    @Column(name = "ator_login", nullable = false, length = 120)
    private String atorLogin;

    @Column(name = "valor_anterior", length = 255)
    private String valorAnterior;

    @Column(name = "valor_novo", length = 255)
    private String valorNovo;

    @Lob
    @Column(name = "metadados")
    private String metadados;

    @Column(name = "metadados_hash", length = 128)
    private String metadadosHash;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
