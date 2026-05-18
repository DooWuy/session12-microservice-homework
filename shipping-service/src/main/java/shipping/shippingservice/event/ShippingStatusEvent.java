package shipping.shippingservice.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ShippingStatusEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long shippingId;
    private Long orderId;
    private Long customerId;
    private String customerEmail;
    private String customerPhone;
    private String status;  // "PENDING", "IN_TRANSIT", "DELIVERED", "FAILED"
    private String address;
    private LocalDateTime updatedAt;
    private String notes;


}
