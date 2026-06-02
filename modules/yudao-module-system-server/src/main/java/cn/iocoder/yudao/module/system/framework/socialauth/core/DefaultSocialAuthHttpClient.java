package cn.iocoder.yudao.module.system.framework.socialauth.core;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.util.http.HttpUtils.encodeUtf8;

/**
 * 默认社交授权 HTTP 客户端。
 */
public class DefaultSocialAuthHttpClient implements SocialAuthHttpClient {

    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded;charset=UTF-8";
    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

    @Override
    public String get(String url, Map<String, ?> query) {
        try (HttpResponse response = HttpRequest.get(appendQuery(url, query)).execute()) {
            return response.body();
        }
    }

    @Override
    public String postForm(String url, Map<String, ?> form) {
        try (HttpResponse response = HttpRequest.post(url)
                .contentType(FORM_CONTENT_TYPE)
                .body(toFormBody(form))
                .execute()) {
            return response.body();
        }
    }

    @Override
    public String postJson(String url, Map<String, ?> query, String body) {
        try (HttpResponse response = HttpRequest.post(appendQuery(url, query))
                .contentType(JSON_CONTENT_TYPE)
                .body(body)
                .execute()) {
            return response.body();
        }
    }

    private static String appendQuery(String url, Map<String, ?> query) {
        String queryString = toFormBody(query);
        if (queryString.isEmpty()) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?") + queryString;
    }

    private static String toFormBody(Map<String, ?> form) {
        return form.entrySet().stream()
                .map(entry -> encodeUtf8(entry.getKey()) + "=" + encodeUtf8(String.valueOf(entry.getValue())))
                .collect(Collectors.joining("&"));
    }

}
