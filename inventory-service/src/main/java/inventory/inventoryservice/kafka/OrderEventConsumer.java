package inventory.inventoryservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service

public class OrderEventConsumer {

    @KafkaListener(topics = "order-events", groupId = "inventory-service-group")
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Inventory Service received OrderCreatedEvent: {}", event);

        // Trừ kho
        decreaseInventory(event);
    }

    private void decreaseInventory(OrderCreatedEvent event) {
        log.info("Decreasing inventory for product: {} by quantity: {}",
                event.getProductName(), event.getQuantity());
        // Logic trừ kho
    }

}
