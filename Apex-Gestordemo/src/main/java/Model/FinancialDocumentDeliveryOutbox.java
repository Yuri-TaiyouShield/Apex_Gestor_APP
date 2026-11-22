package Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Entity
@Audited
@Table(name = "financial_document_outbox")
public class FinancialDocumentDeliveryOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_financial_document_outbox")
    private Long idFinancialDocumentOutbox;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private FinancialDigitalDocument document;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "queued_by", nullable = false, length = 120)
    private String queuedBy;

    @Column(name = "queued_at", nullable = false)
    private LocalDateTime queuedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 255)
    private String lastError;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "dead_lettered_at")
    private LocalDateTime deadLetteredAt;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "filial_id")
    private Long filialId;

    public Long getIdFinancialDocumentOutbox() {
        return idFinancialDocumentOutbox;
    }

    public FinancialDigitalDocument getDocument() {
        return document;
    }

    public void setDocument(FinancialDigitalDocument document) {
        this.document = document;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQueuedBy() {
        return queuedBy;
    }

    public void setQueuedBy(String queuedBy) {
        this.queuedBy = queuedBy;
    }

    public LocalDateTime getQueuedAt() {
        return queuedAt;
    }

    public void setQueuedAt(LocalDateTime queuedAt) {
        this.queuedAt = queuedAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(LocalDateTime nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public LocalDateTime getDeadLetteredAt() {
        return deadLetteredAt;
    }

    public void setDeadLetteredAt(LocalDateTime deadLetteredAt) {
        this.deadLetteredAt = deadLetteredAt;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public Long getFilialId() {
        return filialId;
    }

    public void setFilialId(Long filialId) {
        this.filialId = filialId;
    }
}
