package cn.iocoder.yudao.module.system.framework.socialauth.core;

import lombok.Data;

/**
 * 社交平台客户端配置。
 */
@Data
public class SocialAuthClientConfig {

    private String clientId;
    private String clientSecret;
    private String agentId;
    private String publicKey;
    private String redirectUri;
    private Boolean ignoreCheckRedirectUri;
    private Boolean ignoreCheckState;

}
