package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthHttpClient;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class WechatMpSocialAuthRequestBuilderTest {

    @Test
    void getSourceShouldReturnWechatMpSource() {
        WechatMpSocialAuthRequestBuilder builder = new WechatMpSocialAuthRequestBuilder(mock(SocialAuthHttpClient.class));

        assertEquals(SocialTypeEnum.WECHAT_MP.getSource(), builder.getSource());
    }

    @Test
    void buildShouldCreateWechatMpRequest() {
        WechatMpSocialAuthRequestBuilder builder = new WechatMpSocialAuthRequestBuilder(mock(SocialAuthHttpClient.class));

        SocialAuthRequest request = builder.build(new SocialAuthClientConfig());

        assertInstanceOf(WechatMpSocialAuthRequest.class, request);
    }

}
