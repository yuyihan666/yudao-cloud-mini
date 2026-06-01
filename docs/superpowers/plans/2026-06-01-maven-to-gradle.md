# Maven → Gradle 迁移实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 yudao-cloud-mini 从 Maven 彻底迁移到 Gradle，确保 `gradle build` 编译通过、项目能启动、Swagger 可访问后，再删除 pom.xml。

**Architecture:** 使用 build-logic Composite Build + Version Catalog + Convention Plugins。所有模块平铺到 `modules/` 单层目录。纯库模块用 `yudao-java` 插件，Spring Boot 应用用 `yudao-spring-app` 插件。

**Tech Stack:** Gradle 8.14 (Groovy DSL), Spring Boot 3.5.14, Spring Cloud 2025.0.1, Spring Cloud Alibaba 2025.0.0.0, Java 25

---

## 成功标准（按顺序验证）

1. `./gradlew build -x test` 编译通过
2. `./gradlew bootJar` 生成可执行 jar
3. 本地启动 `YudaoServerApplication`，端口 48080 正常响应
4. 浏览器访问 Swagger 文档页面（`http://localhost:48080/doc.html`）正常
5. 以上全部通过后，删除所有 pom.xml 和 Maven 相关文件

---

## Task 1: 创建 Gradle 骨架

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `build-logic/settings.gradle`
- Create: `build-logic/build.gradle`
- Create: `build-logic/src/main/groovy/yudao-java.gradle`
- Create: `build-logic/src/main/groovy/yudao-spring-app.gradle`
- Create: `gradle/libs.versions.toml`

- [ ] **Step 1: 创建根 settings.gradle**

```groovy
pluginManagement {
    plugins {
        id 'org.springframework.boot' version '3.5.14'
    }
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven { url 'https://mirrors.huaweicloud.com/repository/maven/' }
        maven { url 'https://maven.aliyun.com/repository/public' }
    }
}

rootProject.name = 'yudao-cloud-mini'

includeBuild 'build-logic'

include 'modules:yudao-common'
include 'modules:yudao-spring-boot-starter-env'
include 'modules:yudao-spring-boot-starter-mybatis'
include 'modules:yudao-spring-boot-starter-redis'
include 'modules:yudao-spring-boot-starter-web'
include 'modules:yudao-spring-boot-starter-security'
include 'modules:yudao-spring-boot-starter-websocket'
include 'modules:yudao-spring-boot-starter-monitor'
include 'modules:yudao-spring-boot-starter-protection'
include 'modules:yudao-spring-boot-starter-job'
include 'modules:yudao-spring-boot-starter-mq'
include 'modules:yudao-spring-boot-starter-rpc'
include 'modules:yudao-spring-boot-starter-excel'
include 'modules:yudao-spring-boot-starter-test'
include 'modules:yudao-spring-boot-starter-biz-tenant'
include 'modules:yudao-spring-boot-starter-biz-data-permission'
include 'modules:yudao-spring-boot-starter-biz-ip'
include 'modules:yudao-module-system-api'
include 'modules:yudao-module-system-server'
include 'modules:yudao-module-infra-api'
include 'modules:yudao-module-infra-server'
include 'modules:yudao-gateway'
include 'modules:yudao-server'
```

- [ ] **Step 2: 创建根 build.gradle**

```groovy
allprojects {
    group = 'cn.iocoder.cloud'
    version = '2026.05-SNAPSHOT'

    repositories {
        mavenCentral()
        maven { url 'https://mirrors.huaweicloud.com/repository/maven/' }
        maven { url 'https://maven.aliyun.com/repository/public' }
    }
}
```

- [ ] **Step 3: 创建 build-logic/settings.gradle**

```groovy
rootProject.name = 'yudao-build-logic'
```

- [ ] **Step 4: 创建 build-logic/build.gradle**

```groovy
plugins {
    id 'groovy-gradle-plugin'
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven { url 'https://mirrors.huaweicloud.com/repository/maven/' }
    maven { url 'https://maven.aliyun.com/repository/public' }
}
```

- [ ] **Step 5: 创建 yudao-java.gradle convention plugin**

路径: `build-logic/src/main/groovy/yudao-java.gradle`

