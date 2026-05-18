package product.productservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import product.productservice.dto.OrderRequest;
import product.productservice.dto.OrderResponse;
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


    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        ProductResponse response = productService.getProductById(id);
        long endTime = System.currentTimeMillis();


        return ResponseEntity.ok()
                .header("X-Response-Time-MS", String.valueOf(endTime - startTime))
                .body(response);
    }


    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductUpdateRequest request) {
        ProductResponse updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(updated);
    }


    @PostMapping("/purchase")
    public ResponseEntity<OrderResponse> purchaseProduct(@RequestBody OrderRequest request) {
        try {
            log.info("Purchase request: {}", request);
            OrderResponse response = productService.purchaseProduct(
                    request.getProductId(),
                    request.getQuantity(),
                    request.getCustomerEmail()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Purchase failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    OrderResponse.builder()
                            .status("FAILED")
                            .message(e.getMessage())
                            .build()
            );
        }

}
