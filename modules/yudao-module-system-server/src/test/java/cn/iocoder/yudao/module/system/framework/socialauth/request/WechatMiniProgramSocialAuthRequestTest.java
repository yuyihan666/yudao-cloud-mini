package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.WxMaUserService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthCallback;
import cn.iocoder.yudao.module.system.service.social.dto.SocialAuthUser;
import me.chanjar.weixin.common.error.WxErrorException;
import org.junit.jupiter.api.Test;

import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WechatMiniProgramSocialAuthRequestTest {

    @Test
    void getSourceShouldReturnWechatMiniProgramSource() {
        WechatMiniProgramSocialAuthRequest request = new WechatMiniProgramSocialAuthRequest(mock(WxMaService.class));

        assertEquals(SocialTypeEnum.WECHAT_MINI_PROGRAM.getSource(), request.getSource());
    }

    @Test
    void loginShouldMapSessionInfoToSocialAuthUser() throws WxErrorException {
        WxMaService wxMaService = mock(WxMaService.class);
        WxMaUserService wxMaUserService = mock(WxMaUserService.class);
        when(wxMaService.getUserService()).thenReturn(wxMaUserService);
        WxMaJscode2SessionResult sessionResult = new WxMaJscode2SessionResult();
        sessionResult.setOpenid("openid-1");
        sessionResult.setSessionKey("session-key-1");
        sessionResult.setUnionid("unionid-1");
        when(wxMaUserService.getSessionInfo("code-1")).thenReturn(sessionResult);
        WechatMiniProgramSocialAuthRequest request = new WechatMiniProgramSocialAuthRequest(wxMaService);
        SocialAuthCallback callback = new SocialAuthCallback().setCode("code-1").setState("state-1");

        SocialAuthUser authUser = request.login(callback);

        assertEquals("openid-1", authUser.getUuid());
        assertEquals("session-key-1", authUser.getAccessToken());
        assertEquals(toJsonString(sessionResult), authUser.getRawTokenInfo());
        assertEquals(toJsonString(sessionResult), authUser.getRawUserInfo());
    }

}
