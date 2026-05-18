package product.productservice.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import product.productservice.entity.FlashSale;

import java.time.LocalDateTime;
import java.util.List;

@Repository

}public interface FlashSaleRepository extends JpaRepository<FlashSale, Long> {
    Optional<FlashSale> findByProductId(Long productId);

    List<FlashSale> findByStatusAndEndTimeAfter(String status, LocalDateTime now);
}
