package cn.iocoder.yudao.module.system.framework.socialauth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialAuthPropertiesTest {

    @Test
    void shouldBindLegacyJustAuthPrefix() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "justauth.enabled", "true",
                "justauth.type.WECHAT_MP.client-id", "appid",
                "justauth.type.WECHAT_MP.client-secret", "secret",
                "justauth.type.WECHAT_MP.ignore-check-redirect-uri", "true",
                "justauth.cache.prefix", "social_auth_state:",
                "justauth.cache.timeout", "24h"
        ));

        SocialAuthProperties properties = new Binder(source)
                .bind("justauth", SocialAuthProperties.class)
                .orElseThrow(IllegalStateException::new);

        assertTrue(properties.getEnabled());
        assertEquals("appid", properties.getType().get("WECHAT_MP").getClientId());
        assertEquals("secret", properties.getType().get("WECHAT_MP").getClientSecret());
        assertTrue(properties.getType().get("WECHAT_MP").getIgnoreCheckRedirectUri());
        assertEquals("social_auth_state:", properties.getCache().getPrefix());
        assertEquals(Duration.ofHours(24), properties.getCache().getTimeout());
    }

}
