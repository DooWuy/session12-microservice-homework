package product.productservice.event;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleOrderEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderId;           // Unique ID sinh ra bởi Product-Service
    private Long flashSaleId;         // ID của chương trình flash sale
    private Long productId;
    private String productName;
    private Integer quantity;
    private Double price;             // Giá Flashsale
    private Double totalPrice;
    private Long customerId;
    private String customerEmail;
    private String paymentMethod;
    private LocalDateTime createdAt;
}
