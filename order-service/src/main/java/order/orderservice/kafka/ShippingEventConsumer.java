package order.orderservice.kafka;

import order.orderservice.entity.Order;
import order.orderservice.repository.OrderRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingEventConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "shipping-events",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeShippingStatusEvent(ShippingStatusEvent event) {
        log.info("Order-Service received ShippingStatusEvent: orderId={}, status={}",
                event.getOrderId(), event.getStatus());

        try {
            if ("DELIVERED".equals(event.getStatus())) {
                Order order = orderRepository.findById(event.getOrderId())
                        .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

                order.setStatus("COMPLETED");
                orderRepository.save(order);

                log.info("Order status updated to COMPLETED: orderId={}", event.getOrderId());
            }

        } catch (Exception e) {
            log.error("Error processing shipping event", e);
        }
    }

}