```groovy
plugins {
    id 'java-library'
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    options.debug = false
    options.compilerArgs << '-parameters'
}

dependencies {
    // Spring BOM
    api(platform(libs.spring.boot.bom))
    api(platform(libs.spring.cloud.bom))
    api(platform(libs.spring.cloud.alibaba.bom))

    // 注解处理器
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
    annotationProcessor 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
    annotationProcessor 'org.mapstruct:mapstruct-processor'

    compileOnly 'org.projectlombok:lombok'
    implementation 'org.mapstruct:mapstruct'
}
```

- [ ] **Step 6: 创建 yudao-spring-app.gradle convention plugin**

路径: `build-logic/src/main/groovy/yudao-spring-app.gradle`

```groovy
plugins {
    id 'yudao-java'
    id 'org.springframework.boot'
}

springBoot {
    mainClass = project.hasProperty('mainClass')
        ? project.property('mainClass')
        : null
}

bootJar {
    archiveFileName = "${project.name}.jar"
}

jar {
    enabled = false
}
```

- [ ] **Step 7: 创建 libs.versions.toml**

路径: `gradle/libs.versions.toml`

内容见设计文档 Section 2，完整复制（[versions] + [libraries] + [plugins]），此处省略以避免重复，执行时从 `docs/superpowers/specs/2026-06-01-maven-to-gradle-design.md` Section 2 完整复制。

- [ ] **Step 8: 创建 modules/ 空目录结构**

```bash
mkdir -p modules/yudao-common
mkdir -p modules/yudao-spring-boot-starter-{env,mybatis,redis,web,security,websocket,monitor,protection,job,mq,rpc,excel,test,biz-tenant,biz-data-permission,biz-ip}
mkdir -p modules/yudao-module-{system,infra}-{api,server}
mkdir -p modules/yudao-gateway
mkdir -p modules/yudao-server
```

每个模块目录下放一个空 `build.gradle`（内容为空即可）。

- [ ] **Step 9: 生成 Gradle Wrapper**

```bash
gradle wrapper --gradle-version 8.14
```

- [ ] **Step 10: 验证骨架加载**

```bash
./gradlew help
```

Expected: 成功输出，无报错。

- [ ] **Step 11: 提交**

```bash
git add settings.gradle build.gradle build-logic/ gradle/ modules/
git commit -m "feat: 创建 Gradle 构建骨架（build-logic + version catalog + convention plugins）"
```

---

## Task 2: 迁移 yudao-common

**Files:**
- Move: `yudao-framework/yudao-common/src/` → `modules/yudao-common/src/`
- Create: `modules/yudao-common/build.gradle`

- [ ] **Step 1: 移动源码**

```bash
cp -r yudao-framework/yudao-common/src modules/yudao-common/
```

- [ ] **Step 2: 创建 build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    // Spring 核心（provided — 只有工具类用到）
    compileOnly 'org.springframework:spring-core'
    compileOnly 'org.springframework:spring-expression'
    compileOnly 'org.springframework:spring-aop'
    compileOnly 'org.aspectj:aspectjweaver'
    compileOnly 'org.springframework:spring-web'
    compileOnly 'jakarta.servlet:jakarta.servlet-api'
    compileOnly 'org.springdoc:springdoc-openapi-starter-webmvc-ui'
    compileOnly 'org.springframework.cloud:spring-cloud-openfeign-core'
    compileOnly 'com.google.guava:guava'
    compileOnly 'com.fasterxml.jackson.core:jackson-databind'
    compileOnly 'com.fasterxml.jackson.core:jackson-core'
    compileOnly 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
    compileOnly 'org.slf4j:slf4j-api'
    compileOnly 'jakarta.validation:jakarta.validation-api'

    // 监控
    api libs.skywalking.trace

    // 工具
    api libs.hutool.all
    api 'com.alibaba:transmittable-thread-local:2.14.5'
    api 'com.fhs-opensource:easy-trans-anno:3.0.6'

    // MapStruct（convention plugin 已处理 annotationProcessor/compileOnly，这里补充 jdk8）
    implementation 'org.mapstruct:mapstruct-jdk8'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

- [ ] **Step 3: 验证编译**

```bash
./gradlew :modules:yudao-common:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add modules/yudao-common/
git commit -m "feat: 迁移 yudao-common 到 Gradle"
```

---

## Task 3: 迁移底层 starter（env、redis、job、rpc、biz-ip）

这些模块只依赖 yudao-common，无 starter 间交叉依赖。

**Files:**
- Move: 各模块 `src/` → `modules/<name>/src/`
- Create: 各模块 `build.gradle`

- [ ] **Step 1: 移动源码**

