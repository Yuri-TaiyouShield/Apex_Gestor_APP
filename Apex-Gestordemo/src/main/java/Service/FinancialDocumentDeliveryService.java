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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class FinancialDocumentDeliveryService {

    private static final String STATUS_QUEUED = "QUEUED";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final String STATUS_DEAD_LETTER = "DEAD_LETTER";

    private final FinancialDocumentDeliveryOutboxRepository outboxRepository;
    private final FinancialDigitalDocumentRepository documentRepository;
    private final FinancialDocumentMessagingService messagingService;
    private final FinancialDocumentStateMachine stateMachine;
    private final DataProtectionService dataProtectionService;
    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final boolean rabbitEnabled;
    private final String exchange;
    private final String routingKey;
    private final int maxAttempts;
    private final long baseDelaySeconds;
    private final long maxDelaySeconds;

    public FinancialDocumentDeliveryService(
            FinancialDocumentDeliveryOutboxRepository outboxRepository,
            FinancialDigitalDocumentRepository documentRepository,
            FinancialDocumentMessagingService messagingService,
            FinancialDocumentStateMachine stateMachine,
            DataProtectionService dataProtectionService,
            ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
            @Value("${apex.messaging.rabbit.enabled:false}") boolean rabbitEnabled,
            @Value("${apex.messaging.rabbit.exchange:apex.events}") String exchange,
            @Value("${apex.messaging.rabbit.document-routing-key:financial.document.signed}") String routingKey,
            @Value("${apex.messaging.outbox.max-attempts:5}") int maxAttempts,
            @Value("${apex.messaging.outbox.base-delay-seconds:30}") long baseDelaySeconds,
            @Value("${apex.messaging.outbox.max-delay-seconds:3600}") long maxDelaySeconds
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
        this.maxAttempts = Math.max(1, maxAttempts);
        this.baseDelaySeconds = Math.max(1, baseDelaySeconds);
        this.maxDelaySeconds = Math.max(this.baseDelaySeconds, maxDelaySeconds);
    }

    @Transactional
    public FinancialDocumentDeliveryOutbox enqueueSignedDocument(FinancialDigitalDocument document, String actor) {
        FinancialDocumentDeliveryOutbox outbox = new FinancialDocumentDeliveryOutbox();
        outbox.setDocument(document);
        outbox.setStatus(STATUS_QUEUED);
        outbox.setQueuedBy(actor);
        outbox.setQueuedAt(LocalDateTime.now());
        outbox.setAttempts(0);
        outbox.setNextAttemptAt(outbox.getQueuedAt());
        outbox.setEmpresaId(document.getEmpresaId());
        outbox.setFilialId(document.getFilialId());
        FinancialDocumentDeliveryOutbox saved = outboxRepository.save(outbox);

        if (rabbitEnabled) {
            publishAfterCommit(saved.getIdFinancialDocumentOutbox());
        } else {
            // Sem RabbitMQ, o registro permanece na fila para o poller transacional.
            // Nunca faça entrega externa antes do commit desta transação.
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
        if (STATUS_SENT.equals(outbox.getStatus()) || STATUS_SKIPPED.equals(outbox.getStatus())
                || STATUS_DEAD_LETTER.equals(outbox.getStatus())) {
            return;
        }
        FinancialDigitalDocument document = outbox.getDocument();
        outbox.setAttempts(outbox.getAttempts() + 1);
        outbox.setStatus(STATUS_PROCESSING);
        outboxRepository.save(outbox);
        try {
            FinancialDocumentMessagingService.DeliveryResult result = messagingService.sendSignedDocument(document);
            if (result == FinancialDocumentMessagingService.DeliveryResult.SENT) {
                document.setStatus(stateMachine.transition(document.getStatus(), FinancialDocumentStatus.SENT));
                document.setEnviadoEm(LocalDateTime.now());
                outbox.setStatus(STATUS_SENT);
                outbox.setSentAt(LocalDateTime.now());
            } else {
                outbox.setStatus(STATUS_SKIPPED);
                outbox.setNextAttemptAt(null);
            }
            document.setUltimoErro(null);
            outbox.setLastError(null);
        } catch (RuntimeException ex) {
            String maskedError = dataProtectionService.maskSensitiveText(ex.getMessage());
            String sanitizedError = maskedError != null && maskedError.length() > 255
                    ? maskedError.substring(0, 255)
                    : maskedError;
            boolean deadLetter = outbox.getAttempts() >= maxAttempts;
            outbox.setStatus(deadLetter ? STATUS_DEAD_LETTER : STATUS_FAILED);
            outbox.setLastError(sanitizedError);
            outbox.setNextAttemptAt(deadLetter ? null : nextAttemptAt(outbox.getAttempts()));
            outbox.setDeadLetteredAt(deadLetter ? LocalDateTime.now() : null);
            document.setUltimoErro(sanitizedError);
        }
        documentRepository.save(document);
        outboxRepository.save(outbox);
    }

    @Scheduled(fixedDelayString = "${apex.messaging.outbox.poll-ms:60000}")
    @Transactional
    public void retryDueDeliveries() {
        List<FinancialDocumentDeliveryOutbox> due = outboxRepository
                .findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByQueuedAtAsc(
                        List.of(STATUS_QUEUED, STATUS_FAILED), LocalDateTime.now());
        due.forEach(outbox -> deliver(outbox.getIdFinancialDocumentOutbox()));
    }

    @Transactional
    public void reprocessDeadLetter(Long outboxId) {
        FinancialDocumentDeliveryOutbox outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalArgumentException("Evento de documento nao encontrado"));
        if (!STATUS_DEAD_LETTER.equals(outbox.getStatus())) {
            throw new IllegalArgumentException("Somente eventos em DEAD_LETTER podem ser reprocessados");
        }
        outbox.setStatus(STATUS_QUEUED);
        outbox.setAttempts(0);
        outbox.setDeadLetteredAt(null);
        outbox.setNextAttemptAt(LocalDateTime.now());
        outbox.setLastError(null);
        outboxRepository.save(outbox);
    }

    private LocalDateTime nextAttemptAt(int attempts) {
        long exponential = baseDelaySeconds * (1L << Math.min(attempts - 1, 20));
        long delay = Math.min(maxDelaySeconds, exponential);
        long jitter = ThreadLocalRandom.current().nextLong(Math.max(1, delay / 5 + 1));
        return LocalDateTime.now().plusSeconds(Math.min(maxDelaySeconds, delay + jitter));
    }
}
