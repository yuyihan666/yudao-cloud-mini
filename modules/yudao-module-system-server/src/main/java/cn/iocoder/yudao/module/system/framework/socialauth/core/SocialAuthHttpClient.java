package cn.iocoder.yudao.module.system.framework.socialauth.core;

import java.util.Map;

/**
 * 社交授权 HTTP 客户端。
 */
public interface SocialAuthHttpClient {

    String get(String url, Map<String, ?> query);

    String postForm(String url, Map<String, ?> form);

    String postJson(String url, Map<String, ?> query, String body);

}
