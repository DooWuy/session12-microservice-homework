package product.productservice.service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.Cacheable;
import jakarta.persistence.criteria.Order;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import product.productservice.Repository.ProductRepository;
import product.productservice.dto.ProductResponse;
import product.productservice.dto.ProductUpdateRequest;
import product.productservice.entity.Product;
import product.productservice.kafka.OrderEventProducer;

import java.time.LocalDateTime;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;
    private final RedisLockService redisLockService;
    private final OrderEventProducer orderEventProducer;

    @Value("${lock.product.timeout:5000}")
    private long lockTimeout;

    @Value("${lock.product.lease-time:10000}")
    private long lockLeaseTime;


    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Hàm tự động chạy khi khởi tạo dự án để chèn sẵn 1 sản phẩm mẫu vào database ảo
    @PostConstruct
    public void initData() {
        Product sample = new Product("iPhone 15 Pro", "Mô tả sản phẩm iPhone siêu VIP", "https://image.com/iphone15.png");
        productRepository.save(sample);
        log.info("Đã khởi tạo sản phẩm mẫu thành công với ID = 1");
    }

    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(Long id) {
        log.info("==> Cache MISS! Không tìm thấy trong Redis. Đang đọc từ Database cho Product ID: {}", id);

        // Giả lập độ trễ Database ~200ms theo yêu cầu bối cảnh bải toán
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại với ID: " + id));

        return mapToResponse(product);
    }

    @CacheEvict(value = "products", key = "#id")
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        log.info("==> Admin thực hiện cập nhật. Cache EVICT kích hoạt xóa Key 'products::{}'", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại với ID: " + id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());

        Product updatedProduct = productRepository.save(product);
        return mapToResponse(updatedProduct);
    }

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getImageUrl()
        );
    }

    public void createOrderAndPublishEvent(Order order) {
        // Lưu order vào DB
        Order savedOrder = orderRepository.save(order);

        // Tạo event
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .customerId(savedOrder.getCustomerId())
                .productName(savedOrder.getProductName())
                .quantity(savedOrder.getQuantity())
                .totalPrice(savedOrder.getTotalPrice())
                .email(savedOrder.getCustomerEmail())
                .createdAt(LocalDateTime.now())
                .build();

        // Gửi event vào Kafka
        orderEventProducer.publishOrderCreatedEvent(event);
    }


    @Transactional
    public OrderResponse purchaseProduct(Long productId, Integer quantity, String customerEmail) {
        String lockKey = "lock:product:" + productId;

        return redisLockService.executeWithLock(lockKey, lockTimeout, lockLeaseTime, () -> {
            return processPurchase(productId, quantity, customerEmail);
        });


    }
    @Transactional
    private OrderResponse processPurchase(Long productId, Integer quantity, String customerEmail) {
        log.info("Processing purchase for product: {}, quantity: {}", productId, quantity);


        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Kiểm tra tồn kho
        if (product.getQuantity() < quantity) {
            log.warn("Out of stock for product: {}, available: {}, requested: {}",
                    productId, product.getQuantity(), quantity);
            throw new RuntimeException("Sản phẩm đã hết hàng");
        }

        // Trừ số lượng tồn kho
        product.setQuantity(product.getQuantity() - quantity);
        Product updatedProduct = productRepository.save(product);
        log.info("Inventory reduced for product: {}, remaining: {}", productId, updatedProduct.getQuantity());

        // Tạo event
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(System.currentTimeMillis())
                .customerId(1L)
                .productName(product.getName())
                .quantity(quantity)
                .totalPrice(product.getPrice() * quantity)
                .email(customerEmail)
                .createdAt(LocalDateTime.now())
                .build();

        // Gửi event vào Kafka
        orderEventProducer.publishOrderCreatedEvent(event);

        return OrderResponse.builder()
                .orderId(event.getOrderId())
                .productId(productId)
                .productName(product.getName())
                .quantity(quantity)
                .totalPrice(event.getTotalPrice())
                .status("SUCCESS")
                .message("Đặt hàng thành công")
                .build();
    }

}