```bash
cp -r yudao-framework/yudao-spring-boot-starter-env/src modules/yudao-spring-boot-starter-env/
cp -r yudao-framework/yudao-spring-boot-starter-redis/src modules/yudao-spring-boot-starter-redis/
cp -r yudao-framework/yudao-spring-boot-starter-job/src modules/yudao-spring-boot-starter-job/
cp -r yudao-framework/yudao-spring-boot-starter-rpc/src modules/yudao-spring-boot-starter-rpc/
cp -r yudao-framework/yudao-spring-boot-starter-biz-ip/src modules/yudao-spring-boot-starter-biz-ip/
```

- [ ] **Step 2: 创建 build-logic/src/main/groovy/yudao-spring-boot-starter-env/build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-common')
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework:spring-web'
    implementation 'jakarta.servlet:jakarta.servlet-api'
    implementation 'org.springframework.cloud:spring-cloud-loadbalancer'
    implementation 'io.github.openfeign:feign-core'
    implementation 'com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery'
}
```

- [ ] **Step 3: 创建 modules/yudao-spring-boot-starter-redis/build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-common')
    api libs.redisson
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
}
```

- [ ] **Step 4: 创建 modules/yudao-spring-boot-starter-job/build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-common')
    implementation 'org.springframework.boot:spring-boot-starter'
    api libs.xxl.job
    implementation 'jakarta.validation:jakarta.validation-api'
}
```

- [ ] **Step 5: 创建 modules/yudao-spring-boot-starter-rpc/build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-common')
    api 'org.springframework.cloud:spring-cloud-starter-loadbalancer'
    api 'org.springframework.cloud:spring-cloud-starter-openfeign'
    api 'io.github.openfeign:feign-okhttp'
    implementation 'org.apache.httpcomponents:httpclient:4.5.14'
    implementation 'jakarta.validation:jakarta.validation-api'
}
```

- [ ] **Step 6: 创建 modules/yudao-spring-boot-starter-biz-ip/build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-common')
    api libs.ip2region
    compileOnly 'org.slf4j:slf4j-api'
    testImplementation project(':modules:yudao-spring-boot-starter-test')
}
```

- [ ] **Step 7: 验证编译**

```bash
./gradlew :modules:yudao-spring-boot-starter-env:compileJava
./gradlew :modules:yudao-spring-boot-starter-redis:compileJava
./gradlew :modules:yudao-spring-boot-starter-job:compileJava
./gradlew :modules:yudao-spring-boot-starter-rpc:compileJava
./gradlew :modules:yudao-spring-boot-starter-biz-ip:compileJava
```

Expected: 全部 BUILD SUCCESSFUL

- [ ] **Step 8: 提交**

```bash
git add modules/yudao-spring-boot-starter-{env,redis,job,rpc,biz-ip}/
git commit -m "feat: 迁移 env、redis、job、rpc、biz-ip starter 到 Gradle"
```

---

## Task 4: 迁移 mybatis starter

**Files:**
- Move: `yudao-framework/yudao-spring-boot-starter-mybatis/src/` → `modules/yudao-spring-boot-starter-mybatis/src/`
- Create: `modules/yudao-spring-boot-starter-mybatis/build.gradle`

- [ ] **Step 1: 移动源码**

```bash
cp -r yudao-framework/yudao-spring-boot-starter-mybatis/src modules/yudao-spring-boot-starter-mybatis/
```

- [ ] **Step 2: 创建 build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-common')
    compileOnly project(':modules:yudao-spring-boot-starter-security')

    runtimeOnly 'com.mysql:mysql-connector-j'
    compileOnly 'com.oracle.database.jdbc:ojdbc8'
    compileOnly 'org.postgresql:postgresql'
    compileOnly 'com.microsoft.sqlserver:mssql-jdbc'
    compileOnly 'com.dameng:DmJdbcDriver18'
    compileOnly 'cn.com.kingbase:kingbase8'
    compileOnly 'org.opengauss:opengauss-jdbc'

    api libs.druid
    api libs.mybatis.plus
    api libs.mybatis.plus.jsqlparser
    api libs.mybatis.plus.join
    api libs.dynamic.datasource
    api libs.easy.trans.starter
    api libs.easy.trans.mybatis

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

- [ ] **Step 3: 验证编译**

```bash
./gradlew :modules:yudao-spring-boot-starter-mybatis:compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add modules/yudao-spring-boot-starter-mybatis/
git commit -m "feat: 迁移 mybatis starter 到 Gradle"
```

---

## Task 5: 迁移 mq starter

**Files:**
- Move: `yudao-framework/yudao-spring-boot-starter-mq/src/` → `modules/yudao-spring-boot-starter-mq/src/`
- Create: `modules/yudao-spring-boot-starter-mq/build.gradle`

- [ ] **Step 1: 移动源码**

```bash
cp -r yudao-framework/yudao-spring-boot-starter-mq/src modules/yudao-spring-boot-starter-mq/
```

- [ ] **Step 2: 创建 build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-spring-boot-starter-redis')
    compileOnly 'org.springframework.kafka:spring-kafka'
    compileOnly 'org.springframework.amqp:spring-rabbit'
    compileOnly libs.rocketmq.spring
}
```

