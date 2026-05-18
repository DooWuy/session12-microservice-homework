package shipping.shippingservice.repository;

import io.lettuce.core.output.ListOfGenericMapsOutput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shipping.shippingservice.entity.Shipping;

import java.util.Optional;
@Repository
public interface ShippingRepository extends JpaRepository<Shipping , Long> {

    Optional<Shipping> findByOrderId(Long orderId);
}
