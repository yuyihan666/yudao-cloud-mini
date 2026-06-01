# Maven → Gradle 迁移设计

## 目标

将 yudao-cloud-mini 项目从 Maven 彻底迁移到 Gradle（Groovy DSL），移除所有 pom.xml，用 Gradle 作为唯一构建工具。

## 方案选型

**选定：buildSrc Convention Plugins + Version Catalog**

- Version Catalog (`libs.versions.toml`) 替代 yudao-dependencies BOM
- buildSrc convention plugins 封装公共构建逻辑（Java 编译、注解处理器、Spring Boot 打包）
- 每个模块的 build.gradle 极简，只声明依赖差异

## 1. 目录映射

当前 Maven 3 层嵌套 → Gradle 单层 `modules/`：

| 当前 Maven 路径 | 迁移后 Gradle 路径 |
|----------------|-------------------|
| yudao-dependencies/pom.xml | gradle/libs.versions.toml |
| yudao-framework/yudao-common/ | modules/yudao-common/ |
| yudao-framework/yudao-spring-boot-starter-*/ | modules/yudao-spring-boot-starter-*/ |
| yudao-module-system/yudao-module-system-api/ | modules/yudao-module-system-api/ |
| yudao-module-system/yudao-module-system-server/ | modules/yudao-module-system-server/ |
| yudao-module-infra/yudao-module-infra-api/ | modules/yudao-module-infra-api/ |
| yudao-module-infra/yudao-module-infra-server/ | modules/yudao-module-infra-server/ |
| yudao-gateway/ | modules/yudao-gateway/ |
| yudao-server/ | modules/yudao-server/ |

去掉 yudao-framework、yudao-module-system、yudao-module-infra 三个中间聚合目录。源码 `src/` 内容原样搬入，不改包名和 Java 源码。

### settings.gradle

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

## 2. Version Catalog (libs.versions.toml)

替代 yudao-dependencies BOM，用 Gradle 标准 Version Catalog 管理所有版本。

### 结构

