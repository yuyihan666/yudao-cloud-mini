# AI Agent Test Feedback Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an AI Agent-friendly verification control plane so code changes can be checked quickly, observed clearly, fixed from actionable failure output, and finally guarded by real MySQL integration tests.

**Architecture:** Keep the existing fast H2-based tests as a legacy quick lane, add explicit Gradle verification tasks for agents, and introduce real-dependency integration tests as a separate final DB/API gate. The core boundary is not "replace H2 with Testcontainers"; it is "fast feedback first, real database as final evidence, structured failure output everywhere".

**Tech Stack:** Gradle convention plugins, JUnit 5 tags, Spring Boot 3.5.x testing, Spring Boot Testcontainers, Testcontainers MySQL 8.0, MockMvc, deterministic Test Data Builders.

---

## Design Principles

1. **Agent entrypoints are product surface.** An AI Agent should not infer which Gradle command to run after a change. The project provides stable tasks: `agentQuick`, `agentDb`, `agentApi`, and `agentVerify`.
2. **Fast feedback stays fast.** Compile, pure unit tests, Mockito tests, and controller slice tests must not require Docker.
3. **Real DB is the final DB gate.** New or migrated tests for Mapper, SQL, transaction, pagination, logical delete, MyBatis-Plus wrapper, and schema behavior should use MySQL Testcontainers.
4. **Observability comes before breadth.** Test output must show failed class/method, full exception, and report paths. A slow realistic test with unreadable failure output is not Agent-friendly.
5. **Deterministic data beats random data.** Builders should use readable defaults. Random values are only for uniqueness when required.
6. **Do not break existing tests.** `BaseDbUnitTest` remains compatible. Add new integration bases instead of changing the old base class in place.

---

## Verification Layers

| Layer | Name | Dependency | Purpose | Default Command |
|---|---|---|---|---|
| L0 | Compile | none | Catch syntax, annotation processing, dependency, and generated-source errors | `./gradlew agentQuick` |
| L1 | Unit | none | Pure logic, Mockito service tests, framework utilities | `./gradlew agentQuick` |
| L2 | Slice | none | Controller route, request binding, validation, JSON, optional security behavior | `./gradlew agentQuick` |
| L3 | DB Integration | MySQL Testcontainers | Final evidence for SQL, Mapper, transaction, pagination, logical delete | `./gradlew agentDb` |
| L4 | API Integration | MySQL/Redis Testcontainers | Small end-to-end HTTP smoke tests | `./gradlew agentApi` |

`agentVerify` runs the project-level final gate: quick checks first, then DB/API integration where available.

---

## File Structure

### Create

| File | Responsibility |
|---|---|
| `modules/yudao-spring-boot-starter-test/src/main/java/cn/iocoder/yudao/framework/test/core/ut/BaseDbIntegrationTest.java` | MySQL Testcontainers base class for L3 DB integration tests |
| `modules/yudao-spring-boot-starter-test/src/main/java/cn/iocoder/yudao/framework/test/core/ut/BaseControllerTest.java` | Shared MockMvc JSON helpers for L2 controller slice tests |
| `modules/yudao-spring-boot-starter-test/src/main/java/cn/iocoder/yudao/framework/test/core/builder/TestDataBuilder.java` | Deterministic test data builder base |
| `modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/tenant/TenantPackageDbIntegrationTest.java` | Small MySQL-backed DB integration example |
| `modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/tenant/TenantPackageTestData.java` | Deterministic Builder/Mother example for tenant package tests |

### Modify

| File | Responsibility |
|---|---|
| `build-logic/src/main/groovy/yudao-java.gradle` | Unified JUnit Platform, test logging, agent tasks, integration tags |
| `gradle/libs.versions.toml` | Testcontainers dependency coordinates |
| `modules/yudao-spring-boot-starter-test/build.gradle` | Spring Boot Testcontainers and MySQL Testcontainers dependencies |
| `docs/superpowers/plans/2026-06-02-test-framework-redesign.md` | This implementation plan |

### Do Not Modify In This Plan

- `BaseDbUnitTest.java`: keep the existing H2 lane compatible.
- Existing service tests: do not mass-migrate.
- `create_tables.sql` and `clean.sql`: keep them until a separate schema migration plan exists.
- CI workflow: add CI integration in a later plan after local agent tasks are proven.

---

## Task 1: Add Agent-Friendly Gradle Test Output And Entrypoints

**Files:**
- Modify: `build-logic/src/main/groovy/yudao-java.gradle`

- [ ] **Step 1: Add JUnit Platform and verbose test logging**

Append this block to `build-logic/src/main/groovy/yudao-java.gradle`:

