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
| Test runtime exclusion experiment | Pending | Not run yet |
| Production runtime exclusion experiment | Pending | Not run yet |
| Default runtime exclusion | Pending | Not implemented yet |
