# Fastjson2 Removal Progress

## Target

Remove these artifacts from all production and test runtime classpaths:

- `com.alibaba:fastjson`
- `com.alibaba.fastjson2:fastjson2`
- `com.alibaba.fastjson2:fastjson2-extension`

## Current Runtime Requesters

| Requester | Current role | Removal risk |
|---|---|---|
| `com.alibaba.cloud:spring-cloud-alibaba-dependencies` | BOM-managed constraint path for Alibaba Cloud stack | Medium: must verify Nacos/Gateway/System/Infra boot jars |
| `me.zhyd.oauth:JustAuth` | Social login support | Medium: must verify social auth service tests under no-fastjson runtime |
| `com.fhs-opensource:easy-trans-*` | Translation annotations and MyBatis extension | Medium: must verify MyBatis starter and DB tests under no-fastjson runtime |

## Phase Status

| Phase | Status | Evidence |
|---|---|---|
| Source fastjson usage removed | Complete | No direct `com.alibaba.fastjson*` imports found |
| Direct Gradle fastjson declarations removed | Complete | `gradle/libs.versions.toml` no longer declares `fastjson`; `rg` finds no build references |
| Test runtime exclusion experiment | Blocked for system module | `yudao-common:test` and `yudao-spring-boot-starter-mybatis:test` pass with `-PexcludeFastjsonForTests=true`; `yudao-module-system-server:agentVerify` fails because JustAuth `AuthUser` declares `com.alibaba.fastjson.JSONObject rawUserInfo` |
| Production runtime exclusion experiment | Partial | `yudao-common`, `yudao-spring-boot-starter-mybatis`, `yudao-gateway`, and `yudao-module-infra-server` runtime dependency insight is clean by default; `yudao-module-system-server` and `yudao-server` remain blocked by JustAuth |
| Default runtime exclusion | Partial | Fastjson is excluded by default outside `:modules:yudao-module-system-server` and `:modules:yudao-server`; gateway and infra boot jars contain no `fastjson`, `fastjson2`, or `fastjson2-extension` jars |

## Known Blocker

JustAuth 1.16.7 exposes fastjson in its public model:

```text
me.zhyd.oauth.model.AuthUser
private com.alibaba.fastjson.JSONObject rawUserInfo;
public com.alibaba.fastjson.JSONObject getRawUserInfo();
public void setRawUserInfo(com.alibaba.fastjson.JSONObject);
```

Maven metadata shows `me.zhyd.oauth:JustAuth` release `1.16.7` and `com.xkcoding.justauth:justauth-spring-boot-starter` release `1.4.0`, which are the versions already used by this project. Full removal therefore requires replacing the JustAuth integration or introducing an internal social-auth boundary that no longer exposes `AuthUser`.
