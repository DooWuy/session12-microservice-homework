package email.emailservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service

public class OrderEventConsumer {

    @KafkaListener(topics = "order-events", groupId = "email-service-group")
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Email Service received OrderCreatedEvent: {}", event);

        // Gửi email
        sendEmailNotification(event);
    }

    private void sendEmailNotification(OrderCreatedEvent event) {
        log.info("Sending email to: {} for order: {}", event.getEmail(), event.getOrderId());
        // Logic gửi email
    }
}
