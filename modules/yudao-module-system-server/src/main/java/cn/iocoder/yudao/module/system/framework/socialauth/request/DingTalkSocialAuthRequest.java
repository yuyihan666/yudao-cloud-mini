package cn.iocoder.yudao.module.system.framework.socialauth.request;

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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.getText;
import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree;
import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.SOCIAL_USER_AUTH_FAILURE;
import static cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateSupport.cacheState;
import static cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateSupport.checkCodeAndState;

/**
 * 钉钉扫码授权请求。
 */
@RequiredArgsConstructor
public class DingTalkSocialAuthRequest implements SocialAuthRequest {

    private static final String AUTHORIZE_URL = "https://oapi.dingtalk.com/connect/qrconnect";
    private static final String USER_INFO_URL = "https://oapi.dingtalk.com/sns/getuserinfo_bycode";

    private final SocialAuthClientConfig config;
    private final SocialAuthHttpClient httpClient;
    private final SocialAuthStateCache stateCache;

    @Override
    public String getSource() {
        return SocialTypeEnum.DINGTALK.getSource();
    }

    @Override
    public String authorize(String state) {
        String realState = cacheState(state, stateCache);
        return AUTHORIZE_URL
                + "?response_type=code"
                + "&appid=" + HttpUtils.encodeUtf8(config.getClientId())
                + "&scope=snsapi_login"
                + "&redirect_uri=" + HttpUtils.encodeUtf8(config.getRedirectUri())
                + "&state=" + HttpUtils.encodeUtf8(realState);
    }

    @Override
    public SocialAuthUser login(SocialAuthCallback callback) {
        checkCodeAndState(getSource(), callback, config, stateCache);
        String timestamp = String.valueOf(System.currentTimeMillis());
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("signature", sign(config.getClientSecret(), timestamp));
        query.put("timestamp", timestamp);
        query.put("accessKey", config.getClientId());
        String userJson = httpClient.postJson(USER_INFO_URL, query, toJsonString(Map.of("tmp_auth_code", callback.getCode())));
        JsonNode root = parseTree(userJson);
        if (root.path("errcode").asInt() != 0) {
            throw exception(SOCIAL_USER_AUTH_FAILURE, getText(root, "errmsg"));
        }
        JsonNode userInfo = root.path("user_info");
        return new SocialAuthUser()
                .setUuid(getText(userInfo, "unionid"))
                .setNickname(getText(userInfo, "nick"))
                .setRawTokenInfo(toJsonString(Map.of(
                        "openid", getText(userInfo, "openid"),
                        "unionid", getText(userInfo, "unionid"))))
                .setRawUserInfo(userJson);
    }

    private static String sign(String secret, String timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(timestamp.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("failed to sign dingtalk request", e);
        }
    }

}