```groovy
// ========== Agent-friendly test configuration ==========
tasks.withType(Test).configureEach {
    useJUnitPlatform()

    testLogging {
        events "passed", "skipped", "failed"
        showExceptions true
        showCauses true
        showStackTraces true
        exceptionFormat "full"
    }

    afterSuite { desc, result ->
        if (!desc.parent) {
            logger.lifecycle("Test result: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)")
            logger.lifecycle("HTML report: ${reports.html.outputLocation.get().asFile}")
            logger.lifecycle("XML results: ${reports.junitXml.outputLocation.get().asFile}")
        }
    }
}
```

- [ ] **Step 2: Add quick test task**

Append this block below the previous block:

```groovy
tasks.register('agentQuick') {
    group = 'verification'
    description = 'AI Agent quick gate: compile and run non-integration tests without Docker.'

    dependsOn tasks.named('test')
}

tasks.named('test', Test).configure {
    useJUnitPlatform {
        excludeTags 'db', 'api', 'integration'
    }
}
```

- [ ] **Step 3: Add DB integration task**

Append:

```groovy
tasks.register('agentDb', Test) {
    group = 'verification'
    description = 'AI Agent DB gate: run MySQL Testcontainers tests.'

    useJUnitPlatform {
        includeTags 'db', 'integration'
        excludeTags 'api'
    }

    shouldRunAfter tasks.named('test')
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
}
```

- [ ] **Step 4: Add API integration task**

Append:

```groovy
tasks.register('agentApi', Test) {
    group = 'verification'
    description = 'AI Agent API gate: run end-to-end API integration tests.'

    useJUnitPlatform {
        includeTags 'api'
    }

    shouldRunAfter tasks.named('agentDb')
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
}
```

- [ ] **Step 5: Add final verification task**

Append:

```groovy
tasks.register('agentVerify') {
    group = 'verification'
    description = 'AI Agent final gate: quick tests, DB integration tests, then API integration tests.'

    dependsOn tasks.named('agentQuick')
    dependsOn tasks.named('agentDb')
    dependsOn tasks.named('agentApi')
}
```

- [ ] **Step 6: Verify task discovery**

Run:

```bash
./gradlew tasks --group verification
```

Expected: output includes `agentQuick`, `agentDb`, `agentApi`, and `agentVerify`.

- [ ] **Step 7: Verify quick gate**

Run:

```bash
./gradlew :modules:yudao-module-system-server:agentQuick
```

Expected: non-integration tests run with full failure output and report paths.

- [ ] **Step 8: Commit**

```bash
git add build-logic/src/main/groovy/yudao-java.gradle
git commit -m "feat(test): add AI agent verification tasks and test logging"
```

---

## Task 2: Add Testcontainers Dependencies Through The Test Starter

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `modules/yudao-spring-boot-starter-test/build.gradle`

- [ ] **Step 1: Add Testcontainers versions**

In `gradle/libs.versions.toml`, add under `[versions]` near the existing test dependencies:

```toml
testcontainers = "1.21.1"
```

- [ ] **Step 2: Add Testcontainers libraries**

In `gradle/libs.versions.toml`, add under `[libraries]` near the existing test libraries:

```toml
testcontainers-bom = { module = "org.testcontainers:testcontainers-bom", version.ref = "testcontainers" }
testcontainers-mysql = { module = "org.testcontainers:mysql" }
testcontainers-junit = { module = "org.testcontainers:junit-jupiter" }
```

- [ ] **Step 3: Add dependencies to starter-test**

Update `modules/yudao-spring-boot-starter-test/build.gradle` so the dependency block contains:

```groovy
dependencies {
    api project(':modules:yudao-common')
    api project(':modules:yudao-spring-boot-starter-mybatis')
    api project(':modules:yudao-spring-boot-starter-redis')
    api libs.mockito
    api 'org.springframework.boot:spring-boot-starter-test'
    api 'org.springframework.boot:spring-boot-testcontainers'

    api(enforcedPlatform(libs.testcontainers.bom))
    api libs.testcontainers.mysql
    api libs.testcontainers.junit

    runtimeOnly 'com.h2database:h2'
    runtimeOnly 'com.mysql:mysql-connector-j'
    api libs.jedis.mock
    api libs.podam
}
```

- [ ] **Step 4: Verify dependency resolution**

Run:

```bash
./gradlew :modules:yudao-spring-boot-starter-test:dependencies --configuration compileClasspath
```

