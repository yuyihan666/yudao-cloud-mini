package cn.iocoder.yudao.module.system.framework.socialauth.config;

import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequest;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequestFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 社交授权配置类。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SocialAuthProperties.class)
public class YudaoSocialAuthConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "justauth", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SocialAuthRequestFactory socialAuthRequestFactory(List<SocialAuthRequest> requests) {
        Map<String, SocialAuthRequest> requestMap = requests.stream()
                .collect(Collectors.toMap(request -> request.getSource().toUpperCase(Locale.ROOT), Function.identity()));
        return new SocialAuthRequestFactory(requestMap);
    }

}
