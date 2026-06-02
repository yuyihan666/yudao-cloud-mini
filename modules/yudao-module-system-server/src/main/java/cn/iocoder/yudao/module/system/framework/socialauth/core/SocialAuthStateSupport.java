package cn.iocoder.yudao.module.system.framework.socialauth.core;

import cn.hutool.core.util.StrUtil;

import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.SOCIAL_USER_AUTH_FAILURE;

/**
 * 社交授权 state 与回调参数校验工具。
 */
public final class SocialAuthStateSupport {

    private SocialAuthStateSupport() {
    }

    public static String cacheState(String state, SocialAuthStateCache stateCache) {
        String realState = StrUtil.blankToDefault(state, createState());
        stateCache.cache(realState);
        return realState;
    }

    public static void checkCodeAndState(String source, SocialAuthCallback callback,
                                         SocialAuthClientConfig config, SocialAuthStateCache stateCache) {
        if (callback == null || StrUtil.isBlank(callback.getCode())) {
            throw exception(SOCIAL_USER_AUTH_FAILURE, source + " code is blank");
        }
        if (Boolean.TRUE.equals(config.getIgnoreCheckState())) {
            return;
        }
        if (StrUtil.isBlank(callback.getState()) || !stateCache.contains(callback.getState())) {
            throw exception(SOCIAL_USER_AUTH_FAILURE, source + " state is invalid");
        }
    }

    public static String createState() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
