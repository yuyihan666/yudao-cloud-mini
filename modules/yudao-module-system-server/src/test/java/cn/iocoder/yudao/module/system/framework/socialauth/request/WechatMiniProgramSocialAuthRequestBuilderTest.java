package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequest;
import com.binarywang.spring.starter.wxjava.miniapp.properties.WxMaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class WechatMiniProgramSocialAuthRequestBuilderTest {

    @Test
    void getSourceShouldReturnWechatMiniProgramSource() {
        WechatMiniProgramSocialAuthRequestBuilder builder = new WechatMiniProgramSocialAuthRequestBuilder(
                newWxMaProperties(), mock(StringRedisTemplate.class));

        assertEquals(SocialTypeEnum.WECHAT_MINI_PROGRAM.getSource(), builder.getSource());
    }

    @Test
    void buildShouldCreateWechatMiniProgramRequestWithOverrideConfig() {
        WechatMiniProgramSocialAuthRequestBuilder builder = new WechatMiniProgramSocialAuthRequestBuilder(
                newWxMaProperties(), mock(StringRedisTemplate.class));

        SocialAuthRequest request = builder.build(new SocialAuthClientConfig()
                .setClientId("client-id")
                .setClientSecret("client-secret"));

        assertInstanceOf(WechatMiniProgramSocialAuthRequest.class, request);
    }

    private static WxMaProperties newWxMaProperties() {
        WxMaProperties wxMaProperties = new WxMaProperties();
        wxMaProperties.getConfigStorage().setKeyPrefix("wa");
        return wxMaProperties;
    }

}
