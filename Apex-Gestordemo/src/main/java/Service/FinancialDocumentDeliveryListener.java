package Service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "apex.messaging.rabbit.enabled", havingValue = "true")
public class FinancialDocumentDeliveryListener {

    private final FinancialDocumentDeliveryService deliveryService;

    public FinancialDocumentDeliveryListener(FinancialDocumentDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @RabbitListener(queues = "${apex.messaging.rabbit.document-queue}")
    public void onSignedDocumentQueued(Long outboxId) {
        deliveryService.deliver(outboxId);
    }
}
