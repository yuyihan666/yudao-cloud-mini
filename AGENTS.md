# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## 软件源与镜像

国内网络环境下，任何软件包的安装和下载应优先使用国内加速源和镜像：
- **Maven**：阿里云公共仓库（`https://maven.aliyun.com/repository/public`）或华为云镜像
- **NPM/Yarn**：淘宝 NPM 镜像（`https://registry.npmmirror.com`）
- **Docker**：阿里云容器镜像服务、腾讯云镜像、中科大镜像
- **PyPI**：阿里云（`https://mirrors.aliyun.com/pypi/simple`）、清华源、华为源
- **Git 仓库**：gitee mirror 或配置 git proxy
- **系统包管理器**（brew/apt/yum）：中科大、清华、阿里云等镜像站

执行安装和下载操作前，先检查当前源配置，确保使用可用的高质量国内镜像，避免因网络问题导致超时或失败。

## Build & Run

```bash
# 全量编译（跳过测试）
mvn -B package -Dmaven.test.skip=true

# 编译单个模块
mvn compile -pl yudao-module-system/yudao-module-system-server -am

# 运行单元测试
mvn test

# 运行单个测试类
mvn test -pl yudao-module-system/yudao-module-system-server -Dtest=TenantPackageServiceImplTest

# 运行单个测试方法
mvn test -pl yudao-module-system/yudao-module-system-server -Dtest=TenantPackageServiceImplTest#testCreateTenantPackage_success
```

**本地启动**（单体模式）：运行 `YudaoServerApplication`，激活 `local` profile。需要本地 MySQL（`127.0.0.1:3306/ruoyi-vue-pro`）和 Redis（`127.0.0.1:6379`）。端口 48080。

**微服务模式**：分别启动 `GatewayServerApplication`、`SystemServerApplication`（48081）、`InfraServerApplication`（48082），依赖 Nacos。

## Architecture

这是 yudao-cloud 的精简版（mini），基于 Spring Cloud Alibaba 微服务架构，仅保留 system 和 infra 两个业务模块。

### 双部署模式

项目支持两种部署模式，通过配置切换：
- **单体模式**（yudao-server）：所有模块在同一个 JVM，OpenFeign 被排除，Nacos 禁用
- **微服务模式**：每个模块独立部署，通过 Gateway 路由，Nacos 注册/配置中心

### 模块依赖关系

```
yudao-dependencies (BOM，统一管理所有依赖版本)
       │
yudao-framework（16 个自定义 Spring Boot Starter）
  ├── yudao-common（基础 POJO、枚举、工具类）
  ├── yudao-spring-boot-starter-mybatis（MyBatis-Plus 封装）
  ├── yudao-spring-boot-starter-security（认证鉴权）
  ├── yudao-spring-boot-starter-rpc（OpenFeign + LoadBalancer）
  ├── yudao-spring-boot-starter-redis（Redisson 封装）
  ├── yudao-spring-boot-starter-mq（消息队列抽象，支持 Redis/RocketMQ/RabbitMQ/Kafka）
  ├── yudao-spring-boot-starter-biz-tenant（SaaS 多租户）
  └── ...其他 starter
       │
yudao-module-*-api（Feign 接口 + DTO，供跨模块调用）
yudao-module-*-server（业务实现，依赖 api + framework starters）
       │
yudao-server（单体壳，聚合所有 module-server）
yudao-gateway（Spring Cloud Gateway，路由 + 灰度负载均衡）
```

### 每个业务模块的分层结构

```
yudao-module-<name>/yudao-module-<name>-server/
  src/main/java/cn/iocoder/yudao/module/<name>/
    controller/
      admin/          后台管理 API（@PreAuthorize 权限控制）
      app/            前台用户 API
        vo/           请求/响应 VO
    service/          接口 + Impl（XxxService / XxxServiceImpl）
    dal/
      dataobject/     DO 实体，继承 BaseDO
      mysql/          Mapper 接口，继承 BaseMapperX
      redis/          Redis key 常量
    convert/          MapStruct 转换器
    enums/            ErrorCodeConstants 等
    api/              模块内 RPC 接口（跨模块调用定义在 *-api 子模块）
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

## Testing

测试基类在 `yudao-framework/yudao-spring-boot-starter-test/`：

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

## Tech Stack

- **Java 25** + **Spring Boot 3.5.9** + **Spring Cloud 2025.0.1** + **Spring Cloud Alibaba 2025.0.0.0**
- **MyBatis-Plus** + MyBatis-Plus Join（yulichang）
- **Druid** 连接池 + dynamic-datasource（多数据源）
- **Redisson** 4.4.0
- **Nacos** 注册中心 + 配置中心
- **MapStruct** 1.6.3 + **Lombok** 1.18.46
- **Knife4j**（Swagger/OpenAPI v3 聚合）
- 支持 8 种数据库（MySQL、PostgreSQL、Oracle、SQL Server、DM、OpenGauss、KingBase、DB2）

## CI

GitHub Actions（`.github/workflows/maven.yml`）：push 到 master 触发，Java 25（Temurin），`mvn -B package -Dmaven.test.skip=true`。
