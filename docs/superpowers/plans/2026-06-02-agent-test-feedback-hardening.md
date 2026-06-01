# Agent Test Feedback Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use verification-before-completion before claiming any task in this plan is complete. Use systematic-debugging for any failing verification command before changing production or test code.

**Goal:** Harden the first delivered AI Agent test feedback loop so an agent can run fast checks, escalate to real MySQL checks, observe exact failure artifacts, and repair failures without guessing.

**Architecture:** Keep the global test framework minimal and reusable. Add module-level ownership for real MySQL schema setup, make Gradle agent gates deterministic, strengthen the MySQL sample test so it proves behavior H2 cannot reliably prove, and document the agent verification protocol next to the repo.

**Tech Stack:** Gradle convention plugin, JUnit 5, Spring Test `@Sql`, Spring Boot Testcontainers `@ServiceConnection`, MySQL Testcontainers, existing Yudao test utilities.

---

## Current State

The first implementation already added the core pieces:

- `agentQuick`, `agentDb`, `agentApi`, `agentVerify` Gradle verification tasks.
- Test result logging with HTML and XML report paths.
- `BaseDbIntegrationTest` for MySQL Testcontainers.
- `BaseControllerTest` for controller slice tests.
- Deterministic `TestDataBuilder`.
- A first system-module MySQL integration sample for tenant packages.

The immediate hardening work should not replace that implementation. It should tighten the parts that matter most to AI Agent repair loops:

- Execution order must be obvious and stable.
- DB schema setup must have one module-owned place to change.
- The real-MySQL sample must catch real database behavior, not only prove insert/select.
- The agent must have a short failure-triage document with exact commands and report paths.

## Non-Goals

- Do not migrate all existing H2 tests to MySQL.
- Do not introduce a custom test runner.
- Do not rewrite the whole test hierarchy.
- Do not clear every legacy compiler warning in this pass.
- Do not add broad abstractions before there are at least two module users.

## File Structure

Modify:

```text
build-logic/src/main/groovy/yudao-java.gradle
modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/tenant/TenantPackageDbIntegrationTest.java
```

Create:

```text
modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/framework/test/BaseSystemDbIntegrationTest.java
docs/testing/agent-verification.md
```

## Task 1: Make Agent Gate Ordering Deterministic

**Purpose:** Ensure `agentVerify` always runs gates in the order an AI Agent should reason about failures: quick first, DB second, API last.

### Implementation

Edit `build-logic/src/main/groovy/yudao-java.gradle`.

Keep the existing task names and descriptions. Add explicit ordering between agent gates:

```groovy
tasks.register('agentQuick', Test) {
    description = 'Fast AI agent verification: unit and lightweight Spring tests, excluding real DB and API integration tests.'
    group = 'verification'

    useJUnitPlatform {
        excludeTags 'db-integration', 'api-integration', 'slow'
    }

    shouldRunAfter tasks.named('test')
}
```

```groovy
tasks.register('agentDb', Test) {
    description = 'Real database AI agent verification: tests tagged db-integration.'
    group = 'verification'

    useJUnitPlatform {
        includeTags 'db-integration'
    }

    mustRunAfter tasks.named('agentQuick')
}
```

```groovy
tasks.register('agentApi', Test) {
    description = 'API AI agent verification: tests tagged api-integration.'
    group = 'verification'

    useJUnitPlatform {
        includeTags 'api-integration'
    }

    mustRunAfter tasks.named('agentDb')
}
```

Keep `agentVerify` as the aggregate gate:

```groovy
tasks.register('agentVerify') {
    description = 'Complete AI agent verification: quick + real DB + API integration gates.'
    group = 'verification'

    dependsOn tasks.named('agentQuick')
    dependsOn tasks.named('agentDb')
    dependsOn tasks.named('agentApi')
}
```

### Verification

Run:

```bash
./gradlew :modules:yudao-module-system-server:agentVerify --dry-run
```

