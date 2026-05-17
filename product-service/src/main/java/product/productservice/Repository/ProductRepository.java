package product.productservice.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import product.productservice.entity.Product;
@Repository
public interface ProductRepository extends JpaRepository<Product ,Long> {
}
