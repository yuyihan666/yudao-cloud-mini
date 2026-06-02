# Fastjson Final Deletion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Delete `fastjson`, `fastjson2`, `fastjson2-extension`, `JustAuth`, and `justauth-spring-boot-starter` from normal project runtime and test classpaths.

**Architecture:** Keep Jackson as the only first-party JSON engine. Replace the remaining JustAuth dependency with a project-owned social-auth boundary: business services depend on `SocialAuthUser`, provider implementations depend only on existing platform-specific dependencies, Jackson, Hutool HTTP, and JDK crypto. Compatibility with existing `justauth.*` YAML is kept during the deletion so dependency removal is not coupled to an external configuration migration.

**Tech Stack:** Java 25, Spring Boot 3.5.x, Gradle convention plugins, Jackson via `JsonUtils`, Hutool HTTP helpers, existing WxJava starters, JUnit 5, Mockito, current `agentQuick` / `agentDb` / `agentApi` / `agentVerify` gates.

---

## Current Evidence

- Direct first-party imports of `com.alibaba.fastjson*` are already blocked by `verifyNoFastjsonUsage`.
- Direct Gradle declarations of fastjson artifacts are already blocked by `verifyNoDirectFastjsonDependencies`.
- Fastjson is already excluded by default outside `:modules:yudao-module-system-server` and `:modules:yudao-server`.
- Gateway and infra boot jars already build without fastjson artifacts.
- The remaining blocker is JustAuth:

```text
me.zhyd.oauth.model.AuthUser
private com.alibaba.fastjson.JSONObject rawUserInfo;
public com.alibaba.fastjson.JSONObject getRawUserInfo();
public void setRawUserInfo(com.alibaba.fastjson.JSONObject);
```

This means final deletion is not a JSON utility refactor. It is a social-auth dependency replacement.

## Deletion Principles

- Do not replace JustAuth with another general OAuth aggregation library. That would move supply-chain risk instead of reducing it.
- Do not add a new dependency for a provider until a failing provider test proves existing JDK, Jackson, Hutool, and WxJava APIs cannot support that provider.
- Keep the public service contract stable: `SocialClientService#getAuthorizeUrl(...)` and `SocialClientService#getAuthUser(...)` remain the business entry points.
- Keep the current `justauth.*` YAML prefix during deletion. Rename configuration only in a later migration after dependency removal is verified.
- Every phase must leave an agent-visible proof: focused tests, `agentVerify`, dependency insight, and boot jar scans.

## Success Definition

The work is complete only when all checks below pass without `-PexcludeFastjsonRuntime=true`:

```bash
./gradlew :modules:yudao-module-system-server:agentVerify --rerun-tasks
./gradlew :modules:yudao-server:bootJar :modules:yudao-gateway:bootJar :modules:yudao-module-system-server:bootJar :modules:yudao-module-infra-server:bootJar
jar tf modules/yudao-server/build/libs/yudao-server.jar | rg "BOOT-INF/lib/(fastjson|fastjson2|JustAuth|justauth)" || true
jar tf modules/yudao-module-system-server/build/libs/yudao-module-system-server.jar | rg "BOOT-INF/lib/(fastjson|fastjson2|JustAuth|justauth)" || true
rg "me\\.zhyd\\.oauth|com\\.xkcoding\\.justauth|libs\\.justauth|justauth-starter|com\\.alibaba\\.fastjson" -n modules gradle build-logic --glob '!**/build/**'
```

Expected:

```text
BUILD SUCCESSFUL
no jar scan output
no source/build output except historical docs
```

Do not run multiple Gradle builds in parallel in this repository; shared build directories have already caused transient compile failures.

## File Structure

The final shape should look like this:

```text
modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/service/social/
  SocialClientService.java
  SocialClientServiceImpl.java
  SocialUserServiceImpl.java
  dto/SocialAuthUser.java

modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth/
  config/SocialAuthProperties.java
  config/YudaoSocialAuthConfiguration.java
  core/SocialAuthCallback.java
  core/SocialAuthClientConfig.java
  core/SocialAuthRequest.java
  core/SocialAuthRequestFactory.java
  core/SocialAuthStateCache.java
  core/RedisSocialAuthStateCache.java
  core/AbstractOAuth2SocialAuthRequest.java
  core/SocialAuthHttpClient.java
  request/GiteeSocialAuthRequest.java
  request/WechatMpSocialAuthRequest.java
  request/WechatOpenSocialAuthRequest.java
  request/WechatMiniProgramSocialAuthRequest.java
  request/DingTalkSocialAuthRequest.java
  request/WechatEnterpriseSocialAuthRequest.java
  request/AlipayMiniProgramSocialAuthRequest.java

modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/framework/socialauth/
  SocialAuthPropertiesTest.java
  SocialAuthRequestFactoryTest.java
  RedisSocialAuthStateCacheTest.java
  request/*SocialAuthRequestTest.java

Delete after switch:
modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/justauth/
```

