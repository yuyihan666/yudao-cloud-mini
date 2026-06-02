package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaRedisBetterConfigImpl;
import cn.iocoder.yudao.framework.common.util.cache.CacheUtils;
import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequest;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequestBuilder;
import com.binarywang.spring.starter.wxjava.miniapp.properties.WxMaProperties;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.redis.RedisTemplateWxRedisOps;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 微信小程序授权请求构建器。
 */
@Component
@RequiredArgsConstructor
public class WechatMiniProgramSocialAuthRequestBuilder implements SocialAuthRequestBuilder {

    private final WxMaProperties wxMaProperties;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 缓存 WxMaService 对象，避免每次 build 重新创建。
     *
     * key：clientId + ":" + clientSecret
     */
    private final LoadingCache<String, WxMaService> wxMaServiceCache = CacheUtils.buildAsyncReloadingCache(
            Duration.ofSeconds(10L),
            new CacheLoader<String, WxMaService>() {
                @Override
                public WxMaService load(String key) {
                    String[] keys = key.split(":");
                    return buildWxMaService(keys[0], keys[1]);
                }
            });

    @Override
    public String getSource() {
        return SocialTypeEnum.WECHAT_MINI_PROGRAM.getSource();
    }

    @Override
    public SocialAuthRequest build(SocialAuthClientConfig config) {
        WxMaService wxMaService = wxMaServiceCache.getUnchecked(
                config.getClientId() + ":" + config.getClientSecret());
        return new WechatMiniProgramSocialAuthRequest(wxMaService);
    }

    private WxMaService buildWxMaService(String clientId, String clientSecret) {
        WxMaRedisBetterConfigImpl configStorage = new WxMaRedisBetterConfigImpl(
                new RedisTemplateWxRedisOps(stringRedisTemplate),
                wxMaProperties.getConfigStorage().getKeyPrefix());
        configStorage.setAppid(clientId);
        configStorage.setSecret(clientSecret);

        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(configStorage);
        return service;
    }

}
