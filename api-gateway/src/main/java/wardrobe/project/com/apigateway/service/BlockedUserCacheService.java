package wardrobe.project.com.apigateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BlockedUserCacheService {

    private static final String PREFIX = "blocked:user:";

    private final ReactiveStringRedisTemplate redisTemplate;

    public Mono<Boolean> isBlocked(String userId) {
        return redisTemplate.hasKey(PREFIX + userId);
    }
}