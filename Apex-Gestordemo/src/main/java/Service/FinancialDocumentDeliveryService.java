package Service;

import Model.FinancialDigitalDocument;
import Model.FinancialDocumentDeliveryOutbox;
import Model.FinancialDocumentStatus;
import Repository.FinancialDigitalDocumentRepository;
import Repository.FinancialDocumentDeliveryOutboxRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
public class FinancialDocumentDeliveryService {

    private static final String STATUS_QUEUED = "QUEUED";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";

    private final FinancialDocumentDeliveryOutboxRepository outboxRepository;
    private final FinancialDigitalDocumentRepository documentRepository;
    private final FinancialDocumentMessagingService messagingService;
    private final FinancialDocumentStateMachine stateMachine;
    private final DataProtectionService dataProtectionService;
    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final boolean rabbitEnabled;
    private final String exchange;
    private final String routingKey;

    public FinancialDocumentDeliveryService(
            FinancialDocumentDeliveryOutboxRepository outboxRepository,
            FinancialDigitalDocumentRepository documentRepository,
            FinancialDocumentMessagingService messagingService,
            FinancialDocumentStateMachine stateMachine,
            DataProtectionService dataProtectionService,
            ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
            @Value("${apex.messaging.rabbit.enabled:false}") boolean rabbitEnabled,
            @Value("${apex.messaging.rabbit.exchange}") String exchange,
            @Value("${apex.messaging.rabbit.document-routing-key}") String routingKey
    ) {
        this.outboxRepository = outboxRepository;
        this.documentRepository = documentRepository;
        this.messagingService = messagingService;
        this.stateMachine = stateMachine;
        this.dataProtectionService = dataProtectionService;
        this.rabbitTemplateProvider = rabbitTemplateProvider;
        this.rabbitEnabled = rabbitEnabled;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Transactional
    public FinancialDocumentDeliveryOutbox enqueueSignedDocument(FinancialDigitalDocument document, String actor) {
        FinancialDocumentDeliveryOutbox outbox = new FinancialDocumentDeliveryOutbox();
        outbox.setDocument(document);
        outbox.setStatus(STATUS_QUEUED);
        outbox.setQueuedBy(actor);
        outbox.setQueuedAt(LocalDateTime.now());
        outbox.setAttempts(0);
        FinancialDocumentDeliveryOutbox saved = outboxRepository.save(outbox);

        if (rabbitEnabled) {
            publishAfterCommit(saved.getIdFinancialDocumentOutbox());
        } else {
            deliver(saved.getIdFinancialDocumentOutbox());
        }
        return saved;
    }

    private void publishAfterCommit(Long outboxId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            publish(outboxId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(outboxId);
            }
        });
    }

    private void publish(Long outboxId) {
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            throw new IllegalStateException("RabbitMQ habilitado, mas RabbitTemplate nao esta disponivel");
        }
        rabbitTemplate.convertAndSend(exchange, routingKey, outboxId);
    }

    @Transactional
    public void deliver(Long outboxId) {
        FinancialDocumentDeliveryOutbox outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalArgumentException("Evento de documento nao encontrado"));
        FinancialDigitalDocument document = outbox.getDocument();
        outbox.setAttempts(outbox.getAttempts() + 1);
        try {
            if (messagingService.sendSignedDocument(document)) {
                document.setStatus(stateMachine.transition(document.getStatus(), FinancialDocumentStatus.SENT));
                document.setEnviadoEm(LocalDateTime.now());
            }
            document.setUltimoErro(null);
            outbox.setStatus(STATUS_SENT);
            outbox.setSentAt(LocalDateTime.now());
            outbox.setLastError(null);
        } catch (RuntimeException ex) {
            String sanitizedError = dataProtectionService.maskSensitiveText(ex.getMessage());
            outbox.setStatus(STATUS_FAILED);
            outbox.setLastError(sanitizedError);
            document.setUltimoErro(sanitizedError);
        }
        documentRepository.save(document);
        outboxRepository.save(outbox);
    }
}
