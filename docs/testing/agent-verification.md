# Agent Verification

This repo uses layered verification for AI Agent changes. Start with the narrowest gate that matches the change, then escalate only when the changed behavior crosses a wider boundary.

## Gates

| Gate | Command | Use when |
|---|---|---|
| Quick | `./gradlew :modules:yudao-module-system-server:agentQuick --rerun-tasks` | Service, mapper-free, utility, or lightweight Spring changes |
| DB | `./gradlew :modules:yudao-module-system-server:agentDb --rerun-tasks` | Mapper, SQL, type handler, transaction, tenant, or persistence behavior changes |
| API | `./gradlew :modules:yudao-module-system-server:agentApi --rerun-tasks` | HTTP contract, controller serialization, auth/security, or filter changes |
| Full | `./gradlew :modules:yudao-module-system-server:agentVerify` | Before handing off a completed feature or risky fix |

For framework test-starter changes, also run:

```bash
./gradlew :modules:yudao-spring-boot-starter-test:agentQuick --rerun-tasks
```

## Failure Triage

1. Read the first failing test name from Gradle output.
2. Open the printed HTML report for stack traces and grouped failures.
3. Re-run only the failing class or method.
4. Classify the failure before editing:
   - compile failure: fix type, import, dependency, or generated source issue first.
   - test setup failure: fix missing mock, test data, SQL setup, or Spring context configuration.
   - assertion failure: inspect actual behavior and update implementation or test expectation.
   - container failure: confirm Docker is running, then re-run the DB gate once.
5. After the focused fix passes, re-run the original gate that failed.

## Report Locations

Each test task prints:

- `HTML report: .../build/reports/tests/<task>`
- `XML results: .../build/test-results/<task>`

Use the HTML report for human debugging and XML results for machine parsing.

## Dependency Reduction

Fastjson is excluded from runtime and test runtime classpaths by default.

Run the dependency guard:

```bash
./gradlew :modules:yudao-module-system-server:agentDependencyAudit
```

Run the full system-server agent verification when touching the social-auth or JSON boundary:

```bash
./gradlew :modules:yudao-module-system-server:agentVerify --rerun-tasks
```

Build production jars and scan them for removed JSON/auth libraries:

```bash
./gradlew :modules:yudao-server:bootJar :modules:yudao-gateway:bootJar :modules:yudao-module-system-server:bootJar :modules:yudao-module-infra-server:bootJar
for jar in modules/yudao-server/build/libs/yudao-server.jar modules/yudao-gateway/build/libs/yudao-gateway.jar modules/yudao-module-system-server/build/libs/yudao-module-system-server.jar modules/yudao-module-infra-server/build/libs/yudao-module-infra-server.jar; do
  jar tf "$jar" | rg "BOOT-INF/lib/(fastjson|fastjson2|JustAuth|justauth)" || true
done
```

## Rules For Agents

- Do not claim success without a passing command and the gate name.
- Prefer focused re-runs while repairing.
- Run `agentQuick` before `agentDb` unless the change is DB-only and the target DB test is known.
- Run `agentVerify` before final handoff when the change touches shared test infrastructure.
- Keep new DB integration tests tagged through `BaseDbIntegrationTest`.
- Keep module-owned schema setup in module-owned test bases such as `BaseSystemDbIntegrationTest`.
