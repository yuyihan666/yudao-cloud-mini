# Fastjson2 Removal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove `fastjson` / `fastjson2` from project runtime classpaths while keeping Jackson as the only first-party JSON engine.

**Architecture:** First-party code uses Jackson through `JsonUtils`, Spring `ObjectMapper`, and MyBatis-Plus `JacksonTypeHandler`. Fastjson is treated as an unwanted transitive third-party dependency: first guard against direct usage, then run opt-in exclusion experiments, then add proof tests, and only then make runtime exclusion the default. Every phase must leave an agent-verifiable signal.

**Tech Stack:** Gradle convention plugins, Java 25, Spring Boot 3.5.x, Jackson, JUnit 5, current `agentQuick` / `agentDb` / `agentApi` / `agentVerify` gates.

---

## Current Baseline

The current source baseline is already Jackson-first:

- `modules/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/util/json/JsonUtils.java` owns JSON serialization and parsing through Jackson `ObjectMapper`.
- `modules/yudao-spring-boot-starter-mybatis/src/main/java/cn/iocoder/yudao/framework/mybatis/config/YudaoMybatisAutoConfiguration.java` wires MyBatis-Plus `JacksonTypeHandler` to the Spring `ObjectMapper`.
- There are no direct `com.alibaba.fastjson*` imports in application source.
- `gradle/libs.versions.toml` still contains an unused `fastjson = "1.2.83"` version entry.

The current runtime dependency baseline still contains fastjson:

```text
com.alibaba:fastjson:2.0.58
com.alibaba.fastjson2:fastjson2:2.0.58
com.alibaba.fastjson2:fastjson2-extension:2.0.58
```

Known transitive requesters:

```text
com.alibaba.cloud:spring-cloud-alibaba-dependencies:2025.0.0.0
me.zhyd.oauth:JustAuth:1.16.7
com.fhs-opensource:easy-trans-anno:3.0.6
com.fhs-opensource:easy-trans-service:3.0.6
```

## Success Definition

The final state is complete only when all of these are true:

- No first-party source imports `com.alibaba.fastjson` or `com.alibaba.fastjson2`.
- No first-party Gradle dependency directly declares `com.alibaba:fastjson`, `com.alibaba.fastjson2:fastjson2`, or `com.alibaba.fastjson2:fastjson2-extension`.
- Runtime classpaths for `yudao-common`, `yudao-spring-boot-starter-mybatis`, `yudao-module-system-server`, `yudao-module-infra-server`, `yudao-server`, and `yudao-gateway` do not resolve fastjson artifacts.
- `agentVerify` still passes for the system module.
- Boot jars still build for monolith, gateway, system, and infra apps.
- A classpath test proves `com.alibaba.fastjson.JSON` and `com.alibaba.fastjson2.JSON` are unavailable.

## Non-Goals

- Do not replace Jackson.
- Do not remove Hutool JSON usage in this plan; Hutool is a separate dependency-reduction topic.
- Do not rewrite JustAuth, Easy-Trans, or Nacos integrations before a no-fastjson runtime experiment proves which one actually blocks removal.
- Do not keep a permanent feature flag for fastjson exclusion. The flag is only a migration tool.

## File Structure

Modify:

```text
build-logic/src/main/groovy/yudao-java.gradle
gradle/libs.versions.toml
docs/testing/agent-verification.md
```

Create:

```text
docs/dependencies/json-dependency-policy.md
docs/dependencies/fastjson2-removal-progress.md
modules/yudao-common/src/test/java/cn/iocoder/yudao/framework/common/util/json/FastjsonClasspathTest.java
```

## Task 1: Document The JSON Dependency Policy

**Purpose:** Make the architectural decision explicit before touching dependency behavior.

**Files:**

- Create: `docs/dependencies/json-dependency-policy.md`
- Create: `docs/dependencies/fastjson2-removal-progress.md`

- [ ] **Step 1: Create the dependency docs directory**

Run:

```bash
mkdir -p docs/dependencies
```

- [ ] **Step 2: Add the JSON policy document**

