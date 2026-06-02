package cn.iocoder.yudao.module.system.framework.socialauth.core;

import lombok.Data;

/**
 * 社交平台授权回调参数。
 */
@Data
public class SocialAuthCallback {

    private String code;
    private String state;

}
