package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthCallback;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthHttpClient;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateCache;
import cn.iocoder.yudao.module.system.service.social.dto.SocialAuthUser;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlipayMiniProgramSocialAuthRequestTest {

    @Test
    void getSourceShouldReturnAlipaySource() throws Exception {
        AlipayMiniProgramSocialAuthRequest request = new AlipayMiniProgramSocialAuthRequest(newSocialAuthClientConfig(),
                mock(SocialAuthHttpClient.class), mock(SocialAuthStateCache.class));

        assertEquals(SocialTypeEnum.ALIPAY_MINI_PROGRAM.getSource(), request.getSource());
    }

    @Test
    void authorizeShouldBuildAlipayAuthorizeUrlAndCacheState() throws Exception {
        SocialAuthStateCache stateCache = mock(SocialAuthStateCache.class);
        AlipayMiniProgramSocialAuthRequest request = new AlipayMiniProgramSocialAuthRequest(newSocialAuthClientConfig(),
                mock(SocialAuthHttpClient.class), stateCache);

        String authorizeUrl = request.authorize("state-1");

        assertTrue(authorizeUrl.startsWith("https://openauth.alipay.com/oauth2/publicAppAuthorize.htm?"), authorizeUrl);
        assertTrue(authorizeUrl.contains("app_id=client-id"), authorizeUrl);
        assertTrue(authorizeUrl.contains("scope=auth_user"), authorizeUrl);
        assertTrue(authorizeUrl.contains("redirect_uri=https%3A%2F%2Fapp.example.com%2Fcallback"), authorizeUrl);
        assertTrue(authorizeUrl.contains("state=state-1"), authorizeUrl);
        verify(stateCache).cache("state-1");
    }

    @Test
    void loginShouldSignAndMapAlipayUserInfo() throws Exception {
        SocialAuthStateCache stateCache = mock(SocialAuthStateCache.class);
        when(stateCache.contains("state-1")).thenReturn(true);
        SocialAuthHttpClient httpClient = mock(SocialAuthHttpClient.class);
        String tokenJson = "{\"alipay_system_oauth_token_response\":{\"access_token\":\"access-token-1\","
                + "\"user_id\":\"user-id-1\",\"expires_in\":\"3600\",\"refresh_token\":\"refresh-token-1\"}}";
        String userJson = "{\"alipay_user_info_share_response\":{\"user_id\":\"user-id-1\","
                + "\"nick_name\":\"土豆\",\"avatar\":\"https://avatar.example.com/a.png\"}}";
        when(httpClient.postForm(eq("https://openapi.alipay.com/gateway.do"), anyMap())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> form = invocation.getArgument(1);
            assertEquals("client-id", form.get("app_id"));
            assertEquals("RSA2", form.get("sign_type"));
            assertFalse(String.valueOf(form.get("sign")).isBlank());
            if ("alipay.system.oauth.token".equals(form.get("method"))) {
                assertEquals("authorization_code", form.get("grant_type"));
                assertEquals("code-1", form.get("code"));
                return tokenJson;
            }
            assertEquals("alipay.user.info.share", form.get("method"));
            assertEquals("access-token-1", form.get("auth_token"));
            return userJson;
        });
        AlipayMiniProgramSocialAuthRequest request = new AlipayMiniProgramSocialAuthRequest(newSocialAuthClientConfig(),
                httpClient, stateCache);

        SocialAuthUser authUser = request.login(new SocialAuthCallback().setCode("code-1").setState("state-1"));

        assertEquals("user-id-1", authUser.getUuid());
        assertEquals("土豆", authUser.getNickname());
        assertEquals("https://avatar.example.com/a.png", authUser.getAvatar());
        assertEquals("access-token-1", authUser.getAccessToken());
        assertEquals(tokenJson, authUser.getRawTokenInfo());
        assertEquals(userJson, authUser.getRawUserInfo());
    }

    private static SocialAuthClientConfig newSocialAuthClientConfig() throws Exception {
        return new SocialAuthClientConfig()
                .setClientId("client-id")
                .setClientSecret(newPkcs8PrivateKey())
                .setPublicKey("alipay-public-key")
                .setRedirectUri("https://app.example.com/callback");
    }

    private static String newPkcs8PrivateKey() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
    }

}
