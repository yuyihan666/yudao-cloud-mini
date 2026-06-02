package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthHttpClient;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequest;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequestBuilder;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 钉钉授权请求构建器。
 */
@Component
@RequiredArgsConstructor
public class DingTalkSocialAuthRequestBuilder implements SocialAuthRequestBuilder {

    private final SocialAuthHttpClient httpClient;
    private final SocialAuthStateCache stateCache;

    @Override
    public String getSource() {
        return SocialTypeEnum.DINGTALK.getSource();
    }

    @Override
    public SocialAuthRequest build(SocialAuthClientConfig config) {
        return new DingTalkSocialAuthRequest(config, httpClient, stateCache);
    }

}
