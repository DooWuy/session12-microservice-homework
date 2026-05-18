package product.productservice.service;

import org.springframework.data.redis.core.RedisTemplate;
import product.productservice.event.PromotionUpdateEvent;
import tools.jackson.databind.ObjectMapper;

public class PromotionSubscriber {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**     * Được gọi khi nhận được message từ Redis Channel     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String messageBody = new String(message.getBody());
            log.info("Received promotion update message: {}", messageBody);

            // Parse JSON message
            PromotionUpdateEvent event = objectMapper.readValue(
                    messageBody,
                    PromotionUpdateEvent.class
            );

            log.info("Parsed event: {}", event);

            clearProductCache(event.getProductId());

            log.info("Cache cleared for product: {}", event.getProductId());

        } catch (Exception e) {
            log.error("Error processing promotion update message", e);
        }
    }

    private void clearProductCache(Long productId) {
        String cacheKey = "product:" + productId;
        Boolean deleted = redisTemplate.delete(cacheKey);

        if (Boolean.TRUE.equals(deleted)) {
            log.info("Successfully deleted cache for key: {}", cacheKey);
        } else {
            log.warn("Cache key not found or already expired: {}", cacheKey);
        }
    }
}
