# Fastjson2 Removal Progress

## Target

Remove these artifacts from all production and test runtime classpaths:

- `com.alibaba:fastjson`
- `com.alibaba.fastjson2:fastjson2`
- `com.alibaba.fastjson2:fastjson2-extension`

## Former Runtime Requesters

| Requester | Resolution | Evidence |
|---|---|---|
| `com.alibaba.cloud:spring-cloud-alibaba-dependencies` | Kept as the Spring Cloud Alibaba BOM, but fastjson artifacts are excluded from runtime classpaths | Four boot jars scan clean for `fastjson`, `fastjson2`, and `fastjson2-extension` |
| `me.zhyd.oauth:JustAuth` | Removed from Gradle dependencies and replaced with project-owned social auth requests | `rg` finds no `me.zhyd.oauth`, `com.xkcoding.justauth`, `libs.justauth`, or `justauth-starter` references under `modules`, `gradle`, and `build-logic` |
| `com.fhs-opensource:easy-trans-*` | Kept for translation features, with fastjson artifacts excluded from runtime classpaths | `agentVerify` passes, including `agentDb`, and resolved runtime dependency guard passes |

## Phase Status

| Phase | Status | Evidence |
|---|---|---|
| Source fastjson usage removed | Complete | No direct `com.alibaba.fastjson*` imports found |
| Direct Gradle fastjson declarations removed | Complete | `gradle/libs.versions.toml` no longer declares `fastjson`; `rg` finds no build references |
| Business social-auth boundary isolated | Complete | Business code uses `SocialAuthUser` and internal `SocialAuthRequest`; JustAuth package is deleted |
| Test runtime exclusion experiment | Complete | `agentVerify` passes without `-PexcludeFastjsonForTests=true` |
| Production runtime exclusion experiment | Complete | System, monolith, gateway, and infra boot jars build and scan clean without `-PexcludeFastjsonRuntime=true` |
| Default runtime exclusion | Complete | Fastjson artifacts are excluded from all runtime-like classpaths by default |
| Runtime dependency guard | Complete | `agentDependencyAudit` runs `verifyNoFastjsonUsage`, `verifyNoDirectFastjsonDependencies`, and `verifyNoResolvedFastjsonRuntimeDependencies` |

## Final Evidence

- `./gradlew :modules:yudao-module-system-server:agentVerify --rerun-tasks` -> `BUILD SUCCESSFUL`; normal tests: 510 total, 501 passed, 9 skipped; `agentDb`: 2 passed; `agentApi`: 0 tests.
- `./gradlew :modules:yudao-server:bootJar :modules:yudao-gateway:bootJar :modules:yudao-module-system-server:bootJar :modules:yudao-module-infra-server:bootJar` -> `BUILD SUCCESSFUL`.
- Boot jar scan over `yudao-server.jar`, `yudao-gateway.jar`, `yudao-module-system-server.jar`, and `yudao-module-infra-server.jar` produced no `BOOT-INF/lib/(fastjson|fastjson2|JustAuth|justauth)` matches.
- Source/build scan produced no JustAuth, JustAuth starter, Alipay SDK, or BouncyCastle references under `modules`, `gradle`, and `build-logic`.
- Fastjson import scan only matches the build guard in `build-logic/src/main/groovy/yudao-java.gradle`.

## Resolved Blocker

JustAuth 1.16.7 exposed fastjson in its public model:

```text
me.zhyd.oauth.model.AuthUser
private com.alibaba.fastjson.JSONObject rawUserInfo;
public com.alibaba.fastjson.JSONObject getRawUserInfo();
public void setRawUserInfo(com.alibaba.fastjson.JSONObject);
```

The blocker is resolved by deleting JustAuth and `justauth-spring-boot-starter`, then replacing the integration with the internal `framework/socialauth` boundary. The project keeps the existing `justauth.*` configuration prefix for compatibility, but no longer depends on JustAuth artifacts.