```toml
[versions]
revision = "2026.05-SNAPSHOT"
java = "25"
spring-boot = "3.5.14"
spring-cloud = "2025.0.1"
spring-cloud-alibaba = "2025.0.0.0"
# Web
springdoc = "2.8.17"
knife4j = "4.5.0"
# DB
druid = "1.2.28"
mybatis = "3.5.19"
mybatis-plus = "3.5.16"
mybatis-plus-join = "1.5.7"
dynamic-datasource = "4.5.0"
easy-trans = "3.0.6"
redisson = "4.4.0"
# MQ
rocketmq-spring = "2.3.5"
# Job
xxl-job = "2.4.0"
# 服务保障
lock4j = "2.2.7"
# 监控
skywalking = "9.6.0"
spring-boot-admin = "3.5.8"
opentracing = "0.33.0"
# Test
podam = "8.0.2.RELEASE"
jedis-mock = "1.1.12"
mockito = "5.18.0"
# 工具
lombok = "1.18.46"
mapstruct = "1.6.3"
hutool-5 = "5.8.44"
hutool-6 = "6.0.0-M22"
fastexcel = "1.3.0"
guava = "33.6.0-jre"
fastjson = "1.2.83"
commons-lang3 = "3.20.0"
jsoup = "1.22.2"
reflections = "0.10.2"
# 三方云
justauth = "1.16.7"
justauth-starter = "1.4.0"
weixin-java = "4.8.2-20260501.180637"
bouncycastle = "1.84"
alipay-sdk = "4.40.806.ALL"
awssdk = "2.44.0"

[libraries]
# Spring BOM
spring-boot-bom = { module = "org.springframework.boot:spring-boot-dependencies", version.ref = "spring-boot" }
spring-cloud-bom = { module = "org.springframework.cloud:spring-cloud-dependencies", version.ref = "spring-cloud" }
spring-cloud-alibaba-bom = { module = "com.alibaba.cloud:spring-cloud-alibaba-dependencies", version.ref = "spring-cloud-alibaba" }
# Web
springdoc-openapi = { module = "org.springdoc:springdoc-openapi-starter-webmvc-ui", version.ref = "springdoc" }
knife4j-openapi = { module = "com.github.xiaoymin:knife4j-openapi3-jakarta-spring-boot-starter", version.ref = "knife4j" }
knife4j-gateway = { module = "com.github.xiaoymin:knife4j-gateway-spring-boot-starter", version.ref = "knife4j" }
# DB
druid = { module = "com.alibaba:druid-spring-boot-3-starter", version.ref = "druid" }
mybatis-plus = { module = "com.baomidou:mybatis-plus-spring-boot3-starter", version.ref = "mybatis-plus" }
mybatis-plus-jsqlparser = { module = "com.baomidou:mybatis-plus-jsqlparser", version.ref = "mybatis-plus" }
mybatis-plus-join = { module = "com.github.yulichang:mybatis-plus-join-boot-starter", version.ref = "mybatis-plus-join" }
dynamic-datasource = { module = "com.baomidou:dynamic-datasource-spring-boot3-starter", version.ref = "dynamic-datasource" }
easy-trans-starter = { module = "com.fhs-opensource:easy-trans-spring-boot-starter", version.ref = "easy-trans" }
easy-trans-mybatis = { module = "com.fhs-opensource:easy-trans-mybatis-plus-extend", version.ref = "easy-trans" }
redisson = { module = "org.redisson:redisson-spring-boot-starter", version.ref = "redisson" }
# MQ
rocketmq-spring = { module = "org.apache.rocketmq:rocketmq-spring-boot-starter", version.ref = "rocketmq-spring" }
# Job
xxl-job = { module = "com.xuxueli:xxl-job-core", version.ref = "xxl-job" }
# 服务保障
lock4j = { module = "com.baomidou:lock4j-redisson-spring-boot-starter", version.ref = "lock4j" }
# 监控
skywalking-trace = { module = "org.apache.skywalking:apm-toolkit-trace", version.ref = "skywalking" }
spring-boot-admin-server = { module = "de.codecentric:spring-boot-admin-starter-server", version.ref = "spring-boot-admin" }
spring-boot-admin-client = { module = "de.codecentric:spring-boot-admin-starter-client", version.ref = "spring-boot-admin" }
# Test
jedis-mock = { module = "com.github.fppt:jedis-mock", version.ref = "jedis-mock" }
podam = { module = "uk.co.jemos.podam:podam", version.ref = "podam" }
# 工具
lombok = { module = "org.projectlombok:lombok", version.ref = "lombok" }
mapstruct = { module = "org.mapstruct:mapstruct", version.ref = "mapstruct" }
mapstruct-processor = { module = "org.mapstruct:mapstruct-processor", version.ref = "mapstruct" }
hutool-all = { module = "cn.hutool:hutool-all", version.ref = "hutool-5" }
hutool-extra = { module = "org.dromara.hutool:hutool-extra", version.ref = "hutool-6" }
fastexcel = { module = "cn.idev.excel:fastexcel", version.ref = "fastexcel" }
guava = { module = "com.google.guava:guava", version.ref = "guava" }
reflections = { module = "org.reflections:reflections", version.ref = "reflections" }
# 三方云
justauth = { module = "me.zhyd.oauth:JustAuth", version.ref = "justauth" }
justauth-starter = { module = "com.xkcoding.justauth:justauth-spring-boot-starter", version.ref = "justauth-starter" }
weixin-pay = { module = "com.github.binarywang:weixin-java-pay", version.ref = "weixin-java" }
weixin-mp = { module = "com.github.binarywang:wx-java-mp-spring-boot-starter", version.ref = "weixin-java" }
weixin-miniapp = { module = "com.github.binarywang:wx-java-miniapp-spring-boot-starter", version.ref = "weixin-java" }

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
```

### 设计决策

- Spring BOM 通过 `platform()` 引入：在根 build.gradle 中统一 `api(platform(libs.spring.boot.bom))` 等，子模块直接用 `implementation("org.springframework.boot:spring-boot-starter-web")` 不带版本号
- Maven `${revision}` 不再需要：Gradle 原生支持项目间依赖不带版本
- 排除和可选依赖在模块 build.gradle 中用 Gradle 原生语法处理

## 3. buildSrc Convention Plugins

### 目录结构

```
buildSrc/
├── build.gradle
└── src/main/groovy/
    ├── yudao-java.gradle        // Java 编译 + 注解处理器
    └── yudao-spring-app.gradle  // Spring Boot 应用打包
```

### 3.1 buildSrc/build.gradle

```groovy
plugins {
    id 'groovy-gradle-plugin'
}

repositories {
    mavenCentral()
    maven { url 'https://mirrors.huaweicloud.com/repository/maven/' }
    maven { url 'https://maven.aliyun.com/repository/public' }
}
```

### 3.2 yudao-java.gradle

所有 Java 模块通用。封装：Java 版本、编码、Lombok + MapStruct 注解处理器、`-parameters` 编译参数。

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
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
    annotationProcessor 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
    annotationProcessor 'org.mapstruct:mapstruct-processor'

    compileOnly 'org.projectlombok:lombok'
    implementation 'org.mapstruct:mapstruct'
}
```

### 3.3 yudao-spring-app.gradle

Spring Boot 可执行应用。用于 yudao-server、yudao-gateway、*-server。

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

### 3.4 根 build.gradle

```groovy
plugins {
    id 'java-library' apply false
}

