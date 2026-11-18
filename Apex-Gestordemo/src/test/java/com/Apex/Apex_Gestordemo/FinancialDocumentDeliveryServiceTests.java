package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import Model.FinancialDigitalDocument;
import Model.FinancialDocumentDeliveryOutbox;
import Repository.FinancialDigitalDocumentRepository;
import Repository.FinancialDocumentDeliveryOutboxRepository;
import Service.DataProtectionService;
import Service.FinancialDocumentDeliveryService;
import Service.FinancialDocumentMessagingService;
import Service.FinancialDocumentStateMachine;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

class FinancialDocumentDeliveryServiceTests {

    private final FinancialDocumentDeliveryOutboxRepository outboxRepository = mock();
    private final FinancialDigitalDocumentRepository documentRepository = mock();
    private final FinancialDocumentMessagingService messagingService = mock();
    private final ObjectProvider<org.springframework.amqp.rabbit.core.RabbitTemplate> rabbitProvider = mock();
    private FinancialDocumentDeliveryService deliveryService;
    private FinancialDocumentDeliveryOutbox outbox;
    private FinancialDigitalDocument document;

    @BeforeEach
    void setUp() {
        deliveryService = new FinancialDocumentDeliveryService(outboxRepository, documentRepository, messagingService,
                new FinancialDocumentStateMachine(), new DataProtectionService(), rabbitProvider, false,
                "exchange", "routing-key", 2, 1, 10);
        document = new FinancialDigitalDocument();
        document.setStatus("SIGNED");
        document.setConteudo("conteudo");
        outbox = new FinancialDocumentDeliveryOutbox();
        ReflectionTestUtils.setField(outbox, "idFinancialDocumentOutbox", 99L);
        outbox.setDocument(document);
        outbox.setStatus("QUEUED");
        outbox.setAttempts(0);
        outbox.setQueuedAt(LocalDateTime.now());
        when(outboxRepository.findById(99L)).thenReturn(Optional.of(outbox));
    }

    @Test
    void envioConfirmadoMarcaDocumentoEOutboxComoSent() {
        when(messagingService.sendSignedDocument(document))
                .thenReturn(FinancialDocumentMessagingService.DeliveryResult.SENT);

        deliveryService.deliver(99L);

        assertThat(outbox.getStatus()).isEqualTo("SENT");
        assertThat(document.getStatus()).isEqualTo("SENT");
        assertThat(outbox.getSentAt()).isNotNull();
        verify(documentRepository).save(document);
        verify(outboxRepository, org.mockito.Mockito.atLeastOnce()).save(outbox);
    }

    @Test
    void enfileiramentoSemRabbitNaoEntregaAntesDoCommit() {
        when(outboxRepository.save(any(FinancialDocumentDeliveryOutbox.class))).thenReturn(outbox);

        FinancialDocumentDeliveryOutbox saved = deliveryService.enqueueSignedDocument(document, "operador");

        assertThat(saved.getStatus()).isEqualTo("QUEUED");
        verifyNoInteractions(messagingService, documentRepository);
    }

    @Test
    void entregaSemCanalConfiguradoFicaSkippedSemSimularEnvio() {
        when(messagingService.sendSignedDocument(document))
                .thenReturn(FinancialDocumentMessagingService.DeliveryResult.SKIPPED);

        deliveryService.deliver(99L);

        assertThat(outbox.getStatus()).isEqualTo("SKIPPED");
        assertThat(outbox.getNextAttemptAt()).isNull();
        assertThat(document.getStatus()).isEqualTo("SIGNED");
        assertThat(document.getEnviadoEm()).isNull();
    }

    @Test
    void falhaTransitoriaAgendaRetryEExaustaVaiParaDeadLetter() {
        when(messagingService.sendSignedDocument(document)).thenThrow(new IllegalStateException("falha temporaria"));

        deliveryService.deliver(99L);
        assertThat(outbox.getStatus()).isEqualTo("FAILED");
        assertThat(outbox.getAttempts()).isEqualTo(1);
        assertThat(outbox.getNextAttemptAt()).isAfter(LocalDateTime.now());
        assertThat(outbox.getLastError()).isEqualTo("falha temporaria");

        outbox.setNextAttemptAt(LocalDateTime.now());
        deliveryService.deliver(99L);
        assertThat(outbox.getStatus()).isEqualTo("DEAD_LETTER");
        assertThat(outbox.getAttempts()).isEqualTo(2);
        assertThat(outbox.getNextAttemptAt()).isNull();
        assertThat(outbox.getDeadLetteredAt()).isNotNull();
    }
}
