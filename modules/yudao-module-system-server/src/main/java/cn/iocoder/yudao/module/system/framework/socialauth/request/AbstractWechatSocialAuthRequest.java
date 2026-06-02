package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthCallback;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthHttpClient;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequest;
import cn.iocoder.yudao.module.system.service.social.dto.SocialAuthUser;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.getText;
import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree;

/**
 * 微信 OAuth2 授权请求基类。
 */
@RequiredArgsConstructor
public abstract class AbstractWechatSocialAuthRequest implements SocialAuthRequest {

    private static final String TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String USER_INFO_URL = "https://api.weixin.qq.com/sns/userinfo";

    private final SocialAuthClientConfig config;
    private final SocialAuthHttpClient httpClient;

    protected abstract String getAuthorizeUrl();

    protected abstract String getAuthorizeScope();

    @Override
    public String authorize(String state) {
        return getAuthorizeUrl()
                + "?appid=" + HttpUtils.encodeUtf8(config.getClientId())
                + "&redirect_uri=" + HttpUtils.encodeUtf8(config.getRedirectUri())
                + "&response_type=code"
                + "&scope=" + HttpUtils.encodeUtf8(getAuthorizeScope())
                + "&state=" + HttpUtils.encodeUtf8(state)
                + "#wechat_redirect";
    }

    @Override
    public SocialAuthUser login(SocialAuthCallback callback) {
        String tokenJson = httpClient.get(TOKEN_URL, Map.of(
                "appid", config.getClientId(),
                "secret", config.getClientSecret(),
                "code", callback.getCode(),
                "grant_type", "authorization_code"
        ));
        JsonNode tokenInfo = parseTree(tokenJson);
        String accessToken = getText(tokenInfo, "access_token");
        String openid = getText(tokenInfo, "openid");

        String userJson = httpClient.get(USER_INFO_URL, Map.of(
                "access_token", accessToken,
                "openid", openid,
                "lang", "zh_CN"
        ));
        JsonNode userInfo = parseTree(userJson);
        return new SocialAuthUser()
                .setUuid(getText(userInfo, "openid"))
                .setNickname(getText(userInfo, "nickname"))
                .setAvatar(getText(userInfo, "headimgurl"))
                .setAccessToken(accessToken)
                .setRawTokenInfo(tokenJson)
                .setRawUserInfo(userJson);
    }

}
