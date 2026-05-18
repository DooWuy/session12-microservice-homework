package product.productservice.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import product.productservice.Repository.FlashSaleRepository;
import product.productservice.entity.FlashSale;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleCacheService {


    private final RedisTemplate<String, Object> redisTemplate;
    private final FlashSaleRepository flashSaleRepository;

    private static final String FLASHSALE_QUANTITY_KEY = "flashsale:qty:";
    private static final String FLASHSALE_PRICE_KEY = "flashsale:price:";
    private static final String FLASHSALE_ACTIVE_KEY = "flashsale:active";

    public void loadFlashSaleToCache(Long flashSaleId) {
        try {
            FlashSale flashSale = flashSaleRepository.findById(flashSaleId)
                    .orElseThrow(() -> new RuntimeException("FlashSale not found: " + flashSaleId));

            if (!"ACTIVE".equals(flashSale.getStatus())) {
                log.warn("FlashSale is not active: {}", flashSaleId);
                return;
            }

            String qtyKey = FLASHSALE_QUANTITY_KEY + flashSaleId;
            String priceKey = FLASHSALE_PRICE_KEY + flashSaleId;


            redisTemplate.opsForValue().set(qtyKey, flashSale.getTotalQuantity());

            // Lưu giá Flash Sale vào Redis
            redisTemplate.opsForValue().set(priceKey, flashSale.getFlashSalePrice());

            // Lưu thông tin vào set "flashsale:active"
            redisTemplate.opsForSet().add(FLASHSALE_ACTIVE_KEY, flashSaleId);


            long ttl = java.time.temporal.ChronoUnit.SECONDS.between(
                    LocalDateTime.now(),
                    flashSale.getEndTime()
            );

            if (ttl > 0) {
                redisTemplate.expire(qtyKey, ttl, TimeUnit.SECONDS);
                redisTemplate.expire(priceKey, ttl, TimeUnit.SECONDS);
            }

            log.info("FlashSale loaded to cache: id={}, qty={}, price={}, ttl={}s",
                    flashSaleId, flashSale.getTotalQuantity(), flashSale.getFlashSalePrice(), ttl);

        } catch (Exception e) {
            log.error("Error loading flash sale to cache", e);
        }
    }

    public Integer getFlashSaleQuantity(Long flashSaleId) {
        String key = FLASHSALE_QUANTITY_KEY + flashSaleId;
        Object qty = redisTemplate.opsForValue().get(key);
        return qty != null ? Integer.parseInt(qty.toString()) : null;
    }

    public Double getFlashSalePrice(Long flashSaleId) {
        String key = FLASHSALE_PRICE_KEY + flashSaleId;
        Object price = redisTemplate.opsForValue().get(key);
        return price != null ? Double.parseDouble(price.toString()) : null;
    }

    public Boolean decrementQuantity(Long flashSaleId, Integer quantity) {
        String key = FLASHSALE_QUANTITY_KEY + flashSaleId;
        Long newQty = redisTemplate.opsForValue().decrement(key, quantity);
        return newQty != null && newQty >= 0;
    }

    public void clearFlashSaleCache(Long flashSaleId) {
        String qtyKey = FLASHSALE_QUANTITY_KEY + flashSaleId;
        String priceKey = FLASHSALE_PRICE_KEY + flashSaleId;

        redisTemplate.delete(qtyKey);
        redisTemplate.delete(priceKey);
        redisTemplate.opsForSet().remove(FLASHSALE_ACTIVE_KEY, flashSaleId);

        log.info("FlashSale cache cleared: {}", flashSaleId);
    }

    public Boolean hasStock(Long flashSaleId) {
        Integer qty = getFlashSaleQuantity(flashSaleId);
        return qty != null && qty > 0;
    }

    @Scheduled(fixedRate = 300000)  // 5 phút
    public void syncActiveFlashSales() {
        try {
            log.info("Syncing active flash sales to cache...");
            List<FlashSale> activeFlashSales = flashSaleRepository.findByStatusAndEndTimeAfter(
                    "ACTIVE",
                    LocalDateTime.now()
            );

            for (FlashSale flashSale : activeFlashSales) {
                loadFlashSaleToCache(flashSale.getId());
            }

            log.info("Synced {} active flash sales", activeFlashSales.size());

        } catch (Exception e) {
            log.error("Error syncing flash sales", e);
        }
    }
}
