# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 软件源与镜像

国内网络环境下，任何软件包的安装和下载应优先使用国内加速源和镜像：
- **Maven/Gradle**：阿里云公共仓库（`https://maven.aliyun.com/repository/public`）或华为云镜像
- **NPM/Yarn**：淘宝 NPM 镜像（`https://registry.npmmirror.com`）
- **Docker**：阿里云容器镜像服务、腾讯云镜像、中科大镜像
- **PyPI**：阿里云（`https://mirrors.aliyun.com/pypi/simple`）、清华源、华为源

执行安装和下载操作前，先检查当前源配置，确保使用可用的高质量国内镜像。

## Build & Run

构建系统是 **Gradle**（Groovy DSL），版本目录在 `gradle/libs.versions.toml`。

```bash
# 全量编译（跳过测试）
./gradlew build -x test

# 编译单个模块
./gradlew :modules:yudao-module-system-server:compileJava

# 运行单元测试（排除 db/api/integration tag）
./gradlew test

# 运行单个测试类
./gradlew :modules:yudao-module-system-server:test --tests "TenantPackageServiceImplTest"

# 运行单个测试方法
./gradlew :modules:yudao-module-system-server:test --tests "TenantPackageServiceImplTest.testCreateTenantPackage_success"

# AI Agent 快速验证门：编译 + 依赖审计 + 单元测试
./gradlew agentQuick

# AI Agent 全量验证门：agentQuick → agentDb（Testcontainers MySQL）→ agentApi
./gradlew agentVerify

# 仅运行数据库集成测试（需要 Docker）
./gradlew agentDb

# 仅运行 API 集成测试
./gradlew agentApi

# 依赖审计：检查是否引入了 fastjson
./gradlew agentDependencyAudit
```

**本地启动**（单体模式）：运行 `YudaoServerApplication`（主类），激活 `local` profile。需要本地 MySQL（`127.0.0.1:3306/ruoyi-vue-pro`）和 Redis（`127.0.0.1:6379`）。端口 48080。

可用 `docker-compose.local.yaml` 快速拉起 MySQL 8.0.33 和 Redis 7：
```bash
docker compose -f docker-compose.local.yaml up -d
```
首次启动会自动执行 `sql/mysql/ruoyi-vue-pro.sql` 和 `sql/mysql/quartz.sql` 初始化数据库。

**微服务模式**：分别启动 `GatewayServerApplication`（48080）、`SystemServerApplication`（48081）、`InfraServerApplication`（48082），依赖 Nacos。

## Architecture

yudao-cloud 的精简版（mini），基于 Spring Cloud Alibaba 微服务架构，仅保留 system 和 infra 两个业务模块。

### 双部署模式

- **单体模式**（`yudao-server`）：所有模块在同一个 JVM，OpenFeign 被排除，Nacos 禁用。通过 Spring Profile `local` 激活。
- **微服务模式**：每个模块独立部署，通过 `yudao-gateway`（Spring Cloud Gateway）路由，Nacos 注册/配置中心。

### 模块依赖关系

```
build-logic/                    Gradle 约定插件（yudao-java、yudao-spring-app）
gradle/libs.versions.toml      版本目录（统一管理所有依赖版本）
       │
modules/yudao-common            基础 POJO、枚举、工具类
modules/yudao-spring-boot-starter-*   16 个自定义 Spring Boot Starter
  ├── starter-mybatis           MyBatis-Plus 封装
  ├── starter-security          认证鉴权
  ├── starter-web               Web 层（Spring MVC + 全局异常处理）
  ├── starter-rpc               OpenFeign + LoadBalancer
  ├── starter-redis             Redisson 封装
  ├── starter-mq                消息队列抽象（Redis/RocketMQ/RabbitMQ/Kafka）
  ├── starter-excel             FastExcel 导入导出
  ├── starter-protection        限流、幂等、分布式锁（Lock4j）
  ├── starter-biz-tenant        SaaS 多租户
  ├── starter-biz-data-permission  数据权限
  └── ...其他 starter（env/job/monitor/websocket/biz-ip/test）
       │
modules/yudao-module-*-api      Feign 接口 + DTO，供跨模块调用
modules/yudao-module-*-server   业务实现，依赖 api + framework starters
       │
modules/yudao-server            单体壳，聚合所有 module-server
modules/yudao-gateway           Spring Cloud Gateway，路由 + 灰度负载均衡
```

每个模块的 `build.gradle` 通过约定插件 `yudao-java`（库项目）或 `yudao-spring-app`（Spring Boot 应用）应用公共配置。

### 业务模块分层结构

```
modules/yudao-module-<name>-server/src/main/java/cn/iocoder/yudao/module/<name>/
  controller/
    admin/            后台管理 API（@PreAuthorize 权限控制）
    app/              前台用户 API
      vo/             请求/响应 VO
  service/            接口 + Impl（XxxService / XxxServiceImpl）
  dal/
    dataobject/       DO 实体，继承 BaseDO
    mysql/            Mapper 接口，继承 BaseMapperX
    redis/            Redis key 常量
  convert/            MapStruct 转换器
  enums/              ErrorCodeConstants 等
  api/                模块内 RPC 接口（跨模块调用定义在 *-api 子模块）
```

