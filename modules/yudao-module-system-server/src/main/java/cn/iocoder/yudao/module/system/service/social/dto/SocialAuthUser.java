package cn.iocoder.yudao.module.system.service.social.dto;

import lombok.Data;

/**
 * 社交平台授权后的用户信息。
 *
 * <p>用于隔离业务层和第三方授权 SDK 的模型，避免第三方模型穿透到业务服务边界。</p>
 */
@Data
public class SocialAuthUser {

    private String uuid;
    private String nickname;
    private String avatar;
    private String accessToken;
    private String rawTokenInfo;
    private String rawUserInfo;

}