- [ ] **Step 3: 验证编译**

```bash
./gradlew :modules:yudao-spring-boot-starter-mq:compileJava
```

- [ ] **Step 4: 提交**

```bash
git add modules/yudao-spring-boot-starter-mq/
git commit -m "feat: 迁移 mq starter 到 Gradle"
```

---

## Task 6: 迁移 web starter

**Files:**
- Move: `yudao-framework/yudao-spring-boot-starter-web/src/` → `modules/yudao-spring-boot-starter-web/src/`
- Create: `modules/yudao-spring-boot-starter-web/build.gradle`

- [ ] **Step 1: 移动源码**

```bash
cp -r yudao-framework/yudao-spring-boot-starter-web/src modules/yudao-spring-boot-starter-web/
```

- [ ] **Step 2: 创建 build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-common')
    api 'org.springframework.boot:spring-boot-starter-web'
    api 'org.springframework.boot:spring-boot-starter-validation'
    compileOnly 'org.springframework.security:spring-security-core'
    api libs.knife4j.openapi
    api libs.springdoc.openapi
    compileOnly project(':modules:yudao-spring-boot-starter-rpc')
    compileOnly libs.guava
    api libs.jsoup
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

- [ ] **Step 3: 验证编译**

```bash
./gradlew :modules:yudao-spring-boot-starter-web:compileJava
```

- [ ] **Step 4: 提交**

```bash
git add modules/yudao-spring-boot-starter-web/
git commit -m "feat: 迁移 web starter 到 Gradle"
```

---

## Task 7: 迁移 security starter

**Files:**
- Move: `yudao-framework/yudao-spring-boot-starter-security/src/` → `modules/yudao-spring-boot-starter-security/src/`
- Create: `modules/yudao-spring-boot-starter-security/build.gradle`

- [ ] **Step 1: 移动源码**

```bash
cp -r yudao-framework/yudao-spring-boot-starter-security/src modules/yudao-spring-boot-starter-security/
```

- [ ] **Step 2: 创建 build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-common')
    implementation 'org.springframework.boot:spring-boot-starter-aop'
    api project(':modules:yudao-spring-boot-starter-web')
    implementation 'org.springframework.boot:spring-boot-starter-security'
    compileOnly project(':modules:yudao-spring-boot-starter-rpc')
    api libs.guava
    implementation libs.bizlog.sdk
}
```

注意: `bizlog-sdk` 需要加到 `libs.versions.toml`（已包含但需确认）。如果 libs 中没有，用 `implementation 'io.github.mouzt:bizlog-sdk:3.0.6'` 替代。

- [ ] **Step 3: 验证编译**

```bash
./gradlew :modules:yudao-spring-boot-starter-security:compileJava
```

- [ ] **Step 4: 提交**

```bash
git add modules/yudao-spring-boot-starter-security/
git commit -m "feat: 迁移 security starter 到 Gradle"
```

---

## Task 8: 迁移 monitor、protection、excel starter

**Files:**
- Move 各模块 src/ → modules/
- Create 各 build.gradle

- [ ] **Step 1: 移动源码**

```bash
cp -r yudao-framework/yudao-spring-boot-starter-monitor/src modules/yudao-spring-boot-starter-monitor/
cp -r yudao-framework/yudao-spring-boot-starter-protection/src modules/yudao-spring-boot-starter-protection/
cp -r yudao-framework/yudao-spring-boot-starter-excel/src modules/yudao-spring-boot-starter-excel/
```

- [ ] **Step 2: 创建 monitor build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-common')
    implementation 'org.springframework.boot:spring-boot-starter-aop'
    compileOnly 'org.springframework:spring-web'
    compileOnly 'jakarta.servlet:jakarta.servlet-api'
    compileOnly 'io.opentracing:opentracing-util:0.33.0'
    compileOnly libs.skywalking.trace
    compileOnly 'org.apache.skywalking:apm-toolkit-logback-1.x:9.6.0'
    compileOnly 'org.apache.skywalking:apm-toolkit-opentracing:9.6.0'
    compileOnly 'io.micrometer:micrometer-registry-prometheus'
    compileOnly libs.spring.boot.admin.client
}
```

