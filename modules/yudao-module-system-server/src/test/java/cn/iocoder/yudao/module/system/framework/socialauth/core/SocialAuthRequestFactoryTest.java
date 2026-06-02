package cn.iocoder.yudao.module.system.framework.socialauth.core;

import cn.iocoder.yudao.module.system.service.social.dto.SocialAuthUser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SocialAuthRequestFactoryTest {

    @Test
    void getShouldBuildRequestWithDefaultConfig() {
        SocialAuthClientConfig defaultConfig = new SocialAuthClientConfig().setClientId("default-client-id");
        SocialAuthRequestFactory factory = new SocialAuthRequestFactory(Map.of("GITEE", new FixedSocialAuthRequestBuilder()),
                Map.of("GITEE", defaultConfig));

        FixedSocialAuthRequest request = (FixedSocialAuthRequest) factory.get("GITEE");

        assertEquals(defaultConfig, request.config);
    }

    @Test
    void getShouldBuildRequestWithOverrideConfig() {
        SocialAuthClientConfig defaultConfig = new SocialAuthClientConfig().setClientId("default-client-id");
        SocialAuthClientConfig overrideConfig = new SocialAuthClientConfig().setClientId("override-client-id");
        SocialAuthRequestFactory factory = new SocialAuthRequestFactory(Map.of("GITEE", new FixedSocialAuthRequestBuilder()),
                Map.of("GITEE", defaultConfig));

        FixedSocialAuthRequest request = (FixedSocialAuthRequest) factory.get("gitee", overrideConfig);

        assertEquals(overrideConfig, request.config);
    }

    @Test
    void getShouldFailForBlankSource() {
        SocialAuthRequestFactory factory = new SocialAuthRequestFactory(Map.of(), Map.of());

        assertThrows(IllegalArgumentException.class, () -> factory.get(" "));
    }

    @Test
    void getShouldFailForUnsupportedSource() {
        SocialAuthRequestFactory factory = new SocialAuthRequestFactory(Map.of(), Map.of());

        assertThrows(IllegalArgumentException.class, () -> factory.get("UNKNOWN"));
    }

    private static final class FixedSocialAuthRequestBuilder implements SocialAuthRequestBuilder {

        @Override
        public String getSource() {
            return "GITEE";
        }

        @Override
        public SocialAuthRequest build(SocialAuthClientConfig config) {
            return new FixedSocialAuthRequest(config);
        }

    }

    private static final class FixedSocialAuthRequest implements SocialAuthRequest {

        private final SocialAuthClientConfig config;

        private FixedSocialAuthRequest(SocialAuthClientConfig config) {
            this.config = config;
        }

        @Override
        public String getSource() {
            return "GITEE";
        }

        @Override
        public String authorize(String state) {
            return "https://example.com?state=" + state;
        }

        @Override
        public SocialAuthUser login(SocialAuthCallback callback) {
            return new SocialAuthUser();
        }

    }

}