## Task 1: Finish The Business Boundary Isolation

**Purpose:** Ensure business code and business tests no longer reflect on JustAuth `AuthUser`.

**Files:**

- Create: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/service/social/dto/SocialAuthUser.java`
- Modify: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/service/social/SocialClientService.java`
- Modify: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/service/social/SocialClientServiceImpl.java`
- Modify: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/service/social/SocialUserServiceImpl.java`
- Modify: `modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/social/SocialUserServiceImplTest.java`
- Modify: `modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/social/SocialClientServiceImplTest.java`
- Modify: `docs/dependencies/fastjson2-removal-progress.md`

- [ ] **Step 1: Verify JustAuth is isolated to the adapter**

Run:

```bash
rg "me\\.zhyd\\.oauth\\.model\\.AuthUser|\\bAuthUser\\b" -n \
  modules/yudao-module-system-server/src/main/java \
  modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/social \
  --glob '!**/build/**'
```

Expected: `AuthUser` appears only in `SocialClientServiceImpl.java` and `SocialClientServiceImplTest.java`.

- [ ] **Step 2: Prove business social-user tests do not need fastjson**

Run:

```bash
./gradlew :modules:yudao-module-system-server:test \
  --tests '*SocialUserServiceImplTest' \
  -PexcludeFastjsonForTests=true \
  --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
Test result: SUCCESS
```

- [ ] **Step 3: Prove the current adapter still works with the default classpath**

Run:

```bash
./gradlew :modules:yudao-module-system-server:test \
  --tests '*SocialUserServiceImplTest' \
  --tests '*SocialClientServiceImplTest' \
  --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
Test result: SUCCESS
```

- [ ] **Step 4: Record the boundary evidence**

Add this row to `docs/dependencies/fastjson2-removal-progress.md` under `Phase Status`:

```markdown
| Business social-auth boundary isolated | Complete | `SocialUserServiceImplTest` passes with `-PexcludeFastjsonForTests=true`; JustAuth `AuthUser` remains only in the temporary adapter |
```

- [ ] **Step 5: Commit**

```bash
git add modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/service/social \
  modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/social \
  docs/dependencies/fastjson2-removal-progress.md
git commit -m "refactor: isolate justauth auth user model"
```

## Task 2: Add Project-Owned Social Auth Contracts

**Purpose:** Create the internal seam that replaces JustAuth request and model types.

**Files:**

- Create: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth/core/SocialAuthCallback.java`
- Create: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth/core/SocialAuthClientConfig.java`
- Create: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth/core/SocialAuthRequest.java`
- Create: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth/core/SocialAuthRequestFactory.java`
- Test: `modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/framework/socialauth/core/SocialAuthRequestFactoryTest.java`

- [ ] **Step 1: Add the callback DTO**

Create `SocialAuthCallback.java`:

```java
package cn.iocoder.yudao.module.system.framework.socialauth.core;

import lombok.Data;

@Data
public class SocialAuthCallback {

    private String code;
    private String state;

}
```

- [ ] **Step 2: Add the client config DTO**

Create `SocialAuthClientConfig.java`:

```java
package cn.iocoder.yudao.module.system.framework.socialauth.core;

import lombok.Data;

@Data
public class SocialAuthClientConfig {

    private String clientId;
    private String clientSecret;
    private String agentId;
    private String publicKey;
    private String redirectUri;
    private Boolean ignoreCheckRedirectUri;
    private Boolean ignoreCheckState;

}
```

- [ ] **Step 3: Add the provider request interface**

Create `SocialAuthRequest.java`:

```java
package cn.iocoder.yudao.module.system.framework.socialauth.core;

import cn.iocoder.yudao.module.system.service.social.dto.SocialAuthUser;

public interface SocialAuthRequest {

    String getSource();

    String authorize(String state);

    SocialAuthUser login(SocialAuthCallback callback);

}
```