allprojects {
    group = 'cn.iocoder.cloud'
    version = '2026.05-SNAPSHOT'

    repositories {
        mavenCentral()
        maven { url 'https://mirrors.huaweicloud.com/repository/maven/' }
        maven { url 'https://maven.aliyun.com/repository/public' }
    }
}

subprojects {
    dependencies {
        api(platform(libs.spring.boot.bom))
        api(platform(libs.spring.cloud.bom))
        api(platform(libs.spring.cloud.alibaba.bom))
    }
}
```

### 插件使用矩阵

| 模块类型 | 应用的插件 | 示例模块 |
|---------|-----------|---------|
| 纯库 | `yudao-java` | yudao-common、所有 starter、所有 *-api |
| Spring Boot 应用 | `yudao-spring-app` | yudao-server、yudao-gateway、*-server |

## 4. 模块 build.gradle 示例

### 4.1 纯库 — yudao-common

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api(libs.hutool.all)
    api(libs.guava)
    api(libs.fastjson)
    implementation(libs.commons.lang3)
    implementation(libs.reflections)

    api('org.springframework.boot:spring-boot-starter-validation')
    api('com.fasterxml.jackson.core:jackson-annotations')
    implementation('org.springframework:spring-web')
    implementation('org.springframework:spring-context')

    compileOnly('jakarta.servlet:jakarta.servlet-api')

    api(libs.ip2region)
    implementation(libs.tika.core)
}
```

### 4.2 Starter — yudao-spring-boot-starter-mybatis

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api(project(':modules:yudao-common'))

    compileOnly(project(':modules:yudao-spring-boot-starter-security'))

    runtimeOnly('com.mysql:mysql-connector-j')
    compileOnly('com.oracle.database.jdbc:ojdbc8')
    compileOnly('org.postgresql:postgresql')
    compileOnly('com.microsoft.sqlserver:mssql-jdbc')
    compileOnly('com.dameng:DmJdbcDriver18')
    compileOnly('cn.com.kingbase:kingbase8')
    compileOnly('org.opengauss:opengauss-jdbc')

    api(libs.druid)
    api(libs.mybatis.plus)
    api(libs.mybatis.plus.jsqlparser)
    api(libs.mybatis.plus.join)
    api(libs.dynamic.datasource)
    api(libs.easy.trans.starter)
    api(libs.easy.trans.mybatis)

    testImplementation('org.springframework.boot:spring-boot-starter-test')
}
```

### 4.3 API — yudao-module-system-api

```groovy
plugins {
    id 'yudao-java'
}

dependencies {
    api(project(':modules:yudao-common'))

    compileOnly('org.springframework.cloud:spring-cloud-starter-openfeign')
    compileOnly('org.springframework.boot:spring-boot-starter-validation')
    compileOnly(libs.springdoc.openapi)
}
```

### 4.4 Server — yudao-module-system-server

```groovy
plugins {
    id 'yudao-spring-app'
}

dependencies {
    implementation(project(':modules:yudao-spring-boot-starter-env'))
    implementation(project(':modules:yudao-module-system-api'))
    implementation(project(':modules:yudao-module-infra-api'))

    implementation(project(':modules:yudao-spring-boot-starter-biz-data-permission'))
    implementation(project(':modules:yudao-spring-boot-starter-biz-tenant'))
    implementation(project(':modules:yudao-spring-boot-starter-biz-ip'))

    implementation(project(':modules:yudao-spring-boot-starter-security'))
    implementation(project(':modules:yudao-spring-boot-starter-mybatis'))
    implementation(project(':modules:yudao-spring-boot-starter-redis'))
    implementation(project(':modules:yudao-spring-boot-starter-rpc'))
    implementation(project(':modules:yudao-spring-boot-starter-job'))
    implementation(project(':modules:yudao-spring-boot-starter-mq'))
    implementation(project(':modules:yudao-spring-boot-starter-excel'))
    implementation(project(':modules:yudao-spring-boot-starter-monitor'))

    implementation('com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery')
    implementation('com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-config')

    implementation(libs.justauth)
    implementation(libs.justauth.starter) {
        exclude group: 'cn.hutool', module: 'hutool-core'
    }
    implementation(libs.weixin.mp)
    implementation(libs.weixin.miniapp)
    implementation('com.anji-plus:captcha-spring-boot-starter')
    implementation(libs.hutool.extra)
    implementation('org.springframework.boot:spring-boot-starter-mail')

    testImplementation(project(':modules:yudao-spring-boot-starter-test'))
}
```

### 4.5 单体壳 — yudao-server

```groovy
plugins {
    id 'yudao-spring-app'
}

