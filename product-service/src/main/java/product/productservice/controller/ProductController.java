package product.productservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import product.productservice.dto.ProductResponse;
import product.productservice.dto.ProductUpdateRequest;
import product.productservice.service.ProductService;
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Endpoint xem chi tiết sản phẩm cho người dùng cuối (Có Cache)
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        ProductResponse response = productService.getProductById(id);
        long endTime = System.currentTimeMillis();

        // In thời gian xử lý ra Header của Response để bạn dễ kiểm tra tốc độ
        return ResponseEntity.ok()
                .header("X-Response-Time-MS", String.valueOf(endTime - startTime))
                .body(response);
    }

    // Endpoint cập nhật sản phẩm dành cho Admin (Xóa Cache)
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductUpdateRequest request) {
        ProductResponse updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(updated);
    }

}
