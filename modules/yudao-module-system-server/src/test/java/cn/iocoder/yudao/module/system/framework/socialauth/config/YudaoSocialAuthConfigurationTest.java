package cn.iocoder.yudao.module.system.framework.socialauth.config;

import cn.iocoder.yudao.module.system.framework.socialauth.core.*;
import cn.iocoder.yudao.module.system.service.social.dto.SocialAuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class YudaoSocialAuthConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(YudaoSocialAuthConfiguration.class)
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
            .withBean(SocialAuthRequestBuilder.class, FixedSocialAuthRequestBuilder::new);

    @Test
    void shouldCreateFactoryAndHttpClientBeans() {
        contextRunner
                .withPropertyValues("justauth.type.GITEE.client-id=client-id")
                .run(context -> {
                    assertInstanceOf(DefaultSocialAuthHttpClient.class, context.getBean(SocialAuthHttpClient.class));
                    assertInstanceOf(RedisSocialAuthStateCache.class, context.getBean(SocialAuthStateCache.class));
                    SocialAuthRequestFactory factory = context.getBean(SocialAuthRequestFactory.class);

                    FixedSocialAuthRequest request = (FixedSocialAuthRequest) factory.get("GITEE");

                    assertNotNull(request.config);
                    assertEquals("client-id", request.config.getClientId());
                });
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
