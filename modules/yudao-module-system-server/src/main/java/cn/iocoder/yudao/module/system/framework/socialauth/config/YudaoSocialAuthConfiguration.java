package cn.iocoder.yudao.module.system.framework.socialauth.config;

import cn.iocoder.yudao.module.system.framework.socialauth.core.DefaultSocialAuthHttpClient;
import cn.iocoder.yudao.module.system.framework.socialauth.core.RedisSocialAuthStateCache;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthHttpClient;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequestBuilder;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequestFactory;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

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
    public SocialAuthHttpClient socialAuthHttpClient() {
        return new DefaultSocialAuthHttpClient();
    }

    @Bean
    public SocialAuthStateCache socialAuthStateCache(StringRedisTemplate stringRedisTemplate,
                                                     SocialAuthProperties properties) {
        return new RedisSocialAuthStateCache(stringRedisTemplate,
                properties.getCache().getPrefix(), properties.getCache().getTimeout());
    }

    @Bean
    @ConditionalOnProperty(prefix = "justauth", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SocialAuthRequestFactory socialAuthRequestFactory(List<SocialAuthRequestBuilder> requestBuilders,
                                                             SocialAuthProperties properties) {
        Map<String, SocialAuthRequestBuilder> requestBuilderMap = requestBuilders.stream()
                .collect(Collectors.toMap(request -> request.getSource().toUpperCase(Locale.ROOT), Function.identity()));
        return new SocialAuthRequestFactory(requestBuilderMap, SocialAuthRequestFactory.normalizeConfigs(properties.getType()));
    }

}
