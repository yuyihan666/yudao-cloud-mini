package cn.iocoder.yudao.module.system.framework.socialauth.core;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisSocialAuthStateCacheTest {

    @Test
    void cacheShouldWritePrefixedStateWithTimeout() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RedisSocialAuthStateCache stateCache = new RedisSocialAuthStateCache(redisTemplate,
                "social_auth_state:", Duration.ofMinutes(5));

        stateCache.cache("state-1");

        verify(valueOperations).set("social_auth_state:state-1", "state-1", Duration.ofMinutes(5));
    }

    @Test
    void containsShouldReadPrefixedState() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.hasKey("social_auth_state:state-1")).thenReturn(true);
        RedisSocialAuthStateCache stateCache = new RedisSocialAuthStateCache(redisTemplate,
                "social_auth_state:", Duration.ofMinutes(5));

        assertTrue(stateCache.contains("state-1"));
    }

}
