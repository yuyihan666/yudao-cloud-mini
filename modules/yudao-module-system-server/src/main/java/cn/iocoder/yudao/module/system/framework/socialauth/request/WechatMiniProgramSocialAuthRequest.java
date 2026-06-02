package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthCallback;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequest;
import cn.iocoder.yudao.module.system.service.social.dto.SocialAuthUser;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.error.WxErrorException;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.SOCIAL_USER_AUTH_FAILURE;

/**
 * 微信小程序授权请求。
 */
@RequiredArgsConstructor
public class WechatMiniProgramSocialAuthRequest implements SocialAuthRequest {

    private final WxMaService wxMaService;

    @Override
    public String getSource() {
        return SocialTypeEnum.WECHAT_MINI_PROGRAM.getSource();
    }

    @Override
    public String authorize(String state) {
        throw new UnsupportedOperationException("WECHAT_MINI_PROGRAM does not support authorize URL");
    }

    @Override
    public SocialAuthUser login(SocialAuthCallback callback) {
        WxMaJscode2SessionResult sessionResult;
        try {
            sessionResult = wxMaService.getUserService().getSessionInfo(callback.getCode());
        } catch (WxErrorException e) {
            throw exception(SOCIAL_USER_AUTH_FAILURE, e.getMessage());
        }
        String rawSessionInfo = toJsonString(sessionResult);
        return new SocialAuthUser()
                .setUuid(sessionResult.getOpenid())
                .setAccessToken(sessionResult.getSessionKey())
                .setRawTokenInfo(rawSessionInfo)
                .setRawUserInfo(rawSessionInfo);
    }

}
