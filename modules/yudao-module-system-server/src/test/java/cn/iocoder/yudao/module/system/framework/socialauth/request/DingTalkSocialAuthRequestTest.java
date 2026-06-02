package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthCallback;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthHttpClient;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateCache;
import cn.iocoder.yudao.module.system.service.social.dto.SocialAuthUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DingTalkSocialAuthRequestTest {

    @Test
    void getSourceShouldReturnDingTalkSource() {
        DingTalkSocialAuthRequest request = new DingTalkSocialAuthRequest(newSocialAuthClientConfig(),
                mock(SocialAuthHttpClient.class), mock(SocialAuthStateCache.class));

        assertEquals(SocialTypeEnum.DINGTALK.getSource(), request.getSource());
    }

    @Test
    void authorizeShouldBuildDingTalkQrConnectUrlAndCacheState() {
        SocialAuthStateCache stateCache = mock(SocialAuthStateCache.class);
        DingTalkSocialAuthRequest request = new DingTalkSocialAuthRequest(newSocialAuthClientConfig(),
                mock(SocialAuthHttpClient.class), stateCache);

        String authorizeUrl = request.authorize("state-1");

        assertTrue(authorizeUrl.startsWith("https://oapi.dingtalk.com/connect/qrconnect?"), authorizeUrl);
        assertTrue(authorizeUrl.contains("response_type=code"), authorizeUrl);
        assertTrue(authorizeUrl.contains("appid=client-id"), authorizeUrl);
        assertTrue(authorizeUrl.contains("scope=snsapi_login"), authorizeUrl);
        assertTrue(authorizeUrl.contains("redirect_uri=https%3A%2F%2Fapp.example.com%2Fcallback"), authorizeUrl);
        assertTrue(authorizeUrl.contains("state=state-1"), authorizeUrl);
        verify(stateCache).cache("state-1");
    }

    @Test
    void loginShouldMapDingTalkUserInfoToSocialAuthUser() {
        SocialAuthStateCache stateCache = mock(SocialAuthStateCache.class);
        when(stateCache.contains("state-1")).thenReturn(true);
        SocialAuthHttpClient httpClient = mock(SocialAuthHttpClient.class);
        String userJson = "{\"errcode\":0,\"user_info\":{\"openid\":\"openid-1\","
                + "\"unionid\":\"unionid-1\",\"nick\":\"土豆\"}}";
        when(httpClient.postJson(eq("https://oapi.dingtalk.com/sns/getuserinfo_bycode"), argThat(query -> {
            assertEquals("client-id", query.get("accessKey"));
            assertNotNull(query.get("timestamp"));
            assertNotNull(query.get("signature"));
            return true;
        }), argThat(body -> body.contains("\"tmp_auth_code\":\"code-1\"")))).thenReturn(userJson);
        DingTalkSocialAuthRequest request = new DingTalkSocialAuthRequest(newSocialAuthClientConfig(), httpClient,
                stateCache);

        SocialAuthUser authUser = request.login(new SocialAuthCallback().setCode("code-1").setState("state-1"));

        assertEquals("unionid-1", authUser.getUuid());
        assertEquals("土豆", authUser.getNickname());
        assertEquals(userJson, authUser.getRawUserInfo());
    }

    private static SocialAuthClientConfig newSocialAuthClientConfig() {
        return new SocialAuthClientConfig()
                .setClientId("client-id")
                .setClientSecret("client-secret")
                .setRedirectUri("https://app.example.com/callback");
    }

}
