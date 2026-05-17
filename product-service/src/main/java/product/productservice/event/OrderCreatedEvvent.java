package product.productservice.event;

import java.io.Serializable;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvvent implements Serializable {

    private Long orderId;
    private Long customerId;
    private String productName;
    private Integer quantity;
    private Double totalPrice;
    private String email;
    private LocalDateTime createdAt;

}
