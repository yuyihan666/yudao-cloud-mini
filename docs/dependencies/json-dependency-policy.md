# JSON Dependency Policy

## Decision

Jackson is the only first-party JSON engine in this project.

First-party code must use one of these entry points:

- `cn.iocoder.yudao.framework.common.util.json.JsonUtils`
- Spring-managed `com.fasterxml.jackson.databind.ObjectMapper`
- Framework integrations that are already Jackson-backed, such as MyBatis-Plus `JacksonTypeHandler`

First-party code must not import:

- `com.alibaba.fastjson.*`
- `com.alibaba.fastjson2.*`

## Why

The framework already routes HTTP JSON, utility JSON parsing, MyBatis JSON columns, MQ payloads, WebSocket payloads, and Excel JSON conversion through Jackson-backed APIs. Keeping another JSON engine in application code increases runtime behavior differences, supply-chain surface area, and AI-agent debugging noise.

## Third-Party Boundary

Fastjson may appear only as a temporary transitive dependency while existing third-party libraries still require it. The project goal is to remove it from runtime classpaths entirely. Until that is complete, every remaining fastjson path must be visible in `docs/dependencies/fastjson2-removal-progress.md`.

## Agent Rule

Any agent changing JSON code must prefer `JsonUtils` or `ObjectMapper`. Adding a direct fastjson dependency or import is a build failure.
