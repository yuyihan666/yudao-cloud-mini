package cn.iocoder.yudao.module.system.framework.socialauth.core;

import cn.iocoder.yudao.module.system.service.social.dto.SocialAuthUser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SocialAuthRequestFactoryTest {

    @Test
    void getShouldReturnRegisteredRequest() {
        SocialAuthRequest request = new FixedSocialAuthRequest();
        SocialAuthRequestFactory factory = new SocialAuthRequestFactory(Map.of("GITEE", request));

        assertSame(request, factory.get("GITEE"));
    }

    @Test
    void getShouldBeCaseInsensitive() {
        SocialAuthRequest request = new FixedSocialAuthRequest();
        SocialAuthRequestFactory factory = new SocialAuthRequestFactory(Map.of("GITEE", request));

        assertSame(request, factory.get("gitee"));
    }

    @Test
    void getShouldFailForBlankSource() {
        SocialAuthRequestFactory factory = new SocialAuthRequestFactory(Map.of());

        assertThrows(IllegalArgumentException.class, () -> factory.get(" "));
    }

    @Test
    void getShouldFailForUnsupportedSource() {
        SocialAuthRequestFactory factory = new SocialAuthRequestFactory(Map.of());

        assertThrows(IllegalArgumentException.class, () -> factory.get("UNKNOWN"));
    }

    private static final class FixedSocialAuthRequest implements SocialAuthRequest {

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