springBoot {
    mainClass = 'cn.iocoder.yudao.server.YudaoServerApplication'
}

dependencies {
    implementation(project(':modules:yudao-module-system-server'))
    implementation(project(':modules:yudao-module-infra-server'))

    implementation(project(':modules:yudao-spring-boot-starter-protection'))
    implementation('com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery')
    implementation('com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-config')

    implementation(project(':modules:yudao-spring-boot-starter-rpc')) {
        exclude group: 'org.springframework.cloud', module: 'spring-cloud-starter-openfeign'
    }
}
```

### 4.6 网关 — yudao-gateway

```groovy
plugins {
    id 'yudao-spring-app'
}

springBoot {
    mainClass = 'cn.iocoder.yudao.gateway.GatewayServerApplication'
}

dependencies {
    implementation(project(':modules:yudao-module-system-api')) {
        exclude group: 'org.springdoc', module: 'springdoc-openapi-webmvc-core'
    }

    implementation('org.springframework.cloud:spring-cloud-starter-gateway-server-webflux')
    implementation(libs.knife4j.gateway)
    implementation('org.springframework.cloud:spring-cloud-starter-loadbalancer')
    implementation('com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery')
    implementation('com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-config')
    implementation(project(':modules:yudao-spring-boot-starter-monitor'))
    implementation(libs.guava)
    implementation('com.github.ben-manes.caffeine:caffeine')
}
```

## 5. 迁移执行计划

### Phase 1：骨架搭建

- 创建 settings.gradle + 根 build.gradle
- 创建 gradle/libs.versions.toml
- 创建 buildSrc/ + 两个 convention plugins
- 创建 modules/ 目录结构（空壳 + 空 build.gradle）
- `gradle tasks` 验证骨架能加载

### Phase 2：逐模块搬运 + 编译验证

按依赖顺序搬入：

1. yudao-common（最底层，无内部依赖）
2. 各 starter（按依赖顺序：env → redis → mybatis → web → security → websocket → mq → rpc → ...）
3. *-api 模块
4. *-server 模块
5. yudao-gateway、yudao-server

每搬入一个模块，运行 `gradle :modules:<name>:compileJava` 验证。

### Phase 3：集成验证

- `gradle build -x test` 全量编译
- `gradle test` 运行测试（可选）
- `gradle bootJar` 打包验证
- 本地启动 YudaoServerApplication 验证运行

### Phase 4：收尾

- 删除所有 pom.xml 和 .flattened-pom.xml
- 删除 yudao-dependencies、yudao-framework、yudao-module-system、yudao-module-infra 空目录
- 更新 CLAUDE.md 构建命令
- 更新 .github/workflows/maven.yml → gradle.yml
- 更新 .gitignore（添加 .gradle/、build/，移除 target/ 相关规则）
- 生成 Gradle Wrapper (`gradle wrapper --gradle-version 8.14`)
- git commit

## 6. 边界情况

| 问题 | 处理方式 |
|------|---------|
| Maven `<optional>true</optional>` | 映射为 `compileOnly` |
| Maven `<scope>provided</scope>` | 映射为 `compileOnly` |
| Maven `<scope>test</scope>` | 映射为 `testImplementation` |
| Maven `<exclusions>` | Gradle `exclude group:, module:` 块 |
| lombok.config | 保持原样，Lombok 自动读取 |
| 双部署模式（单体/微服务） | 与 Maven 同理，单体模式 exclude OpenFeign |
| Spring Cloud Alibaba BOM | 在根 build.gradle 中用 `platform()` 引入 |
| 多数据库驱动 | 与 Maven 相同，runtimeOnly 放 MySQL，其余 compileOnly |

## 7. 构建命令对照

| 操作 | Maven | Gradle |
|------|-------|--------|
| 全量编译（跳过测试） | `mvn -B package -Dmaven.test.skip=true` | `gradle build -x test` |
| 编译单个模块 | `mvn compile -pl <module> -am` | `gradle :modules:<name>:compileJava` |
| 运行测试 | `mvn test` | `gradle test` |
| 单个测试类 | `mvn test -pl <module> -Dtest=FooTest` | `gradle :modules:xxx:test --tests "pkg.FooTest"` |
| 清理 | `mvn clean` | `gradle clean` |
| 打胖包 | `mvn package` | `gradle bootJar` |
| 依赖树 | `mvn dependency:tree` | `gradle :modules:xxx:dependencies` |
