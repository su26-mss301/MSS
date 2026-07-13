package wardrobe.project.com.userservice.service.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BlockedUserCacheService {

    private static final String PREFIX = "blocked:user:";

    private final StringRedisTemplate redisTemplate;

    public void block(String userId) {
        redisTemplate.opsForValue().set(
                PREFIX + userId,
                "true"
        );
    }

    public void unblock(String userId) {
        redisTemplate.delete(PREFIX + userId);
    }

    public boolean isBlocked(String userId) {
        return redisTemplate.hasKey(PREFIX + userId);
    }
}