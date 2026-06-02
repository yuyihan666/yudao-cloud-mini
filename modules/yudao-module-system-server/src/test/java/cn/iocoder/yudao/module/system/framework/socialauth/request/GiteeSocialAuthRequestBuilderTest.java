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

class GiteeSocialAuthRequestBuilderTest {

    @Test
    void getSourceShouldReturnGiteeSource() {
        GiteeSocialAuthRequestBuilder builder = new GiteeSocialAuthRequestBuilder(mock(SocialAuthHttpClient.class),
                mock(SocialAuthStateCache.class));

        assertEquals(SocialTypeEnum.GITEE.getSource(), builder.getSource());
    }

    @Test
    void buildShouldCreateGiteeRequestWithConfig() {
        GiteeSocialAuthRequestBuilder builder = new GiteeSocialAuthRequestBuilder(mock(SocialAuthHttpClient.class),
                mock(SocialAuthStateCache.class));
        SocialAuthClientConfig config = new SocialAuthClientConfig()
                .setClientId("client-id")
                .setClientSecret("client-secret")
                .setRedirectUri("https://app.example.com/callback");

        SocialAuthRequest request = builder.build(config);

        assertInstanceOf(GiteeSocialAuthRequest.class, request);
        assertEquals("https://gitee.com/oauth/authorize?response_type=code&client_id=client-id"
                + "&redirect_uri=https%3A%2F%2Fapp.example.com%2Fcallback&state=state-1",
                request.authorize("state-1"));
    }

}
