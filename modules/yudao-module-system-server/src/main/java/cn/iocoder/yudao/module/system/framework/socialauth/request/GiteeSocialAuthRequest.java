package cn.iocoder.yudao.module.system.framework.socialauth.request;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthCallback;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthHttpClient;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequest;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateCache;
import cn.iocoder.yudao.module.system.service.social.dto.SocialAuthUser;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.getText;
import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree;
import static cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateSupport.cacheState;
import static cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateSupport.checkCodeAndState;

/**
 * Gitee 授权请求。
 */
@RequiredArgsConstructor
public class GiteeSocialAuthRequest implements SocialAuthRequest {

    private static final String AUTHORIZE_URL = "https://gitee.com/oauth/authorize";
    private static final String TOKEN_URL = "https://gitee.com/oauth/token";
    private static final String USER_INFO_URL = "https://gitee.com/api/v5/user";

    private final SocialAuthClientConfig config;
    private final SocialAuthHttpClient httpClient;
    private final SocialAuthStateCache stateCache;

    @Override
    public String getSource() {
        return SocialTypeEnum.GITEE.getSource();
    }

    @Override
    public String authorize(String state) {
        String realState = cacheState(state, stateCache);
        return AUTHORIZE_URL
                + "?response_type=code"
                + "&client_id=" + HttpUtils.encodeUtf8(config.getClientId())
                + "&redirect_uri=" + HttpUtils.encodeUtf8(config.getRedirectUri())
                + "&state=" + HttpUtils.encodeUtf8(realState);
    }

    @Override
    public SocialAuthUser login(SocialAuthCallback callback) {
        checkCodeAndState(getSource(), callback, config, stateCache);
        Map<String, Object> tokenForm = new LinkedHashMap<>();
        tokenForm.put("grant_type", "authorization_code");
        tokenForm.put("code", callback.getCode());
        tokenForm.put("client_id", config.getClientId());
        tokenForm.put("client_secret", config.getClientSecret());
        tokenForm.put("redirect_uri", config.getRedirectUri());
        String tokenJson = httpClient.postForm(TOKEN_URL, tokenForm);
        String accessToken = getText(parseTree(tokenJson), "access_token");

        String userJson = httpClient.get(USER_INFO_URL, Map.of("access_token", accessToken));
        JsonNode userInfo = parseTree(userJson);
        String nickname = getText(userInfo, "name");
        if (StrUtil.isBlank(nickname)) {
            nickname = getText(userInfo, "login");
        }
        return new SocialAuthUser()
                .setUuid(getText(userInfo, "id"))
                .setNickname(nickname)
                .setAvatar(getText(userInfo, "avatar_url"))
                .setAccessToken(accessToken)
                .setRawTokenInfo(tokenJson)
                .setRawUserInfo(userJson);
    }

}
