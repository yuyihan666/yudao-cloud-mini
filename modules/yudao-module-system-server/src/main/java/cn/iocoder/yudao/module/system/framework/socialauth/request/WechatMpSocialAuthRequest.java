package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthHttpClient;

/**
 * 微信公众号授权请求。
 */
public class WechatMpSocialAuthRequest extends AbstractWechatSocialAuthRequest {

    public WechatMpSocialAuthRequest(SocialAuthClientConfig config, SocialAuthHttpClient httpClient) {
        super(config, httpClient);
    }

    @Override
    public String getSource() {
        return SocialTypeEnum.WECHAT_MP.getSource();
    }

    @Override
    protected String getAuthorizeUrl() {
        return "https://open.weixin.qq.com/connect/oauth2/authorize";
    }

    @Override
    protected String getAuthorizeScope() {
        return "snsapi_userinfo";
    }

}
