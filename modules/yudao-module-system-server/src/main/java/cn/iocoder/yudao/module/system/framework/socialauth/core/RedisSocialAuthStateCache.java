package cn.iocoder.yudao.module.system.framework.socialauth.core;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 基于 Redis 的社交授权 state 缓存。
 */
@RequiredArgsConstructor
public class RedisSocialAuthStateCache implements SocialAuthStateCache {

    private final StringRedisTemplate stringRedisTemplate;
    private final String prefix;
    private final Duration timeout;

    @Override
    public void cache(String state) {
        stringRedisTemplate.opsForValue().set(buildKey(state), state, timeout);
    }

    @Override
    public boolean contains(String state) {
        // 使用 GET + DELETE 代替 hasKey，消费 state 令牌防止重放
        String key = buildKey(state);
        Boolean deleted = stringRedisTemplate.delete(key);
        return Boolean.TRUE.equals(deleted);
    }

    private String buildKey(String state) {
        return prefix + state;
    }

}
