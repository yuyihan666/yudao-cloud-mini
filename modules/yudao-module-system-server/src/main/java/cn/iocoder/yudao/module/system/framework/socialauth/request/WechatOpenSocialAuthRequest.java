package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthHttpClient;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateCache;

/**
 * 微信开放平台授权请求。
 */
public class WechatOpenSocialAuthRequest extends AbstractWechatSocialAuthRequest {

    public WechatOpenSocialAuthRequest(SocialAuthClientConfig config, SocialAuthHttpClient httpClient,
                                       SocialAuthStateCache stateCache) {
        super(config, httpClient, stateCache);
    }

    @Override
    public String getSource() {
        return SocialTypeEnum.WECHAT_OPEN.getSource();
    }

    @Override
    protected String getAuthorizeUrl() {
        return "https://open.weixin.qq.com/connect/qrconnect";
    }

    @Override
    protected String getAuthorizeScope() {
        return "snsapi_login";
    }

}