Expected signal:

```text
:modules:yudao-module-system-server:test SKIPPED
:modules:yudao-module-system-server:agentQuick SKIPPED
:modules:yudao-module-system-server:agentDb SKIPPED
:modules:yudao-module-system-server:agentApi SKIPPED
:modules:yudao-module-system-server:agentVerify SKIPPED
```

If `agentDb` appears before `agentQuick`, stop and fix task ordering before continuing.

### Commit

```bash
git add build-logic/src/main/groovy/yudao-java.gradle
git commit -m "test: make agent verification gate order explicit"
```

## Task 2: Centralize System Module MySQL Schema Setup

**Purpose:** Remove duplicated schema/cleanup annotations from individual DB tests while keeping schema ownership inside the module that owns the tables.

### Implementation

Create `modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/framework/test/BaseSystemDbIntegrationTest.java`:

```java
package cn.iocoder.yudao.module.system.framework.test;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbIntegrationTest;
import org.springframework.test.context.jdbc.Sql;

/**
 * System module base class for real MySQL integration tests.
 *
 * <p>The shared framework base starts MySQL. This module base owns system-module schema
 * setup and cleanup so individual DB tests do not duplicate SQL annotations.</p>
 */
@Sql(scripts = "/sql/mysql/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/mysql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public abstract class BaseSystemDbIntegrationTest extends BaseDbIntegrationTest {
}
```

Update `TenantPackageDbIntegrationTest`:

```java
import cn.iocoder.yudao.module.system.framework.test.BaseSystemDbIntegrationTest;
```

Change:

```java
class TenantPackageDbIntegrationTest extends BaseDbIntegrationTest {
```

To:

```java
class TenantPackageDbIntegrationTest extends BaseSystemDbIntegrationTest {
```

Remove local `@Sql` annotations and unused imports from `TenantPackageDbIntegrationTest`.

### Verification

Run:

```bash
./gradlew :modules:yudao-module-system-server:agentDb \
  --tests "cn.iocoder.yudao.module.system.service.tenant.TenantPackageDbIntegrationTest" \
  --rerun-tasks
```

Expected signal:

```text
Test result: SUCCESS (1 tests, 1 passed, 0 failed, 0 skipped)
```

The exact report paths may differ by local Gradle build directory, but the task must print both HTML and XML locations.

### Commit

```bash
git add modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/framework/test/BaseSystemDbIntegrationTest.java
git add modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/tenant/TenantPackageDbIntegrationTest.java
git commit -m "test: centralize system MySQL test schema setup"
```

## Task 3: Make The MySQL Sample Prove Database-Specific Behavior

**Purpose:** A real MySQL gate should catch behavior that is easy to miss with pure mocks or H2. The first high-value assertion is logical delete filtering through MyBatis-Plus against the real MySQL schema.

### Implementation

In `TenantPackageDbIntegrationTest`, add a second test:

```java
@Test
void shouldFilterLogicallyDeletedTenantPackage() {
    TenantPackageDO tenantPackage = TenantPackageTestData.tenantPackage()
            .name("逻辑删除套餐")
            .build();
    tenantPackageMapper.insert(tenantPackage);

    tenantPackageMapper.deleteById(tenantPackage.getId());

    assertNull(tenantPackageMapper.selectById(tenantPackage.getId()));
}
```

Add the static import:

```java
import static org.junit.jupiter.api.Assertions.assertNull;
```

Keep the existing permission-menu round-trip test. Together the two tests cover:

- MySQL container boot and Spring datasource binding.
- Real table DDL compatibility.
- Jackson type handler round-trip for `Set<Long>`.
- MyBatis-Plus logical delete behavior against the real schema.

### Verification

Run:

```bash
./gradlew :modules:yudao-module-system-server:agentDb \
  --tests "cn.iocoder.yudao.module.system.service.tenant.TenantPackageDbIntegrationTest" \
  --rerun-tasks
```