Create `docs/dependencies/json-dependency-policy.md`:

```markdown
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
```

- [ ] **Step 3: Add the removal progress baseline**

Create `docs/dependencies/fastjson2-removal-progress.md`:

```markdown
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
| Direct Gradle fastjson declarations removed | Pending | `gradle/libs.versions.toml` still has unused `fastjson` version |
| Test runtime exclusion experiment | Pending | Not run yet |
| Production runtime exclusion experiment | Pending | Not run yet |
| Default runtime exclusion | Pending | Not implemented yet |
```

- [ ] **Step 4: Commit**

```bash
git add docs/dependencies/json-dependency-policy.md docs/dependencies/fastjson2-removal-progress.md
git commit -m "docs: define json dependency policy"
```

## Task 2: Add Agent-Visible Fastjson Guardrails

**Purpose:** Prevent regression while the transitive dependency is being removed.

**Files:**

- Modify: `build-logic/src/main/groovy/yudao-java.gradle`

- [ ] **Step 1: Add source and direct dependency audit tasks**

Edit `build-logic/src/main/groovy/yudao-java.gradle`. Insert this block after the existing `configurations.configureEach` block and before the `tasks.withType(Test).configureEach` block:

```groovy
// ========== JSON dependency guardrails ==========
def isDirectFastjsonDependency = { dep ->
    (dep.group == 'com.alibaba' && dep.name == 'fastjson') ||
            (dep.group == 'com.alibaba.fastjson2' && dep.name in ['fastjson2', 'fastjson2-extension'])
}

tasks.register('verifyNoFastjsonUsage') {
    group = 'verification'
    description = 'Fails if first-party source imports Alibaba fastjson APIs.'

    doLast {
        def sourceFiles = fileTree(project.projectDir) {
            include 'src/main/java/**/*.java'
            include 'src/test/java/**/*.java'
            exclude '**/build/**'
        }
        def violations = []
        sourceFiles.files.each { sourceFile ->
            sourceFile.eachLine { line, number ->
                def trimmed = line.trim()
                if (trimmed.startsWith('import com.alibaba.fastjson.') ||
                        trimmed == 'import com.alibaba.fastjson;' ||
                        trimmed.startsWith('import com.alibaba.fastjson2.') ||
                        trimmed == 'import com.alibaba.fastjson2;') {
                    violations << "${project.path}:${project.relativePath(sourceFile)}:${number}: ${trimmed}"
                }
            }
        }
        if (!violations.isEmpty()) {
            throw new GradleException("Direct fastjson usage is forbidden. Use JsonUtils or ObjectMapper instead:\n" + violations.join('\n'))
        }
    }
}

tasks.register('verifyNoDirectFastjsonDependencies') {
    group = 'verification'
    description = 'Fails if a project directly declares Alibaba fastjson dependencies.'

    doLast {
        def violations = []
        configurations.each { configuration ->
            configuration.dependencies.each { dependency ->
                if (isDirectFastjsonDependency(dependency)) {
                    violations << "${project.path}:${configuration.name}:${dependency.group}:${dependency.name}:${dependency.version ?: '(managed)'}"
                }
            }
        }
        if (!violations.isEmpty()) {
            throw new GradleException("Direct fastjson dependencies are forbidden:\n" + violations.join('\n'))
        }
    }
}

tasks.register('agentDependencyAudit') {
    group = 'verification'
    description = 'AI Agent dependency guard: no direct fastjson source usage or direct fastjson dependency declarations.'

    dependsOn tasks.named('verifyNoFastjsonUsage')
    dependsOn tasks.named('verifyNoDirectFastjsonDependencies')
}
```

- [ ] **Step 2: Wire the audit into the quick agent gate**

In the existing `agentQuick` task, add the dependency audit:

```groovy
tasks.register('agentQuick') {
    group = 'verification'
    description = 'AI Agent quick gate: compile and run non-integration tests without Docker.'

    dependsOn tasks.named('agentDependencyAudit')
    dependsOn tasks.named('test')
}
```

