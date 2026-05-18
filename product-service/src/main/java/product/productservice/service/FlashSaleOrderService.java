package product.productservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import product.productservice.Repository.ProductRepository;
import product.productservice.entity.Product;
import product.productservice.event.FlashSaleOrderEvent;

import java.time.LocalDateTime;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleOrderService {

    private final FlashSaleCacheService flashSaleCacheService;
    private final RedisLockService redisLockService;
    private final FlashSaleEventProducer flashSaleEventProducer;
    private final ProductRepository productRepository;

    @Value("${lock.flashsale.timeout:3000}")
    private long lockTimeout;

    @Value("${lock.flashsale.lease-time:5000}")
    private long lockLeaseTime;


    public FlashSaleOrderResponse buyNow(FlashSaleOrderRequest request) {
        log.info("Flash sale buy now request: {}", request);

        String lockKey = "lock:flashsale:" + request.getFlashSaleId();

        try {

            return redisLockService.executeWithLock(
                    lockKey,
                    lockTimeout,
                    lockLeaseTime,
                    () -> processBuyNow(request)
            );
        } catch (Exception e) {
            log.error("Error processing flash sale order", e);
            return FlashSaleOrderResponse.builder()
                    .status("FAILED")
                    .message(e.getMessage())
                    .build();
        }
    }


    private FlashSaleOrderResponse processBuyNow(FlashSaleOrderRequest request) {
        // 1. Kiểm tra tồn kho
        if (!flashSaleCacheService.hasStock(request.getFlashSaleId())) {
            log.warn("Out of stock for flash sale: {}", request.getFlashSaleId());
            throw new RuntimeException("Sản phẩm flash sale đã hết hàng");
        }

        Integer availableQty = flashSaleCacheService.getFlashSaleQuantity(request.getFlashSaleId());
        if (availableQty < request.getQuantity()) {
            log.warn("Insufficient stock: available={}, requested={}",
                    availableQty, request.getQuantity());
            throw new RuntimeException(
                    String.format("Số lượng không đủ. Còn lại: %d", availableQty)
            );
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));

        Double flashSalePrice = flashSaleCacheService.getFlashSalePrice(request.getFlashSaleId());

        Boolean decremented = flashSaleCacheService.decrementQuantity(
                request.getFlashSaleId(),
                request.getQuantity()
        );

        if (!decremented) {
            log.error("Failed to decrement quantity");
            throw new RuntimeException("Lỗi khi xử lý đơn hàng");
        }

        String orderId = UUID.randomUUID().toString();
        FlashSaleOrderEvent event = FlashSaleOrderEvent.builder()
                .orderId(orderId)
                .flashSaleId(request.getFlashSaleId())
                .productId(request.getProductId())
                .productName(product.getName())
                .quantity(request.getQuantity())
                .price(flashSalePrice)
                .totalPrice(flashSalePrice * request.getQuantity())
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .paymentMethod(request.getPaymentMethod())
                .createdAt(LocalDateTime.now())
                .build();

        flashSaleEventProducer.publishFlashSaleOrderEvent(event);

        log.info("Flash sale order created successfully: orderId={}", orderId);

        return FlashSaleOrderResponse.builder()
                .orderId(orderId)
                .flashSaleId(request.getFlashSaleId())
                .productId(request.getProductId())
                .productName(product.getName())
                .quantity(request.getQuantity())
                .price(flashSalePrice)
                .totalPrice(event.getTotalPrice())
                .status("SUCCESS")
                .message("Đặt hàng thành công. Chúng tôi đang xử lý đơn hàng của bạn...")
                .build();
    }
}
