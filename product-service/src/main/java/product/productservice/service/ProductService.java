package product.productservice.service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.Cacheable;
import jakarta.persistence.criteria.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import product.productservice.Repository.ProductRepository;
import product.productservice.dto.ProductResponse;
import product.productservice.dto.ProductUpdateRequest;
import product.productservice.entity.Product;

import java.time.LocalDateTime;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;

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
        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

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

}
