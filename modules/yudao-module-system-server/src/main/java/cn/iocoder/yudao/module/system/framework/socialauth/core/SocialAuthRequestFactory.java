package cn.iocoder.yudao.module.system.framework.socialauth.core;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;

import java.util.Locale;
import java.util.Map;

/**
 * 社交平台授权请求工厂。
 */
@RequiredArgsConstructor
public class SocialAuthRequestFactory {

    private final Map<String, SocialAuthRequest> requests;

    public SocialAuthRequest get(String source) {
        if (StrUtil.isBlank(source)) {
            throw new IllegalArgumentException("social auth source must not be blank");
        }
        SocialAuthRequest request = requests.get(source.toUpperCase(Locale.ROOT));
        if (request == null) {
            throw new IllegalArgumentException("unsupported social auth source: " + source);
        }
        return request;
    }

}