- [ ] **Step 4: Add a factory test before implementation**

Create `SocialAuthRequestFactoryTest.java` with a test that registers one source and verifies unsupported sources fail fast:

```java
package cn.iocoder.yudao.module.system.framework.socialauth.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SocialAuthRequestFactoryTest {

    @Test
    void getShouldReturnRegisteredRequest() {
        SocialAuthRequest request = new FixedSocialAuthRequest();
        SocialAuthRequestFactory factory = new SocialAuthRequestFactory(Map.of("GITEE", request));

        assertSame(request, factory.get("GITEE"));
    }

    @Test
    void getShouldFailForUnsupportedSource() {
        SocialAuthRequestFactory factory = new SocialAuthRequestFactory(Map.of());

        assertThrows(IllegalArgumentException.class, () -> factory.get("UNKNOWN"));
    }

    private static final class FixedSocialAuthRequest implements SocialAuthRequest {
        @Override
        public String getSource() {
            return "GITEE";
        }

        @Override
        public String authorize(String state) {
            return "https://example.com?state=" + state;
        }

        @Override
        public cn.iocoder.yudao.module.system.service.social.dto.SocialAuthUser login(SocialAuthCallback callback) {
            return new cn.iocoder.yudao.module.system.service.social.dto.SocialAuthUser();
        }
    }

}
```

- [ ] **Step 5: Add the factory**

Create `SocialAuthRequestFactory.java`:

```java
package cn.iocoder.yudao.module.system.framework.socialauth.core;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;

import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor
public class SocialAuthRequestFactory {

    private final Map<String, SocialAuthRequest> requests;

    public SocialAuthRequest get(String source) {
        if (StrUtil.isBlank(source)) {
            throw new IllegalArgumentException("social auth source must not be blank");
        }
        SocialAuthRequest request = requests.get(source.toUpperCase(Locale.ROOT));
        if (request == null) {
            throw new IllegalArgumentException("unsupported social auth source: " + source);
        }
        return request;
    }

}
```

- [ ] **Step 6: Verify and commit**

Run:

```bash
./gradlew :modules:yudao-module-system-server:test --tests '*SocialAuthRequestFactoryTest' --rerun-tasks
git add modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth \
  modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/framework/socialauth
git commit -m "feat: add internal social auth contracts"
```

Expected:

```text
BUILD SUCCESSFUL
```

## Task 3: Bind Existing YAML To Project-Owned Properties

**Purpose:** Keep `justauth.*` configuration compatibility while deleting JustAuth classes.

**Files:**

- Create: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth/config/SocialAuthProperties.java`
- Create: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth/config/YudaoSocialAuthConfiguration.java`
- Test: `modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/framework/socialauth/config/SocialAuthPropertiesTest.java`

- [ ] **Step 1: Add properties binding test**

Create `SocialAuthPropertiesTest.java`:

```java
package cn.iocoder.yudao.module.system.framework.socialauth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialAuthPropertiesTest {

    @Test
    void shouldBindLegacyJustAuthPrefix() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "justauth.enabled", "true",
                "justauth.type.WECHAT_MP.client-id", "appid",
                "justauth.type.WECHAT_MP.client-secret", "secret",
                "justauth.type.WECHAT_MP.ignore-check-redirect-uri", "true",
                "justauth.cache.prefix", "social_auth_state:",
                "justauth.cache.timeout", "24h"
        ));

        SocialAuthProperties properties = new Binder(source)
                .bind("justauth", SocialAuthProperties.class)
                .orElseThrow();

        assertTrue(properties.getEnabled());
        assertEquals("appid", properties.getType().get("WECHAT_MP").getClientId());
        assertEquals("secret", properties.getType().get("WECHAT_MP").getClientSecret());
        assertTrue(properties.getType().get("WECHAT_MP").getIgnoreCheckRedirectUri());
        assertEquals("social_auth_state:", properties.getCache().getPrefix());
        assertEquals(Duration.ofHours(24), properties.getCache().getTimeout());
    }

}
```

- [ ] **Step 2: Add `SocialAuthProperties`**

Create `SocialAuthProperties.java`:

```java
package cn.iocoder.yudao.module.system.framework.socialauth.config;

import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthClientConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "justauth")
public class SocialAuthProperties {

    private Boolean enabled = true;
    private Map<String, SocialAuthClientConfig> type = new LinkedHashMap<>();
    private Cache cache = new Cache();

    @Data
    public static class Cache {
        private String prefix = "social_auth_state:";
        private Duration timeout = Duration.ofMinutes(3);
    }

}
```

- [ ] **Step 3: Add Spring configuration**

Create `YudaoSocialAuthConfiguration.java`:

```java
package cn.iocoder.yudao.module.system.framework.socialauth.config;

import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequest;
import cn.iocoder.yudao.module.system.framework.socialauth.core.SocialAuthRequestFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SocialAuthProperties.class)
public class YudaoSocialAuthConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "justauth", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SocialAuthRequestFactory socialAuthRequestFactory(List<SocialAuthRequest> requests) {
        Map<String, SocialAuthRequest> requestMap = requests.stream()
                .collect(Collectors.toMap(request -> request.getSource().toUpperCase(Locale.ROOT), Function.identity()));
        return new SocialAuthRequestFactory(requestMap);
    }

}
```

The final `SocialAuthRequestFactoryTest` must pin every supported `SocialTypeEnum#getSource()` key.

- [ ] **Step 4: Verify and commit**

Run:

```bash
./gradlew :modules:yudao-module-system-server:test --tests '*SocialAuthPropertiesTest' --rerun-tasks
git add modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth \
  modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/framework/socialauth
git commit -m "feat: bind social auth configuration"
```

Expected:

```text
BUILD SUCCESSFUL
```

## Task 4: Implement Provider Requests Without New Aggregate Dependencies

**Purpose:** Replace JustAuth behavior with provider-specific code owned by this project.

**Files:**

