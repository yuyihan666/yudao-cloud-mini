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

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.getText;
import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.SOCIAL_USER_AUTH_FAILURE;
import static cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateSupport.cacheState;
import static cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateSupport.checkCodeAndState;

/**
 * 支付宝小程序授权请求。
 */
@RequiredArgsConstructor
public class AlipayMiniProgramSocialAuthRequest implements SocialAuthRequest {

    private static final String AUTHORIZE_URL = "https://openauth.alipay.com/oauth2/publicAppAuthorize.htm";
    private static final String GATEWAY_URL = "https://openapi.alipay.com/gateway.do";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SocialAuthClientConfig config;
    private final SocialAuthHttpClient httpClient;
    private final SocialAuthStateCache stateCache;

    @Override
    public String getSource() {
        return SocialTypeEnum.ALIPAY_MINI_PROGRAM.getSource();
    }

    @Override
    public String authorize(String state) {
        String realState = cacheState(state, stateCache);
        return AUTHORIZE_URL
                + "?app_id=" + HttpUtils.encodeUtf8(config.getClientId())
                + "&scope=auth_user"
                + "&redirect_uri=" + HttpUtils.encodeUtf8(config.getRedirectUri())
                + "&state=" + HttpUtils.encodeUtf8(realState);
    }

    @Override
    public SocialAuthUser login(SocialAuthCallback callback) {
        checkCodeAndState(getSource(), callback, config, stateCache);
        // 1. 获取 access_token
        Map<String, Object> tokenForm = signedForm("alipay.system.oauth.token", Map.of(
                "grant_type", "authorization_code",
                "code", callback.getCode()
        ));
        String tokenJson = httpClient.postForm(GATEWAY_URL, tokenForm);
        JsonNode tokenRoot = parseTree(tokenJson);
        JsonNode tokenInfo = tokenRoot.path("alipay_system_oauth_token_response");
        checkAlipayResponse(tokenRoot, tokenInfo, "alipay_system_oauth_token_response");
        String accessToken = getText(tokenInfo, "access_token");

        // 2. 获取用户信息
        Map<String, Object> userForm = signedForm("alipay.user.info.share", Map.of("auth_token", accessToken));
        String userJson = httpClient.postForm(GATEWAY_URL, userForm);
        JsonNode userRoot = parseTree(userJson);
        JsonNode userInfo = userRoot.path("alipay_user_info_share_response");
        checkAlipayResponse(userRoot, userInfo, "alipay_user_info_share_response");
        return new SocialAuthUser()
                .setUuid(getText(userInfo, "user_id"))
                .setNickname(getText(userInfo, "nick_name"))
                .setAvatar(getText(userInfo, "avatar"))
                .setAccessToken(accessToken)
                .setRawTokenInfo(tokenJson)
                .setRawUserInfo(userJson);
    }

    private void checkAlipayResponse(JsonNode root, JsonNode responseNode, String responseKey) {
        // 支付宝错误响应在 error_response 节点中
        JsonNode errorNode = root.path("error_response");
        if (!errorNode.isMissingNode()) {
            String subCode = getText(errorNode, "sub_code");
            String subMsg = getText(errorNode, "sub_msg");
            throw exception(SOCIAL_USER_AUTH_FAILURE,
                    subCode != null ? subCode + ": " + subMsg : getText(errorNode, "msg"));
        }
    }

    private Map<String, Object> signedForm(String method, Map<String, ?> bizParams) {
        Map<String, Object> form = new TreeMap<>();
        form.put("app_id", config.getClientId());
        form.put("method", method);
        form.put("format", "json");
        form.put("charset", "UTF-8");
        form.put("sign_type", "RSA2");
        form.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        form.put("version", "1.0");
        form.putAll(bizParams);
        form.put("sign", sign(form));
        return new LinkedHashMap<>(form);
    }

    private String sign(Map<String, Object> form) {
        String content = form.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> !"sign".equals(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(parsePrivateKey(config.getClientSecret()));
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("failed to sign alipay request", e);
        }
    }

    private static PrivateKey parsePrivateKey(String privateKey) throws Exception {
        String key = privateKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

}
