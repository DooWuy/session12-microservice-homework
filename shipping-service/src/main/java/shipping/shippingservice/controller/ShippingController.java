package shipping.shippingservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import shipping.shippingservice.entity.Shipping;
import shipping.shippingservice.service.ShippingService;

public class ShippingController {

    private final ShippingService shippingService;

    @PutMapping("/{shippingId}/status")
    public ResponseEntity<Shipping> updateStatus(
            @PathVariable Long shippingId,
            @RequestBody UpdateShippingStatusRequest request) {
        log.info("Update shipping status request: {}", request);

        Shipping updated = shippingService.updateShippingStatus(
                shippingId,
                request.getStatus(),
                request.getNotes()
        );

        return ResponseEntity.ok(updated);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Shipping> getByOrderId(@PathVariable Long orderId) {
        Shipping shipping = shippingService.getShippingByOrderId(orderId);
        return ResponseEntity.ok(shipping);
    }
}
