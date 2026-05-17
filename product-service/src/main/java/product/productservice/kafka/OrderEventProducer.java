package product.productservice.kafka;

import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;

public class OrderEventProducer {
    reatedEvent(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent: {}", event);

        Message<OrderCreatedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, orderTopic)
                .build();

        kafkaTemplate.send(message)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully sent OrderCreatedEvent with id: {}", event.getOrderId());
                    } else {
                        log.error("Failed to send OrderCreatedEvent: {}", event, ex);
                    }
                });


}
