package cn.iocoder.yudao.module.system.framework.socialauth.core;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void containsShouldConsumeExistingState() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        // delete 返回 true 表示 key 存在并被删除
        when(redisTemplate.delete("social_auth_state:state-1")).thenReturn(true);
        RedisSocialAuthStateCache stateCache = new RedisSocialAuthStateCache(redisTemplate,
                "social_auth_state:", Duration.ofMinutes(5));

        assertTrue(stateCache.contains("state-1"));
        // 验证 delete 被调用（消费式检查，防止重放）
        verify(redisTemplate).delete("social_auth_state:state-1");
    }

    @Test
    void containsShouldReturnFalseForMissingState() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        // delete 返回 false 表示 key 不存在
        when(redisTemplate.delete("social_auth_state:state-unknown")).thenReturn(false);
        RedisSocialAuthStateCache stateCache = new RedisSocialAuthStateCache(redisTemplate,
                "social_auth_state:", Duration.ofMinutes(5));

        assertFalse(stateCache.contains("state-unknown"));
    }

}