Expected: output includes Spring Boot testcontainers and Testcontainers MySQL.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml modules/yudao-spring-boot-starter-test/build.gradle
git commit -m "feat(test): add Testcontainers dependencies to starter test module"
```

---

## Task 3: Add MySQL DB Integration Base

**Files:**
- Create: `modules/yudao-spring-boot-starter-test/src/main/java/cn/iocoder/yudao/framework/test/core/ut/BaseDbIntegrationTest.java`
- Test: `modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/tenant/TenantPackageDbIntegrationTest.java`

- [ ] **Step 1: Create DB integration base**

Create `modules/yudao-spring-boot-starter-test/src/main/java/cn/iocoder/yudao/framework/test/core/ut/BaseDbIntegrationTest.java`:

```java
package cn.iocoder.yudao.framework.test.core.ut;

import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.datasource.config.YudaoDataSourceAutoConfiguration;
import cn.iocoder.yudao.framework.mybatis.config.YudaoMybatisAutoConfiguration;
import cn.iocoder.yudao.framework.test.config.SqlInitializationTestConfiguration;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusJoinAutoConfiguration;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * MySQL-backed DB integration test base.
 *
 * <p>Use this for final evidence of Mapper, SQL dialect, transaction,
 * pagination, logical delete, and schema behavior. Keep {@link BaseDbUnitTest}
 * for legacy fast H2 tests.
 */
@Tag("db")
@Tag("integration")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = BaseDbIntegrationTest.Application.class)
@ActiveProfiles("unit-test")
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public abstract class BaseDbIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("ruoyi-vue-pro")
            .withUsername("test")
            .withPassword("test");

    @Import({
            YudaoDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            SqlInitializationTestConfiguration.class,
            YudaoMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            SpringUtil.class
    })
    public static class Application {
    }

}
```

- [ ] **Step 2: Add a minimal MySQL-backed test**

Create `modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/tenant/TenantPackageDbIntegrationTest.java`:

```java
package cn.iocoder.yudao.module.system.service.tenant;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbIntegrationTest;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantPackageDO;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantPackageMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TenantPackageDbIntegrationTest extends BaseDbIntegrationTest {

    @Resource
    private TenantPackageMapper tenantPackageMapper;

    @Test
    void should_insert_and_select_with_real_mysql() {
        TenantPackageDO tenantPackage = new TenantPackageDO()
                .setName("标准套餐")
                .setStatus(0)
                .setMenuIds("[1,2,3]")
                .setRemark("mysql integration");

        tenantPackageMapper.insert(tenantPackage);

        assertNotNull(tenantPackage.getId());
        TenantPackageDO result = tenantPackageMapper.selectById(tenantPackage.getId());
        assertNotNull(result);
        assertEquals("标准套餐", result.getName());
        assertEquals("[1,2,3]", result.getMenuIds());
    }

}
```

- [ ] **Step 3: Verify quick gate excludes DB integration**

Run:

```bash
./gradlew :modules:yudao-module-system-server:agentQuick --tests "TenantPackageDbIntegrationTest"
```

Expected: test is not executed because `agentQuick` excludes `db` and `integration` tags.

- [ ] **Step 4: Verify DB gate runs the MySQL test**

Run:

```bash
./gradlew :modules:yudao-module-system-server:agentDb --tests "TenantPackageDbIntegrationTest"
```

Expected: MySQL container starts and the test passes.

- [ ] **Step 5: Commit**

```bash
git add modules/yudao-spring-boot-starter-test/src/main/java/cn/iocoder/yudao/framework/test/core/ut/BaseDbIntegrationTest.java
git add modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/tenant/TenantPackageDbIntegrationTest.java
git commit -m "feat(test): add MySQL integration test base for agent DB gate"
```

---

## Task 4: Add Deterministic Test Data Builder Base

**Files:**
- Create: `modules/yudao-spring-boot-starter-test/src/main/java/cn/iocoder/yudao/framework/test/core/builder/TestDataBuilder.java`
- Test: `modules/yudao-spring-boot-starter-test/src/test/java/cn/iocoder/yudao/framework/test/core/builder/TestDataBuilderTest.java`

- [ ] **Step 1: Write builder base test**

Create `modules/yudao-spring-boot-starter-test/src/test/java/cn/iocoder/yudao/framework/test/core/builder/TestDataBuilderTest.java`:

```java
package cn.iocoder.yudao.framework.test.core.builder;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestDataBuilderTest {

    static class PageParamBuilder extends TestDataBuilder<PageParam, PageParamBuilder> {

        PageParamBuilder() {
            super(new PageParam().setPageNo(1).setPageSize(10));
        }

        PageParamBuilder pageNo(Integer pageNo) {
            data.setPageNo(pageNo);
            return self();
        }

        PageParamBuilder pageSize(Integer pageSize) {
            data.setPageSize(pageSize);
            return self();
        }

    }

    @Test
    void should_build_with_readable_defaults() {
        PageParam result = new PageParamBuilder().build();

        assertEquals(1, result.getPageNo());
        assertEquals(10, result.getPageSize());
    }

    @Test
    void should_override_defaults() {
        PageParam result = new PageParamBuilder()
                .pageNo(2)
                .pageSize(50)
                .build();

        assertEquals(2, result.getPageNo());
        assertEquals(50, result.getPageSize());
    }

}
```

- [ ] **Step 2: Verify test fails before implementation**

Run:

```bash
./gradlew :modules:yudao-spring-boot-starter-test:test --tests "TestDataBuilderTest"
```

Expected: FAIL because `TestDataBuilder` does not exist.

- [ ] **Step 3: Create builder base**

Create `modules/yudao-spring-boot-starter-test/src/main/java/cn/iocoder/yudao/framework/test/core/builder/TestDataBuilder.java`:

```java
package cn.iocoder.yudao.framework.test.core.builder;