- [ ] **Step 3: 创建 protection build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    compileOnly project(':modules:yudao-spring-boot-starter-web')
    api project(':modules:yudao-spring-boot-starter-redis')
    compileOnly libs.lock4j
    testImplementation project(':modules:yudao-spring-boot-starter-test')
}
```

- [ ] **Step 4: 创建 excel build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-common')
    implementation 'org.springframework.boot:spring-boot-starter'
    compileOnly project(':modules:yudao-spring-boot-starter-rpc')
    compileOnly 'org.springframework:spring-web'
    compileOnly 'jakarta.servlet:jakarta.servlet-api'
    api libs.fastexcel
    api libs.guava
    compileOnly project(':modules:yudao-spring-boot-starter-biz-ip')
    testImplementation project(':modules:yudao-spring-boot-starter-test')
}
```

- [ ] **Step 5: 验证编译**

```bash
./gradlew :modules:yudao-spring-boot-starter-monitor:compileJava
./gradlew :modules:yudao-spring-boot-starter-protection:compileJava
./gradlew :modules:yudao-spring-boot-starter-excel:compileJava
```

- [ ] **Step 6: 提交**

```bash
git add modules/yudao-spring-boot-starter-{monitor,protection,excel}/
git commit -m "feat: 迁移 monitor、protection、excel starter 到 Gradle"
```

---

## Task 9: 迁移 biz-tenant、biz-data-permission、websocket、test

**Files:**
- Move 各模块 src/ → modules/
- Create 各 build.gradle

- [ ] **Step 1: 移动源码**

```bash
cp -r yudao-framework/yudao-spring-boot-starter-biz-tenant/src modules/yudao-spring-boot-starter-biz-tenant/
cp -r yudao-framework/yudao-spring-boot-starter-biz-data-permission/src modules/yudao-spring-boot-starter-biz-data-permission/
cp -r yudao-framework/yudao-spring-boot-starter-websocket/src modules/yudao-spring-boot-starter-websocket/
cp -r yudao-framework/yudao-spring-boot-starter-test/src modules/yudao-spring-boot-starter-test/
```

- [ ] **Step 2: 创建 biz-tenant build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-common')
    api project(':modules:yudao-spring-boot-starter-security')
    api project(':modules:yudao-spring-boot-starter-mybatis')
    api project(':modules:yudao-spring-boot-starter-redis')
    compileOnly project(':modules:yudao-spring-boot-starter-rpc')
    compileOnly project(':modules:yudao-spring-boot-starter-job')
    compileOnly project(':modules:yudao-spring-boot-starter-mq')
    compileOnly 'org.springframework.kafka:spring-kafka'
    compileOnly 'org.springframework.amqp:spring-rabbit'
    compileOnly libs.rocketmq.spring
    api libs.guava
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

- [ ] **Step 3: 创建 biz-data-permission build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-common')
    compileOnly project(':modules:yudao-spring-boot-starter-security')
    api project(':modules:yudao-spring-boot-starter-mybatis')
    compileOnly project(':modules:yudao-spring-boot-starter-rpc')
    testImplementation project(':modules:yudao-spring-boot-starter-test')
}
```

- [ ] **Step 4: 创建 websocket build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-common')
    compileOnly project(':modules:yudao-spring-boot-starter-security')
    api 'org.springframework.boot:spring-boot-starter-websocket'
    api project(':modules:yudao-spring-boot-starter-mq')
    compileOnly 'org.springframework.kafka:spring-kafka'
    compileOnly 'org.springframework.amqp:spring-rabbit'
    compileOnly libs.rocketmq.spring
    compileOnly project(':modules:yudao-spring-boot-starter-biz-tenant')
}
```

