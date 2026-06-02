package cn.iocoder.yudao.module.system.framework.socialauth.core;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 社交平台授权请求工厂。
 */
@RequiredArgsConstructor
public class SocialAuthRequestFactory {

    private final Map<String, SocialAuthRequestBuilder> requestBuilders;
    private final Map<String, SocialAuthClientConfig> defaultConfigs;

    public SocialAuthRequest get(String source) {
        return get(source, defaultConfigs.get(normalizeSource(source)));
    }

    public SocialAuthRequest get(String source, SocialAuthClientConfig config) {
        String normalizedSource = normalizeSource(source);
        SocialAuthRequestBuilder requestBuilder = requestBuilders.get(normalizedSource);
        if (requestBuilder == null) {
            throw new IllegalArgumentException("unsupported social auth source: " + source);
        }
        if (config == null) {
            throw new IllegalArgumentException("missing social auth config: " + source);
        }
        return requestBuilder.build(config);
    }

    public static Map<String, SocialAuthClientConfig> normalizeConfigs(Map<String, SocialAuthClientConfig> configs) {
        Map<String, SocialAuthClientConfig> normalizedConfigs = new LinkedHashMap<>();
        configs.forEach((source, config) -> normalizedConfigs.put(normalizeSource(source), config));
        return normalizedConfigs;
    }

    public static String normalizeSource(String source) {
        if (StrUtil.isBlank(source)) {
            throw new IllegalArgumentException("social auth source must not be blank");
        }
        return source.toUpperCase(Locale.ROOT);
    }

}