import java.util.function.Consumer;

/**
 * Deterministic test data builder base.
 *
 * <p>Prefer readable defaults over random values. Use randomness only when a
 * field must be unique for the test.
 */
public abstract class TestDataBuilder<T, SELF extends TestDataBuilder<T, SELF>> {

    protected final T data;

    protected TestDataBuilder(T data) {
        this.data = data;
    }

    public SELF modify(Consumer<T> modifier) {
        modifier.accept(data);
        return self();
    }

    public T build() {
        return data;
    }

    @SuppressWarnings("unchecked")
    protected SELF self() {
        return (SELF) this;
    }

}
```

- [ ] **Step 4: Verify builder tests pass**

Run:

```bash
./gradlew :modules:yudao-spring-boot-starter-test:test --tests "TestDataBuilderTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add modules/yudao-spring-boot-starter-test/src/main/java/cn/iocoder/yudao/framework/test/core/builder/TestDataBuilder.java
git add modules/yudao-spring-boot-starter-test/src/test/java/cn/iocoder/yudao/framework/test/core/builder/TestDataBuilderTest.java
git commit -m "feat(test): add deterministic test data builder base"
```

---

## Task 5: Add TenantPackage Test Data Example

**Files:**
- Create: `modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/tenant/TenantPackageTestData.java`

- [ ] **Step 1: Create TenantPackage test data factory**

Create `modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/tenant/TenantPackageTestData.java`:

```java
package cn.iocoder.yudao.module.system.service.tenant;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.builder.TestDataBuilder;
import cn.iocoder.yudao.module.system.controller.admin.tenant.vo.packages.TenantPackageSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantPackageDO;

public final class TenantPackageTestData {

    private TenantPackageTestData() {
    }

    public static TenantPackageDOBuilder tenantPackage() {
        return new TenantPackageDOBuilder();
    }

    public static TenantPackageSaveReqVOBuilder createTenantPackageReq() {
        return new TenantPackageSaveReqVOBuilder().withoutId();
    }

    public static class TenantPackageDOBuilder extends TestDataBuilder<TenantPackageDO, TenantPackageDOBuilder> {

        TenantPackageDOBuilder() {
            super(new TenantPackageDO()
                    .setName("标准套餐")
                    .setStatus(CommonStatusEnum.ENABLE.getStatus())
                    .setMenuIds("[1,2,3]")
                    .setRemark("标准套餐备注"));
        }

        public TenantPackageDOBuilder disabled() {
            data.setStatus(CommonStatusEnum.DISABLE.getStatus());
            return self();
        }

        public TenantPackageDOBuilder name(String name) {
            data.setName(name);
            return self();
        }

        public TenantPackageDOBuilder menuIds(String menuIds) {
            data.setMenuIds(menuIds);
            return self();
        }

    }

