package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthCallback;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthHttpClient;
import cn.iocoder.yudao.module.system.service.social.dto.SocialAuthUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WechatMpSocialAuthRequestTest {

    @Test
    void getSourceShouldReturnWechatMpSource() {
        WechatMpSocialAuthRequest request = new WechatMpSocialAuthRequest(newSocialAuthClientConfig(),
                mock(SocialAuthHttpClient.class));

        assertEquals(SocialTypeEnum.WECHAT_MP.getSource(), request.getSource());
    }

    @Test
    void authorizeShouldBuildWechatMpAuthorizeUrl() {
        WechatMpSocialAuthRequest request = new WechatMpSocialAuthRequest(newSocialAuthClientConfig(),
                mock(SocialAuthHttpClient.class));

        String authorizeUrl = request.authorize("state-1");

        assertTrue(authorizeUrl.startsWith("https://open.weixin.qq.com/connect/oauth2/authorize?"), authorizeUrl);
        assertTrue(authorizeUrl.contains("appid=client-id"), authorizeUrl);
        assertTrue(authorizeUrl.contains("redirect_uri=https%3A%2F%2Fapp.example.com%2Fcallback"), authorizeUrl);
        assertTrue(authorizeUrl.contains("response_type=code"), authorizeUrl);
        assertTrue(authorizeUrl.contains("scope=snsapi_userinfo"), authorizeUrl);
        assertTrue(authorizeUrl.contains("state=state-1"), authorizeUrl);
        assertTrue(authorizeUrl.endsWith("#wechat_redirect"), authorizeUrl);
    }

    @Test
    void loginShouldMapWechatUserInfoToSocialAuthUser() {
        SocialAuthHttpClient httpClient = mock(SocialAuthHttpClient.class);
        String tokenJson = "{\"access_token\":\"access-token-1\",\"openid\":\"openid-1\"}";
        when(httpClient.get(eq("https://api.weixin.qq.com/sns/oauth2/access_token"), argThat(query -> {
            assertEquals("client-id", query.get("appid"));
            assertEquals("client-secret", query.get("secret"));
            assertEquals("code-1", query.get("code"));
            assertEquals("authorization_code", query.get("grant_type"));
            return true;
        }))).thenReturn(tokenJson);
        String userJson = "{\"openid\":\"openid-1\",\"nickname\":\"土豆\",\"headimgurl\":\"https://avatar.example.com/a.png\"}";
        when(httpClient.get(eq("https://api.weixin.qq.com/sns/userinfo"), argThat(query -> {
            assertEquals("access-token-1", query.get("access_token"));
            assertEquals("openid-1", query.get("openid"));
            assertEquals("zh_CN", query.get("lang"));
            return true;
        }))).thenReturn(userJson);
        WechatMpSocialAuthRequest request = new WechatMpSocialAuthRequest(newSocialAuthClientConfig(), httpClient);

        SocialAuthUser authUser = request.login(new SocialAuthCallback().setCode("code-1").setState("state-1"));

        assertEquals("openid-1", authUser.getUuid());
        assertEquals("土豆", authUser.getNickname());
        assertEquals("https://avatar.example.com/a.png", authUser.getAvatar());
        assertEquals("access-token-1", authUser.getAccessToken());
        assertEquals(tokenJson, authUser.getRawTokenInfo());
        assertEquals(userJson, authUser.getRawUserInfo());
    }

    private static SocialAuthClientConfig newSocialAuthClientConfig() {
        return new SocialAuthClientConfig()
                .setClientId("client-id")
                .setClientSecret("client-secret")
                .setRedirectUri("https://app.example.com/callback");
    }

}