- Create: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth/core/AbstractOAuth2SocialAuthRequest.java`
- Create: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth/core/SocialAuthHttpClient.java`
- Create: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth/request/GiteeSocialAuthRequest.java`
- Create: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth/request/WechatMpSocialAuthRequest.java`
- Create: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth/request/WechatOpenSocialAuthRequest.java`
- Create: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth/request/WechatMiniProgramSocialAuthRequest.java`
- Create: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth/request/DingTalkSocialAuthRequest.java`
- Create: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth/request/WechatEnterpriseSocialAuthRequest.java`
- Create: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth/request/AlipayMiniProgramSocialAuthRequest.java`
- Test: `modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/framework/socialauth/request/*SocialAuthRequestTest.java`

- [ ] **Step 1: Land WeChat mini program first**

Use existing `WxMaService` instead of HTTP or a new SDK. The test should mock `WxMaService#getUserService()` and `WxMaUserService#getSessionInfo(code)`, then assert:

```text
uuid = openid
accessToken = sessionKey
rawTokenInfo is Jackson JSON
rawUserInfo is Jackson JSON
```

Run:

```bash
./gradlew :modules:yudao-module-system-server:test --tests '*WechatMiniProgramSocialAuthRequestTest' --rerun-tasks
```

- [ ] **Step 2: Land standard OAuth JSON providers**

Implement `GiteeSocialAuthRequest`, `WechatMpSocialAuthRequest`, and `WechatOpenSocialAuthRequest` on top of `AbstractOAuth2SocialAuthRequest`. Use `SocialAuthHttpClient` so tests can stub token and user-info JSON without hitting remote providers.

Each provider must return the exact `SocialTypeEnum#getSource()` value from `getSource()`: `GITEE`, `WECHAT_MP`, or `WECHAT_OPEN`.

Run:

```bash
./gradlew :modules:yudao-module-system-server:test \
  --tests '*GiteeSocialAuthRequestTest' \
  --tests '*WechatMpSocialAuthRequestTest' \
  --tests '*WechatOpenSocialAuthRequestTest' \
  --rerun-tasks
```

- [ ] **Step 3: Land non-standard providers**

Implement `DingTalkSocialAuthRequest`, `WechatEnterpriseSocialAuthRequest`, and `AlipayMiniProgramSocialAuthRequest` as separate classes. Keep their signing and response parsing code local to each class; do not generalize before two classes share identical code.

Each provider must return the exact `SocialTypeEnum#getSource()` value from `getSource()`: `DINGTALK`, `WECHAT_ENTERPRISE`, or `ALIPAY`.

Run:

```bash
./gradlew :modules:yudao-module-system-server:test \
  --tests '*DingTalkSocialAuthRequestTest' \
  --tests '*WechatEnterpriseSocialAuthRequestTest' \
  --tests '*AlipayMiniProgramSocialAuthRequestTest' \
  --rerun-tasks
```

- [ ] **Step 4: Verify all provider tests without fastjson**

Run:

```bash
./gradlew :modules:yudao-module-system-server:test \
  --tests 'cn.iocoder.yudao.module.system.framework.socialauth.*' \
  -PexcludeFastjsonForTests=true \
  --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 5: Commit**

```bash
git add modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/socialauth \
  modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/framework/socialauth
git commit -m "feat: replace justauth provider requests"
```

## Task 5: Switch `SocialClientServiceImpl` To Internal Social Auth

**Purpose:** Remove JustAuth from the application service path.

**Files:**

- Modify: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/service/social/SocialClientServiceImpl.java`
- Modify: `modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/social/SocialClientServiceImplTest.java`

- [ ] **Step 1: Replace `AuthRequestFactory` with `SocialAuthRequestFactory` in tests**

Update `SocialClientServiceImplTest` so `testGetAuthorizeUrl`, `testAuthSocialUser_success`, and `testAuthSocialUser_fail` mock `SocialAuthRequest` instead of JustAuth `AuthRequest`.

- [ ] **Step 2: Run the service test and confirm it fails before implementation**

Run:

```bash
./gradlew :modules:yudao-module-system-server:test --tests '*SocialClientServiceImplTest' -PexcludeFastjsonForTests=true --rerun-tasks
```

Expected: compile or test failure because `SocialClientServiceImpl` still uses JustAuth.

- [ ] **Step 3: Replace the adapter implementation**

In `SocialClientServiceImpl`:

- Replace `AuthRequestFactory authRequestFactory` with `SocialAuthRequestFactory socialAuthRequestFactory`.
- Replace `AuthStateUtils.createState()` with a project-owned state generator.
- Replace `AuthCallback.builder()` with `new SocialAuthCallback().setCode(code).setState(state)`.
- Replace the `AuthResponse` handling with direct `SocialAuthUser` handling.
- Replace `buildAuthRequest(...)` return type from JustAuth `AuthRequest` to internal `SocialAuthRequest`.

- [ ] **Step 4: Verify service tests without fastjson**

Run:

```bash
./gradlew :modules:yudao-module-system-server:test \
  --tests '*SocialClientServiceImplTest' \
  --tests '*SocialUserServiceImplTest' \
  -PexcludeFastjsonForTests=true \
  --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 5: Commit**

```bash
git add modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/service/social/SocialClientServiceImpl.java \
  modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/social/SocialClientServiceImplTest.java
git commit -m "refactor: use internal social auth requests"
```

## Task 6: Delete JustAuth Code And Dependencies

**Purpose:** Remove the dependency that currently keeps fastjson on system runtime classpaths.

**Files:**

- Delete: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/justauth/config/YudaoJustAuthConfiguration.java`
- Delete: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/justauth/core/AuthRequestFactory.java`
- Delete: `modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework/justauth/package-info.java`
- Modify: `modules/yudao-module-system-server/build.gradle`
- Modify: `gradle/libs.versions.toml`
- Modify: `build-logic/src/main/groovy/yudao-java.gradle`
- Modify: `docs/dependencies/fastjson2-removal-progress.md`
- Modify: `docs/testing/agent-verification.md`

- [ ] **Step 1: Remove JustAuth dependencies**

In `modules/yudao-module-system-server/build.gradle`, delete:

```groovy
implementation libs.justauth
implementation(libs.justauth.starter) {
    exclude group: 'cn.hutool', module: 'hutool-core'
}
```

In `gradle/libs.versions.toml`, delete:

```toml
justauth = "1.16.7"
justauth-starter = "1.4.0"
justauth = { module = "me.zhyd.oauth:JustAuth", version.ref = "justauth" }
justauth-starter = { module = "com.xkcoding.justauth:justauth-spring-boot-starter", version.ref = "justauth-starter" }
```

- [ ] **Step 2: Remove the runtime exception list**

In `build-logic/src/main/groovy/yudao-java.gradle`, remove `fastjsonRuntimeRequiredProjects` and make the runtime exclusion unconditional for all projects:

```groovy
configurations.matching { it.name.endsWith('RuntimeClasspath') || it.name == 'runtimeClasspath' }.configureEach {
    excludeFastjson(it)
}
```

- [ ] **Step 3: Delete the copied JustAuth framework package**

Use `apply_patch` to delete the three files listed in this task's file list. Do not leave an empty `framework/justauth` package.

- [ ] **Step 4: Verify no JustAuth source/build references remain**

Run:

```bash
rg "me\\.zhyd\\.oauth|com\\.xkcoding\\.justauth|AuthRequestFactory|libs\\.justauth|justauth-starter" -n \
  modules gradle build-logic \
  --glob '!**/build/**'
```

Expected: no output.

- [ ] **Step 5: Verify and commit**

Run:

```bash
./gradlew :modules:yudao-module-system-server:agentQuick -PexcludeFastjsonForTests=true --rerun-tasks
git add modules/yudao-module-system-server/build.gradle gradle/libs.versions.toml build-logic/src/main/groovy/yudao-java.gradle \
  docs/dependencies/fastjson2-removal-progress.md docs/testing/agent-verification.md \
  modules/yudao-module-system-server/src/main/java/cn/iocoder/yudao/module/system/framework
git commit -m "build: remove justauth and fastjson runtime exception"
```

Expected:

```text
BUILD SUCCESSFUL
```

## Task 7: Add Resolved Runtime Dependency Guard

**Purpose:** Prevent a future transitive dependency from silently reintroducing fastjson.

**Files:**

- Modify: `build-logic/src/main/groovy/yudao-java.gradle`
- Test by running Gradle tasks, no Java test file required.

- [ ] **Step 1: Add a resolved runtime audit task**

Add a task named `verifyNoResolvedFastjsonRuntimeDependencies` that scans resolved artifacts from runtime-like configurations and fails if any module is:

```text
com.alibaba:fastjson
com.alibaba.fastjson2:fastjson2
com.alibaba.fastjson2:fastjson2-extension
```

Attach it to `agentDependencyAudit`.

- [ ] **Step 2: Run the audit**

Run:

```bash
./gradlew agentDependencyAudit
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 3: Commit**

```bash
git add build-logic/src/main/groovy/yudao-java.gradle
git commit -m "build: block resolved fastjson runtime dependencies"
```

## Task 8: Final Full Verification

**Purpose:** Prove the deletion is real at source, dependency, test, and packaged artifact levels.

- [ ] **Step 1: Run system agent verification**

```bash
./gradlew :modules:yudao-module-system-server:agentVerify --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 2: Build runnable jars**

```bash
./gradlew :modules:yudao-server:bootJar :modules:yudao-gateway:bootJar :modules:yudao-module-system-server:bootJar :modules:yudao-module-infra-server:bootJar
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 3: Scan boot jars**

```bash
for jar in \
  modules/yudao-server/build/libs/yudao-server.jar \
  modules/yudao-gateway/build/libs/yudao-gateway.jar \
  modules/yudao-module-system-server/build/libs/yudao-module-system-server.jar \
  modules/yudao-module-infra-server/build/libs/yudao-module-infra-server.jar
do
  echo "$jar"
  jar tf "$jar" | rg "BOOT-INF/lib/(fastjson|fastjson2|JustAuth|justauth)" || true
done
```

Expected: each jar path prints, with no matching library entries under it.

- [ ] **Step 4: Scan source and build files**

```bash
rg "me\\.zhyd\\.oauth|com\\.xkcoding\\.justauth|libs\\.justauth|justauth-starter|com\\.alibaba\\.fastjson|com\\.alibaba\\.fastjson2" -n \
  modules gradle build-logic \
  --glob '!**/build/**'
```

Expected: no output.

- [ ] **Step 5: Update progress docs and commit**

Set these rows in `docs/dependencies/fastjson2-removal-progress.md`:

```markdown
| Test runtime exclusion experiment | Complete | `agentVerify` passes without fastjson on the normal test runtime |
| Production runtime exclusion experiment | Complete | all app boot jars scan clean for fastjson and JustAuth artifacts |
| Default runtime exclusion | Complete | fastjson runtime exclusion is unconditional across all projects |
| JustAuth replacement | Complete | no source or build references to JustAuth remain |
```

Then run:

```bash
git add docs/dependencies/fastjson2-removal-progress.md docs/testing/agent-verification.md
git commit -m "docs: record fastjson final deletion evidence"
```
