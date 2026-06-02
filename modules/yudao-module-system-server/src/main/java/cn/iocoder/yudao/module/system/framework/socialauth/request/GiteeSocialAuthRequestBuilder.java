package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthHttpClient;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequest;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequestBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Gitee 授权请求构建器。
 */
@Component
@RequiredArgsConstructor
public class GiteeSocialAuthRequestBuilder implements SocialAuthRequestBuilder {

    private final SocialAuthHttpClient httpClient;

    @Override
    public String getSource() {
        return SocialTypeEnum.GITEE.getSource();
    }

    @Override
    public SocialAuthRequest build(SocialAuthClientConfig config) {
        return new GiteeSocialAuthRequest(config, httpClient);
    }

}
