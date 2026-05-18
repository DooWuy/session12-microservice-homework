package shipping.shippingservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import shipping.shippingservice.event.ShippingStatusEvent;
@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingEventProducer {

    @Value("${kafka.topic.shipping:shipping-events}")
    private String shippingTopic;

    private final KafkaTemplate<String, ShippingStatusEvent> kafkaTemplate;

    public void publishShippingStatusEvent(ShippingStatusEvent event) {
        log.info("Publishing ShippingStatusEvent: orderId={}, status={}",
                event.getOrderId(), event.getStatus());

        Message<ShippingStatusEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, shippingTopic)
                .setHeader("orderId", event.getOrderId().toString())
                .build();

        kafkaTemplate.send(message)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published shipping event for order: {}",
                                event.getOrderId());
                    } else {
                        log.error("Failed to publish shipping event for order: {}",
                                event.getOrderId(), ex);
                    }
                });
    }
}