    public static class TenantPackageSaveReqVOBuilder
            extends TestDataBuilder<TenantPackageSaveReqVO, TenantPackageSaveReqVOBuilder> {

        TenantPackageSaveReqVOBuilder() {
            super(new TenantPackageSaveReqVO()
                    .setName("标准套餐")
                    .setStatus(CommonStatusEnum.ENABLE.getStatus())
                    .setMenuIds("[1,2,3]")
                    .setRemark("标准套餐备注"));
        }

        public TenantPackageSaveReqVOBuilder withoutId() {
            data.setId(null);
            return self();
        }

        public TenantPackageSaveReqVOBuilder id(Long id) {
            data.setId(id);
            return self();
        }

        public TenantPackageSaveReqVOBuilder name(String name) {
            data.setName(name);
            return self();
        }

    }

}
```

- [ ] **Step 2: Verify compilation**

Run:

```bash
./gradlew :modules:yudao-module-system-server:testClasses
```

Expected: test sources compile.

- [ ] **Step 3: Commit**

```bash
git add modules/yudao-module-system-server/src/test/java/cn/iocoder/yudao/module/system/service/tenant/TenantPackageTestData.java
git commit -m "feat(test): add deterministic tenant package test data factory"
```

---

## Task 6: Add Controller Slice Base

**Files:**
- Create: `modules/yudao-spring-boot-starter-test/src/main/java/cn/iocoder/yudao/framework/test/core/ut/BaseControllerTest.java`

- [ ] **Step 1: Create controller test base**

Create `modules/yudao-spring-boot-starter-test/src/main/java/cn/iocoder/yudao/framework/test/core/ut/BaseControllerTest.java`:

```java
package cn.iocoder.yudao.framework.test.core.ut;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    protected String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

}
```

- [ ] **Step 2: Verify starter-test compiles**

Run:

```bash
./gradlew :modules:yudao-spring-boot-starter-test:compileJava
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add modules/yudao-spring-boot-starter-test/src/main/java/cn/iocoder/yudao/framework/test/core/ut/BaseControllerTest.java
git commit -m "feat(test): add controller slice test base"
```

---

## Task 7: Document Agent Verification Rules In The Plan

**Files:**
- Modify: `docs/superpowers/plans/2026-06-02-test-framework-redesign.md`

- [ ] **Step 1: Confirm rules are present**

Check this plan contains these rules:

```text
1. Use agentQuick after every code change.
2. Use agentDb when changing Mapper, DO, SQL, transaction, pagination, or DB-backed service behavior.
3. Use agentApi before claiming full API behavior is verified.
4. Do not use H2 as final evidence for MySQL behavior.
5. Prefer deterministic builders over randomPojo for new tests.
```

- [ ] **Step 2: Verify no old plan framing remains**

Run:

```bash
rg -n "替代 H[2]|改造 BaseDbUnitTes[t]|四层测试体[系]|V2Tes[t]|REST Assure[d]" docs/superpowers/plans/2026-06-02-test-framework-redesign.md
```

Expected: no output for obsolete framing.

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/plans/2026-06-02-test-framework-redesign.md
git commit -m "docs(test): define AI agent test feedback loop plan"
```

---

## Final Verification

- [ ] **Run quick verification**

```bash
./gradlew :modules:yudao-spring-boot-starter-test:agentQuick
./gradlew :modules:yudao-module-system-server:agentQuick
```

Expected: both pass without Docker.

- [ ] **Run DB verification**

```bash
./gradlew :modules:yudao-module-system-server:agentDb --tests "TenantPackageDbIntegrationTest"
```

Expected: MySQL container starts and the test passes.

- [ ] **Run final verification**

```bash
./gradlew :modules:yudao-module-system-server:agentVerify
```

Expected: quick gate and DB/API gates execute in order. `agentApi` may have no tests at this stage and should pass with no matching API-tagged tests.

---

## Agent Verification Rules

1. Use `agentQuick` after every code change.
2. Use `agentDb` when changing Mapper, DO, SQL, transaction, pagination, logical delete, DB-backed service behavior, or any `create_tables.sql` / `clean.sql` content.
3. Use `agentApi` before claiming full HTTP/API behavior is verified.
4. Do not use H2 as final evidence for MySQL behavior.
5. Prefer deterministic builders over `randomPojo()` for new tests.
6. When a test fails, first classify the failure: compile, test compile, Spring context, DB schema, SQL dialect, assertion, external dependency, or flaky.
7. Always include the smallest reproducible rerun command in the fix notes.

---

## Self-Review

### Spec Coverage

| Requirement | Task |
|---|---|
| Agent has stable verification commands | Task 1 |
| Fast feedback does not require Docker | Task 1 |
| Real MySQL is final DB gate | Task 2, Task 3 |
| Existing H2 tests remain compatible | Task 3 explicitly adds new base instead of modifying `BaseDbUnitTest` |
| Test output is observable | Task 1 |
| Deterministic test data path exists | Task 4, Task 5 |
| Controller slice base exists | Task 6 |
| Agent execution rules are documented | Task 7 |

### Placeholder Scan

No placeholder red flags are present.

### Scope Check

This plan intentionally does not migrate all existing tests, remove H2, remove `create_tables.sql`, or add CI integration. Those are separate plans after the local Agent feedback loop proves stable.
