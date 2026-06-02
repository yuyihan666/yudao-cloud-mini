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
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(buildKey(state)));
    }

    private String buildKey(String state) {
        return prefix + state;
    }

}
