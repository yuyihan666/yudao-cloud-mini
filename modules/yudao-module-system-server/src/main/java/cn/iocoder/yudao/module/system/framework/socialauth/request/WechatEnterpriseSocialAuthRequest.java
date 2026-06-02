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
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.getText;
import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree;
import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.SOCIAL_USER_AUTH_FAILURE;
import static cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateSupport.cacheState;
import static cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthStateSupport.checkCodeAndState;

/**
 * 企业微信扫码授权请求。
 */
@RequiredArgsConstructor
public class WechatEnterpriseSocialAuthRequest implements SocialAuthRequest {

    private static final String AUTHORIZE_URL = "https://open.work.weixin.qq.com/wwopen/sso/qrConnect";
    private static final String TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    private static final String USER_INFO_URL = "https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo";
    private static final String USER_GET_URL = "https://qyapi.weixin.qq.com/cgi-bin/user/get";
    private static final String USER_DETAIL_URL = "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserdetail";

    private final SocialAuthClientConfig config;
    private final SocialAuthHttpClient httpClient;
    private final SocialAuthStateCache stateCache;

    @Override
    public String getSource() {
        return SocialTypeEnum.WECHAT_ENTERPRISE.getSource();
    }

    @Override
    public String authorize(String state) {
        String realState = cacheState(state, stateCache);
        return AUTHORIZE_URL
                + "?appid=" + HttpUtils.encodeUtf8(config.getClientId())
                + "&agentid=" + HttpUtils.encodeUtf8(config.getAgentId())
                + "&redirect_uri=" + HttpUtils.encodeUtf8(config.getRedirectUri())
                + "&state=" + HttpUtils.encodeUtf8(realState)
                + "&lang=zh_CN";
    }

    @Override
    public SocialAuthUser login(SocialAuthCallback callback) {
        checkCodeAndState(getSource(), callback, config, stateCache);
        String tokenJson = httpClient.get(TOKEN_URL, Map.of(
                "corpid", config.getClientId(),
                "corpsecret", config.getClientSecret()
        ));
        JsonNode tokenInfo = checkResponse(tokenJson);
        String accessToken = getText(tokenInfo, "access_token");

        String userTicketJson = httpClient.get(USER_INFO_URL, Map.of(
                "access_token", accessToken,
                "code", callback.getCode()
        ));
        JsonNode userTicketInfo = checkResponse(userTicketJson);
        String userId = getText(userTicketInfo, "UserId");
        if (StrUtil.isBlank(userId)) {
            throw exception(SOCIAL_USER_AUTH_FAILURE, getSource() + " user id is blank");
        }
        String userInfoJson = httpClient.get(USER_GET_URL, Map.of(
                "access_token", accessToken,
                "userid", userId
        ));
        JsonNode userInfoNode = checkResponse(userInfoJson);
        if (!(userInfoNode instanceof ObjectNode userInfo)) {
            throw exception(SOCIAL_USER_AUTH_FAILURE, getSource() + " user info response is not a JSON object");
        }
        String userTicket = getText(userTicketInfo, "user_ticket");
        if (StrUtil.isNotBlank(userTicket)) {
            String userDetailJson = httpClient.postJson(USER_DETAIL_URL, Map.of("access_token", accessToken),
                    toJsonString(Map.of("user_ticket", userTicket)));
            mergeUserDetail(userInfo, checkResponse(userDetailJson));
        }
        String nickname = StrUtil.blankToDefault(getText(userInfo, "alias"), getText(userInfo, "name"));
        return new SocialAuthUser()
                .setUuid(userId)
                .setNickname(nickname)
                .setAvatar(getText(userInfo, "avatar"))
                .setAccessToken(accessToken)
                .setRawTokenInfo(tokenJson)
                .setRawUserInfo(toJsonString(userInfo));
    }

    private JsonNode checkResponse(String json) {
        JsonNode root = parseTree(json);
        if (root.has("errcode") && root.path("errcode").asInt() != 0) {
            throw exception(SOCIAL_USER_AUTH_FAILURE, getText(root, "errmsg"));
        }
        return root;
    }

    private static void mergeUserDetail(ObjectNode userInfo, JsonNode userDetail) {
        for (Map.Entry<String, JsonNode> field : userDetail.properties()) {
            if (!"errcode".equals(field.getKey()) && !"errmsg".equals(field.getKey())) {
                userInfo.set(field.getKey(), field.getValue());
            }
        }
    }

}
