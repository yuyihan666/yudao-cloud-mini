package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthHttpClient;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequest;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateCache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class DingTalkSocialAuthRequestBuilderTest {

    @Test
    void getSourceShouldReturnDingTalkSource() {
        DingTalkSocialAuthRequestBuilder builder = new DingTalkSocialAuthRequestBuilder(
                mock(SocialAuthHttpClient.class), mock(SocialAuthStateCache.class));

        assertEquals(SocialTypeEnum.DINGTALK.getSource(), builder.getSource());
    }

    @Test
    void buildShouldCreateDingTalkRequest() {
        DingTalkSocialAuthRequestBuilder builder = new DingTalkSocialAuthRequestBuilder(
                mock(SocialAuthHttpClient.class), mock(SocialAuthStateCache.class));

        SocialAuthRequest request = builder.build(new SocialAuthClientConfig());

        assertInstanceOf(DingTalkSocialAuthRequest.class, request);
    }

}
