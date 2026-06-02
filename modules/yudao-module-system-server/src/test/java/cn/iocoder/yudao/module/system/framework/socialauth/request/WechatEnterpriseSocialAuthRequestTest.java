package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthCallback;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthHttpClient;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateCache;
import cn.iocoder.yudao.module.system.service.social.dto.SocialAuthUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WechatEnterpriseSocialAuthRequestTest {

    @Test
    void getSourceShouldReturnWechatEnterpriseSource() {
        WechatEnterpriseSocialAuthRequest request = new WechatEnterpriseSocialAuthRequest(newSocialAuthClientConfig(),
                mock(SocialAuthHttpClient.class), mock(SocialAuthStateCache.class));

        assertEquals(SocialTypeEnum.WECHAT_ENTERPRISE.getSource(), request.getSource());
    }

    @Test
    void authorizeShouldBuildWechatEnterpriseAuthorizeUrlAndCacheState() {
        SocialAuthStateCache stateCache = mock(SocialAuthStateCache.class);
        WechatEnterpriseSocialAuthRequest request = new WechatEnterpriseSocialAuthRequest(newSocialAuthClientConfig(),
                mock(SocialAuthHttpClient.class), stateCache);

        String authorizeUrl = request.authorize("state-1");

        assertTrue(authorizeUrl.startsWith("https://open.work.weixin.qq.com/wwopen/sso/qrConnect?"), authorizeUrl);
        assertTrue(authorizeUrl.contains("appid=client-id"), authorizeUrl);
        assertTrue(authorizeUrl.contains("agentid=agent-id"), authorizeUrl);
        assertTrue(authorizeUrl.contains("redirect_uri=https%3A%2F%2Fapp.example.com%2Fcallback"), authorizeUrl);
        assertTrue(authorizeUrl.contains("state=state-1"), authorizeUrl);
        assertTrue(authorizeUrl.contains("lang=zh_CN"), authorizeUrl);
        verify(stateCache).cache("state-1");
    }

    @Test
    void loginShouldMergeWechatEnterpriseUserDetail() {
        SocialAuthStateCache stateCache = mock(SocialAuthStateCache.class);
        when(stateCache.contains("state-1")).thenReturn(true);
        SocialAuthHttpClient httpClient = mock(SocialAuthHttpClient.class);
        String tokenJson = "{\"errcode\":0,\"access_token\":\"access-token-1\",\"expires_in\":7200}";
        when(httpClient.get(eq("https://qyapi.weixin.qq.com/cgi-bin/gettoken"), argThat(query -> {
            assertEquals("client-id", query.get("corpid"));
            assertEquals("client-secret", query.get("corpsecret"));
            return true;
        }))).thenReturn(tokenJson);
        when(httpClient.get(eq("https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo"), argThat(query -> {
            assertEquals("access-token-1", query.get("access_token"));
            assertEquals("code-1", query.get("code"));
            return true;
        }))).thenReturn("{\"errcode\":0,\"UserId\":\"user-id-1\",\"user_ticket\":\"ticket-1\"}");
        when(httpClient.get(eq("https://qyapi.weixin.qq.com/cgi-bin/user/get"), argThat(query -> {
            assertEquals("access-token-1", query.get("access_token"));
            assertEquals("user-id-1", query.get("userid"));
            return true;
        }))).thenReturn("{\"errcode\":0,\"userid\":\"user-id-1\",\"name\":\"张三\","
                + "\"alias\":\"土豆\",\"avatar\":\"https://avatar.example.com/a.png\"}");
        when(httpClient.postJson(eq("https://qyapi.weixin.qq.com/cgi-bin/auth/getuserdetail"), argThat(query -> {
            assertEquals("access-token-1", query.get("access_token"));
            return true;
        }), argThat(body -> body.contains("\"user_ticket\":\"ticket-1\""))))
                .thenReturn("{\"errcode\":0,\"email\":\"tudou@example.com\"}");
        WechatEnterpriseSocialAuthRequest request = new WechatEnterpriseSocialAuthRequest(newSocialAuthClientConfig(),
                httpClient, stateCache);

        SocialAuthUser authUser = request.login(new SocialAuthCallback().setCode("code-1").setState("state-1"));

        assertEquals("user-id-1", authUser.getUuid());
        assertEquals("土豆", authUser.getNickname());
        assertEquals("https://avatar.example.com/a.png", authUser.getAvatar());
        assertEquals("access-token-1", authUser.getAccessToken());
        assertEquals(tokenJson, authUser.getRawTokenInfo());
        assertTrue(authUser.getRawUserInfo().contains("\"email\":\"tudou@example.com\""));
    }

    private static SocialAuthClientConfig newSocialAuthClientConfig() {
        return new SocialAuthClientConfig()
                .setClientId("client-id")
                .setClientSecret("client-secret")
                .setAgentId("agent-id")
                .setRedirectUri("https://app.example.com/callback");
    }

}
