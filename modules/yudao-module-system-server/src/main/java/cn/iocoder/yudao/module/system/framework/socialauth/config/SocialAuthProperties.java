package cn.iocoder.yudao.module.system.framework.socialauth.config;

import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 社交授权配置。
 */
@Data
@ConfigurationProperties(prefix = "justauth")
public class SocialAuthProperties {

    private Boolean enabled = true;
    private Map<String, SocialAuthClientConfig> type = new LinkedHashMap<>();
    private Cache cache = new Cache();

    @Data
    public static class Cache {

        private String prefix = "social_auth_state:";
        private Duration timeout = Duration.ofMinutes(3);

    }

}
