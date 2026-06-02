package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaRedisBetterConfigImpl;
import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequest;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequestBuilder;
import com.binarywang.spring.starter.wxjava.miniapp.properties.WxMaProperties;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.redis.RedisTemplateWxRedisOps;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 微信小程序授权请求构建器。
 */
@Component
@RequiredArgsConstructor
public class WechatMiniProgramSocialAuthRequestBuilder implements SocialAuthRequestBuilder {

    private final WxMaProperties wxMaProperties;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String getSource() {
        return SocialTypeEnum.WECHAT_MINI_PROGRAM.getSource();
    }

    @Override
    public SocialAuthRequest build(SocialAuthClientConfig config) {
        return new WechatMiniProgramSocialAuthRequest(buildWxMaService(config));
    }

    private WxMaService buildWxMaService(SocialAuthClientConfig config) {
        WxMaRedisBetterConfigImpl configStorage = new WxMaRedisBetterConfigImpl(
                new RedisTemplateWxRedisOps(stringRedisTemplate),
                wxMaProperties.getConfigStorage().getKeyPrefix());
        configStorage.setAppid(config.getClientId());
        configStorage.setSecret(config.getClientSecret());

        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(configStorage);
        return service;
    }

}
