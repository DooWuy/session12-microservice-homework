package product.productservice.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionUpdateEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long productId;
    private Double newPrice;
    private Double discountPercentage;
    private String promotionName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;


}
