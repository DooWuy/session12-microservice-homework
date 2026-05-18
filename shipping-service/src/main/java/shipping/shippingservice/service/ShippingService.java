package shipping.shippingservice.service;

import jakarta.transaction.Transactional;
import shipping.shippingservice.entity.Shipping;
import shipping.shippingservice.event.ShippingStatusEvent;
import shipping.shippingservice.kafka.ShippingEventProducer;
import shipping.shippingservice.repository.ShippingRepository;

public class ShippingService {

    private final ShippingRepository shippingRepository;
    private final ShippingEventProducer shippingEventProducer;

    @Transactional
    public Shipping updateShippingStatus(Long shippingId, String status, String notes) {
        log.info("Updating shipping status: id={}, status={}", shippingId, status);

        Shipping shipping = shippingRepository.findById(shippingId)
                .orElseThrow(() -> new RuntimeException("Shipping not found: " + shippingId));

        shipping.setStatus(status);
        shipping.setNotes(notes);
        Shipping updated = shippingRepository.save(shipping);

        ShippingStatusEvent event = ShippingStatusEvent.builder()
                .shippingId(updated.getId())
                .orderId(updated.getOrderId())
                .customerId(updated.getCustomerId())
                .customerEmail(updated.getCustomerEmail())
                .customerPhone(updated.getCustomerPhone())
                .status(updated.getStatus())
                .address(updated.getAddress())
                .updatedAt(updated.getUpdatedAt())
                .notes(updated.getNotes())
                .build();

        shippingEventProducer.publishShippingStatusEvent(event);

        return updated;
    }

    public Shipping getShippingByOrderId(Long orderId) {
        return shippingRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Shipping not found for order: " + orderId));
    }
}