- [ ] **Step 5: 创建 test build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-common')
    api project(':modules:yudao-spring-boot-starter-mybatis')
    api project(':modules:yudao-spring-boot-starter-redis')
    api libs.mockito
    api 'org.springframework.boot:spring-boot-starter-test'
    runtimeOnly 'com.h2database:h2'
    api libs.jedis.mock
    api libs.podam
}
```

- [ ] **Step 6: 验证编译**

```bash
./gradlew :modules:yudao-spring-boot-starter-biz-tenant:compileJava
./gradlew :modules:yudao-spring-boot-starter-biz-data-permission:compileJava
./gradlew :modules:yudao-spring-boot-starter-websocket:compileJava
./gradlew :modules:yudao-spring-boot-starter-test:compileJava
```

- [ ] **Step 7: 提交**

```bash
git add modules/yudao-spring-boot-starter-{biz-tenant,biz-data-permission,websocket,test}/
git commit -m "feat: 迁移 biz-tenant、biz-data-permission、websocket、test starter 到 Gradle"
```

---

## Task 10: 迁移 API 模块（system-api、infra-api）

**Files:**
- Move src/ → modules/
- Create build.gradle

- [ ] **Step 1: 移动源码**

```bash
cp -r yudao-module-system/yudao-module-system-api/src modules/yudao-module-system-api/
cp -r yudao-module-infra/yudao-module-infra-api/src modules/yudao-module-infra-api/
```

- [ ] **Step 2: 创建 system-api build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-common')
    compileOnly libs.springdoc.openapi
    compileOnly 'org.springframework.boot:spring-boot-starter-validation'
    compileOnly 'org.springframework.cloud:spring-cloud-starter-openfeign'
}
```

- [ ] **Step 3: 创建 infra-api build.gradle**

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api project(':modules:yudao-common')
    compileOnly libs.springdoc.openapi
    compileOnly 'org.springframework.boot:spring-boot-starter-validation'
    compileOnly 'org.springframework.cloud:spring-cloud-starter-openfeign'
}
```

- [ ] **Step 4: 验证编译**

```bash
./gradlew :modules:yudao-module-system-api:compileJava
./gradlew :modules:yudao-module-infra-api:compileJava
```

- [ ] **Step 5: 提交**

```bash
git add modules/yudao-module-{system,infra}-api/
git commit -m "feat: 迁移 system-api、infra-api 到 Gradle"
```

---

## Task 11: 迁移 Server 模块（system-server、infra-server）

**Files:**
- Move src/ → modules/
- Create build.gradle

- [ ] **Step 1: 移动源码**

```bash
cp -r yudao-module-system/yudao-module-system-server/src modules/yudao-module-system-server/
cp -r yudao-module-infra/yudao-module-infra-server/src modules/yudao-module-infra-server/
```

- [ ] **Step 2: 创建 system-server build.gradle**

```groovy
plugins {
    id 'yudao-spring-app'
}

dependencies {
    implementation project(':modules:yudao-spring-boot-starter-env')
    implementation project(':modules:yudao-module-system-api')
    implementation project(':modules:yudao-module-infra-api')

    implementation project(':modules:yudao-spring-boot-starter-biz-data-permission')
    implementation project(':modules:yudao-spring-boot-starter-biz-tenant')
    implementation project(':modules:yudao-spring-boot-starter-biz-ip')

    implementation project(':modules:yudao-spring-boot-starter-security')
    implementation project(':modules:yudao-spring-boot-starter-mybatis')
    implementation project(':modules:yudao-spring-boot-starter-redis')
    implementation project(':modules:yudao-spring-boot-starter-rpc')
    implementation project(':modules:yudao-spring-boot-starter-job')
    implementation project(':modules:yudao-spring-boot-starter-mq')
    implementation project(':modules:yudao-spring-boot-starter-excel')
    implementation project(':modules:yudao-spring-boot-starter-monitor')

    implementation 'com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery'
    implementation 'com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-config'

    implementation libs.justauth
    implementation(libs.justauth.starter) {
        exclude group: 'cn.hutool', module: 'hutool-core'
    }
    implementation libs.weixin.mp
    implementation libs.weixin.miniapp
    implementation 'com.anji-plus:captcha-spring-boot-starter:1.4.0'
    implementation libs.hutool.extra
    implementation 'org.springframework.boot:spring-boot-starter-mail'

    testImplementation project(':modules:yudao-spring-boot-starter-test')
}
```

- [ ] **Step 3: 创建 infra-server build.gradle**

```groovy
plugins {
    id 'yudao-spring-app'
}

