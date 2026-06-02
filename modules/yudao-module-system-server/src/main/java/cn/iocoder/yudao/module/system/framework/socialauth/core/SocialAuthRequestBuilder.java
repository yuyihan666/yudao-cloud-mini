package cn.iocoder.yudao.module.system.framework.socialauth.core;

/**
 * 社交平台授权请求构建器。
 */
public interface SocialAuthRequestBuilder {

    String getSource();

    SocialAuthRequest build(SocialAuthClientConfig config);

}