Expected signal:

```text
Test result: SUCCESS (2 tests, 2 passed, 0 failed, 0 skipped)
```

### Commit

```bash
git add modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/tenant/TenantPackageDbIntegrationTest.java
git commit -m "test: strengthen MySQL tenant package integration coverage"
```

## Task 4: Add Agent Verification Runbook

**Purpose:** Give future AI Agents the exact commands, escalation path, and artifact locations so test failures become observable and repairable.

### Implementation

Create `docs/testing/agent-verification.md`:

```markdown
# Agent Verification

This repo uses layered verification for AI Agent changes. Start with the narrowest gate that matches the change, then escalate only when the changed behavior crosses a wider boundary.

## Gates

| Gate | Command | Use when |
|---|---|---|
| Quick | `./gradlew :modules:yudao-module-system-server:agentQuick --rerun-tasks` | Service, mapper-free, utility, or lightweight Spring changes |
| DB | `./gradlew :modules:yudao-module-system-server:agentDb --rerun-tasks` | Mapper, SQL, type handler, transaction, tenant, or persistence behavior changes |
| API | `./gradlew :modules:yudao-module-system-server:agentApi --rerun-tasks` | HTTP contract, controller serialization, auth/security, or filter changes |
| Full | `./gradlew :modules:yudao-module-system-server:agentVerify` | Before handing off a completed feature or risky fix |

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

- `HTML report: .../build/reports/tests/<task>/index.html`
- `XML results: .../build/test-results/<task>`

Use the HTML report for human debugging and XML results for machine parsing.

## Rules For Agents

- Do not claim success without a passing command and the gate name.
- Prefer focused re-runs while repairing.
- Run `agentQuick` before `agentDb` unless the change is DB-only and the target DB test is known.
- Run `agentVerify` before final handoff when the change touches shared test infrastructure.
- Keep new DB integration tests tagged through `BaseDbIntegrationTest`.
```

### Verification

Run:

```bash
test -f docs/testing/agent-verification.md
rg "agentQuick|agentDb|agentApi|agentVerify" docs/testing/agent-verification.md
```

Expected signal:

```text
docs/testing/agent-verification.md:# Agent Verification
```

And the four gate names must appear in the command matrix.

### Commit

```bash
git add docs/testing/agent-verification.md
git commit -m "docs: add agent verification runbook"
```

## Task 5: Final Verification

Run the final gate sequence from the repo root:

```bash
./gradlew tasks --group verification --warning-mode all
./gradlew :modules:yudao-spring-boot-starter-test:agentQuick --rerun-tasks
./gradlew :modules:yudao-module-system-server:agentQuick --rerun-tasks
./gradlew :modules:yudao-module-system-server:agentDb \
  --tests "cn.iocoder.yudao.module.system.service.tenant.TenantPackageDbIntegrationTest" \
  --rerun-tasks
./gradlew :modules:yudao-module-system-server:agentVerify
```

Expected final signal:

- Verification tasks are visible under Gradle's `verification` group.
- Starter-test `agentQuick` passes.
- System-server `agentQuick` passes.
- Targeted system-server `agentDb` passes with 2 tenant package DB tests.
- System-server `agentVerify` passes.

If warnings remain, classify them in the final report:

- Build-breaking or Gradle deprecation warnings: fix before handoff.
- Legacy annotation processor or third-party runtime warnings: document as residual noise and do not expand scope unless they hide test failures.

## Self-Review Checklist

- Every code change maps to one of the four hardening goals.
- No production code changed.
- No broad framework abstraction was added before another module needs it.
- `BaseDbIntegrationTest` remains module-agnostic.
- System module SQL ownership lives in the system test tree.
- The MySQL sample verifies at least one behavior that matters for real DB compatibility.
- Final response names the exact verification commands that passed and any residual warnings.

