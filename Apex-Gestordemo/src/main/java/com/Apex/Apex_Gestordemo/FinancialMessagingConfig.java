package com.Apex.Apex_Gestordemo;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "apex.messaging.rabbit.enabled", havingValue = "true")
public class FinancialMessagingConfig {

    @Bean
    DirectExchange apexEventsExchange(@Value("${apex.messaging.rabbit.exchange}") String exchange) {
        return new DirectExchange(exchange, true, false);
    }

    @Bean
    Queue financialDocumentQueue(@Value("${apex.messaging.rabbit.document-queue}") String queue) {
        return new Queue(queue, true);
    }

    @Bean
    Binding financialDocumentBinding(
            DirectExchange apexEventsExchange,
            Queue financialDocumentQueue,
            @Value("${apex.messaging.rabbit.document-routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(financialDocumentQueue).to(apexEventsExchange).with(routingKey);
    }
}
