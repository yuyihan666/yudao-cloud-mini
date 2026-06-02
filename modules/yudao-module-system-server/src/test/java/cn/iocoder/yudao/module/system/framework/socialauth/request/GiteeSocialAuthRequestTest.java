package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthCallback;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthHttpClient;
import cn.iocoder.yudao.module.system.service.social.dto.SocialAuthUser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GiteeSocialAuthRequestTest {

    @Test
    void getSourceShouldReturnGiteeSource() {
        GiteeSocialAuthRequest request = new GiteeSocialAuthRequest(newSocialAuthClientConfig(),
                mock(SocialAuthHttpClient.class));

        assertEquals(SocialTypeEnum.GITEE.getSource(), request.getSource());
    }

    @Test
    void authorizeShouldBuildAuthorizeUrl() {
        GiteeSocialAuthRequest request = new GiteeSocialAuthRequest(newSocialAuthClientConfig(),
                mock(SocialAuthHttpClient.class));

        String authorizeUrl = request.authorize("state-1");

        assertTrue(authorizeUrl.startsWith("https://gitee.com/oauth/authorize?"), authorizeUrl);
        assertTrue(authorizeUrl.contains("response_type=code"), authorizeUrl);
        assertTrue(authorizeUrl.contains("client_id=client-id"), authorizeUrl);
        assertTrue(authorizeUrl.contains("redirect_uri=https%3A%2F%2Fapp.example.com%2Fcallback"), authorizeUrl);
        assertTrue(authorizeUrl.contains("state=state-1"), authorizeUrl);
    }

    @Test
    void loginShouldMapTokenAndUserInfoToSocialAuthUser() {
        SocialAuthHttpClient httpClient = mock(SocialAuthHttpClient.class);
        String tokenJson = "{\"access_token\":\"access-token-1\"}";
        when(httpClient.postForm(eq("https://gitee.com/oauth/token"), argThat(form -> {
            assertEquals("authorization_code", form.get("grant_type"));
            assertEquals("code-1", form.get("code"));
            assertEquals("client-id", form.get("client_id"));
            assertEquals("client-secret", form.get("client_secret"));
            assertEquals("https://app.example.com/callback", form.get("redirect_uri"));
            return true;
        }))).thenReturn(tokenJson);
        String userJson = "{\"id\":123,\"name\":\"土豆\",\"avatar_url\":\"https://avatar.example.com/a.png\"}";
        when(httpClient.get(eq("https://gitee.com/api/v5/user"), argThat(query -> {
            assertEquals("access-token-1", query.get("access_token"));
            return true;
        }))).thenReturn(userJson);
        GiteeSocialAuthRequest request = new GiteeSocialAuthRequest(newSocialAuthClientConfig(), httpClient);
        SocialAuthCallback callback = new SocialAuthCallback().setCode("code-1").setState("state-1");

        SocialAuthUser authUser = request.login(callback);

        assertEquals("123", authUser.getUuid());
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