- [ ] **Step 3: Verify the guard passes on current source**

Run:

```bash
./gradlew :modules:yudao-common:agentDependencyAudit :modules:yudao-module-system-server:agentDependencyAudit
```

Expected signal:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 4: Verify the guard is part of `agentQuick`**

Run:

```bash
./gradlew :modules:yudao-module-system-server:agentQuick --dry-run
```

Expected signal includes:

```text
:modules:yudao-module-system-server:verifyNoFastjsonUsage SKIPPED
:modules:yudao-module-system-server:verifyNoDirectFastjsonDependencies SKIPPED
:modules:yudao-module-system-server:agentDependencyAudit SKIPPED
```

- [ ] **Step 5: Commit**

```bash
git add build-logic/src/main/groovy/yudao-java.gradle
git commit -m "build: guard against direct fastjson usage"
```

## Task 3: Remove The Unused Version Catalog Entry

**Purpose:** Remove the only first-party fastjson declaration that is not needed by the current build.

**Files:**

- Modify: `gradle/libs.versions.toml`
- Modify: `docs/dependencies/fastjson2-removal-progress.md`

- [ ] **Step 1: Delete the unused version**

In `gradle/libs.versions.toml`, remove this line:

```toml
fastjson = "1.2.83"
```

- [ ] **Step 2: Verify no build script references the alias**

Run:

```bash
rg "libs\\.fastjson|version\\.ref = \"fastjson\"|fastjson = " -n gradle build-logic modules --glob '!**/build/**'
```

Expected signal:

```text
No matches
```

- [ ] **Step 3: Update progress**

Edit `docs/dependencies/fastjson2-removal-progress.md` and change the direct declaration row to:

```markdown
| Direct Gradle fastjson declarations removed | Complete | `gradle/libs.versions.toml` no longer declares `fastjson`; `rg` finds no build references |
```

- [ ] **Step 4: Verify**

Run:

```bash
./gradlew :modules:yudao-common:agentDependencyAudit :modules:yudao-module-system-server:agentDependencyAudit
```

Expected signal:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml docs/dependencies/fastjson2-removal-progress.md
git commit -m "build: remove unused fastjson version alias"
```

## Task 4: Add Opt-In Fastjson Exclusion Switches

**Purpose:** Let agents run no-fastjson experiments without changing default production behavior yet.

**Files:**

- Modify: `build-logic/src/main/groovy/yudao-java.gradle`
- Modify: `docs/testing/agent-verification.md`

- [ ] **Step 1: Add property-gated excludes**

Edit `build-logic/src/main/groovy/yudao-java.gradle`. Insert this block after the JSON dependency guardrails block:

```groovy
def excludeFastjson = { configuration ->
    configuration.exclude group: 'com.alibaba', module: 'fastjson'
    configuration.exclude group: 'com.alibaba.fastjson2', module: 'fastjson2'
    configuration.exclude group: 'com.alibaba.fastjson2', module: 'fastjson2-extension'
}

if (providers.gradleProperty('excludeFastjsonForTests').map { it.toBoolean() }.getOrElse(false)) {
    configurations.matching { it.name == 'testRuntimeClasspath' }.configureEach {
        excludeFastjson(it)
    }
}