dependencies {
    implementation project(':modules:yudao-spring-boot-starter-env')
    implementation project(':modules:yudao-module-infra-api')

    implementation project(':modules:yudao-spring-boot-starter-biz-tenant')
    implementation project(':modules:yudao-spring-boot-starter-security')
    implementation project(':modules:yudao-spring-boot-starter-websocket')
    implementation project(':modules:yudao-spring-boot-starter-mybatis')
    implementation 'com.baomidou:mybatis-plus-generator'
    implementation project(':modules:yudao-spring-boot-starter-redis')
    implementation project(':modules:yudao-spring-boot-starter-rpc')
    implementation 'com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery'
    implementation 'com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-config'
    implementation project(':modules:yudao-spring-boot-starter-job')
    implementation project(':modules:yudao-spring-boot-starter-mq')
    implementation project(':modules:yudao-spring-boot-starter-excel')
    implementation libs.velocity
    implementation project(':modules:yudao-spring-boot-starter-monitor')

    implementation 'commons-net:commons-net:3.13.0'
    implementation 'com.github.mwiede:jsch:2.28.2'
    implementation 'software.amazon.awssdk:s3'
    implementation libs.tika.core

    testImplementation project(':modules:yudao-spring-boot-starter-test')
}
```

注意: `libs.velocity` 需要在 `libs.versions.toml` 中添加:
```toml
velocity = { module = "org.apache.velocity:velocity-engine-core", version.ref = "velocity" }
```
版本已在 `[versions]` 中定义: `velocity = "2.4.1"`

- [ ] **Step 4: 验证编译**

```bash
./gradlew :modules:yudao-module-system-server:compileJava
./gradlew :modules:yudao-module-infra-server:compileJava
```

- [ ] **Step 5: 提交**

```bash
git add modules/yudao-module-{system,infra}-server/
git commit -m "feat: 迁移 system-server、infra-server 到 Gradle"
```

---

## Task 12: 迁移 gateway 和 server

**Files:**
- Move src/ → modules/
- Create build.gradle

- [ ] **Step 1: 移动源码**

```bash
cp -r yudao-gateway/src modules/yudao-gateway/
cp -r yudao-server/src modules/yudao-server/
```

- [ ] **Step 2: 创建 gateway build.gradle**

```groovy
plugins {
    id 'yudao-spring-app'
}

dependencies {
    implementation(project(':modules:yudao-module-system-api')) {
        exclude group: 'org.springdoc', module: 'springdoc-openapi-webmvc-core'
    }

    implementation 'org.springframework.cloud:spring-cloud-starter-gateway-server-webflux'
    implementation libs.knife4j.gateway
    implementation 'org.springframework.cloud:spring-cloud-starter-loadbalancer'
    implementation 'com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery'
    implementation 'com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-config'
    implementation project(':modules:yudao-spring-boot-starter-monitor')
    implementation libs.guava
    implementation 'com.github.ben-manes.caffeine:caffeine'
}
```

- [ ] **Step 3: 创建 server build.gradle**

```groovy
plugins {
    id 'yudao-spring-app'
}

springBoot {
    mainClass = 'cn.iocoder.yudao.server.YudaoServerApplication'
}

dependencies {
    implementation project(':modules:yudao-module-system-server')
    implementation project(':modules:yudao-module-infra-server')

    implementation project(':modules:yudao-spring-boot-starter-protection')
    implementation 'com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery'
    implementation 'com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-config'

    implementation(project(':modules:yudao-spring-boot-starter-rpc')) {
        exclude group: 'org.springframework.cloud', module: 'spring-cloud-starter-openfeign'
    }
}
```

- [ ] **Step 4: 复制 gateway 配置文件**

gateway 和 server 的配置文件（application.yml 等）在 `src/main/resources/` 下，已在 Step 1 中随 src/ 一起复制。

检查 gateway 是否有 `bootstrap.yml` 等 Spring Cloud 配置文件，确认已包含。

- [ ] **Step 5: 验证编译**

```bash
./gradlew :modules:yudao-gateway:compileJava
./gradlew :modules:yudao-server:compileJava
```

- [ ] **Step 6: 提交**

```bash
git add modules/yudao-gateway/ modules/yudao-server/
git commit -m "feat: 迁移 gateway、server 到 Gradle"
```

---

## Task 13: 全量编译验证

- [ ] **Step 1: 全量编译**

```bash
./gradlew build -x test
```

Expected: BUILD SUCCESSFUL，所有 22 个模块编译通过。

如果编译失败，根据报错信息逐个修复：
- 缺少依赖：在对应模块的 build.gradle 中添加
- 版本冲突：检查 libs.versions.toml 中的版本号
- 找不到符号：检查 project() 依赖路径是否正确

- [ ] **Step 2: 打胖包验证**

```bash
./gradlew bootJar
```

Expected: 生成 `modules/yudao-server/build/libs/modules.yudao-server.jar` 等可执行 jar。

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "fix: 修复全量编译问题"
```

