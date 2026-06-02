package cn.iocoder.yudao.module.system.framework.socialauth.core;

/**
 * 社交授权 state 缓存。
 */
public interface SocialAuthStateCache {

    void cache(String state);

    boolean contains(String state);

}