## Code Conventions

### REST API 标准 CRUD

所有 Controller 返回 `CommonResult<T>`，URL 遵循 `/<module>/<business>/<action>` 模式：
- `POST /create` → `CommonResult<Long>`
- `PUT /update` → `CommonResult<Boolean>`
- `DELETE /delete` → `CommonResult<Boolean>`（`@RequestParam` 传 id）
- `GET /get` → `CommonResult<XxxRespVO>`
- `GET /page` → `CommonResult<PageResult<XxxRespVO>>`
- `GET /export-excel` / `POST /import` → Excel 导入导出

### VO 命名

| 类型 | 命名 | 用途 |
|------|------|------|
| SaveReqVO | `XxxSaveReqVO` | 新增和更新共用（id 为 null 是新增） |
| PageReqVO | `XxxPageReqVO` | 继承 PageParam，带筛选条件 |
| RespVO | `XxxRespVO` | 响应对象 |

### Service 方法命名

- `createXxx(SaveReqVO)` → 返回 `Long`（新 id）
- `updateXxx(SaveReqVO)` → `void`
- `deleteXxx(Long id)` → `void`
- `getXxx(Long id)` → `XxxDO`
- `getXxxPage(PageReqVO)` → `PageResult<XxxDO>`
- `validXxx(Long id)` → 校验存在性，不存在则抛 ServiceException

### DAL 层

- DO 继承 `BaseDO`（含 createTime/updateTime/creator/updater/deleted，支持逻辑删除和自动填充）
- Mapper 继承 `BaseMapperX<T>`（扩展自 MPJBaseMapper，提供分页、批量操作等默认方法）
- 查询使用 `LambdaQueryWrapperX` 的 `xxxIfPresent` 方法（null 安全的条件拼接）
- 查询方法写在 Mapper 接口中作为 `default` 方法，不需要 XML

### 其他约定

- 注入使用 `@Resource`（非 `@Autowired`）
- 错误码通过 `ErrorCodeConstants` 的 `static final ErrorCode` 定义，用 `throw exception(ERROR_CODE)` 抛出（静态导入）
- 简单转换用 `BeanUtils.toBean()`，复杂转换用 MapStruct（`XxxConvert.INSTANCE`）
- 权限注解：`@PreAuthorize("@ss.hasPermission('module:business:action')")`
- 多租户忽略：`@TenantIgnore`
- 数据权限忽略：`@DataPermission(enable = false)`

### Lombok 配置（lombok.config）

```
config.stopBubbling = true
lombok.accessors.chain = true          # setter 返回 this
lombok.tostring.callsuper = CALL       # toString 调用 super
lombok.equalsandhashcode.callsuper = CALL
```

### 禁止 fastjson

项目严格禁止直接使用 Alibaba fastjson（fastjson 1.x 和 fastjson2）。Gradle 构建脚本中有三个验证 task：
- `verifyNoFastjsonUsage` — 检查源码 import
- `verifyNoDirectFastjsonDependencies` — 检查直接依赖声明
- `verifyNoResolvedFastjsonRuntimeDependencies` — 检查解析后的运行时依赖

JSON 操作统一使用 `JsonUtils`（基于 Jackson ObjectMapper）。

## Testing

测试基类在 `modules/yudao-spring-boot-starter-test/`：

| 基类 | 场景 |
|------|------|
| `BaseMockitoUnitTest` | 纯 Mockito，无 Spring 上下文 |
| `BaseDbUnitTest` | H2 内存数据库测试 |
| `BaseRedisUnitTest` | 内嵌 Redis 测试 |

测试工具：
- `RandomUtils.randomPojo(Class, Consumer...)` — 生成随机 POJO（Podam）
- `AssertUtils.assertPojoEquals(expected, actual, ignoreFields...)` — 反射比较字段
- `AssertUtils.assertServiceException(executable, errorCode)` — 断言 ServiceException

跨模块依赖用 `@MockitoBean` mock。

测试通过 JUnit 5 Tag 区分层级：默认跑的 `test` 排除了 `db`、`api`、`integration` 标签。DB 集成测试用 Testcontainers（需要 Docker）。

## Tech Stack

- **Java 25** + **Spring Boot 3.5.14** + **Spring Cloud 2025.0.1** + **Spring Cloud Alibaba 2025.0.0.0**
- **MyBatis-Plus 3.5.16** + MyBatis-Plus Join 1.5.7（yulichang）
- **Druid** 连接池 + dynamic-datasource 4.5.0（多数据源）
- **Redisson** 4.4.0
- **Easy-Trans** 3.0.6（字段翻译）
- **XXL-Job** 2.4.0（分布式任务调度）
- **Nacos** 注册中心 + 配置中心
- **MapStruct** 1.6.3 + **Lombok** 1.18.46
- **Knife4j** 4.5.0（Swagger/OpenAPI v3 聚合）
- 支持 8 种数据库（MySQL、PostgreSQL、Oracle、SQL Server、DM、OpenGauss、KingBase、DB2）

## CI

GitHub Actions（`.github/workflows/`）：push 到 master 触发，Java 25（Temurin），`./gradlew build -x test`。
