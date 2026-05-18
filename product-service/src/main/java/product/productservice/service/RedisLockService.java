package product.productservice.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisLockService {

    private final RedissonClient redissonClient;

    /**     * Thực thi hàm dưới sự bảo vệ của khóa phân tán     * @param lockKey Khóa Redis (ví dụ: lock:product:1)     * @param timeOut Thời gian chờ để giành khóa (milliseconds)     * @param leaseTime Thời gian tối đa giữ khóa (milliseconds)     * @param supplier Hàm cần execute     * @return Kết quả từ supplier     */
    public <T> T executeWithLock(String lockKey, long timeOut, long leaseTime, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean lockAcquired = false;

        try {
            // Thử giành khóa trong timeOut milliseconds
            lockAcquired = lock.tryLock(timeOut, leaseTime, TimeUnit.MILLISECONDS);

            if (!lockAcquired) {
                log.warn("Failed to acquire lock: {}", lockKey);
                throw new RuntimeException("Could not acquire lock for: " + lockKey);
            }

            log.info("Lock acquired: {}", lockKey);
            return supplier.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for lock: {}", lockKey, e);
            throw new RuntimeException("Lock acquisition interrupted", e);
        } finally {
            if (lockAcquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Lock released: {}", lockKey);
            }
        }
    }

    /**     * Phiên bản đơn giản hơn mà không cần trả về giá trị     */
    public void executeWithLock(String lockKey, long timeOut, long leaseTime, Runnable runnable) {
        executeWithLock(lockKey, timeOut, leaseTime, () -> {
            runnable.run();
            return null;
        });
    }

}
