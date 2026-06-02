package cn.iocoder.yudao.module.system.framework.socialauth.core;

import cn.iocoder.yudao.module.system.service.social.dto.SocialAuthUser;

/**
 * 社交平台授权请求。
 */
public interface SocialAuthRequest {

    String getSource();

    String authorize(String state);

    SocialAuthUser login(SocialAuthCallback callback);

}
