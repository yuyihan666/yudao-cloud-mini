package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthHttpClient;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateCache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class WechatOpenSocialAuthRequestTest {

    @Test
    void getSourceShouldReturnWechatOpenSource() {
        WechatOpenSocialAuthRequest request = new WechatOpenSocialAuthRequest(newSocialAuthClientConfig(),
                mock(SocialAuthHttpClient.class), mock(SocialAuthStateCache.class));

        assertEquals(SocialTypeEnum.WECHAT_OPEN.getSource(), request.getSource());
    }

    @Test
    void authorizeShouldBuildWechatOpenAuthorizeUrl() {
        WechatOpenSocialAuthRequest request = new WechatOpenSocialAuthRequest(newSocialAuthClientConfig(),
                mock(SocialAuthHttpClient.class), mock(SocialAuthStateCache.class));

        String authorizeUrl = request.authorize("state-1");

        assertTrue(authorizeUrl.startsWith("https://open.weixin.qq.com/connect/qrconnect?"), authorizeUrl);
        assertTrue(authorizeUrl.contains("appid=client-id"), authorizeUrl);
        assertTrue(authorizeUrl.contains("redirect_uri=https%3A%2F%2Fapp.example.com%2Fcallback"), authorizeUrl);
        assertTrue(authorizeUrl.contains("response_type=code"), authorizeUrl);
        assertTrue(authorizeUrl.contains("scope=snsapi_login"), authorizeUrl);
        assertTrue(authorizeUrl.contains("state=state-1"), authorizeUrl);
        assertTrue(authorizeUrl.endsWith("#wechat_redirect"), authorizeUrl);
    }

    private static SocialAuthClientConfig newSocialAuthClientConfig() {
        return new SocialAuthClientConfig()
                .setClientId("client-id")
                .setClientSecret("client-secret")
                .setRedirectUri("https://app.example.com/callback");
    }

}