（仅在有修复时提交）

---

## Task 14: 启动验证 + Swagger 验证

前置条件：本地 MySQL（127.0.0.1:3306/ruoyi-vue-pro）和 Redis（127.0.0.1:6379）已运行。

- [ ] **Step 1: 复制 lombok.config 到 modules/ 各模块**

lombok.config 在项目根目录，`config.stopBubbling = true` 会阻止向上查找。需要确认 lombok.config 对 modules/ 下各子目录是否可见。

如果不可见，复制到每个模块目录：

```bash
for dir in modules/*/; do cp lombok.config "$dir"; done
```

- [ ] **Step 2: 启动 YudaoServerApplication**

```bash
./gradlew :modules:yudao-server:bootRun
```

或直接运行 jar：

```bash
java -jar modules/yudao-server/build/libs/modules.yudao-server.jar
```

Expected: 应用成功启动，端口 48080 监听。

- [ ] **Step 3: 验证 Swagger 文档**

浏览器打开: `http://localhost:48080/doc.html`

Expected: Knife4j 文档页面正常显示，可以看到 system 和 infra 模块的 API 列表。

- [ ] **Step 4: 验证基本 API**

```bash
curl http://localhost:48080/
```

Expected: 正常响应（非 404）。

- [ ] **Step 5: 提交**

如果有 lombok.config 相关修复：

```bash
git add -A
git commit -m "fix: 修复 lombok.config 和启动配置"
```

---

## Task 15: 清理 Maven 文件和更新项目配置

**仅在 Task 13-14 全部验证通过后执行。**

**Files:**
- Delete: 所有 pom.xml
- Delete: `.flattened-pom.xml`
- Delete: `yudao-dependencies/`、`yudao-framework/`、`yudao-module-system/`、`yudao-module-infra/`（原 Maven 目录，源码已复制到 modules/）
- Delete: 原 `yudao-gateway/`、`yudao-server/`（源码已复制到 modules/）
- Modify: `CLAUDE.md`
- Modify: `.github/workflows/maven.yml` → `.github/workflows/gradle.yml`
- Modify: `.gitignore`

- [ ] **Step 1: 删除所有 pom.xml**

```bash
find . -name "pom.xml" -not -path "*/.git/*" -delete
```

- [ ] **Step 2: 删除 .flattened-pom.xml**

```bash
rm -f .flattened-pom.xml
```

- [ ] **Step 3: 删除原 Maven 目录**

确认 modules/ 下所有源码完整后：

```bash
rm -rf yudao-dependencies/
rm -rf yudao-framework/
rm -rf yudao-module-system/
rm -rf yudao-module-infra/
rm -rf yudao-gateway/
rm -rf yudao-server/
```

- [ ] **Step 4: 更新 .gitignore**

添加:
```
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar
```

移除 Maven 相关的 target/ 规则（如果有的话）。

- [ ] **Step 5: 更新 CLAUDE.md**

将 Build & Run 部分改为 Gradle 命令：

```markdown
## Build & Run

\```bash
# 全量编译（跳过测试）
./gradlew build -x test

# 编译单个模块
./gradlew :modules:yudao-module-system-server:compileJava

# 运行单元测试
./gradlew test

# 运行单个测试类
./gradlew :modules:yudao-module-system-server:test --tests "cn.iocoder...TenantPackageServiceImplTest"

# 运行单个测试方法
./gradlew :modules:yudao-module-system-server:test --tests "cn.iocoder...TenantPackageServiceImplTest.testCreateTenantPackage_success"
\```
```

更新架构描述中的模块路径引用。

- [ ] **Step 6: 更新 CI**

将 `.github/workflows/maven.yml` 重命名为 `.github/workflows/gradle.yml`，内容改为：

```yaml
name: Gradle Build
on:
  push:
    branches: [ master ]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
      - name: Build with Gradle
        run: ./gradlew build -x test
```

- [ ] **Step 7: 最终验证**

```bash
./gradlew clean build -x test
```

Expected: BUILD SUCCESSFUL（从干净状态重新编译通过）。

- [ ] **Step 8: 提交**

```bash
git add -A
git commit -m "feat: 完成 Maven → Gradle 迁移，删除所有 Maven 文件"
```