if (providers.gradleProperty('excludeFastjsonRuntime').map { it.toBoolean() }.getOrElse(false)) {
    configurations.matching { it.name.endsWith('RuntimeClasspath') || it.name == 'runtimeClasspath' }.configureEach {
        excludeFastjson(it)
    }
}
```

- [ ] **Step 2: Document the experiment commands**

Append this section to `docs/testing/agent-verification.md`:

````markdown
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
````

- [ ] **Step 3: Verify the default classpath is unchanged**

Run:

```bash
./gradlew :modules:yudao-module-system-server:dependencyInsight --dependency fastjson2 --configuration runtimeClasspath
```

Expected signal still includes:

```text
com.alibaba.fastjson2:fastjson2:2.0.58
```

- [ ] **Step 4: Verify the opt-in test exclusion resolves**

Run:

```bash
./gradlew :modules:yudao-module-system-server:dependencyInsight --dependency fastjson2 --configuration testRuntimeClasspath -PexcludeFastjsonForTests=true
```

Expected signal:

```text
No dependencies matching given input were found
```

- [ ] **Step 5: Commit**

```bash
git add build-logic/src/main/groovy/yudao-java.gradle docs/testing/agent-verification.md
git commit -m "build: add opt-in fastjson exclusion checks"
```

## Task 5: Run The Test Runtime Exclusion Experiment

**Purpose:** Prove first-party tests do not require fastjson before production runtime is changed.

**Files:**

- Modify: `docs/dependencies/fastjson2-removal-progress.md`

- [ ] **Step 1: Run the quick and DB gates without fastjson on test runtime**

Run:

```bash
./gradlew :modules:yudao-module-system-server:agentVerify -PexcludeFastjsonForTests=true --rerun-tasks
```

Expected signal:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 2: Run common and MyBatis starter tests without fastjson on test runtime**

Run:

```bash
./gradlew :modules:yudao-common:test :modules:yudao-spring-boot-starter-mybatis:test -PexcludeFastjsonForTests=true --rerun-tasks
```

Expected signal:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 3: Record the evidence**

Edit `docs/dependencies/fastjson2-removal-progress.md` and change the test runtime row to:

```markdown
| Test runtime exclusion experiment | Complete | `agentVerify`, `yudao-common:test`, and `yudao-spring-boot-starter-mybatis:test` pass with `-PexcludeFastjsonForTests=true` |
```

- [ ] **Step 4: Commit**

```bash
git add docs/dependencies/fastjson2-removal-progress.md
git commit -m "test: record fastjson-free test runtime evidence"
```

## Task 6: Run The Production Runtime Exclusion Experiment

**Purpose:** Identify whether JustAuth, Easy-Trans, or Alibaba Cloud runtime paths actually block removal.

**Files:**

- Modify: `docs/dependencies/fastjson2-removal-progress.md`

- [ ] **Step 1: Verify no-fastjson runtime dependency insight for common**

Run:

```bash
./gradlew :modules:yudao-common:dependencyInsight --dependency fastjson --configuration runtimeClasspath -PexcludeFastjsonRuntime=true
```

Expected signal:

```text
No dependencies matching given input were found
```

- [ ] **Step 2: Verify no-fastjson runtime dependency insight for MyBatis starter**

Run:

```bash
./gradlew :modules:yudao-spring-boot-starter-mybatis:dependencyInsight --dependency fastjson --configuration runtimeClasspath -PexcludeFastjsonRuntime=true
```

Expected signal:

```text
No dependencies matching given input were found
```

- [ ] **Step 3: Verify no-fastjson runtime dependency insight for system server**

Run:

```bash
./gradlew :modules:yudao-module-system-server:dependencyInsight --dependency fastjson --configuration runtimeClasspath -PexcludeFastjsonRuntime=true
```

Expected signal:

```text
No dependencies matching given input were found
```

- [ ] **Step 4: Verify no-fastjson runtime dependency insight for infra server**

Run:

```bash
./gradlew :modules:yudao-module-infra-server:dependencyInsight --dependency fastjson --configuration runtimeClasspath -PexcludeFastjsonRuntime=true
```

Expected signal:

```text
No dependencies matching given input were found
```

- [ ] **Step 5: Verify no-fastjson runtime dependency insight for monolith app**

Run:

```bash
./gradlew :modules:yudao-server:dependencyInsight --dependency fastjson --configuration runtimeClasspath -PexcludeFastjsonRuntime=true
```

Expected signal:

```text
No dependencies matching given input were found
```

- [ ] **Step 6: Verify no-fastjson runtime dependency insight for gateway**

Run:

```bash
./gradlew :modules:yudao-gateway:dependencyInsight --dependency fastjson --configuration runtimeClasspath -PexcludeFastjsonRuntime=true
```

Expected signal:

```text
No dependencies matching given input were found
```

- [ ] **Step 7: Build all runnable jars without fastjson**

Run:

```bash
./gradlew :modules:yudao-server:bootJar :modules:yudao-gateway:bootJar :modules:yudao-module-system-server:bootJar :modules:yudao-module-infra-server:bootJar -PexcludeFastjsonRuntime=true
```

Expected signal:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 8: Run focused tests with production runtime exclusion enabled**

Run:

```bash
./gradlew :modules:yudao-module-system-server:test --tests '*SocialClientServiceImplTest' -PexcludeFastjsonRuntime=true --rerun-tasks
```

Expected signal:

```text
BUILD SUCCESSFUL
```

Run:

```bash
./gradlew :modules:yudao-spring-boot-starter-mybatis:test -PexcludeFastjsonRuntime=true --rerun-tasks
```

Expected signal:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 9: Record the evidence**

Edit `docs/dependencies/fastjson2-removal-progress.md` and change the production runtime row to:

```markdown
| Production runtime exclusion experiment | Complete | Runtime dependency insight is clean for common, MyBatis starter, system, infra, monolith, and gateway; boot jars build with `-PexcludeFastjsonRuntime=true`; focused SocialClient and MyBatis starter tests pass |
```

- [ ] **Step 10: Commit**

```bash
git add docs/dependencies/fastjson2-removal-progress.md
git commit -m "test: record fastjson-free runtime evidence"
```

## Task 7: Make Fastjson Exclusion The Default

**Purpose:** Remove fastjson from normal runtime and test classpaths.

**Files:**

- Modify: `build-logic/src/main/groovy/yudao-java.gradle`
- Create: `modules/yudao-common/src/test/java/cn/iocoder/yudao/framework/common/util/json/FastjsonClasspathTest.java`
- Modify: `docs/dependencies/fastjson2-removal-progress.md`

- [ ] **Step 1: Add a failing classpath absence test**

Create `modules/yudao-common/src/test/java/cn/iocoder/yudao/framework/common/util/json/FastjsonClasspathTest.java`:

```java
package cn.iocoder.yudao.framework.common.util.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class FastjsonClasspathTest {

    @Test
    void fastjsonShouldNotBeAvailableOnRuntimeClasspath() {
        assertFalse(isPresent("com.alibaba.fastjson.JSON"));
        assertFalse(isPresent("com.alibaba.fastjson2.JSON"));
        assertFalse(isPresent("com.alibaba.fastjson2.JSONReader"));
    }

    private static boolean isPresent(String className) {
        try {
            Class.forName(className, false, FastjsonClasspathTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

}
```

- [ ] **Step 2: Verify the test fails before default exclusion**

Run:

```bash
./gradlew :modules:yudao-common:test --tests '*FastjsonClasspathTest' --rerun-tasks
```

Expected signal:

```text
FastjsonClasspathTest > fastjsonShouldNotBeAvailableOnRuntimeClasspath() FAILED
```

- [ ] **Step 3: Make exclusion default**

Edit `build-logic/src/main/groovy/yudao-java.gradle`. Replace the property-gated exclusion block from Task 4 with this default exclusion:

```groovy
def excludeFastjson = { configuration ->
    configuration.exclude group: 'com.alibaba', module: 'fastjson'
    configuration.exclude group: 'com.alibaba.fastjson2', module: 'fastjson2'
    configuration.exclude group: 'com.alibaba.fastjson2', module: 'fastjson2-extension'
}

configurations.configureEach {
    excludeFastjson(it)
}
```

- [ ] **Step 4: Verify the classpath test passes**

Run:

```bash
./gradlew :modules:yudao-common:test --tests '*FastjsonClasspathTest' --rerun-tasks
```

Expected signal:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 5: Verify the final agent gate**

Run:

```bash
./gradlew :modules:yudao-module-system-server:agentVerify --rerun-tasks
```

Expected signal:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 6: Verify runnable jars**

Run:

```bash
./gradlew :modules:yudao-server:bootJar :modules:yudao-gateway:bootJar :modules:yudao-module-system-server:bootJar :modules:yudao-module-infra-server:bootJar
```

Expected signal:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 7: Verify dependency insight is clean without flags**

Run:

```bash
./gradlew :modules:yudao-module-system-server:dependencyInsight --dependency fastjson --configuration runtimeClasspath
```

Expected signal:

```text
No dependencies matching given input were found
```

Run:

```bash
./gradlew :modules:yudao-server:dependencyInsight --dependency fastjson --configuration runtimeClasspath
```

Expected signal:

```text
No dependencies matching given input were found
```

Run:

```bash
./gradlew :modules:yudao-gateway:dependencyInsight --dependency fastjson --configuration runtimeClasspath
```

Expected signal:

```text
No dependencies matching given input were found
```

- [ ] **Step 8: Update progress to complete**

Edit `docs/dependencies/fastjson2-removal-progress.md` and change the default runtime row to:

```markdown
| Default runtime exclusion | Complete | `FastjsonClasspathTest`, `agentVerify`, boot jars, and runtime dependency insight pass without opt-in flags |
```

- [ ] **Step 9: Commit**

```bash
git add build-logic/src/main/groovy/yudao-java.gradle modules/yudao-common/src/test/java/cn/iocoder/yudao/framework/common/util/json/FastjsonClasspathTest.java docs/dependencies/fastjson2-removal-progress.md
git commit -m "build: remove fastjson from runtime classpaths"
```

## Task 8: Remove Temporary Migration Wording

**Purpose:** Leave the project in a clean final state after fastjson is gone.

**Files:**

- Modify: `docs/testing/agent-verification.md`
- Modify: `docs/dependencies/fastjson2-removal-progress.md`

- [ ] **Step 1: Replace experiment wording in agent verification docs**

In `docs/testing/agent-verification.md`, replace the "Dependency Reduction Experiments" section from Task 4 with:

````markdown
## Dependency Guardrails

The agent verification gate includes dependency guardrails:

```bash
./gradlew :modules:yudao-module-system-server:agentDependencyAudit
```

The guard fails when first-party code imports Alibaba fastjson APIs or directly declares fastjson dependencies. JSON code must use `JsonUtils` or a Spring-managed `ObjectMapper`.
````

- [ ] **Step 2: Add final status**

Append this section to `docs/dependencies/fastjson2-removal-progress.md`:

```markdown
## Final Status

Fastjson is not a first-party JSON engine and is not present on the verified runtime classpaths. Future additions of fastjson imports or direct dependencies are blocked by `agentDependencyAudit`.
```

- [ ] **Step 3: Run final verification**

Run:

```bash
./gradlew :modules:yudao-common:test :modules:yudao-module-system-server:agentVerify --rerun-tasks
```

Expected signal:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 4: Commit**

```bash
git add docs/testing/agent-verification.md docs/dependencies/fastjson2-removal-progress.md
git commit -m "docs: finalize fastjson removal guidance"
```

## Blocker Protocol

If any no-fastjson command fails with `ClassNotFoundException`, `NoClassDefFoundError`, or a third-party JSON parse failure:

1. Do not make default exclusion permanent.
2. Record the failing command and exception in `docs/dependencies/fastjson2-removal-progress.md`.
3. Keep the guardrails from Tasks 1-3.
4. Replace or upgrade only the failing dependency path.

Known replacement decision points:

- If JustAuth is the blocker, isolate the affected social provider and replace that integration path instead of reintroducing fastjson globally.
- If Easy-Trans is the blocker, remove Easy-Trans from the affected module or replace the feature with explicit service lookups.
- If Alibaba Cloud/Nacos is the blocker, keep the transitive dependency quarantined and do not claim final fastjson removal.

## Self-Review

- The plan covers the user's end goal: final deletion, not only warning suppression.
- The plan separates architecture policy, guardrails, test experiments, production runtime experiments, and final exclusion.
- The plan does not conflate Hutool JSON with fastjson2.
- Every phase has an observable Gradle command and expected signal.
- The final state has both negative dependency evidence and a runtime classpath absence test.
