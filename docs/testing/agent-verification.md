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

## Dependency Reduction Experiments

Fastjson removal experiments are opt-in until the production runtime classpath is proven safe.

Run test runtime without fastjson:

```bash
./gradlew :modules:yudao-module-system-server:agentVerify -PexcludeFastjsonForTests=true --rerun-tasks
```

Run production runtime classpath checks without fastjson:

```bash
./gradlew :modules:yudao-server:bootJar :modules:yudao-gateway:bootJar :modules:yudao-module-system-server:bootJar :modules:yudao-module-infra-server:bootJar -PexcludeFastjsonRuntime=true
```

Do not make `excludeFastjsonRuntime` the default until `docs/dependencies/fastjson2-removal-progress.md` records passing evidence for JustAuth, Easy-Trans, Nacos-backed apps, and `agentVerify`.

## Rules For Agents

- Do not claim success without a passing command and the gate name.
- Prefer focused re-runs while repairing.
- Run `agentQuick` before `agentDb` unless the change is DB-only and the target DB test is known.
- Run `agentVerify` before final handoff when the change touches shared test infrastructure.
- Keep new DB integration tests tagged through `BaseDbIntegrationTest`.
- Keep module-owned schema setup in module-owned test bases such as `BaseSystemDbIntegrationTest`.
